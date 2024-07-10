package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.Arrays
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ChocolateFactoryStrayTracker {

    private val config get() = ChocolateFactoryAPI.config
    private val profileStorage get() = ChocolateFactoryAPI.profileStorage
    private var straysDisplay = listOf<Renderable>()

    private var claimedStraysSlots = mutableListOf<Int>()

    private val strayCaughtPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.caught",
        ".* §d§lCAUGHT!.*"
    )

    /**
     * REGEX-TEST: §7You caught a stray §6§lGolden Rabbit§7!§7You gained §6+13,566,571 Chocolate§7!
     * REGEX-TEST: §7You caught a stray §fCrush §7and§7gained §6+282,636 Chocolate§7!
     * REGEX-TEST: §7You caught a stray §9Fish the Rabbit§7!§7You have already found §9Fish the§9Rabbit§7, so you received §655,750,128§6Chocolate§7!
     */
    private val strayLorePattern by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.loreinfo",
        "§7You caught a stray (?<rabbit>.*) .*§6\\+(?<amount>[\\d,]*)[ §6]Chocolate§7!"
    )

    private val goldenStrayHoardPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.goldenhoard",
        "§7You caught a stray (?<rabbit>.*) .*§6A hoard of §aStray Rabbits §r§7has appeared!"
    )

    private val fishTheRabbitPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "stray.fish",
        "§7You caught a stray §9Fish the Rabbit§7!§7You have already found §9Fish the*§Rabbit*7, so you received .*§6\\+(?<amount>[\\d,]*) Chocolate§7!"
    )

    @SubscribeEvent
    fun onTick(event: SecondPassedEvent) {
        if(!LorenzUtils.inSkyBlock) return
        if(!config.strayRabbitTracker) return
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        val profileStorage = profileStorage ?: return
        InventoryUtils.getItemsInOpenChest().filter {
            !claimedStraysSlots.contains(it.slotIndex)
        }.forEach {
            strayCaughtPattern.matchMatcher(it.stack.name) {
                if(it.stack.getLore().isEmpty()) return
                claimedStraysSlots.add(it.slotIndex)
                val loreText = it.stack.getLore().toTypedArray().joinToString("")
                ChatUtils.chat("§d§lStray Matched!\n§6§lLore§r§7: ${loreText.replace("§", "*")}")
                strayLorePattern.matchMatcher(loreText) {
                    val rarity = group("rabbit").let {rab ->
                        when {
                            rab.startsWith("§f") -> "common"
                            rab.startsWith("§a") -> "uncommon"
                            rab.startsWith("§9") -> "rare"
                            rab.startsWith("§5") -> "epic"
                            rab.startsWith("§6") -> "legendary"
                            else -> return
                        }
                    }
                    val amount = group("amount").formatLong()
                    val format = ChocolateFactoryAPI.timeUntilNeed(amount + 1)
                    profileStorage.straysCaught[rarity] = profileStorage.straysCaught[rarity]?.plus(1) ?: 1
                    profileStorage.straysExtraChocMs[rarity] = profileStorage.straysExtraChocMs[rarity]?.plus(format.inWholeMilliseconds) ?: format.inWholeMilliseconds
                    //Asynchronously update to immediately reflect caught stray
                    updateStraysDisplay()
                }
            }
        }
        InventoryUtils.getItemsInOpenChest().filter {
            claimedStraysSlots.contains(it.slotIndex)
        }.forEach {
            if(!strayCaughtPattern.matches(it.stack.name)) {
                claimedStraysSlots.removeAt(claimedStraysSlots.indexOf(it.slotIndex))
            }
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!LorenzUtils.inSkyBlock) return;
        if(!ChocolateFactoryAPI.inChocolateFactory) return;
        updateStraysDisplay()
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        if(!config.strayRabbitTracker) return

        config.strayRabbitTrackerPosition.renderRenderables(straysDisplay, posLabel = "Stray Tracker")
    }

    private val rarityFormatMap = buildMap {
        put("common", "§fCommon§7: §r§f")
        put("uncommon", "§aUncommon§7: §r§a")
        put("rare", "§9Rare§7: §r§9")
        put("epic", "§5Epic§7: §r§5")
        put("legendary", "§6Legendary§7: §r§6")
    }

    private fun updateStraysDisplay() {
        val profileStorage = profileStorage ?: return
        val extraChocMs = profileStorage.straysExtraChocMs.values.sum().milliseconds
        val formattedExtraTime = extraChocMs.let { if(it == 0.milliseconds) "0s" else it.format() }

        straysDisplay = listOfNotNull(
            Renderable.string("§6§lStray Tracker"),
            extractHoverableOfRarity("common"),
            extractHoverableOfRarity("uncommon"),
            extractHoverableOfRarity("rare"),
            extractHoverableOfRarity("epic"),
            extractHoverableOfRarity("legendary"),
            Renderable.string(""),
            Renderable.string("§a+§b${formattedExtraTime} §afrom strays§7")
        )
    }

    private fun extractHoverableOfRarity(rarity: String): Renderable? {
        val profileStorage = profileStorage ?: return null
        val caughtOfRarity = profileStorage.straysCaught[rarity]
        val caughtString = caughtOfRarity?.toString() ?: return null

        val rarityExtraChocMs = profileStorage.straysExtraChocMs[rarity]?.milliseconds
        val extraChocFormat = rarityExtraChocMs?.format() ?: ""

        val lineHeader = rarityFormatMap[rarity] ?: ""
        val lineFormat = "${lineHeader}${caughtString}";

        return if (rarityExtraChocMs == null) {
            Renderable.string(lineFormat)
        } else {
            Renderable.hoverTips(
                Renderable.string(lineFormat),
                tips = listOf("§a+§b${extraChocFormat} §aof production§7")
            )
        }
    }
}
