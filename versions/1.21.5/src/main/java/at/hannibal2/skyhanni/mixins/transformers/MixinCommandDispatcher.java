package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.chat.TabCompletionEvent;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Mixin(value = CommandDispatcher.class, remap = false)
public class MixinCommandDispatcher<S> {

    @Inject(method = "getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Ljava/lang/String;toLowerCase(Ljava/util/Locale;)Ljava/lang/String;"), cancellable = true)
    public void getCompletionSuggestions(ParseResults<S> parse, int cursor, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir, @Local(ordinal = 1) int start, @Local(ordinal = 0) String fullInput, @Local(ordinal = 1) String beforeCursor) {
        TabCompletionEvent tabCompletionEvent = new TabCompletionEvent(fullInput, beforeCursor, new ArrayList<>());
        tabCompletionEvent.post();
        String[] additional = tabCompletionEvent.intoSuggestionArray();
        if (additional == null) return;

        SuggestionsBuilder suggestionsBuilder = new SuggestionsBuilder(beforeCursor, start);

        for (String s : additional) {
            suggestionsBuilder.suggest(s);
        }

        cir.setReturnValue(suggestionsBuilder.buildFuture());
    }
}
