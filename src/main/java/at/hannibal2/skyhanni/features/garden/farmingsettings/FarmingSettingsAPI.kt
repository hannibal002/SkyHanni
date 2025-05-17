package at.hannibal2.skyhanni.features.garden.farmingsettings

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.farmingsettings.FarmingSettingsConfig.WarningType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SignUtils
import at.hannibal2.skyhanni.utils.SignUtils.isRancherSign
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FarmingSettingsAPI {

    private val config get() = GardenApi.config.farmingSettings

    private var sneakingSince = SimpleTimeMark.farFuture()
    private val sneaking get() = Minecraft.getMinecraft().thePlayer.isSneaking

    private var lastToolSwitch = SimpleTimeMark.farPast()
    private var lastWarnTime = SimpleTimeMark.farPast()

    private val rancherBoots = "RANCHERS_BOOTS".toInternalName()
    private val mousemat = "SQUEAKY_MOUSEMAT".toInternalName()

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
    private var currentYaw = 0f
    private var currentPitch = 0f

    private var cropType: CropType? = null

    private data class CropSettings(val speed: Int, val yaw: Float, val pitch: Float)
    private data class CropPropertys(val speed: Property<Float>, val yaw: Property<Float>, val pitch: Property<Float>)

    fun createDisplay(gui: GuiEditSign): List<Renderable> {
        val crops = CropType.entries.map { it to it.getOptimalSettings() }
        val isRancher = gui.isRancherSign()

        return if (config.compactShortcutGUI) {
            crops.groupBy(
                { if (isRancher) it.second.speed to 0 else it.second.yaw to it.second.pitch },
                { it.first },
            ).map { (settings, crops) ->
                val label = if (isRancher) "${settings.first}" else "${settings.first}°/${settings.second}°"

                buildCropLink(
                    HorizontalContainerRenderable(crops.map { Renderable.itemStack(it.icon) }),
                    label,
                    isSelected = cropType in crops,
                    onClick = {
                        SignUtils.setTextIntoSign("${settings.first}")
                        if (!isRancher) SignUtils.setTextIntoSign("${settings.second}", line = 3)
                    },
                )
            }
        } else {
            crops.map { (crop, settings) ->
                val label = crop.cropName + if (isRancher) "${settings.speed}" else "${settings.yaw}°/${settings.pitch}°"

                buildCropLink(
                    Renderable.itemStack(crop.icon), label, isSelected = cropType == crop,
                    onClick = {
                        SignUtils.setTextIntoSign(
                            if (isRancher) "${settings.speed}" else "${settings.yaw}",
                        )
                        if (!isRancher) SignUtils.setTextIntoSign("${settings.pitch}", line = 3)
                    },
                )
            }
        }
    }

    private fun buildCropLink(
        iconRenderable: Renderable,
        label: String,
        isSelected: Boolean,
        onClick: () -> Unit,
    ): Renderable {
        val color = if (isSelected) LorenzColor.GOLD else LorenzColor.WHITE
        return Renderable.link(
            HorizontalContainerRenderable(
                listOf(
                    iconRenderable,
                    RenderableString("${color.getChatColor()} - $label"),
                ),
                spacing = 2,
            ),
            onLeftClick = onClick,
            underlineColor = color.toColor(),
        )
    }

    fun createStatus(): Renderable? {
        if (!GardenApi.hasFarmingToolInHand() && !isHolding(rancherBoots) && !isHolding(mousemat)) return null

        val (optimalSpeed, optimalYaw, optimalPitch) = cropType?.getOptimalSettings() ?: return null

        val recentlySwitchedTool = lastToolSwitch.passedSince() < 1.5.seconds
        val recentlyStartedSneaking = sneaking && sneakingSince.passedSince() < 5.seconds

        val (speedColor, yawColor, pitchColor) = listOf(
            if (recentlySwitchedTool || recentlyStartedSneaking) "7" else if (optimalSpeed != currentSpeed) "c" else "a",
            if (recentlySwitchedTool) "7" else if (optimalYaw != currentYaw) "c" else "a",
            if (recentlySwitchedTool) "7" else if (optimalPitch != currentPitch) "c" else "a",
        )

        return VerticalContainerRenderable(
            listOf(
                buildStatusString("Speed", speedColor, optimalSpeed, currentSpeed),
                buildStatusString("Yaw", yawColor, optimalYaw, currentYaw),
                buildStatusString("Pitch", pitchColor, optimalPitch, currentPitch),
            ),
            0, RenderUtils.HorizontalAlignment.LEFT, RenderUtils.VerticalAlignment.TOP,
        )
    }

    private fun buildStatusString(
        type: String,
        color: String,
        value: Any,
        currentValue: Any,
    ): Renderable {
        var statusString = "§${color}Optimal $type: §f$value"
        if (value != currentValue) {
            statusString += " (§eCurrent: §f$currentValue"
            if (sneaking) statusString += " §7[Sneaking]"
            statusString += "§f)"
        }
        return RenderableString(statusString)
    }

    fun handleWarning() {
        if (!isEnabled() || lastWarnTime.passedSince() < 20.seconds) return
        lastWarnTime = SimpleTimeMark.now()

        if (WarningType.WHEN_USING.isSelected()) sendWarnings(WarningType.WHEN_USING)

        if (Minecraft.getMinecraft().thePlayer.onGround && !GardenApi.onBarnPlot) {
            if (WarningType.WHEN_FARMING.isSelected() && GardenApi.isCurrentlyFarming()) sendWarnings(WarningType.WHEN_FARMING)
            if (WarningType.WHEN_WALKING.isSelected()) sendWarnings(WarningType.WHEN_WALKING)
        }
    }

    private fun sendWarnings(type: WarningType) {
        val (optimalSpeed, optimalYaw, optimalPitch) = cropType?.getOptimalSettings() ?: return

        val speedWarn = optimalSpeed != currentSpeed
        val yawWarn = optimalYaw != currentYaw
        val pitchWarn = optimalPitch != currentPitch

        if (type == WarningType.WHEN_USING) {
            if (speedWarn && isWearingRanchers()) {
                TitleManager.sendTitle("§cWrong Speed! Fix it in chat.", duration = 3.seconds)
                warn("speed", currentSpeed, optimalSpeed, true)
            }

            if (isHolding(mousemat)) {
                TitleManager.sendTitle("§cWrong Settings!", duration = 3.seconds)
                if (yawWarn) warn("yaw", currentYaw, optimalYaw, true)
                if (pitchWarn) warn("pitch", currentPitch, optimalPitch, true)
            }
        } else {
            TitleManager.sendTitle("§cWrong Settings!", duration = 3.seconds)

            val mousematPresent = InventoryUtils.getItemsInOwnInventory().any { it.getInternalNameOrNull() == mousemat }

            if (speedWarn) warn("speed", currentSpeed, optimalSpeed, isWearingRanchers())
            if (yawWarn) warn("yaw", currentYaw, optimalYaw, mousematPresent)
            if (pitchWarn) warn("pitch", currentPitch, optimalPitch, mousematPresent)
        }
    }

    private fun warn(type: String, current: Any, optimal: Any, present: Boolean) {
        val text = warnText(type, "$current", "$optimal")

        if (present) {
            if (type in listOf("yaw", "pitch")) {
                val mousematStack = InventoryUtils.getItemsInOwnInventory().find { it.getInternalNameOrNull() == mousemat } ?: return
                val saved = mousematStack.extraAttributes.getFloat("mousemat_$type")

                if (saved == optimal) return ChatUtils.hoverableChat(
                    text,
                    hover = listOf("§eLeft Click your §6Squeaky Mousemat §eto set the correct $type!"),
                )
            }

            val actionName =
                "change the $type${if (type in listOf("yaw", "pitch") && !isHolding(mousemat)) " (must hold Squeaky Mousemat)" else ""}"

            ChatUtils.clickToActionOrDisable(
                text,
                config::warning,
                actionName,
                action = {
                    if (type == "speed") HypixelCommands.setMaxSpeed(optimal as Int)
                    else HypixelCommands.setDirection(type, optimal as Float)
                },
            )
        } else {
            ChatUtils.clickableChat(
                text,
                onClick = { config::warning.jumpToEditor() },
                hover = "§eClick to disable this feature!",
                replaceSameMessage = true,
            )
        }
    }

    private fun warnText(type: String, current: String, optimal: String): String {
        val cropInHand = cropType ?: return ""
        return "§cWrong $type while farming ${cropInHand.cropName} detected!" +
            "\n§eCurrent ${type.firstLetterUppercase()}: §f$current§e, Optimal ${type.firstLetterUppercase()}: §f$optimal"
    }

    private fun isWearingRanchers() = InventoryUtils.getBoots()?.getInternalNameOrNull() == rancherBoots

    private fun isHolding(internalName: NeuInternalName) = InventoryUtils.getItemInHand()?.getInternalNameOrNull() == internalName

    private fun WarningType.isSelected(): Boolean = config.warningTypes.contains(this)

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick() {
        if (!isEnabled()) return
        val player = Minecraft.getMinecraft().thePlayer

        currentSpeed = (player.capabilities.walkSpeed * 1000).toInt()
        currentYaw = LocationUtils.calculatePlayerYaw().roundTo(2)
        currentPitch = player.rotationPitch.roundTo(2)

        if (sneaking && !sneakingSince.isInPast()) {
            sneakingSince = SimpleTimeMark.now()
            currentSpeed = (currentSpeed * 0.3).toInt()
        } else if (!sneaking && sneakingSince.isInPast()) {
            sneakingSince = SimpleTimeMark.farFuture()
        }
    }

    @HandleEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        if (!isEnabled()) return

        lastToolSwitch = SimpleTimeMark.now()
        event.crop?.let { cropType = it }
    }

    private fun CropType.getOptimalSettings(): CropSettings = with(getConfig()) {
        return CropSettings(
            speed = this.speed.get().toInt(),
            yaw = this.yaw.get(),
            pitch = this.pitch.get(),
        )
    }

    private fun CropType.getConfig(): CropPropertys = with(config.customSettings) {
        when (this@getConfig) {
            CropType.WHEAT -> CropPropertys(wheat.speed, wheat.yaw, wheat.pitch)
            CropType.CARROT -> CropPropertys(carrot.speed, carrot.yaw, carrot.pitch)
            CropType.POTATO -> CropPropertys(potato.speed, potato.yaw, potato.pitch)
            CropType.NETHER_WART -> CropPropertys(netherWart.speed, netherWart.yaw, netherWart.pitch)
            CropType.PUMPKIN -> CropPropertys(pumpkin.speed, pumpkin.yaw, pumpkin.pitch)
            CropType.MELON -> CropPropertys(melon.speed, melon.yaw, melon.pitch)
            CropType.COCOA_BEANS -> CropPropertys(cocoaBeans.speed, cocoaBeans.yaw, cocoaBeans.pitch)
            CropType.SUGAR_CANE -> CropPropertys(sugarCane.speed, sugarCane.yaw, sugarCane.pitch)
            CropType.CACTUS -> CropPropertys(cactus.speed, cactus.yaw, cactus.pitch)
            CropType.MUSHROOM -> CropPropertys(mushroom.speed, mushroom.yaw, mushroom.pitch)
        }
    }

    private fun isEnabled() = GardenApi.inGarden() && (config.showOnHUD || config.warning || config.shortcutGUI)
}
