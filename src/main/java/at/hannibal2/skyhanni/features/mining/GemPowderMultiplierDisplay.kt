package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.HotmApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.data.BossbarData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.hotx.HotmData
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.mining.powdertracker.PowderChestReward
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object GemPowderMultiplierDisplay {

    private val config get() = SkyHanniMod.feature.chat

    private var is2xPowderActive = false

    private val patternGroup = RepoPattern.group("mining.powder.multiplier")
    private val powderBossBarPattern by patternGroup.pattern(
        "powder.bossbar",
        "§e§lPASSIVE EVENT §b§l2X POWDER §e§lRUNNING FOR §a§l(?<time>.*)§r",
    )

    private val drillBasePowderPattern by patternGroup.pattern(
        "drill.powder.base",
        "§7§7Grants §d\\+(?<bonus>\\d+)% §dGemstone Powder§7, and"
    )

    private val drillUpgradePowderPattern by patternGroup.pattern(
        "drill.powder.upgrade",
        "§7Earn §9\\+(?<bonus>\\d+)% Powder §7from all sources."
    )

    // This is pretty much duplicated from the powder tracker...
    // I am going to look for a way to deduplicate them without cluttering the repo.
    @HandleEvent
    fun onSecondPassed() {
        if (!isEnabled()) return

        is2xPowderActive = false

        if (TabWidget.EVENT.isActive) {
            for (line in TabWidget.EVENT.lines) {
                if (line.contains("2x Powder")) {
                    is2xPowderActive = true
                    break
                }
            }
        } else {
            powderBossBarPattern.matchMatcher(BossbarData.getBossbar()) {
                is2xPowderActive = true
            }
        }
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return

        PowderChestReward.GEMSTONE_POWDER.chatPattern.matchMatcher(event.message) {
            val amountStr = groupOrNull("amount") ?: return
            val originalAmount = amountStr.formatInt()

            val multiplier = calculateTotalMultiplier()

            val effectiveAmount = (originalAmount * multiplier).toInt()

            ChatUtils.debug("[PowderDebug] Base: $originalAmount, Effective: $effectiveAmount, Multiplier: $multiplier")
            if (multiplier > 1.0) {
                val originalFormatted = originalAmount.addSeparators()
                val effectiveFormatted = effectiveAmount.addSeparators()

                event.chatComponent = "    §r§dGemstone Powder §r§8x$originalFormatted §7(x$effectiveFormatted)".asComponent()
            }
        }
    }

    private fun getDrillBonus(): Int {
        val heldItem = InventoryUtils.getItemInHand() ?: return 0

        val internalName = heldItem.getInternalName()
        if (!internalName.contains("DRILL")) return 0
        var totalBonus = 0

        val lore = heldItem.getLore()

        lore.forEach { line ->
            drillBasePowderPattern.matchMatcher(line) {
                val bonus = group("bonus").toInt()
                totalBonus += bonus
            }
            drillUpgradePowderPattern.matchMatcher(line) {
                val bonus = group("bonus").toInt()
                totalBonus += bonus
            }
        }
        ChatUtils.debug("[PowderDebug] Drill multiplier bonus: $totalBonus")
        return totalBonus
    }


    private fun getPetMultiplier(): Double {
        var multiplier = 1.0
        val pet = CurrentPetApi.currentPet ?: return multiplier
        if (pet.cleanName == "Scatha" && pet.rarity == LorenzRarity.LEGENDARY) {
            multiplier += 0.2 * pet.level / 100.0
        }
        ChatUtils.debug("[PowderDebug] Pet multiplier bonus: $multiplier")
        return multiplier
    }

    private fun getHOTMTrackableMultiplier(): Double {
        var multiplier = 1.0

        if (HotmData.POWDER_BUFF.enabled) {
            ChatUtils.debug("[PowderDebug] HOTM active level: ${HotmData.POWDER_BUFF.activeLevel}, mul: ${1 + (HotmData.POWDER_BUFF.activeLevel / 100)}")
            multiplier += HotmData.POWDER_BUFF.activeLevel / 100.0
        }

        multiplier += getDrillBonus() / 100.0

        return multiplier
    }

    private fun calculateTotalMultiplier(): Double {
        var multiplier = 1.0
        // This is atomized crystals shard
        val atomizedCrystalsMulti = 0.10
        // test numbers until I can get these
        val echoOfAtomizedMulti = 0.02
        multiplier += atomizedCrystalsMulti
        multiplier += atomizedCrystalsMulti * echoOfAtomizedMulti

        if (is2xPowderActive) {
            multiplier *= 2.0
        }

        if (HotmData.SKY_MALL.enabled && HotmApi.skymall == HotmApi.SkymallPerk.EXTRA_POWDER) {
            multiplier *= 1.15
        }

        ChatUtils.debug("[PowderDebug] Multiplier after 2x: $multiplier")
        multiplier *= getHOTMTrackableMultiplier();
        ChatUtils.debug("[PowderDebug] Multiplier after drill: $multiplier")
        multiplier *= getPetMultiplier();
        ChatUtils.debug("[PowderDebug] Multiplier after pet: $multiplier")

        return multiplier
    }

    private fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isCurrent() && config.showEffectivePowder
}
