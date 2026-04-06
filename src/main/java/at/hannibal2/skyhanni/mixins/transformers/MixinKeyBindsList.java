package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.utils.SkyHanniKeyBindManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;

@Mixin(KeyBindsList.class)
public class MixinKeyBindsList {

    @ModifyVariable(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Ljava/util/Arrays;sort([Ljava/lang/Object;)V", shift = At.Shift.AFTER),
        name = "keyMappings"
    )
    private KeyMapping[] injectSkyHanniKeybinds(KeyMapping[] keyMappings) {
        KeyMapping[] active = SkyHanniKeyBindManager.INSTANCE.getActiveKeyMappings();
        if (active.length == 0) return keyMappings;
        KeyMapping[] combined = ArrayUtils.addAll(keyMappings, active);
        Arrays.sort(combined);
        return combined;
    }
}
