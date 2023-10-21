package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.test.command.CopyItemCommand.copyItemToClipboard
import at.hannibal2.skyhanni.utils.ItemStackTypeAdapterFactory
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.KotlinTypeAdapterFactory
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NBTTypeAdapter
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.io.InputStreamReader
import java.io.Reader

object TestExportTools {
    private val config get() = SkyHanniMod.feature.dev.debug

    val gson = GsonBuilder()
        .registerTypeAdapterFactory(KotlinTypeAdapterFactory())
        .registerTypeAdapter(NBTTagCompound::class.java, NBTTypeAdapter)
        .registerTypeAdapterFactory(ItemStackTypeAdapterFactory)
        .create()

    class Key<T> internal constructor(val name: String)

    val Item = Key<ItemStack>("Item")

    @KSerializable
    data class TestValue(
        val type: String,
        val data: JsonElement,
    )

    fun <T> toJson(key: Key<T>, value: T): String {
        return gson.toJson(TestValue(key.name, gson.toJsonTree(value)))
    }

    inline fun <reified T> fromJson(key: Key<T>, reader: Reader): T {
        val serializable = gson.fromJson<TestValue>(reader)
        require(key.name == serializable.type)
        return gson.fromJson(serializable.data)
    }

    @SubscribeEvent
    fun onKeybind(event: GuiScreenEvent.KeyboardInputEvent.Post) {
        if (!config.copyItemDataCompressed.isKeyHeld() && !config.copyItemData.isKeyHeld()) return
        val gui = event.gui as? GuiContainer ?: return
        val stack = gui.slotUnderMouse?.stack ?: return
        if (config.copyItemData.isKeyHeld()) {
            copyItemToClipboard(stack)
            return
        }
        val json = toJson(Item, stack)
        OSUtils.copyToClipboard(json)
        LorenzUtils.chat("§e[SkyHanni] Compressed item info copied into the clipboard!")
    }


    inline fun <reified T> getTestData(category: Key<T>, name: String): T {
        val reader = InputStreamReader(javaClass.getResourceAsStream("/testdata/${category.name}/$name.json")!!)
        return fromJson(category, reader)
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.copyNBTDataCompressed", "dev.debug.copyNBTDataCompressed")
        event.move(4, "dev.debug.copyNBTData", "dev.debug.copyItemData")
        event.move(4, "dev.debug.copyNBTDataCompressed", "dev.debug.copyItemDataCompressed")
    }
}