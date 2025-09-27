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
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import org.king_shack.config.KSSubConfig;
import org.king_shack.mixin.accessor.SubtitleEntryAccessor;
import org.king_shack.mixin.accessor.SubtitlesHudAccessor;
import org.king_shack.mixin.accessor.SubtitlesHudAudibleAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

@Mixin(SubtitlesHud.class)
public class SubtitleHudMixin {

    // 내 낚시찌 Splash만 통과 (시그니처 고정)
    @Inject(
            method = "onSoundPlayed(Lnet/minecraft/client/sound/SoundInstance;Lnet/minecraft/client/sound/WeightedSoundSet;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ks$onlyMyBobberSubtitles(SoundInstance sound, WeightedSoundSet soundSet, float range, CallbackInfo ci) {
        Identifier currentId = sound.getId();
        Identifier splashId = Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_FISHING_BOBBER_SPLASH);
        if (splashId != null && splashId.equals(currentId)) {
            if (!isMyBobberSound(sound)) {
                ci.cancel();
            }
        }
    }

    // 렌더 TAIL: 유지시간 초과 정리 + 외부 스플래시 제거(후처리 안전망)
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("TAIL"))
    private void ks$tailCleanupAndForeignSplashCull(DrawContext context, CallbackInfo ci) {
        final int durationMs = Math.max(100, KSSubConfig.INSTANCE.subtitleDurationMs);
        final long now = Util.getMeasuringTimeMs();

        // 기본 엔트리
        List<?> entries = ((SubtitlesHudAccessor) (Object) this).getEntries();
        if (entries != null) {
            pruneList(entries, now, durationMs, true);
        }
        // 실제 표시 목록
        List<?> audibles = ((SubtitlesHudAudibleAccessor) (Object) this).getAudibleEntries();
        if (audibles != null) {
            pruneList(audibles, now, durationMs, false);
        }
    }

    private void pruneList(List<?> list, long now, int durationMs, boolean coarseOnly) {
        Iterator<?> it = list.iterator();
        while (it.hasNext()) {
            Object raw = it.next();
            SubtitleEntryAccessor acc = (SubtitleEntryAccessor) (Object) raw;

            long start = reflectSoundTimeFirst(acc.getSounds());
            if (start <= 0L || (now - start) > durationMs) {
                it.remove();
                continue;
            }

            // 외부 낚시 스플래시 제거 (coarseOnly가 true면 유지시간만 정리)
            if (!coarseOnly) {
                Text t = acc.getText();
                if (t.getContent() instanceof TranslatableTextContent tc) {
                    if ("subtitles.entity.fishing_bobber.splash".equals(tc.getKey())) {
                        double lx = reflectSoundXLast(acc.getSounds());
                        double ly = reflectSoundYLast(acc.getSounds());
                        double lz = reflectSoundZLast(acc.getSounds());
                        if (!isMyBobberNearPos(lx, ly, lz)) {
                            it.remove();
                        }
                    }
                }
            }
        }
    }

    // ======= Reflection helpers for SoundEntry(record x,y,z,time) =======
    private static long reflectSoundTimeFirst(List<?> sounds) {
        if (sounds == null || sounds.isEmpty()) return 0L;
        Object first = sounds.getFirst();
        return (long) invokeNoArg(first, "time", long.class, 0L);
    }
    private static double reflectSoundXLast(List<?> sounds) {
        if (sounds == null || sounds.isEmpty()) return 0.0;
        Object last = sounds.getLast();
        return (double) invokeNoArg(last, "x", double.class, 0.0);
    }
    private static double reflectSoundYLast(List<?> sounds) {
        if (sounds == null || sounds.isEmpty()) return 0.0;
        Object last = sounds.getLast();
        return (double) invokeNoArg(last, "y", double.class, 0.0);
    }
    private static double reflectSoundZLast(List<?> sounds) {
        if (sounds == null || sounds.isEmpty()) return 0.0;
        Object last = sounds.getLast();
        return (double) invokeNoArg(last, "z", double.class, 0.0);
    }
    private static Object invokeNoArg(Object target, String methodName, Class<?> ret, Object def) {
        try {
            Method m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Throwable t) {
            return def;
        }
    }
    // =========================================================

    private static boolean isMyBobberSound(SoundInstance sound) {
        return isMyBobberNearPos(sound.getX(), sound.getY(), sound.getZ());
    }

    private static boolean isMyBobberNearPos(double sx, double sy, double sz) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return false;

        double r = KSSubConfig.INSTANCE.bobberMatchRadius;
        Box box = new Box(sx - r, sy - r, sz - r, sx + r, sy + r, sz + r);

        for (FishingBobberEntity bobber :
                mc.world.getEntitiesByClass(FishingBobberEntity.class, box, e -> true)) {
            Entity owner = bobber.getOwner();
            if (owner != null && owner.getId() == mc.player.getId()) {
                return true;
            }
        }
        return false;
    }
}
