package org.king_shack.mixin.accessor;

import net.minecraft.client.gui.hud.SubtitlesHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(SubtitlesHud.class)
public interface SubtitlesHudAudibleAccessor {
    @Accessor("audibleEntries")
    List<?> getAudibleEntries(); // inner 타입이 비공개라 와일드카드로 받음
}
