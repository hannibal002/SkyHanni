package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class CollectionCounter {

    private val RECENT_GAIN_TIME = 1_500

    companion object {

        private var display = ""

        private var itemName = ""
        private var internalName = ""
        private var itemAmount = -1L

        private var lastAmountInInventory = -1

        private var recentGain = 0
        private var lastGainTime = -1L

        fun command(args: Array<String>) {
            if (args.isEmpty()) {
                if (internalName == "") {
                    LorenzUtils.chat("§c/shtrackcollection <item name>")
                    return
                }
                LorenzUtils.chat("§e[SkyHanni] Stopped collection tracker.")
                resetData()
                return
            }

            val name = args.joinToString(" ").replace(" ", "_")
            val pair = CollectionAPI.getCollectionCounter(name)
            if (pair == null) {
                LorenzUtils.chat("§c[SkyHanni] Item $name is not in the collection data! (Maybe the API is disabled or try to open the collection inventory)")
                return
            }

            internalName = pair.first
            if (internalName.contains("MUSHROOM") || internalName.endsWith("_GEM")) {
                LorenzUtils.chat("§7Mushroom and Gemstone items are not fully supported for the counter!")
                internalName = ""
                return
            }
            itemName = NEUItems.getItemStack(internalName).name!!
            itemAmount = pair.second

            lastAmountInInventory = countCurrentlyInInventory()
            updateDisplay()
            LorenzUtils.chat("§e[SkyHanni] Started tracking $itemName collection.")
        }

        private fun resetData() {
            itemAmount = -1
            internalName = ""

            lastAmountInInventory = -1
            display = ""

            recentGain = 0
        }

        private fun updateDisplay() {
            val format = LorenzUtils.formatInteger(itemAmount)

            var gainText = ""
            if (recentGain != 0) {
                gainText = "§a+" + LorenzUtils.formatInteger(recentGain)
            }

            display = "$itemName collection: §e$format $gainText"
        }

        private fun countCurrentlyInInventory() =
            InventoryUtils.countItemsInLowerInventory { it.getInternalName() == internalName }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
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
        if (diff != 0) {
            if (diff > 0) {
                gainItems(diff)
            }
        }

        lastAmountInInventory = currentlyInInventory
    }

    private fun updateGain() {
        if (recentGain != 0) {
            if (System.currentTimeMillis() > lastGainTime + RECENT_GAIN_TIME) {
                recentGain = 0
                updateDisplay()
            }
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
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return

        SkyHanniMod.feature.misc.collectionCounterPos.renderString(display, posLabel = "Collection Counter")
    }
}