package at.hannibal2.skyhanni.test.event;

import at.hannibal2.skyhanni.api.event.HandleEvent;
import kotlin.coroutines.Continuation;

public class FakeSuspendEventHandler {
    @HandleEvent
    public Object invalid(TestAsyncEvent event, Continuation<? super kotlin.Unit> continuation) {
        return kotlin.Unit.INSTANCE;
    }
}
