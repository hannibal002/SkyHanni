package at.hannibal2.skyhanni.mixins.hooks;

import net.minecraft.network.chat.Component;

public interface ChatLineData {

    default Component skyhanni$getFullComponent() { throw new UnsupportedOperationException("Implemented via mixin"); }
    default void skyhanni$setFullComponent(Component fullComponent) { throw new UnsupportedOperationException("Implemented via mixin"); }

}
