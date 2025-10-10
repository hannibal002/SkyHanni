package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.ErrorManager
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.UIManager

object DialogUtils {

    private val closeListener = object : MouseAdapter() {
        override fun mouseClicked(event: MouseEvent) {
            (event.source as? JFrame)?.isVisible = false
        }
    }

    private val baseFrame by lazy {
        JFrame().apply {
            isUndecorated = true
            isAlwaysOnTop = true
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

    private val okButton = JButton("Ok").apply { addMouseListener(closeListener) }

    /**
     * Taken and modified from Skytils
     */
    fun openPopupWindow(title: String, message: String) = runCatching {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        JOptionPane.showOptionDialog(
            baseFrame, message, title,
            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
            listOf(okButton).toTypedArray(), okButton,
        )
    }.onFailure { e ->
        ErrorManager.logErrorWithData(
            e, "Failed to open a popup window",
            "message" to message,
        )
    }
}
