package at.hannibal2.skyhanni.test.utils

import at.hannibal2.skyhanni.utils.ExtraData
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.KotlinTypeAdapterFactory
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class KSerializableTest {

    private val gson = GsonBuilder()
        .registerTypeAdapterFactory(KotlinTypeAdapterFactory())
        .create()

    @KSerializable
    private data class Simple(val name: String, val count: Int)

    @KSerializable
    private data class WithRename(
        @SerializedName("full_name") val name: String,
        val age: Int,
    )

    @KSerializable
    private data class WithDefault(val name: String, val count: Int = 42)

    @KSerializable
    private data class WithNullable(val value: String?)

    @KSerializable
    private data class Inner(val x: Int)

    @KSerializable
    private data class Outer(val inner: Inner, val label: String)

    private abstract class LowestBase(lowest: Long, lowestVolume: Int) {
        val combined: String = "$lowest/$lowestVolume"
    }

    @KSerializable
    private data class WithInheritance(
        val name: String,
        val lowest: Long,
        val lowestVolume: Int,
    ) : LowestBase(lowest, lowestVolume)

    @KSerializable
    private data class WithExtra(
        val known: String,
        @ExtraData val extras: MutableMap<String, JsonElement> = mutableMapOf(),
    )

    private data class NotAnnotated(val x: Int)

    @KSerializable
    private class NotDataClass(val x: Int)

    // Read

    @Test
    fun `basic deserialization reads all fields`() {
        val result = gson.fromJson("""{"name":"hello","count":5}""", Simple::class.java)
        assertEquals(Simple("hello", 5), result)
    }

    @Test
    fun `SerializedName deserialization reads renamed key`() {
        val result = gson.fromJson("""{"full_name":"Alice","age":30}""", WithRename::class.java)
        assertEquals(WithRename("Alice", 30), result)
    }

    @Test
    fun `missing optional field uses Kotlin default value`() {
        val result = gson.fromJson("""{"name":"test"}""", WithDefault::class.java)
        assertEquals(WithDefault("test", 42), result)
    }

    @Test
    fun `null value deserializes to null`() {
        val result = gson.fromJson("""{"value":null}""", WithNullable::class.java)
        assertNull(result?.value)
    }

    @Test
    fun `nested KSerializable type deserializes correctly`() {
        val result = gson.fromJson("""{"inner":{"x":7},"label":"hi"}""", Outer::class.java)
        assertEquals(Outer(Inner(7), "hi"), result)
    }

    @Test
    fun `data class extending abstract base deserializes fields from superclass`() {
        val result = gson.fromJson(
            """{"name":"item","lowest":100,"lowestVolume":5}""",
            WithInheritance::class.java,
        )
        assertEquals("item", result.name)
        assertEquals("100/5", result.combined)
    }

    @Test
    fun `ExtraData deserialization captures unknown fields`() {
        val result = gson.fromJson("""{"known":"yes","extra1":"a","extra2":42}""", WithExtra::class.java)
        assertEquals("yes", result.known)
        assertEquals(2, result.extras.size)
        assertEquals("a", result.extras["extra1"]?.asString)
        assertEquals(42, result.extras["extra2"]?.asInt)
    }

    @Test
    fun `non-annotated class returns null from factory`() {
        val adapter = KotlinTypeAdapterFactory().create(gson, TypeToken.get(NotAnnotated::class.java))
        assertNull(adapter)
    }

    @Test
    fun `non-data class annotated with KSerializable returns null from factory`() {
        val adapter = KotlinTypeAdapterFactory().create(gson, TypeToken.get(NotDataClass::class.java))
        assertNull(adapter)
    }

    // Write

    @Test
    fun `basic serialization emits all fields`() {
        val result = gson.toJson(Simple("hello", 5))
        assertEquals("""{"name":"hello","count":5}""", result)
    }

    @Test
    fun `SerializedName serialization uses annotation key`() {
        val result = gson.toJson(WithRename("Alice", 30))
        assertEquals("""{"full_name":"Alice","age":30}""", result)
    }

    @Test
    fun `ExtraData serialization emits extra fields at top level`() {
        val obj = WithExtra("yes", mutableMapOf("extra1" to JsonPrimitive("abc")))
        val parsed = JsonParser.parseString(gson.toJson(obj)).asJsonObject
        assertEquals("yes", parsed["known"]?.asString)
        assertEquals("abc", parsed["extra1"]?.asString)
    }

    @Test
    fun `null value serializes as JSON null`() {
        val result = gson.toJson(WithNullable(null))
        assertEquals("""{"value":null}""", result)
    }

    @Test
    fun `nested KSerializable type serializes correctly`() {
        val result = gson.toJson(Outer(Inner(7), "hi"))
        assertEquals("""{"inner":{"x":7},"label":"hi"}""", result)
    }

    @Test
    fun `inheritance serialization emits own and inherited constructor fields`() {
        val obj = WithInheritance("item", 100L, 5)
        val parsed = JsonParser.parseString(gson.toJson(obj)).asJsonObject
        assertEquals("item", parsed["name"]?.asString)
        assertEquals(100L, parsed["lowest"]?.asLong)
        assertEquals(5, parsed["lowestVolume"]?.asInt)
    }
}
