package org.king_shack.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** JSON 기반 간단 설정 */
public class KSSubConfig {
    public static final KSSubConfig INSTANCE = new KSSubConfig();

    // === 설정값 ===
    /** 자막 지속시간(ms). 기본 500ms */
    public int subtitleDurationMs = 500;

    // === 내부 ===
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "kingshacksub.json";

    private static Path configDir() {
        // Fabric은 기본적으로 .minecraft/config 를 사용
        return Path.of(System.getProperty("user.dir"), "config");
    }

    private static Path configPath() {
        return configDir().resolve(FILE_NAME);
    }

    public static void load() {
        try {
            Path dir = configDir();
            if (Files.notExists(dir)) {
                Files.createDirectories(dir);
            }
            Path file = configPath();
            if (Files.exists(file)) {
                try (var r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    KSSubConfig loaded = GSON.fromJson(r, KSSubConfig.class);
                    if (loaded != null) {
                        INSTANCE.subtitleDurationMs = loaded.subtitleDurationMs;
                    }
                }
            } else {
                // 최초 생성
                save();
            }
        } catch (IOException ignored) {
        }
    }

    public static void save() {
        try {
            Path dir = configDir();
            if (Files.notExists(dir)) {
                Files.createDirectories(dir);
            }
            try (var w = Files.newBufferedWriter(configPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(INSTANCE, w);
            }
        } catch (IOException ignored) {
        }
    }
}
