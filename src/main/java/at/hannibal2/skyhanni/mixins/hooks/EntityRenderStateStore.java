package at.hannibal2.skyhanni.mixins.hooks;

//? if < 26.2 {
import net.minecraft.world.entity.Entity;

public interface EntityRenderStateStore extends GlowingStateStore {

    default Entity skyhanni$getEntity() { throw new UnsupportedOperationException("Implemented via mixin"); }
    default void skyhanni$setEntity(Entity entity) { throw new UnsupportedOperationException("Implemented via mixin"); }

}
//?}
