package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.CakeCounterConfig.OfflineStatsMode
import at.hannibal2.skyhanni.config.features.misc.CakeCounterConfig.OfflineTrackingMode
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CakeCounterFeatures {

    private val patternGroup = RepoPattern.group("misc.cakecounter")

    /**
     * REGEX-TEST: §7You placed a §r§eCake Counter§r§7. §r§7(9/15)
     */
    private val cakeCounterPlacedPattern by patternGroup.pattern(
        "placed",
        "§7You placed a §r§eCake Counter§r§7\\. §r§7\\([\\d\\/]+\\)",
    )

    /**
     * REGEX-TEST: §7You removed a §r§eCake Counter§r§7. (4/15)
     */
    private val cakeCounterRemovedPattern by patternGroup.pattern(
        "removed",
        "§7You removed a §r§eCake Counter§r§7\\. \\([\\d\\/]+\\)",
    )

    /**
     * REGEX-TEST: Cakes Eaten: §d9,453,416
     */
    private val cakesEatenPattern by patternGroup.pattern(
        "cakeseaten",
        "Cakes Eaten: §d(?<cakes>[\\d,]+)",
    )

    /**
     * REGEX-TEST: Souls Found: §b9,341
     */
    private val soulsFoundPattern by patternGroup.pattern(
        "soulsfound",
        "Souls Found: §b(?<souls>[\\d,]+)",
    )

    /**
     * REGEX-TEST: §eYou found a §r§dCake Soul§r§e!
     */
    private val cakeSoulFoundPattern by RepoPattern.pattern(
        "misc.cakesoul.found",
        "§eYou found a §r§dCake Soul§r§e!",
    )

    private val config get() = SkyHanniMod.feature.misc.cakeCounter
    private val storage get() = ProfileStorageData.profileSpecific?.cakeCounterData

    private var cakesEaten: Int
        get() = storage?.cakesEaten ?: -1
        set(value) {
            storage?.cakesEaten = value
        }

    private var soulsFound: Int
        get() = storage?.soulsFound ?: 0
        set(value) {
            storage?.soulsFound = value
        }

    private var cakesDifference: Int? = null
    private var soulsDifference: Int? = null
    private var statsToBeSent = true

    private var cakesEatenEntityId: Int? = null
    private var soulsFoundEntityId: Int? = null
    private var soulsStandExists: Boolean = true

    private var lastSoulFoundBySelf = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onEntityChangeName(event: EntityCustomNameUpdateEvent<EntityArmorStand>) {
        val entity = event.entity
        val name = entity.name
        val entityId = entity.entityId

        if (cakesEatenEntityId == null) {
            cakesEatenPattern.matchMatcher(name) {
                cakesEatenEntityId = entityId
                ChatUtils.debug("Found \"Cakes Eaten\" entity")

                // -1 means that cakesEaten has never been found before on this profile
                // stats should therefore not be sent as this likely means the Cake Counter has only just been placed
                statsToBeSent = statsToBeSent && cakesEaten != -1

                updateCakesEaten()

                DelayedRun.runDelayed(2.seconds) {
                    checkForSoulsStand(entity)
                    sendOfflineStatsMessage()
                }
            }
        }
        if (soulsFoundEntityId == null) {
            soulsFoundPattern.matchMatcher(name) {
                soulsFoundEntityId = entityId
                ChatUtils.debug("Found \"Souls Found\" entity")
                updateSoulsFound()
                sendOfflineStatsMessage()
            }
        }

        if (statsToBeSent) return
        when (entityId) {
            cakesEatenEntityId -> cakesEatenPattern.matchMatcher(name) { updateCakesEaten(true) }
            soulsFoundEntityId -> soulsFoundPattern.matchMatcher(name) { updateSoulsFound(true) }
        }
    }

    private fun checkForSoulsStand(cakesStand: EntityArmorStand) {
        if (soulsFoundEntityId != null) return // in case it was found during DelayedRun time

        val nearbyArmorStands = EntityUtils.getEntitiesNearby<EntityArmorStand>(cakesStand.position.toLorenzVec(), 1.0)
        soulsStandExists = nearbyArmorStands.any { armorStand ->
            soulsFoundPattern.matchMatcher(armorStand.name) {
                soulsFoundEntityId = armorStand.entityId
                ChatUtils.debug("Found \"Souls Found\" entity (from \"Cakes Eaten\" location)")
                updateSoulsFound()
                true
            } ?: false
        }
        if (!soulsStandExists) {
            ChatUtils.debug(
                "Couldn't find \"Souls Found\" entity near \"Cakes Eaten\" entity: " + "assuming it doesn't exist",
            )
        }
    }

    private fun Matcher.updateCakesEaten(happenedOnIsland: Boolean = false) {
        val currentCakes = group("cakes").formatInt()
        if (currentCakes > cakesEaten && !(happenedOnIsland && config.offlineTrackingMode == OfflineTrackingMode.SINCE_LAST_JOINED)) {
            cakesDifference = currentCakes - cakesEaten
            cakesEaten = currentCakes
            ChatUtils.debug("Updated cakesEaten to $cakesEaten")
        }
    }

    private fun Matcher.updateSoulsFound(happenedOnIsland: Boolean = false) {
        val currentSouls = group("souls").formatInt()
        if (currentSouls > soulsFound) {
            if (!happenedOnIsland || config.offlineTrackingMode != OfflineTrackingMode.SINCE_LAST_JOINED) {
                soulsDifference = currentSouls - soulsFound
                soulsFound = currentSouls
                ChatUtils.debug("Updated soulsFound to $soulsFound")
            }
            if (happenedOnIsland && config.soulFoundAlert && lastSoulFoundBySelf.passedSince() > 1.seconds) {
                ChatUtils.chat("Someone just found a Cake Soul on your Island!")
            }
        }
    }

    private fun sendOfflineStatsMessage() {
        if (!statsToBeSent) return
        if (cakesEatenEntityId == null || (soulsStandExists && soulsFoundEntityId == null)) return

        val noCakeDiff = cakesDifference == null
        val noSoulDiff = soulsDifference == null

        val shouldReturn = when (config.offlineStatsMode) {
            OfflineStatsMode.CAKES_ONLY -> noCakeDiff
            OfflineStatsMode.SOULS_ONLY -> noSoulDiff
            OfflineStatsMode.BOTH -> noCakeDiff && noSoulDiff
            OfflineStatsMode.DISABLED -> true
        }

        if (shouldReturn) return

        ChatUtils.chat("${buildMessage()}.")
        statsToBeSent = false
    }

    private fun buildMessage(): String = buildString {
        append("Since you ")
        when (config.offlineTrackingMode) {
            OfflineTrackingMode.SINCE_LAST_LEFT -> append("were last on your Private Island, ")
            OfflineTrackingMode.SINCE_LAST_JOINED -> append("last joined your Private Island, ")
        }

        val cakesMessage = cakesDifference?.let {
            "ate §d${it.addSeparators()}§e ${StringUtils.pluralize(it, "Century Cake")}"
        }

        val soulsMessage = soulsDifference?.let {
            "found §b${it.addSeparators()}§e ${StringUtils.pluralize(it, "Cake Soul")}"
        }

        when (config.offlineStatsMode) {
            OfflineStatsMode.CAKES_ONLY -> append("players ${cakesMessage ?: return@buildString}")
            OfflineStatsMode.SOULS_ONLY -> append("players ${soulsMessage ?: return@buildString}")
            OfflineStatsMode.BOTH -> {
                val combinedMessage = listOfNotNull(cakesMessage, soulsMessage).joinToString(" and ")
                append("players $combinedMessage")
            }

            OfflineStatsMode.DISABLED -> return@buildString
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onChat(event: SkyHanniChatEvent) {
        if (cakeSoulFoundPattern.matches(event.message)) {
            lastSoulFoundBySelf = SimpleTimeMark.now()
        }

        if (cakeCounterRemovedPattern.matches(event.message)) {
            cakesEatenEntityId?.let {
                if (EntityUtils.getEntityByID(it) == null) {
                    cakesEatenEntityId = null
                    ChatUtils.debug("Discarded stored entityId of \"Cakes Eaten\" armor stand.")
                }
            }
            soulsFoundEntityId?.let {
                if (EntityUtils.getEntityByID(it) == null) {
                    soulsFoundEntityId = null
                    ChatUtils.debug("Discarded stored entityId of \"Souls Found\" armor stand.")
                }
            }
        }

        if (config.offlineStatsMode != OfflineStatsMode.DISABLED) return
        if (cakeCounterPlacedPattern.matches(event.message)) {
            DelayedRun.runNextTick {
                ChatUtils.chatAndOpenConfig(
                    "Click here to be notified of any stat changes on your Cake Counter every time you rejoin your Private Island.",
                    config::offlineStatsMode,
                )
            }
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        cakesEatenEntityId = null
        soulsFoundEntityId = null
        statsToBeSent = true
        cakesDifference = null
        soulsDifference = null
    }
}
