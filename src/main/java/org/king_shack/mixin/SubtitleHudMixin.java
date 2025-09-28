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
import net.minecraft.util.math.Box;
import org.king_shack.config.KSSubConfig;
import org.king_shack.mixin.accessor.SubtitlesHudAccessor;
import org.king_shack.duck.KSEntryExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

@Mixin(SubtitlesHud.class)
public class SubtitleHudMixin {

    /** 전 자막 지속시간(밀리초) – 1000ms = 1초로 강제 */
    private static final long GLOBAL_SUBTITLE_MS = 1000L;

    /* ---------------------------
     * 타인 낚시 스플래시 차단
     * --------------------------- */
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
                ci.cancel(); // 내 낚시가 아니면 등록 자체를 막음
            }
        }
    }

    /* ---------------------------------------------------
     * render(...) 끝에서 1초 지난 엔트리를 즉시 제거 (페이드 제거)
     * --------------------------------------------------- */
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V",
            at = @At("TAIL"))
    private void ks$trimExpiredAtTail(DrawContext ctx, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        // SubtitlesHud.entries 접근
        List<?> entries = ((SubtitlesHudAccessor) (Object) this).getEntries();

        // 1) 생성시간 없는 엔트리(구버전/외부 모드가 만든 것) → 지금 시각으로 초기화
        for (Object e : entries) {
            if (e instanceof KSEntryExt ext && ext.ks$getStartAt() == 0L) {
                ext.ks$setStartAt(now);
            }
        }

        // 2) 1초 지난 것들 즉시 제거
        Iterator<?> it = entries.iterator();
        while (it.hasNext()) {
            Object e = it.next();
            if (e instanceof KSEntryExt ext) {
                if (now - ext.ks$getStartAt() >= GLOBAL_SUBTITLE_MS) {
                    it.remove();
                }
            }
        }
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

        for (FishingBobberEntity bobber :
                mc.world.getEntitiesByClass(FishingBobberEntity.class, box, e -> true)) {
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
