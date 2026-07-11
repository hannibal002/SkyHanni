package at.hannibal2.skyhanni.test.event

import at.hannibal2.skyhanni.api.event.AbstractSkyHanniEvent
import at.hannibal2.skyhanni.api.event.AsyncEventDispatchTracker
import at.hannibal2.skyhanni.api.event.AsyncSkyHanniEvent
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.SkyHanniEvents
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.events.minecraft.ClientShutdownEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Explicit annotation event types are deprecated, but are intentionally exercised for runtime compatibility validation.
@Suppress("DEPRECATION")
class EventDispatchTest {
    private val registered = mutableListOf<Any>()

    private fun register(listener: Any) {
        SkyHanniEvents.registerForTest(listener)
        registered.add(listener)
    }

    @AfterEach
    fun cleanup() {
        registered.asReversed().forEach(SkyHanniEvents::unregister)
        AsyncEventDispatchTracker.resetForTest()
    }

    @Test
    fun `synchronous handlers run inline on the posting thread`() {
        var invokedOn: Thread? = null
        val listener = object {
            @HandleEvent
            fun onEvent(event: TestSyncEvent) {
                invokedOn = Thread.currentThread()
            }
        }
        register(listener)

        val postingThread = Thread.currentThread()
        assertFalse(TestSyncEvent().post().isCancelled)
        assertSame(postingThread, invokedOn)
    }

    @Test
    fun `synchronous cancellation remains immediate`() {
        val listener = object {
            @HandleEvent
            fun onEvent(event: TestCancellableSyncEvent) = event.cancel()
        }
        register(listener)

        assertTrue(TestCancellableSyncEvent().post().isCancelled)
    }

    @Test
    fun `async cancellation stops later listeners unless they receive cancelled`() = runBlocking {
        val order = mutableListOf<String>()
        register(
            object {
                @HandleEvent(priority = HandleEvent.HIGH)
                private suspend fun onEvent(event: TestCancellableAsyncEvent) {
                    yield()
                    order.add("cancelling")
                    event.cancel()
                }
            },
        )
        register(
            object {
                @HandleEvent
                private suspend fun onEvent(event: TestCancellableAsyncEvent) {
                    order.add("skipped")
                }
            },
        )
        register(
            object {
                @HandleEvent(priority = HandleEvent.LOW, receiveCancelled = true)
                private suspend fun onEvent(event: TestCancellableAsyncEvent) {
                    order.add("receives-cancelled")
                }
            },
        )

        assertTrue(TestCancellableAsyncEvent().post())
        assertEquals(listOf("cancelling", "receives-cancelled"), order)
    }

    @Test
    fun `registration rejects the wrong function kind`() {
        val suspendSync = object {
            @HandleEvent
            private suspend fun onEvent(event: TestSyncEvent) = Unit
        }
        val ordinaryAsync = object {
            @HandleEvent
            fun onEvent(event: TestAsyncEvent) = Unit
        }

        assertThrows(IllegalArgumentException::class.java) { SkyHanniEvents.registerForTest(suspendSync) }
        assertThrows(IllegalArgumentException::class.java) { SkyHanniEvents.registerForTest(ordinaryAsync) }
    }

    @Test
    fun `async posting awaits genuinely suspended listeners in priority order`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val entered = CompletableDeferred<Unit>()
        val order = mutableListOf<String>()
        val later = object {
            @HandleEvent(priority = HandleEvent.LOW)
            private suspend fun onEvent(event: TestAsyncEvent) {
                order.add("later")
            }
        }
        val first = object {
            @HandleEvent(priority = HandleEvent.HIGH)
            private suspend fun onEvent(event: TestAsyncEvent) {
                order.add("first-start")
                entered.complete(Unit)
                gate.await()
                yield()
                order.add("first-end")
            }
        }
        register(later)
        register(first)

