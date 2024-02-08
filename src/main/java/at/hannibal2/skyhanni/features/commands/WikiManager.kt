package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.net.URLEncoder

object WikiManager {
    private const val OFFICIALURLPREFIX = "https://wiki.hypixel.net/"
    private const val OFFICIALSEARCHPREFIX = "index.php?search="
    private const val FANDOMURLPREFIX = "https://hypixel-skyblock.fandom.com/wiki/"
    private const val FANDOMSEARCHPREFIX = "Special:Search?query="

    private val config get() = SkyHanniMod.feature.commands.betterWiki

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(6, "commands.useFandomWiki", "commands.fandomWiki.enabled")
    }

    @SubscribeEvent
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return
        val message = event.message.lowercase()
        if (!(message.startsWith("/wiki"))) return

        event.isCanceled = true
        if (message == "/wiki") {
            sendWikiMessage()
            return
        }
        if (message.startsWith("/wiki ")){
            val search = event.message.drop("/wiki ".length)
            sendWikiMessage(search)
            return
        }
        if (message == ("/wikithis")) {
            val itemInHand = InventoryUtils.getItemInHand() ?: run {
                LorenzUtils.chat("§cYou must be holding an item to use this command!")
                return
            }
            wikiTheItem(itemInHand, config.autoOpenWiki)
            return
        }
    }

    @SubscribeEvent
    fun onKeybind(event: GuiScreenEvent.KeyboardInputEvent.Post) {
        if (!LorenzUtils.inSkyBlock) return
        val gui = event.gui as? GuiContainer ?: return
        if (NEUItems.neuHasFocus()) return //because good heavens if this worked on neuitems...
        val stack = gui.slotUnderMouse?.stack ?: return

        if (!config.wikiKeybind.isKeyHeld()) return
        wikiTheItem(stack, config.menuOpenWiki)
    }

    private fun wikiTheItem(item: ItemStack, autoOpen: Boolean, useFandom: Boolean = config.useFandom) {
        val itemDisplayName =
            (item.nameWithEnchantment ?: return).replace("§a✔ ", "").replace("§c✖ ", "")
        val internalName = item.getInternalName().asString()
        val wikiUrlSearch = if (internalName != "NONE") internalName else itemDisplayName.removeColor()

        sendWikiMessage(wikiUrlSearch, itemDisplayName, autoOpen, useFandom)
    }

    fun otherWikiCommands(args: Array<String>, useFandom: Boolean, wikithis: Boolean = false) {
        if (wikithis && !LorenzUtils.inSkyBlock) {
            LorenzUtils.chat("§cYou must be in SkyBlock to do this!")
            return
        }

        var search = ""
        for (arg in args) search = "$search${arg}"

        if (wikithis) {
            val itemInHand = InventoryUtils.getItemInHand() ?: run {
                LorenzUtils.chat("§cYou must be holding an item to use this command!")
                return
            }
            wikiTheItem(itemInHand, false, useFandom = useFandom)
            return
        }
        if (search == "") {
            sendWikiMessage(useFandom = useFandom)
            return
        }
        sendWikiMessage(search, useFandom = useFandom)
    }

    fun sendWikiMessage(
        search: String = "", displaySearch: String = search,
        autoOpen: Boolean = config.autoOpenWiki, useFandom: Boolean = config.useFandom
    ) {
        val wiki = if(useFandom) "SkyBlock Fandom Wiki" else "Official SkyBlock Wiki"
        val urlPrefix = if (useFandom) FANDOMURLPREFIX else OFFICIALURLPREFIX
        if (search == "") {
            LorenzUtils.clickableLinkChat(
                "§7Click §e§lHERE §7to visit the §6$wiki§7!", urlPrefix, "§7The $wiki!"
            )
            return
        }

        val urlSearchPrefix = if (useFandom) "$urlPrefix$FANDOMSEARCHPREFIX" else "$urlPrefix$OFFICIALSEARCHPREFIX"
        val searchUrl = "$urlSearchPrefix${URLEncoder.encode(search, "UTF-8")}&scope=internal"

        LorenzUtils.clickableLinkChat(
            "§7Click §e§lHERE §7to find §a$displaySearch §7on the §6$wiki§7!",
            searchUrl,
            "§7View §a$displaySearch §7on the §6$wiki§7!",
            autoOpen
        )
    }

    private fun isEnabled() = config.enabled
}
