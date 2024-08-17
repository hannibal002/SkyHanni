package at.hannibal2.skyhanni.features.gui.electionviewer

import at.hannibal2.skyhanni.data.Mayor
import at.hannibal2.skyhanni.data.jsonobjects.other.MayorCandidate
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.renderables.Renderable
import com.google.gson.JsonParser
import java.util.Base64

object ElectionViewerUtils {

    fun getFakeMayor(mayor: Mayor) = Renderable.fakePlayer(
        FakePlayer.getFakePlayer(getSkinFromMayorName(mayor.name)),
        followMouse = true,
        entityScale = 50,
    )

    fun getFakeCandidate(candidate: MayorCandidate) = Renderable.fakePlayer(
        FakePlayer.getFakePlayer(getSkinFromMayorName(candidate.name)),
        followMouse = true,
        entityScale = 50,
    )

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
