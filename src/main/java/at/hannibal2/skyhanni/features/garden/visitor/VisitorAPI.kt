package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorArrivalEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorLeftEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRefusedEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.NEUInternalName
import net.minecraft.item.ItemStack

object VisitorAPI {
    private var visitors = mapOf<String, Visitor>()
    var inInventory = false
    val config get() = GardenAPI.config.visitors
    private val logger = LorenzLogger("garden/visitors/api")

    fun getVisitorsMap() = visitors
    fun getVisitors() = visitors.values
    fun getVisitor(id: Int) = visitors.map { it.value }.find { it.entityId == id }
    fun getVisitor(name: String) = visitors[name]

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
                VisitorAcceptedEvent(visitor).postAndCatch()
            }

            VisitorStatus.REFUSED -> {
                VisitorRefusedEvent(visitor).postAndCatch()
            }

            else -> {}
        }
    }

    fun getOrCreateVisitor(name: String): Visitor? {
        var visitor = visitors[name]
        if (visitor == null) {
            // workaround if the tab list has not yet updated when opening the visitor
            addVisitor(name)
            LorenzUtils.debug("Found visitor from npc that is not in tab list. Adding it still.")
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
        VisitorLeftEvent(visitor).postAndCatch()
        return true
    }

    fun addVisitor(name: String): Boolean {
        if (visitors.containsKey(name)) return false
        val visitor = Visitor(name, status = VisitorStatus.NEW)
        visitors = visitors.editCopy { this[name] = visitor }
        VisitorArrivalEvent(visitor).postAndCatch()
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
        val offerItem: ItemStack
    )

    class Visitor(
        val visitorName: String,
        var entityId: Int = -1,
        var nameTagEntityId: Int = -1,
        var status: VisitorStatus,
        var inSacks: Boolean = false,
        val items: MutableMap<NEUInternalName, Int> = mutableMapOf(),
        var offer: VisitorOffer? = null,
    ) {
        var pricePerCopper: Int = -1
        var lore: List<String> = emptyList()
        var allRewards = listOf<NEUInternalName>()
        var lastLore = listOf<String>()

        fun getEntity() = EntityUtils.getEntityByID(entityId)
        fun getNameTagEntity() = EntityUtils.getEntityByID(nameTagEntityId)

        fun hasReward(): VisitorReward? {
            for (internalName in allRewards) {
                val reward = VisitorReward.getByInternalName(internalName) ?: continue
                if (reward in config.rewardWarning.drops) {
                    return reward
                }
            }

            return null
        }
    }

    enum class VisitorStatus(val displayName: String, val color: Int) {
        NEW("§eNew", LorenzColor.YELLOW.toColor().withAlpha(100)),
        WAITING("Waiting", -1),
        READY("§aItems Ready", LorenzColor.GREEN.toColor().withAlpha(80)),
        ACCEPTED("§7Accepted", LorenzColor.DARK_GRAY.toColor().withAlpha(80)),
        REFUSED("§cRefused", LorenzColor.RED.toColor().withAlpha(60)),
    }

    fun visitorsInTabList(tabList: List<String>): MutableList<String> {
        var found = false
        val visitorsInTab = mutableListOf<String>()
        for (line in tabList) {
            if (line.startsWith("§b§lVisitors:")) {
                found = true
                continue
            }
            if (!found) continue

            if (line.isEmpty() || line.contains("Account Info")) {
                found = false
                continue
            }
            val name = fromHypixelName(line)

            // Hide hypixel watchdog entries
            if (name.contains("§c") && !name.contains("Spaceman") && !name.contains("Grandma Wolf")) {
                logger.log("Ignore wrong red name: '$name'")
                continue
            }

            //hide own player name
            if (name.contains(LorenzUtils.getPlayerName())) {
                logger.log("Ignore wrong own name: '$name'")
                continue
            }

            visitorsInTab.add(name)
        }
        return visitorsInTab
    }
}
