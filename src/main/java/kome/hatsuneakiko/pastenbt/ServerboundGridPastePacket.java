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

public class ServerboundGridPastePacket {
    private final String name;
    private final int radius;
    private final SnapMode snapMode;

    public ServerboundGridPastePacket(String name, int radius, SnapMode snapMode) {
        this.name = name;
        this.radius = radius;
        this.snapMode = snapMode;
    }

    public static void encode(ServerboundGridPastePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name);
        buf.writeInt(msg.radius);
        buf.writeEnum(msg.snapMode);
    }

    public static ServerboundGridPastePacket decode(FriendlyByteBuf buf) {
        return new ServerboundGridPastePacket(buf.readUtf(), buf.readInt(), buf.readEnum(SnapMode.class));
    }

    public static void handle(ServerboundGridPastePacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
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
                    int playerChunkX = player.blockPosition().getX() >> 4;
                    int playerChunkZ = player.blockPosition().getZ() >> 4;
                    ctx.enqueueWork(() -> {
                        for (int x = -msg.radius; x <= msg.radius; x++) {
                            for (int z = -msg.radius; z <= msg.radius; z++) {
                                BlockPos origin = msg.snapMode.snap(new BlockPos((playerChunkX + x) << 4, player.blockPosition().getY(), (playerChunkZ + z) << 4));
                                List<StructureTemplate.StructureBlockInfo> blocks = template.filterBlocks(origin, new StructurePlaceSettings().setRotation(Rotation.NONE), null);
                                PlacementManager.add(new PlacementManager.PlacementTask(player.getUUID(), player.serverLevel(), blocks));
                            }
                        }
                        player.sendSystemMessage(Component.literal("§b[Java 17]§r Grid enqueued: " + ((msg.radius * 2 + 1) * (msg.radius * 2 + 1)) + " tasks."));
                    });
                }
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("Error: " + e.getMessage()));
            }
        }).start();
        ctx.setPacketHandled(true);
    }
}
