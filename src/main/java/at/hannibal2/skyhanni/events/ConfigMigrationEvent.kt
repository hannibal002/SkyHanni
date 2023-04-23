package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.config.migration.LoadResult
import at.hannibal2.skyhanni.config.migration.MigratingConfigLoader
import at.hannibal2.skyhanni.config.migration.ResolutionPath
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Field
import java.lang.reflect.Type
import kotlin.reflect.KProperty1


data class ConfigMigrationEvent(
    val loader: MigratingConfigLoader,
    val resolutionPath: ResolutionPath,
    val objectHierarchy: List<JsonElement?>,
    val type: Type,
    var value: LoadResult<*>
) : LorenzEvent() {
    val property = (resolutionPath as? ResolutionPath.FieldChild)?.field


    fun use(value: Any?) {
        this.value = LoadResult.Instance(value)
    }

    fun isFailing(): Boolean {
        return value is LoadResult.Failure || value is LoadResult.Invalid
    }

    fun useDefault() {
        this.value = LoadResult.UseDefault
    }

    data class MigrateContext(val hierarchy: List<JsonElement?>) {
        fun parent(n: Int = 1): MigrateContext = MigrateContext(hierarchy.dropLast(n))
        fun child(name: String) = MigrateContext(hierarchy + (hierarchy.last() as? JsonObject)?.get(name))
        fun root(): MigrateContext = MigrateContext(hierarchy.take(1))
    }

    inline fun <reified T, V> migrate(prop: KProperty1<T, V>, noinline block: MigrateContext.() -> MigrateContext) {
        if (prop.name != property?.name) return
        val field = try {
            T::class.java.getDeclaredField(prop.name)
        } catch (e: NoSuchFieldException) {
            return
        }
        migrate(field, block)
    }

    inline fun <reified T, V> migrate(
        prop: KProperty1<T, V>,
        noinline block: MigrateContext.() -> MigrateContext,
        oldType: Type,
        noinline mapper: (Any?) -> T?
    ) {
        if (prop.name != property?.name) return
        val field = try {
            T::class.java.getDeclaredField(prop.name)
        } catch (e: NoSuchFieldException) {
            return
        }
        migrate(field, block, oldType, mapper)
    }

    fun migrate(prop: Field, block: MigrateContext.() -> MigrateContext) = migrate(prop, block, type) { it }
    fun migrate(prop: Field, block: MigrateContext.() -> MigrateContext, oldType: Type, mapper: (Any?) -> Any?) {
        if (prop != property) return
        this.value =
            loader.loadElement(resolutionPath, MigrateContext(objectHierarchy).let(block).hierarchy, oldType)
                .map(mapper)
    }
}