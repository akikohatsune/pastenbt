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
            .then(Commands.literal("ls")
                .executes { listStructures(it, "") }
                .then(Commands.argument("folder", StringArgumentType.string())
                    .executes { listStructures(it, StringArgumentType.getString(it, "folder")) })
            )
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

    private fun listStructures(context: com.mojang.brigadier.context.CommandContext<CommandSourceStack>, folder: String): Int {
        val source = context.source
        val structuresDir = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("structures").toFile()
        val targetDir = java.io.File(structuresDir, folder).canonicalFile

        if (!targetDir.path.startsWith(structuresDir.canonicalPath)) {
            source.sendFailure(Component.literal("Access denied."))
            return 0
        }

        if (!targetDir.exists() || !targetDir.isDirectory) {
            source.sendFailure(Component.literal("Folder not found: $folder"))
            return 0
        }

        val files = targetDir.listFiles() ?: emptyArray()
        source.sendSuccess({ Component.literal("§6--- Structures in /$folder ---") }, false)

        files.sortedBy { it.name }.forEach { file ->
            val prefix = if (file.isDirectory) "§e[DIR] §r" else "§b[FILE] §r"
            val displayName = if (file.name.endsWith(".nbt")) file.name.removeSuffix(".nbt") else file.name
            val relPath = if (folder.isEmpty()) displayName else "$folder/$displayName"
            
            val message = Component.literal(prefix)
                .append(Component.literal(displayName).withStyle { s -> 
                    s.withColor(if (file.isDirectory) 0xFFFF55 else 0x55FFFF)
                     .withClickEvent(net.minecraft.network.chat.ClickEvent(
                         if (file.isDirectory) net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND 
                         else net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND,
                         if (file.isDirectory) "/paste ls $relPath" else "/paste $relPath"
                     ))
                     .withHoverEvent(net.minecraft.network.chat.HoverEvent(
                         net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                         Component.literal(if (file.isDirectory) "Click to open folder" else "Click to suggest paste command")
                     ))
                })
            
            source.sendSuccess({ message }, false)
        }

        return 1
    }
}
