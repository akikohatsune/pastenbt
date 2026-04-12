package kome.hatsuneakiko.pastenbt

import net.minecraft.core.registries.Registries
import net.minecraft.nbt.NbtIo
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraftforge.fml.loading.FMLPaths
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent
import java.io.FileInputStream
import java.util.function.Supplier

class ServerboundRequestPreviewPacket(val name: String, val snapMode: SnapMode) {
    companion object {
        @JvmStatic
        fun encode(msg: ServerboundRequestPreviewPacket, buf: FriendlyByteBuf) {
            buf.writeUtf(msg.name)
            buf.writeEnum(msg.snapMode)
        }

        @JvmStatic
        fun decode(buf: FriendlyByteBuf): ServerboundRequestPreviewPacket {
            return ServerboundRequestPreviewPacket(buf.readUtf(), buf.readEnum(SnapMode::class.java))
        }

        @JvmStatic
        fun handle(msg: ServerboundRequestPreviewPacket, ctxGetter: Supplier<NetworkEvent.Context>) {
            val ctx = ctxGetter.get()
            val player = ctx.sender ?: return
            if (!ModConfig.canUse(player)) return

            Thread.ofVirtual().start {
                try {
                    val nbtFile = FMLPaths.CONFIGDIR.get().resolve("structures/${msg.name}.nbt").toFile()
                    if (!nbtFile.exists()) {
                        player.sendSystemMessage(Component.literal("File not found: ${msg.name}"))
                        return@start
                    }
                    FileInputStream(nbtFile).use { fis ->
                        val nbt = NbtIo.readCompressed(fis)
                        Networking.INSTANCE.sendTo(ClientboundPreviewDataPacket(msg.name, nbt, msg.snapMode), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT)
                    }
                } catch (e: Exception) {
                    player.sendSystemMessage(Component.literal("Error: ${e.message}"))
                }
            }
            ctx.packetHandled = true
        }
    }
}
