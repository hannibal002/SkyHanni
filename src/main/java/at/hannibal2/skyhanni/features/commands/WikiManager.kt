package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.jsonobjects.repo.WikiJson
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.takeUnlessEmpty
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.net.URLEncoder

@SkyHanniModule
object WikiManager {
    private lateinit var data: WikiJson

    private val config get() = SkyHanniMod.feature.misc.commands.betterWiki

    val wiki get() = data.unofficial

    /**
     * REGEX-TEST: Close
     * REGEX-TEST: Go Back
     */
    private val ignoredKeybindItemPattern by RepoPattern.pattern(
        "commands.wiki.ignored-item",
        "Close|Go Back",
    )

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(136, "commands.betterWiki.sbGuide", "commands.betterWiki.skyblockGuide")
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onGuiKeyPress(event: GuiKeyPressEvent) {
        if (!config.wikiKeybind.isKeyHeld()) return

        val stack = event.stackUnderCursor()?.takeUnlessEmpty() ?: return
        if (isIgnoredItem(stack)) return

        wikiTheItem(stack, config.menuOpenWiki)
    }

    // Menu items the wiki search should never be triggered on
    private fun isIgnoredItem(stack: SafeItemStack): Boolean {
        if (ItemUtils.isSkyBlockMenuItem(stack)) return true
        if (stack.getInternalNameOrNull() != null) return false

        // Filler panes have a blank name, which would open the wiki start page
        val name = stack.cleanName.trim()
        return name.isBlank() || ignoredKeybindItemPattern.matches(name)
    }

    fun getSearchUrl(search: String): String {
        val urlSearchPrefix = wiki.fullSearchPrefix
        return "$urlSearchPrefix${URLEncoder.encode(search, "UTF-8")}&scope=internal"
    }

    private fun wikiTheItem(item: SafeItemStack, autoOpen: Boolean) {
        val itemDisplayName =
            item.hoverName.formattedTextCompatLeadingWhiteLessResets().replace("§a✔ ", "").replace("§c✖ ", "")
        val internalName = item.getInternalName().asString()
        val wikiUrlSearch = if (internalName != "NONE") internalName else itemDisplayName.removeColor()

        sendWikiMessage(wikiUrlSearch, itemDisplayName.removeColor(), autoOpen)
    }

    fun wikiThisItem(autoOpen: Boolean) {
        if (!SkyBlockUtils.inSkyBlock) {
            ChatUtils.userError("You must be in SkyBlock to do this!")
            return
        }

        val itemInHand = InventoryUtils.getItemInHand() ?: run {
            ChatUtils.userError("You must be holding an item to use this command!")
            return
        }
        wikiTheItem(itemInHand, autoOpen)
    }

    fun sendWikiMessage(
        search: String? = null,
        displaySearch: String? = search,
        autoOpen: Boolean,
    ) {
        if (search.isNullOrBlank()) {
            ChatUtils.clickableLinkChat(
                "§7Click §e§lHERE §7to visit the §6${wiki.name}§7!",
                wiki.urlPrefix,
            )
        } else {
            ChatUtils.clickableLinkChat(
                "§7Click §e§lHERE §7to find §a$displaySearch §7on the §6${wiki.name}§7!",
                getSearchUrl(search),
                "§7Search for §a$search §7on the §6${wiki.name}§7",
                autoOpen,
            )
        }
    }

    private fun wikiCommand(search: String? = null) {
        if (!SkyBlockUtils.inSkyBlock) {
            ChatUtils.userError("You must be in SkyBlock to do this!")
            return
        }

        sendWikiMessage(search, autoOpen = config.autoOpenWiki)
    }

    // TODO: Make it possible to disable the /wiki and /wikithis aliases.
    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shwiki") {
            aliases = listOf("shindependentwiki", "shunofficialwiki", "shfandomwiki", "wiki")
            description = "Searches the independent wiki."
            category = CommandCategory.USERS_ACTIVE
            simpleCallback {
                wikiCommand()
            }
            argCallback("search", BrigadierArguments.greedyString()) { search ->
                wikiCommand(search)
            }
        }
        event.registerBrigadier("shwikithis") {
            aliases = listOf(
                "shindependentwikithis", "shunofficialwikithis", "shfandomwikithis",
                "wikithis", "wikihand",
            )
            description = "Searches the currently held item in the independent wiki."
            category = CommandCategory.USERS_ACTIVE
            simpleCallback {
                wikiThisItem(config.autoOpenWiki)
            }
        }
    }

    @HandleEvent(priority = HandleEvent.LOW)
    private fun onRepoReload(event: RepositoryReloadEvent) {
        data = event.getConstant<WikiJson>("misc/Wiki")
    }
}
