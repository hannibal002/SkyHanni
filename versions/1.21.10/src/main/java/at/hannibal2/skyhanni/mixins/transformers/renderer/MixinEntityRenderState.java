package at.hannibal2.hanni.mixins.transformers.renderer;

import at.hannibal2.hanni.mixins.hooks.EntityRenderStateStore;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class MixinEntityRenderState implements EntityRenderStateStore {

    @Unique
    Entity hanni$savedEntity = null;

    @Unique
    boolean hanni$usingCustomOutline = false;

    @Override
    public void hanni$setEntity(Entity entity) {
        hanni$savedEntity = entity;
    }

    @Override
    public Entity hanni$getEntity() {
        return hanni$savedEntity;
    }

    @Override
    public void hanni$setUsingCustomOutline() {
        hanni$usingCustomOutline = true;
    }

    @Override
    public boolean hanni$isUsingCustomOutline() {
        return hanni$usingCustomOutline;
    }

}
