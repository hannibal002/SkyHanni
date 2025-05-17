package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.SkyHanniMod.modules
import at.hannibal2.skyhanni.test.hotswap.HotswapSupport
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent

@Mod(
    modid = SkyHanniMod.MODID,
    clientSideOnly = true,
    useMetadata = true,
    guiFactory = "at.hannibal2.skyhanni.config.ConfigGuiForgeInterop",
    version = SkyHanniMod.VERSION,
    modLanguageAdapter = "at.hannibal2.skyhanni.utils.system.KotlinLanguageAdapter",
)
class SkyHanniModLoader {

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent?) {
        HotswapSupport.load()
        SkyHanniMod.preInit()
        loadedClasses.clear()
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent?) {
        SkyHanniMod.init()
    }

    companion object {
        private val loadedClasses = mutableSetOf<String>()

        fun loadModule(obj: Any) {
            if (!loadedClasses.add(obj.javaClass.name)) throw IllegalStateException("Module ${obj.javaClass.name} is already loaded")
            modules.add(obj)
            MinecraftForge.EVENT_BUS.register(obj)
        }
    }
}
