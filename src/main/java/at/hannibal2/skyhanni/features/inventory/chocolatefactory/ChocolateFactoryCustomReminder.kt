package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.minutes
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ChocolateFactoryCustomReminder {
    private val configReminder get() = ChocolateFactoryAPI.config.customReminder
    private val configUpgradeWarnings get() = ChocolateFactoryAPI.config.chocolateUpgradeWarnings

    private var targetGoal: Long?
        get() = ChocolateFactoryAPI.profileStorage?.targetGoal
        set(value) {
            ChocolateFactoryAPI.profileStorage?.targetGoal = value
        }
    private var targetName: String?
        get() = ChocolateFactoryAPI.profileStorage?.targetName
        set(value) {
            ChocolateFactoryAPI.profileStorage?.targetName = value
        }

    fun isActive() = targetGoal != null && configReminder.enabled

    private var display = emptyList<Renderable>()

    private var lastUpgradeWarning = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onChat(event: SystemMessageEvent) {
        if (!isEnabled()) return
        if (configReminder.hideChat) {
            if (event.message == "§cYou don't have enough Chocolate!") {
                event.blockedReason = "custom_reminder"
            }
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        update()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        val item = event.item ?: return
        if (event.clickedButton != 0) return
        // TODO add support for prestige and for Chocolate Milestone
        val cost = ChocolateFactoryAPI.getChocolateBuyCost(item.getLore()) ?: return
        val duration = ChocolateAmount.CURRENT.timeUntilGoal(cost)

        // the user has enough chocolate, and just bought something
        if (duration.isNegative()) {
            reset()
            return
        }
        setReminder(cost, item.name)
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!inChocolateMenu()) return
        if (ReminderUtils.isBusy()) return

        configReminder.position.renderRenderables(display, posLabel = "Chocolate Factory Custom Reminder")
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!configReminder.always) return
        if (Minecraft.getMinecraft().currentScreen is GuiChest) return
        if (ReminderUtils.isBusy()) return

        configReminder.position.renderRenderables(display, posLabel = "Chocolate Factory Custom Reminder")
    }

    private fun inChocolateMenu() = ChocolateShopPrice.inInventory || ChocolateFactoryAPI.inChocolateFactory ||
        ChocolateFactoryAPI.chocolateFactoryPaused

    private fun setReminder(target: Long, name: String) {
        if (targetName == name) {
            reset()
            return
        }
        targetGoal = target
        targetName = name
        update()
    }

    private fun update() {
        display = mutableListOf<Renderable>().also { list ->
            getTargetDescription()?.let {
                list.add(
                    Renderable.clickAndHover(
                        it, listOf("§eClick to remove the goal!"),
                        onClick = {
                            reset()
                        },
                    ),
                )
            }
        }
    }

    private fun getTargetDescription(): String? {
        val goal = targetGoal ?: return null
        val duration = ChocolateAmount.CURRENT.timeUntilGoal(goal)
        if (duration.isNegative()) {
            warn()
            return "§aGoal Reached! §eBuy §f$targetName"
        }
        val format = duration.format(maxUnits = 2)
        return "§f$targetName §ein §b$format"
    }

    private fun warn() {
        if (ReminderUtils.isBusy()) return
        if (inChocolateMenu()) return

        if (lastUpgradeWarning.passedSince() < configUpgradeWarnings.timeBetweenWarnings.minutes) return
        lastUpgradeWarning = SimpleTimeMark.now()

        if (configUpgradeWarnings.upgradeWarningSound) {
            SoundUtils.playBeepSound()
        }
        ChatUtils.clickableChat(
            "You can now purchase §f$targetName §ein Chocolate factory!",
            onClick = {
                HypixelCommands.chocolateFactory()
            },
            "§eClick to run /cf!",
        )
    }

    private fun reset() {
        targetGoal = null
        targetName = ""

        display = emptyList()
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && configReminder.enabled
}
