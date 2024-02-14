package at.hannibal2.skyhanni.features.itemabilities.abilitycooldown

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.item.ItemAbilityCastEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangFreezeCooldown
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ActiveAbility(
    val type: ItemAbilityType,
    var text: ItemText? = null,

    private var lastActivation: SimpleTimeMark = SimpleTimeMark.farPast(),
    var specialColor: LorenzColor? = null,
    private var lastItemClick: SimpleTimeMark = SimpleTimeMark.farPast(),
) {

    fun activate(color: LorenzColor? = null, customCooldown: Duration = type.cooldown) {
        specialColor = color
        lastActivation = SimpleTimeMark.now() - (type.cooldown - customCooldown)
    }

    fun isOnCooldown(): Boolean = getDuration().isPositive()

    private fun getCooldown(): Duration {
        // Some items aren't really a cooldown but an effect over time, so don't apply cooldown multipliers
        if (type == ItemAbilityType.WAND_OF_ATONEMENT || type == ItemAbilityType.RAGNAROCK_AXE) {
            return type.cooldown
        }

        return type.cooldown * getMultiplier()
    }

    fun getDurationText(): String = getDuration().format(showMilliSeconds = getDuration() < 1.6.seconds).dropLast(1)

    fun getDuration() = getCooldown() - lastActivation.passedSince()

    fun onClick(clickType: ClickType) {
        if (lastItemClick.passedSince() < type.allowRecastAfter) return
        if (!isAllowed()) return

        // only allow left clicks on alternative position, only right clicks on others
        if (type.alternativePosition != (clickType == ClickType.LEFT_CLICK)) return

        startAbility()
    }

    private fun startAbility() {
        if (ItemAbilityCastEvent(type).postAndCatch()) return

        lastItemClick = SimpleTimeMark.now()
        lastActivation = SimpleTimeMark.now()
    }

    fun getMultiplier(): Double = getMageCooldownReduction() ?: 1.0

    private fun getMageCooldownReduction(): Double? {
        if (type.ignoreMageCooldownReduction) return null
        if (!LorenzUtils.inDungeons) return null
        if (DungeonAPI.playerClass != DungeonAPI.DungeonClass.MAGE) return null

        var abilityCooldownMultiplier = 1.0
        abilityCooldownMultiplier -= if (DungeonAPI.isUniqueClass) {
            0.5 // 50% base reduction at level 0
        } else {
            0.25 // 25% base reduction at level 0
        }

        // 1% ability reduction every other level
        abilityCooldownMultiplier -= 0.01 * floor(DungeonAPI.playerClassLevel / 2f)

        return abilityCooldownMultiplier
    }

    private fun allAbilitiesBlocked(): Boolean {
        if (LorenzUtils.skyBlockArea == "Matriarch's Lair") return true
        if (AshfangFreezeCooldown.iscurrentlyFrozen()) return true

        return false
    }

    fun isAllowed(): Boolean = type.isAllowed() && !allAbilitiesBlocked()
}
