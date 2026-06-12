package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import net.minecraft.sounds.SoundEvents
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.features.misc.effects.NonGodPotEffectDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import net.minecraft.client.Minecraft
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GummyWarning {

    private val config get() = SkyHanniMod.feature.slayer.gummyWarning

    private var lastWarned = SimpleTimeMark.farPast()

    private val SLAYER_WEAPONS = setOf(
        // Zombie
        "REVENANT_SWORD",
        "REAPER_SWORD",
        "AXE_OF_THE_SHREDDED",
        // Spider
        "TARANTULA_FANG",
        "SCORPION_FOIL",
        "STING",
        // Wolf
        "SHAMAN_SWORD",
        "POOCH_SWORD",
        // Enderman
        "VOIDEDGE_KATANA",
        "VORPAL_KATANA",
        "ATOMSPLIT_KATANA",
        // Blaze
        "FIREDUST_DAGGER",
        "MAWDUST_DAGGER",
        "BURSTMAW_DAGGER",
        "BURSTFIRE_DAGGER",
        "HEARTFIRE_DAGGER",
        "HEARTMAW_DAGGER",
    )

    @HandleEvent
    fun onEntityClick(event: EntityClickEvent) {
        if (event.action != EntityClickEvent.ActionType.ATTACK) return
        if (!config.enabled) return

        val player = Minecraft.getInstance().player ?: return
        val heldItem = player.mainHandItem
        if (!isSlayerWeapon(heldItem)) return

        val armor = InventoryUtils.getArmor()

        val hasHabanero = armor.any { piece ->
            if (piece == null) return@any false
            val enchants = piece.getHypixelEnchantments() ?: return@any false
            enchants.containsKey("ultimate_habanero_tactics")
        }

        val hasSmoldering = NonGodPotEffectDisplay.isActive(NonGodPotEffect.SMOLDERING)

        if (!hasHabanero) return
        if (hasSmoldering) return

        if (lastWarned.passedSince() < 10.seconds) return
        lastWarned = SimpleTimeMark.now()

        sendWarning()
    }

    private fun isSlayerWeapon(stack: SafeItemStack): Boolean {
        val id = stack.getInternalNameOrNull()?.asString() ?: return false
        return id in SLAYER_WEAPONS
    }

    private fun sendWarning() {
        TitleManager.sendTitle("§4§lNo Gummy Warning", duration = 2.seconds)
        Minecraft.getInstance().player?.playSound(
            SoundEvents.BLAZE_HURT,
            0.3f, 0.5f
        )
    }
}
