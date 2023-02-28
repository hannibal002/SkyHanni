package at.hannibal2.skyhanni.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


suspend fun <T> CompletableFuture<T>.await(): T {
    return suspendCancellableCoroutine {
        this.handle { value, error ->
            if (error != null) {
                it.resumeWithException(error)
            } else {
                it.resume(value as T)
            }
        }
        it.invokeOnCancellation {
            this.cancel(true)
        }
    }
}



