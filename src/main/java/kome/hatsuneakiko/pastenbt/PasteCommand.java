package kome.hatsuneakiko.pastenbt;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class PasteCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("paste")
            .then(Commands.literal("ls")
                .executes(ctx -> listStructures(ctx, ""))
                .then(Commands.argument("folder", StringArgumentType.string())
                    .executes(ctx -> listStructures(ctx, StringArgumentType.getString(ctx, "folder"))))
            )
            .then(Commands.literal("grid")
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.argument("radius", IntegerArgumentType.integer(0, 5))
                        .executes(ctx -> executeGrid(ctx, SnapMode.CORNER))
                        .then(Commands.literal("center").executes(ctx -> executeGrid(ctx, SnapMode.CENTER)))
                        .then(Commands.literal("corner").executes(ctx -> executeGrid(ctx, SnapMode.CORNER)))
                    )
                )
            )
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(ctx -> execute(ctx, SnapMode.CORNER))
                .then(Commands.literal("center").executes(ctx -> execute(ctx, SnapMode.CENTER)))
                .then(Commands.literal("corner").executes(ctx -> execute(ctx, SnapMode.CORNER)))
            )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context, SnapMode snapMode) {
        String name = StringArgumentType.getString(context, "name");
        Networking.INSTANCE.sendToServer(new ServerboundRequestPreviewPacket(name, snapMode));
        context.getSource().sendSuccess(() -> Component.literal("Requesting preview for: " + name + " (" + snapMode + ")"), false);
        return 1;
    }

    private static int executeGrid(CommandContext<CommandSourceStack> context, SnapMode snapMode) {
        String name = StringArgumentType.getString(context, "name");
        int radius = IntegerArgumentType.getInteger(context, "radius");
        Networking.INSTANCE.sendToServer(new ServerboundGridPastePacket(name, radius, snapMode));
        context.getSource().sendSuccess(() -> Component.literal("Enqueuing grid for: " + name + " (radius " + radius + ")"), false);
        return 1;
    }

    private static int listStructures(CommandContext<CommandSourceStack> context, String folder) {
        CommandSourceStack source = context.getSource();
        File structuresDir = FMLPaths.GAMEDIR.get().resolve("structures").toFile();
        File targetDir = new File(structuresDir, folder);
        
        try {
            if (!targetDir.getCanonicalPath().startsWith(structuresDir.getCanonicalPath())) {
                source.sendFailure(Component.literal("Access denied."));
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            source.sendFailure(Component.literal("Folder not found: " + folder));
            return 0;
        }

        File[] filesArray = targetDir.listFiles();
        if (filesArray == null) filesArray = new File[0];
        
        Arrays.sort(filesArray, Comparator.comparing(File::getName));
        source.sendSuccess(() -> Component.literal("§6--- Structures in /" + folder + " ---"), false);

        for (File file : filesArray) {
            String prefix = file.isDirectory() ? "§e[DIR] §r" : "§b[FILE] §r";
            String displayName = file.getName().endsWith(".nbt") ? file.getName().substring(0, file.getName().length() - 4) : file.getName();
            String relPath = folder.isEmpty() ? displayName : folder + "/" + displayName;

            Component message = Component.literal(prefix).append(
                Component.literal(displayName).withStyle(style -> 
                    style.withColor(file.isDirectory() ? 0xFFFF55 : 0x55FFFF)
                         .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, file.isDirectory() ? "/paste ls " + relPath : "/paste " + relPath))
                         .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(file.isDirectory() ? "Click to open folder" : "Click to suggest paste command")))
                )
            );
            source.sendSuccess(() -> message, false);
        }
        return 1;
    }
}
