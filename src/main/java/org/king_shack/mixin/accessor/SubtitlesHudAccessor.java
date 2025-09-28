package org.king_shack.mixin.accessor;

import net.minecraft.client.gui.hud.SubtitlesHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** SubtitlesHud.entries 에 접근하기 위한 액세서 */
@Mixin(SubtitlesHud.class)
public interface SubtitlesHudAccessor {
    @Accessor("entries")
    List<?> getEntries();
}
