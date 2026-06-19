package at.hannibal2.skyhanni.mixins.hooks;

public interface MessageIdStore {

    default int skyhanni$getMessageId() { throw new UnsupportedOperationException("Implemented via mixin"); }
}
