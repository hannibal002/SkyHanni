package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.features.misc.effects.NonGodPotEffectDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
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
    private val smolderingAreaPattern by RepoPattern.pattern(
        "slayer.gummy.smoldering-area",
        "Smoldering Tomb|The Wasteland"
    )
    private var inSmolderingArea = false
    private val display = Renderable.text("§4§lNo Polar Bear Active!", scale = 2.0)

    @HandleEvent(onlyOnSkyblock = true)
    fun onAreaChange(event: GraphAreaChangeEvent) {
        inSmolderingArea = smolderingAreaPattern.matcher(SkyBlockUtils.graphArea ?: "").find()
    }

    private var slayerData: RemainingSlayerKills.SlayerData? = null

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        slayerData = event.getConstant<RemainingSlayerKills.SlayerData>("Slayer")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityClick(event: EntityClickEvent) {
        if (event.action != EntityClickEvent.ActionType.ATTACK) return
        if (!config.gummyWarning) return
        if (SlayerApi.activeType == null) return

        val id = event.itemInHand?.getInternalNameOrNull() ?: return
        if (slayerData?.weapons?.get(SlayerApi.activeType)?.containsKey(id) != true) return

        val armor = InventoryUtils.getArmor()
        val hasHabanero = armor.any { piece ->
            if (piece == null) return@any false
            val enchants = piece.getHypixelEnchantments() ?: return@any false
            enchants.containsKey("ultimate_habanero_tactics")
        }

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
                option = SkyHanniMod.feature.slayer::gummyWarning,
            )
        }
    }
        @HandleEvent(onlyOnSkyblock = true)
        fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
            if (!config.gummyWarning) return
            if (lastWarningShown.passedSince() > 3.seconds) return

            config.gummyWarningPosition.renderRenderable(display, posLabel = "Gummy Warning")
        }
    }

