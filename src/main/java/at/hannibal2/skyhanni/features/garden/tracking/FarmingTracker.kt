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
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.Pet
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
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.WebhookUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object FarmingTracker {

    val storage get() = ProfileStorageData.playerSpecific
    val config get() = SkyHanniMod.feature.garden.tracking

    var lastNotification = SimpleTimeMark.farPast()
    var farmingSince = SimpleTimeMark.farFuture()
    var playerFaceURL = ""
    var stats = mutableMapOf<String, Int>()
    var lastTablist = listOf<String>()
    var cookieBuffTimer = ""
    var godPotionTimer = ""
    var activeAnitaBuff = ""
    var currentCrop: Crop? = null
    var currentPlacement = 0.0
    const val SKYHANNI_URL = "https://github.com/hannibal002/SkyHanni/blob/beta/src/main/resources/assets/skyhanni/logo.png"

    private val patternGroup = RepoPattern.group("garden.tracking")

    /**
     * REGEX-TEST:  Speed: ✦328
     * REGEX-TEST:  Farming Fortune: ☘135
     */
    private val tablistStatsPattern by patternGroup.pattern(
        "tablist.stats",
        "^ (?<stat>[^:]+): .?(?<amount>\\d+)\$",
    )

    /**
     * REGEX-TEST:  ☘ Cocoa Beans
     * REGEX-TEST:  ☘ Mushroom
     */
    private val tablistUpcomingContestPattern by patternGroup.pattern(
        "tablist.contest.upcoming",
        "^ ☘ (?<crop>.+)\$",
    )

    /**
     * REGEX-TEST:  ☘ Wheat ◆ Top 0.2%
     * REGEX-TEST:  ○ Wheat ◆ Top 0.2%
     */
    private val tablistActiveContestPattern by patternGroup.pattern(
        "tablist.contest.active",
        "^ (?<boost>[☘○]) (?<crop>.+) ◆ Top (?<placement>\\S+)%\$",
    )

    /**
     * REGEX-TEST:  Cookie Buff: 28h
     * REGEX-TEST:  God Potion: 6m
     */
    private val tablistEffectsPattern by patternGroup.pattern(
        "tablist.effects",
        "^ (?<type>Cookie Buff|God Potion): (?<duration>.+)\$",
    )

    /**
     * REGEX-TEST: You have a God Potion active! 32 Minutes
     */
    private val tablistFooterGodPotionPattern by patternGroup.pattern(
        "tablist.footer.godpotion",
        "You have a God Potion active! (?<length>.+)",
    )

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (lastNotification.passedSince() < config.webhook.interval.minutes) return

        if (playerFaceURL.isBlank()) playerFaceURL = "https://api.mineatar.io/face/${LorenzUtils.getPlayerUuid()}?scale=12"

        val status = if (GardenAPI.isCurrentlyFarming()) "Farming" else "Idle"
        farmingSince = if (status == "Farming") SimpleTimeMark.now() else SimpleTimeMark.farFuture()

        val color = toIntColor(config.color.toConfigColour())

        val fields = mutableListOf<Field>()

        for (type in config.information) {
            if (!type.isSelected()) continue

            val value = when (type) {
                InformationType.FARMING_FORTUNE -> stats["Farming Fortune"] ?: ""
                InformationType.FARMING_WISDOM -> stats["Farming Wisdom"] ?: ""
                InformationType.BONUS_PEST_CHANCE -> stats["Bonus Pest Chance"] ?: ""
                InformationType.SPEED -> stats["Speed"] ?: ""
                InformationType.STRENGTH -> stats["Strength"] ?: ""
                InformationType.PET -> PetAPI.currentPet?.let { pet ->
                    Pet.entries.find { it.toString() == pet.removeColor() }?.petName ?: ""
                } ?: ""

                InformationType.COOKIE_BUFF -> cookieBuffTimer.ifBlank { "<:no:1263210393723998278>" }
                InformationType.GOD_POTION -> godPotionTimer.ifBlank { "<:no:1263210393723998278>" }
                InformationType.JACOBS_CONTEST -> {
                    if (!FarmingContestAPI.inContest) ""
                    else convertPlacement(currentPlacement)?.let { medal ->
                        "$currentPlacement% ${medal.emoji}"
                    } ?: ""
                }

                InformationType.ACTIVE_CROP -> {
                    GardenAPI.getCurrentlyFarmedCrop()?.let { farmedCrop ->
                        getCropEnum(farmedCrop.cropName)?.let { cropEnum ->
                            currentCrop = cropEnum
                            cropEnum.name + cropEnum.emoji
                        } ?: ""
                    } ?: ""
                }

                InformationType.ANITA_BUFF -> activeAnitaBuff.ifBlank { "<:no:1263210393723998278>" }
                InformationType.BPS -> GardenCropSpeed.averageBlocksPerSecond.round(2)
                else -> ""
            }.toString()

            val fieldName =
                if (type != InformationType.JACOBS_CONTEST) type.fieldName
                else currentCrop?.let {
                    "${it.name} Contest ${it.emoji}"
                } ?: type.fieldName

            if (value != "") fields.add(
                Field(
                    name = fieldName,
                    value = value,
                    inline = true,
                ),
            )
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

        val username = "[FARMING TRACKER] ${LorenzUtils.getPlayerName()}"

        when (config.messageType) {
            NEW_MESSAGE -> {
                WebhookUtils.sendEmbedsToWebhook(
                    config.webhook.url,
                    embeds,
                    username,
                    SKYHANNI_URL,
                )
            }

            EDITED_MESSAGE -> {
                WebhookUtils.editMessageEmbeds(
                    config.webhook.url,
                    embeds,
                    username,
                    SKYHANNI_URL,
                )
            }

            else -> {
                WebhookUtils.sendEmbedsToWebhook(
                    config.webhook.url,
                    embeds,
                    username,
                    SKYHANNI_URL,
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
            if (this[1].contains("Starts In:")) {
                matchAll(tablistUpcomingContestPattern) {
                    getCropEnum(group("crop"))?.let { cropEnum ->
                        activeAnitaBuff = cropEnum.name + cropEnum.emoji
                    }
                }
            } else {
                matchAll(tablistActiveContestPattern) {
                    currentPlacement = group("placement").toDouble()
                    if (group("boost") == "☘") activeAnitaBuff = group("crop")
                }
            }
        }

        widgetLines(TabWidget.ACTIVE_EFFECTS).matchAll(tablistEffectsPattern) {
            println(group("type"))
            println(group("duration"))
            println(cookieBuffTimer to godPotionTimer)
            when (group("type")) {
                "Cookie Buff" -> cookieBuffTimer = group("duration")
                "God Potion" -> godPotionTimer = group("duration")
            }
        }

        val footerLines = TabListData.getFooter().removeColor().lines()

        val cookieBuffIndex = footerLines.indexOfFirst { it.contains("Cookie Buff") } + 1
        if (cookieBuffIndex <= footerLines.lastIndex) cookieBuffTimer = footerLines[cookieBuffIndex]

        if (
            footerLines.any {
                it.contains("No effects active.")
            } ||
            footerLines.none { tablistFooterGodPotionPattern.matches(it) }
        ) {
            godPotionTimer = "INACTIVE"
        } else footerLines.matchAll(tablistFooterGodPotionPattern) {
            godPotionTimer = group("length")
        }
    }

    private fun getCropEnum(cropName: String): Crop? =
        Crop.entries.find { it.name == cropName }

    private fun toIntColor(configString: String): Int {
        val parts = configString.split(':')

        val red = parts[2].toInt()
        val green = parts[3].toInt()
        val blue = parts[4].toInt()

        return (red shl 16) or (green shl 8) or blue
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
