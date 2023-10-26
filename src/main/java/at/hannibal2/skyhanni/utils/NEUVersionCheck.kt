package at.hannibal2.skyhanni.utils

import net.minecraftforge.fml.common.FMLCommonHandler
import java.awt.Desktop
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.UIManager

object NEUVersionCheck {

    @JvmStatic
    fun checkIfNeuIsLoaded() {
        try {
            Class.forName("io.github.moulberry.notenoughupdates.NotEnoughUpdates")
        } catch (e: Throwable) {
            neuWarning(
                "NotEnoughUpdates is missing!\n" +
                    "SkyHanni requires the latest version of NotEnoughUpdates to work.\n" +
                    "You currently need NEU version 2.1.1-Alpha-19 or later.\n" +
                    "NEU 2.1 is NOT the latest version.\n" +
                    "It is ONLY in the #neu-alphas channel in the NEU discord\n" +
                    "Or in the #neu-updates channel in the SkyHanni discord\n" +
                    "Use these links to download the latest version:"
            )
            return
        }

        try {
            val clazz = Class.forName("io.github.moulberry.notenoughupdates.util.ItemResolutionQuery")

            for (field in clazz.methods) {
                if (field.name == "findInternalNameByDisplayName") return
            }
        } catch (_: Throwable) {
        }
        neuWarning(
            "NotEnoughUpdates is outdated!\n" +
                "You currently need NEU version 2.1.1-Alpha-19 or later.\n\n" +
                "NEU 2.1 is NOT the latest version.\n\n" +
                "NEU 2.1.1 is NOT on the NEU GitHub.\n\n" +
                "It is ONLY in the #neu-alphas channel in the NEU discord\n" +
                "Or in the #neu-updates channel in the SkyHanni discord\n" +
                "Use these links to download the latest version:"
        )
    }

    private fun neuWarning(text: String) {
        openPopupWindow(
            text,
            Pair("Join SkyHanni Discord", "https://discord.com/invite/skyhanni-997079228510117908"),
            Pair("Open SkyHanni GitHub", "https://github.com/hannibal002/SkyHanni"),
            Pair("Join NEU Discord", "https://discord.gg/moulberry"),
        )
        closeMinecraft()
    }

    /**
     * Taken and modified from Skytils
     */
    private fun openPopupWindow(errorMessage: String, vararg options: Pair<String, String>) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        val frame = JFrame()
        frame.isUndecorated = true
        frame.isAlwaysOnTop = true
        frame.setLocationRelativeTo(null)
        frame.isVisible = true

        val buttons = mutableListOf<JButton>()
        for ((name, link) in options) {
            val button = JButton(name)
            button.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    try {
                        Desktop.getDesktop().browse(URI(link))
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
            })
            buttons.add(button)
        }
        val close = JButton("Close")
        close.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                try {
                    closeMinecraft()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        })
        buttons.add(close)

        val allOptions = buttons.toTypedArray()
        JOptionPane.showOptionDialog(
            frame,
            errorMessage,
            "SkyHanni Error",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null,
            allOptions,
            allOptions[0]
        )
    }

    fun closeMinecraft() {
        FMLCommonHandler.instance().handleExit(-1)
        FMLCommonHandler.instance().expectServerStopped()
    }
}
