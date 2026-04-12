package kome.hatsuneakiko.pastenbt;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class NbtPasterCommand {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;
    private static final int TIMEOUT = 10000;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nbtpaster")
            .requires(source -> source.getEntity() instanceof ServerPlayer && ModConfig.canUse((ServerPlayer) source.getEntity()))
            .then(Commands.literal("reload").executes(ctx -> {
                if (ModConfig.load()) {
                    ctx.getSource().sendSuccess(() -> Component.literal("NbtPaster config reloaded."), true);
                    return 1;
                } else {
                    ctx.getSource().sendFailure(Component.literal("Failed to reload config."));
                    return 0;
                }
            }))
            .then(Commands.literal("get")
                .then(Commands.argument("url", StringArgumentType.string())
                    .executes(ctx -> download(ctx, null))
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> download(ctx, StringArgumentType.getString(ctx, "name"))))
                )
            )
            .then(Commands.literal("delete")
                .then(Commands.argument("path", StringArgumentType.string())
                    .executes(NbtPasterCommand::delete)
                )
            )
        );
    }

    private static int delete(CommandContext<CommandSourceStack> context) {
        String pathStr = StringArgumentType.getString(context, "path");
        CommandSourceStack source = context.getSource();
        File structuresDir = FMLPaths.GAMEDIR.get().resolve("structures").toFile();
        if (!structuresDir.exists()) structuresDir.mkdirs();

        try {
            File targetFile = new File(structuresDir, pathStr).getCanonicalFile();
            File nbtFile = new File(structuresDir, pathStr + ".nbt").getCanonicalFile();

            if (!targetFile.getPath().startsWith(structuresDir.getCanonicalPath()) && !nbtFile.getPath().startsWith(structuresDir.getCanonicalPath())) {
                source.sendFailure(Component.literal("Access denied: Path is outside structures folder."));
                return 0;
            }

            File fileToDelete = null;
            if (targetFile.exists()) {
                fileToDelete = targetFile;
            } else if (nbtFile.exists()) {
                fileToDelete = nbtFile;
            }

            if (fileToDelete == null) {
                source.sendFailure(Component.literal("File or folder not found: " + pathStr));
                return 0;
            }

            if (deleteRecursively(fileToDelete)) {
                File finalFileToDelete = fileToDelete;
                source.sendSuccess(() -> Component.literal("§aDeleted:§r " + finalFileToDelete.getName()), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("Failed to delete: " + pathStr));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        return file.delete();
    }

    private static int download(CommandContext<CommandSourceStack> context, String customName) {
        String urlString = StringArgumentType.getString(context, "url");
        CommandSourceStack source = context.getSource();

        if (!urlString.toLowerCase().endsWith(".nbt")) {
            source.sendFailure(Component.literal("URL must end with .nbt"));
            return 0;
        }

        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(TIMEOUT);
                connection.setReadTimeout(TIMEOUT);
                
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    source.sendFailure(Component.literal("HTTP error: " + connection.getResponseCode()));
                    return;
                }

                if (connection.getContentLengthLong() > MAX_FILE_SIZE) {
                    source.sendFailure(Component.literal("File too large (>5MB)"));
                    return;
                }

                String rawName = customName != null ? customName : getFileNameFromUrl(urlString);
                String fileName = sanitizeFileName(rawName);
                if (!fileName.endsWith(".nbt")) fileName += ".nbt";

                File targetDir = FMLPaths.GAMEDIR.get().resolve("structures/downloads/" + UUID.randomUUID()).toFile();
                targetDir.mkdirs();
                File targetFile = new File(targetDir, fileName);

                try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                }
                String finalFileName = fileName;
                source.sendSuccess(() -> Component.literal("§a[Java 17 Async]§r Saved to downloads/" + targetDir.getName() + "/" + finalFileName), true);
            } catch (Exception e) {
                source.sendFailure(Component.literal("Download failed: " + e.getMessage()));
            }
        }).start();

        source.sendSuccess(() -> Component.literal("Starting download..."), false);
        return 1;
    }

    private static String getFileNameFromUrl(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash == -1 || lastSlash == url.length() - 1) return "structure.nbt";
        return url.substring(lastSlash + 1);
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
