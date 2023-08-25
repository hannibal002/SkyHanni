package at.hannibal2.skyhanni.features.misc.powdertracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.slayer.SlayerItemProfitTracker
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.LorenzUtils.afterChange
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityChest
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class PowderTracker {

    private val config get() = SkyHanniMod.feature.misc.powderTrackerConfig
    private var display = emptyList<List<Any>>()
    private val picked = "§6You have successfully picked the lock on this chest!".toPattern()
    private val powderEvent = ".*§r§b§l2X POWDER STARTED!.*".toPattern()
    private val powderEnded = ".*§r§b§l2X POWDER ENDED!.*".toPattern()

    private var doublePowder = false
    private var currentDisplayMode = DisplayMode.TOTAL
    private var inventoryOpen = false
    private var currentSessionData = mutableMapOf<Int, Storage.ProfileSpecific.PowderTracker>()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        val currentlyOpen = Minecraft.getMinecraft().currentScreen is GuiInventory
        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            saveAndUpdate()
        }

        config.position.renderStringsAndItems(
            display,
            posLabel = "Powder Chest Tracker")
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val msg = event.message
        val both = currentLog() ?: return
        println("msg: ${event.message}")

        picked.matchMatcher(msg) { both.modify { it.totalChestPicked += 1 } }
        powderEvent.matchMatcher(msg) { doublePowder = true }
        powderEnded.matchMatcher(msg) { doublePowder = false }


        for (reward in PowderChestReward.entries) {
            reward.pattern.matchMatcher(msg) {
                both.modify {
                    val count = it.rewards[reward] ?: 0
                    it.rewards[reward] = count + group("amount").formatNumber()
                }
            }
        }
        saveAndUpdate()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.textFormat.afterChange { saveAndUpdate() }
        saveAndUpdate()
    }

    private fun saveAndUpdate() {
        display = formatDisplay(drawDisplay())
    }

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        val list = mutableListOf<List<Any>>()
        for (index in config.textFormat.get()) {
            list.add(map[index])
        }
        return list
    }

    private fun drawDisplay() = buildList<List<Any>> {
        addAsSingletonList("§b§lPowder Tracker")
        if (inventoryOpen) {
            addSelector<DisplayMode>(
                "§7Display Mode: ",
                getName = { type -> type.displayName },
                isCurrent = { it == currentDisplayMode },
                onChange = {
                    currentDisplayMode = it
                    saveAndUpdate()
                }
            )
        }
        val both = currentLog() ?: return@buildList
        val chest = both.get(currentDisplayMode)
        val rewards = chest.rewards

        addAsSingletonList("§d${chest.totalChestPicked.addSeparators()} Total Chests Picked")
        addAsSingletonList("§bDouble Powder: ${if (doublePowder) "§aActive!" else "§cInactive!"}")

        for (reward in PowderChestReward.entries.subList(0, 2)) {
            val count = rewards.getOrDefault(reward, 0).addSeparators()
            addAsSingletonList("§b$count ${reward.displayName}")
        }
        addAsSingletonList("")

        var ruby = rewards.getOrDefault(PowderChestReward.ROUGH_RUBY_GEMSTONE, 0)
        ruby += rewards.getOrDefault(PowderChestReward.FLAWED_RUBY_GEMSTONE, 0) * 80
        ruby += rewards.getOrDefault(PowderChestReward.FINE_RUBY_GEMSTONE, 0) * 6400
        val (rubyFine, rubyFlawed, rubyRough) = convert(ruby)
        addAsSingletonList("§9$rubyFine§f-§a$rubyFlawed§f-$rubyRough §cRuby Gemstone")

        var sapphire = rewards.getOrDefault(PowderChestReward.ROUGH_SAPPHIRE_GEMSTONE, 0)
        sapphire += rewards.getOrDefault(PowderChestReward.FLAWED_SAPPHIRE_GEMSTONE, 0) * 80
        sapphire += rewards.getOrDefault(PowderChestReward.FINE_SAPPHIRE_GEMSTONE, 0) * 6400
        val (sapphireFine, sapphireFlawed, sapphireRough) = convert(sapphire)
        addAsSingletonList("§9$sapphireFine§f-§a$sapphireFlawed§f-$sapphireRough §bSapphire Gemstone")

        var amber = rewards.getOrDefault(PowderChestReward.ROUGH_AMBER_GEMSTONE, 0)
        amber += rewards.getOrDefault(PowderChestReward.FLAWED_AMBER_GEMSTONE, 0) * 80
        amber += rewards.getOrDefault(PowderChestReward.FINE_AMBER_GEMSTONE, 0) * 6400
        val (amberFine, amberFlawed, amberRough) = convert(amber)
        addAsSingletonList("§9$amberFine§7-§a$amberFlawed§f-$amberRough §6Amber Gemstone")

        var amethyst = rewards.getOrDefault(PowderChestReward.ROUGH_AMETHYST_GEMSTONE, 0)
        amethyst += rewards.getOrDefault(PowderChestReward.FLAWED_AMETHYST_GEMSTONE, 0) * 80
        amethyst += rewards.getOrDefault(PowderChestReward.FINE_AMETHYST_GEMSTONE, 0) * 6400
        val (amethystFine, amethystFlawed, amethystRough) = convert(amethyst)
        addAsSingletonList("§9$amethystFine§7-§a$amethystFlawed§f-$amethystRough §5Amethyst Gemstone")

        var jade = rewards.getOrDefault(PowderChestReward.ROUGH_JADE_GEMSTONE, 0)
        jade += rewards.getOrDefault(PowderChestReward.FLAWED_JADE_GEMSTONE, 0) * 80
        jade += rewards.getOrDefault(PowderChestReward.FINE_JADE_GEMSTONE, 0) * 6400
        val (jadeFine, jadeFlawed, jadeRough) = convert(jade)
        addAsSingletonList("§9$jadeFine§7-§a$jadeFlawed§f-$jadeRough §aJade Gemstone")

        var topaz = rewards.getOrDefault(PowderChestReward.ROUGH_TOPAZ_GEMSTONE, 0)
        topaz += rewards.getOrDefault(PowderChestReward.FLAWED_TOPAZ_GEMSTONE, 0) * 80
        topaz += rewards.getOrDefault(PowderChestReward.FINE_TOPAZ_GEMSTONE, 0) * 6400
        val (topazFine, topazFlawed, topazRough) = convert(topaz)
        addAsSingletonList("§9$topazFine§7-§a$topazFlawed§f-$topazRough §eTopaz Gemstone")

        for (reward in PowderChestReward.entries.subList(20, 26)) {
            val count = rewards.getOrDefault(reward, 0).addSeparators()
            addAsSingletonList("§b$count ${reward.displayName}")
        }

        val goblinEgg = rewards.getOrDefault(PowderChestReward.GOBLIN_EGG, 0)
        val greenEgg = rewards.getOrDefault(PowderChestReward.GREEN_GOBLIN_EGG, 0)
        val redEgg = rewards.getOrDefault(PowderChestReward.RED_GOBLIN_EGG, 0)
        val yellowEgg = rewards.getOrDefault(PowderChestReward.YELLOW_GOBLIN_EGG, 0)
        val blueEgg = rewards.getOrDefault(PowderChestReward.BLUE_GOBLIN_EGG, 0)
        addAsSingletonList("§9$goblinEgg§7-§a$greenEgg§7-§c$redEgg§f-§e$yellowEgg§f-§3$blueEgg §fGoblin Egg")

        for (reward in PowderChestReward.entries.subList(31, 40)) {
            val count = rewards.getOrDefault(reward, 0).addSeparators()
            addAsSingletonList("§b$count ${reward.displayName}")
        }
    }

    private fun convert(roughCount: Long): Triple<Long, Long, Long> {
        val flawedRatio = 80
        val fineRatio = 6400

        val fineCount = roughCount / fineRatio
        val flawedCount = roughCount % fineRatio / flawedRatio
        val remainingRoughCount = roughCount % fineRatio % flawedRatio

        return Triple(fineCount, flawedCount, remainingRoughCount)
    }


    enum class DisplayMode(val displayName: String) {
        TOTAL("Total"),
        CURRENT("This Session"),
        ;
    }


    private fun currentLog(): AbstractPowderTracker? {
        val profileSpecific = ProfileStorageData.profileSpecific ?: return null

        return AbstractPowderTracker(
            profileSpecific.powderTracker.getOrPut(0) { Storage.ProfileSpecific.PowderTracker() },
            currentSessionData.getOrPut(0) { Storage.ProfileSpecific.PowderTracker() }
        )
    }

    class AbstractPowderTracker(
        private val total: Storage.ProfileSpecific.PowderTracker,
        private val currentSession: Storage.ProfileSpecific.PowderTracker,
    ) {

        fun modify(modifyFunction: (Storage.ProfileSpecific.PowderTracker) -> Unit) {
            modifyFunction(total)
            modifyFunction(currentSession)
        }

        fun get(displayMode: DisplayMode) = when (displayMode) {
            DisplayMode.TOTAL -> total
            DisplayMode.CURRENT -> currentSession
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.CRYSTAL_HOLLOWS


    /*
    delete all of this before making pr
     */

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!isEnabled()) return
        getTilesNearby<TileEntityChest>(LocationUtils.playerLocation(), 20.0).forEach {
            event.drawWaypointFilled(it.pos.toLorenzVec(), Color.GREEN, seeThroughBlocks = true)
        }
    }

    private inline fun <reified T : TileEntity> getTilesNearby(location: LorenzVec, radius: Double): Sequence<T> =
        getTiles<T>().filter { it.pos.toLorenzVec().distanceToPlayer() <= radius }

    private inline fun <reified R : TileEntity> getTiles(): Sequence<R> = getAllTiles().filterIsInstance<R>()
    private fun getAllTiles(): Sequence<TileEntity> = Minecraft.getMinecraft()?.theWorld?.loadedTileEntityList?.let {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread) it else it.toMutableList()
    }?.asSequence() ?: emptySequence()
}