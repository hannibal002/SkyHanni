package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.config.features.Garden
import at.hannibal2.skyhanni.config.migration.MigratingConfigLoader
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.lang.reflect.Field
import java.lang.reflect.Type
import kotlin.reflect.KProperty1


data class ConfigMigrationEvent(
    val property: Field?,
    val objectHierarchy: List<JsonElement?>,
    val type: Type,
    var value: MigratingConfigLoader.LoadResult<*>
) : LorenzEvent() {


    @SubscribeEvent
    fun migrateSomething(event: ConfigMigrationEvent) {
        migrate(Garden::composter) {
            parent().parent().child("oldPropertyIdk")
        }
    }


    fun use(value: Any?) {
        this.value = MigratingConfigLoader.LoadResult.Instance(value)
    }

    fun isFailing(): Boolean {
        return value is MigratingConfigLoader.LoadResult.Failure || value is MigratingConfigLoader.LoadResult.Invalid
    }

    fun useDefault() {
        this.value = MigratingConfigLoader.LoadResult.UseDefault
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
            MigratingConfigLoader.loadElement(null, MigrateContext(objectHierarchy).let(block).hierarchy, oldType)
                .map(mapper)
    }
}