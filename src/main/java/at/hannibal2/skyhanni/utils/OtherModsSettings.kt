package at.hannibal2.skyhanni.utils

import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

class OtherModsSettings private constructor(private val modConfigPath: String) {

    companion object {
        private val classCache = ConcurrentHashMap<String, Class<*>>()
        private val fieldCache = ConcurrentHashMap<Triple<String, Class<*>, String>, Field>()

        fun patcher() = OtherModsSettings("club.sk1er.patcher.config.PatcherConfig")
        fun aaron() = OtherModsSettings("net.azureaaron.mod.config.AaronModConfigManager")

        internal fun loadClass(path: String): Class<*>? =
            runCatching { classCache.getOrPut(path) { Class.forName(path) } }
                .getOrNull()

        internal fun getField(modPath: String, clazz: Class<*>, name: String): Field =
            fieldCache.getOrPut(Triple(modPath, clazz, name)) {
                runCatching {
                    clazz.getField(name).apply { isAccessible = true }
                }.getOrElse {
                    clazz.getDeclaredField(name).apply { isAccessible = true }
                }
            }

        fun clearCaches() {
            classCache.clear()
            fieldCache.clear()
        }
    }

    fun isLoaded(): Boolean = loadClass(modConfigPath) != null

    private val chainCache = ConcurrentHashMap<Pair<String, String>, List<Field>>()

    fun isEnabled(path: String): Boolean = getBoolean(path) ?: false
    fun getBoolean(path: String): Boolean? = getNestedValue<Boolean>(path)
    fun setBoolean(path: String, value: Boolean) = setNestedValue(path, value)
    fun getConfigValue(path: String): Any? = getNestedValue<Any>(path)
    fun setConfigValue(path: String, value: Any) = setNestedValue(path, value)

    private inline fun <reified T> getNestedValue(path: String): T? {
        loadClass(modConfigPath) ?: return null
        val key = modConfigPath to path
        val chain = chainCache.computeIfAbsent(key) { buildFieldChain(path) }
        if (chain.isEmpty()) return null

        val (rootInstance, firstField) = getOptionPair(chain[0].name) ?: return null
        var current: Any? = runCatching { firstField.get(rootInstance) }.getOrNull()

        for (field in chain.drop(1)) {
            current = current?.let { runCatching { field.get(it) }.getOrNull() } ?: return null
        }
        return current as? T
    }

    private fun setNestedValue(path: String, value: Any) {
        loadClass(modConfigPath) ?: return
        val key = modConfigPath to path
        val chain = chainCache.computeIfAbsent(key) { buildFieldChain(path) }
        if (chain.size < 2) return

        val (rootInstance, firstField) = getOptionPair(chain[0].name) ?: return
        var current: Any? = runCatching { firstField.get(rootInstance) }.getOrNull()

        for (field in chain.drop(1).dropLast(1)) {
            current = current?.let { runCatching { field.get(it) }.getOrNull() } ?: return
        }
        runCatching { chain.last().set(current, value) }
    }

    private fun buildFieldChain(path: String): List<Field> {
        val segments = path.split('.')
        val rootPair = getOptionPair(segments.first()) ?: return emptyList()
        val fields = mutableListOf(rootPair.second)
        var currentClass = rootPair.second.type
        for (segment in segments.drop(1)) {
            val field = getField(modConfigPath, currentClass, segment)
            fields += field
            currentClass = field.type
        }
        return fields
    }

    private fun getOptionPair(rootName: String): Pair<Any?, Field>? {
        val cfgClass = loadClass(modConfigPath) ?: return null
        runCatching {
            val field = getField(modConfigPath, cfgClass, rootName)
            return null to field
        }
        return runCatching {
            val instance = cfgClass.getMethod("get").invoke(null)!!
            val field = getField(modConfigPath, instance.javaClass, rootName)
            instance to field
        }.getOrNull()
    }

    fun clearCaches() {
        Companion.clearCaches()
        chainCache.clear()
    }
}
