package com.thatgravyboat.skyblockhud.mixins;

import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.api.item.IAbility;
import com.thatgravyboat.skyblockhud.handlers.CooldownHandler;
import java.util.regex.Matcher;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class MixinItemStack implements IAbility {

    private String ability;
    private int abilityTime;

    @Inject(method = "setTagCompound", at = @At("HEAD"))
    public void onNbt(NBTTagCompound nbt, CallbackInfo ci) {
        if (SkyblockHud.config != null && !SkyblockHud.config.misc.hideItemCooldowns) {
            Matcher abilityMatcher = CooldownHandler.getAbility(nbt);
            if (abilityMatcher != null) {
                ability = abilityMatcher.group(1);
                try {
                    abilityTime = Integer.parseInt(abilityMatcher.group(2).trim());
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public String getAbility() {
        return ability;
    }

    @Override
    public int getAbilityTime() {
        return abilityTime;
    }
}
