package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.utils.TimeUtils;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.components.debugchart.AbstractDebugChart;
import net.minecraft.client.gui.components.debugchart.PingDebugChart;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractDebugChart.class)
public abstract class MixinAbstractDebugChart {

    @WrapMethod(method = "getValueForAggregation")
    public long getValueForAggregation(final int sampleIndex, Operation<Long> original) {
        long orig = original.call(sampleIndex);
        //noinspection ConstantConditions
        if (!((Object) (this) instanceof PingDebugChart)) return orig;
        return TimeUtils.isAprilFoolsDay() ? orig * 2: orig;
    }
}
