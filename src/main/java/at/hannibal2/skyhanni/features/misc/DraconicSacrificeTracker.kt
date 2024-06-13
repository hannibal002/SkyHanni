package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
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
        "§c§lSACRIFICE! §r§eYou turned §r§\\w(?<item>.*) §r§einto §r§d(?<amount>\\d+) Dragon Essence§r§e!",
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
            itemsSacrifice = 0
            essences = 0
        }

        @Expose
        var itemsSacrifice = 0

        @Expose
        var essences = 0

        @Expose
        var bonusRewards: MutableMap<BonusReward, Long> = mutableMapOf();
    }

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat) {
            // TODO, change functionality to use enum rather than ordinals
            newList.add(map[index.ordinal])
        }
        return newList
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList(Renderable.string("§5§lDraconic Sacrifice Tracker"))
        addAsSingletonList(Renderable.string("§6${data.itemsSacrifice.addSeparators()} Items Sacrified"))
        addAsSingletonList(Renderable.string("§b${data.essences.addSeparators()} §5Dragon Essences"))

        for (reward in BonusReward.entries) {
            val count = data.bonusRewards[reward] ?: 0
            add(
                buildList {
                    add(Renderable.itemStack(reward.internalName.getItemStack()))
                    add("§b${count} ${reward.displayName}")
                },
            )
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val msg = event.message
        sacrificeLoot.matchMatcher(msg) {
            val amount = group("amount").toInt()
            tracker.modify {
                it.itemsSacrifice += 1
                it.essences += amount
            }
        }

        bonusLoot.matchMatcher(msg) {
            val item = group("item")
            val amount = group("amount").toLongOrNull() ?: 1L
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

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

    enum class BonusReward(
        val internalName: NEUInternalName,
        val displayName: String,
    ) {
        RITUAL_RESIDUE("RITUAL_RESIDUE".asInternalName(), "§5Ritual Residue"),
        SUMMONING_EYE("SUMMONING_EYE".asInternalName(), "§5Summoning Eye"),
        DRAGON_HORN("DRAGON_HORN".asInternalName(), "§5Dragon Horn"),
        DRAGON_CLAW("DRAGON_CLAW".asInternalName(), "§5Dragon Claw"),
        ENCHANTED_ENDER_PEARL("ENCHANTED_ENDER_PEARL".asInternalName(), "§aEnchanted Ender Pearl"),
        ENCHANTED_EYE_OF_ENDER("ENCHANTED_EYE_OF_ENDER".asInternalName(), "§aEnchanted Eye Of Ender"),
        ENCHANTED_END_STONE("ENCHANTED_END_STONE".asInternalName(), "§aEnchanted End Stone"),
        ENCHANTED_OBSIDIAN("ENCHANTED_OBSIDIAN".asInternalName(), "§aEnchanted Obsidian"),
        YOUNG_DRAGON_FRAGMENT("YOUNG_FRAGMENT".asInternalName(), "§5Young Dragon Fragment"),
        OLD_DRAGON_FRAGMENT("OLD_FRAGMENT".asInternalName(), "§5Old Dragon Fragment"),
        STRONG_DRAGON_FRAGMENT("STRONG_FRAGMENT".asInternalName(), "§5Strong Dragon Fragment"),
        WISE_DRAGON_FRAGMENT("WISE_FRAGMENT".asInternalName(), "§5Wise Dragon Fragment"),
        UNSTABLE_DRAGON_FRAGMENT("UNSTABLE_FRAGMENT".asInternalName(), "§5Unstable Dragon Fragment"),
        PROTECTOR_DRAGON_FRAGMENT("PROTECTOR_FRAGMENT".asInternalName(), "§5Protector Dragon Fragment"),
        SUPERIOR_DRAGON_FRAGMENT("SUPERIOR_FRAGMENT".asInternalName(), "§5Superior Dragon Fragment"),
        HOLY_DRAGON_FRAGMENT("HOLY_FRAGMENT".asInternalName(), "§5Holy Dragon Fragment"),
        ;
    }
}
