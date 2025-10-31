package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.CollectionApi
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.NeuItems.getItemStack
import at.hannibal2.hanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.formatLong
import at.hannibal2.hanni.utils.NumberUtil.isFormatNumber
import at.hannibal2.hanni.utils.NumberUtil.percentWithColorCode
import at.hannibal2.hanni.utils.RenderUtils.renderRenderable
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import net.minecraft.client.Minecraft

@HanniModule
object CollectionTracker {

    private const val RECENT_GAIN_TIME = 1_500

    private var display: Renderable? = null

    private var itemName = ""
    private var internalName: NeuInternalName? = null
    private var itemAmount = -1L
    private var goalAmount = -1L

    private var lastAmountInInventory = -1

    private var recentGain = 0
    private var lastGainTime = -1L

    private val CACTUS = "CACTUS".toInternalName()
    private val CACTUS_GREEN = "INK_SACK-2".toInternalName()
    private val YOUNGITE = "YOUNGITE".toInternalName()
    private val OBSOLITE = "OBSOLITE".toInternalName()
    private val TIMITE = "TIMITE".toInternalName()

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtrackcollection") {
            description = "Tracks your collection gain over time"
            category = CommandCategory.USERS_ACTIVE
            arg(
                "args", BrigadierArguments.greedyString(),
            ) { args ->
                callback { command(getArg(args).split(" ").toTypedArray()) }
            }
            simpleCallback {
                if (internalName == null) {
                    ChatUtils.userError("/shtrackcollection <item name> [goal amount]")
                } else {
                    ChatUtils.chat("Stopped collection tracker.")
                    resetData()
                }
            }
        }
    }

    private fun command(args: Array<String>) {
        val lastArg = args.last()

        val nameArgs = if (lastArg.isFormatNumber()) {
            val goal = lastArg.formatLong()
            if (goal <= 0) {
                ChatUtils.chat("Invalid Amount for Goal.")
                return
            }
            goalAmount = goal
            args.dropLast(1).toTypedArray()
        } else {
            goalAmount = -1L
            args
        }

        val rawName = fixTypo(nameArgs.joinToString(" ").lowercase().replace("_", " "))
        if (rawName == "gemstone") {
            ChatUtils.userError("Gemstone collection is not supported!")
            return
        } else if (rawName == "mushroom") {
            ChatUtils.userError("Mushroom collection is not supported!")
            return
        }

        val foundInternalName = NeuInternalName.fromItemNameOrNull(rawName)
        if (foundInternalName == null) {
            ChatUtils.userError("Item '$rawName' does not exist!")
            return
        }

        val stack = foundInternalName.getItemStackOrNull()
        if (stack == null) {
            ChatUtils.userError("Item '$rawName' does not exist!")
            return
        }
        setNewCollection(foundInternalName, stack.displayName.removeColor())
    }

    // TODO repo
    private fun fixTypo(rawName: String) = when (rawName) {
        "carrots" -> "carrot"
        "melons" -> "melon"
        "seed" -> "seeds"
        "iron" -> "iron ingot"
        "gold" -> "gold ingot"
        "sugar" -> "sugar cane"
        "cocoa bean", "cocoa" -> "cocoa beans"
        "lapis" -> "lapis lazuli"
        "cacti" -> "cactus"
        "pumpkins" -> "pumpkin"
        "potatoes" -> "potato"
        "nether warts", "wart", "warts" -> "nether wart"
        "stone" -> "cobblestone"
        "red mushroom", "brown mushroom", "mushrooms" -> "mushroom"
        "gemstones" -> "gemstone"
        "caducous" -> "caducous stem"
        "agaricus" -> "agaricus cap"
        "quartz" -> "nether quartz"
        "glowstone" -> "glowstone dust"

        else -> rawName
    }

    private fun setNewCollection(internalName: NeuInternalName, name: String) {
        val foundAmount = CollectionApi.getCollectionCounter(internalName)
        if (foundAmount == null) {
            ChatUtils.userError("$name collection not found. Try to open the collection inventory!")
            return
        }
        this.internalName = internalName
        itemName = name
        itemAmount = foundAmount

        lastAmountInInventory = countCurrentlyInInventory()
        updateDisplay()
        ChatUtils.chat("Started tracking $itemName §ecollection.")
    }

    private fun resetData() {
        itemAmount = -1L
        goalAmount = -1L
        internalName = null

        lastAmountInInventory = -1
        display = null

        recentGain = 0
    }

    private fun updateDisplay() {
        val format = itemAmount.addSeparators()

        var gainText = ""
        if (recentGain != 0) {
            gainText = "§a+" + recentGain.addSeparators()
        }

        if (goalAmount != -1L && itemAmount >= goalAmount) {
            ChatUtils.chat("Collection goal of §a${goalAmount.addSeparators()} reached!")
            goalAmount = -1L
        }

        val goal = if (goalAmount == -1L) "" else " §f/ §b${goalAmount.addSeparators()} §f(§a${
            itemAmount.percentWithColorCode(goalAmount, 1)
        }§f)"

        display = Renderable.horizontal {
            internalName?.let {
                addItemStack(it.getItemStack())
            }
            addString("$itemName collection: §e$format$goal $gainText")
        }
    }

    private fun countCurrentlyInInventory(): Int = InventoryUtils.countItemsInLowerInventory {
        val name = it.getInternalName()
        if (internalName == CACTUS && name == CACTUS_GREEN) {
            return@countItemsInLowerInventory true
        }
        if (internalName == TIMITE && (name == YOUNGITE || name == OBSOLITE)) {
            return@countItemsInLowerInventory true
        }
        name == internalName
    }

    fun handleTabComplete(command: String): List<String>? {
        if (command != "shtrackcollection") return null

        return CollectionApi.collectionValue.keys.mapNotNull { it.getItemStackOrNull() }
            .map { it.displayName.removeColor().replace(" ", "_") }
    }

    @HandleEvent
    fun onTick() {
        compareInventory()
        updateGain()
    }

    private fun compareInventory() {
        if (lastAmountInInventory == -1) return
        if (Minecraft.getMinecraft().currentScreen != null) return

        val currentlyInInventory = countCurrentlyInInventory()
        val diff = currentlyInInventory - lastAmountInInventory
        if (diff != 0 && diff > 0) {
            gainItems(diff)
        }

        lastAmountInInventory = currentlyInInventory
    }

    private fun updateGain() {
        if (recentGain != 0 && System.currentTimeMillis() > lastGainTime + RECENT_GAIN_TIME) {
            recentGain = 0
            updateDisplay()
        }
    }

    private fun gainItems(amount: Int) {
        itemAmount += amount

        if (System.currentTimeMillis() > lastGainTime + RECENT_GAIN_TIME) {
            recentGain = 0
        }
        lastGainTime = System.currentTimeMillis()
        recentGain += amount

        updateDisplay()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        display?.let {
            HanniMod.feature.misc.collectionCounterPos.renderRenderable(it, posLabel = "Collection Tracker")
        }
    }
}
