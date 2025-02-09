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
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import java.util.regex.Matcher

@SkyHanniModule
object OfflineCakeCounter {

    private val patternGroup = RepoPattern.group("misc.cakecounter")

    /**
     * REGEX-TEST: §7You placed a §r§eCake Counter§r§7. §r§7(9/15)
     */
    private val cakeCounterPlacedPattern by patternGroup.pattern(
        "placed",
        "§7You placed a §r§eCake Counter§r§7\\. §r§7\\([\\d/]+\\)",
    )

    /**
     * REGEX-TEST: §7You removed a §r§eCake Counter§r§7. (4/15)
     */
    private val cakeCounterRemovedPattern by patternGroup.pattern(
        "removed",
        "§7You removed a §r§eCake Counter§r§7\\. §r§7\\([\\d/]+\\)",
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

    private var newCakesEaten: Int? = null
    private var newSoulsFound: Int? = null

    private var statsToBeSent = true

    private var cakesEatenEntityId: Int? = null
    private var soulsFoundEntityId: Int? = null
    private var soulsStandExists: Boolean = true

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onEntityJoin(event: EntityEnterWorldEvent<EntityArmorStand>) {
        if (cakesEatenEntityId != null && soulsFoundEntityId != null) return

        val entity = event.entity

        ChatUtils.debug(entity.name)

        cakesEatenPattern.matchMatcher(entity.name) {
            cakesEatenEntityId = entity.entityId
            ChatUtils.debug("Found \"Cakes Eaten\" entity.")

            // -1 means that cakesEaten has never been found before on this profile
            // stats should therefore not be sent as this likely means the Cake Counter has only just been placed
            statsToBeSent = cakesEaten == -1

            this.updateCakesEaten()

            val nearbyArmorStands = EntityUtils.getEntitiesNearby<EntityArmorStand>(entity.position.toLorenzVec(), 0.1)
            soulsStandExists = nearbyArmorStands.any { armorStand -> soulsFoundPattern.matches(armorStand.name) }
            sendOfflineStatsMessage()
        }

        soulsFoundPattern.matchMatcher(entity.name) {
            soulsFoundEntityId = entity.entityId
            ChatUtils.debug("Found \"Souls Found\" entity.")
            this.updateSoulsFound()
            sendOfflineStatsMessage()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onEntityChangeName(event: EntityCustomNameUpdateEvent<EntityArmorStand>) {
        val entity = event.entity
        val entityID = entity.entityId
        if (entityID != cakesEatenEntityId && entityID != soulsFoundEntityId) return

        val name = entity.name
        cakesEatenPattern.matchMatcher(name) { this.updateCakesEaten(true) }
        soulsFoundPattern.matchMatcher(name) { this.updateSoulsFound(true) }
    }

    private fun Matcher.updateCakesEaten(happenedOnIsland: Boolean = false) {
        newCakesEaten = group("cakes").formatInt()
        newCakesEaten?.let {
            if (it > cakesEaten && !(happenedOnIsland && config.offlineTrackingMode == OfflineTrackingMode.SINCE_LAST_JOINED)) {
                cakesEaten = it
                ChatUtils.debug("Updated cakesEaten to $cakesEaten")
            }
        }
    }

    private fun Matcher.updateSoulsFound(happenedOnIsland: Boolean = false) {
        newSoulsFound = group("souls").formatInt()
        newSoulsFound?.let {
            if (it > soulsFound) {
                if (!happenedOnIsland || config.offlineTrackingMode != OfflineTrackingMode.SINCE_LAST_JOINED) {
                    soulsFound = it
                }
                if (happenedOnIsland && config.soulFoundAlert) {
                    ChatUtils.chat("Someone just found a Cake Soul on your Island!")
                }
                ChatUtils.debug("Updated soulsFound to $soulsFound")
            }
        }
    }

    private fun sendOfflineStatsMessage() {
        if (cakesEatenEntityId == null || (soulsStandExists && soulsFoundEntityId == null)) return
        if (newCakesEaten == null) return

        if (statsToBeSent && config.offlineStatsMode != OfflineStatsMode.DISABLED) {
            val newCakesEaten = newCakesEaten ?: return
            val cakeDifference = newCakesEaten - cakesEaten
            val cakesFormat = StringUtils.pluralize(cakeDifference, "Century Cake")

            var message = "Since you "
            if (config.offlineTrackingMode == OfflineTrackingMode.SINCE_LAST_LEFT) {
                message += "were last on your Private Island, "
            } else if (config.offlineTrackingMode == OfflineTrackingMode.SINCE_LAST_JOINED) {
                message += "last joined your Private Island, "
            }
            message += "players ate §d$cakeDifference§e $cakesFormat"
            if (newSoulsFound != null) {
                val newSoulsFound = newSoulsFound ?: return
                val soulDifference = newSoulsFound - soulsFound
                val soulsFormat = StringUtils.pluralize(soulDifference, "Cake Soul")
                message += " and found §b$soulDifference§e $soulsFormat"
            }
            ChatUtils.chat("$message.")
            statsToBeSent = false
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onChat(event: SkyHanniChatEvent) {
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
        newCakesEaten = null
        newSoulsFound = null
    }
}
