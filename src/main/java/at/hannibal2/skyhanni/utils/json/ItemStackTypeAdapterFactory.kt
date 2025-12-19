package at.hannibal2.skyhanni.utils.json

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

object ItemStackTypeAdapterFactory : TypeAdapterFactory {

    override fun <T : Any> create(gson: Gson?, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType == ItemStack::class.java) {
            val nbtCompoundTypeAdapter = gson!!.getAdapter(CompoundTag::class.java)
            return object : TypeAdapter<ItemStack>() {
                override fun write(out: JsonWriter, value: ItemStack) {
                    nbtCompoundTypeAdapter.write(out, value.serializeNBT())
                }

                override fun read(reader: JsonReader): ItemStack {
                    return ItemStack(nbtCompoundTypeAdapter.read(reader))
                }
            } as TypeAdapter<T>
        }
        return null
    }
}
