package org.king_shack.mixin;

import org.king_shack.duck.KSEntryExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * net.minecraft.client.gui.hud.SubtitlesHud$SubtitleEntry 에 섞어서
 * 생성시각(ks$startAt)을 기록.
 */
@Mixin(targets = "net.minecraft.client.gui.hud.SubtitlesHud$SubtitleEntry")
public abstract class SubtitleEntryInitMixin implements KSEntryExt {

    @Unique
    private long ks$startAt;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ks$initStamp(CallbackInfo ci) {
        this.ks$startAt = System.currentTimeMillis();
    }

    @Override
    public long ks$getStartAt() {
        return ks$startAt;
    }

    @Override
    public void ks$setStartAt(long timeMillis) {
        this.ks$startAt = timeMillis;
    }
}
