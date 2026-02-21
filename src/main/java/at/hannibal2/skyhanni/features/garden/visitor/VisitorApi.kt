package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorArrivalEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorCountChangeEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorLeftEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorListChangeEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRefusedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorNextArrivalChangeEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@SkyHanniModule
object VisitorApi {

    private val visitors = ConcurrentHashMap<String, Visitor>()

    var inInventory = false
    var lastClickedNpc = 0
    var visitorCount = 0
        private set(value) {
            if (visitorCount == value) return
            field = value
            VisitorCountChangeEvent(value).post()
        }
    var queueFull = false
        private set
    var visitorsUnlocked = true
        private set
    private var visitorJustArrived = false
    private var nextVisitorLastUpdated: SimpleTimeMark = SimpleTimeMark.farPast()
    private var nextVisitorArrivalDuration: Duration? = null
    private val nextVisitorArrivalMark: SimpleTimeMark get() = nextVisitorArrivalDuration?.let {
        SimpleTimeMark.now() + it
    } ?: SimpleTimeMark.farFuture()
    private var visitorInterval: Duration?
        get() = GardenApi.storage?.visitorInterval?.toDuration(DurationUnit.MILLISECONDS)
        set(value) {
            value?.let {
                GardenApi.storage?.visitorInterval = it.inWholeMilliseconds
            }
        }

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
     * REGEX-TEST:  §r§6Mayor Cole §r§fWild Rose §r§b§lNEW!
     * REGEX-TEST:  §r§aEmissary Ceanna §r§fPotato
     * REGEX-TEST:  §r§aFarmhand §r§fCarrot
     * REGEX-TEST:  §r§dMaeve §r§5Fermento
     */
    private val visitorNamePattern by patternGroup.pattern(
        "visitor.name",
        "^ (?:§.)+(?<name>§.[\\w. ]+(?:\\w|\\w(?! ))) *(?:(?:§.)+(?<crop>[\\w ]+)?)?(?: (?:§.)+(?<new>NEW!))?\$",
    )

    /**
     * REGEX-TEST:  Next Visitor: §r§b11m
     * REGEX-TEST:  Next Visitor: §r§c§lQueue Full!
     */
    private val timePattern by RepoPattern.pattern(
        "garden.visitor.timer.time.new",
        " Next Visitor: §r(?<info>.*)",
    )

    fun getVisitorsMap() = visitors
    fun getVisitors() = visitors.values
    fun getVisitor(id: Int) = visitors.map { it.value }.find { it.entityId == id }
    fun getVisitor(name: String) = visitors[name]

    fun isVisitorPresent(name: String) = visitors.keys.any { it.removeColor() == name }

    fun reset() {
        visitors.clear()
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
        visitors.remove(name)
        VisitorLeftEvent(visitor).post()
        return true
    }

    fun addVisitor(name: String, cropInternalName: NeuInternalName? = null): Boolean {
        if (visitors.containsKey(name)) return false
        val visitor = Visitor(name, status = VisitorStatus.NEW, tabCrop = cropInternalName)
        visitors[name] = visitor
        VisitorArrivalEvent(visitor).post()
        return true
    }

    fun fromHypixelName(line: String): String {
        var name = line.trim().replace("§r", "").trim()
        if (!name.contains("§")) name = "§f$name"
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
        val tabCrop: NeuInternalName? = null,
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

        fun getRewardWarningAwards() = allRewards.mapNotNull {
            val reward = VisitorReward.getByInternalName(it) ?: return@mapNotNull null
            if (reward in config.rewardWarning.drops) reward else null
        }
    }

    enum class VisitorStatus(val displayName: String, val color: Color?) {
        NEW("§eNew", LorenzColor.YELLOW.toColor().addAlpha(100)),
        NEW_NEW("§bNEW!", LorenzColor.AQUA.toColor().addAlpha(120)),
        WAITING("Waiting", null),
        READY("§aItems Ready", LorenzColor.GREEN.toColor().addAlpha(80)),
        ACCEPTED("§7Accepted", LorenzColor.DARK_GRAY.toColor().addAlpha(80)),
        REFUSED("§cRefused", LorenzColor.RED.toColor().addAlpha(60)),
    }

