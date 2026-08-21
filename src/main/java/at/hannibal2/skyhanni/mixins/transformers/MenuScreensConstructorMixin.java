package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.MenuScreensHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Refrence: https://github.com/SkyblockerMod/Skyblocker/blob/main/src/main/java/de/hysky/skyblocker/mixins/MenuScreensConstructorMixin.java
@Mixin(MenuScreens.ScreenConstructor.class)
public interface MenuScreensConstructorMixin<T extends AbstractContainerMenu> {

    @Inject(method = "fromPacket", at = @At("HEAD"), cancellable = true)
    private void skyHanni$openCustomMenu(
        Component name,
        MenuType<T> type,
        Minecraft client,
        int id,
        CallbackInfo ci
    ) {
        if (MenuScreensHook.openCustomMenu(name, type, id)) {
            ci.cancel();
        }
    }
}
