package at.hannibal2.skyhanni.mixins.hooks;

public interface MessageIdStore {

    default int skyhanni$getMessageId() { throw new UnsupportedOperationException("Implemented via mixin"); }
    //? if < 26.1
    //default void skyhanni$setMessageId(int id) { throw new UnsupportedOperationException("Implemented via mixin"); }
}
