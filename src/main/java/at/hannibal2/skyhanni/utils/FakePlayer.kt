package at.hannibal2.skyhanni.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.ResourceLocation
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.imageio.ImageIO

object FakePlayer {
    fun getFakePlayer(skinUrl: String? = null): EntityOtherPlayerMP {
        val mc = Minecraft.getMinecraft()
        val player = mc.thePlayer!!

        val skinResource = skinUrl?.let { url ->
            val skinFile = getSkinFile(url)
            loadTextureFromFile(skinFile)
        } ?: player.locationSkin ?: DefaultPlayerSkin.getDefaultSkin(player.uniqueID)

        return object : EntityOtherPlayerMP(mc.theWorld, player.gameProfile) {
            override fun getLocationSkin(): ResourceLocation = skinResource

            override fun getTeam() = object : ScorePlayerTeam(null, null) {
                override fun getNameTagVisibility() = EnumVisible.NEVER
            }

            override fun isWearing(part: EnumPlayerModelParts): Boolean = player.isWearing(part) && part != EnumPlayerModelParts.CAPE
        }
    }

    private fun downloadAndSaveSkin(url: String, file: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun getSkinFile(url: String): File {
        val mc = Minecraft.getMinecraft()
        val skinDirectory = File(mc.mcDataDir, "skins")

        // Ensure the directory exists
        if (!skinDirectory.exists()) {
            skinDirectory.mkdirs() // Create the directory if it does not exist
        }

        val skinFile = File(skinDirectory, "${url.hashCode()}.png")

        // Download and save the skin if it does not already exist
        if (!skinFile.exists()) {
            downloadAndSaveSkin(url, skinFile)
        }

        return skinFile
    }


    private fun loadTextureFromFile(file: File): ResourceLocation? {
        val mc = Minecraft.getMinecraft()
        val textureManager = mc.textureManager

        // Load the image
        val bufferedImage = ImageIO.read(file)

        // Create a DynamicTexture from the BufferedImage
        val dynamicTexture = DynamicTexture(bufferedImage)
        textureManager.loadTexture(ResourceLocation("skins/${file.name}"), dynamicTexture)

        return ResourceLocation("skins/${file.name}")
    }
}
