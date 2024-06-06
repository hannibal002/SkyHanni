package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isRancherSign
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class GardenOptimalSpeed {

    private val config get() = GardenAPI.config.optimalSpeeds

    private val configCustomSpeed get() = config.customSpeed
    private var sneakingTime = 0.seconds
    private val sneaking get() = Minecraft.getMinecraft().thePlayer.isSneaking
    private val sneakingPersistent get() = sneakingTime > 5.seconds

    /**
     * This speed value represents the walking speed, not the speed stat.
     * Blocks per second = 4.317 * speed / 100
     *
     * It has an absolute speed cap of 500, and items that normally increase the cap do not apply here:
     * (Black Cat pet, Cactus knife, Racing Helmet or Young Dragon Armor)
     *
     * If this information ever gets abstracted away and made available outside this class,
     * and some features need the actual value of the Speed stat instead,
     * we can always just have two separate variables, like walkSpeed and speedStat.
     * But since this change is confined to Garden-specific code, it's fine the way it is for now.
     */
    private var currentSpeed = 100

    private var optimalSpeed = -1
    private var lastWarnTime = 0L
    private var cropInHand: CropType? = null
    private var rancherOverlayList: List<List<Any?>> = emptyList()
    private var lastToolSwitch = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        currentSpeed = (Minecraft.getMinecraft().thePlayer.capabilities.walkSpeed * 1000).toInt()

        if (sneaking) {
            currentSpeed = (currentSpeed * 0.3).toInt()
            sneakingTime += 50.milliseconds
        } else {
            sneakingTime = 0.seconds
        }
    }

    @SubscribeEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (!isRancherOverlayEnabled()) return
        val gui = event.gui
        if (gui !is GuiEditSign) return
        if (!gui.isRancherSign()) return
        rancherOverlayList = CropType.entries.map { crop ->
            listOf(crop.icon, Renderable.link("${crop.cropName} - ${crop.getOptimalSpeed()}") {
                LorenzUtils.setTextIntoSign("${crop.getOptimalSpeed()}")
            })
        }
    }

    @SubscribeEvent
    fun onGuiRender(event: DrawScreenEvent.Post) {
        if (!isRancherOverlayEnabled()) return
        val gui = event.gui
        if (gui !is GuiEditSign) return
        if (!gui.isRancherSign()) return
        config.signPosition.renderStringsAndItems(
            rancherOverlayList,
            posLabel = "Optimal Speed Rancher Overlay"
        )
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        lastToolSwitch = SimpleTimeMark.now()
        cropInHand = event.crop
        optimalSpeed = cropInHand?.getOptimalSpeed() ?: -1
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        for (value in CropType.entries) {
            ConditionalUtils.onToggle(value.getConfig()) {
                if (value == cropInHand) {
                    optimalSpeed = value.getOptimalSpeed()
                }
            }
        }
    }

    private fun CropType.getOptimalSpeed() = getConfig().get().toInt()

    private fun CropType.getConfig(): Property<Float> = with(configCustomSpeed) {
        when (this@getConfig) {
            CropType.WHEAT -> wheat
            CropType.CARROT -> carrot
            CropType.POTATO -> potato
            CropType.NETHER_WART -> netherWart
            CropType.PUMPKIN -> pumpkin
            CropType.MELON -> melon
            CropType.COCOA_BEANS -> cocoaBeans
            CropType.SUGAR_CANE -> sugarCane
            CropType.CACTUS -> cactus
            CropType.MUSHROOM -> mushroom
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!GardenAPI.inGarden()) return

        if (optimalSpeed == -1) return

        if (GardenAPI.hideExtraGuis()) return

        var text = "Optimal Speed: §f$optimalSpeed"
        if (optimalSpeed != currentSpeed) {
            text += " (§eCurrent: §f$currentSpeed"
            if (sneaking) text += " §7[Sneaking]"
            text += "§f)"
        }

        val recentlySwitchedTool = lastToolSwitch.passedSince() < 1.5.seconds
        val recentlyStartedSneaking = sneaking && !sneakingPersistent

        val colorCode =
            if (recentlySwitchedTool || recentlyStartedSneaking) "7" else if (optimalSpeed != currentSpeed) "c" else "a"

        if (config.showOnHUD) config.pos.renderString("§$colorCode$text", posLabel = "Garden Optimal Speed")
        if (optimalSpeed != currentSpeed && !recentlySwitchedTool && !recentlyStartedSneaking) warn()
    }

    private fun warn() {
        if (!config.warning) return
        if (!Minecraft.getMinecraft().thePlayer.onGround) return
        if (GardenAPI.onBarnPlot) return
        if (System.currentTimeMillis() < lastWarnTime + 20_000) return

        lastWarnTime = System.currentTimeMillis()
        LorenzUtils.sendTitle("§cWrong speed!", 3.seconds)
        cropInHand?.let {
            var text = "Wrong speed for ${it.cropName}: §f$currentSpeed"
            if (sneaking) text += " §7[Sneaking]"
            text += " §e(§f$optimalSpeed §eis optimal)"

            ChatUtils.chat(text)
        }
    }

    private fun isRancherOverlayEnabled() = GardenAPI.inGarden() && config.signEnabled

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.optimalSpeedEnabled", "garden.optimalSpeeds.enabled")
        event.move(3, "garden.optimalSpeedWarning", "garden.optimalSpeeds.warning")
        event.move(3, "garden.optimalSpeedSignEnabled", "garden.optimalSpeeds.signEnabled")
        event.move(3, "garden.optimalSpeedSignPosition", "garden.optimalSpeeds.signPosition")
        event.move(3, "garden.optimalSpeedPos", "garden.optimalSpeeds.pos")
        event.move(3, "garden.optimalSpeedCustom.wheat", "garden.optimalSpeeds.customSpeed.wheat")
        event.move(3, "garden.optimalSpeedCustom.carrot", "garden.optimalSpeeds.customSpeed.carrot")
        event.move(3, "garden.optimalSpeedCustom.potato", "garden.optimalSpeeds.customSpeed.potato")
        event.move(3, "garden.optimalSpeedCustom.netherWart", "garden.optimalSpeeds.customSpeed.netherWart")
        event.move(3, "garden.optimalSpeedCustom.pumpkin", "garden.optimalSpeeds.customSpeed.pumpkin")
        event.move(3, "garden.optimalSpeedCustom.melon", "garden.optimalSpeeds.customSpeed.melon")
        event.move(3, "garden.optimalSpeedCustom.cocoaBeans", "garden.optimalSpeeds.customSpeed.cocoaBeans")
        event.move(3, "garden.optimalSpeedCustom.sugarCane", "garden.optimalSpeeds.customSpeed.sugarCane")
        event.move(3, "garden.optimalSpeedCustom.cactus", "garden.optimalSpeeds.customSpeed.cactus")
        event.move(3, "garden.optimalSpeedCustom.mushroom", "garden.optimalSpeeds.customSpeed.mushroom")

        event.move(14, "garden.optimalSpeeds.enabled", "garden.optimalSpeeds.showOnHUD")
    }
}
