package at.hannibal2.skyhanni.compat

import me.shedaniel.rei.api.client.REIRuntime

object ReiCompat {
    @JvmStatic
    fun searchHasFocus(): Boolean {
        return try {
            REIRuntime.getInstance().searchTextField?.isFocused == true
        } catch (e: NoClassDefFoundError) {
            false
        }
    }
}
