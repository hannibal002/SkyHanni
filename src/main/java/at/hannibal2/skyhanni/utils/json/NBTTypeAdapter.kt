package at.hannibal2.skyhanni.utils.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

object NBTTypeAdapter : TypeAdapter<CompoundTag>() {

    override fun write(out: JsonWriter, value: CompoundTag) {
        val baos = ByteArrayOutputStream()
        NbtIo.writeCompressed(value, baos)
        out.value(Base64.getEncoder().encode(baos.toByteArray()).decodeToString())
    }

    override fun read(reader: JsonReader): CompoundTag {
        val bais = ByteArrayInputStream(Base64.getDecoder().decode(reader.nextString()))
        return NbtIo.readCompressed(bais)
    }
}
