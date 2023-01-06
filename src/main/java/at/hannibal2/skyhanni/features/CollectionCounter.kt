package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.bazaar.BazaarData
import at.hannibal2.skyhanni.test.GriffinJavaUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class CollectionCounter {

    companion object {

        private var textToRender = ""
        private var itemName = ""
        private var itemApiName = ""
        private var itemAmount = -1

        private var lastAmountInInventory = -1

        private val apiCollectionData = mutableMapOf<String, Int>()

        fun command(args: Array<String>) {
            if (args.isEmpty()) {
                if (itemName == "") {
                    LorenzUtils.chat("§c/shtrackcollection <item name>")
                    return
                }
                LorenzUtils.chat("§e[SkyHanni] Disabled collection tracking for $itemName")
                apiCollectionData[itemApiName] = itemAmount
                resetData()
                return
            }

            var name = args.joinToString(" ")

            var data: BazaarData? = null
            for (bazaarData in BazaarApi.bazaarMap.values) {
                if (bazaarData.itemName.equals(name, ignoreCase = true)) {
                    data = bazaarData
                    break
                }
            }

            if (data == null) {
                LorenzUtils.error("Item '$name' not found!")
                return
            }
            name = data.itemName

            val apiName = data.apiName
            if (!apiCollectionData.contains(apiName)) {
                LorenzUtils.error("Item '$name' not in collection data!")
                return
            }

            if (itemAmount != -1) {
                resetData()
            }

            itemName = name
            itemApiName = apiName
            itemAmount = apiCollectionData[apiName]!!

            lastAmountInInventory = countCurrentlyInInventory()
            update()
            LorenzUtils.chat("§e[SkyHanni] Enabled collection tracking for $itemName")
        }

        private fun resetData() {
            itemAmount = -1
            itemName = ""
            itemApiName = ""

            lastAmountInInventory = -1
            textToRender = ""
        }

        private fun update() {
            val format = GriffinJavaUtils.formatInteger(itemAmount)
            textToRender = "$itemName collection: $format"
        }

        private fun countCurrentlyInInventory(): Int {
            var currentlyInInventory = 0
            val player = Minecraft.getMinecraft().thePlayer
            for (stack in player.inventory.mainInventory) {
                if (stack == null) continue
                val internalName = stack.getInternalName()
                if (internalName == itemApiName) {
                    currentlyInInventory += stack.stackSize
                }
            }
            return currentlyInInventory
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        val thePlayer = Minecraft.getMinecraft().thePlayer ?: return
        thePlayer.worldObj ?: return

        if (lastAmountInInventory == -1) return

        if (Minecraft.getMinecraft().currentScreen != null) return

        val currentlyInInventory = countCurrentlyInInventory()
        val diff = currentlyInInventory - lastAmountInInventory
        if (diff != 0) {
            if (diff > 0) {
                itemAmount += diff
                update()
            } else {
                LorenzUtils.debug("Collection counter! Negative collection change: $diff")
            }
        }

        lastAmountInInventory = currentlyInInventory
    }

    @SubscribeEvent
    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
        val profileData = event.profileData
        val collection = profileData["collection"].asJsonObject

        apiCollectionData.clear()
        for (entry in collection.entrySet()) {
            val name = entry.key
            val value = entry.value.asInt
            apiCollectionData[name] = value
            if (name == itemApiName) {
                val diff = value - itemAmount
                if (diff != 0) {
                    LorenzUtils.debug("Collection counter was wrong by $diff items. (Compared against API data)")
                }
                itemAmount = value
                update()
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!LorenzUtils.inSkyblock) return

        SkyHanniMod.feature.misc.collectionCounterPos.renderString(textToRender)
    }
}