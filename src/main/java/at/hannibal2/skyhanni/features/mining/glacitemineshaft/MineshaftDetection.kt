package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.ChatFormatting

@SkyHanniModule
object MineshaftDetection {
    private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.mineshaftDetectionConfig

    private val profileStorage get() = ProfileStorageData.profileSpecific?.mining?.mineshaft

    private fun getSinceMineshaftType(type: MineshaftType): Int = profileStorage?.mineshaftsEnteredSinceNew?.get(type) ?: 0

    private fun setSinceMineshaftType(type: MineshaftType, value: Int) {
        profileStorage?.mineshaftsEnteredSinceNew?.set(type, value)
    }

    private fun getTimeSinceMineshaftType(type: MineshaftType): SimpleTimeMark =
        profileStorage?.lastMineshaftTimeNew?.get(type) ?: SimpleTimeMark.farPast()

    private fun setTimeSinceMineshaftType(type: MineshaftType, time: SimpleTimeMark) {
        profileStorage?.lastMineshaftTimeNew?.set(type, time)
    }

    private var found = false

    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() {
        if (!config.mineshaftDetection) return
        found = false
    }

    @HandleEvent(SecondPassedEvent::class, onlyOnIsland = IslandType.MINESHAFT)
    fun onSecondPassed() {
        if (!config.mineshaftDetection) return
        if (found) return

        val matchingLine = ScoreboardData.sidebarLinesFormatted
            .firstOrNull { line -> MineshaftType.entries.any { line.contains(it.name) } }
            ?.removeColor() ?: return

        val areaName = matchingLine.split(" ").last()

        ChatUtils.debug("In area: $areaName")

        val type = MineshaftType.entries.firstOrNull { areaName.contains(it.name) } ?: return
        found = true

        ChatUtils.debug("Found a ${type.name} mineshaft! [$areaName]")

        val sinceThis = getSinceMineshaftType(type)
        val timeSinceThis = getTimeSinceMineshaftType(type)
        val formattedTime = if (!timeSinceThis.isFarPast()) {
            timeSinceThis.passedSince().format()
        } else {
            "Unknown (no data yet)"
        }

        ChatUtils.chat("You entered a ${type.displayName} mineshaft!".asComponent())

        if (type in config.mineshaftsToTrack) {
            TitleManager.sendTitle(type.displayName)
            ChatUtils.chat(
                componentBuilder {
                    withColor(ChatFormatting.GREEN)
                    append("It took ")
                    appendWithColor(formattedTime, ChatFormatting.YELLOW)
                    append(" and ")
                    appendWithColor("$sinceThis ", ChatFormatting.YELLOW)
                    append("mineshaft".pluralize(sinceThis))
                    append(" entered to get a ")
                    append(type.displayName)
                    append(" mineshaft.")
                }
            )
        }

        handleShaftData(type)

        if (config.sendTypeToPartyChat && PartyApi.isInParty()) {
            val partyChatBuilder = StringBuilder()

            val formattedMessage = config.partyChatFormat
                .replace("{type}", type.displayName)
                .replace("{amountSinceThis}", sinceThis.toString())
                .replace("{timeSinceThis}", formattedTime)

            partyChatBuilder.append(formattedMessage)

            HypixelCommands.partyChat(partyChatBuilder.toString().removeColor())
        }
    }

    private fun handleShaftData(type: MineshaftType) {
        setSinceMineshaftType(type, 0)
        setTimeSinceMineshaftType(type, SimpleTimeMark.now())

        for (otherTypes in MineshaftType.entries) {
            if (otherTypes == type) continue
            setSinceMineshaftType(otherTypes, getSinceMineshaftType(otherTypes) + 1)
        }
    }

