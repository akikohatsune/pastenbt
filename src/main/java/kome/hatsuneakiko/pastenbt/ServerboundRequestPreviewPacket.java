package kome.hatsuneakiko.pastenbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.io.FileInputStream;
import java.util.function.Supplier;

public class ServerboundRequestPreviewPacket {
    private final String name;
    private final SnapMode snapMode;

    public ServerboundRequestPreviewPacket(String name, SnapMode snapMode) {
        this.name = name;
        this.snapMode = snapMode;
    }

    public static void encode(ServerboundRequestPreviewPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name);
        buf.writeEnum(msg.snapMode);
    }

    public static ServerboundRequestPreviewPacket decode(FriendlyByteBuf buf) {
        return new ServerboundRequestPreviewPacket(buf.readUtf(), buf.readEnum(SnapMode.class));
    }

    public static void handle(ServerboundRequestPreviewPacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
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
                    Networking.INSTANCE.sendTo(new ClientboundPreviewDataPacket(msg.name, nbt, msg.snapMode), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                }
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("Error: " + e.getMessage()));
            }
        }).start();
        ctx.setPacketHandled(true);
    }
}
