package kome.hatsuneakiko.pastenbt;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.Random;

public class RtpCommand {
    private static final int RANGE = 10000;
    private static final Random RANDOM = new Random();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rtp")
            .requires(source -> source.getEntity() instanceof ServerPlayer && ModConfig.canUse((ServerPlayer) source.getEntity()))
            .executes(ctx -> execute(ctx.getSource()))
        );
    }

    private static int execute(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer)) return 0;
        ServerPlayer player = (ServerPlayer) source.getEntity();
        ServerLevel level = player.serverLevel();

        source.sendSuccess(() -> Component.literal("§eFinding safe location..."), false);

        int x = RANDOM.nextInt(RANGE * 2) - RANGE;
        int z = RANDOM.nextInt(RANGE * 2) - RANGE;
        
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;

        player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
        
        source.sendSuccess(() -> Component.literal("§aTeleported to:§r " + x + ", " + y + ", " + z), true);
        return 1;
    }
}
