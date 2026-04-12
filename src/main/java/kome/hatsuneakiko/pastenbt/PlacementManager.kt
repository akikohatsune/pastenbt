package kome.hatsuneakiko.pastenbt

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo
import java.util.*

object PlacementManager {
    private val WAITING_QUEUE = mutableListOf<PlacementTask>()
    private val ACTIVE_TASKS = mutableListOf<PlacementTask>()

    class PlacementTask(
        val playerUUID: UUID,
        val level: ServerLevel,
        blocks: List<StructureBlockInfo>
    ) {
        private val chunkGroups = mutableMapOf<ChunkPos, MutableList<StructureBlockInfo>>()
        private val chunkOrder = mutableListOf<ChunkPos>()
        private val totalBlocks: Int
        private val origin: BlockPos
        private var blocksPlaced = 0
        private var lastMessageTick = 0

        init {
            origin = blocks.firstOrNull()?.pos ?: BlockPos.ZERO
            var count = 0
            blocks.filter { !ModConfig.INSTANCE.skipAir || !it.state.isAir }.forEach { info ->
                val cp = ChunkPos(info.pos)
                chunkGroups.getOrPut(cp) {
                    chunkOrder.add(cp)
                    mutableListOf()
                }.add(info)
                count++
            }
            totalBlocks = count
        }

        fun tick(): Boolean {
            if (chunkOrder.isEmpty()) return true
            
            val limit = ModConfig.INSTANCE.blocksPerTick
            var placedInTick = 0

            while (placedInTick < limit && chunkOrder.isNotEmpty()) {
                val currentChunk = chunkOrder.first()
                val blocks = chunkGroups[currentChunk] ?: mutableListOf()

                while (placedInTick < limit && blocks.isNotEmpty()) {
                    val info = blocks.removeAt(0)
                    if (level.getBlockState(info.pos) != info.state) {
                        level.setBlock(info.pos, info.state, 2 or 16)
                        info.nbt?.let { level.getBlockEntity(info.pos)?.load(it) }
                    }
                    placedInTick++
                    blocksPlaced++
                }

                if (blocks.isEmpty()) {
                    chunkGroups.remove(currentChunk)
                    chunkOrder.removeAt(0)
                }
            }

            if (++lastMessageTick >= 20 || chunkOrder.isEmpty()) {
                lastMessageTick = 0
                level.server.playerList.getPlayer(playerUUID)?.let { player ->
                    val percent = if (totalBlocks == 0) 100 else (blocksPlaced * 100 / totalBlocks)
                    player.sendSystemMessage(Component.literal("§b[Kotlin]§r Placement: $percent% ($blocksPlaced/$totalBlocks)"), true)
                }
            }
            return chunkOrder.isEmpty()
        }

        fun canStart(): Boolean = totalBlocks <= ModConfig.INSTANCE.maxBlocksPerTask
    }

    fun add(task: PlacementTask) {
        if (task.canStart()) WAITING_QUEUE.add(task)
        else println("Rejected task: too many blocks")
    }

    fun processTick() {
        while (ACTIVE_TASKS.size < ModConfig.INSTANCE.maxActiveTasks && WAITING_QUEUE.isNotEmpty()) {
            ACTIVE_TASKS.add(WAITING_QUEUE.removeAt(0))
        }

        val iterator = ACTIVE_TASKS.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().tick()) iterator.remove()
        }
    }
}
