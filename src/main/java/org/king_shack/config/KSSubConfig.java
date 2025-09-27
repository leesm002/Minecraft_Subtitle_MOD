package org.king_shack.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 게임 중에 참조되는 간단한 설정 싱글톤.
 * - 파일 위치: <minecraft-dir>/config/king_shack_sub.json
 * - 존재하지 않으면 기본값으로 생성
 */
public enum KSSubConfig {
    INSTANCE;

    // ======= 설정 값들 (기본값 포함) =======
    /** 자막 유지시간(ms). 예: 300 => 0.3초 */
    public int subtitleDurationMs = 1000;

    /** 내 찌와 사운드 위치의 수평 허용 반경(블록) */
    public double bobberMatchRadius = 1.50;

    /** 내 찌와 사운드 위치의 수직 허용 오차(블록) */
    public double bobberVerticalTolerance = 2.00;

    /**
     * 외부(다른 플레이어) 찌가 내 찌보다 이 값(블록) 이상 더 가깝다면
     * 해당 스플래시를 “남의 것”으로 판단해 자막을 거부.
     * 값이 클수록 남의 것을 더 공격적으로 차단.
     */
    public double foreignBiasBlocks = 0.75;

    private static final String FILE_NAME = "king_shack_sub.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    /** 시작 시 한 번 호출 (없으면 생성, 있으면 로드) */
    public static void load() {
        Path path = configPath();
        if (Files.notExists(path)) {
            // 최초 생성
            save();
            return;
        }
        try (BufferedReader r = Files.newBufferedReader(path)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d == null) return;

            // 누락 필드는 기본값 유지
            if (d.subtitleDurationMs != null && d.subtitleDurationMs > 0) {
                INSTANCE.subtitleDurationMs = d.subtitleDurationMs;
            }
            if (d.bobberMatchRadius != null && d.bobberMatchRadius > 0.0) {
                INSTANCE.bobberMatchRadius = d.bobberMatchRadius;
            }
            if (d.bobberVerticalTolerance != null && d.bobberVerticalTolerance > 0.0) {
                INSTANCE.bobberVerticalTolerance = d.bobberVerticalTolerance;
            }
            if (d.foreignBiasBlocks != null && d.foreignBiasBlocks > 0.0) {
                INSTANCE.foreignBiasBlocks = d.foreignBiasBlocks;
            }
        } catch (IOException ignored) {
        }
    }

    /** 수동 저장(옵션 스크린 등에서 변경했을 때 호출할 수 있음) */
    public static void save() {
        Path path = configPath();
        Data d = new Data();
        d.subtitleDurationMs = INSTANCE.subtitleDurationMs;
        d.bobberMatchRadius = INSTANCE.bobberMatchRadius;
        d.bobberVerticalTolerance = INSTANCE.bobberVerticalTolerance;
        d.foreignBiasBlocks = INSTANCE.foreignBiasBlocks;

        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(path)) {
                GSON.toJson(d, w);
            }
        } catch (IOException ignored) {
        }
    }

    /** JSON 직렬화/역직렬화용 단순 DTO */
    private static class Data {
        Integer subtitleDurationMs;
        Double bobberMatchRadius;
        Double bobberVerticalTolerance;
        Double foreignBiasBlocks;
    }
}
