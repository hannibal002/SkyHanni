package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.OwnInventoryArmorUpdateEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.features.misc.effects.NonGodPotEffectDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GummyWarning {

    private val config get() = SkyHanniMod.feature.slayer
    private var lastWarned = SimpleTimeMark.farPast()
    private var lastWarningShown = SimpleTimeMark.farPast()
    private var hasHabanero = false

    /**
     * REGEX-TEST: Smoldering Tomb
     * REGEX-TEST: The Wasteland
     */
    private val smolderingAreaPattern by RepoPattern.pattern(
        "slayer.gummy.smoldering-area",
        "Smoldering Tomb|The Wasteland",
    )

    private var inSmolderingArea = false
    private var holdingSlayerWeapon = false
    private val display = Renderable.text("§4§lNo Polar Bear Active!", scale = 2.0)

    @HandleEvent(onlyOnSkyblock = true)
    fun onAreaChange(event: GraphAreaChangeEvent) {
        inSmolderingArea = smolderingAreaPattern.matches(SkyBlockUtils.graphArea)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onArmorChange(event: OwnInventoryArmorUpdateEvent) {
        if (!config.gummyWarning) return
        val armor = InventoryUtils.getArmor()
        hasHabanero = armor.any { piece ->
            if (piece == null) return@any false
            val enchants = piece.getHypixelEnchantments() ?: return@any false
            enchants.containsKey("ultimate_habanero_tactics")
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        if (!isEnabled()) return
        val activeSlayer = SlayerApi.activeType ?: run {
            holdingSlayerWeapon = false
            return
        }
        holdingSlayerWeapon = SlayerApi.slayerJsonData?.weapons[activeSlayer]?.contains(event.newItem) == true
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityClick(event: EntityClickEvent) {
        if (!isEnabled()) return
        if (event.action != EntityClickEvent.ActionType.ATTACK) return
        if (!holdingSlayerWeapon) return

        if (!hasHabanero && !inSmolderingArea) return

        val hasSmoldering = NonGodPotEffectDisplay.isActive(NonGodPotEffect.SMOLDERING)
        if (hasSmoldering) return

        if (lastWarned.passedSince() < 10.seconds) return
        lastWarned = SimpleTimeMark.now()

        DelayedRun.runDelayed(0.5.seconds) {
            lastWarningShown = SimpleTimeMark.now()
            SoundUtils.createSound("block.anvil.land", 0.5f).playSound()
            ChatUtils.notifyOrDisable(
                message = "You do not have an active Re-Heated Gummy Polar Bear!",
                option = config::gummyWarning,
            )
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (lastWarningShown.passedSince() > 3.seconds) return
        config.gummyWarningPosition.renderRenderable(display, posLabel = "Gummy Warning")
    }

    private fun isEnabled() = config.gummyWarning && SlayerApi.activeType != null
}

