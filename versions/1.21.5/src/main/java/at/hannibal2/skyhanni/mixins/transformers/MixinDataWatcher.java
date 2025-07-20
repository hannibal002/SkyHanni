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

import java.util.ArrayList;
import java.util.List;

@Mixin(DataTracker.class)
public abstract class MixinDataWatcher {
    @Shadow
    @Final
    private DataTracked trackedEntity;

    @Shadow
    @Final
    private DataTracker.Entry<?>[] entries;

    @Shadow
    protected abstract void copyToFrom(DataTracker.Entry<?> par1, DataTracker.SerializedEntry<?> par2);

    @Inject(method = "writeUpdatedEntries", at = @At("TAIL"))
    public void onWhatever(List<DataTracker.SerializedEntry<?>> entries, CallbackInfo ci) {
        if (trackedEntity instanceof Entity entity) {
            List<DataTracker.Entry<?>> dataEntries = new ArrayList<>();
            for (DataTracker.SerializedEntry<?> serializedEntry : entries) {
                DataTracker.Entry<?> entry = this.entries[serializedEntry.id()];
                this.copyToFrom(entry, serializedEntry);
                dataEntries.add(entry);
            }

            new DataWatcherUpdatedEvent<>(entity, dataEntries).post();
        }
    }
}
