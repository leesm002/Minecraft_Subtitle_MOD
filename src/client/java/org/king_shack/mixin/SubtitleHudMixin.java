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

@Mixin(SubtitlesHud.class)
public class SubtitleHudMixin {

    @Shadow @Final private List<?> entries;

    @Inject(method = "onSoundPlayed", at = @At("HEAD"), cancellable = true)

    private void ks$onlyMyBobberSubtitles(SoundInstance sound, WeightedSoundSet soundSet, float range, CallbackInfo ci) {
        Identifier current = sound.getId();
        Identifier splash = Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_FISHING_BOBBER_SPLASH);

        if (splash != null && splash.equals(current)) {
            if (!isMyBobberSound(sound)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void ks$applyCustomDuration(CallbackInfo ci) {
        final int durationMs = Math.max(100, KSSubConfig.INSTANCE.subtitleDurationMs);
        final long now = Util.getMeasuringTimeMs();

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
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        }
    }

    private static boolean isMyBobberSound(SoundInstance sound) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return false;

        double sx = sound.getX();
        double sy = sound.getY();
        double sz = sound.getZ();

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
