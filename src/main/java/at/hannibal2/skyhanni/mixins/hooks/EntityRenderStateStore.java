package at.hannibal2.skyhanni.mixins.hooks;

import net.minecraft.world.entity.Entity;

//? if >= 26.2 {
public interface EntityRenderStateStore  {
//?} else {
/*public interface EntityRenderStateStore extends GlowingStateStore
*///?}

    default Entity skyhanni$getEntity() { throw new UnsupportedOperationException("Implemented via mixin"); }
    default void skyhanni$setEntity(Entity entity) { throw new UnsupportedOperationException("Implemented via mixin"); }

}
