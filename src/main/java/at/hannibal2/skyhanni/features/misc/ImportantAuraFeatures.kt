package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.ElectionApi
import at.hannibal2.skyhanni.data.ElectionCandidate
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SignUtils
import at.hannibal2.skyhanni.utils.SignUtils.isPlayerElectionSign
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.fakePlayer
import net.minecraft.client.gui.inventory.GuiEditSign

@SkyHanniModule
object ImportantAuraFeatures {

    private val fakePlayer by lazy { FakePlayer(hannibal = true) }

    val pos = Position(100, 100)

    @HandleEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (ElectionApi.currentMayor != ElectionCandidate.AURA) return
        if (event.inventoryName == "Player Election") ChatUtils.chat("§eMake sure to vote for hannibal2 :)", prefix = false)
    }

    @HandleEvent
    fun onRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (ElectionApi.currentMayor != ElectionCandidate.AURA) return
        if (InventoryUtils.openInventoryName() != "Player Election") return

        val renderables = buildList {
            addString("§eVote For hannibal2")
            add(Renderable.fakePlayer(fakePlayer, width = 100, height = 200, entityScale = 100, followMouse = true))
            addString("§eA vote for hannibal2 is a vote for freedom")
        }

        pos.renderRenderables(renderables, posLabel = "Important Propaganda")
    }

    @HandleEvent
    fun onSignOpen(event: GuiScreenOpenEvent) {
        if (ElectionApi.currentMayor != ElectionCandidate.AURA) return
        val gui = event.gui as? GuiEditSign ?: return
        if (!gui.isPlayerElectionSign()) return
        DelayedRun.runNextTick {
            SignUtils.setTextIntoSign("hannibal2", 0)
        }
        ChatUtils.chat("§eAutomatically Voting For The Best Candidate", prefix = false)
    }
}
