package at.hannibal2.skyhanni.mixins.hooks;

import net.minecraft.world.entity.Entity;

public interface EntityRenderStateStore extends GlowingStateStore {

    void skyhanni$setEntity(Entity entity);
    Entity skyhanni$getEntity();

}
