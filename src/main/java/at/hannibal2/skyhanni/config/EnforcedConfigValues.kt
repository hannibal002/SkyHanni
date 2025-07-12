package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.NotificationManager
import at.hannibal2.skyhanni.data.SkyHanniNotification
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnforcedConfigValuesJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnforcedValueData
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.json.Shimmy
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import kotlin.time.Duration.Companion.INFINITE

@SkyHanniModule
object EnforcedConfigValues {

    private var enforcedConfigValuesData: List<EnforcedValueData> = listOf()
    private var hasSentPSAsOnce = false

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val constant = event.getConstant<EnforcedConfigValuesJson>("misc/EnforcedConfigValues").enforcedConfigValues
        val oldEnforcedValues = enforcedConfigValuesData
        enforcedConfigValuesData = constant.filter {
            SkyHanniMod.modVersion <= it.affectedVersion
        }.filter {
            it.affectedMinecraftVersions?.contains(PlatformUtils.MC_VERSION) ?: true
        }
        if (oldEnforcedValues == enforcedConfigValuesData) return
        hasSentPSAsOnce = false
        // we have to recreate the whole config when a value changes
        // so that the option is blocked off inside the config
        SkyHanniMod.configManager.recreateConfig()
    }

    @HandleEvent
    fun onGuiOpen(event: GuiScreenOpenEvent) {
        enforceOntoConfig(SkyHanniMod.feature)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(tickEvent: SkyHanniTickEvent) {
        if (hasSentPSAsOnce) return
        hasSentPSAsOnce = true
        sendPSAs()
        enforceOntoConfig(SkyHanniMod.feature)
    }

    private fun sendPSAs() {
        val notifications = enforcedConfigValuesData.mapNotNull { it.notificationPSA }
        for (notification in notifications) {
            if (notification.isNotEmpty()) {
                NotificationManager.queueNotification(SkyHanniNotification(notification, INFINITE, true))
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
                    ErrorManager.skyHanniError("Could not create shimmy for path ${enforcedValue.path}")
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
