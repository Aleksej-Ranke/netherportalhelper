package de.alek.netherportalhelper.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("netherportalhelper.json");

    private static ModConfig config = new ModConfig();

    private ConfigManager() {
    }

    public static ModConfig get() {
        return config;
    }

    public static void load(Logger logger) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
        } catch (IOException ioException) {
            logger.error("Konnte Config-Verzeichnis nicht erstellen: {}", CONFIG_PATH.getParent(), ioException);
        }

        if (!Files.exists(CONFIG_PATH)) {
            config = new ModConfig();
            config.normalize();
            save(logger);
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ModConfig parsed = GSON.fromJson(reader, ModConfig.class);
            if (parsed == null) {
                config = new ModConfig();
            } else {
                config = parsed;
            }
            config.normalize();
        } catch (IOException | JsonParseException exception) {
            logger.error("Config konnte nicht geladen werden, Defaults werden verwendet: {}", CONFIG_PATH, exception);
            config = new ModConfig();
            config.normalize();
        }
    }

    public static void save(Logger logger) {
        config.normalize();
        try (Writer writer = Files.newBufferedWriter(
                CONFIG_PATH,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            GSON.toJson(config, writer);
        } catch (IOException exception) {
            logger.error("Config konnte nicht gespeichert werden: {}", CONFIG_PATH, exception);
        }
    }
}