    enum class MineshaftType(val color: LorenzColor, val rawName: String) {
        TOPA_1(LorenzColor.YELLOW, "Topaz 1"),
        TOPA_2(LorenzColor.YELLOW, "Topaz 2"),
        SAPP_1(LorenzColor.BLUE, "Sapphire 1"),
        SAPP_2(LorenzColor.BLUE, "Sapphire 2"),
        AMET_1(LorenzColor.DARK_PURPLE, "Amethyst 1"),
        AMET_2(LorenzColor.DARK_PURPLE, "Amethyst 2"),
        AMBE_1(LorenzColor.GOLD, "Amber 1"),
        AMBE_2(LorenzColor.GOLD, "Amber 2"),
        JADE_1(LorenzColor.GREEN, "Jade 1"),
        JADE_2(LorenzColor.GREEN, "Jade 2"),
        TITA_1(LorenzColor.GRAY, "Titanium"),
        UMBE_1(LorenzColor.GOLD, "Umber"),
        TUNG_1(LorenzColor.DARK_GRAY, "Tungsten"),
        FAIR_1(LorenzColor.WHITE, "Vanguard"),
        RUBY_1(LorenzColor.RED, "Ruby 1"),
        RUBY_2(LorenzColor.RED, "Ruby 2"),
        RUBY_C(LorenzColor.RED, "Ruby Crystal"),
        ONYX_1(LorenzColor.BLACK, "Onyx 1"),
        ONYX_2(LorenzColor.BLACK, "Onyx 2"),
        ONYX_C(LorenzColor.BLACK, "Onyx Crystal"),
        AQUA_1(LorenzColor.DARK_BLUE, "Aquamarine 1"),
        AQUA_2(LorenzColor.DARK_BLUE, "Aquamarine 2"),
        AQUA_C(LorenzColor.DARK_BLUE, "Aquamarine Crystal"),
        CITR_1(LorenzColor.YELLOW, "Citrine 1"),
        CITR_2(LorenzColor.YELLOW, "Citrine 2"),
        CITR_C(LorenzColor.YELLOW, "Citrine Crystal"),
        PERI_1(LorenzColor.DARK_GREEN, "Peridot 1"),
        PERI_2(LorenzColor.DARK_GREEN, "Peridot 2"),
        PERI_C(LorenzColor.DARK_GREEN, "Peridot Crystal"),
        JASP_1(LorenzColor.LIGHT_PURPLE, "Jasper"),
        JASP_C(LorenzColor.LIGHT_PURPLE, "Jasper Crystal"),
        OPAL_1(LorenzColor.WHITE, "Opal"),
        OPAL_C(LorenzColor.WHITE, "Opal Crystal")
        ;

        val displayName: String = color.getChatColor() + rawName
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(108, "mining.glaciteMineshaft.mineshaftDetectionConfig.mineshaftsToTrack") { element ->
            val newList = JsonArray()
            for (entry in element.asJsonArray) {
                val fixedEnumValue = transformMineshaftTypeEnum(entry.asString)
                for (newEntry in fixedEnumValue) {
                    newList.add(JsonPrimitive(newEntry))
                }
            }
            newList
        }
        event.move(
            109,
            "#profile.mining.mineshaft.mineshaftsEnteredSince",
            "#profile.mining.mineshaft.mineshaftsEnteredSinceNew",
        ) { transformElementMap(it) }
        event.move(
            109,
            "#profile.mining.mineshaft.lastMineshaftTime",
            "#profile.mining.mineshaft.lastMineshaftTimeNew",
        ) { transformElementMap(it) }
    }

    private fun transformElementMap(originalElement: JsonElement): JsonObject {
        val newObj = JsonObject()
        for ((key, value) in originalElement.asJsonObject.entrySet()) {
            val fixedEnumValue = transformMineshaftTypeEnum(key)
            for (newKey in fixedEnumValue) {
                newObj.add(newKey, value)
            }
        }
        return newObj
    }

    private fun transformMineshaftTypeEnum(original: String): List<String> {
        val type = original.dropLast(1)
        val newList = mutableListOf<String>()
        return when (original.last()) {
            '1' -> {
                newList.add("${type}_1")
                enumValues<MineshaftType>().find { it.name == "${type}_2" }?.let {
                    newList.add(it.name)
                }
                newList
            }

            '2' -> listOf("${type}_C")
            else -> listOf()
        }
    }
}
