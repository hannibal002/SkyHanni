package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.MessageHandlerHookKt;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = MessageHandler.class, priority = 500)
public class MixinMessageHandler {

    @WrapMethod(method = "onGameMessage")
    private void onGameMessage(Text message, boolean actionBar, Operation<Void> original) {
        MessageHandlerHookKt.onGameMessage(message, actionBar, original);
    }
}
