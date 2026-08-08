package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.jsonobjects.repo.WikiJson
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
    private lateinit var data: WikiJson

    private val config get() = SkyHanniMod.feature.misc.commands.betterWiki

    val wiki get() = data.unofficial

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(6, "commands.useFandomWiki", "commands.fandomWiki.enabled")
        // Apparently the above got changed again at some point but never got a migration
        event.move(123, "commands.betterWiki.useFandom", "commands.betterWiki.useIndependent")
        event.move(136, "commands.betterWiki.sbGuide", "commands.betterWiki.skyblockGuide")
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onGuiKeyPress() {
        val stack = stackUnderCursor() ?: return

        if (!config.wikiKeybind.isKeyHeld()) return
        wikiTheItem(stack, config.menuOpenWiki)
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

    fun wikiThisItem(autoOpen: Boolean = config.autoOpenWiki) {
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
        autoOpen: Boolean = config.autoOpenWiki,
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

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shindependentwiki") {
            aliases = listOf("shunofficialwiki", "shfandomwiki", "wiki")
            description = "Searches the independent wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            simpleCallback {
                sendWikiMessage("")
            }
            argCallback("search", BrigadierArguments.greedyString(), ) { search ->
                sendWikiMessage(search)
            }
        }
        event.registerBrigadier("shindependentwikithis") {
            aliases = listOf("shunofficialwikithis", "shfandomwikithis", "wikithis")
            description = "Searches the independent wiki with SkyHanni's own method."
            category = CommandCategory.USERS_ACTIVE
            simpleCallback {
                wikiThisItem()
            }
        }
    }

    @HandleEvent(priority = HandleEvent.LOW)
    private fun onRepoReload(event: RepositoryReloadEvent) {
        data = event.getConstant<WikiJson>("misc/Wiki")
    }

    // TOOD: Make it disablable
    private fun isEnabled() = config.enabled
}
