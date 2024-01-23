package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.features.commands.LimboCommands
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class LimboTimeTracker {
    private val config get() = SkyHanniMod.feature.misc

    private var limboJoinTime = SimpleTimeMark.farPast()
    private var inLimbo = false
    private var inFakeLimbo = false
    private var shownPB = false
    private var oldPB: Duration = 0.seconds
    private var userLuck: Float = 0.0F
    private val userLuckMultiplier = 0.000810185F
    private val fireMultiplier = 1.01F
    private var onFire = false

    private lateinit var modifiedArray: MutableList<String>
    private var setMinutes = false
    private val minutesRegex by RepoPattern.pattern("limbo.tooltip.minutes", "§5§o§a\\d+(\\.\\d+)? minutes.+\$")
    private val hoursRegex by RepoPattern.pattern("limbo.tooltip.hours", "§5§o§b\\d+(\\.\\d+)? hours.+\$")

    private var wholeMinutes: Long = 0
    private var hoursString: String = ""

    private val bedwarsLobbyLimbo = AxisAlignedBB(-662.0, 43.0, -76.0, -619.0, 86.0, -27.0)

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (event.message == "§cYou are AFK. Move around to return from AFK." || event.message == "§cYou were spawned in Limbo.") {
            limboJoinTime = SimpleTimeMark.now()
            inLimbo = true
            LimboCommands.enterLimbo(limboJoinTime)
            onFire = Minecraft.getMinecraft().thePlayer.isBurning
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        if (config.limboPlaytime < config.limboTimePB) {
            config.limboPlaytime = config.limboTimePB
            LorenzUtils.debug("Setting limboPlaytime = limboTimePB, since limboPlaytime was lower.")
        }
    }

    @SubscribeEvent
    fun catchPlaytime(event: MessageSendToServerEvent) {
        if (event.message.startsWith("/playtime") && inLimbo) {
            event.isCanceled
            LimboCommands.printPlaytime(true)
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (inLimbo && !shownPB && limboJoinTime.passedSince() >= config.limboTimePB.seconds && config.limboTimePB != 0) {
            shownPB = true
            oldPB = config.limboTimePB.seconds
            LorenzUtils.chat("§d§lPERSONAL BEST§f! You've surpassed your previous record of §e$oldPB§f!")
            LorenzUtils.chat("§fKeep it up!")
        }
        val lobbyName: String? = HypixelData.locrawData?.get("lobbyname")?.asString
        if (lobbyName.toString().startsWith("bedwarslobby")) {
            if (bedwarsLobbyLimbo.isPlayerInside()) {
                if (inFakeLimbo) return
                limboJoinTime = SimpleTimeMark.now()
                inLimbo = true
                LimboCommands.enterLimbo(limboJoinTime)
                inFakeLimbo = true
            }
            else {
                if (inLimbo) {
                    leaveLimbo()
                    inFakeLimbo = false
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (!inLimbo) return
        leaveLimbo()
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (event.inventory !is ContainerLocalMenu) return
        if (event.inventory.displayName.unformattedText != "Detailed /playtime") return
        if (event.slotNumber != 43) return
        val limboItem by lazy {
            val neuItem = NEUItems.getItemStack("ENDER_PEARL")
            if (wholeMinutes >= 60) Utils.createItemStack(neuItem.item, "§aLimbo", "§7Playtime: §a${wholeMinutes.addSeparators()} minutes", "§7Or: §b$hoursString hours")
            else Utils.createItemStack(neuItem.item, "§aLimbo", "§7Playtime: §a$wholeMinutes minutes")
        }
        event.replaceWith(limboItem)
    }

    private fun isValidIndex(index: Int): Boolean {
        return !(index % 9 == 0 || index + 1 % 9 == 0 || index !in 10..43)
    }

    @SubscribeEvent
    fun onHoverItem(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!event.slot.inventory.displayName.unformattedText.startsWith("Detailed /playtime")) return
        if (event.slot.slotIndex != 4) return

        val lore = event.toolTip
        val hoursArray = lore.filter { hoursRegex.matches(it) }.toMutableList()
        val minutesArray = lore.filter { minutesRegex.matches(it) }.toMutableList()

        addLimbo(hoursArray, minutesArray)
        remakeArray(event.toolTip, minutesArray, hoursArray)
    }

    @SubscribeEvent
    fun onRenderGUI(event: InventoryOpenEvent) {
        if (event.inventoryName != "Detailed /playtime") return
        val playtime = config.limboPlaytime.seconds
        val wholeHours = playtime.inWholeHours
        wholeMinutes = playtime.inWholeMinutes
        if ((wholeMinutes%60).toInt() == 0) {
            hoursString = "$wholeHours"
        } else {
            val minutes:Float = ((wholeMinutes - wholeHours * 60).toFloat() / 60)
            hoursString = wholeHours.addSeparators()+minutes.round(1).toString().replace("0", "")
        }
    }

    private fun addLimbo(hoursArray: MutableList<String>, minutesArray: MutableList<String>) {
        if (wholeMinutes >= 60) {
            val hours = config.limboPlaytime.seconds.inWholeHours
            val minutes = (config.limboPlaytime.seconds.inWholeMinutes-(hours*60).toFloat()/6).toInt()
            modifiedArray = hoursArray
            if (minutes == 0) modifiedArray.add("§b$hours hours §7on Limbo")
            else modifiedArray.add("§b$hoursString hours §7on Limbo")
            modifiedArray = modifiedArray.sortedByDescending {
                it.substringAfter("§b").substringBefore(" hours").toDoubleOrNull()
            }.toMutableList()
            setMinutes = false
        }
        else {
            val minutes = config.limboPlaytime.seconds.inWholeMinutes
            modifiedArray = minutesArray
            modifiedArray.add("§a$minutes minutes §7on Limbo")
            modifiedArray = modifiedArray.sortedByDescending {
                it.substringAfter("§a").substringBefore(" minutes").toDoubleOrNull()
            }.toMutableList()
            setMinutes = true
        }
    }

    private fun remakeArray(toolTip: MutableList<String>, minutesArray: MutableList<String>, hoursArray: MutableList<String>) {
        val firstLine = toolTip.first()
        val totalPlaytime = toolTip.last()
        toolTip.clear()
        toolTip.add(firstLine)
        if (!setMinutes) {
            toolTip.addAll(modifiedArray)
            toolTip.addAll(minutesArray)
        } else {
            toolTip.addAll(hoursArray)
            toolTip.addAll(modifiedArray)
        }
        toolTip.add(totalPlaytime)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!inLimbo) return
        if (LorenzUtils.inSkyBlock) {
            leaveLimbo()
            return
        }
        val duration = limboJoinTime.passedSince().format()
        config.showTimeInLimboPosition.renderString("§eIn limbo since §b$duration", posLabel = "Limbo Time Tracker")
    }

    private fun leaveLimbo() {
        inLimbo = false
        if (!isEnabled()) return
        val passedSince = limboJoinTime.passedSince()
        val duration = passedSince.format()
        val currentPB = config.limboTimePB.seconds
        if (passedSince > currentPB) {
            oldPB = currentPB
            config.limboTimePB = passedSince.toInt(DurationUnit.SECONDS)
            userLuck = (config.limboTimePB * userLuckMultiplier).round(2)
            if (onFire) userLuck *= fireMultiplier
            LorenzUtils.chat("§fYou were in Limbo for §e$duration§f! §d§lPERSONAL BEST§r§f!")
            LorenzUtils.chat("§fYour previous Personal Best was §e$oldPB.")
        } else LorenzUtils.chat("§fYou were in Limbo for §e$duration§f.")
        if (userLuck > config.userLuck) {
            if (onFire) {
                LorenzUtils.chat("§fYour §aPersonal Bests§f perk is now granting you §a+${userLuck.round(2)}§c✴ §aSkyHanni User Luck§f! ")
            } else {
                LorenzUtils.chat("§fYour §aPersonal Bests§f perk is now granting you §a+${userLuck.round(2)}✴ SkyHanni User Luck§f!")
            }
        }
        config.limboPlaytime += passedSince.toInt(DurationUnit.SECONDS)
        if (userLuck > config.userLuck) config.userLuck = userLuck
        onFire = false
        shownPB = false
    }

    fun isEnabled() = config.showTimeInLimbo
}
