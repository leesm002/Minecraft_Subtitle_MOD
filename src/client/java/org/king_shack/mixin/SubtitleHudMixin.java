package org.king_shack.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.SubtitlesHud;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import org.king_shack.config.KSSubConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

/**
 * 1) 낚시찌 첨벙 소리는 '내 보버'일 때만 자막 생성
 * 2) 자막 지속시간(기본 500ms, JSON 설정 적용)에 맞춰 tick()에서 오래된 항목 제거
 */
@Mixin(SubtitlesHud.class)
public class SubtitleHudMixin {

    // SubtitlesHud의 자막 엔트리 리스트 (내부 타입이라 와일드카드)
    @Shadow @Final private List<?> entries;

    // 1) 1.21.4 실제 시그니처: SoundInstance 단독
    @Inject(
            method = "onSoundPlayed(Lnet/minecraft/client/sound/SoundInstance;Lnet/minecraft/client/sound/WeightedSoundSet;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ks$onlyMyBobberSubtitles(SoundInstance sound, WeightedSoundSet soundSet, float range, CallbackInfo ci) {
        Identifier current = sound.getId();
        Identifier splash = Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_FISHING_BOBBER_SPLASH);
        if (splash != null && splash.equals(current)) {
            if (!isMyBobberSound(sound)) {
                ci.cancel();
            }
        }
    }


    // 2) tick 끝에서 커스텀 지속시간 적용
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void ks$applyCustomDuration(CallbackInfo ci) {
        final int durationMs = Math.max(100, KSSubConfig.INSTANCE.subtitleDurationMs); // 최소 100ms
        final long now = Util.getMeasuringTimeMs();

        // 내부 클래스(SubtitleEntry)의 startTime을 리플렉션으로 읽어 필터링
        Iterator<?> it = entries.iterator();
        while (it.hasNext()) {
            Object entry = it.next();
            try {
                Field f = entry.getClass().getDeclaredField("startTime");
                f.setAccessible(true);
                long start = f.getLong(entry);
                if ((now - start) > durationMs) {
                    it.remove();
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                // 구조 변경 시에도 크래시 방지
            }
        }
    }

    // 소리 좌표 주변의 낚시찌가 '내 것'인지 판정
    private static boolean isMyBobberSound(SoundInstance sound) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return false;

        double sx = sound.getX();
        double sy = sound.getY();
        double sz = sound.getZ();

        // 1.5블록 큐브 범위
        Box box = new Box(sx - 1.5, sy - 1.5, sz - 1.5, sx + 1.5, sy + 1.5, sz + 1.5);

        for (FishingBobberEntity bobber : mc.world.getEntitiesByClass(FishingBobberEntity.class, box, e -> true)) {
            Entity owner = bobber.getOwner();
            if (owner != null && owner.getId() == mc.player.getId()) {
                return true;
            }
        }
        return false;
    }
}
