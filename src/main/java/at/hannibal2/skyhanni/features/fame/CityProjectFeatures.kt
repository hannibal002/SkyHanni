package at.hannibal2.skyhanni.features.fame

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CityProjectFeatures {

    private var display = emptyList<List<Any>>()
    private var inInventory = false
    private var lastReminderSend = 0L

    private val patternGroup = RepoPattern.group("fame.projects")
    private val contributeAgainPattern by patternGroup.pattern(
        "contribute",
        "§7Contribute again: §e(?<time>.*)"
    )
    private val completedPattern by patternGroup.pattern(
        "completed",
        "§aProject is (?:being built|released)!"
    )

    companion object {

        private val config get() = SkyHanniMod.feature.event.cityProject
        fun disable() {
            config.dailyReminder = false
            ChatUtils.chat("Disabled city project reminder messages!")
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.repeatSeconds(1)) {
            checkDailyReminder()
        }
    }

    private fun checkDailyReminder() {
        if (!config.dailyReminder) return
        val playerSpecific = ProfileStorageData.playerSpecific ?: return
        if (ReminderUtils.isBusy()) return

        if (LorenzUtils.skyBlockArea == "Community Center") return

        if (playerSpecific.nextCityProjectParticipationTime == 0L) return
        if (System.currentTimeMillis() <= playerSpecific.nextCityProjectParticipationTime) return

        if (lastReminderSend + 30_000 > System.currentTimeMillis()) return
        lastReminderSend = System.currentTimeMillis()

        ChatUtils.clickableChat(
            "Daily City Project Reminder! (Click here to disable this reminder)",
            "shstopcityprojectreminder"
        )
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return

        inInventory = false
        if (!inCityProject(event)) return
        inInventory = true

        if (config.showMaterials) {
            // internal name -> amount
            val materials = mutableMapOf<String, Int>()
            for ((_, item) in event.inventoryItems) {
                val itemName = item.name ?: continue
                if (itemName != "§eContribute this component!") continue
                fetchMaterials(item, materials)
            }

            display = buildList(materials)
        }

        if (config.showReady) {
            var nextTime = Long.MAX_VALUE
            for ((_, item) in event.inventoryItems) {
                val itemName = item.name ?: continue

                val lore = item.getLore()
                val completed = lore.lastOrNull()?.let { completedPattern.matches(it) } ?: false
                if (completed) continue
                for (line in lore) {
                    contributeAgainPattern.matchMatcher(line) {
                        val rawTime = group("time")
                        if (rawTime.contains("Soon!")) return@matchMatcher
                        val duration = TimeUtils.getMillis(rawTime)
                        val endTime = System.currentTimeMillis() + duration
                        if (endTime < nextTime) {
                            nextTime = endTime
                        }
                    }
                }
                if (itemName != "§eContribute this component!") continue
                nextTime = System.currentTimeMillis()
            }
            ProfileStorageData.playerSpecific?.nextCityProjectParticipationTime = nextTime
        }
    }

    private fun inCityProject(event: InventoryFullyOpenedEvent): Boolean {
        val lore = event.inventoryItems[4]?.getLore() ?: return false
        if (lore.isEmpty()) return false
        if (lore[0] != "§8City Project") return false
        return true
    }

    private fun buildList(materials: MutableMap<String, Int>) = buildList<List<Any>> {
        addAsSingletonList("§7City Project Materials")

        if (materials.isEmpty()) {
            addAsSingletonList("§cNo Materials to contribute.")
            return@buildList
        }

        for ((internalName, amount) in materials) {
            val stack = NEUItems.getItemStack(internalName)
            val name = stack.name ?: continue
            val list = mutableListOf<Any>()
            list.add(" §7- ")
            list.add(stack)

            list.add(Renderable.optionalLink("$name §ex${amount.addSeparators()}", {
                if (Minecraft.getMinecraft().currentScreen is GuiEditSign) {
                    LorenzUtils.setTextIntoSign("$amount")
                } else {
                    BazaarApi.searchForBazaarItem(name, amount)
                }
            }) { inInventory && !NEUItems.neuHasFocus() })

            val price = NEUItems.getPrice(internalName) * amount
            val format = NumberUtil.format(price)
            list.add(" §7(§6$format§7)")
            add(list)
        }
    }

    private fun fetchMaterials(item: ItemStack, materials: MutableMap<String, Int>) {
        var next = false
        val lore = item.getLore()
        val completed = lore.lastOrNull()?.let { completedPattern.matches(it) } ?: false
        if (completed) return
        for (line in lore) {
            if (line == "§7Cost") {
                next = true
                continue
            }
            if (!next) continue
            if (line == "") break
            if (line.contains("Bits")) break

            val (name, amount) = ItemUtils.readItemAmount(line) ?: continue
            val internalName = NEUItems.getRawInternalName(name)
            val old = materials.getOrPut(internalName) { 0 }
            materials[internalName] = old + amount
        }
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.showMaterials) return
        if (!inInventory) return

        config.pos.renderStringsAndItems(display, posLabel = "City Project Materials")
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.showReady) return
        if (!inInventory) return


        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest

        for (slot in chest.inventorySlots) {
            if (slot == null) continue
            if (slot.slotNumber != slot.slotIndex) continue
            val stack = slot.stack ?: continue
            val lore = stack.getLore()
            if (lore.isEmpty()) continue
            val last = lore.last()
            if (last == "§eClick to contribute!") {
                slot highlight LorenzColor.YELLOW
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.cityProject", "event.cityProject")
    }
}
