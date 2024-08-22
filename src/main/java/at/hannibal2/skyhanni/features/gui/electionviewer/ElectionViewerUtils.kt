package at.hannibal2.skyhanni.features.gui.electionviewer

import at.hannibal2.skyhanni.data.Mayor
import at.hannibal2.skyhanni.data.jsonobjects.other.MayorCandidate
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.renderables.Renderable
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraft.entity.passive.EntityVillager
import java.util.Base64

object ElectionViewerUtils {

    private val specialMayorStart = mapOf(
        "Scorpius" to 1,
        "Derpy" to 9,
        "Jerry" to 17,
    )

    /**
     * The code doesn't work correctly if the [currentYear] is below 17
     * @param currentYear The current year
     * @return A list of the next three special mayors and the year they will be elected
     */
    fun getNextSpecialMayors(currentYear: Int) =
        specialMayorStart.map { it.key to it.value + ((currentYear - it.value) / 24 + 1) * 24 }.sortedBy { it.second }

    fun getFakeMayor(mayor: Mayor): Renderable {
        // NEU Repo store special mayors with SPECIAL infix
        val mayorName = if (mayor in listOf(Mayor.DERPY, Mayor.JERRY, Mayor.SCORPIUS)) {
            mayor.name + "_SPECIAL"
        } else {
            mayor.name
        }

        // Jerry is a Villager, not a player
        val entity = if (mayor == Mayor.JERRY) {
            EntityVillager(Minecraft.getMinecraft().theWorld)
        } else {
            FakePlayer.getFakePlayer(getSkinFromMayorName(mayorName))
        }

        return Renderable.fakePlayer(
            entity,
            followMouse = true,
            entityScale = 50,
        )
    }

    fun getFakeCandidate(candidate: MayorCandidate) = getFakeMayor(Mayor.getMayorFromName(candidate.name) ?: Mayor.UNKNOWN)

    private fun getSkinFromMayorName(mayorName: String): String? {
        try {
            val base64Texture = "${mayorName}_MAYOR_MONSTER".asInternalName().getItemStack().getSkullTexture()
            val decodedTextureJson = String(Base64.getDecoder().decode(base64Texture), Charsets.UTF_8)
            val decodedJsonObject = JsonParser().parse(decodedTextureJson).asJsonObject
            val textures = decodedJsonObject.getAsJsonObject("textures")
            val skin = textures.getAsJsonObject("SKIN")
            return skin["url"].asString
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to get skin for $mayorName")
            return null
        }
    }

}
