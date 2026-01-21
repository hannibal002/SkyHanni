package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.features.misc.ContributorManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CircularList
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import net.minecraft.network.chat.Component

@SkyHanniModule
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
    fun onTooltip(event: ToolTipTextEvent) {
        if (!isEnabled()) return
        if (!HoppityCollectionStats.inInventory) return

        val itemStack = event.itemStack
        val lore = itemStack.getLoreComponent()
        val last = lore.lastOrNull() ?: return
        if (!last.string.endsWith(" RABBIT")) return

        val realName = itemStack.hoverName.formattedTextCompatLeadingWhiteLessResets()
        val cleanName = realName.removeColor()
        val fakeName = replaceMap[cleanName] ?: return

        event.toolTip[0] = event.toolTip[0].formattedTextCompat().replace(cleanName, fakeName).asComponent()

        event.toolTip.add(" ")
        event.toolTip.add("§8§oSome might say this rabbit is also known as $realName")

        // TODO find a way to handle non containing entries in a kotlin nullable way instead of checking for -1
        val index = event.toolTip.indexOfFirst { it.string.contains(" a duplicate") }
        if (index == -1) return
        val oldLine = event.toolTip[index]
        event.toolTip[index] = Component.literal(oldLine.formattedTextCompat().replace(cleanName, fakeName))
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.contributorRabbitName
}
