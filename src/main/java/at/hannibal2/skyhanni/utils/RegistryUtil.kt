package at.hannibal2.skyhanni.utils

object RegistryUtil {

    private const val PREFIX: String = "REG"
    private const val SUCCESS_CODE: Int = 0
    private const val FAILURE_CODE: Int = 1

    fun addKey(runtime: Runtime, location: RegistryPaths, key: String) : Boolean {
        val proc: Process = runtime.exec("$PREFIX ADD ${location.path}\\$key")
        proc.waitFor()

        return proc.exitValue() == SUCCESS_CODE
    }

    fun keyExists(runtime: Runtime, location: RegistryPaths, key: String = "") : Boolean {
        val proc: Process = if (key.isEmpty()) {
            runtime.exec("$PREFIX QUERY ${location.path}")
        } else {
            runtime.exec("$PREFIX QUERY ${location.path}\\$key")
        }
        proc.waitFor()
        return proc.exitValue() == SUCCESS_CODE
    }

    fun addValue(runtime: Runtime, location: RegistryPaths, key: String, valueName: String, valueType: KeyTypes, value: Any) : Boolean {
        val proc: Process = runtime.exec("$PREFIX ADD ${location.path}\\$key /v $valueName /d $value /t $valueType")
        proc.waitFor()
        return proc.exitValue() == SUCCESS_CODE
    }

    fun valueExists(runtime: Runtime, location: RegistryPaths, key: String, valueName: String) : Boolean {
        val proc: Process = runtime.exec("$PREFIX QUERY ${location.path}\\$key /v $valueName")
        proc.waitFor()
        return proc.exitValue() == SUCCESS_CODE
    }

    enum class RegistryPaths(val path: String) {
        NOTIFICATIONS("HKCR\\AppUserModelId\\hannibal02.SkyHanni")
    }

     enum class KeyTypes(private val text: String) {
        KEY(""),
        STRING("REG_SZ"),
        EXPANDABLE_STRING("REG_EXPAND_SZ"),
        INTEGER("REG_DWORD"),
        BINARY("REG_BINARY"),
        ;

        override fun toString() : String {
            return text
        }

    }

}
