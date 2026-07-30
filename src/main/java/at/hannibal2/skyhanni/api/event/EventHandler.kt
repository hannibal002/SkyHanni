package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.EventListeners.Listener
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.test.command.ErrorManager.maybeSkipError
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.ChatFormatting

class EventHandler<T : SkyHanniEvent> private constructor(
    val name: String,
    private val listenerCollection: ListenerCollection,
    private val canReceiveCancelled: Boolean,
    private val allowedThreads: Set<ThreadType>?,
) {

    val invokeLog = SkyHanniEvents.EventInvokeLog()

    constructor(event: Class<T>, listeners: List<Listener>) : this(
        (event.name.split(".").lastOrNull() ?: event.name).replace("$", "."),
        ListenerCollection(listeners),
        listeners.any { it.receiveCancelled },
        if (PlatformUtils.isDevEnvironment) {
            event.getAnnotation(Thread::class.java)?.values?.toSet()
        } else null
    )

    fun post(event: T, onError: ((Throwable) -> Unit)? = null) {
        invokeLog.invokeCount++
        if (SkyHanniEvents.isDisabledHandler(name)) return

        EventThreadSanitizer.checkThread(name, allowedThreads)

        var errors = 0
        listenerCollection.forEachCurrent { listener ->
            if (!listener.shouldInvoke(event)) return@forEachCurrent true

            try {
                listener.invoker.accept(event)
            } catch (originalThrowable: Throwable) {
                val throwable = originalThrowable.maybeSkipError()
                errors++
                if (errors <= 3) {
                    val errorName = throwable::class.simpleName ?: "error"
                    val aOrAn = StringUtils.optionalAn(errorName)
                    val message = "Caught $aOrAn $errorName in ${listener.name} at $name: ${throwable.message}"
                    ErrorManager.logErrorWithData(throwable, message, ignoreErrorCache = onError != null)
                }
                onError?.invoke(throwable)
            }

            !event.isCancelled || canReceiveCancelled
        }

        if (errors > 3) {
            val hiddenErrors = errors - 3
            ChatUtils.chat(
                componentBuilder {
                    append("[SkyHanni/${SkyHanniMod.VERSION}] $hiddenErrors more errors in $name are hidden!")
                    withColor(ChatFormatting.RED)
                }
            )
        }
    }
}
