package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.NotificationManager
import at.hannibal2.skyhanni.data.SkyHanniNotification
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnforcedConfigValuesJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnforcedValue
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnforcedValueData
import at.hannibal2.skyhanni.data.repo.SkyHanniRepoManager
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.json.Shimmy
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import kotlin.time.Duration.Companion.INFINITE

@SkyHanniModule
object EnforcedConfigValues {
    private const val CONSTANT = "misc/EnforcedConfigValues"

    private var enforcedConfigValuesData: List<EnforcedValueData> = listOf()
    private var hasSentPSAsOnce = false

    /**
     * Applies the values cached in the local repo, so that enforcement does not have to wait for the
     * initial repo fetch. Called during config loading, before the config gets built for the first time.
     */
    fun loadFromLocalRepo() {
        val json = SkyHanniRepoManager.readLocalConstantOrNull<EnforcedConfigValuesJson>(CONSTANT) ?: return
        updateData(json)
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    private fun onRepoReload(event: RepositoryReloadEvent) {
        if (!updateData(event.getConstant<EnforcedConfigValuesJson>(CONSTANT))) return
        hasSentPSAsOnce = false
        // We have to recreate the whole config when a value changes
        // so that the option is blocked off inside the config
        SkyHanniMod.configManager.recreateConfig()
        trySendPSAs()
    }

    // Returns whether the set of enforced values changed.
    private fun updateData(json: EnforcedConfigValuesJson): Boolean {
        val oldEnforcedValues = enforcedConfigValuesData
        enforcedConfigValuesData = json.enforcedConfigValues.filter {
            SkyHanniMod.modVersion <= it.affectedVersion &&
                (it.minimumAffectedVersion?.let { minVersion -> SkyHanniMod.modVersion >= minVersion } ?: true)
        }.filter {
            it.affectedMinecraftVersions?.contains(PlatformUtils.MC_VERSION) ?: true
        }
        enforceOntoConfig(SkyHanniMod.feature)
        return oldEnforcedValues != enforcedConfigValuesData
    }

    @HandleEvent
    private fun onWorldChange() = trySendPSAs()

    // PSAs need a player to be shown to, so they wait until the user is actually on Hypixel.
    private fun trySendPSAs() {
        if (hasSentPSAsOnce || !SkyBlockUtils.onHypixel) return
        hasSentPSAsOnce = true
        sendPSAs()
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
            try {
                enforceValue(config, enforcedValue)
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(
                    e, "Failed to enforce a config value from the repo",
                    "path" to enforcedValue.path,
                    "value" to enforcedValue.value,
                )
            }
        }
    }

    private fun enforceValue(config: Any, enforcedValue: EnforcedValue) {
        val shimmy = Shimmy(config, enforcedValue.path.split("."))
            ?: ErrorManager.skyHanniError("Could not create shimmy for path ${enforcedValue.path}")
        if (shimmy.getJson() == enforcedValue.value) return
        shimmy.setJson(enforcedValue.value)
    }

    fun isBlockedFromEditing(optionPath: String): String? {
        val firstEnforcedValue = enforcedConfigValuesData.firstOrNull { enforcedValueData ->
            enforcedValueData.enforcedValues.any { it.path == optionPath }
        } ?: return null
        return firstEnforcedValue.extraMessage.orEmpty()
    }
}
