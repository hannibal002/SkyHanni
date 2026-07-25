package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.WikiJson
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.stackUnderCursor
import java.net.URLEncoder

@SkyHanniModule
object WikiManager {
    lateinit var data: WikiJson
        private set

    private val config get() = SkyHanniMod.feature.misc.commands.betterWiki

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(6, "commands.useFandomWiki", "commands.fandomWiki.enabled")
        // Apparently the above got changed again at some point but never got a migration
        event.move(123, "commands.betterWiki.useFandom", "commands.betterWiki.useIndependent")

        event.move(136, "commands.betterWiki.sbGuide", "commands.betterWiki.skyblockGuide", { element ->
            config.enabled = true
            return@move element
        })
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!isEnabled()) return
        val message = event.message.lowercase()
        if (!(message.startsWith("/wiki"))) return

        event.cancel()
        if (message == "/wiki") {
            sendWikiMessage()
            return
        }
        if (message.startsWith("/wiki ")) {
            val search = event.message.drop("/wiki ".length)
            sendWikiMessage(search)
            return
        }
        if (message == ("/wikithis")) {
            val itemInHand = InventoryUtils.getItemInHand() ?: run {
                ChatUtils.userError("You must be holding an item to use this command!")
                return
            }
            wikiTheItem(itemInHand, config.autoOpenWiki)
            return
        }
    }

    @HandleEvent(GuiKeyPressEvent::class, onlyOnSkyblock = true)
    fun onKeybind() {
        val stack = stackUnderCursor() ?: return

        if (!config.wikiKeybind.isKeyHeld()) return
        wikiTheItem(stack, config.menuOpenWiki)
    }

    fun getSearchUrl(search: String, useIndependent: Boolean = config.useIndependent): String {
        val wiki = if (useIndependent) data.unofficial else data.official
        val urlSearchPrefix = wiki.fullSearchPrefix
        return "$urlSearchPrefix${URLEncoder.encode(search, "UTF-8")}&scope=internal"
    }

    private fun wikiTheItem(item: SafeItemStack, autoOpen: Boolean, useIndependent: Boolean = config.useIndependent) {
        val itemDisplayName =
            item.hoverName.formattedTextCompatLeadingWhiteLessResets().replace("§a✔ ", "").replace("§c✖ ", "")
        val internalName = item.getInternalName().asString()
        val wikiUrlSearch = if (internalName != "NONE") internalName else itemDisplayName.removeColor()

        sendWikiMessage(wikiUrlSearch, itemDisplayName.removeColor(), autoOpen, useIndependent)
    }

    fun otherWikiCommands(args: Array<String>, useIndependent: Boolean, wikithis: Boolean = false) {
        if (wikithis && !SkyBlockUtils.inSkyBlock) {
            ChatUtils.userError("You must be in SkyBlock to do this!")
            return
        }

        var search = ""
        for (arg in args) search = "$search$arg"

        if (wikithis) {
            val itemInHand = InventoryUtils.getItemInHand() ?: run {
                ChatUtils.userError("You must be holding an item to use this command!")
                return
            }
            wikiTheItem(itemInHand, false, useIndependent = useIndependent)
            return
        }
        if (search == "") {
            sendWikiMessage(useIndependent = useIndependent)
            return
        }
        sendWikiMessage(search, useIndependent = useIndependent)
    }

    fun sendWikiMessage(
        search: String? = null,
        displaySearch: String? = search,
        autoOpen: Boolean = config.autoOpenWiki,
        useIndependent: Boolean = config.useIndependent,
    ) {
        val wiki = if (useIndependent) data.unofficial else data.official

        if (search.isNullOrBlank()) {
            ChatUtils.clickableLinkChat(
                "§7Click §e§lHERE §7to visit the §6${wiki.name}§7!",
                wiki.urlPrefix,
            )
        } else {
            ChatUtils.clickableLinkChat(
                "§7Click §e§lHERE §7to find §a$displaySearch §7on the §6${wiki.name}§7!",
                getSearchUrl(search, useIndependent = useIndependent),
                "§7Search for §a$search §7on the §6${wiki.name}§7",
                autoOpen,
            )
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shindependentwiki") {
            aliases = listOf("shunofficialwiki", "shfandomwiki")
            description = "Searches the independent wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            legacyCallbackArgs { otherWikiCommands(it, true) }
        }
        event.registerBrigadier("shindependentwikithis") {
            aliases = listOf("shunofficialwikithis", "shfandomwikithis")
            description = "Searches the independent wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            legacyCallbackArgs { otherWikiCommands(it, useIndependent = true, wikithis = true) }
        }
        event.registerBrigadier("shofficialwiki") {
            description = "Searches the official wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            legacyCallbackArgs { otherWikiCommands(it, false) }
        }
        event.registerBrigadier("shofficialwikithis") {
            description = "Searches the official wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            legacyCallbackArgs { otherWikiCommands(it, useIndependent = false, wikithis = true) }
        }
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onRepoReload(event: RepositoryReloadEvent) {
        data = event.getConstant<WikiJson>("misc/Wiki")
    }

    private fun isEnabled() = config.enabled

}
