package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils.createSound
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object UltraRareBookAlert {

    private val config get() = SkyHanniMod.feature.inventory.helper.enchanting
    private val dragonSound by lazy { createSound("mob.enderdragon.growl", 1f) }

    private val superpairsGui by RepoPattern.pattern(
        "inventory.experimentstable.gui",
        "Superpairs.*"
    )

    private val ultraRarePattern by RepoPattern.pattern(
        "inventory.experimentstable.ultrarare",
        "§9Rare Book!"
    )

    private val bookPattern by RepoPattern.pattern(
        "inventory.experimentstable.book",
        "§9(?<enchant>.*)"
    )

    private var enchantsFound = false

    private var lastNotificationTime = SimpleTimeMark.farPast()

    fun notification(enchantsName: String) {
        dragonSound.playSound()
        ChatUtils.chat("You have uncovered a §d§kXX§5 ULTRA-RARE BOOK! §d§kXX§e! You found: §9$enchantsName")
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.ultraRareBookAlert) return
        if (!superpairsGui.matches(InventoryUtils.openInventoryName())) return
        if (lastNotificationTime.passedSince() > 5.seconds) return
        val gui = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return

        GlStateManager.translate(0f,0f,300f)

        val pos = Position(gui.width/2, gui.height/2, 2f, true)

        pos.renderString("§d§kXX§5 ULTRA-RARE BOOK! §d§kXX", posLabel = "ULTRA-RARE Book Notification")

        GlStateManager.translate(0f,0f,-300f)
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.ultraRareBookAlert) return
        if (enchantsFound) return
        if (!superpairsGui.matches(event.inventoryName)) return

        for ((slotId, item) in event.inventoryItems) {
            val firstLine = item.getLore().firstOrNull() ?: continue
            if (!ultraRarePattern.matches(firstLine)) continue
            val bookNameLine = item.getLore().getOrNull(2) ?: continue
            bookPattern.matchMatcher(bookNameLine){
                val enchantsName = group ("enchant")
                notification(enchantsName)
                enchantsFound = true
                lastNotificationTime = SimpleTimeMark.now()
            }
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        enchantsFound = false
    }
}
