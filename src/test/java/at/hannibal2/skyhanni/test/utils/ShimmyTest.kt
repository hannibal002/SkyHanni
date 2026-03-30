package at.hannibal2.skyhanni.test.utils

import at.hannibal2.skyhanni.utils.json.Shimmy
import com.google.gson.JsonPrimitive
import io.github.notenoughupdates.moulconfig.observer.Property
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

abstract class ShimmyTestBase {

    protected class Simple { var value: String = "hello"; var number: Int = 42 }
    protected class Nested { val inner = Simple() }
    protected class DeepNested { val middle = Nested() }
    protected class WithProperty { val prop: Property<String> = Property.of("propertyValue") }
    protected class WithNullable { val inner: Simple? = null }

    protected abstract fun shimmy(source: Any?, path: List<String>): ShimmyCompat?

    protected interface ShimmyCompat {
        var value: Any?
        val clazz: Class<*>
        fun getJson(): com.google.gson.JsonElement
        fun setJson(element: com.google.gson.JsonElement)
    }

    @Test fun `returns null for empty path`() = assertNull(shimmy(Simple(), emptyList()))
    @Test fun `returns null for null source`() = assertNull(shimmy(null, listOf("value")))
    @Test fun `returns null for non-existent field`() = assertNull(shimmy(Simple(), listOf("doesNotExist")))

    @Test fun `resolves single field`() {
        val s = shimmy(Simple(), listOf("value"))
        assertNotNull(s); assertEquals("hello", s!!.value)
    }

    @Test fun `resolves nested path`() {
        val s = shimmy(Nested(), listOf("inner", "value"))
        assertNotNull(s); assertEquals("hello", s!!.value)
    }

    @Test fun `resolves deeply nested path`() {
        val s = shimmy(DeepNested(), listOf("middle", "inner", "number"))
        assertNotNull(s); assertEquals(42, s!!.value)
    }

    @Test fun `sets value on single field`() {
        val source = Simple()
        shimmy(source, listOf("value"))!!.also { it.value = "world" }
        assertEquals("world", source.value)
    }

    @Test fun `sets value on nested field`() {
        val source = Nested()
        shimmy(source, listOf("inner", "number"))!!.also { it.value = 99 }
        assertEquals(99, source.inner.number)
    }

    @Test fun `getJson returns correct json element`() =
        assertEquals(JsonPrimitive("hello"), shimmy(Simple(), listOf("value"))!!.getJson())

    @Test fun `setJson sets value from json`() {
        val source = Simple()
        shimmy(source, listOf("value"))!!.setJson(JsonPrimitive("fromJson"))
        assertEquals("fromJson", source.value)
    }

    @Test fun `unwraps moulconfig Property transparently`() {
        val s = shimmy(WithProperty(), listOf("prop"))
        assertNotNull(s); assertEquals("propertyValue", s!!.value)
    }

    @Test fun `sets value through moulconfig Property`() {
        val source = WithProperty()
        shimmy(source, listOf("prop"))!!.also { it.value = "newValue" }
        assertEquals("newValue", source.prop.get())
    }

    @Test fun `clazz reflects actual field type`() =
        assertEquals(Int::class.java, shimmy(Simple(), listOf("number"))!!.clazz)

    @Test fun `returns null when intermediate path segment is null`() =
        assertNull(shimmy(WithNullable(), listOf("inner", "value")))
}

// Tests against our kotlin version
class ShimmyNewTest : ShimmyTestBase() {
    override fun shimmy(source: Any?, path: List<String>) = Shimmy(source, path)?.let {
        object : ShimmyCompat {
            override var value get() = it.value; set(v) { it.value = v }
            override val clazz get() = it.clazz
            override fun getJson() = it.getJson()
            override fun setJson(element: com.google.gson.JsonElement) = it.setJson(element)
        }
    }
}
