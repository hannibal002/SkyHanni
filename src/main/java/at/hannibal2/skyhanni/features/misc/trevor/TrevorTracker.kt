package at.hannibal2.skyhanni.features.misc.trevor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Matcher

object TrevorTracker {
    private val config get() = SkyHanniMod.feature.misc.trevorTheTrapper
    private var display = emptyList<List<Any>>()

    private val selfKillMobPattern = "§aYour mob died randomly, you are rewarded §r§5(?<pelts>.*) pelts§r§a.".toPattern()
    private val killMobPattern = "§aKilling the animal rewarded you §r§5(?<pelts>.*) pelts§r§a.".toPattern()

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        saveAndUpdate()
    }

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat) {
            newList.add(map[index])
        }
        return newList
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!TrevorFeatures.onFarmingIsland()) return
        val storage = ProfileStorageData.profileSpecific?.trapperData ?: return

        var matcher = selfKillMobPattern.matcher(event.message.removeColor())
        if (matcher.matches()) {
            val pelts = matcher.group("pelts").toInt()
            storage.peltsGained += pelts
            storage.selfKillingAnimals += 1
            saveAndUpdate()
        }
        matcher = killMobPattern.matcher(event.message.removeColor())
        if (matcher.matches()) {
            val pelts = matcher.group("pelts").toInt()
            storage.peltsGained += pelts
            saveAndUpdate()
        }
    }

    fun startQuest(matcher: Matcher) {
        val storage = ProfileStorageData.profileSpecific?.trapperData ?: return
        storage.questsDone += 1
        val rarity = matcher.group("rarity")
        val foundRarity = TrapperMobRarity.values().firstOrNull { it.formattedName == rarity } ?: return
        val old = storage.animalRarities[foundRarity] ?: 0
        storage.animalRarities = storage.animalRarities.editCopy { this[foundRarity] = old + 1 }
        saveAndUpdate()
    }

    private fun saveAndUpdate() {
        val storage = ProfileStorageData.profileSpecific?.trapperData ?: return
        display = formatDisplay(drawTrapperDisplay(storage))
    }

    private fun drawTrapperDisplay(storage: Storage.ProfileSpecific.TrapperData) = buildList<List<Any>> {

    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!shouldDisplay()) return
        config.position.renderStringsAndItems(display, posLabel = "Frozen Treasure Tracker")
    }

    private fun shouldDisplay(): Boolean {
        if (!config.dataTracker) return false
        if (!TrevorFeatures.onFarmingIsland()) return false
        if (TrevorFeatures.inTrapperDen()) return true
        return when (config.displayType) {
            0 -> true
            1 -> TrevorFeatures.inBetweenQuests
            2 -> TrevorFeatures.questActive
            else -> false
        }
    }

    enum class TrapperMobRarity(val formattedName: String) {
        TRACKABLE("TRACKABLE"),
        UNTRACKABLE("UNTRACKABLE"),
        UNDETECTED("UNDETECTED"),
        ENDANGERED("ELUSIVE"),
        ELUSIVE("ELUSIVE")
    }
}