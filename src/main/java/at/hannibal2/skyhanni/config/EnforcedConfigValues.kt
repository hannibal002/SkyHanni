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
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.json.Shimmy
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.JsonElement
import com.google.gson.JsonObject
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
    // Guarded by its own monitor: the config auto-save thread reads it while it serializes the config.
    internal val userValues = mutableMapOf<String, UserValue>()

    internal class UserValue(val userValue: JsonElement, val enforcedValue: JsonElement)

    /**
     * Applies the values cached in the local repo, so that enforcement does not have to wait for the
     * initial repo fetch. Called during config loading, before the config gets built for the first time.
     */
    fun loadFromLocalRepo() {
        val json = SkyHanniRepoManager.readLocalConstantOrNull<EnforcedConfigValuesJson>(CONSTANT) ?: return
        try {
            updateData(json)
        } catch (e: Exception) {
            // Gson does not enforce Kotlin nullability, so a malformed cache can still fail here.
            // Nothing is enforced until the repo reload delivers proper data, rather than failing the config load.
            enforcedConfigValuesData = listOf()
            ErrorManager.logErrorWithData(e, "Failed to apply cached enforced config values")
        }
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    private fun onRepoReload(event: RepositoryReloadEvent) {
        val json = event.getConstant<EnforcedConfigValuesJson>(CONSTANT)
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

    // PSAs need a player to be shown to, so they wait until the user is actually on Hypixel.
    @HandleEvent
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

    private fun enforceOntoConfig(config: Any) = synchronized(userValues) {
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

    internal fun enforceValue(config: Any, enforcedValue: EnforcedValue) {
        val shimmy = Shimmy(config, enforcedValue.path.split("."))
        if (shimmy == null) {
            ChatUtils.debug("EnforcedConfigValues: Could not create shimmy for path ${enforcedValue.path}; skipping")
            return
        }
        // Config options are never null, and Shimmy leaves explicit nulls to the caller
        require(!enforcedValue.value.isJsonNull) { "Enforced value for ${enforcedValue.path} is null" }
        val currentValue = shimmy.getJson()
        // When the option is already enforced, the current value is a previously enforced one, not the user's,
        // unless the user changed it in the meantime (e.g. via /shconfig set)
        val previous = userValues[enforcedValue.path]
        val userValue = if (previous != null && currentValue == previous.enforcedValue) previous.userValue else currentValue
        try {
            shimmy.setJson(enforcedValue.value)
        } finally {
            // A property observer can throw after the value has already been applied, so the backup is recorded
            // regardless. It is re-read so that it compares equal to what the field serializes to later
            // (e.g. a float from the repo JSON does not equal the same float serialized by Gson)
            userValues[enforcedValue.path] = UserValue(userValue, shimmy.getJson())
        }
        // Persistent values replace the user's value for good, so there is nothing to restore later
        if (enforcedValue.persist) userValues.remove(enforcedValue.path)
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
     * Serializes [config] with every enforced option replaced by the value the user set themselves,
     * so that the enforced values never end up in the config file.
     */
    fun toUserJsonTree(config: Any): JsonElement = synchronized(userValues) {
        val json = ConfigManager.gson.toJsonTree(config)
        for ((path, backup) in userValues) {
            val segments = path.split(".")
            val parent = segments.dropLast(1).fold<String, JsonElement?>(json) { element, segment ->
                (element as? JsonObject)?.get(segment)
            } as? JsonObject ?: continue
            // Skips options that are not part of the file (e.g. not exposed) and values changed after enforcement.
            // The snapshot is compared rather than the live field, which the client thread can change in between
            if (parent.get(segments.last()) != backup.enforcedValue) continue
            parent.add(segments.last(), backup.userValue)
        }
        json
    }

    fun isBlockedFromEditing(optionPath: String): String? {
        val firstEnforcedValue = enforcedConfigValuesData.firstOrNull { enforcedValueData ->
            enforcedValueData.enforcedValues.any { it.path == optionPath }
        } ?: return null
        return firstEnforcedValue.extraMessage.orEmpty()
    }
}
