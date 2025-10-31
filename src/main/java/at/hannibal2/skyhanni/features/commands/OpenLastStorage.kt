package at.hannibal2.hanni.features.commands

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.data.ProfileStorageData
import at.hannibal2.hanni.events.MessageSendToServerEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ChatUtils.senderIsHanni
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.NumberUtil.formatIntOrUserError
import at.hannibal2.hanni.utils.SkyBlockUtils

@HanniModule
object OpenLastStorage {

    // TODO use [StorageApi]

    private val config get() = HanniMod.feature.misc.lastStorage
    private val storage get() = ProfileStorageData.profileSpecific?.lastStorage

    enum class StorageType(private val validPages: IntRange, val runCommand: (Int) -> Unit, vararg val commands: String) {
        ENDER_CHEST(1..9, { HypixelCommands.enderChest(it) }, "/enderchest", "/ec"),
        BACKPACK(0..18, { HypixelCommands.backPack(it) }, "/backpack", "/bp"),
        ;

        val storageName = name.lowercase().replace("_", " ")
        fun isValidPage(page: Int) = page in validPages

        companion object {
            fun fromCommand(command: String): StorageType? {
                return entries.find { command in it.commands }
            }
        }
    }

    private fun openLastStoragePage(type: StorageType) {
        val message = storage?.page?.let { page ->
            type.runCommand(page)
            "Opened last ${type.storageName} $page."
        } ?: run {
            ChatUtils.sendMessageToServer("/${config.fallbackCommand}")
            "No last ${type.storageName} found. Running /${config.fallbackCommand}."
        }
        ChatUtils.chat(message)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!isEnabled()) return
        if (event.senderIsHanni()) return
        val args = event.message.lowercase().split(" ")
        val type = StorageType.fromCommand(args[0]) ?: return

        if (handleStorage(args, type)) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shlastopened") {
            description = "Opens the storage page last accessed by either /ec or /bp"
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("shlo")
            callback {
                val storage = storage ?: return@callback
                if (isEnabled() && SkyBlockUtils.inSkyBlock) {
                    openLastStoragePage(storage.type)
                } else {
                    ChatUtils.chatAndOpenConfig(
                        "This feature is disabled, enable it in the config if you want to use it.",
                        config::openLastStorage,
                    )
                }
            }
        }
    }

    private fun handleStorage(args: List<String>, type: StorageType): Boolean {
        if (args.getOrNull(1) == "-") {
            openLastStoragePage(type)
            return true
        }
        val storage = storage ?: return false
        storage.page = if (args.size <= 1) {
            // No argument means open the first page of the respective storage
            1
        } else {
            val pageNumber = args[1].formatIntOrUserError() ?: return false
            pageNumber.takeIf { type.isValidPage(it) }
        }
        storage.type = type
        return false
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(71, "misc.openLastStorage", "misc.lastStorage.openLastStorage")
    }

    private fun isEnabled() = config.openLastStorage
}
