package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.NotificationManager
import at.hannibal2.skyhanni.data.SkyHanniNotification
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnforcedConfigValuesJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnforcedValue
import at.hannibal2.skyhanni.data.jsonobjects.repo.EnforcedValueData
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.json.Shimmy
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.JsonElement
import kotlin.time.Duration.Companion.INFINITE

@SkyHanniModule
object EnforcedConfigValues {

    private var enforcedConfigValuesData: List<EnforcedValueData> = listOf()
    private var hasSentPSAsOnce = false

    val userValues: MutableMap<String, JsonElement>
        get() = SkyHanniMod.enforcedUserValuesStorage.userValues

    private val config: Any
        get() = SkyHanniMod.feature

    @HandleEvent(priority = HandleEvent.HIGHEST)
    private fun onRepoReload(event: RepositoryReloadEvent) {
        val json = event.getConstant<EnforcedConfigValuesJson>("misc/EnforcedConfigValues").enforcedConfigValues
        // The repo reloads from a coroutine, but property observers expect the client thread
        DelayedRun.runOrNextTick("EnforcedConfigValues.onRepoReload") {
            if (!updateData(json)) return@runOrNextTick
            hasSentPSAsOnce = false
            // We have to recreate the whole config when a value changes
            // so that the option is blocked off inside the config
            SkyHanniMod.configManager.recreateConfig()
            trySendPSAs()
        }
    }

    internal fun updateData(data: List<EnforcedValueData>): Boolean {
        val oldEnforcedValues = enforcedConfigValuesData

        enforcedConfigValuesData = data
            .filter {
                SkyHanniMod.modVersion <= it.affectedVersion &&
                    (it.minimumAffectedVersion?.let { minVersion -> SkyHanniMod.modVersion >= minVersion } ?: true)
            }.filter {
                it.affectedMinecraftVersions?.contains(PlatformUtils.MC_VERSION) ?: true
            }

        val enforcedValues = enforcedConfigValuesData.flatMap { it.enforcedValues }

        restoreExpiredEnforcedValues(enforcedValues.map { it.path }.toSet())
        enforceOntoConfig(enforcedValues)
        return oldEnforcedValues != enforcedConfigValuesData
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onTick() = trySendPSAs()

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

    private fun enforceOntoConfig(enforcedValues: List<EnforcedValue>) {
        var dirtyUserValues = false
        for ((path, value, persist) in enforcedValues) {
            val shimmy = Shimmy(config, path.split(".")) ?: run {
                SkyHanniMod.logger.warn("Could not create shimmy for path $path")
                continue
            }

            val currentValue = shimmy.getJson()

            if (currentValue != value) {
                try {
                    shimmy.setJson(value)
                } catch (e: Exception) {
                    SkyHanniMod.logger.warn("Could not set enforced value for path $path: ${e.message}")
                    continue
                }
            }

            if (!persist && path !in userValues) {
                userValues[path] = currentValue.deepCopy()
                dirtyUserValues = true
            }
        }

        if (dirtyUserValues) {
            SkyHanniMod.configManager.saveConfig(
                ENFORCED_USER_VALUES,
                "enforced values",
            )
        }
    }

    private fun restoreExpiredEnforcedValues(currentEnforcedPaths: Set<String>) {
        // If there is a user value that is not currently enforced, we should restore it to the config and remove it from the user values.
        val pathsToRestore = userValues.keys
            .filter { it !in currentEnforcedPaths }

        if (pathsToRestore.isEmpty()) return

        for (path in pathsToRestore) {
            val previousValue = userValues[path] ?: continue

            val shimmy = Shimmy(config, path.split(".")) ?: continue
            shimmy.setJson(previousValue)
            userValues.remove(path)
        }

        SkyHanniMod.configManager.saveConfig(
            ENFORCED_USER_VALUES,
            "restored enforced values",
        )
    }

    fun isBlockedFromEditing(optionPath: String): String? {
        val firstEnforcedValue = enforcedConfigValuesData.firstOrNull { enforcedValueData ->
            enforcedValueData.enforcedValues.any { it.path == optionPath }
        } ?: return null

        return firstEnforcedValue.extraMessage.orEmpty()
    }
}
