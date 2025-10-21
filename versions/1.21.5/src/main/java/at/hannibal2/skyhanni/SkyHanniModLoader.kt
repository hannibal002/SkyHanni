package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.SkyHanniMod.modules
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader;

class SkyHanniModLoader : ModInitializer {

    override fun onInitialize() {
        SkyHanniMod.preInit(FabricLoader.getInstance().getConfigDir().toFile())
        SkyHanniMod.init()
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
