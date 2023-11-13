package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import io.github.moulberry.moulconfig.processor.ConfigProcessorDriver
import net.minecraft.client.Minecraft
import net.minecraft.command.CommandBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object DefaultConfigFeatures {
    private var didNotifyOnce = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (didNotifyOnce) return
        Minecraft.getMinecraft().thePlayer ?: return
        didNotifyOnce = true

        val knownToggles = SkyHanniMod.feature.storage.knownFeatureToggles
        val updated = SkyHanniMod.version !in knownToggles
        val processor = FeatureToggleProcessor()
        ConfigProcessorDriver.processConfig(SkyHanniMod.feature.javaClass, SkyHanniMod.feature, processor)
        knownToggles[SkyHanniMod.version] = processor.allOptions.map { it.path }
        SkyHanniMod.configManager.saveConfig("Updated known feature flags")
        if (!SkyHanniMod.feature.storage.hasPlayedBefore) {
            SkyHanniMod.feature.storage.hasPlayedBefore = true
            LorenzUtils.clickableChat(
                "§e[SkyHanni] Looks like this is the first time you are using SkyHanni. " +
                    "Click here to configure default options, or run /shdefaultoptions.",
                "shdefaultoptions"
            )
        } else if (updated) {
            val mostFeatureFulOldVersion =
                knownToggles.maxByOrNull { if (it.key != SkyHanniMod.version) it.value.size else -1 }
            val command = "/shdefaultoptions ${mostFeatureFulOldVersion?.key} ${SkyHanniMod.version}"
            LorenzUtils.clickableChat(
                "§e[SkyHanni] Looks like you updated SkyHanni. " +
                    "Click here to configure the newly introduced options, or run $command.",
                command
            )
        }
    }

    fun onCommand(old: String, new: String) {
        val processor = FeatureToggleProcessor()
        ConfigProcessorDriver.processConfig(SkyHanniMod.feature.javaClass, SkyHanniMod.feature, processor)
        var optionList = processor.orderedOptions
        val knownToggles = SkyHanniMod.feature.storage.knownFeatureToggles
        val togglesInNewVersion = knownToggles[new]
        if (new != "null" && togglesInNewVersion == null) {
            LorenzUtils.chat("§e[SkyHanni] Unknown version $new")
            return
        }
        val togglesInOldVersion = knownToggles[old]
        if (old != "null" && togglesInOldVersion == null) {
            LorenzUtils.chat("§e[SkyHanni] Unknown version $old")
            return
        }
        optionList = optionList
            .mapValues { it ->
                it.value.filter {
                    (togglesInNewVersion == null || it.path in togglesInNewVersion) &&
                        (togglesInOldVersion == null || it.path !in togglesInOldVersion)
                }
            }
            .filter { (_, filteredOptions) -> filteredOptions.isNotEmpty() }
        if (optionList.isEmpty()) {
            LorenzUtils.chat("There are no new options to configure between $old and $new")
            return
        }
        SkyHanniMod.screenToOpen = DefaultConfigOptionGui(optionList, old, new)
    }

    fun applyCategorySelections(
        resetSuggestionState: MutableMap<Category, ResetSuggestionState>,
        orderedOptions: Map<Category, List<FeatureToggleableOption>>
    ) {
        orderedOptions.forEach { (cat, options) ->
            for (option in options) {
                val resetState = option.toggleOverride ?: resetSuggestionState[cat]!!
                if (resetState == ResetSuggestionState.LEAVE_DEFAULTS) continue
                val onState = option.isTrueEnabled
                val setTo = if (resetState == ResetSuggestionState.TURN_ALL_ON) {
                    onState
                } else {
                    !onState
                }
                option.setter(setTo)
            }
        }
    }

    fun onComplete(strings: Array<String>): List<String> {
        if (strings.size <= 2)
            return CommandBase.getListOfStringsMatchingLastWord(
                strings,
                SkyHanniMod.feature.storage.knownFeatureToggles.keys + listOf("null")
            )
        return listOf()
    }
}
