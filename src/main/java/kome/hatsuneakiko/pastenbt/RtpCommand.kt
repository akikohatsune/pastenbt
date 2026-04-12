package kome.hatsuneakiko.pastenbt

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.levelgen.Heightmap
import kotlin.random.Random

object RtpCommand {
    private const val RANGE = 10000

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(Commands.literal("rtp")
            .requires { source -> source.entity is ServerPlayer && ModConfig.canUse(source.entity as ServerPlayer) }
            .executes { execute(it.source) }
        )
    }

    private fun execute(source: CommandSourceStack): Int {
        val player = source.entity as? ServerPlayer ?: return 0
        val level = player.serverLevel()
        
        source.sendSuccess({ Component.literal("§eFinding safe location...") }, false)

        val x = Random.nextInt(-RANGE, RANGE)
        val z = Random.nextInt(-RANGE, RANGE)
        
        // Tìm tọa độ Y cao nhất tại (X, Z) để hạ cánh an toàn
        val y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1

        player.teleportTo(level, x.toDouble(), y.toDouble(), z.toDouble(), player.yRot, player.xRot)
        
        source.sendSuccess({ Component.literal("§aTeleported to:§r $x, $y, $z") }, true)
        
        return 1
    }
}