        val posting = launch { TestAsyncEvent().post() }
        entered.await()
        assertEquals(listOf("first-start"), order)
        assertFalse(posting.isCompleted)
        gate.complete(Unit)
        posting.join()
        assertEquals(listOf("first-start", "first-end", "later"), order)
    }

    @Test
    fun `equal priority async listeners keep registration order`() = runBlocking {
        val order = mutableListOf<Int>()
        register(object {
            @HandleEvent
            private suspend fun onEvent(event: TestAsyncEvent) {
                order.add(1)
            }
        })
        register(object {
            @HandleEvent
            private suspend fun onEvent(event: TestAsyncEvent) {
                order.add(2)
            }
        })

        TestAsyncEvent().post()
        assertEquals(listOf(1, 2), order)
    }

    @Test
    fun `async errors reach onError and do not stop later listeners`() = runBlocking {
        val failure = IllegalStateException("expected")
        val errors = mutableListOf<Throwable>()
        var laterRan = false
        register(object {
            @HandleEvent(priority = HandleEvent.HIGH)
            private suspend fun onEvent(event: TestAsyncEvent) {
                throw failure
            }
        })
        register(object {
            @HandleEvent(priority = HandleEvent.LOW)
            private suspend fun onEvent(event: TestAsyncEvent) {
                laterRan = true
            }
        })

        TestAsyncEvent().post(errors::add)
        assertEquals(listOf(failure), errors)
        assertTrue(laterRan)
    }

    @Test
    fun `cancellation propagates without error delivery`() {
        val errors = mutableListOf<Throwable>()
        var laterRan = false
        register(object {
            @HandleEvent(priority = HandleEvent.HIGH)
            private suspend fun onEvent(event: TestAsyncEvent) {
                throw CancellationException("expected")
            }
        })
        register(object {
            @HandleEvent(priority = HandleEvent.LOW)
            private suspend fun onEvent(event: TestAsyncEvent) {
                laterRan = true
            }
        })

        assertThrows(CancellationException::class.java) {
            runBlocking { TestAsyncEvent().post(errors::add) }
        }
        assertTrue(errors.isEmpty())
        assertFalse(laterRan)
    }

    @Test
    fun `async listeners respect island and skyblock restrictions`() = runBlocking {
        val invoked = mutableListOf<String>()
        register(object {
            @HandleEvent(onlyOnSkyblock = true)
            private suspend fun onEvent(event: TestAsyncEvent) {
                invoked.add("skyblock")
            }
        })
        register(object {
            @HandleEvent(onlyOnIsland = IslandType.HUB)
            private suspend fun onEvent(event: TestAsyncEvent) {
                invoked.add("island")
            }
        })
        register(object {
            @HandleEvent(onlyOnIslands = [IslandType.HUB, IslandType.PRIVATE_ISLAND])
            private suspend fun onEvent(event: TestAsyncEvent) {
                invoked.add("islands")
            }
        })
        register(object {
            @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.MINING])
            private suspend fun onEvent(event: TestAsyncEvent) {
                invoked.add("tag")
            }
        })
        register(object {
            @HandleEvent
            private suspend fun onEvent(event: TestAsyncEvent) {
                invoked.add("unrestricted")
            }
        })

        // Tests run outside SkyBlock, so only the unrestricted listener may fire.
        TestAsyncEvent().post()
        assertEquals(listOf("unrestricted"), invoked)
    }

    @Test
    fun `client shutdown cancels active dispatch and rejects new dispatch`() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        register(AsyncEventDispatchTracker)
        register(object {
            @HandleEvent
            private suspend fun onEvent(event: TestAsyncEvent) {
                entered.complete(Unit)
                gate.await()
            }
        })

        val posting = launch { TestAsyncEvent().post() }
        entered.await()
        ClientShutdownEvent.post()
        posting.join()
        assertTrue(posting.isCancelled)
        assertThrows(CancellationException::class.java) {
            runBlocking { TestAsyncEvent().post() }
        }
    }

    @Test
    fun `zero parameter handlers work for both event families`() = runBlocking {
        var syncInvocations = 0
        var asyncInvocations = 0
        register(object {
            @HandleEvent(eventType = TestSyncEvent::class)
            fun sync() {
                syncInvocations++
            }
        })
        register(object {
            @HandleEvent(eventType = TestAsyncEvent::class)
            suspend fun async() {
                yield()
                asyncInvocations++
            }
        })

        TestSyncEvent().post()
        TestAsyncEvent().post()
        assertEquals(1, syncInvocations)
        assertEquals(1, asyncInvocations)
    }

    @Test
    fun `private handlers are supported for both event families`() = runBlocking {
        val handler = PrivateHandler()
        register(handler)

        TestSyncEvent().post()
        TestAsyncEvent().post()
        assertEquals(1, handler.syncInvocations)
        assertEquals(1, handler.asyncInvocations)
    }

    @Test
    fun `invalid runtime signatures are rejected`() {
        val tooManyParameters = object {
            @HandleEvent
            fun invalid(event: TestSyncEvent, extra: String) = Unit
        }
        val wrongParameter = object {
            @HandleEvent
            fun invalid(value: String) = Unit
        }
        val wrongReturn = object {
            @HandleEvent
            suspend fun invalid(event: TestAsyncEvent): String = "invalid"
        }
        val mixedFamilies = object {
            @HandleEvent(eventTypes = [TestSyncEvent::class, TestAsyncEvent::class])
            fun invalid() = Unit
        }

        listOf(tooManyParameters, wrongParameter, wrongReturn, mixedFamilies, FakeSuspendEventHandler())
            .forEach { listener ->
                assertThrows(IllegalArgumentException::class.java) { SkyHanniEvents.registerForTest(listener) }
            }
    }

    private class PrivateHandler {
        var syncInvocations = 0
        var asyncInvocations = 0

        @HandleEvent
        private fun onSync(event: TestSyncEvent) {
            syncInvocations++
        }

        @HandleEvent
        private suspend fun onAsync(event: TestAsyncEvent) {
            yield()
            asyncInvocations++
        }
    }
}

class TestSyncEvent : SkyHanniEvent()
class TestCancellableSyncEvent : SkyHanniEvent(), AbstractSkyHanniEvent.Cancellable
class TestAsyncEvent : AsyncSkyHanniEvent()
class TestCancellableAsyncEvent : AsyncSkyHanniEvent(), AbstractSkyHanniEvent.Cancellable
