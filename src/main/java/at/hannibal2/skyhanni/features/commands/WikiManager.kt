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
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.stackUnderCursor
import net.minecraft.world.item.ItemStack
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
        event.move(123, "commands.betterWiki.useFandom", "commands.betterWiki.useUnofficial")
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

    fun getSearchUrl(search: String, useUnofficial: Boolean = config.useUnofficial): String {
        val wiki = if (useUnofficial) data.unofficial else data.official
        val urlSearchPrefix = wiki.urlPrefix + wiki.searchPrefix
        return "$urlSearchPrefix${URLEncoder.encode(search, "UTF-8")}&scope=internal"
    }

    private fun wikiTheItem(item: ItemStack, autoOpen: Boolean, useUnofficial: Boolean = config.useUnofficial) {
        val itemDisplayName =
            item.hoverName.formattedTextCompatLeadingWhiteLessResets().replace("§a✔ ", "").replace("§c✖ ", "")
        val internalName = item.getInternalName().asString()
        val wikiUrlSearch = if (internalName != "NONE") internalName else itemDisplayName.removeColor()

        sendWikiMessage(wikiUrlSearch, itemDisplayName.removeColor(), autoOpen, useUnofficial)
    }

    fun otherWikiCommands(args: Array<String>, useUnofficial: Boolean, wikithis: Boolean = false) {
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
            wikiTheItem(itemInHand, false, useUnofficial = useUnofficial)
            return
        }
        if (search == "") {
            sendWikiMessage(useUnofficial = useUnofficial)
            return
        }
        sendWikiMessage(search, useUnofficial = useUnofficial)
    }

    fun sendWikiMessage(
        search: String? = null,
        displaySearch: String? = search,
        autoOpen: Boolean = config.autoOpenWiki,
        useUnofficial: Boolean = config.useUnofficial,
    ) {
        val wiki = if (useUnofficial) data.unofficial else data.official

        if (search.isNullOrBlank()) {
            ChatUtils.clickableLinkChat(
                "§7Click §e§lHERE §7to visit the §6${wiki.name}§7!",
                wiki.urlPrefix,
            )
        } else {
            ChatUtils.clickableLinkChat(
                "§7Click §e§lHERE §7to find §a$displaySearch §7on the §6${wiki.name}§7!",
                getSearchUrl(search, useUnofficial = useUnofficial),
                "§7Search for §a$search §7on the §6${wiki.name}§7",
                autoOpen,
            )
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shunofficialwiki") {
            aliases = listOf("shfandomwiki")
            description = "Searches the unofficial wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            legacyCallbackArgs { otherWikiCommands(it, true) }
        }
        event.registerBrigadier("shunofficialwikithis") {
            aliases = listOf("shfandomwikithis")
            description = "Searches the unofficial wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            legacyCallbackArgs { otherWikiCommands(it, useUnofficial = true, wikithis = true) }
        }
        event.registerBrigadier("shofficialwiki") {
            description = "Searches the official wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            legacyCallbackArgs { otherWikiCommands(it, false) }
        }
        event.registerBrigadier("shofficialwikithis") {
            description = "Searches the official wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            legacyCallbackArgs { otherWikiCommands(it, useUnofficial = false, wikithis = true) }
        }
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onRepoReload(event: RepositoryReloadEvent) {
        data = event.getConstant<WikiJson>("misc/Wiki")
    }

    private fun isEnabled() = config.enabled

}
