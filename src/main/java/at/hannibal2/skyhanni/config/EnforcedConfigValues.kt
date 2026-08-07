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
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.INFINITE

/**
 * Overrides config options with values from the repo. By default, the overrides only ever exist in memory: the config
 * file on disk keeps the user's own values, so lifting an enforcement (or downgrading) never loses them. Enforced
 * values marked as persistent are written to the config file like any other value instead.
 */
@SkyHanniModule
object EnforcedConfigValues {
    private const val CONSTANT = "misc/EnforcedConfigValues"

    private var enforcedConfigValuesData: List<EnforcedValueData> = listOf()
    private var hasSentPSAsOnce = false

    // The user's own values of the options that are currently enforced, keyed by config path.
    // Accessed from the config auto-save thread as well, hence the concurrent map.
    private val userValues = ConcurrentHashMap<String, UserValue>()

    private class UserValue(val userValue: JsonElement, val enforcedValue: JsonElement)

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
        val enforcedValues = enforcedConfigValuesData.flatMap { it.enforcedValues }
        restoreNoLongerEnforced(config, enforcedValues.mapTo(mutableSetOf()) { it.path })

        for (enforcedValue in enforcedValues) {
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
        val currentValue = shimmy.getJson()
        if (enforcedValue.persist) {
            // Persistent values replace the user's value for good, so there is nothing to restore later
            userValues.remove(enforcedValue.path)
        } else if (currentValue != enforcedValue.value) {
            // When the option is already enforced, the current value is a previously enforced one, not the user's
            val userValue = userValues[enforcedValue.path]?.userValue ?: currentValue
            userValues[enforcedValue.path] = UserValue(userValue, enforcedValue.value)
        }
        if (currentValue == enforcedValue.value) return
        shimmy.setJson(enforcedValue.value)
    }

    private fun restoreNoLongerEnforced(config: Any, enforcedPaths: Set<String>) {
        for (path in userValues.keys.filter { it !in enforcedPaths }) {
            val backup = userValues.remove(path) ?: continue
            val shimmy = Shimmy(config, path.split(".")) ?: continue
            // Keep the current value if anything changed it after it was enforced
            if (shimmy.getJson() != backup.enforcedValue) continue
            shimmy.setJson(backup.userValue)
        }
    }

    /**
     * Replaces every enforced option in the serialized [config] with the value the user set themselves,
     * so that the enforced values never end up in the config file.
     */
    fun writeUserValues(config: JsonElement) {
        for ((path, backup) in userValues) {
            val segments = path.split(".")
            val parent = segments.dropLast(1).fold<String, JsonElement?>(config) { element, segment ->
                (element as? JsonObject)?.get(segment)
            } as? JsonObject ?: continue
            // Options that are not part of the file (e.g. not exposed) have nothing to restore
            if (!parent.has(segments.last())) continue
            parent.add(segments.last(), backup.userValue)
        }
    }

    fun isBlockedFromEditing(optionPath: String): String? {
        val firstEnforcedValue = enforcedConfigValuesData.firstOrNull { enforcedValueData ->
            enforcedValueData.enforcedValues.any { it.path == optionPath }
        } ?: return null
        return firstEnforcedValue.extraMessage.orEmpty()
    }
}
