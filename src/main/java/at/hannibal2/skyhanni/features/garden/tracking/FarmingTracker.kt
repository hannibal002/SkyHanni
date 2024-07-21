package at.hannibal2.skyhanni.features.garden.tracking

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.Crop
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.EmbedConfig.InformationType
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
import at.hannibal2.skyhanni.events.TablistFooterUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.contest.ContestBracket
import at.hannibal2.skyhanni.features.garden.contest.ContestBracket.BRONZE
import at.hannibal2.skyhanni.features.garden.contest.ContestBracket.DIAMOND
import at.hannibal2.skyhanni.features.garden.contest.ContestBracket.GOLD
import at.hannibal2.skyhanni.features.garden.contest.ContestBracket.PLATINUM
import at.hannibal2.skyhanni.features.garden.contest.ContestBracket.SILVER
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestAPI
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.APIUtil.getPlayerSkin
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matches
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
    var farmingSince = SimpleTimeMark.farFuture()
    var playerFaceURL = ""
    var stats = mutableMapOf<String, Int>()
    var lastTablist = listOf<String>()
    var cookieBuffTimer = ""
    var godPotionTimer = ""
    var activeAnitaBuff = ""
    var currentCrop: Crop? = null
    var currentPlacement = 0.0

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

        if (playerFaceURL.isBlank()) playerFaceURL = getPlayerSkin(config.embed.bodyPart, 12)

        val status = if (GardenAPI.isCurrentlyFarming()) "Farming" else "Idle"
        if (status == "Idle") farmingSince = SimpleTimeMark.farFuture()
        else if (farmingSince.isInFuture()) farmingSince = SimpleTimeMark.now()

        val color = toIntColor(config.embed.color.toConfigColour())

        val fields = mutableListOf<Field>()

        for (type in config.embed.information) {
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
                    else convertPlacement(currentPlacement)?.let { bracket ->
                        "$currentPlacement% ${bracket.emoji}"
                    } ?: ""
                }

                InformationType.ACTIVE_CROP -> {
                    GardenAPI.getCurrentlyFarmedCrop()?.let { farmedCrop ->
                        getCropEnum(farmedCrop.cropName)?.let { cropEnum ->
                            currentCrop = cropEnum
                            "${cropEnum.name} ${cropEnum.emoji}"
                        } ?: ""
                    } ?: ""
                }

                InformationType.ANITA_BUFF -> activeAnitaBuff.ifBlank { "<:no:1263210393723998278>" }
                InformationType.BPS -> GardenCropSpeed.averageBlocksPerSecond.round(2)
                InformationType.FARMING_SINCE -> {
                    if (farmingSince.isInFuture()) ""
                    else farmingSince.passedSince().toString()
                }
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

        if(fields.isEmpty()) {
            lastNotification = SimpleTimeMark.now()
            return ChatUtils.chatAndOpenConfig(
                "No information could be displayed! Do you have them activated? Click to open Config.",
                config.embed::information
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
        val threadID = config.threadId.ifBlank { null }

        when (config.messageType) {
            NEW_MESSAGE -> {
                WebhookUtils.sendEmbedsToWebhook(
                    config.webhook.url,
                    embeds,
                    threadID = threadID,
                    username = username,
                )
            }

            EDITED_MESSAGE -> {
                WebhookUtils.editMessageEmbeds(
                    config.webhook.url,
                    embeds,
                    threadID = threadID,
                    username = username,
                )
            }

            else -> {
                WebhookUtils.sendEmbedsToWebhook(
                    config.webhook.url,
                    embeds,
                    threadID = threadID,
                    username = username,
                )
            }
        }

        lastNotification = SimpleTimeMark.now()
    }

    @SubscribeEvent
    fun onWidgetUpdated(event: WidgetUpdateEvent) {
        if (!isEnabled()) return

        val widget = event.widget
        val widgetLines = event.widget.lines.map { it.removeColor() }
        if (widgetLines.isEmpty()) return

        when (widget) {
            TabWidget.STATS -> widgetLines.matchAll(tablistStatsPattern) {
                stats[group("stat")] = group("amount").toInt()
            }

            TabWidget.JACOB_CONTEST -> {
                if (widgetLines[1].contains("Starts In:")) {
                    widgetLines.matchAll(tablistUpcomingContestPattern) {
                        getCropEnum(group("crop"))?.let { cropEnum ->
                            activeAnitaBuff = cropEnum.name + cropEnum.emoji
                        }
                    }
                } else {
                    widgetLines.matchAll(tablistActiveContestPattern) {
                        currentPlacement = group("placement").toDouble()
                        if (group("boost") == "☘") activeAnitaBuff = group("crop")
                    }
                }
            }

            TabWidget.ACTIVE_EFFECTS -> widgetLines.matchAll(tablistEffectsPattern) {
                when (group("type")) {
                    "Cookie Buff" -> cookieBuffTimer = group("duration")
                    "God Potion" -> godPotionTimer = group("duration")
                }
            }

            else -> {}
        }
    }

    @SubscribeEvent
    fun onFooterUpdated(event: TablistFooterUpdateEvent) {
        if (!isEnabled()) return

        val footerLines = event.footer.removeColor().lines()

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

    private fun convertPlacement(placement: Double): ContestBracket? {
        val isFinnegan = MayorAPI.currentMayor == Mayor.FINNEGAN
        val (requiredBronze, requiredSilver, requiredGold, requiredPlatinum, requiredDiamond) = ContestBracket.entries
            .map {
                if (isFinnegan) it.requiredNormal else it.requiredFinnegan
            }

        return when {
            placement >= requiredBronze -> null
            placement in requiredSilver..requiredBronze -> BRONZE
            placement in requiredGold..requiredSilver -> SILVER
            placement in requiredPlatinum..requiredGold -> GOLD
            placement in requiredDiamond..requiredPlatinum -> PLATINUM
            else -> DIAMOND
        }
    }

    fun InformationType.isSelected() = config.embed.information.contains(this)

    fun isEnabled() = config.tracking && LorenzUtils.inSkyBlock && GardenAPI.inGarden()
}
