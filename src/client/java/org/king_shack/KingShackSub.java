package org.king_shack;

import net.fabricmc.api.ClientModInitializer;
import org.king_shack.config.KSSubConfig;

public class KingShackSub implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KSSubConfig.load();
    }
}
