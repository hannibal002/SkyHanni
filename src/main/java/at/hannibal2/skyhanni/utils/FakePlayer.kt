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


/**
 * Utility class for creating fake players with either the player's skin or a custom skin.
 *
 * These skins are loaded from URLs and saved to disk to avoid downloading them multiple times
 * and because it's the only way to load the custom skins.
 *
 * They are downloaded and saved to the `skins` directory in the Minecraft directory.
 *
 * @param skinUrl The URL of the skin to use for the fake player, or null to use the player skin
 * @return A fake player class with the specified skin
 */

class FakePlayer(private val skinUrl: String? = null) :
    EntityOtherPlayerMP(Minecraft.getMinecraft().theWorld, Minecraft.getMinecraft().thePlayer.gameProfile) {

    override fun getLocationSkin(): ResourceLocation {
        val player = Minecraft.getMinecraft().thePlayer

        return skinUrl?.let { url ->
            val skinFile = getSkinFile(url)
            loadTextureFromFile(skinFile)
        } ?: player.locationSkin ?: DefaultPlayerSkin.getDefaultSkin(player.uniqueID)
    }

    override fun getTeam() = object : ScorePlayerTeam(null, null) {
        override fun getNameTagVisibility() = EnumVisible.NEVER
    }

    override fun isWearing(part: EnumPlayerModelParts): Boolean =
        Minecraft.getMinecraft().thePlayer.isWearing(part) && part != EnumPlayerModelParts.CAPE


    companion object {
        /**
         * Get the skin file for the specified URL, downloading it if it does not already exist
         */
        private fun getSkinFile(url: String): File {
            val mc = Minecraft.getMinecraft()
            val skinDirectory = File(mc.mcDataDir, "skins")

            // Ensure the directory exists, creating it if it does not
            if (!skinDirectory.exists()) {
                skinDirectory.mkdirs()
            }

            val skinFile = File(skinDirectory, "${url.hashCode()}.png")

            // Download and save the skin if it does not already exist
            if (!skinFile.exists()) {
                downloadAndSaveSkin(url, skinFile)
            }

            return skinFile
        }

        /**
         * Download the skin from the specified URL and save it to the specified file
         */
        private fun downloadAndSaveSkin(url: String, file: File) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }

        /**
         * Load a texture from the specified file
         */
        private fun loadTextureFromFile(file: File): ResourceLocation {
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
}
