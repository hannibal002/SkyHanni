package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest

import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.FactionType
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailykuudra.KuudraTier
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.DojoQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.FetchQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.KuudraQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.MiniBossQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.ProgressQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.Quest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.QuestCategory
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.QuestState
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.RescueMissionQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.TrophyFishQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.UnknownQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss.CrimsonMiniBoss
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DailyQuestHelper(val reputationHelper: CrimsonIsleReputationHelper) {

    private val townBoardMage = LorenzVec(-138, 92, -755)
    private val townBoardBarbarian = LorenzVec(-572, 100, -687)

    private val questLoader = QuestLoader(this)
    val quests = mutableListOf<Quest>()
    private val sacksCache = mutableMapOf<String, Long>()
    var greatSpook = false

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return

        questLoader.checkInventory(event)
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!isEnabled()) return

        questLoader.loadFromTabList()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        if (event.repeatSeconds(3)) {
            checkInventoryForFetchItem()
        }
    }

    fun update() {
        reputationHelper.update()
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return

        if (event.gui !is GuiChest) return
        val chest = event.gui.inventorySlots as ContainerChest
        val chestName = chest.getInventoryName()

        if (chestName == "Challenges") {
            if (LorenzUtils.skyBlockArea != "Dojo") return
            val dojoQuest = getQuest<DojoQuest>() ?: return
            if (dojoQuest.state != QuestState.ACCEPTED) return

            for (slot in chest.inventorySlots) {
                if (slot == null) continue
                if (slot.slotNumber != slot.slotIndex) continue
                val stack = slot.stack ?: continue
                val itemName = stack.name ?: continue

                if (itemName.contains(dojoQuest.dojoName)) {
                    slot highlight LorenzColor.AQUA
                }
            }
        }
        if (chestName == "Sack of Sacks") {
            val fetchQuest = getQuest<FetchQuest>() ?: return
            if (fetchQuest.state != QuestState.ACCEPTED) return

            val fetchItem = fetchQuest.itemName
            for (slot in chest.inventorySlots) {
                if (slot == null) continue
                if (slot.slotNumber != slot.slotIndex) continue
                val stack = slot.stack ?: continue
                if (stack.name!!.contains("Enchanted")) continue

                if (stack.getLore().any { it.contains(fetchItem) }) {
                    slot highlight LorenzColor.AQUA
                }
            }
        }
        if (chestName.contains("Nether Sack")) {
            val fetchQuest = getQuest<FetchQuest>() ?: return
            if (fetchQuest.state != QuestState.ACCEPTED) return

            val fetchItem = fetchQuest.itemName
            for (slot in chest.inventorySlots) {
                if (slot == null) continue
                if (slot.slotNumber != slot.slotIndex) continue
                val stack = slot.stack ?: continue
                val itemName = stack.name ?: continue

                if (itemName.contains(fetchItem)) {
                    slot highlight LorenzColor.AQUA
                }
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        val message = event.message
        if (message == "§aYou completed your Dojo quest! Visit the Town Board to claim the rewards.") {
            val dojoQuest = getQuest<DojoQuest>() ?: return
            dojoQuest.state = QuestState.READY_TO_COLLECT
            update()
        }
        if (message == "§aYou completed your rescue quest! Visit the Town Board to claim the rewards,") {
            val rescueMissionQuest = getQuest<RescueMissionQuest>() ?: return
            rescueMissionQuest.state = QuestState.READY_TO_COLLECT
            update()
        }

        if (message.contains("§6§lTROPHY FISH! §r§bYou caught a")) {
            val fishQuest = getQuest<TrophyFishQuest>() ?: return
            if (fishQuest.state != QuestState.ACCEPTED && fishQuest.state != QuestState.READY_TO_COLLECT) return
            val fishName = fishQuest.fishName

            if (message.contains(fishName)) {
                updateProcessQuest(fishQuest, fishQuest.haveAmount + 1)
            }
        }
    }

    inline fun <reified T : Quest> getQuest() = quests.filterIsInstance<T>().firstOrNull()

    private fun checkInventoryForFetchItem() {
        val fetchQuest = getQuest<FetchQuest>() ?: return
        if (fetchQuest.state != QuestState.ACCEPTED && fetchQuest.state != QuestState.READY_TO_COLLECT) return

        val itemName = fetchQuest.itemName

        val count = InventoryUtils.countItemsInLowerInventory { it.name?.contains(itemName) ?: false }
        updateProcessQuest(fetchQuest, count)
    }

    private fun updateProcessQuest(quest: ProgressQuest, newAmount: Int) {
        var count = newAmount
        val needAmount = quest.needAmount
        if (count > needAmount) {
            count = needAmount
        }
        if (quest.haveAmount == count) return
        LorenzUtils.chat("§e[SkyHanni] ${quest.displayName} progress: $count/$needAmount")

        quest.haveAmount = count
        quest.state = if (count == needAmount) QuestState.READY_TO_COLLECT else QuestState.ACCEPTED
        update()
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (!reputationHelper.showLocations()) return

        for (quest in quests) {
            if (quest is MiniBossQuest) continue
            if (quest.state != QuestState.ACCEPTED) continue
            val location = quest.location ?: continue

            event.drawWaypointFilled(location, LorenzColor.WHITE.toColor())
            event.drawDynamicText(location, quest.displayName, 1.5)
        }

        renderTownBoard(event)
    }

    private fun renderTownBoard(event: LorenzRenderWorldEvent) {
        if (quests.any { it.state == QuestState.READY_TO_COLLECT || it.state == QuestState.NOT_ACCEPTED }) {
            val location = when (reputationHelper.factionType) {
                FactionType.BARBARIAN -> townBoardBarbarian
                FactionType.MAGE -> townBoardMage

                FactionType.NONE -> return
            }
            event.drawWaypointFilled(location, LorenzColor.WHITE.toColor())
            event.drawDynamicText(location, "Town Board", 1.5)
        }
    }

    fun render(display: MutableList<List<Any>>) {
        if (greatSpook) {
            display.addAsSingletonList("")
            display.addAsSingletonList("§7Daily Quests (§cdisabled§7)")
            display.addAsSingletonList(" §5§lThe Great Spook §7happened :O")
            return
        }
        val done = quests.count { it.state == QuestState.COLLECTED }
        display.addAsSingletonList("")
        display.addAsSingletonList("§7Daily Quests (§e$done§8/§e5 collected§7)")
        if (done != 5) {
            quests.mapTo(display) { renderQuest(it) }
        }
    }

    private fun renderQuest(quest: Quest): List<Any> {
        val category = quest.category
        val state = quest.state.displayName
        val stateColor = quest.state.color

        val progressText = if (quest is ProgressQuest && quest.state != QuestState.COLLECTED) {
            val haveAmount = quest.haveAmount
            val needAmount = quest.needAmount
            " §e$haveAmount§8/§e$needAmount"
        } else {
            ""
        }

        val sacksText = if (quest is FetchQuest && quest.state != QuestState.COLLECTED) {
            val name = quest.itemName.uppercase().replace(" ", "_")
            val amount = sacksCache.getOrDefault(name, 0)
            val needAmount = quest.needAmount
            val amountFormat = LorenzUtils.formatInteger(amount)
            val color = if (amount >= needAmount) {
                "§a"
            } else {
                "§c"
            }
            " §7($color$amountFormat §7in sacks)"
        } else {
            ""
        }

        val stateText = if (quest !is UnknownQuest) {
            "$stateColor[$state] §f"
        } else {
            ""
        }

        val result = mutableListOf<Any>()
        val internalItemName = quest.displayItem

        val displayName = if (category == QuestCategory.FETCH || category == QuestCategory.FISHING) {
            if (internalItemName != null) {
                val name = NEUItems.getItemStack(internalItemName).name
                if (category == QuestCategory.FISHING) {
                    name!!.split(" ").dropLast(1).joinToString(" ")
                } else name

            } else {
                quest.displayName
            }
        } else quest.displayName

        val categoryName = category.displayName
        if (internalItemName == null) {
            result.add("  $stateText$categoryName: §f$displayName$progressText$sacksText")
        } else {
            result.add("  $stateText$categoryName: ")
            try {
                result.add(NEUItems.getItemStack(internalItemName))
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
            result.add("§f$displayName$progressText$sacksText")
        }
        return result
    }

    fun finishMiniBoss(miniBoss: CrimsonMiniBoss) {
        val miniBossQuest = getQuest<MiniBossQuest>() ?: return
        if (miniBossQuest.miniBoss == miniBoss && miniBossQuest.state == QuestState.ACCEPTED) {
            updateProcessQuest(miniBossQuest, miniBossQuest.haveAmount + 1)
        }
    }

    fun finishKuudra(kuudraTier: KuudraTier) {
        val kuudraQuest = getQuest<KuudraQuest>() ?: return
        //TODO make inline method for this two lines
        if (kuudraQuest.kuudraTier == kuudraTier && kuudraQuest.state == QuestState.ACCEPTED) {
            kuudraQuest.state = QuestState.READY_TO_COLLECT
        }
    }

    fun reset() {
        quests.clear()
    }

    fun load(storage: Storage.ProfileSpecific.CrimsonIsleStorage) {
        reset()
        questLoader.loadConfig(storage)
    }

    fun saveConfig(storage: Storage.ProfileSpecific.CrimsonIsleStorage) {
        storage.quests.clear()
        for (quest in quests) {
            val builder = StringBuilder()
            val internalName = quest.internalName
            builder.append(internalName)
            builder.append(":")
            val state = quest.state
            builder.append(state)

            if (quest is ProgressQuest) {
                val need = quest.needAmount
                val have = quest.haveAmount

                builder.append(":")
                builder.append(need)
                builder.append(":")
                builder.append(have)
            } else {
                builder.append(":0")
            }
            storage.quests.add(builder.toString())
        }
    }

    private fun isEnabled() = IslandType.CRIMSON_ISLE.isInIsland() && reputationHelper.config.enabled
}
