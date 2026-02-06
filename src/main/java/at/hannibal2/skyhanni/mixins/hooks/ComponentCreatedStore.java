package at.hannibal2.skyhanni.mixins.hooks;

public interface ComponentCreatedStore {

    default boolean skyhanni$didCreate() { throw new UnsupportedOperationException("Implemented via mixin"); }
    default void skyhanni$setCreated(boolean value) { throw new UnsupportedOperationException("Implemented via mixin"); }

}
