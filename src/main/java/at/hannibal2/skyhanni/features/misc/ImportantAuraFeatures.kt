package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.ElectionApi
import at.hannibal2.skyhanni.data.ElectionCandidate
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.render.gui.ScreenDrawnEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SignUtils
import at.hannibal2.skyhanni.utils.SignUtils.isPlayerElectionSign
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.fakePlayer
import com.mojang.authlib.GameProfile
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import java.util.UUID
import java.util.concurrent.CompletableFuture

//#if MC > 1.21
//$$ import kotlin.jvm.optionals.getOrNull
//#endif

@SkyHanniModule
object ImportantAuraFeatures {

    private val fakePlayer by lazy { FakePlayer() }

    val pos = Position(100, 100)

    private var hasSent = false

    @HandleEvent
    fun onIslandChanged(event: IslandChangeEvent) {
        if (!isEnabled()) return
        if (ElectionApi.currentMayor != ElectionCandidate.AURA) return
        if (event.newIsland != IslandType.HUB) return
        if (!hasSent) {
            ChatUtils.chat("Make sure to vote in the minister election!")
            hasSent = true
        }
    }

    private var lastFakePlayerUUID: String? = null

    @HandleEvent(ConfigLoadEvent::class)
    fun onRepoReload() {
        if (getTarget() == lastFakePlayerUUID) return

        CompletableFuture.runAsync {
            //#if MC < 1.21
            val gameProfile = Minecraft.getMinecraft().sessionService.fillProfileProperties(
                GameProfile(
                    UUID.fromString(getTarget()),
                    "UnknownPlayer"
                ),
                true
            )

            fakePlayer.gameProfile = gameProfile

            Minecraft.getMinecraft().skinManager.loadProfileTextures(
                gameProfile,
                { type, resourceLocation, profileTexture ->
                    when (type) {
                        MinecraftProfileTexture.Type.CAPE -> fakePlayer.setCape(resourceLocation)
                        MinecraftProfileTexture.Type.SKIN -> {
                            fakePlayer.setSkin(resourceLocation)
                            fakePlayer.setSkinType(profileTexture.getMetadata("model"))
                            lastFakePlayerUUID = getTarget()
                        }
                    }
                },
                true
            )
            //#else
            //$$ val gameProfile = MinecraftClient.getInstance().apiServices.profileResolver().getProfileById(UUID.fromString(getTarget())).getOrNull()

            //$$ fakePlayer.gameProfile = gameProfile

            //$$ fakePlayer.setSkinTextures(MinecraftClient.getInstance().skinProvider.supplySkinTextures(
            //$$     gameProfile,
            //$$     true
            //$$ ).get())
            //#endif
        }
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (ElectionApi.currentMayor != ElectionCandidate.AURA) return
        if (event.inventoryName == "Player Election") ChatUtils.chat("§eMake sure to vote!", prefix = false)
    }

    @HandleEvent(GuiRenderEvent.ChestGuiOverlayRenderEvent::class)
    fun onRender() {
        if (!isEnabled()) return
        if (ElectionApi.currentMayor != ElectionCandidate.AURA) return
        if (InventoryUtils.openInventoryName() != "Player Election") return

        val renderables = buildList {
            addString("§eVote For ${fakePlayer.getNameForScoreboard()}")
            add(Renderable.fakePlayer(fakePlayer, width = 100, height = 200, entityScale = 100, followMouse = true))
            addString("§eA vote for ${fakePlayer.getNameForScoreboard()} is a vote for freedom")
        }

        pos.renderRenderables(
            renderables,
            posLabel = "Important Propaganda",
            addToGuiManager = false
        )
    }

    @HandleEvent
    fun onScreenDrawn(event: ScreenDrawnEvent) {
        if (!isEnabled()) return
        if (ElectionApi.currentMayor != ElectionCandidate.AURA) return
        val gui = event.gui as? GuiEditSign ?: return
        if (!gui.isPlayerElectionSign()) return

        val renderables = buildList {
            addString("§eVote For ${fakePlayer.getNameForScoreboard()}")
            add(Renderable.link(
                Renderable.fakePlayer(fakePlayer, width = 100, height = 200, entityScale = 100, followMouse = true),
                onLeftClick = {
                    SignUtils.setTextIntoSign("${fakePlayer.getNameForScoreboard()}")
                }
            ))
            add(Renderable.link(
                "§eClick §lHERE§e to vote for ${fakePlayer.getNameForScoreboard()}!",
                onLeftClick = {
                    SignUtils.setTextIntoSign("${fakePlayer.getNameForScoreboard()}")
                }
            ))
        }

        pos.renderRenderables(
            renderables,
            posLabel = "Important Propaganda",
            addToGuiManager = false
        )
    }


    fun getTarget() = SkyHanniMod.feature.dev.debug.auraPropagandas.targetUUID

    fun isEnabled() = SkyHanniMod.feature.dev.debug.auraPropagandas.enabled
}
