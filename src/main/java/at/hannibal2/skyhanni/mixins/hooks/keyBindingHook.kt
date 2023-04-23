package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.garden.farming.GardenCustomKeybinds
import net.minecraft.client.settings.KeyBinding
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

fun isKeyDown(keyBinding: KeyBinding, cir: CallbackInfoReturnable<Boolean>) {
    GardenCustomKeybinds.isKeyDown(keyBinding, cir)
}

fun isPressed(keyBinding: KeyBinding, cir: CallbackInfoReturnable<Boolean>) {
    GardenCustomKeybinds.isPressed(keyBinding, cir)
}