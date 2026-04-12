package kome.hatsuneakiko.pastenbt

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

class ClientboundPreviewDataPacket(val name: String, val nbt: CompoundTag, val snapMode: SnapMode) {
    companion object {
        @JvmStatic
        fun encode(msg: ClientboundPreviewDataPacket, buf: FriendlyByteBuf) {
            buf.writeUtf(msg.name)
            buf.writeNbt(msg.nbt)
            buf.writeEnum(msg.snapMode)
        }

        @JvmStatic
        fun decode(buf: FriendlyByteBuf): ClientboundPreviewDataPacket {
            return ClientboundPreviewDataPacket(buf.readUtf(), buf.readNbt()!!, buf.readEnum(SnapMode::class.java))
        }

        @JvmStatic
        fun handle(msg: ClientboundPreviewDataPacket, ctxGetter: Supplier<NetworkEvent.Context>) {
            val ctx = ctxGetter.get()
            ctx.enqueueWork {
                PreviewHandler.startPreview(msg.name, msg.nbt, msg.snapMode)
            }
            ctx.packetHandled = true
        }
    }
}
