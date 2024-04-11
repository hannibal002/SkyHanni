package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.config.features.garden.visitor.VisitorConfig
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorOpenEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRenderEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI.ACCEPT_SLOT
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI.INFO_SLOT
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI.lastClickedNpc
import at.hannibal2.skyhanni.mixins.transformers.gui.AccessorGuiContainer
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.exactLocation
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import kotlin.time.Duration.Companion.seconds

class VisitorListener {
    private val offersAcceptedPattern by RepoPattern.pattern(
        "garden.visitor.offersaccepted",
        "§7Offers Accepted: §a(?<offersAccepted>\\d+)"
    )

    private val config get() = VisitorAPI.config

    private val logger = LorenzLogger("garden/visitors/listener")


    companion object {
        private val VISITOR_INFO_ITEM_SLOT = 13
        private val VISITOR_ACCEPT_ITEM_SLOT = 29
        private val VISITOR_REFUSE_ITEM_SLOT = 33
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        VisitorAPI.reset()
    }

    // TODO make event
    @SubscribeEvent
    fun onSendEvent(event: PacketEvent.SendEvent) {
        val packet = event.packet
        if (packet !is C02PacketUseEntity) return

        val theWorld = Minecraft.getMinecraft().theWorld
        val entity = packet.getEntityFromWorld(theWorld) ?: return
        val entityId = entity.entityId

        lastClickedNpc = entityId
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!GardenAPI.inGarden()) return

        val hasVisitorInfo = event.tabList.any { VisitorAPI.visitorCountPattern.matches(it) }
        if (!hasVisitorInfo) return

        val visitorsInTab = VisitorAPI.visitorsInTabList(event.tabList)

        if (LorenzUtils.lastWorldSwitch.passedSince() > 2.seconds) {
            VisitorAPI.getVisitors().forEach {
                val name = it.visitorName
                val removed = name !in visitorsInTab
                if (removed) {
                    logger.log("Removed old visitor: '$name'")
                    VisitorAPI.removeVisitor(name)
                }
            }
        }

        for (name in visitorsInTab) {
            VisitorAPI.addVisitor(name)
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!GardenAPI.inGarden()) return
        val npcItem = event.inventoryItems[INFO_SLOT] ?: return
        val lore = npcItem.getLore()
        if (!VisitorAPI.isVisitorInfo(lore)) return

        val offerItem = event.inventoryItems[ACCEPT_SLOT] ?: return
        if (offerItem.name != "§aAccept Offer") return

        VisitorAPI.inInventory = true

        val visitorOffer = VisitorAPI.VisitorOffer(offerItem)

        var name = npcItem.name
        if (name.length == name.removeColor().length + 4) {
            name = name.substring(2)
        }

        val visitor = VisitorAPI.getOrCreateVisitor(name) ?: return

        visitor.offersAccepted = offersAcceptedPattern.matchMatcher(lore[3]) { group("offersAccepted").toInt() }
        visitor.entityId = lastClickedNpc
        visitor.offer = visitorOffer
        VisitorOpenEvent(visitor).postAndCatch()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        VisitorAPI.inInventory = false
    }

    @SubscribeEvent
    fun onKeybind(event: GuiKeyPressEvent) {
        if (!VisitorAPI.inInventory) return
        if (!config.acceptHotkey.isKeyHeld()) return
        val inventory = event.guiContainer as? AccessorGuiContainer ?: return
        inventory as GuiContainer
        val slot = inventory.inventorySlots.getSlot(29)
        inventory.handleMouseClick_skyhanni(slot, slot.slotIndex, 0, 0)
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!VisitorAPI.inInventory) return
        if (event.clickType != 0) return

        val visitor = VisitorAPI.getVisitor(lastClickedNpc) ?: return

        if (event.slotId == VISITOR_REFUSE_ITEM_SLOT) {
            if (event.slot?.stack?.name != "§cRefuse Offer") return

            visitor.hasReward()?.let {
                if (config.rewardWarning.preventRefusing) {
                    if (config.rewardWarning.bypassKey.isKeyHeld()) {
                        ChatUtils.chat("§cBypassed blocking refusal of visitor ${visitor.visitorName} §7(${it.displayName}§7)")
                        return
                    }
                    event.isCanceled = true
                    ChatUtils.chat("§cBlocked refusing visitor ${visitor.visitorName} §7(${it.displayName}§7)")
                    if (config.rewardWarning.bypassKey == Keyboard.KEY_NONE) {
                        ChatUtils.chatAndOpenConfig(
                            "§eIf you want to deny this visitor, set a keybind in §e/sh bypass",
                            GardenAPI.config.visitors.rewardWarning::bypassKey
                        )
                    }
                    Minecraft.getMinecraft().thePlayer.closeScreen()
                    return
                }
            }

            VisitorAPI.changeStatus(visitor, VisitorAPI.VisitorStatus.REFUSED, "refused")
            // fallback if tab list is disabled
            DelayedRun.runDelayed(10.seconds) {
                VisitorAPI.removeVisitor(visitor.visitorName)
            }
            return
        }
        if (event.slotId == VISITOR_ACCEPT_ITEM_SLOT && event.slot?.stack?.getLore()
                ?.any { it == "§eClick to give!" } == true
        ) {
            VisitorAPI.changeStatus(visitor, VisitorAPI.VisitorStatus.ACCEPTED, "accepted")
            return
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onTooltip(event: ItemTooltipEvent) {
        if (!GardenAPI.onBarnPlot) return
        if (!VisitorAPI.inInventory) return
        val visitor = VisitorAPI.getVisitor(lastClickedNpc) ?: return
        GardenVisitorFeatures.onTooltip(visitor, event.itemStack, event.toolTip)
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!GardenAPI.inGarden()) return
        if (!GardenAPI.onBarnPlot) return
        if (config.highlightStatus != VisitorConfig.HighlightMode.NAME && config.highlightStatus != VisitorConfig.HighlightMode.BOTH) return

        val entity = event.entity
        if (entity is EntityArmorStand && entity.name == "§e§lCLICK") {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!GardenAPI.inGarden()) return
        if (!GardenAPI.onBarnPlot) return
        if (config.highlightStatus != VisitorConfig.HighlightMode.NAME && config.highlightStatus != VisitorConfig.HighlightMode.BOTH) return

        for (visitor in VisitorAPI.getVisitors()) {
            visitor.getNameTagEntity()?.let {
                if (it.distanceToPlayer() > 15) return@let
                VisitorRenderEvent(visitor, event.exactLocation(it), event).postAndCatch()
            }
        }
    }
}
