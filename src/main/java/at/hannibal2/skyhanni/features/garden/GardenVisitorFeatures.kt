package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.*
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
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class GardenVisitorFeatures {

    private val visitors = mutableMapOf<String, Visitor>()
    private val display = mutableListOf<String>()
    private var lastClickedNpc = 0
    private var nearby = false

    @SubscribeEvent
    fun onChatPacket(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (!SkyHanniMod.feature.garden.visitorNeedsDisplay &&
            !SkyHanniMod.feature.garden.visitorHighlightReady
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

            val (itemName, amount) = ItemUtils.readItemAmount(line)
            if (itemName == null) continue
            visitor.items[itemName] = amount
        }

        val visitorName = npcItem.name!!
        visitors[visitorName] = visitor

        update()
    }

    private fun update() {
        display.clear()

        val requiredItems = mutableMapOf<String, Int>()
        val newVisitors = mutableListOf<String>()
        for ((visitorName, visitor) in visitors) {
            val items = visitor.items
            if (items.isEmpty()) {
                newVisitors.add(visitorName)
            }
            for ((itemName, amount) in items) {
                val old = requiredItems.getOrDefault(itemName, 0)
                requiredItems[itemName] = old + amount
            }
        }
        if (requiredItems.isNotEmpty()) {
            display.add("Visitor items needed:")
            for ((name, amount) in requiredItems) {
                display.add(" -$name §8x$amount")
            }
        }
        if (newVisitors.isNotEmpty()) {
            if (requiredItems.isNotEmpty()) {
                display.add("")
            }
            val amount = newVisitors.size
            val visitorLabel = if (amount == 1) "visitor" else "visitors"
            display.add("$amount new $visitorLabel:")
            for (visitor in newVisitors) {
                display.add(" -$visitor")
            }
        }
    }

    var tick = 0

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onTooltip(event: ItemTooltipEvent) {
        if (!isEnabled()) return
        if (!nearby) return
        if (!SkyHanniMod.feature.garden.visitorShowPrice) return

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
                    list[1] = list[1] + "$line §f(§6Total §6$format§f)"
                }
                break
            }

            if (i > 1) {
                val (itemName, amount) = ItemUtils.readItemAmount(line)
                if (itemName != null) {
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
        if (!SkyHanniMod.feature.garden.visitorNeedsDisplay &&
            !SkyHanniMod.feature.garden.visitorHighlightReady &&
            !SkyHanniMod.feature.garden.visitorShowPrice
        ) return
        if (tick++ % 60 != 0) return

        val defaultVanillaSkin = LorenzVec(8.4, 72.0, -14.1)
        val castleSkin = LorenzVec(-5, 75, 18)
        val bambooSkin = LorenzVec(-12, 72, -25)
        val hiveSkin = LorenzVec(-17, 71, -19)
        val cubeSkin = LorenzVec(-17, 71, -19)

        // TODO Only check current one, ignore others.
        val list = mutableListOf<LorenzVec>()
        list.add(defaultVanillaSkin)
        list.add(castleSkin)
        list.add(bambooSkin)
        list.add(hiveSkin)
        list.add(cubeSkin)

        val playerLocation = LocationUtils.playerLocation()
        nearby = list.map { playerLocation.distance(it) < 15 }.any { it }


        if (nearby && SkyHanniMod.feature.garden.visitorHighlightReady) {
            checkVisitorsReady()
        }
    }

    @SubscribeEvent
    fun onTick(event: TabListUpdateEvent) {
        if (!isEnabled()) return
        var found = false
        val visitorsInTab = mutableListOf<String>()
        for (line in event.tabList) {
            if (line.startsWith("§b§lVisitors:")) {
                found = true
                continue
            }
            if (found) {
                if (line.isEmpty()) {
                    found = false
                    continue
                }
                val name = line.substring(3)
                visitorsInTab.add(name)
            }
        }
        if (visitors.keys.removeIf { it !in visitorsInTab }) {
            println("removed an npc")
            update()
        }
        for (name in visitorsInTab) {
            if (!visitors.containsKey(name)) {
                visitors[name] = Visitor(-1)
                // todo notification? (check world age first)
                println("found an npc: '$name'")
                update()
            }
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
                    { SkyHanniMod.feature.garden.visitorHighlightReady }
                }
            }
        }
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
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (!SkyHanniMod.feature.garden.visitorNeedsDisplay) return

        if (SkyHanniMod.feature.garden.visitorNeedsOnlyWhenClose) {
            if (!nearby) return
        }

        SkyHanniMod.feature.garden.visitorNeedsPos.renderStrings(display)
    }

    class Visitor(val entityId: Int, val items: MutableMap<String, Int> = mutableMapOf())

    private fun isEnabled() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.GARDEN
}