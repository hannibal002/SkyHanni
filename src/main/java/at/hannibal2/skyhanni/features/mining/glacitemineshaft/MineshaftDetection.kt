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
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
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

        val areaName = matchingLine.split(" ").last()

        ChatUtils.debug("In area: $areaName")

        val type = MineshaftTypes.entries.firstOrNull { areaName.contains(it.name) } ?: return
        found = true

        ChatUtils.debug("Found a ${type.name} mineshaft! [$areaName]")

        val sinceThis = getSinceMineshaftType(type)
        val timeSinceThis = getTimeSinceMineshaftType(type)
        val formattedTime = if (!timeSinceThis.isFarPast()) {
            timeSinceThis.passedSince().format()
        } else {
            "Unknown (no data yet)"
        }

        ChatUtils.chat("You entered a ${type.displayName} mineshaft!")

        if (type in config.mineshaftsToTrack) {
            TitleManager.sendTitle(type.displayName)

            val message = "§aIt took §e$formattedTime §aand" +
                " §e$sinceThis ${"§amineshaft".pluralize(sinceThis)}" +
                " entered to get a §e${type.displayName} §amineshaft."

            ChatUtils.chat(message)
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

    private fun handleShaftData(type: MineshaftTypes) {
        setSinceMineshaftType(type, 0)
        setTimeSinceMineshaftType(type, SimpleTimeMark.now())

        for (otherTypes in MineshaftTypes.entries) {
            if (otherTypes == type) continue
            setSinceMineshaftType(otherTypes, getSinceMineshaftType(otherTypes) + 1)
        }
    }

    enum class MineshaftTypes(val color: LorenzColor, val rawName: String) {
        TOPA1(LorenzColor.YELLOW, "Topaz"),
        SAPP1(LorenzColor.BLUE, "Sapphire"),
        AMET1(LorenzColor.DARK_PURPLE, "Amethyst"),
        AMBE1(LorenzColor.GOLD, "Amber"),
        JADE1(LorenzColor.GREEN, "Jade"),
        TITA1(LorenzColor.GRAY, "Titanium"),
        UMBE1(LorenzColor.GOLD, "Umber"),
        TUNG1(LorenzColor.DARK_GRAY, "Tungsten"),
        FAIR1(LorenzColor.WHITE, "Vanguard"),
        RUBY1(LorenzColor.RED, "Ruby"),
        RUBY2(LorenzColor.RED, "Ruby Crystal"),
        ONYX1(LorenzColor.BLACK, "Onyx"),
        ONYX2(LorenzColor.BLACK, "Onyx Crystal"),
        AQUA1(LorenzColor.DARK_BLUE, "Aquamarine"),
        AQUA2(LorenzColor.DARK_BLUE, "Aquamarine Crystal"),
        CITR1(LorenzColor.YELLOW, "Citrine"),
        CITR2(LorenzColor.YELLOW, "Citrine Crystal"),
        PERI1(LorenzColor.DARK_GREEN, "Peridot"),
        PERI2(LorenzColor.DARK_GREEN, "Peridot Crystal"),
        JASP1(LorenzColor.LIGHT_PURPLE, "Jasper"),
        JASP2(LorenzColor.LIGHT_PURPLE, "Jasper Crystal"),
        OPAL1(LorenzColor.WHITE, "Opal"),
        OPAL2(LorenzColor.WHITE, "Opal Crystal")
        ;

        val displayName: String = color.getChatColor() + rawName
    }
}
