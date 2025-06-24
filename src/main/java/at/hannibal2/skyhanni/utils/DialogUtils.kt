package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.ErrorManager
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.UIManager

object DialogUtils {

    /**
     * Taken and modified from Skytils
     */
    fun openPopupWindow(title: String, message: String) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: java.lang.Exception) {
            ErrorManager.logErrorWithData(
                e, "Failed to open a popup window",
                "message" to message,
            )
        }

        val frame = JFrame().apply {
            isUndecorated = true
            isAlwaysOnTop = true
            setLocationRelativeTo(null)
            isVisible = true
        }

        val buttons = mutableListOf<JButton>()
        val close = JButton("Ok")
        close.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    frame.isVisible = false
                }
            },
        )
        buttons.add(close)

        val allOptions = buttons.toTypedArray()
        JOptionPane.showOptionDialog(
            frame,
            message,
            title,
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            allOptions,
            allOptions[0],
        )
    }
}
