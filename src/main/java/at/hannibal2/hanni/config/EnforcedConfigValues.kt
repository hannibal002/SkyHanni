package at.hannibal2.hanni.config

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.NotificationManager
import at.hannibal2.hanni.data.HanniNotification
import at.hannibal2.hanni.data.jsonobjects.repo.EnforcedConfigValuesJson
import at.hannibal2.hanni.data.jsonobjects.repo.EnforcedValueData
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.events.render.gui.GuiScreenOpenEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.json.Shimmy
import at.hannibal2.hanni.utils.system.PlatformUtils
import kotlin.time.Duration.Companion.INFINITE

@HanniModule
object EnforcedConfigValues {

    private var enforcedConfigValuesData: List<EnforcedValueData> = listOf()
    private var hasSentPSAsOnce = false

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val constant = event.getConstant<EnforcedConfigValuesJson>("misc/EnforcedConfigValues").enforcedConfigValues
        val oldEnforcedValues = enforcedConfigValuesData
        enforcedConfigValuesData = constant.filter {
            HanniMod.modVersion <= it.affectedVersion &&
                (it.minimumAffectedVersion?.let { minVersion -> HanniMod.modVersion >= minVersion } ?: true)
        }.filter {
            it.affectedMinecraftVersions?.contains(PlatformUtils.MC_VERSION) ?: true
        }
        if (oldEnforcedValues == enforcedConfigValuesData) return
        hasSentPSAsOnce = false
        // we have to recreate the whole config when a value changes
        // so that the option is blocked off inside the config
        HanniMod.configManager.recreateConfig()
    }

    @HandleEvent
    fun onGuiOpen(event: GuiScreenOpenEvent) {
        enforceOntoConfig(HanniMod.feature)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(tickEvent: HanniTickEvent) {
        if (hasSentPSAsOnce) return
        hasSentPSAsOnce = true
        sendPSAs()
        enforceOntoConfig(HanniMod.feature)
    }

    private fun sendPSAs() {
        val notifications = enforcedConfigValuesData.mapNotNull { it.notificationPSA }
        for (notification in notifications) {
            if (notification.isNotEmpty()) {
                NotificationManager.queueNotification(HanniNotification(notification, INFINITE, true))
            }
        }
        val chat = enforcedConfigValuesData.flatMap { it.chatPSA.orEmpty() }
        if (chat.isNotEmpty()) {
            var shouldPrefix = true
            for (line in chat) {
                ChatUtils.chat(line, prefix = shouldPrefix)
                shouldPrefix = false
            }
        }
    }

    private fun enforceOntoConfig(config: Any) {
        for (enforcedValue in enforcedConfigValuesData.flatMap { it.enforcedValues }) {
            val shimmy = Shimmy.makeShimmy(config, enforcedValue.path.split("."))
            if (shimmy == null) {
                try {
                    ErrorManager.hanniError("Could not create shimmy for path ${enforcedValue.path}")
                } catch (_: Exception) {
                    continue
                }
            }
            val currentValue = shimmy.getJson()
            if (currentValue != enforcedValue.value) {
                shimmy.setJson(enforcedValue.value)
            }
        }
    }

    fun isBlockedFromEditing(optionPath: String): String? {
        val firstEnforcedValue = enforcedConfigValuesData.firstOrNull { enforcedValueData ->
            enforcedValueData.enforcedValues.any { it.path == optionPath }
        } ?: return null
        return firstEnforcedValue.extraMessage.orEmpty()
    }

}
