package kome.hatsuneakiko.pastenbt

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.fml.loading.FMLPaths
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object NbtPasterCommand {
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024L
    private const val TIMEOUT = 10000

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(Commands.literal("nbtpaster")
            .requires { source -> source.entity is ServerPlayer && ModConfig.canUse(source.entity as ServerPlayer) }
            .then(Commands.literal("reload").executes { 
                if (ModConfig.load()) {
                    it.source.sendSuccess({ Component.literal("NbtPaster config reloaded.") }, true)
                    1
                } else {
                    it.source.sendFailure(Component.literal("Failed to reload config."))
                    0
                }
            })
            .then(Commands.literal("get")
                .then(Commands.argument("url", StringArgumentType.string())
                    .executes { download(it, null) }
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes { download(it, StringArgumentType.getString(it, "name")) })
                )
            )
        )
    }

    private fun download(context: CommandContext<CommandSourceStack>, customName: String?): Int {
        val urlString = StringArgumentType.getString(context, "url")
        val source = context.source

        if (!urlString.lowercase().endsWith(".nbt")) {
            source.sendFailure(Component.literal("URL must end with .nbt"))
            return 0
        }

        Thread {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = TIMEOUT
                connection.readTimeout = TIMEOUT
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    source.sendFailure(Component.literal("HTTP error: ${connection.responseCode}"))
                    return@Thread
                }

                if (connection.contentLengthLong > MAX_FILE_SIZE) {
                    source.sendFailure(Component.literal("File too large (>5MB)"))
                    return@Thread
                }

                val fileName = sanitizeFileName(customName ?: getFileNameFromUrl(urlString)).let {
                    if (it.endsWith(".nbt")) it else "$it.nbt"
                }

                val targetDir = FMLPaths.CONFIGDIR.get().resolve("structures/downloads/${UUID.randomUUID()}").toFile()
                targetDir.mkdirs()
                val targetFile = File(targetDir, fileName)

                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                source.sendSuccess({ Component.literal("§a[Java 17 Async]§r Saved to downloads/${targetDir.name}/$fileName") }, true)
            } catch (e: Exception) {
                source.sendFailure(Component.literal("Download failed: ${e.message}"))
            }
        }.start()

        source.sendSuccess({ Component.literal("Starting download...") }, false)
        return 1
    }

    private fun getFileNameFromUrl(url: String) = url.substringAfterLast('/').ifEmpty { "structure.nbt" }
    private fun sanitizeFileName(name: String) = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
}
