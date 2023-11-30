package at.hannibal2.skyhanni.features.misc.customscoreboard

import net.minecraft.client.gui.Gui
import kotlin.math.sqrt

object DrawBackground {
    fun drawFilledRoundedRect(x: Int, y: Int, width: Int, height: Int, cornerRadius: Int, color: Int) {
        /*
            https://discord.com/channels/997079228510117908/1094190239532208228/1178102696742494229
         */

        // big middle one
        Gui.drawRect(x, y, x + width, y + height, color)
        // top
        Gui.drawRect(x, y, x + width, y - cornerRadius, color)
        // right
        Gui.drawRect(x + width, y, x + width + cornerRadius, y + height, color)
        // bottom
        Gui.drawRect(x, y + height, x + width, y + height + cornerRadius, color)
        // left
        Gui.drawRect(x, y, x - cornerRadius, y + height, color)

        // top left
        for (newX in -cornerRadius * 2..cornerRadius * 2) {
            for (newY in -cornerRadius * 2..cornerRadius * 2) {
                val distance = sqrt((newX * newX + newY * newY).toDouble())
                if (distance <= cornerRadius) {
                    val alpha = (255 * (1.0 - distance / cornerRadius)).toInt()
                    if (alpha > 0) {
                        Gui.drawRect(x + newX, y + newY, x + newX + 1, y + newY + 1, color)
                    }
                }
            }
        }
    }

    // Function to draw a filled quarter circle
}
