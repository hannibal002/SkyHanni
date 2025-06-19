package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.getReadableNBTDump
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getMinecraftId
import net.minecraft.item.ItemStack

@SkyHanniModule
object CopyItemCommand {

    private fun command() {
        val itemStack = InventoryUtils.getItemInHand()
        if (itemStack == null) {
            ChatUtils.userError("No item in hand!")
            return
        }
        copyItemToClipboard(itemStack)
    }

    fun copyItemToClipboard(itemStack: ItemStack) {
        val resultList = mutableListOf<String>()
        resultList.add("internal name: " + itemStack.getInternalName().asString())
        resultList.add("display name: '" + itemStack.displayName.toString() + "'")
        resultList.add("minecraft id: '" + itemStack.getMinecraftId() + "'")
        resultList.add("lore:")
        for (line in itemStack.getLore()) {
            resultList.add(" '$line'")
        }
        resultList.add("")
        val attributes = itemStack.extraAttributes.getReadableNBTDump()
        if (attributes.isEmpty()) {
            resultList.add("no tag compound")
        } else {
            resultList.add("getTagCompound")
            resultList.addAll(attributes)
        }

        val string = resultList.joinToString("\n")
        OSUtils.copyToClipboard(string)
        ChatUtils.chat("Item info copied into the clipboard!")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcopyitem") {
            description = "Copies information about the item in hand to the clipboard"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { command() }
        }
    }
}
