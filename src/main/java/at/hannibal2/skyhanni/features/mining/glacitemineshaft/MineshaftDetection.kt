package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
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
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format

@SkyHanniModule
object MineshaftDetection {
    private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.mineshaftDetectionConfig

    private val profileStorage get() = ProfileStorageData.profileSpecific?.mining?.mineshaft

    private fun getSinceMineshaftType(type: MineshaftTypes): Int = profileStorage?.mineshaftsEnteredSince?.get(type) ?: 0

    private fun setSinceMineshaftType(type: MineshaftTypes, value: Int) {
        profileStorage?.mineshaftsEnteredSince?.set(type, value)
    }

    private fun getTimeSinceMineshaftType(type: MineshaftTypes): SimpleTimeMark =
        profileStorage?.lastMineshaftTime?.get(type) ?: SimpleTimeMark.farPast()

    private fun setTimeSinceMineshaftType(type: MineshaftTypes, time: SimpleTimeMark) {
        profileStorage?.lastMineshaftTime?.set(type, time)
    }

    private var found = false

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (!config.mineshaftDetection) return
        found = false
    }

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.mineshaftDetection) return
        if (found) return

        val matchingLine = ScoreboardData.sidebarLinesFormatted
            .firstOrNull { line -> MineshaftTypes.entries.any { line.contains(it.name) } }
            ?.removeColor() ?: return

        val areaName = matchingLine.split(" ").last().dropLast(1)

        ChatUtils.debug("In area: $areaName")

        val type = MineshaftTypes.entries.firstOrNull { areaName.contains(it.name) } ?: return
        found = true

        ChatUtils.debug("Found a ${type.name} mineshaft! [$areaName]")

        val sinceThis = getSinceMineshaftType(type)
        val timeSinceThis = getTimeSinceMineshaftType(type)

        ChatUtils.chat("You entered a ${type.displayName} mineshaft!")

        if (type == config.mineshaftToTrack) {
            TitleManager.sendTitle(config.mineshaftToTrack.displayName)

            val builder = StringBuilder()
            builder.append("It took ")
                .append(LorenzColor.RED.getChatColor())
                .append(timeSinceThis.passedSince().format())
                .append(LorenzColor.YELLOW.getChatColor())
                .append(" and ")
                .append(LorenzColor.RED.getChatColor())
                .append(sinceThis)
                .append(LorenzColor.YELLOW.getChatColor())
                .append(if (sinceThis == 1) " mineshaft " else " mineshafts ")
                .append("entered to get a ")
                .append(config.mineshaftToTrack.displayName)
                .append(LorenzColor.YELLOW.getChatColor())
                .append(" mineshaft.")

            ChatUtils.chat(builder.toString())
        }

        handleShaftData(type)

        if (config.sendTypeToPartyChat && PartyApi.isInParty()) {
            val partyChatBuilder = StringBuilder()

            val formattedMessage = config.partyChatFormat
                .replace("{type}", type.displayName)
                .replace("{sinceThis}", sinceThis.toString())
                .replace("{timeSinceThis}", timeSinceThis.passedSince().format())
                .let { msg ->
                    if (type != config.mineshaftToTrack) {
                        msg
                            .replace("{sinceConfigShaft}", getSinceMineshaftType(config.mineshaftToTrack).toString())
                            .replace("{timeSinceConfigShaft}", getTimeSinceMineshaftType(config.mineshaftToTrack).passedSince().format())
                    } else {
                        msg
                    }
                }

            partyChatBuilder.append(formattedMessage)

            HypixelCommands.partyChat(partyChatBuilder.toString().removeColor())
        }
    }

    private fun handleShaftData(type: MineshaftTypes) {
        setSinceMineshaftType(type, 0)
        setTimeSinceMineshaftType(type, SimpleTimeMark.now())

        for (otherTypes in MineshaftTypes.entries) {
            if (otherTypes == type) continue
            setSinceMineshaftType(otherTypes, getSinceMineshaftType(otherTypes) + 1)
        }
    }

    enum class MineshaftTypes(val color: LorenzColor, val rawName: String) {
        TOPA(LorenzColor.YELLOW, "Topaz"),
        SAPP(LorenzColor.BLUE, "Sapphire"),
        AMET(LorenzColor.DARK_PURPLE, "Amethyst"),
        AMBE(LorenzColor.GOLD, "Amber"),
        JADE(LorenzColor.GREEN, "Jade"),
        TITA(LorenzColor.GRAY, "Titanium"),
        UMBE(LorenzColor.GOLD, "Umber"),
        TUNG(LorenzColor.DARK_GRAY, "Tungsten"),
        FAIR(LorenzColor.WHITE, "Vanguard"),
        RUBY(LorenzColor.RED, "Ruby"),
        ONYX(LorenzColor.BLACK, "Onyx"),
        AQUA(LorenzColor.DARK_BLUE, "Aquamarine"),
        CITR(LorenzColor.YELLOW, "Citrine"),
        PERI(LorenzColor.DARK_GREEN, "Peridot"),
        JASP(LorenzColor.LIGHT_PURPLE, "Jasper"),
        OPAL(LorenzColor.WHITE, "Opal"),
        ;

        val displayName: String = color.getChatColor() + rawName
    }
}
