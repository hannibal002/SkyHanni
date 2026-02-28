package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorArrivalEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorLeftEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRefusedEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.isInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.editCopy
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.json.addElementsAfter
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.minecraft.world.item.ItemStack
import java.awt.Color

@SkyHanniModule
object VisitorApi {

    private var visitors = mapOf<String, Visitor>()
    var inInventory = false
    var lastClickedNpc = 0
    val config get() = GardenApi.config.visitors
    private val logger = LorenzLogger("garden/visitors/api")

    const val INFO_SLOT = 13
    const val ACCEPT_SLOT = 29
    const val REFUSE_SLOT = 33

    val patternGroup = RepoPattern.group("garden.visitor.api")

    /**
     * REGEX-TEST:  §r§aEmissary Carlton
     * REGEX-TEST:  §r§6Madame Eleanor Q. Goldsworth III
     * REGEX-TEST:  §r§9Lazy Miner
     */
    private val visitorNamePattern by patternGroup.pattern(
        "visitor.name",
        " (?:§.)+(?<name>§.[^§]+).*",
    )

    fun getVisitorsMap() = visitors
    fun getVisitors() = visitors.values
    fun getVisitor(id: Int) = visitors.map { it.value }.find { it.entityId == id }

    fun reset() {
        visitors = emptyMap()
    }

    fun changeStatus(visitor: Visitor, newStatus: VisitorStatus, reason: String) {
        val old = visitor.status
        if (old == newStatus) return
        visitor.status = newStatus
        logger.log("Visitor status change for '${visitor.visitorName}': $old -> $newStatus ($reason)")

        when (newStatus) {
            VisitorStatus.ACCEPTED -> {
                VisitorAcceptedEvent(visitor).post()
            }

            VisitorStatus.REFUSED -> {
                VisitorRefusedEvent(visitor).post()
            }

            else -> {}
        }
    }

    fun getOrCreateVisitor(name: String): Visitor? {
        var visitor = visitors[name]
        if (visitor == null) {
            // workaround if the tab list has not yet updated when opening the visitor
            addVisitor(name)
            ChatUtils.debug("Found visitor from npc that is not in tab list. Adding it still.")
            visitor = visitors[name]
        }

        if (visitor != null) return visitor

        ErrorManager.logErrorStateWithData(
            "Error finding the visitor `$name§c`. Try to reopen the inventory",
            "Visitor is null while opening visitor inventory",
            "name" to name,
            "visitors" to visitors,
        )
        return null
    }

    fun removeVisitor(name: String): Boolean {
        if (!visitors.containsKey(name)) return false
        val visitor = visitors[name] ?: return false
        visitors = visitors.editCopy { remove(name) }
        VisitorLeftEvent(visitor).post()
        return true
    }

    fun addVisitor(name: String): Boolean {
        if (visitors.containsKey(name)) return false
        val visitor = Visitor(name, status = VisitorStatus.NEW)
        visitors = visitors.editCopy { this[name] = visitor }
        VisitorArrivalEvent(visitor).post()
        return true
    }

    fun fromHypixelName(line: String): String {
        var name = line.trim().replace("§r", "").trim()
        if (!name.contains("§")) {
            name = "§f$name"
        }
        return name
    }

    fun isVisitorInfo(lore: List<String>): Boolean {
        if (lore.size != 4) return false
        return lore[3].startsWith("§7Offers Accepted: §a")
    }

    class VisitorOffer(
        val offerItem: ItemStack,
    )

