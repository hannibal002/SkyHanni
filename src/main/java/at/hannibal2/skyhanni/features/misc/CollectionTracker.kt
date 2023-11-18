package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.Collections

class CollectionTracker {

    private val RECENT_GAIN_TIME = 1_500

    companion object {

        private var display = emptyList<List<Any>>()

        private var itemName = ""
        private var internalName: NEUInternalName? = null
        private var itemAmount = -1L

        private var lastAmountInInventory = -1

        private var recentGain = 0
        private var lastGainTime = -1L

        fun command(args: Array<String>) {
            if (args.isEmpty()) {
                if (internalName == null) {
                    LorenzUtils.userError("/shtrackcollection <item name>")
                    return
                }
                LorenzUtils.chat("Stopped collection tracker.")
                resetData()
                return
            }

            val rawName = fixTypo(args.joinToString(" ").lowercase().replace("_", " "))
            if (rawName == "gemstone") {
                LorenzUtils.userError("Gemstone collection is not supported!")
                return
            } else if (rawName == "mushroom") {
                LorenzUtils.userError("Mushroom collection is not supported!")
                return
            }

            val foundInternalName = NEUItems.getInternalNameOrNullIgnoreCase(rawName)
            if (foundInternalName == null) {
                LorenzUtils.error("Item '$rawName' does not exist!")
                return
            }

            val stack = foundInternalName.getItemStackOrNull()
            if (stack == null) {
                LorenzUtils.error("Item '$rawName' does not exist!")
                return
            }
            setNewCollection(foundInternalName, stack.name!!.removeColor())
        }

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

        private fun setNewCollection(internalName: NEUInternalName, name: String) {
            val foundAmount = CollectionAPI.getCollectionCounter(internalName)
            if (foundAmount == null) {
                LorenzUtils.userError("$name collection not found. Try to open the collection inventory!")
                return
            }
            this.internalName = internalName
            itemName = name
            itemAmount = foundAmount

            lastAmountInInventory = countCurrentlyInInventory()
            updateDisplay()
            LorenzUtils.chat("Started tracking $itemName §ecollection.")
        }

        private fun resetData() {
            itemAmount = -1
            internalName = null

            lastAmountInInventory = -1
            display = emptyList()

            recentGain = 0
        }

        private fun updateDisplay() {
            val format = LorenzUtils.formatInteger(itemAmount)

            var gainText = ""
            if (recentGain != 0) {
                gainText = "§a+" + LorenzUtils.formatInteger(recentGain)
            }

            display = Collections.singletonList(buildList {
                internalName?.let {
                    add(it.getItemStack())
                }
                add("$itemName collection: §e$format $gainText")
            })
        }

        private fun countCurrentlyInInventory(): Int {
            val cactus = "CACTUS".asInternalName()
            val cactusGreen = "INK_SACK-2".asInternalName()
            return InventoryUtils.countItemsInLowerInventory {
                if (internalName == cactus && it.getInternalName() == cactusGreen) {
                    return@countItemsInLowerInventory true
                }
                it.getInternalName() == internalName
            }
        }

        fun handleTabComplete(command: String): List<String>? {
            if (command != "shtrackcollection") return null

            return CollectionAPI.collectionValue.keys.mapNotNull { it.getItemStackOrNull() }
                .map { it.displayName.removeColor().replace(" ", "_") }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        val thePlayer = Minecraft.getMinecraft().thePlayer ?: return
        thePlayer.worldObj ?: return

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

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return

        SkyHanniMod.feature.misc.collectionCounterPos.renderStringsAndItems(display, posLabel = "Collection Tracker")
    }
}
