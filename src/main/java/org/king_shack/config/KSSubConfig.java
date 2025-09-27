package org.king_shack.config;

public class KSSubConfig {
    public static final KSSubConfig INSTANCE = new KSSubConfig();

    // 전역 자막 유지시간 (ms)
    public int subtitleDurationMs = 500; // 0.5s

    // 내 낚시찌 판정 반경 (blocks)
    public double bobberMatchRadius = 6.0; // 6 blocks

    private KSSubConfig() {
        // 기본값은 여기에서 초기화됨
    }

    // KingShackSub에서 호출하는 진입점 (현재는 기본값/향후 파일 로드 자리)
    public static void load() {
        // TODO: 필요하면 JSON 등에서 값을 읽어와 INSTANCE 필드에 반영
        // 지금은 기본값 사용(노옵)
    }
}
