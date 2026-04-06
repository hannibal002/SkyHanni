package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.CopyItemCommand.copyItemToClipboard
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.compat.stackUnderCursor
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.JsonElement
import net.minecraft.world.item.ItemStack
import java.io.InputStreamReader
import java.io.Reader

@SkyHanniModule
object TestExportTools {
    private val config get() = DevApi.config.debug
    private val copyConfig = CoroutineSettings("copy item data")
    internal val itemKey = Key<ItemStack>("Item")
    class Key<T> internal constructor(val name: String)

    @KSerializable
    data class TestValue(
        val type: String,
        val data: JsonElement,
    )

    private fun <T> toJson(key: Key<T>, value: T): String = with(ConfigManager.gson) {
        toJson(TestValue(key.name, toJsonTree(value)))
    }

    inline fun <reified T> fromJson(key: Key<T>, reader: Reader): T = with(ConfigManager.gson) {
        val serializable = fromJson<TestValue>(reader)
        require(key.name == serializable.type)
        return fromJson(serializable.data)
    }

    @HandleEvent
    fun onKeybind(event: GuiKeyPressEvent) {
        if (!config.copyItemDataCompressed.isKeyHeld() && !config.copyItemData.isKeyHeld()) return
        val stack = stackUnderCursor() ?: return
        copyConfig.launch {
            if (config.copyItemData.isKeyHeld()) copyItemToClipboard(stack)
            else {
                val itemJson = toJson(itemKey, stack)
                val copied = ClipboardUtils.copyToClipboardAsync(itemJson).await() ?: false
                if (!copied) ChatUtils.chat("Failed to copy item to clipboard!")
                else ChatUtils.chat("Compressed item info copied into the clipboard!")
            }
        }
    }

    inline fun <reified T> getTestData(category: Key<T>, name: String): T {
        val reader = InputStreamReader(javaClass.getResourceAsStream("/testdata/${category.name}/$name.json")!!)
        return fromJson(category, reader)
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.copyNBTDataCompressed", "dev.debug.copyNBTDataCompressed")
        event.move(4, "dev.debug.copyNBTData", "dev.debug.copyItemData")
        event.move(4, "dev.debug.copyNBTDataCompressed", "dev.debug.copyItemDataCompressed")
    }
}
