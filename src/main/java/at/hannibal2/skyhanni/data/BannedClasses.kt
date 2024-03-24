package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.BannedClassesJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object BannedClasses {

    private var list = listOf<String>()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        list = event.getConstant<BannedClassesJson>("BannedClasses").bannedClasses[SkyHanniMod.version] ?: emptyList()
    }

    fun isBanned(javaClass: Class<*>): Boolean = list.contains(javaClass.name)
}
