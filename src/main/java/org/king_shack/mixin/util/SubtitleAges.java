package org.king_shack.mixin.util;

import net.minecraft.util.Util;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** SubtitleEntry 인스턴스 생성 시각을 약하게 추적 */
public final class SubtitleAges {
    private static final Map<Object, Long> CREATED_AT =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SubtitleAges() {}

    public static long now() {
        return Util.getMeasuringTimeMs();
    }

    public static void markCreated(Object entry) {
        CREATED_AT.put(entry, now());
    }

    public static long getCreatedAt(Object entry) {
        Long v = CREATED_AT.get(entry);
        return v == null ? -1L : v;
    }

    public static void forget(Object entry) {
        CREATED_AT.remove(entry);
    }
}
