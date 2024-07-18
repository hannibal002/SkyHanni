package at.hannibal2.skyhanni.features.garden.tracking

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.Crop
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.InformationType
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.Medal
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.Medal.BRONZE_MEDAL
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.Medal.DIAMOND_MEDAL
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.Medal.GOLD_MEDAL
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.Medal.PLATINUM_MEDAL
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.Medal.SILVER_MEDAL
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.MessageType.EDITED_MESSAGE
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.MessageType.NEW_MESSAGE
import at.hannibal2.skyhanni.data.Embed
import at.hannibal2.skyhanni.data.Field
import at.hannibal2.skyhanni.data.Footer
import at.hannibal2.skyhanni.data.Mayor
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.Thumbnail
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestAPI
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.WebhookUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object FarmingTracker {

    val storage get() = ProfileStorageData.playerSpecific
    val config get() = SkyHanniMod.feature.garden.tracking

    var lastNotification = SimpleTimeMark.farPast()
    var playerFaceURL = ""
    var stats = mutableMapOf<String, Int>()
    var lastTablist = listOf<String>()
    var cookieBuffTimer = ""
    var godPotionTimer = ""
    var activeAnitaBuff = ""
    var currentCrop: Crop? = null
    var currentPlacement = 0.0
    val farmingTrackerURL = "https://cdn.discordapp.com/attachments/1263194220630638714/1263216589898252359/FT.png?" +
        "ex=66996da0&is=66981c20&hm=b50b1993a4aa9485b2b161b61830668451ffdb84002ee2a0d7886d8e6e7f5cc7&"


    private val patternGroup = RepoPattern.group("garden.tracking")
    private val webhookPattern by patternGroup.pattern(
        "webhook",
        "https://discord\\.com/api/webhooks/(?<id>\\d+)/(?<token>\\S+)",
    )

    private val tablistStatsPattern by patternGroup.pattern(
        "tablist.stats",
        "^ (?<stat>[^:]+): .?(?<amount>\\d+)\$",
    )

    private val tablistUpcomingContestPattern by patternGroup.pattern(
        "tablist.contest.upcoming",
        "^ ☘ (?<crop>.+)\$",
    )

    private val tablistActiveContestPattern by patternGroup.pattern(
        "tablist.contest.active",
        "^ (?<boost>[☘○]) (?<crop>.+) ◆ Top (?<placement>\\S+)%\$",
    )

    private val tablistEffectsPattern by patternGroup.pattern(
        "tablist.effects",
        "^ (?<type>Cookie Buff|God Potion): (?<duration>.+)\$",
    )

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (lastNotification.passedSince() < config.webhook.interval.minutes) return

        if (playerFaceURL.isBlank()) playerFaceURL = "https://api.mineatar.io/face/${LorenzUtils.getPlayerUuid()}?scale=12"

        val status = if (GardenAPI.isCurrentlyFarming()) "Farming" else "Idle"

        val color = 16777045
        val fields = config.information
            .filter { it.isSelected() }
            .mapNotNull { type ->

                val value = when (type) {
                    InformationType.FARMING_FORTUNE -> stats["Farming Fortune"]
                    InformationType.FARMING_WISDOM -> stats["Farming Wisdom"]
                    InformationType.BONUS_PEST_CHANCE -> stats["Bonus Pest Chance"]
                    InformationType.SPEED -> stats["Speed"]
                    InformationType.STRENGTH -> stats["Strength"]
                    InformationType.PET -> PetAPI.currentPet
                    InformationType.COOKIE_BUFF -> cookieBuffTimer
                    InformationType.GOD_POTION -> godPotionTimer
                    InformationType.JACOBS_CONTEST -> if (FarmingContestAPI.inContest) "$currentPlacement% ${
                        convertPlacement(
                            currentPlacement,
                        )?.emoji
                    }" else ""

                    InformationType.ACTIVE_CROP -> {
                        currentCrop = GardenAPI.getCurrentlyFarmedCrop()?.niceName?.let { niceName ->
                            Crop.entries.find { it.name.lowercase() == niceName }
                        }
                        currentCrop?.name + currentCrop?.emoji
                    }

                    InformationType.ANITA_BUFF -> activeAnitaBuff
                    InformationType.BPS -> GardenCropSpeed.averageBlocksPerSecond
                    else -> ""
                }?.toString().takeIf { !it.isNullOrBlank() }

                val fieldName = if (type == InformationType.JACOBS_CONTEST) {
                    GardenAPI.getCurrentlyFarmedCrop()?.niceName?.let { niceName ->
                        Crop.entries.find { it.name.lowercase() == niceName }?.run { "$name Contest $emoji" }
                    } ?: type.fieldName
                } else {
                    type.fieldName
                }

                value?.let { Field(fieldName, it, true) }
            }

        val embed = Embed(
            title = "Status - $status",
            color = color,
            fields = fields,
            timestamp = SimpleTimeMark.now().formattedDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
            thumbnail = Thumbnail(playerFaceURL),
            footer = Footer("Automatic Status Report"),
        )

        val embeds = listOf(embed)

        when (config.messageType) {
            NEW_MESSAGE -> {
                WebhookUtils.sendEmbedsToWebhook(
                    config.webhook.url,
                    embeds,
                    "[FARMING TRACKER] ${LorenzUtils.getPlayerName()}",
                    farmingTrackerURL,
                )
            }

            EDITED_MESSAGE -> {
                WebhookUtils.editMessageEmbeds(
                    config.webhook.url,
                    embeds,
                    "[FARMING TRACKER] ${LorenzUtils.getPlayerName()}",
                    farmingTrackerURL,
                )
            }

            else -> {
                WebhookUtils.sendEmbedsToWebhook(
                    config.webhook.url,
                    embeds,
                    "[FARMING TRACKER] ${LorenzUtils.getPlayerName()}",
                    farmingTrackerURL,
                )
            }
        }

        lastNotification = SimpleTimeMark.now()
    }

    @SubscribeEvent
    fun onTablistUpdated(event: TabListUpdateEvent) {
        if (!isEnabled()) return

        val tabList = event.tabList.map { it.removeColor() }
        val changedLines = tabList.filter { !lastTablist.contains(it) }
        lastTablist = tabList
        val widgets = TabWidget.entries.filter { it.isActive }

        val widgetLines = { widget: TabWidget -> widgets.find { it == widget }?.lines?.map { it.removeColor() } ?: changedLines }

        widgetLines(TabWidget.STATS).matchAll(tablistStatsPattern) {
            stats[group("stat")] = group("amount").toInt()
        }

        widgetLines(TabWidget.JACOB_CONTEST).apply {
            if (firstOrNull()?.contains("Starts In:") == true) {
                matchAll(tablistUpcomingContestPattern) { activeAnitaBuff = group("crop") }
            } else {
                matchAll(tablistActiveContestPattern) {
                    currentPlacement = group("placement").toDouble()
                    if (group("boost") == "☘") activeAnitaBuff = group("crop")
                }
            }
        }

        widgetLines(TabWidget.ACTIVE_EFFECTS).matchAll(tablistEffectsPattern) {
            when (group("type")) {
                "Cookie Buff" -> cookieBuffTimer = group("duration")
                "God Potion" -> godPotionTimer = group("duration")
            }
        }
    }

    private fun convertPlacement(placement: Double): Medal? {
        val isFinnegan = MayorAPI.currentMayor == Mayor.FINNEGAN
        val (requiredBronze, requiredSilver, requiredGold, requiredPlatinum, requiredDiamond) = if (isFinnegan) {
            listOf(
                BRONZE_MEDAL.requiredNormal,
                SILVER_MEDAL.requiredNormal,
                GOLD_MEDAL.requiredNormal,
                PLATINUM_MEDAL.requiredNormal,
                DIAMOND_MEDAL.requiredNormal,
            )
        } else {
            listOf(
                BRONZE_MEDAL.requiredFinnegan,
                SILVER_MEDAL.requiredFinnegan,
                GOLD_MEDAL.requiredFinnegan,
                PLATINUM_MEDAL.requiredFinnegan,
                DIAMOND_MEDAL.requiredFinnegan,
            )
        }

        return when {
            placement >= requiredBronze -> null
            placement in requiredSilver..requiredBronze -> BRONZE_MEDAL
            placement in requiredGold..requiredSilver -> SILVER_MEDAL
            placement in requiredPlatinum..requiredGold -> GOLD_MEDAL
            placement in requiredDiamond..requiredPlatinum -> PLATINUM_MEDAL
            else -> DIAMOND_MEDAL
        }
    }

    fun InformationType.isSelected() = config.information.contains(this)

    fun isEnabled() = config.tracking && LorenzUtils.inSkyBlock && GardenAPI.inGarden()
}
