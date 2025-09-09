package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;

//#if MC < 1.21
//$$ @Mixin(SignEditScreen.class)
//$$ public interface AccessorGuiEditSign {
//$$     @Accessor("sign")
//$$     SignBlockEntity getTileSign();
//$$
//$$     @Accessor
//$$     int getCurrentRow();
//$$ }
//#endif
