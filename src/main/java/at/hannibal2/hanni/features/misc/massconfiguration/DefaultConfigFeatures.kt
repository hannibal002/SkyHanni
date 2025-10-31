package at.hannibal2.hanni.features.misc.massconfiguration

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigFileType
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.hanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.hanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.hanni.features.misc.update.ChangelogViewer
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.utils.ChatUtils
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver

@HanniModule
object DefaultConfigFeatures {

    private var didNotifyOnce = false

    @HandleEvent
    fun onHypixelJoin(event: HypixelJoinEvent) {
        if (didNotifyOnce) return
        didNotifyOnce = true

        val knownToggles = HanniMod.knownFeaturesData.knownFeatures
        val updated = HanniMod.VERSION !in knownToggles
        val processor = FeatureToggleProcessor()
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(HanniMod.feature)
        knownToggles[HanniMod.VERSION] = processor.allOptions.map { it.path }
        HanniMod.configManager.saveConfig(ConfigFileType.KNOWN_FEATURES, "Updated known feature flags")
        if (!HanniMod.feature.storage.hasPlayedBefore) {
            HanniMod.feature.storage.hasPlayedBefore = true
            ChatUtils.clickableChat(
                "Looks like this is the first time you are using Hanni. " +
                    "Click here to configure default options, or run /shdefaultoptions.",
                onClick = { onCommand("null", "null") },
                "§eClick to run /shdefaultoptions!",
            )
        } else if (updated) {
            val lastVersion = knownToggles.keys.lastOrNull { it != HanniMod.VERSION }
                ?: ErrorManager.hanniError(
                    "lastVersion is null, this should never happen",
                    "knownToggles" to knownToggles,
                    "version" to HanniMod.VERSION,
                )
            val command = "/shdefaultoptions $lastVersion ${HanniMod.VERSION}"
            ChatUtils.chat("Looks like you updated Hanni.")
            ChatUtils.clickableChat(
                "Click here to configure the newly introduced options, or run $command.",
                onClick = { onCommand(lastVersion, HanniMod.VERSION) },
                "§eClick to run /shdefaultoptions $lastVersion ${HanniMod.VERSION}!",
            )
            ChatUtils.clickableChat(
                "Click here to see the changelog.",
                onClick = {
                    ChangelogViewer.showChangelog(lastVersion, HanniMod.VERSION)
                },
            )
        }
    }

    private fun onCommand(old: String, new: String) {
        val processor = FeatureToggleProcessor()
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(HanniMod.feature)
        var optionList = processor.orderedOptions
        val knownToggles = HanniMod.knownFeaturesData.knownFeatures
        val togglesInNewVersion = knownToggles[new]
        if (new != "null" && togglesInNewVersion == null) {
            ChatUtils.chat("Unknown version $new")
            return
        }
        val togglesInOldVersion = knownToggles[old]
        if (old != "null" && togglesInOldVersion == null) {
            ChatUtils.chat("Unknown version $old")
            return
        }
        optionList = optionList
            .mapValues { option ->
                option.value.filter {
                    (togglesInNewVersion == null || it.path in togglesInNewVersion) &&
                        (togglesInOldVersion == null || it.path !in togglesInOldVersion)
                }
            }
            .filter { (_, filteredOptions) -> filteredOptions.isNotEmpty() }
        if (optionList.isEmpty()) {
            ChatUtils.chat("There are no new options to configure between $old and $new")
            return
        }
        HanniMod.screenToOpen = DefaultConfigOptionGui(optionList, old, new)
    }

    fun applyCategorySelections(
        resetSuggestionState: MutableMap<Category, ResetSuggestionState>,
        orderedOptions: Map<Category, List<FeatureToggleableOption>>,
    ) {
        for ((cat, options) in orderedOptions) {
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

    private val autocomplete get() = HanniMod.knownFeaturesData.knownFeatures.keys + listOf("null")

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdefaultoptions") {
            description = "Select default options"
            arg("oldVersion", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { autocomplete }) { oldVersion ->
                arg("newVersion", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { autocomplete }) { newVersion ->
                    callback {
                        onCommand(getArg(oldVersion), getArg(newVersion))
                    }
                }
                callback {
                    onCommand(getArg(oldVersion), "null")
                }
            }
            simpleCallback {
                onCommand("null", "null")
            }
        }
    }
}
