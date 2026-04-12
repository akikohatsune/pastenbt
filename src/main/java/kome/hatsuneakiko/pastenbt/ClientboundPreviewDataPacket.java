package kome.hatsuneakiko.pastenbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundPreviewDataPacket {
    private final String name;
    private final CompoundTag nbt;
    private final SnapMode snapMode;

    public ClientboundPreviewDataPacket(String name, CompoundTag nbt, SnapMode snapMode) {
        this.name = name;
        this.nbt = nbt;
        this.snapMode = snapMode;
    }

    public static void encode(ClientboundPreviewDataPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name);
        buf.writeNbt(msg.nbt);
        buf.writeEnum(msg.snapMode);
    }

    public static ClientboundPreviewDataPacket decode(FriendlyByteBuf buf) {
        return new ClientboundPreviewDataPacket(buf.readUtf(), buf.readNbt(), buf.readEnum(SnapMode.class));
    }

    public static void handle(ClientboundPreviewDataPacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> PreviewHandler.startPreview(msg.name, msg.nbt, msg.snapMode));
        ctx.setPacketHandled(true);
    }
}
