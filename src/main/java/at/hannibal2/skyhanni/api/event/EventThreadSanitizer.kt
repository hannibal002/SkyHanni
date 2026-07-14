package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.test.command.ErrorManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import java.lang.Thread as JavaThread

object EventThreadSanitizer {
    private fun getCurrentThreadType(): ThreadType? {
        return when {
            RenderSystem.isOnRenderThread() -> RENDER
            Minecraft.getInstance().packetProcessor().isSameThread -> NETWORK
            JavaThread.currentThread().name.startsWith("DefaultDispatcher-worker") -> DISPATCHER
            else -> null
        }
    }

    fun checkThread(name: String, allowedThreads: Set<ThreadType>?) {
        val threads = allowedThreads.takeUnless { it.isNullOrEmpty() } ?: return
        if (ANY in threads) return

        val currentThreadType = getCurrentThreadType() ?: return
        if (currentThreadType !in threads) {
            ErrorManager.logErrorStateWithData(
                "Event $name was posted on thread $currentThreadType, but allowed threads are $threads",
                "Event posted from unexpected thread",
            )
        }
    }
}
