package at.hannibal2.hanni.test

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.GuiKeyPressEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.test.command.CopyItemCommand.copyItemToClipboard
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.KSerializable
import at.hannibal2.hanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.hanni.utils.KotlinTypeAdapterFactory
import at.hannibal2.hanni.utils.OSUtils
import at.hannibal2.hanni.utils.compat.stackUnderCursor
//#if TODO
import at.hannibal2.hanni.utils.json.ItemStackTypeAdapterFactory
import at.hannibal2.hanni.utils.json.NBTTypeAdapter
//#endif
import at.hannibal2.hanni.utils.json.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import java.io.InputStreamReader
import java.io.Reader

@HanniModule
object TestExportTools {

    private val config get() = HanniMod.feature.dev.debug

    val gson = GsonBuilder()
        .registerTypeAdapterFactory(KotlinTypeAdapterFactory())
        //#if TODO
        .registerTypeAdapter(NBTTagCompound::class.java, NBTTypeAdapter)
        .registerTypeAdapterFactory(ItemStackTypeAdapterFactory)
        //#endif
        .create()

    class Key<T> internal constructor(val name: String)

    val Item = Key<ItemStack>("Item")

    @KSerializable
    data class TestValue(
        val type: String,
        val data: JsonElement,
    )

    private fun <T> toJson(key: Key<T>, value: T): String {
        return gson.toJson(TestValue(key.name, gson.toJsonTree(value)))
    }

    inline fun <reified T> fromJson(key: Key<T>, reader: Reader): T {
        val serializable = gson.fromJson<TestValue>(reader)
        require(key.name == serializable.type)
        return gson.fromJson(serializable.data)
    }

    @HandleEvent
    fun onKeybind(event: GuiKeyPressEvent) {
        if (!config.copyItemDataCompressed.isKeyHeld() && !config.copyItemData.isKeyHeld()) return
        val stack = stackUnderCursor() ?: return
        if (config.copyItemData.isKeyHeld()) {
            copyItemToClipboard(stack)
            return
        }
        val json = toJson(Item, stack)
        OSUtils.copyToClipboard(json)
        ChatUtils.chat("Compressed item info copied into the clipboard!")
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
