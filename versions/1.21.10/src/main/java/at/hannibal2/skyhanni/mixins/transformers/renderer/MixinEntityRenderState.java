package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderStateStore;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class MixinEntityRenderState implements EntityRenderStateStore {

    @Unique
    Entity skyhanni$savedEntity = null;

    @Override
    public void skyhanni$setEntity(Entity entity) {
        skyhanni$savedEntity = entity;
    }

    @Override
    public Entity skyhanni$getEntity() {
        return skyhanni$savedEntity;
    }
}
