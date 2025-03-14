package at.hannibal2.skyhanni

// import at.hannibal2.skyhanni.SkyHanniMod.modules
import net.fabricmc.api.ModInitializer

class SkyHanniModLoader : ModInitializer {

    override fun onInitialize() {
//         SkyHanniMod.preInit()
//         SkyHanniMod.init()
        println("skyhanni loaded")
        loadedClasses.clear()
    }

    companion object {
        private val loadedClasses = mutableSetOf<String>()

        fun loadModule(obj: Any) {
            if (!loadedClasses.add(obj.javaClass.name)) throw IllegalStateException("Module ${obj.javaClass.name} is already loaded")
//             modules.add(obj)
        }
    }
}
