package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.features.garden.tracking.EmbedConfig.InformationType
import at.hannibal2.skyhanni.config.features.garden.tracking.EmbedConfig.MessageType
import at.hannibal2.skyhanni.data.BitsApi.cookieBuffTime
import at.hannibal2.skyhanni.data.Embed
import at.hannibal2.skyhanni.data.Field
import at.hannibal2.skyhanni.data.Footer
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.Thumbnail
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.SecondPassedEvent
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
import java.util.TimeZone
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

// This module sends status updates (like stats and buffs) to a user-defined Discord webhook.
// Only the data selected by the user in the config is sent.
// No tokens, session data, passwords, or account information is ever accessed or sent.

@SkyHanniModule
object FarmingStatusTracker {

    private val config get() = SkyHanniMod.feature.garden.tracking
    private val godPotionTimer get() = ProfileStorageData.profileSpecific?.godPotExpiry

    var status = "Offline"
    private var lastNotification = SimpleTimeMark.farPast()
    private var playerFaceURL = ""

    private val cropEmojis: Map<CropType, String> = mapOf(
        CropType.WHEAT to "<:wheat:1263207588296790048>",
        CropType.POTATO to "<:potato:1263207583502569522>",
        CropType.CARROT to "<:carrot:1263207574472359956>",
        CropType.PUMPKIN to "<:pumpkin:1263207585004257321>",
        CropType.MELON to "<:melon:1263207577920213083>",
        CropType.SUGAR_CANE to "<:sugar:1263207586463748289>",
        CropType.MUSHROOM to "<:mushroom:1263207580268888096>", // TODO NEW EMOJI
        CropType.CACTUS to "<:cactus:1263207572962414724>",
        CropType.COCOA_BEANS to "<:cocoa_beans:1263207576330567795>",
        CropType.NETHER_WART to "<:nether_wart:1263207581770579970>",
    )

    private val petEmojis: Map<String, String> = mapOf(
        "Bee" to "<:bee:1263201131064983673>",
        "Chicken" to "<:chicken:1263201132658823178>",
        "Elephant" to "<:elephant:1263201134466830357>",
        "Mooshroom Cow" to "<:mooshroom_cow:1263201135884374026>",
        "Pig" to "<:pig:1263201137188802651",
        "Rabbit" to "<:rabbit:1263201138371727421>",
        "Slug" to "<:slug:1263201140086931511>",
    )

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

        if (success) lastNotification = SimpleTimeMark.now() else ChatUtils.chat("§cCouldn't send embed (Farming Status Tracker).")
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
        val username = "[FARMING STATUS TRACKER] ${PlayerUtils.getName()}"

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
        InformationType.FARMING_FORTUNE -> SkyblockStat.FARMING_FORTUNE.lastKnownInt()
        InformationType.FARMING_WISDOM -> SkyblockStat.FARMING_WISDOM.lastKnownInt()
        InformationType.BONUS_PEST_CHANCE -> SkyblockStat.BONUS_PEST_CHANCE.lastKnownInt()
        InformationType.SPEED -> SkyblockStat.SPEED.lastKnownInt()
        InformationType.STRENGTH -> SkyblockStat.STRENGTH.lastKnownInt()

        InformationType.PET -> CurrentPetApi.currentPet?.let { pet ->
            "${pet.cleanName} ${petEmojis[pet.cleanName]}"
        }

        InformationType.COOKIE_BUFF -> cookieBuffTime.formatTime()
        InformationType.GOD_POTION -> godPotionTimer.formatTime()

        InformationType.JACOBS_CONTEST -> if (!FarmingContestApi.inContest) "" else with(FarmingContestApi.contestData) {
            "$placement% ($collected)${bracket?.emoji?.let { " $it" }.orEmpty()}"
        }

        InformationType.ACTIVE_CROP -> GardenApi.getCurrentlyFarmedCrop()?.let { crop ->
            "${crop.cropName} ${cropEmojis[crop]}".takeUnless { status in listOf("Idle", "Offline") }
        }

        InformationType.ANITA_BUFF -> FarmingContestApi.anitaBuffCrop?.cropName
        InformationType.BPS -> GardenCropSpeed.averageBlocksPerSecond.roundTo(2).takeUnless { it == 0.0 }
        InformationType.FARMING_SINCE -> if (GardenApi.farmingSince.isInFuture()) "" else GardenApi.farmingSince.passedSince()
    }

    private fun SkyblockStat.lastKnownInt() = lastKnownValue?.roundToInt()

    private fun SimpleTimeMark?.formatTime() =
        this?.takeIf { it.isInFuture() }?.timeUntil()?.toString()?.substringBeforeLast(" ") ?: "<:no:1263210393723998278>"

    private fun InformationType.getFieldDisplayName(): String {
        return if (this != InformationType.JACOBS_CONTEST) {
            fieldName
        } else {
            FarmingContestApi.contestCrop?.let { "${it.cropName} Contest ${cropEmojis[it]}" } ?: fieldName
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
        return when (config.embed.messageType) {
            MessageType.NEW_MESSAGE -> WebhookUtils.sendEmbedsToWebhook(config.webhook.url, listOf(embed), threadID, username)
            MessageType.EDITED_MESSAGE -> WebhookUtils.editMessageEmbeds(config.webhook.url, listOf(embed), threadID, username)
        }
    }

    private fun LorenzColor.toIntColor(): Int {
        val color = this.toColor()

        val red = color.red
        val green = color.green
        val blue = color.blue

        return (red shl 16) or (green shl 8) or blue
    }

    fun sendTutorial() {
        ChatUtils.clickableLinkChat(
            "§aWebhook Tutorial:\n" +
                "§e1. Open your Discord server, click the arrow next to the server name, and select \"§6Server Settings§e\".\n\n" +
                "§e2. In the Server Settings, find and click on the \"§6Integrations§e\" tab.\n\n" +
                "§e3. Click on the \"§6Webhooks§e\" section and then click \"§6Create Webhook§e\".\n\n" +
                "§e4. You can customize the webhook by:\n" +
                "§e  -  Giving your webhook a name (e.g. \"§6Farming Status Tracker§e\").\n" +
                "§e  -  Setting an avatar image for the webhook.\n" +
                "§e  -  Choosing the specific text channel where you want the webhook to post messages.\n\n" +
                "§e5. Once you've customized your webhook, click the \"§6Copy Webhook URL§e\" button and paste it in the config.\n",
            url = "https://www.youtube.com/watch?v=fKksxz2Gdnc",
            hover = "§eOpen YouTube tutorial",
        )
    }

    fun InformationType.isSelected() = config.embed.information.contains(this)

    fun isEnabled() = config.enabled
}
