package at.hannibal2.skyhanni.utils

import com.mojang.authlib.GameProfile
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import net.minecraft.client.Minecraft
import java.util.UUID
import java.util.concurrent.CompletableFuture
//#if MC > 1.21
//$$ import kotlin.jvm.optionals.getOrNull
//#endif


fun FakePlayer.setPlayerUUID(uuidString: String): FakePlayer {
    CompletableFuture.runAsync {
        val uuid = UUID.fromString(uuidString)

        if (uuid == this.gameProfile?.id) return@runAsync

        //#if MC < 1.21
        val gameProfile = Minecraft.getMinecraft().sessionService.fillProfileProperties(
            GameProfile(
                uuid,
                "UnknownPlayer"
            ),
            true
        )

        this.gameProfile = gameProfile

        Minecraft.getMinecraft().skinManager.loadProfileTextures(
            gameProfile,
            { type, resourceLocation, profileTexture ->
                when (type) {
                    MinecraftProfileTexture.Type.CAPE -> this.setCape(resourceLocation)
                    MinecraftProfileTexture.Type.SKIN -> {
                        this.setSkin(resourceLocation)
                        this.setSkinType(profileTexture.getMetadata("model"))
                    }
                }
            },
            true
        )
        //#elseif MC < 1.21.10
        //$$ val profileResult = MinecraftClient.getInstance().sessionService.fetchProfile(
        //$$     uuid,
        //$$     true
        //$$ )

        //$$ val gameProfile = profileResult?.profile

        //$$ this.gameProfile = gameProfile

        //$$ this.setSkinTextures(
        //$$     gameProfile?.let {
        //$$         MinecraftClient.getInstance().skinProvider.getSkinTextures(
        //$$             gameProfile
        //$$         )
        //$$     }
        //$$ )
        //#else
        //$$ val gameProfile = MinecraftClient.getInstance().apiServices.profileResolver().getProfileById(uuid).getOrNull()

        //$$ this.gameProfile = gameProfile

        //$$ this.setSkinTextures(
        //$$     MinecraftClient.getInstance().skinProvider.supplySkinTextures(
        //$$     gameProfile,
        //$$     true
        //$$     ).get()
        //$$ )
        //#endif
    }
    return this
}
