package at.hannibal2.hanni

import at.hannibal2.hanni.HanniMod.modules
import net.fabricmc.api.ModInitializer

class HanniModLoader : ModInitializer {

    override fun onInitialize() {
        HanniMod.preInit()
        HanniMod.init()
        loadedClasses.clear()
    }

    companion object {
        private val loadedClasses = mutableSetOf<String>()

        fun loadModule(obj: Any) {
            if (!loadedClasses.add(obj.javaClass.name)) throw IllegalStateException("Module ${obj.javaClass.name} is already loaded")
            modules.add(obj)
        }
    }
}
