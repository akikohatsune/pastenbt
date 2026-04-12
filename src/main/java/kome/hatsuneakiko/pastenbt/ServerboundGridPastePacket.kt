package kome.hatsuneakiko.pastenbt

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
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

class ServerboundGridPastePacket(val name: String, val radius: Int, val snapMode: SnapMode) {
    companion object {
        @JvmStatic
        fun encode(msg: ServerboundGridPastePacket, buf: FriendlyByteBuf) {
            buf.writeUtf(msg.name)
            buf.writeInt(msg.radius)
            buf.writeEnum(msg.snapMode)
        }

        @JvmStatic
        fun decode(buf: FriendlyByteBuf): ServerboundGridPastePacket {
            return ServerboundGridPastePacket(buf.readUtf(), buf.readInt(), buf.readEnum(SnapMode::class.java))
        }

        @JvmStatic
        fun handle(msg: ServerboundGridPastePacket, ctxGetter: Supplier<NetworkEvent.Context>) {
            val ctx = ctxGetter.get()
            val player = ctx.sender ?: return
            if (!ModConfig.canUse(player)) return

            Thread {
                try {
                    val nbtFile = FMLPaths.GAMEDIR.get().resolve("structures/${msg.name}.nbt").toFile()
                    if (!nbtFile.exists()) {
                        player.sendSystemMessage(Component.literal("File not found: ${msg.name}"))
                        return@Thread
                    }
                    FileInputStream(nbtFile).use { fis ->
                        val nbt = NbtIo.readCompressed(fis)
                        val template = StructureTemplate()
                        template.load(player.serverLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK), nbt)

                        val playerChunkX = player.blockPosition().x shr 4
                        val playerChunkZ = player.blockPosition().z shr 4
                        
                        ctx.enqueueWork {
                            for (x in -msg.radius..msg.radius) {
                                for (z in -msg.radius..msg.radius) {
                                    val origin = msg.snapMode.snap(BlockPos((playerChunkX + x) shl 4, player.blockPosition().y, (playerChunkZ + z) shl 4))
                                    val blocks = template.filterBlocks(origin, StructurePlaceSettings().setRotation(Rotation.NONE), null)
                                    PlacementManager.add(PlacementManager.PlacementTask(player.uuid, player.serverLevel(), blocks))
                                }
                            }
                            player.sendSystemMessage(Component.literal("§b[Kotlin]§r Grid enqueued: ${(msg.radius * 2 + 1) * (msg.radius * 2 + 1)} tasks."))
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
