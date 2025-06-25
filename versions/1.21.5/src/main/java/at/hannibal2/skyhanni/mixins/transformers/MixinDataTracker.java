package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.DataWatcherUpdatedEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracked;
import net.minecraft.entity.data.DataTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DataTracker.class)
public class MixinDataTracker {
    @Shadow
    @Final
    private DataTracked trackedEntity;

    @Inject(method = "writeUpdatedEntries", at = @At("TAIL"))
    public void onWhatever(List<DataTracker.Entry<?>> entries, CallbackInfo ci) {
        if (trackedEntity instanceof Entity entity) {
            new DataWatcherUpdatedEvent(entity, entries).post();
        }
    }
}
