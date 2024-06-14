package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object DraconicSacrificeTracker {

    private val config get() = SkyHanniMod.feature.misc.draconicSacrificeTracker
    private val patternGroup = RepoPattern.group("misc.draconicsacrifice")
    private val sacrificeLoot by patternGroup.pattern(
        "sacrifice",
        "§c§lSACRIFICE! §r§eYou turned §r(?<item>.*) §r§einto §r§d(?<amount>\\d+) Dragon Essence§r§e!",
    )
    private val bonusLoot by patternGroup.pattern(
        "bonus",
        "§c§lBONUS LOOT! §r§eYou also received §r(?:§\\w(?<amount>\\d+)?x)?(?: §r)?(?<item>.*) §r§efrom your sacrifice!",
    )

    private val tracker = SkyHanniTracker("Draconic Sacrifice Tracker", { Data() }, { it.draconicSacrificeTracker }) {
        formatDisplay(drawDisplay(it))
    }
    private val altarArea = AxisAlignedBB(-601.0, 4.0, -282.0, -586.0, 15.0, -269.0)

    class Data : TrackerData() {

        override fun reset() {
            bonusRewards.clear()
            sacrifiedItemsMap.clear()
            itemsSacrifice = 0
            essences = 0
        }

        @Expose
        var itemsSacrifice = 0

        @Expose
        var essences = 0

        @Expose
        var bonusRewards: MutableMap<BonusReward, Long> = mutableMapOf()

        @Expose
        var sacrifiedItemsMap: MutableMap<String, Long> = mutableMapOf()
    }

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat.get()) {
            // TODO, change functionality to use enum rather than ordinals
            newList.add(map[index.ordinal])
        }
        return newList
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList("§5§lDraconic Sacrifice Tracker")
        addAsSingletonList(
            Renderable.hoverTips(
                "§b${data.itemsSacrifice.addSeparators()} §6Items Sacrified",
                data.sacrifiedItemsMap.map { (key, value) -> "$key: §b$value" },
            ),
        )
        addAsSingletonList("§b${data.essences.addSeparators()} §5Dragon Essences")
        addAsSingletonList(" ")

        for (reward in BonusReward.entries.subList(0, 3)) {
            val count = data.bonusRewards[reward] ?: 0
            addAsSingletonList("§b${count} ${reward.displayName}")
        }
        addAsSingletonList(" ")
        for (reward in BonusReward.entries.subList(3, 11)) {
            val count = data.bonusRewards[reward] ?: 0
            addAsSingletonList("§b${count} ${reward.displayName}")
        }
        addAsSingletonList(" ")
        for (reward in BonusReward.entries.subList(11, 16)) {
            val count = data.bonusRewards[reward] ?: 0
            addAsSingletonList("§b${count} ${reward.displayName}")
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val msg = event.message
        sacrificeLoot.matchMatcher(msg) {
            val amount = group("amount").toInt()
            val item = group("item")
            tracker.modify {
                it.itemsSacrifice += 1
                it.essences += amount
                it.sacrifiedItemsMap.addOrPut(item, 1)
            }
        }

        bonusLoot.matchMatcher(msg) {
            val amount = groupOrNull("amount")?.toLong() ?: 1
            val item = group("item")

            BonusReward.entries.find { it.displayName == item }?.let { reward ->
                tracker.modify {
                    it.bonusRewards.addOrPut(reward, amount)
                }
            }
        }

        tracker.update()
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (config.onlyInVoidSlate && !altarArea.isPlayerInside()) return

        tracker.renderDisplay(config.position)
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.textFormat) {
            tracker.update()
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

    enum class BonusReward(val displayName: String) {
        RITUAL_RESIDUE("§5Ritual Residue"),
        SUMMONING_EYE("§5Summoning Eye"),
        DRAGON_HORN("§5Dragon Horn"),
        YOUNG_DRAGON_FRAGMENT("§5Young Dragon Fragment"),
        OLD_DRAGON_FRAGMENT("§5Old Dragon Fragment"),
        STRONG_DRAGON_FRAGMENT("§5Strong Dragon Fragment"),
        WISE_DRAGON_FRAGMENT("§5Wise Dragon Fragment"),
        UNSTABLE_DRAGON_FRAGMENT("§5Unstable Dragon Fragment"),
        PROTECTOR_DRAGON_FRAGMENT("§5Protector Dragon Fragment"),
        SUPERIOR_DRAGON_FRAGMENT("§5Superior Dragon Fragment"),
        HOLY_DRAGON_FRAGMENT("§5Holy Dragon Fragment"),
        DRAGON_CLAW("§5Dragon Claw"),
        ENCHANTED_ENDER_PEARL("§aEnchanted Ender Pearl"),
        ENCHANTED_EYE_OF_ENDER("§aEnchanted Eye Of Ender"),
        ENCHANTED_END_STONE("§aEnchanted End Stone"),
        ENCHANTED_OBSIDIAN("§aEnchanted Obsidian"),
    }
}
