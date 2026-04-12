package kome.hatsuneakiko.pastenbt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("nbtpaster.json").toFile();
    
    public static Data INSTANCE = new Data();

    public static class Data {
        public List<String> allowedPlayers = new ArrayList<>();
        public List<String> allowedUUIDs = new ArrayList<>();
        public boolean allowOps = true;
        public int blocksPerTick = 100;
        public int maxActiveTasks = 2;
        public int maxBlocksPerTask = 200000;
        public boolean skipAir = true;
    }

    public static boolean load() {
        if (!CONFIG_FILE.exists()) {
            save();
            return true;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data != null) INSTANCE = data;
            return true;
        } catch (Exception e) {
            PasteNbtMod.LOGGER.error("Failed to load config", e);
            return false;
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            PasteNbtMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static boolean canUse(ServerPlayer player) {
        if (INSTANCE.allowOps && player.hasPermissions(2)) return true;
        if (INSTANCE.allowedPlayers.contains(player.getGameProfile().getName())) return true;
        return INSTANCE.allowedUUIDs.contains(player.getUUID().toString());
    }
}
