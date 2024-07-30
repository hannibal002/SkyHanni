package at.hannibal2.skyhanni.features.gui.customscoreboard.replacements

import at.hannibal2.skyhanni.utils.LorenzUtils.round
import net.minecraft.client.Minecraft

object Pitch : ScoreboardReplacements() {
    override val trigger = "%pitch%"
    override val name = "Pitch"
    override fun replacement(): String = Minecraft.getMinecraft().thePlayer.rotationPitch.round(2).toString()
}
