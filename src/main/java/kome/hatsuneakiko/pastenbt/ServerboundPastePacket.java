package kome.hatsuneakiko.pastenbt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.function.Supplier;

public class ServerboundPastePacket {
    private final String name;
    private final Rotation rotation;
    private final SnapMode snapMode;

    public ServerboundPastePacket(String name, Rotation rotation, SnapMode snapMode) {
        this.name = name;
        this.rotation = rotation;
        this.snapMode = snapMode;
    }

    public static void encode(ServerboundPastePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name);
        buf.writeEnum(msg.rotation);
        buf.writeEnum(msg.snapMode);
    }

    public static ServerboundPastePacket decode(FriendlyByteBuf buf) {
        return new ServerboundPastePacket(buf.readUtf(), buf.readEnum(Rotation.class), buf.readEnum(SnapMode.class));
    }

    public static void handle(ServerboundPastePacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ServerPlayer player = ctx.getSender();
        if (player == null || !ModConfig.canUse(player)) {
            ctx.setPacketHandled(true);
            return;
        }

        new Thread(() -> {
            try {
                File nbtFile = FMLPaths.GAMEDIR.get().resolve("structures/" + msg.name + ".nbt").toFile();
                if (!nbtFile.exists()) {
                    player.sendSystemMessage(Component.literal("File not found: " + msg.name));
                    return;
                }
                try (FileInputStream fis = new FileInputStream(nbtFile)) {
                    CompoundTag nbt = NbtIo.readCompressed(fis);
                    StructureTemplate template = new StructureTemplate();
                    template.load(player.serverLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK), nbt);
                    BlockPos origin = msg.snapMode.snap(player.blockPosition());
                    StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(msg.rotation);
                    List<StructureTemplate.StructureBlockInfo> blocks = template.filterBlocks(origin, settings, null);
                    ctx.enqueueWork(() -> {
                        PlacementManager.add(new PlacementManager.PlacementTask(player.getUUID(), player.serverLevel(), blocks));
                        player.sendSystemMessage(Component.literal("§b[Java 17 Async]§r Prepared: " + msg.name));
                    });
                }
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("Error: " + e.getMessage()));
            }
        }).start();
        ctx.setPacketHandled(true);
    }
}
