package at.hannibal2.skyhanni.mixins.transformers;

import io.github.notenoughupdates.moulconfig.processor.ProcessedOptionImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ProcessedOptionImpl.class, remap = false)
public interface AcceessmoulConfigPlease {
    @Accessor("path")
    String getPath();
}