    class Visitor(
        val visitorName: String,
        var entityId: Int = -1,
        var nameTagEntityId: Int = -1,
        var status: VisitorStatus,
        val shoppingList: MutableMap<NeuInternalName, Int> = mutableMapOf(),
        var offer: VisitorOffer? = null,
    ) {
        var offersAccepted: Int? = null
        var pricePerCopper: Int? = null
        var totalPrice: Double? = null
        var totalReward: Double? = null
        var lore: List<String> = emptyList()
        var allRewards = listOf<NeuInternalName>()
        var lastLore = listOf<String>()
        var blockedLore = listOf<Component>()
        var blockReason: VisitorBlockReason? = null

        fun getEntity() = EntityUtils.getEntityByID(entityId)
        fun getNameTagEntity() = EntityUtils.getEntityByID(nameTagEntityId)

        fun getRewardWarningAwards(): List<VisitorReward> = buildList {
            for (internalName in allRewards) {
                val reward = VisitorReward.getByInternalName(internalName) ?: continue
                if (reward in config.rewardWarning.drops) {
                    add(reward)
                }
            }
        }
    }

    enum class VisitorStatus(val displayName: String, val color: Color?) {
        NEW("§eNew", LorenzColor.YELLOW.toColor().addAlpha(100)),
        WAITING("Waiting", null),
        READY("§aItems Ready", LorenzColor.GREEN.toColor().addAlpha(80)),
        ACCEPTED("§7Accepted", LorenzColor.DARK_GRAY.toColor().addAlpha(80)),
        REFUSED("§cRefused", LorenzColor.RED.toColor().addAlpha(60)),
    }

    fun visitorsInTabList(tabList: List<Component>): List<String> {
        var visitorCount = 0
        var found = false
        var visitorsRemaining = 0

        val visitorsInTab = mutableListOf<String>()
        loop@ for (line in tabList) {
            TabWidget.VISITORS.pattern.matchMatcher(line) {
                found = true
                val countInfo = group("count")
                if (countInfo.isInt()) {
                    visitorCount = countInfo.toInt()
                }

                visitorsRemaining = visitorCount
                continue@loop
            }

            if (!found) continue
            if (visitorsRemaining <= 0) {
                found = false
                continue
            }

            visitorNamePattern.matchMatcher(line.formattedTextCompat()) {
                visitorsInTab.add(group("name").trim())
            }

            visitorsRemaining--
        }
        return visitorsInTab
    }

    fun Visitor.blockReason(): VisitorBlockReason? = with(config.rewardWarning) {
        val pricePerCopper = pricePerCopper ?: error("pricePerCopper is null")
        val totalPrice = totalPrice ?: error("totalPrice is null")
        val totalReward = totalReward ?: error("totalReward is null")
        val loss = totalPrice - totalReward
        return when {
            preventRefusing && getRewardWarningAwards().isNotEmpty() -> VisitorBlockReason.RARE_REWARD
            preventRefusingNew && !SkyBlockUtils.isBingoProfile && offersAccepted == 0 -> VisitorBlockReason.NEVER_ACCEPTED
            preventRefusingCopper && pricePerCopper <= coinsPerCopperPrice -> VisitorBlockReason.CHEAP_COPPER
            preventAcceptingCopper && pricePerCopper > coinsPerCopperPrice -> VisitorBlockReason.EXPENSIVE_COPPER
            preventRefusingLowLoss && loss <= coinsLossThreshold -> VisitorBlockReason.LOW_LOSS
            preventAcceptingHighLoss && loss > coinsLossThreshold -> VisitorBlockReason.HIGH_LOSS

            else -> null
        }
    }

    enum class VisitorBlockReason(val description: String, val blockRefusing: Boolean) {
        NEVER_ACCEPTED("§cNever accepted", true),
        RARE_REWARD("§aRare visitor reward found", true),
        CHEAP_COPPER("§aCheap copper", true),
        EXPENSIVE_COPPER("§cExpensive copper", false),
        LOW_LOSS("§aLow Loss", true),
        HIGH_LOSS("§cHigh Loss", false)
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Garden Visitor Stats")

        if (!GardenApi.inGarden()) {
            event.addIrrelevant("not in garden")
            return
        }

