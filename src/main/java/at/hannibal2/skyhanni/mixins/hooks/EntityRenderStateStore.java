package at.hannibal2.skyhanni.mixins.hooks;

import net.minecraft.world.entity.Entity;

//~ if < 26.2 '{' -> 'extends GlowingStateStore {'
public interface EntityRenderStateStore {
    default Entity skyhanni$getEntity() { throw new UnsupportedOperationException("Implemented via mixin"); }

    default void skyhanni$setEntity(Entity entity) { throw new UnsupportedOperationException("Implemented via mixin"); }
}
