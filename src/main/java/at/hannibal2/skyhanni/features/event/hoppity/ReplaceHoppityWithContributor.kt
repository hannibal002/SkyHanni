package at.hannibal2.hanni.features.event.hoppity

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.NeuRepositoryReloadEvent
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.events.item.ItemHoverEvent
import at.hannibal2.hanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.hanni.features.misc.ContributorManager
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.collection.CircularList

@HanniModule
object ReplaceHoppityWithContributor {

    private val config get() = CFApi.config

    private val replaceMap = mutableMapOf<String, String>()

    @HandleEvent(priority = 5)
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        update()
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onRepoReload(event: RepositoryReloadEvent) {
        update()
    }

    fun update() {
        replaceMap.clear()

        val contributors = ContributorManager.contributorNames
        val rabbits = HoppityCollectionData.rabbitRarities

        if (contributors.isEmpty()) return
        if (rabbits.isEmpty()) return

        val newNames = CircularList(contributors.toList())
        for (internalName in rabbits.map { it.key }.shuffled()) {
            val realName = internalName.allLettersFirstUppercase()
            val newName = newNames.next()
            replaceMap[realName] = newName
        }
    }

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onTooltip(event: ItemHoverEvent) {
        if (!isEnabled()) return
        if (!HoppityCollectionStats.inInventory) return

        val itemStack = event.itemStack
        val lore = itemStack.getLore()
        val last = lore.lastOrNull() ?: return
        if (!last.endsWith(" RABBIT")) return

        val realName = itemStack.displayName
        val cleanName = realName.removeColor()
        val fakeName = replaceMap[cleanName] ?: return

        val newName = event.toolTip[0].replace(cleanName, fakeName)
        event.toolTip[0] = newName

        event.toolTip.add(" ")
        event.toolTip.add("§8§oSome might say this rabbit is also known as $realName")

        // TODO find a way to handle non containing entries in a kotlin nullable way instead of checking for -1
        val index = event.toolTip.indexOfFirst { it.contains(" a duplicate") }
        if (index == -1) return
        val oldLine = event.toolTip[index]
        val newLine = oldLine.replace(cleanName, fakeName)
        event.toolTip[index] = newLine
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.contributorRabbitName
}
