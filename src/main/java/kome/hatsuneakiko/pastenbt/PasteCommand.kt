package kome.hatsuneakiko.pastenbt

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

object PasteCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(Commands.literal("paste")
            .then(Commands.literal("grid")
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.argument("radius", IntegerArgumentType.integer(0, 5))
                        .executes { executeGrid(it, SnapMode.CORNER) }
                        .then(Commands.literal("center").executes { executeGrid(it, SnapMode.CENTER) })
                        .then(Commands.literal("corner").executes { executeGrid(it, SnapMode.CORNER) })
                    )
                )
            )
            .then(Commands.argument("name", StringArgumentType.string())
                .executes { execute(it, SnapMode.CORNER) }
                .then(Commands.literal("center").executes { execute(it, SnapMode.CENTER) })
                .then(Commands.literal("corner").executes { execute(it, SnapMode.CORNER) })
            )
        )
    }

    private fun execute(context: com.mojang.brigadier.context.CommandContext<CommandSourceStack>, snapMode: SnapMode): Int {
        val name = StringArgumentType.getString(context, "name")
        Networking.INSTANCE.sendToServer(ServerboundRequestPreviewPacket(name, snapMode))
        context.source.sendSuccess({ Component.literal("Requesting preview for: $name ($snapMode)") }, false)
        return 1
    }

    private fun executeGrid(context: com.mojang.brigadier.context.CommandContext<CommandSourceStack>, snapMode: SnapMode): Int {
        val name = StringArgumentType.getString(context, "name")
        val radius = IntegerArgumentType.getInteger(context, "radius")
        Networking.INSTANCE.sendToServer(ServerboundGridPastePacket(name, radius, snapMode))
        context.source.sendSuccess({ Component.literal("Enqueuing grid for: $name (radius $radius)") }, false)
        return 1
    }
}
