package de.sterni.voidtrading.customtrades;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static de.sterni.voidtrading.VoidTrading.LOGGER;

public class FileManager {

    public static final String dir = Path.of("config", "customtrades").toString();

    public static void saveToFile(@NotNull String fileName, @NotNull JsonElement line) {
        String path = Path.of(dir, fileName).toString();
        ensurePathExists(path);
        File file = new File(path);
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath())) {
            String saveThis = new GsonBuilder().setPrettyPrinting().create().toJson(line);
            writer.write(saveThis);
        } catch (IOException ex) {
            LOGGER.warn("Could not save {}! Reason: {}", fileName, ex.getMessage());
        }
    }

    public static JsonElement loadListFromFile(@NotNull String listFilePath, @NotNull JsonElement defaultContent) {
        LOGGER.info("Loading list file...");
        String path = Path.of(dir, listFilePath).toString();
        File file = new File(path);
        ensurePathExists(path);
        ensureFileIsNotEmpty(file, defaultContent);
        JsonElement result = null;
        try {
            result = JsonParser.parseString(Files.readString(file.toPath()));
        } catch (IOException ex) {
            LOGGER.error("Could not load {}, Reason: {}", listFilePath, ex.getMessage());
        }
        LOGGER.info("Successfully loaded \"{}\"!", file.getName());
        return result;
    }

    private static void ensureFileIsNotEmpty(@NotNull File file, @NotNull JsonElement defaultContent) {
        if (file.length() == 0) {
            saveToFile(file.getName(), defaultContent);
        }
    }

    private static void ensurePathExists(@NotNull String listFilePath) {
        if (!Files.exists(Path.of(listFilePath))) {
            File file = new File(listFilePath);
            try {
                //noinspection ResultOfMethodCallIgnored
                file.getParentFile().mkdirs();// createNewFile will through exception if this failed
                if (file.createNewFile()) {
                    LOGGER.info("Created new {} in \"<Server directory>{}{}/\"", file.getName(), File.separator, dir);
                }
            } catch (IOException ex) {
                LOGGER.warn("Could not create {}, reason: {}", file.getName(), ex.getMessage());
            }
        }
    }
}
