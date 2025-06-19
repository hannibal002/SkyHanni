package at.hannibal2.skyhanni.features.garden.tracking

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.Crop
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.EmbedConfig.InformationType
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.MessageType
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.Pet
import at.hannibal2.skyhanni.data.ElectionApi
import at.hannibal2.skyhanni.data.ElectionCandidate
import at.hannibal2.skyhanni.data.Embed
import at.hannibal2.skyhanni.data.Field
import at.hannibal2.skyhanni.data.Footer
import at.hannibal2.skyhanni.data.PetApi
import at.hannibal2.skyhanni.data.Thumbnail
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.TablistFooterUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.contest.ContestBracket
import at.hannibal2.skyhanni.features.garden.contest.ContestBracket.BRONZE
import at.hannibal2.skyhanni.features.garden.contest.ContestBracket.DIAMOND
import at.hannibal2.skyhanni.features.garden.contest.ContestBracket.GOLD
import at.hannibal2.skyhanni.features.garden.contest.ContestBracket.PLATINUM
import at.hannibal2.skyhanni.features.garden.contest.ContestBracket.SILVER
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestApi
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ApiUtils
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.WebhookUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import java.util.TimeZone
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object FarmingTracker {

    val config get() = SkyHanniMod.feature.garden.tracking

    var status = "Offline"
    private var lastNotification = SimpleTimeMark.farPast()
    private var farmingSince = SimpleTimeMark.farFuture()
    private var playerFaceURL = ""
    private var cookieBuffTimer = ""
    private var godPotionTimer = ""
    private var activeAnitaBuff = ""
    private var currentCrop: Crop? = null
    private var currentPlacement = 0.0

    private val patternGroup = RepoPattern.group("garden.tracking")

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

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (lastNotification.passedSince() < config.webhook.interval.minutes) return

        status = when {
            GardenApi.isCurrentlyFarming() -> "Farming"
            !GardenApi.isCurrentlyFarming() && GardenApi.inGarden() -> "Idle"
            SkyBlockUtils.inSkyBlock && !GardenApi.inGarden() -> "in Skyblock"
            SkyBlockUtils.onHypixel -> "Online"
            else -> status
        }

        val success = prepareAndSendEmbed(status)

        if (success) lastNotification = SimpleTimeMark.now() else ChatUtils.chat("§cCouldn't send embed (Farming Tracker).")
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        status = "Offline"

        if (!isEnabled()) return

        val success = prepareAndSendEmbed(status)

        if (success) lastNotification = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onWidgetUpdated(event: WidgetUpdateEvent) {
        if (!isEnabled() && SkyBlockUtils.inSkyBlock) return

        val widget = event.widget
        val widgetLines = event.widget.lines.map { it.removeColor() }
        if (widgetLines.isEmpty()) return

        when (widget) {
            TabWidget.JACOB_CONTEST -> {
                if (widgetLines[1].contains("Starts In:")) {
                    tablistUpcomingContestPattern.matchAll(widgetLines) {
                        getCropEnum(group("crop"))?.let { cropEnum ->
                            activeAnitaBuff = cropEnum.name + cropEnum.emoji
                        }
                    }
                } else {
                    tablistActiveContestPattern.matchAll(widgetLines) {
                        currentPlacement = group("placement").toDouble()
                        if (group("boost") == "☘") activeAnitaBuff = group("crop")
                    }
                }
            }

            TabWidget.ACTIVE_EFFECTS -> tablistEffectsPattern.matchAll(widgetLines) {
                when (group("type")) {
                    "Cookie Buff" -> cookieBuffTimer = group("duration")
                    "God Potion" -> godPotionTimer = group("duration")
                }
            }

            else -> {}
        }
    }

    @HandleEvent
    fun onFooterUpdated(event: TablistFooterUpdateEvent) {
        if (!isEnabled() && SkyBlockUtils.inSkyBlock) return

        val footerLines = event.footer.removeColor().lines()

        cookieBuffTimer = footerLines.indexOfFirst { it.contains("Cookie Buff") }
            .takeIf { it != -1 && it + 1 <= footerLines.lastIndex }
            ?.let { footerLines[it + 1] }
            ?.takeUnless { it.contains("Not active!") }
            ?: "<:no:1263210393723998278>"

        if (
            footerLines.any { it.contains("No effects active.") } ||
            footerLines.none { tablistFooterGodPotionPattern.matches(it) }
        ) {
            godPotionTimer = "<:no:1263210393723998278>"
        } else tablistFooterGodPotionPattern.matchAll(footerLines) {
            godPotionTimer = group("length")
        }
    }

    fun prepareAndSendEmbed(status: String): Boolean {
        playerFaceURL = playerFaceURL.ifBlank { ApiUtils.getPlayerSkin(config.embed.bodyPart, 12) }

        farmingSince =
            if (status != "Farming") SimpleTimeMark.farFuture() else farmingSince.takeUnless { it.isInFuture() } ?: SimpleTimeMark.now()

        val color = config.embed.takeIf { it.useDefault }?.run {
            when (status) {
                "Farming", "Online", "in Skyblock" -> LorenzColor.GREEN
                "Offline" -> LorenzColor.RED
                else -> LorenzColor.YELLOW
            }.toIntColor()
        } ?: config.embed.color.toIntColor()

        val fields = config.embed.information
            .filter { it.isSelected() }
            .mapNotNull { type ->
                val value = when (type) {
                    InformationType.FARMING_FORTUNE -> SkyblockStat.FARMING_FORTUNE.lastKnownValue?.roundToInt()
                    InformationType.FARMING_WISDOM -> SkyblockStat.FARMING_WISDOM.lastKnownValue?.roundToInt()
                    InformationType.BONUS_PEST_CHANCE -> SkyblockStat.BONUS_PEST_CHANCE.lastKnownValue?.roundToInt()
                    InformationType.SPEED -> SkyblockStat.SPEED.lastKnownValue?.roundToInt()
                    InformationType.STRENGTH -> SkyblockStat.STRENGTH.lastKnownValue?.roundToInt()
                    InformationType.PET -> PetApi.currentPet?.let { pet ->
                        Pet.entries.find { it.toString() == pet.removeColor() }?.petName ?: ""
                    } ?: ""

                    InformationType.COOKIE_BUFF -> cookieBuffTimer.ifBlank { "<:no:1263210393723998278>" }
                    InformationType.GOD_POTION -> godPotionTimer.ifBlank { "<:no:1263210393723998278>" }
                    InformationType.JACOBS_CONTEST ->
                        if (!FarmingContestApi.inContest) ""
                        else convertPlacement(currentPlacement)?.let { bracket -> "$currentPlacement% ${bracket.emoji}" }
                            ?: ""

                    InformationType.ACTIVE_CROP -> GardenApi.getCurrentlyFarmedCrop()?.let { farmedCrop ->
                        getCropEnum(farmedCrop.cropName)?.let { cropEnum ->
                            "${cropEnum.name} ${cropEnum.emoji}"
                        }.takeUnless { status == "Idle" || status == "Offline" } ?: ""
                    } ?: ""

                    InformationType.ANITA_BUFF -> activeAnitaBuff.ifBlank { "<:no:1263210393723998278>" }
                    InformationType.BPS -> GardenCropSpeed.averageBlocksPerSecond.roundTo(2).takeUnless { it == 0.0 } ?: ""
                    InformationType.FARMING_SINCE -> if (farmingSince.isInFuture()) "" else farmingSince.passedSince()
                }.toString().takeIf { it.isNotBlank() } ?: return@mapNotNull null

                Field(
                    name = if (type != InformationType.JACOBS_CONTEST) type.fieldName else currentCrop?.let { "${it.name} Contest ${it.emoji}" }
                        ?: type.fieldName,
                    value = value,
                    inline = true,
                )
            }

        if (fields.isEmpty()) {
            lastNotification = SimpleTimeMark.now()
            ChatUtils.chatAndOpenConfig(
                "No information could be displayed! Do you have them activated? Click to open Config.",
                config.embed::information,
            )
            return false
        }


        val time = SimpleTimeMark.now().let {
            SimpleTimeMark(it.toMillis() - TimeZone.getDefault().getOffset(it.toMillis()))
        }.formattedDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

        val embed = Embed(
            title = "Status - $status",
            color = color,
            fields = fields,
            timestamp = time,
            thumbnail = Thumbnail(playerFaceURL),
            footer = Footer("Automatic Status Report"),
        )

        val threadID = config.threadId.ifBlank { null }
        val username = "[FARMING TRACKER] ${PlayerUtils.getName()}"

        return when (config.messageType) {
            MessageType.NEW_MESSAGE -> WebhookUtils.sendEmbedsToWebhook(config.webhook.url, listOf(embed), threadID, username)
            MessageType.EDITED_MESSAGE -> WebhookUtils.editMessageEmbeds(config.webhook.url, listOf(embed), threadID, username)
        }
    }

    private fun getCropEnum(cropName: String): Crop? =
        Crop.entries.find { it.name == cropName }

    private fun LorenzColor.toIntColor(): Int {
        val parts = this.toChromaColor().toString().split(":")

        val red = parts[2].toInt()
        val green = parts[3].toInt()
        val blue = parts[4].toInt()

        return (red shl 16) or (green shl 8) or blue
    }

    private fun convertPlacement(placement: Double): ContestBracket? {
        val isFinnegan = ElectionApi.currentMayor == ElectionCandidate.FINNEGAN
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
            placement >= requiredDiamond -> DIAMOND
            else -> null
        }
    }

    fun InformationType.isSelected() = config.embed.information.contains(this)

    fun isEnabled() = config.tracking
}
