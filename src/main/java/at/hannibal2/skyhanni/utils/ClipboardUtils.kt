package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import kotlin.time.Duration.Companion.milliseconds
//#if MC > 1.21
//$$ import net.minecraft.client.MinecraftClient
//#endif

object ClipboardUtils {

    private var lastClipboardAccessTime = SimpleTimeMark.farPast()

    private fun canAccessClipboard(): Boolean {
        val result = lastClipboardAccessTime.passedSince() > 10.milliseconds
        if (result) {
            lastClipboardAccessTime = SimpleTimeMark.now()
        }
        return result
    }

    //#if MC < 1.21
    private suspend fun getClipboard(retries: Int = 20): Clipboard? = if (canAccessClipboard()) {
        Toolkit.getDefaultToolkit().systemClipboard
    } else if (retries > 0) {
        delay(11)
        getClipboard(retries - 1)
    } else {
        ErrorManager.logErrorStateWithData(
            "can not read clipboard",
            "clipboard can not be accessed after 20 retries",
        )
        null
    }
    //#endif

    fun copyToClipboard(text: String, step: Int = 0) {
        SkyHanniMod.coroutineScope.launch {
            try {
                //#if MC < 1.21
                getClipboard()?.setContents(StringSelection(text), null)
                //#else
                //$$ net.minecraft.client.util.Clipboard().setClipboard(MinecraftClient.getInstance().window.handle, text)
                //#endif
            } catch (e: Exception) {
                if (step == 3) {
                    ErrorManager.logErrorWithData(e, "Error while trying to access the clipboard.")
                } else {
                    copyToClipboard(text, step + 1)
                }
            }
        }
    }

    //#if MC < 1.21
    suspend fun readFromClipboard(step: Int = 0): String? {
        try {
            return try {
                withContext(Dispatchers.IO) {
                    getClipboard()?.getData(DataFlavor.stringFlavor)?.toString()
                }
            } catch (e: UnsupportedFlavorException) {
                null
            }
        } catch (e: Exception) {
            return if (step == 3) {
                ErrorManager.logErrorWithData(e, "Error while trying to access the clipboard.")
                null
            } else {
                readFromClipboard(step + 1)
            }
        }
    }
    //#else
    //$$ fun readFromClipboard(step: Int = 0): String? {
    //$$     var shouldRetry = false
    //$$     val clipboard = net.minecraft.client.util.Clipboard().getClipboard(
    //$$         0,
    //$$     ) { _, _ ->
    //$$         shouldRetry = true
    //$$     }
    //$$     if (shouldRetry) {
    //$$         if (step == 3) {
    //$$             ErrorManager.logErrorStateWithData(
    //$$                 "can not read clipboard",
    //$$                 "clipboard can not be accessed after 3 retries",
    //$$             )
    //$$             return null
    //$$         } else {
    //$$             return readFromClipboard(step + 1)
    //$$         }
    //$$     }
    //$$     return clipboard
    //$$ }
    //#endif
}
