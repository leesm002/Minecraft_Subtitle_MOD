package org.king_shack.mixin.accessor;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

// 비공개 inner라 심볼을 직접 못 씀 → 문자열 타겟
@Mixin(targets = "net.minecraft.client.gui.hud.SubtitlesHud$SubtitleEntry")
public interface SubtitleEntryAccessor {
    @Accessor("sounds")
    List<?> getSounds(); // List<SoundEntry> 이지만 비공개라 와일드카드

    @Accessor("text")
    Text getText();

    @Accessor("range")
    float getRange();
}
