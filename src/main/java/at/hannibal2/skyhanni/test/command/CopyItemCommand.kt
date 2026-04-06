package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.ItemUtils.getReadableNBTDump
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getMinecraftId
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object CopyItemCommand {

    private val copyItemConfig = CoroutineSettings("copy item command")
    private val itemPropertyMap: Map<String, (ItemStack.() -> String)> = buildMap {
        fun String.quoteWrap(leadingSpace: Boolean = false) = "${if (leadingSpace) " " else ""}'$this'"
        put("internal name") { getInternalName().asString() }
        put("display name") { hoverName.formattedTextCompatLeadingWhiteLessResets().quoteWrap() }
        put("minecraft id") { getMinecraftId().toString().quoteWrap() }
        put("lore") {
            getLoreComponent().joinToString("\n") { it.formattedTextCompat().quoteWrap(true) }
        }
        put("_spacer") { "" }
        put("nbt attributes") {
            val attributeNBT = extraAttributes.getReadableNBTDump()
            if (attributeNBT.isEmpty()) "no tag compound"
            else buildString {
                appendLine("getTagCompound")
            }
        }
    }

    suspend fun copyItemToClipboard(itemStack: ItemStack) {
        val itemInfoString = buildString {
            itemPropertyMap.forEach { (property, calculation) ->
                val propertyValue = calculation(itemStack)
                val propertyPrintName = if (property.startsWith("_")) "" else "$property:"
                val separator = if (propertyValue.contains("\n")) "\n" else " "
                appendLine("$propertyPrintName:${separator}$propertyValue")
            }
        }

        val copied = OSUtils.copyToClipboardAsync(itemInfoString) ?: false
        if (!copied) ChatUtils.chat("Failed to copy item to clipboard!")
        else ChatUtils.chat("Item info copied into the clipboard!")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcopyitem") {
            description = "Copies information about the item in hand to the clipboard"
            category = CommandCategory.DEVELOPER_DEBUG
            coroutineSimpleCallback(copyItemConfig) {
                val itemStack = InventoryUtils.getItemInHand()
                    ?: return@coroutineSimpleCallback ChatUtils.userError("No item in hand!")
                copyItemToClipboard(itemStack)
            }
        }
    }
}
