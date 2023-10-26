package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import kotlin.time.Duration.Companion.milliseconds

object ClipboardUtils {
    private var lastClipboardAccessTime = SimpleTimeMark.farPast()

    private fun canAccessClipboard(): Boolean {
        val result = lastClipboardAccessTime.passedSince() > 10.milliseconds
        if (result) {
            lastClipboardAccessTime = SimpleTimeMark.now()
        }
        return result
    }

    private suspend fun getClipboard(): Clipboard? {
        val deferred = CompletableDeferred<Clipboard?>()
        if (canAccessClipboard()) {
            deferred.complete(Toolkit.getDefaultToolkit().systemClipboard)
        } else {
            LorenzUtils.runDelayed(5.milliseconds) {
                SkyHanniMod.coroutineScope.launch {
                    deferred.complete(getClipboard())
                }
            }
        }
        return deferred.await()
    }

    fun copyToClipboard(text: String, step: Int = 0) {
        SkyHanniMod.coroutineScope.launch {
            try {
                getClipboard()?.setContents(StringSelection(text), null)
            } catch (e: Exception) {
                if (step == 3) {
                    ErrorManager.logError(e, "Error while trying to access the clipboard.")
                } else {
                    copyToClipboard(text, step + 1)
                }
            }
        }
    }

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
                ErrorManager.logError(e, "Error while trying to access the clipboard.")
                null
            } else {
                readFromClipboard(step + 1)
            }
        }
    }
}