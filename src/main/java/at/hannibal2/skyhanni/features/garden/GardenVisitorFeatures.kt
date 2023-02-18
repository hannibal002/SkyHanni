package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.server.S13PacketDestroyEntities
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.regex.Pattern

class GardenVisitorFeatures {

    private val pattern = Pattern.compile("(.*)§8x(.*)")
    private val visitors = mutableMapOf<String, Visitor>()
    private val display = mutableListOf<String>()
    private var lastClickedNpc = 0
    private var nearby = false

    @SubscribeEvent
    fun onChatPacket(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (!SkyHanniMod.feature.garden.visitorHelperDisplay &&
            !SkyHanniMod.feature.garden.visitorHelperHighlightReady
        ) return

        val npcItem = event.inventory.items[13] ?: return
        val lore = npcItem.getLore()
        var isVisitor = false
        if (lore.size == 4) {
            val line = lore[3]
            if (line.startsWith("§7Offers Accepted: §a")) {
                isVisitor = true
            }
        }
        if (!isVisitor) return

        val offerItem = event.inventory.items[29] ?: return
        if (offerItem.name != "§aAccept Offer") return

        val visitor = Visitor(lastClickedNpc)
        for (line in offerItem.getLore()) {
            if (line == "§7Items Required:") continue
            if (line.isEmpty()) break

            val matcher = pattern.matcher(line)
            if (!matcher.matches()) continue

            val itemName = matcher.group(1).trim()
            val amount = matcher.group(2).toInt()
            visitor.items[itemName] = amount
        }

        val visitorName = npcItem.name!!
        visitors[visitorName] = visitor

        update()
    }

    private fun update() {
        display.clear()

        val requiredItems = mutableMapOf<String, Int>()
        for ((_, visitor) in visitors) {
            for ((itemName, amount) in visitor.items) {
                val old = requiredItems.getOrDefault(itemName, 0)
                requiredItems[itemName] = old + amount
            }
        }
        if (requiredItems.isEmpty()) return

        display.add("Visitors need:")
        for ((name, amount) in requiredItems) {
            display.add(" -$name §8x$amount")
        }
    }

    var tick = 0

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onTooltip(event: ItemTooltipEvent) {
        if (!isEnabled()) return
        if (!nearby) return
        if (!SkyHanniMod.feature.garden.visitorHelperShowPrice) return

        val name = event.itemStack.name ?: return
        if (name != "§aAccept Offer") return

        var i = 0
        val list = event.toolTip
        var totalPrice = 0.0
        var amountDifferentItems = 0
        for (l in list) {
            val line = l.substring(4)
            if (line == "") {
                if (amountDifferentItems > 1) {
                    val format = NumberUtil.format(totalPrice)
                    list[1] = list[1] + "$line §f(§6$format§f)"
                }
                break
            }

            if (i > 1) {
                val matcher = pattern.matcher(line)
                if (matcher.matches()) {
                    val itemName = matcher.group(1).trim()
                    val amount = matcher.group(2).toInt()

                    val internalName = NEUItems.getInternalNameByName(itemName)
                    val auctionManager = NotEnoughUpdates.INSTANCE.manager.auctionManager
                    val lowestBin = auctionManager.getBazaarOrBin(internalName, false)
                    val price = lowestBin * amount
                    totalPrice += price
                    val format = NumberUtil.format(price)
                    list[i] = "$line §f(§6$format§f)"
                    amountDifferentItems++
                }
            }
            i++
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return
        if (!SkyHanniMod.feature.garden.visitorHelperDisplay &&
            !SkyHanniMod.feature.garden.visitorHelperHighlightReady &&
            !SkyHanniMod.feature.garden.visitorHelperShowPrice
        ) return
        if (tick++ % 60 != 0) return

        nearby = LocationUtils.playerLocation().distance(LorenzVec(8.4, 72.0, -14.1)) < 10

        if (nearby && SkyHanniMod.feature.garden.visitorHelperHighlightReady) {
            checkVisitorsReady()
        }
    }

    private fun checkVisitorsReady() {
        for (visitor in visitors.values) {
            var ready = true
            for ((name, need) in visitor.items) {
                val cleanName = name.removeColor()
                val having = InventoryUtils.countItemsInLowerInventory { it.name?.contains(cleanName) ?: false }
                if (having < need) {
                    ready = false
                }
            }

            if (ready) {
                val world = Minecraft.getMinecraft().theWorld
                val entity = world.getEntityByID(visitor.entityId)
                if (entity is EntityLivingBase) {
                    val color = LorenzColor.GREEN.toColor().withAlpha(120)
                    RenderLivingEntityHelper.setEntityColor(entity, color)
                    { SkyHanniMod.feature.garden.visitorHelperHighlightReady }
                }
            }
        }
    }

    @SubscribeEvent
    fun onReceiveEvent(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        if (packet is S13PacketDestroyEntities) {
            for (entityID in packet.entityIDs) {
                for ((name, visitor) in visitors.toMutableMap()) {
                    if (visitor.entityId == entityID) {
                        visitors.remove(name)
                        update()
                    }
                }
            }
        }
    }

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
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!isEnabled()) return
        if (!SkyHanniMod.feature.garden.visitorHelperDisplay) return
        if (!nearby) return

        SkyHanniMod.feature.garden.visitorHelperPos.renderStrings(display)
    }

    class Visitor(val entityId: Int, val items: MutableMap<String, Int> = mutableMapOf())

    private fun isEnabled() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.GARDEN
}