    @HandleEvent(ProfileJoinEvent::class)
    fun onProfileJoin() {
        nextVisitorLastUpdated = SimpleTimeMark.farPast()
        visitorJustArrived = false
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onVisitorArrival(event: VisitorArrivalEvent) {
        visitorJustArrived = true
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onPestKill(event: PestKillEvent) {
        nextVisitorArrivalDuration?.let { if (it <= 5.minutes) return } ?: return
        nextVisitorArrivalDuration = nextVisitorArrivalDuration?.minus(30.seconds)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onCropClick(event: CropClickEvent) {
        nextVisitorArrivalDuration?.let { if (it <= 5.minutes) return } ?: return
        nextVisitorArrivalDuration = nextVisitorArrivalDuration?.minus(100.milliseconds)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.VISITORS) || event.isClear()) return
        val formattedLines = event.lines.map { it.formattedTextCompat() }
        val beforeHash = visitors.hashCode()
        val beforeVisitorCount = visitors.values.size

        val headerLine = formattedLines.firstOrNull() ?: return
        visitorCount = TabWidget.VISITORS.pattern.matchMatcher(headerLine) {
            groupOrNull("count")?.toIntOrNull()
        } ?: return

        visitorNamePattern.matchAll(formattedLines.drop(1).slice(0 until (visitorCount))) {
            val cropInternalName = groupOrNull("crop")?.let {
                NeuInternalName.fromItemName(it.removeColor())
            }
            addVisitor(group("name"), cropInternalName)
        }

        timePattern.firstMatcher(formattedLines) {
            when (val timeInfo = group("info").removeColor()) {
                "Not Unlocked!" -> { visitorsUnlocked = false }
                "Queue Full!" -> { queueFull = true }
                else -> {
                    val convertedTime = TimeUtils.getDurationOrNull(timeInfo) ?: return@firstMatcher
                    if (convertedTime < Duration.ZERO || convertedTime == nextVisitorArrivalDuration) return@firstMatcher
                    nextVisitorLastUpdated = SimpleTimeMark.now()
                    nextVisitorArrivalDuration = convertedTime
                }
            }
        }

        // Todo fire WidgetNotFound event when time pattern does not match

        val newHash = visitors.hashCode()
        if (beforeHash != newHash) VisitorListChangeEvent(getVisitors().toList()).post()

        updateSixthVisitorTimer(beforeVisitorCount)
    }

    private fun updateSixthVisitorTimer(beforeVisitorCount: Int) {
        val visitorInterval = this.visitorInterval ?: return
        val beforeArrivalDuration = nextVisitorArrivalDuration ?: 0.seconds

        if (beforeVisitorCount != -1 && visitorCount - beforeVisitorCount == 1) {
            if (!queueFull) this.visitorInterval = nextVisitorArrivalDuration
            else nextVisitorArrivalDuration = (SimpleTimeMark.now() + visitorInterval).timeUntil()
        }

        if (queueFull) {
            if (visitorJustArrived && visitorCount - beforeVisitorCount == 1) {
                visitorJustArrived = false
                nextVisitorArrivalDuration = (SimpleTimeMark.now() + visitorInterval).timeUntil()
            }

            GardenApi.storage?.nextSixthVisitorArrival = nextVisitorArrivalMark
            if (nextVisitorArrivalMark.isInPast()) visitorCount++
        }

        val sinceLastTimerUpdate = nextVisitorLastUpdated.passedSince() - 100.milliseconds
        val guessTime = visitorCount < 5 && sinceLastTimerUpdate in 500.milliseconds..60.seconds
        if (guessTime) nextVisitorArrivalDuration = nextVisitorArrivalDuration?.minus(sinceLastTimerUpdate)

        if (nextVisitorArrivalDuration == INFINITE) {
            ErrorManager.logErrorStateWithData(
                "Found Visitor Timer bug, reset value", "nextVisitorArrivalDuration was infinite",
                "nextVisitorArrivalDuration" to nextVisitorArrivalDuration,
            )
            nextVisitorArrivalDuration = 0.seconds
        }

        val afterArrivalDuration = nextVisitorArrivalDuration ?: return
        val diff = beforeArrivalDuration - afterArrivalDuration
        if (diff == 0.seconds && visitorCount == beforeVisitorCount) return
        VisitorNextArrivalChangeEvent(SimpleTimeMark.now() + afterArrivalDuration).post()
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
}
