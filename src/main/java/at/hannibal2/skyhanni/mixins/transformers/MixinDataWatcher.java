package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.DataWatcherUpdatedEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(SynchedEntityData.class)
public abstract class MixinDataWatcher {
    @Shadow
    @Final
    private SyncedDataHolder entity;

    @Shadow
    @Final
    private SynchedEntityData.DataItem<?>[] itemsById;

    @Shadow
    protected abstract void assignValue(SynchedEntityData.DataItem<?> par1, SynchedEntityData.DataValue<?> par2);

    @Inject(method = "assignValues", at = @At("TAIL"))
    public void onWhatever(List<SynchedEntityData.DataValue<?>> entries, CallbackInfo ci) {
        if (this.entity instanceof Entity entity) {
            List<SynchedEntityData.DataItem<?>> dataEntries = new ArrayList<>();
            for (SynchedEntityData.DataValue<?> serializedEntry : entries) {
                SynchedEntityData.DataItem<?> entry = this.itemsById[serializedEntry.id()];
                this.assignValue(entry, serializedEntry);
                dataEntries.add(entry);
            }

            new DataWatcherUpdatedEvent<>(entity, dataEntries).post();
        }
    }
}
