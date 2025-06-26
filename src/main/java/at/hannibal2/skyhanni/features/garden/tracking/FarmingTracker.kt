package at.hannibal2.skyhanni.features.garden.tracking

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.Crop
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.EmbedConfig.InformationType
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.MessageType
import at.hannibal2.skyhanni.config.features.garden.TrackingConfig.Pet
import at.hannibal2.skyhanni.data.BitsApi.cookieBuffTime
import at.hannibal2.skyhanni.data.ElectionApi
import at.hannibal2.skyhanni.data.ElectionCandidate
import at.hannibal2.skyhanni.data.Embed
import at.hannibal2.skyhanni.data.Field
import at.hannibal2.skyhanni.data.Footer
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.Thumbnail
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.SecondPassedEvent
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
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.WebhookUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import java.util.TimeZone
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

// This module sends status updates (like stats and buffs) to a user-defined Discord webhook.
// Only the data selected by the user in the config is sent.
// No tokens, session data, passwords, or account information is ever accessed or sent.

@SkyHanniModule
object FarmingTracker {

    private val config get() = SkyHanniMod.feature.garden.tracking
    private val godPotionTimer get() = ProfileStorageData.profileSpecific?.godPotExpiry

    var status = "Offline"
    private var lastNotification = SimpleTimeMark.farPast()
    private var playerFaceURL = ""
    private val currentCrop: Crop? = null

    // Sends embed periodically
    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
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

    // Sends an embed when disconnecting from a server
    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        status = "Offline"

        if (!isEnabled()) return

        val success = prepareAndSendEmbed(status)

        if (success) lastNotification = SimpleTimeMark.now()
    }

    // Prepares and sends the embed to the configured webhook
    fun prepareAndSendEmbed(status: String): Boolean {
        playerFaceURL = playerFaceURL.ifBlank { ApiUtils.getPlayerSkin(config.embed.bodyPart, 12) }

        val color = resolveColor(status)
        val fields = collectFields(status)

        if (fields.isEmpty()) {
            notifyMissingFields()
            return false
        }

        val embed = buildEmbed(status, color, fields)
        val threadID = config.webhook.threadId.ifBlank { null }
        val username = "[FARMING TRACKER] ${PlayerUtils.getName()}"

        return sendOrEditMessage(embed, threadID, username)
    }

    private fun resolveColor(status: String): Int {
        return config.embed.takeIf { it.useDefault }?.run {
            when (status) {
                "Farming", "Online", "in Skyblock" -> LorenzColor.GREEN
                "Offline" -> LorenzColor.RED
                else -> LorenzColor.YELLOW
            }.toIntColor()
        } ?: config.embed.color.toIntColor()
    }

    private fun collectFields(status: String): List<Field> {
        return config.embed.information
            .filter { it.isSelected() }
            .mapNotNull { type -> type.buildField(status) }
    }

    private fun InformationType.buildField(status: String): Field? {
        val value = resolveValue(status)?.toString()?.takeIf { it.isNotBlank() } ?: return null
        val name = getFieldDisplayName()
        return Field(name, value, inline = true)
    }

    // Returns the value for a given information type
    @Suppress("CyclomaticComplexity")
    private fun InformationType.resolveValue(status: String): Any? = when (this) {
        InformationType.FARMING_FORTUNE -> SkyblockStat.FARMING_FORTUNE.lastKnownValue?.roundToInt()
        InformationType.FARMING_WISDOM -> SkyblockStat.FARMING_WISDOM.lastKnownValue?.roundToInt()
        InformationType.BONUS_PEST_CHANCE -> SkyblockStat.BONUS_PEST_CHANCE.lastKnownValue?.roundToInt()
        InformationType.SPEED -> SkyblockStat.SPEED.lastKnownValue?.roundToInt()
        InformationType.STRENGTH -> SkyblockStat.STRENGTH.lastKnownValue?.roundToInt()
        InformationType.PET -> CurrentPetApi.currentPet?.let { pet ->
            Pet.entries.find { it.toString() == pet.cleanName }?.petName.orEmpty()
        }

        InformationType.COOKIE_BUFF -> cookieBuffTime?.takeIf { it.isInFuture() }?.timeUntil()?.toString() ?: "<:no:1263210393723998278>"
        InformationType.GOD_POTION -> godPotionTimer?.takeIf { it.isInFuture() }?.timeUntil()?.toString() ?: "<:no:1263210393723998278>"
        InformationType.JACOBS_CONTEST -> if (!FarmingContestApi.inContest) "" else with(FarmingContestApi.contestData) {
            "$placement% ($collected)${bracket?.emoji?.let { " $it" }.orEmpty()}"
        }

        InformationType.ACTIVE_CROP -> GardenApi.getCurrentlyFarmedCrop()?.let { crop ->
            getCropEnum(crop.cropName)?.let { "${it.display} ${it.emoji}" }
                .takeUnless { status == "Idle" || status == "Offline" }
        }

        InformationType.ANITA_BUFF -> FarmingContestApi.anitaBuffCrop?.cropName?.let { getCropEnum(it) }
        InformationType.BPS -> GardenCropSpeed.averageBlocksPerSecond.roundTo(2).takeUnless { it == 0.0 }
        InformationType.FARMING_SINCE -> if (GardenApi.farmingSince.isInFuture()) "" else GardenApi.farmingSince.passedSince()
    }

    private fun InformationType.getFieldDisplayName(): String {
        return if (this != InformationType.JACOBS_CONTEST) {
            fieldName
        } else {
            currentCrop?.let { "${it.name} Contest ${it.emoji}" } ?: fieldName
        }
    }

    private fun notifyMissingFields() {
        lastNotification = SimpleTimeMark.now()
        ChatUtils.chatAndOpenConfig(
            "No information could be displayed! Do you have them activated? Click to open Config.",
            config.embed::information,
        )
    }

    // Builds the actual embed to send
    private fun buildEmbed(status: String, color: Int, fields: List<Field>): Embed {
        val time = SimpleTimeMark.now().let {
            SimpleTimeMark(it.toMillis() - TimeZone.getDefault().getOffset(it.toMillis()))
        }.formattedDate("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

        return Embed(
            title = "Status - $status",
            color = color,
            fields = fields,
            timestamp = time,
            thumbnail = Thumbnail(playerFaceURL),
            footer = Footer("Automatic Status Report"),
        )
    }

    // Sends the embed to the webhook
    private fun sendOrEditMessage(embed: Embed, threadID: String?, username: String): Boolean {
        return when (config.messageType) {
            MessageType.NEW_MESSAGE -> WebhookUtils.sendEmbedsToWebhook(config.webhook.url, listOf(embed), threadID, username)
            MessageType.EDITED_MESSAGE -> WebhookUtils.editMessageEmbeds(config.webhook.url, listOf(embed), threadID, username)
        }
    }

    private fun getCropEnum(cropName: String): Crop? =
        Crop.entries.find { it.display == cropName }

    private fun LorenzColor.toIntColor(): Int {
        val color = this.toColor()

        val red = color.red
        val green = color.green
        val blue = color.blue

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
