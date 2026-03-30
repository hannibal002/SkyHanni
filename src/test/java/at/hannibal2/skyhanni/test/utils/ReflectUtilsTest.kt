package at.hannibal2.skyhanni.test.utils

import at.hannibal2.skyhanni.utils.ReflectionUtils
import at.hannibal2.skyhanni.utils.ReflectionUtils.findGenericSuperclassTypeArgument
import at.hannibal2.skyhanni.utils.ReflectionUtils.getPrivateField
import at.hannibal2.skyhanni.utils.ReflectionUtils.getPrivateFieldValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable

class ReflectUtilsTest {
    class SomeClass
    open class Parent<A, B>
    open class Child<A, B> : Parent<B, A>()
    open class GrandChild<T> : Child<String, T>()

    abstract class Holder<X>

    inline fun <reified T> resolve(typeParam: TypeVariable<*>): Class<*>? {
        return ReflectionUtils.resolveUpperBoundSuperClassGenericParameter(
            (object : Holder<T>() {}
                .javaClass.genericSuperclass as ParameterizedType)
                .actualTypeArguments[0],
            typeParam,
        )
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Test
    fun testResolveUpperBoundSuperClassGenericParameter() {
        val firstParent = Parent::class.java.typeParameters[0]
        val secondParent = Parent::class.java.typeParameters[1]
        assertEquals(String::class.java, resolve<Parent<String, Int>>(firstParent))
        assertEquals(Integer::class.java, resolve<Parent<String, Int>>(secondParent))
        assertEquals(Integer::class.java, resolve<Child<String, Int>>(firstParent))
        assertEquals(String::class.java, resolve<Child<String, Int>>(secondParent))
        assertEquals(SomeClass::class.java, resolve<GrandChild<SomeClass>>(firstParent))
        assertEquals(String::class.java, resolve<GrandChild<SomeClass>>(secondParent))
        assertEquals(SomeClass::class.java, resolve<GrandChild<out SomeClass>>(firstParent))
        assertEquals(Any::class.java, resolve<GrandChild<*>>(firstParent))
    }

    private class WithFields {
        private val secret = "hidden"
        private val number = 7
        private val nullableField: String? = null
    }

    @Test
    fun `getPrivateField by name returns accessible field`() {
        val field = WithFields().getPrivateField("secret")
        assertNotNull(field)
        assertEquals("secret", field.name)
    }

    @Test
    fun `getPrivateField by index returns accessible field`() {
        assertNotNull(WithFields().getPrivateField(0))
    }

    @Test
    fun `getPrivateField by name throws for missing field`() {
        assertThrows(NoSuchFieldException::class.java) {
            WithFields().getPrivateField("doesNotExist")
        }
    }

    @Test
    fun `getPrivateFieldValue by name returns correct value`() {
        assertEquals("hidden", WithFields().getPrivateFieldValue("secret"))
    }

    @Test
    fun `getPrivateFieldValue by index returns correct value`() {
        assertEquals("hidden", WithFields().getPrivateFieldValue(0))
    }

    @Test
    fun `getPrivateFieldValue by name returns correct int value`() {
        assertEquals(7, WithFields().getPrivateFieldValue("number"))
    }

    @Test
    fun `getPrivateFieldValue throws for null field`() {
        assertThrows(IllegalArgumentException::class.java) {
            WithFields().getPrivateFieldValue("nullableField")
        }
    }

    @Test
    fun `Class getPrivateFieldValue error message wrong name`() {
        class Holder {
            @Suppress("unused")
            private val field: String? = null
        }

        val exception = assertThrows(IllegalArgumentException::class.java) {
            Holder::class.java.getPrivateFieldValue("field", Holder())
        }
        assert(exception.message?.contains("Holder") == true) {
            "Expected class name in message, was: ${exception.message}"
        }
    }

    @Test
    fun `getPrivateFieldValue throws NoSuchFieldException for missing field`() {
        assertThrows(NoSuchFieldException::class.java) {
            WithFields().getPrivateFieldValue("doesNotExist")
        }
    }

    class ConsumerTarget {
        var received: String? = null
        fun accept(value: String) {
            received = value
        }
    }

    class RunnableTarget {
        var ran = false
        fun run() {
            ran = true
        }
    }

    @Test
    fun `createConsumerFromMethod invokes method with argument`() {
        val target = ConsumerTarget()
        val method = ConsumerTarget::class.java.getMethod("accept", String::class.java)
        ReflectionUtils.createConsumerFromMethod(target, method).accept("hello")
        assertEquals("hello", target.received)
    }

    @Test
    fun `createConsumerFromMethod throws for non-consumer method`() {
        val target = RunnableTarget()
        val method = RunnableTarget::class.java.getMethod("run")
        assertThrows(IllegalArgumentException::class.java) {
            ReflectionUtils.createConsumerFromMethod(target, method)
        }
    }

    @Test
    fun `createRunnableFromMethod invokes method`() {
        val target = RunnableTarget()
        val method = RunnableTarget::class.java.getMethod("run")
        ReflectionUtils.createRunnableFromMethod(target, method).run()
        assertEquals(true, target.ran)
    }

    abstract class SingleParam<T>
    abstract class TwoParam<A, B>
    class StringHolder : SingleParam<String>()
    class IntStringHolder : TwoParam<Int, String>()

    @Test
    fun `findGenericSuperclassTypeArgument resolves single type parameter`() {
        assertEquals(String::class.java, StringHolder().findGenericSuperclassTypeArgument<SingleParam<*>, String>())
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Test
    fun `findGenericSuperclassTypeArgument resolves first of two parameters`() {
        assertEquals(Integer::class.java, IntStringHolder().findGenericSuperclassTypeArgument<TwoParam<*, *>, Int>(0))
    }

    @Test
    fun `findGenericSuperclassTypeArgument resolves second of two parameters`() {
        assertEquals(String::class.java, IntStringHolder().findGenericSuperclassTypeArgument<TwoParam<*, *>, String>(1))
    }

    @Test
    fun `findGenericSuperclassTypeArgument throws when Stop class not in hierarchy`() {
        assertThrows(IllegalStateException::class.java) {
            StringHolder().findGenericSuperclassTypeArgument<TwoParam<*, *>, String>()
        }
    }
}
