package kome.hatsuneakiko.pastenbt

import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import net.minecraftforge.fml.loading.FMLPaths
import net.minecraftforge.network.NetworkEvent
import java.io.FileInputStream
import java.util.function.Supplier

class ServerboundPastePacket(
    val name: String,
    val rotation: Rotation,
    val snapMode: SnapMode
) {
    companion object {
        @JvmStatic
        fun encode(msg: ServerboundPastePacket, buf: FriendlyByteBuf) {
            buf.writeUtf(msg.name)
            buf.writeEnum(msg.rotation)
            buf.writeEnum(msg.snapMode)
        }

        @JvmStatic
        fun decode(buf: FriendlyByteBuf): ServerboundPastePacket {
            return ServerboundPastePacket(buf.readUtf(), buf.readEnum(Rotation::class.java), buf.readEnum(SnapMode::class.java))
        }

        @JvmStatic
        fun handle(msg: ServerboundPastePacket, ctxGetter: Supplier<NetworkEvent.Context>) {
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
                        val template = StructureTemplate()
                        template.load(player.serverLevel().holderLookup(Registries.BLOCK), nbt)

                        val origin = msg.snapMode.snap(player.blockPosition())
                        val settings = StructurePlaceSettings().setRotation(msg.rotation)
                        val blocks = template.filterBlocks(origin, settings, null)

                        ctx.enqueueWork {
                            PlacementManager.add(PlacementManager.PlacementTask(player.uuid, player.serverLevel(), blocks))
                            player.sendSystemMessage(Component.literal("§b[Kotlin Coroutine]§r Prepared: ${msg.name}"))
                        }
                    }
                } catch (e: Exception) {
                    player.sendSystemMessage(Component.literal("Error: ${e.message}"))
                }
            }
            ctx.packetHandled = true
        }
    }
}
