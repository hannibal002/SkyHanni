package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.features.misc.effects.NonGodPotEffectDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GummyWarning {

    private val config get() = SkyHanniMod.feature.slayer

    private var lastWarned = SimpleTimeMark.farPast()
    private var warningActive = SimpleTimeMark.farPast()

    private val SLAYER_WEAPONS = setOf(
        // Zombie
        "REVENANT_SWORD",
        "REAPER_SWORD",
        "AXE_OF_THE_SHREDDED",
        // Tara
        "TARANTULA_FANG",
        "SCORPION_FOIL",
        "STING",
        // Sven
        "SHAMAN_SWORD",
        "POOCH_SWORD",
        // Enderman
        "VOIDEDGE_KATANA",
        "VORPAL_KATANA",
        "ATOMSPLIT_KATANA",
        // TODO: repoify blaze daggers
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
        if (!config.gummyWarning) return
        if (SlayerApi.activeType == null) return

        val id = event.itemInHand?.getInternalNameOrNull()?.asString() ?: return
        if (!SLAYER_WEAPONS.contains(id)) return

        val armor = InventoryUtils.getArmor()
        val hasHabanero = armor.any { piece ->
            if (piece == null) return@any false
            val enchants = piece.getHypixelEnchantments() ?: return@any false
            enchants.containsKey("ultimate_habanero_tactics")
        }

        val inSmolderingTomb = SkyBlockUtils.scoreboardArea == "Smoldering Tomb"

        if (!hasHabanero && !inSmolderingTomb) return

        val hasSmoldering = NonGodPotEffectDisplay.isActive(NonGodPotEffect.SMOLDERING)
        if (hasSmoldering) return

        if (lastWarned.passedSince() < 10.seconds) return
        lastWarned = SimpleTimeMark.now()

        DelayedRun.runDelayed(0.5.seconds) {
            warningActive = SimpleTimeMark.now()
            SoundUtils.createSound("block.anvil.land", 0.5f).playSound()
        }
    }
        @HandleEvent(onlyOnSkyblock = true)
        fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
            if (!config.gummyWarning) return
            if (warningActive.passedSince() > 3.seconds) return

            val display = Renderable.text("§4§lNo Gummy Warning")
            config.gummyWarningPosition.renderRenderable(display, posLabel = "Gummy Warning")
        }
    }
