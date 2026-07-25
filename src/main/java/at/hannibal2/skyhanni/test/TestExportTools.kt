package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.CopyItemCommand.copyItemToClipboard
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.KotlinTypeAdapterFactory
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.stackUnderCursor
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.resources.Identifier
import net.minecraft.world.item.component.CustomData
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.Reader
import java.util.Base64
import kotlin.jvm.optionals.getOrNull

@SkyHanniModule
object TestExportTools {

    private val config get() = DevApi.config.debug

    @PublishedApi
    internal val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(KotlinTypeAdapterFactory())
        .registerTypeAdapter(SafeItemStack::class.java, LegacyItemStackTypeAdapter())
        .create()

    private class LegacyItemStackTypeAdapter : TypeAdapter<SafeItemStack>() {
        override fun write(out: JsonWriter, value: SafeItemStack?) {
            out.nullValue()
        }

        override fun read(reader: JsonReader): SafeItemStack {
            val base64 = reader.nextString()
            val bytes = Base64.getDecoder().decode(base64)
            val tag = NbtIo.readCompressed(ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap())
            return buildFromLegacyNbt(tag)
        }

        private fun buildFromLegacyNbt(tag: CompoundTag): SafeItemStack {
            val rawId = tag.getString("id").getOrNull().orEmpty()
                .replace("minecraft:skull", "minecraft:player_head")
            val count = tag.getByte("Count").getOrNull()?.toInt()?.coerceAtLeast(1) ?: 1
            val oldTag = tag.getCompound("tag").getOrNull() ?: CompoundTag()
            val extraAttribs = oldTag.getCompound("ExtraAttributes").getOrNull() ?: CompoundTag()

            val item = BuiltInRegistries.ITEM.getValue(Identifier.parse(rawId))
            val stack = SafeItemStack(item, count)

            if (!extraAttribs.isEmpty) {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(extraAttribs))
            }
            if (oldTag.contains("ench")) {
                stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
            }

            return stack
        }
    }

    class Key<T> internal constructor(val name: String)

    val Item = Key<SafeItemStack>("Item")

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
    fun onGuiKeyPress(event: GuiKeyPressEvent) {
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
