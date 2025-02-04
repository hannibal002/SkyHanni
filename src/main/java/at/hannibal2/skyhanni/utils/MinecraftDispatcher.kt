package at.hannibal2.skyhanni.utils

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.internal.MainDispatcherFactory
import net.minecraft.client.Minecraft
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

@Suppress("unused")
public val Dispatchers.Minecraft: MinecraftDispatcher
    get() = at.hannibal2.skyhanni.utils.Minecraft

@OptIn(InternalCoroutinesApi::class)
public sealed class MinecraftDispatcher : MainCoroutineDispatcher(), Delay {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Minecraft.getMinecraft().addScheduledTask(block)
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val cancellableRun = schedule(timeMillis) {
            with(continuation) { resumeUndispatched(Unit) }
        }
        continuation.invokeOnCancellation { cancellableRun.cancel() }
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val cancellableRun = schedule(timeMillis) {
            block.run()
        }
        return DisposableHandle { cancellableRun.cancel() }
    }

    private fun schedule(timeMillis: Long, action: () -> Unit): CancellableRun {
        val run = CancellableRun(action)
        DelayedRun.runDelayed(timeMillis.milliseconds, run)
        return run
    }

    class CancellableRun(val run: () -> Unit, @Transient var isCancelled: Boolean = false) : () -> Unit {
        override fun invoke() {
            if (isCancelled) return
            this.run()
        }

        fun cancel() {
            isCancelled = true
        }
    }
}

@OptIn(InternalCoroutinesApi::class)
internal class MinecraftDispatcherFactory : MainDispatcherFactory {
    override val loadPriority: Int
        get() = 0

    override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher = Minecraft
}

private object ImmediateMinecraftDispatcher : MinecraftDispatcher() {
    override val immediate: MainCoroutineDispatcher
        get() = this

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = !Minecraft.getMinecraft().isCallingFromMinecraftThread

    @OptIn(InternalCoroutinesApi::class)
    override fun toString() = toStringInternalImpl() ?: "Minecraft.immediate"
}

internal object Minecraft : MinecraftDispatcher() {
    override val immediate: MainCoroutineDispatcher
        get() = ImmediateMinecraftDispatcher

    @OptIn(InternalCoroutinesApi::class)
    override fun toString() = toStringInternalImpl() ?: "Minecraft"
}

