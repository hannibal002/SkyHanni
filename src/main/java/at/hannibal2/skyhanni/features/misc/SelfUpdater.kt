package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.nea.libautoupdate.PotentialUpdate
import java.util.*

object SelfUpdater {

    fun getUpdateStream() = when (SkyHanniMod.feature.update.updateStream) {
        0 -> null
        1 -> "pre"
        2 -> "full"
        else -> error("Invalid update stream")
    }

    val updates = mutableMapOf<UUID, PotentialUpdate>()

    fun checkForUpdates() = SkyHanniMod.coroutineScope.launch {
        val updateStream = getUpdateStream()
        if (updateStream == null) {
            LorenzUtils.chat("No update stream selected.")
            return@launch
        }
        LorenzUtils.chat("Update check started")
        val potentialUpdate =
            withContext(Dispatchers.IO) { SkyHanniMod.updateContext.checkUpdate(updateStream).await() }
        if (potentialUpdate == null || !potentialUpdate.isUpdateAvailable) {
            LorenzUtils.chat("No update found")
            return@launch
        }
        updates[potentialUpdate.updateUUID] = potentialUpdate
        LorenzUtils.chat("Update found! ${potentialUpdate.update.versionName} Run /skyhanniupdate download ${potentialUpdate.updateUUID} to download the update")
    }

    fun downloadUpdate(uuid: String) {
        val uuid = runCatching { UUID.fromString(uuid) }.getOrElse { UUID.nameUUIDFromBytes(ByteArray(0)) }
        val update = updates[uuid]
        if (update == null) {
            LorenzUtils.chat("Could not find update")
            return
        }
        LorenzUtils.chat("Download started")
        SkyHanniMod.coroutineScope.launch {
            withContext(Dispatchers.IO) {
                update.prepareUpdate()
            }
            LorenzUtils.chat("Update downloaded. Run /skyhanniupdate execute ${update.updateUUID}")
        }
    }

    fun runUpdate(uuid: String) {
        val uuid = runCatching { UUID.fromString(uuid) }.getOrElse { UUID.nameUUIDFromBytes(ByteArray(0)) }
        val update = updates[uuid]
        if (update == null) {
            LorenzUtils.chat("Could not find update")
            return
        }
        LorenzUtils.chat("Executing update")
        update.executePreparedUpdate()
        LorenzUtils.chat("Update enqueued. Restart your game now!")
    }

    fun handle(args: Array<String>) {
        if (args.size !in listOf(0, 2)) {
            LorenzUtils.chat("Invalid usage. Just use /skyhanniupdate")
        }
        when (args.firstOrNull()) {
            null -> checkForUpdates()
            "download" -> downloadUpdate(args[1])
            "execute" -> runUpdate(args[1])
            else -> LorenzUtils.chat("Invalid usage. Just use /skyhanniupdate")
        }
    }

}