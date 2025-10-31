package at.hannibal2.hanni

import at.hannibal2.hanni.HanniMod.modules
import at.hannibal2.hanni.test.hotswap.HotswapSupport
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent

@Mod(
    modid = HanniMod.MODID,
    clientSideOnly = true,
    useMetadata = true,
    guiFactory = "at.hannibal2.hanni.config.ConfigGuiForgeInterop",
    version = HanniMod.VERSION,
    modLanguageAdapter = "at.hannibal2.hanni.utils.system.KotlinLanguageAdapter",
)
class HanniModLoader {

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent?) {
        HotswapSupport.load()
        HanniMod.preInit()
        loadedClasses.clear()
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent?) {
        HanniMod.init()
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
