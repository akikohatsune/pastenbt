package kome.hatsuneakiko.pastenbt;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkDirection;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class PasteCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("paste")
            .requires(source -> source.hasPermission(2) || (source.getEntity() instanceof ServerPlayer && ModConfig.canUse((ServerPlayer) source.getEntity())))
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
            .then(Commands.literal("preview")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ctx -> executePreview(ctx, SnapMode.CORNER))
                    .then(Commands.literal("center").executes(ctx -> executePreview(ctx, SnapMode.CENTER)))
                    .then(Commands.literal("corner").executes(ctx -> executePreview(ctx, SnapMode.CORNER)))
                )
            )
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(ctx -> executePlace(ctx, SnapMode.CORNER))
                .then(Commands.literal("center").executes(ctx -> executePlace(ctx, SnapMode.CENTER)))
                .then(Commands.literal("corner").executes(ctx -> executePlace(ctx, SnapMode.CORNER)))
            )
        );
    }

    private static int executePlace(CommandContext<CommandSourceStack> context, SnapMode snapMode) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer)) return 0;
        ServerPlayer player = (ServerPlayer) source.getEntity();

        new Thread(() -> {
            try {
                File nbtFile = FMLPaths.GAMEDIR.get().resolve("structures/" + name + ".nbt").toFile();
                if (!nbtFile.exists()) {
                    player.sendSystemMessage(Component.literal("File not found: " + name));
                    return;
                }
                try (FileInputStream fis = new FileInputStream(nbtFile)) {
                    CompoundTag nbt = NbtIo.readCompressed(fis);
                    StructureTemplate template = new StructureTemplate();
                    template.load(player.serverLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK), nbt);
                    BlockPos origin = snapMode.snap(player.blockPosition());
                    List<StructureTemplate.StructureBlockInfo> blocks = template.filterBlocks(origin, new StructurePlaceSettings().setRotation(Rotation.NONE), null);
                    player.server.execute(() -> {
                        PlacementManager.add(new PlacementManager.PlacementTask(player.getUUID(), player.serverLevel(), blocks));
                        player.sendSystemMessage(Component.literal("§aEnqueued placement for:§r " + name));
                    });
                }
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("Error: " + e.getMessage()));
            }
        }).start();
        return 1;
    }

    private static int executePreview(CommandContext<CommandSourceStack> context, SnapMode snapMode) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer)) return 0;
        ServerPlayer player = (ServerPlayer) source.getEntity();

        new Thread(() -> {
            try {
                File nbtFile = FMLPaths.GAMEDIR.get().resolve("structures/" + name + ".nbt").toFile();
                if (!nbtFile.exists()) {
                    player.sendSystemMessage(Component.literal("File not found: " + name));
                    return;
                }
                try (FileInputStream fis = new FileInputStream(nbtFile)) {
                    CompoundTag nbt = NbtIo.readCompressed(fis);
                    Networking.INSTANCE.sendTo(new ClientboundPreviewDataPacket(name, nbt, snapMode), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                    player.sendSystemMessage(Component.literal("§ePreview mode activated. Use R to rotate, P to confirm, C to cancel."));
                }
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("Error: " + e.getMessage()));
            }
        }).start();
        return 1;
    }

    private static int executeGrid(CommandContext<CommandSourceStack> context, SnapMode snapMode) {
        String name = StringArgumentType.getString(context, "name");
        int radius = IntegerArgumentType.getInteger(context, "radius");
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer)) return 0;
        ServerPlayer player = (ServerPlayer) source.getEntity();

        new Thread(() -> {
            try {
                File nbtFile = FMLPaths.GAMEDIR.get().resolve("structures/" + name + ".nbt").toFile();
                if (!nbtFile.exists()) {
                    player.sendSystemMessage(Component.literal("File not found: " + name));
                    return;
                }
                try (FileInputStream fis = new FileInputStream(nbtFile)) {
                    CompoundTag nbt = NbtIo.readCompressed(fis);
                    StructureTemplate template = new StructureTemplate();
                    template.load(player.serverLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK), nbt);
                    int playerChunkX = player.blockPosition().getX() >> 4;
                    int playerChunkZ = player.blockPosition().getZ() >> 4;
                    
                    player.server.execute(() -> {
                        for (int x = -radius; x <= radius; x++) {
                            for (int z = -radius; z <= radius; z++) {
                                BlockPos origin = snapMode.snap(new BlockPos((playerChunkX + x) << 4, player.blockPosition().getY(), (playerChunkZ + z) << 4));
                                List<StructureTemplate.StructureBlockInfo> blocks = template.filterBlocks(origin, new StructurePlaceSettings().setRotation(Rotation.NONE), null);
                                PlacementManager.add(new PlacementManager.PlacementTask(player.getUUID(), player.serverLevel(), blocks));
                            }
                        }
                        player.sendSystemMessage(Component.literal("§aGrid enqueued:§r " + ((radius * 2 + 1) * (radius * 2 + 1)) + " tasks."));
                    });
                }
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("Error: " + e.getMessage()));
            }
        }).start();
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
