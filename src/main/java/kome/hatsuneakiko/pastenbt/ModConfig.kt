package kome.hatsuneakiko.pastenbt

import com.google.gson.GsonBuilder
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.fml.loading.FMLPaths
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object ModConfig {
    private val GSON = GsonBuilder().setPrettyPrinting().create()
    private val CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("nbtpaster.json").toFile()
    
    var INSTANCE = Data()

    class Data {
        val allowedPlayers = mutableListOf<String>()
        val allowedUUIDs = mutableListOf<String>()
        var allowOps = true
        var blocksPerTick = 100
        var maxActiveTasks = 2
        var maxBlocksPerTask = 200000
        var skipAir = true
    }

    fun load(): Boolean {
        if (!CONFIG_FILE.exists()) {
            save()
            return true
        }
        return try {
            FileReader(CONFIG_FILE).use { reader ->
                INSTANCE = GSON.fromJson(reader, Data::class.java) ?: Data()
            }
            true
        } catch (e: Exception) {
            PasteNbtMod.LOGGER.error("Failed to load config", e)
            false
        }
    }

    fun save() {
        try {
            FileWriter(CONFIG_FILE).use { writer ->
                GSON.toJson(INSTANCE, writer)
            }
        } catch (e: Exception) {
            PasteNbtMod.LOGGER.error("Failed to save config", e)
        }
    }

    fun canUse(player: ServerPlayer): Boolean {
        if (INSTANCE.allowOps && player.hasPermissions(2)) return true
        if (INSTANCE.allowedPlayers.contains(player.gameProfile.name)) return true
        return INSTANCE.allowedUUIDs.contains(player.uuid.toString())
    }
}