        event.addIrrelevant {
            val visitors = getVisitors()

            add("visitors: ${visitors.size}")

            for (visitor in visitors) {
                add(" ")
                add("visitorName: '${visitor.visitorName}'")
                add("status: '${visitor.status}'")
                if (visitor.shoppingList.isNotEmpty()) {
                    add("shoppingList: '${visitor.shoppingList}'")
                }
                visitor.offer?.offerItem?.getInternalName()?.let {
                    add("offer: '$it'")
                }
            }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.visitorNeedsDisplay", "garden.visitors.needs.display")
        event.move(3, "garden.visitorNeedsPos", "garden.visitors.needs.pos")
        event.move(3, "garden.visitorNeedsOnlyWhenClose", "garden.visitors.needs.onlyWhenClose")
        event.move(3, "garden.visitorNeedsInBazaarAlley", "garden.visitors.needs.inBazaarAlley")
        event.move(3, "garden.visitorNeedsShowPrice", "garden.visitors.needs.showPrice")
        event.move(3, "garden.visitorItemPreview", "garden.visitors.needs.itemPreview")
        event.move(3, "garden.visitorShowPrice", "garden.visitors.inventory.showPrice")
        event.move(3, "garden.visitorExactAmountAndTime", "garden.visitors.inventory.exactAmountAndTime")
        event.move(3, "garden.visitorCopperPrice", "garden.visitors.inventory.copperPrice")
        event.move(3, "garden.visitorCopperTime", "garden.visitors.inventory.copperTime")
        event.move(3, "garden.visitorExperiencePrice", "garden.visitors.inventory.experiencePrice")
        event.move(3, "garden.visitorRewardWarning.notifyInChat", "garden.visitors.rewardWarning.notifyInChat")
        event.move(3, "garden.visitorRewardWarning.showOverName", "garden.visitors.rewardWarning.showOverName")
        event.move(
            3,
            "garden.visitorRewardWarning.preventRefusing",
            "garden.visitors.rewardWarning.preventRefusing",
        )
        event.move(3, "garden.visitorRewardWarning.bypassKey", "garden.visitors.rewardWarning.bypassKey")
        event.move(3, "garden.visitorRewardWarning.drops", "garden.visitors.rewardWarning.drops")
        event.move(3, "garden.visitorNotificationChat", "garden.visitors.notificationChat")
        event.move(3, "garden.visitorNotificationTitle", "garden.visitors.notificationTitle")
        event.move(3, "garden.visitorHighlightStatus", "garden.visitors.highlightStatus")
        event.move(3, "garden.visitorColoredName", "garden.visitors.coloredName")
        event.move(3, "garden.visitorHypixelArrivedMessage", "garden.visitors.hypixelArrivedMessage")
        event.move(3, "garden.visitorHideChat", "garden.visitors.hideChat")

        event.transform(12, "garden.visitors.rewardWarning.drops") { element ->
            val drops = JsonArray()
            for (jsonElement in element.asJsonArray) {
                val old = jsonElement.asString
                val new = VisitorReward.entries.firstOrNull { old.startsWith(it.name) }
                if (new == null) {
                    println("error with migrating old VisitorReward entity: '$old'")
                    continue
                }
                drops.add(JsonPrimitive(new.name))
            }

            drops
        }
        event.transform(54, "garden.visitors.rewardWarning.drops") { element ->
            element.addElementsAfter(arrayOf(VisitorReward.COPPER_DYE))
        }
        event.transform(124, "garden.visitors.rewardWarning.drops") { element ->
            element.addElementsAfter(arrayOf(VisitorReward.DYE_WILD_STRAWBERRY))
        }

        event.move(18, "garden.visitors.needs", "garden.visitors.shoppingList")

        event.move(87, "garden.visitors.shoppingList.display", "garden.visitors.shoppingList.enabled")
        event.move(87, "garden.visitors.shoppingList.pos", "garden.visitors.shoppingList.position")
    }
}
