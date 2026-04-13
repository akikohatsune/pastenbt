package kome.hatsuneakiko.pastenbt;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.*;

public class PlacementManager {
    private static final List<PlacementTask> WAITING_QUEUE = new ArrayList<>();
    private static final List<PlacementTask> ACTIVE_TASKS = new ArrayList<>();

    public static class PlacementTask {
        private final UUID playerUUID;
        private final ServerLevel level;
        private final Map<ChunkPos, List<StructureTemplate.StructureBlockInfo>> chunkGroups = new LinkedHashMap<>();
        private final List<ChunkPos> chunkOrder = new ArrayList<>();
        private final int totalBlocks;
        private final BlockPos origin;
        private int blocksPlaced = 0;
        private int lastMessageTick = 0;

        public PlacementTask(UUID playerUUID, ServerLevel level, List<StructureTemplate.StructureBlockInfo> blocks) {
            this.playerUUID = playerUUID;
            this.level = level;
            this.origin = !blocks.isEmpty() ? blocks.get(0).pos() : BlockPos.ZERO;
            
            int count = 0;
            for (StructureTemplate.StructureBlockInfo info : blocks) {
                if (ModConfig.INSTANCE.skipAir && info.state().isAir()) continue;
                
                ChunkPos cp = new ChunkPos(info.pos());
                chunkGroups.computeIfAbsent(cp, k -> {
                    chunkOrder.add(k);
                    return new ArrayList<>();
                }).add(info);
                count++;
            }
            this.totalBlocks = count;
        }

        public boolean tick() {
            if (chunkOrder.isEmpty()) return true;

            int limit = ModConfig.INSTANCE.blocksPerTick;
            int placedInTick = 0;

            while (placedInTick < limit && !chunkOrder.isEmpty()) {
                ChunkPos currentChunk = chunkOrder.get(0);
                List<StructureTemplate.StructureBlockInfo> blocks = chunkGroups.get(currentChunk);

                while (placedInTick < limit && !blocks.isEmpty()) {
                    StructureTemplate.StructureBlockInfo info = blocks.remove(0);
                    
                    BlockState currentState = level.getBlockState(info.pos());
                    if (currentState != info.state()) {
                        level.setBlock(info.pos(), info.state(), 3);
                        if (info.nbt() != null) {
                            BlockEntity be = level.getBlockEntity(info.pos());
                            if (be != null) be.load(info.nbt());
                        }
                    }
                    
                    placedInTick++;
                    blocksPlaced++;
                }

                if (blocks.isEmpty()) {
                    chunkGroups.remove(currentChunk);
                    chunkOrder.remove(0);
                }
            }

            lastMessageTick++;
            if (lastMessageTick >= 20 || chunkOrder.isEmpty()) {
                lastMessageTick = 0;
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUUID);
                if (player != null) {
                    int percent = totalBlocks == 0 ? 100 : (int) ((double) blocksPlaced / totalBlocks * 100);
                    player.sendSystemMessage(Component.literal("§b[Java 17]§r Placement: " + percent + "% (" + blocksPlaced + "/" + totalBlocks + ")"), true);
                }
            }

            return chunkOrder.isEmpty();
        }

        public boolean canStart() {
            return totalBlocks <= ModConfig.INSTANCE.maxBlocksPerTask;
        }
    }

    public static void add(PlacementTask task) {
        if (task.canStart()) {
            WAITING_QUEUE.add(task);
        } else {
            PasteNbtMod.LOGGER.warn("Rejected task with " + task.totalBlocks + " blocks (Limit: " + ModConfig.INSTANCE.maxBlocksPerTask + ")");
        }
    }

    public static void processTick() {
        while (ACTIVE_TASKS.size() < ModConfig.INSTANCE.maxActiveTasks && !WAITING_QUEUE.isEmpty()) {
            ACTIVE_TASKS.add(WAITING_QUEUE.remove(0));
        }

        if (ACTIVE_TASKS.isEmpty()) return;

        Iterator<PlacementTask> it = ACTIVE_TASKS.iterator();
        while (it.hasNext()) {
            if (it.next().tick()) {
                it.remove();
            }
        }
    }
}
