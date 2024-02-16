package at.hannibal2.skyhanni.utils

import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.ModContainer
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.isAccessible

object ReflectionUtils {

    // TODO nea?
//    fun <T> dynamic(block: () -> KMutableProperty0<T>?): ReadWriteProperty<Any?, T?> {
//        return object : ReadWriteProperty<Any?, T?> {
//            override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
//                return block()?.get()
//            }
//
//            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
//                if (value != null)
//                    block()?.set(value)
//            }
//        }
//    }

    fun <T, R> dynamic(root: KProperty0<R?>, child: KMutableProperty1<R, T>) =
        object : ReadWriteProperty<Any?, T?> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
                val rootObj = root.get() ?: return null
                return child.get(rootObj)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
                if (value == null) return
                val rootObj = root.get() ?: return
                child.set(rootObj, value)
            }
        }

    inline fun <reified T : Any> Any.getPropertiesWithType() =
        this::class.memberProperties
            .filter { it.returnType.isSubtypeOf(T::class.starProjectedType) }
            .map {
                it.isAccessible = true
                (it as KProperty1<Any, T>).get(this)
            }

    fun Field.makeAccessible() = also { isAccessible = true }

    fun <T> Constructor<T>.makeAccessible() = also { isAccessible = true }

    fun Field.removeFinal(): Field {
        javaClass.getDeclaredField("modifiers").makeAccessible().set(this, modifiers and (Modifier.FINAL.inv()))
        return this
    }

    fun StackTraceElement.getClassInstance(): Class<*> {
        return Class.forName(this.className)
    }

    private val packageLookup by lazy {
        Loader.instance().modList
            .flatMap { mod -> mod.ownedPackages.map { it to mod } }
            .toMap()
    }

    val Class<*>.shPackageName
        get() =
            canonicalName?.substringBeforeLast('.')

    fun Class<*>.getModContainer(): ModContainer? {
        return packageLookup[shPackageName]
    }

}
