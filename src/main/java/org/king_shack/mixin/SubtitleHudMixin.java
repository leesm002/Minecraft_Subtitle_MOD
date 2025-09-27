package org.king_shack.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * - "다른 사람이 낚시할 때" 첨벙 자막은 차단
 * - 모든 자막 표시시간을 사실상 1초로 축소
 *
 * 표시시간 축소는 render() 내부의 시간 비교에서 쓰이는 현재시각을
 * (원래시각 + 2000ms) 로 바꿔, 3초 조건을 1초처럼 느껴지게 만드는 방식.
 * (버전마다 3000 상수가 인라이닝/삭제되어 ModifyConstant가 실패하는 걸 회피)
 */
@Mixin(SubtitlesHud.class)
public class SubtitleHudMixin {

    /** 실효 자막 표시시간(ms). 1000 = 1초 */
    private static final long TARGET_DURATION_MS = 1000L;
    /** 바닐라 기본 3000ms 를 1000ms 처럼 보이게 하려면 +2000ms 오프셋을 준다. */
    private static final long RENDER_TIME_OFFSET_MS = Math.max(0L, 3000L - TARGET_DURATION_MS);

    /* -------------------------------------------------
     * 타인 낚시 스플래시(보브 첨벙) 자막 차단
     * ------------------------------------------------- */
    @Inject(
            method = "onSoundPlayed(Lnet/minecraft/client/sound/SoundInstance;Lnet/minecraft/client/sound/WeightedSoundSet;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ks$onlyMyBobberSubtitles(SoundInstance sound, WeightedSoundSet soundSet, float range, CallbackInfo ci) {
        Identifier currentId = sound.getId();
        Identifier splashId = Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_FISHING_BOBBER_SPLASH);
        if (splashId != null && splashId.equals(currentId)) {
            if (!isMyBobberSplash(sound.getX(), sound.getY(), sound.getZ())) {
                // 내 낚시가 아니면 자막 등록 자체를 막음
                ci.cancel();
            }
        }
    }

    /* -------------------------------------------------
     * 자막 표시시간 1초 적용:
     * render(...) 내의 시간 기준(Util.getMeasuringTimeMs())을
     * +RENDER_TIME_OFFSET_MS 해서, 3초 조건을 1초 체감으로 바꿈.
     * ------------------------------------------------- */
    @Redirect(
            method = "render(Lnet/minecraft/client/gui/DrawContext;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J")
    )
    private long ks$renderTimeOffset() {
        // 현재 시간에 오프셋을 더해 "더 빨리 3초가 지난 것처럼" 만들기
        return Util.getMeasuringTimeMs() + RENDER_TIME_OFFSET_MS;
    }

    /* ---------------------------
     * Helper
     * --------------------------- */
    private static boolean isMyBobberSplash(double sx, double sy, double sz) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return false;

        final double r = Math.max(0.5, KSSubConfig.INSTANCE.bobberMatchRadius);
        final double v = Math.max(0.25, KSSubConfig.INSTANCE.bobberVerticalTolerance);
        final double bias = Math.max(0.0, KSSubConfig.INSTANCE.foreignBiasBlocks);

        Box box = new Box(sx - r, sy - v, sz - r, sx + r, sy + v, sz + r);

        double bestOwned = Double.POSITIVE_INFINITY;
        double bestForeign = Double.POSITIVE_INFINITY;

        for (FishingBobberEntity bobber : mc.world.getEntitiesByClass(FishingBobberEntity.class, box, e -> true)) {
            Entity owner = bobber.getOwner();
            double dx = bobber.getX() - sx;
            double dz = bobber.getZ() - sz;
            double distHoriz = Math.hypot(dx, dz);

            if (owner != null && owner.getId() == mc.player.getId()) {
                if (distHoriz < bestOwned) bestOwned = distHoriz;
            } else {
                if (distHoriz < bestForeign) bestForeign = distHoriz;
            }
        }

        if (bestOwned == Double.POSITIVE_INFINITY) return false; // 내 낚시찌 없음
        return (bestForeign == Double.POSITIVE_INFINITY) || (bestOwned <= bestForeign - bias);
    }
}
