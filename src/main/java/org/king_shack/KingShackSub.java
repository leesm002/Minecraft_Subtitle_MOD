package org.king_shack;

import net.fabricmc.api.ClientModInitializer;
import org.king_shack.config.KSSubConfig;

public class KingShackSub implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 설정 로드 (없으면 기본 생성 후 저장)
        KSSubConfig.load();
    }
}
