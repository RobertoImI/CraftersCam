package org.crafterscr.crafterscam.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.crafterscr.crafterscam.CraftersCam;
import org.crafterscr.crafterscam.camera.CamVisualSettings;
import org.crafterscr.crafterscam.camera.CameraPoint;
import org.crafterscr.crafterscam.camera.CameraSequence;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class CamStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Path loadedFile;
    private CamData data = new CamData();

    public CamData data(MinecraftServer server) {
        loadIfNeeded(server);
        return data;
    }

    public void loadIfNeeded(MinecraftServer server) {
        Path file = getFile(server);

        if (loadedFile != null && loadedFile.equals(file)) {
            return;
        }

        loadedFile = file;

        try {
            Files.createDirectories(file.getParent());

            if (!Files.exists(file)) {
                data = new CamData();
                save(server);
                return;
            }

            try (Reader reader = Files.newBufferedReader(file)) {
                CamData loaded = GSON.fromJson(reader, CamData.class);
                data = loaded == null ? new CamData() : loaded;
                data.fixNulls();
            }
        } catch (Exception e) {
            CraftersCam.LOGGER.error("[CraftersCam] No se pudo cargar cameras.json", e);
            data = new CamData();
        }
    }

    public void save(MinecraftServer server) {
        Path file = getFile(server);

        try {
            Files.createDirectories(file.getParent());

            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            CraftersCam.LOGGER.error("[CraftersCam] No se pudo guardar cameras.json", e);
        }
    }

    private Path getFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("crafterscam")
                .resolve("cameras.json");
    }

    public static class CamData {
        public Map<String, CameraPoint> points = new LinkedHashMap<>();
        public Map<String, CameraSequence> sequences = new LinkedHashMap<>();
        public CamVisualSettings settings = new CamVisualSettings();

        public void fixNulls() {
            if (points == null) {
                points = new LinkedHashMap<>();
            }

            if (sequences == null) {
                sequences = new LinkedHashMap<>();
            }

            if (settings == null) {
                settings = new CamVisualSettings();
            }

            settings.fadeInTicks = Math.max(0, settings.fadeInTicks);
            settings.fadeOutTicks = Math.max(0, settings.fadeOutTicks);
        }
    }
}