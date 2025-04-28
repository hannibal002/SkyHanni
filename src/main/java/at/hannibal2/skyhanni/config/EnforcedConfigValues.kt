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
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.VersionConstants
import at.hannibal2.skyhanni.utils.json.Shimmy
import kotlin.time.Duration.Companion.INFINITE

@SkyHanniModule
object EnforcedConfigValues {

    var enforcedValues: List<EnforcedValueData> = listOf()
    var hasSentPSAsOnce = false

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val constant = event.getConstant<EnforcedConfigValuesJson>("misc/EnforcedConfigValues").enforcedConfigValues
        val oldEnforcedValues = enforcedValues
        enforcedValues = constant.filter {
            SkyHanniMod.modVersion <= it.affectedVersion
        }.filter {
            it.affectedMinecraftVersions?.contains(VersionConstants.MC_VERSION) ?: true
        }
        if (oldEnforcedValues == enforcedValues) return
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

    fun sendPSAs() {
        val notifications = enforcedValues.mapNotNull { it.notificationPSA }
        for (notification in notifications) {
            if (notification.isNotEmpty()) {
                NotificationManager.queueNotification(SkyHanniNotification(notification, INFINITE, true))
            }
        }
        val chat = enforcedValues.flatMap { it.chatPSA ?: emptyList() }
        if (chat.isNotEmpty()) {
            for (line in chat) {
                ChatUtils.chat(line)
            }
        }
    }

    fun enforceOntoConfig(config: Any) {
        for (enforcedValue in enforcedValues.flatMap { it.enforcedValues }) {
            val shimmy = Shimmy.makeShimmy(config, enforcedValue.path.split("."))
            if (shimmy == null) {
                println("Could not create shimmy for path ${enforcedValue.path}")
                continue
            }
            val currentValue = shimmy.getJson()
            if (currentValue != enforcedValue.value) {
                println("Resetting ${enforcedValue.path} to ${enforcedValue.value} from $currentValue")
                shimmy.setJson(enforcedValue.value)
            }
        }
    }

    fun isBlockedFromEditing(optionPath: String): Boolean {
        return enforcedValues.flatMap { it.enforcedValues }.any {
            it.path == optionPath
        }
    }

}
