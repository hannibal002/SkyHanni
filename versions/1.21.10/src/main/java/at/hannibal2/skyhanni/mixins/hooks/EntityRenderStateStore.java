package at.hannibal2.hanni.mixins.hooks;

import net.minecraft.entity.Entity;

public interface EntityRenderStateStore extends GlowingStateStore {

    void hanni$setEntity(Entity entity);
    Entity hanni$getEntity();

}
