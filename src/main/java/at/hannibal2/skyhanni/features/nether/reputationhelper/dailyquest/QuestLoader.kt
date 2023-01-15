package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.*
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.TabListUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest

class QuestLoader(val dailyQuestHelper: DailyQuestHelper) {

    fun loadFromTabList() {
        var i = -1
        for (line in TabListUtils.getTabList()) {
            if (line.contains("Faction Quests:")) {
                i = 0
                continue
            }
            if (i == -1) continue

            i++
            readQuest(line)
            if (i == 5) {
                break
            }
        }
    }

    private fun readQuest(line: String) {
        var text = line.substring(3)
        val green = text.startsWith("§a")
        text = text.substring(2)

        val amount: Int
        val name: String
        if (text.contains(" §r§8x")) {
            val split = text.split(" §r§8x")
            name = split[0]
            amount = split[1].toInt()
        } else {
            name = text
            amount = 1
        }

        checkQuest(name, green, amount)
    }

    private fun checkQuest(name: String, green: Boolean, needAmount: Int) {
        val oldQuest = getQuestByName(name)
        if (oldQuest != null) {
            if (green) {
                if (oldQuest.state != QuestState.READY_TO_COLLECT && oldQuest.state != QuestState.COLLECTED) {
                    oldQuest.state = QuestState.READY_TO_COLLECT
                    dailyQuestHelper.update()
                    LorenzUtils.debug("Reputation Helper: Tab-List updated ${oldQuest.internalName} (This should not happen)")
                }
            }
            return
        }

        val state = if (green) QuestState.READY_TO_COLLECT else QuestState.NOT_ACCEPTED
        dailyQuestHelper.update()
        dailyQuestHelper.quests.add(addQuest(name, state, needAmount))
    }

    private fun addQuest(
        name: String,
        state: QuestState,
        needAmount: Int
    ): Quest {
        for (miniBoss in dailyQuestHelper.reputationHelper.miniBossHelper.miniBosses) {
            if (name == miniBoss.displayName) {
                return MiniBossQuest(miniBoss, state, needAmount)
            }
        }

        for (entry in dailyQuestHelper.reputationHelper.repoData.entrySet()) {
            val category = entry.key

            for (element in entry.value.asJsonArray) {
                val entryName = element.asString

                if (name.startsWith("$entryName Rank ")) {
                    val split = name.split(" Rank ")
                    val dojoName = split[0]
                    val dojoRankGoal = split[1]
                    return DojoQuest(dojoName, dojoRankGoal, state)
                }

                if (name == entryName) {
                    when (category) {
                        "FISHING" -> return TrophyFishQuest(name, state, needAmount)
                        "RESCUE" -> return RescueMissionQuest(state)
                        "FETCH" -> return FetchQuest(name, state, needAmount)
                    }
                }
            }
        }

        println("Unknown quest: '$name'")
        return UnknownQuest(name)
    }

    private fun getQuestByName(name: String): Quest? {
        return dailyQuestHelper.quests.firstOrNull { it.internalName == name }
    }

    fun checkInventory() {
        if (LorenzUtils.skyBlockArea != "Community Center") return

        val gui = Minecraft.getMinecraft().currentScreen
        if (gui !is GuiChest) return
        val chest = gui.inventorySlots as ContainerChest
        val name = chest.getInventoryName()

        for (quest in dailyQuestHelper.quests) {
            val categoryName = quest.category.name
            if (categoryName.equals(name, ignoreCase = true)) {
                for (slot in chest.inventorySlots) {
                    if (slot == null) continue
                    if (slot.slotNumber != slot.slotIndex) continue

                    // Only checking the middle slot
                    if (slot.slotNumber != 22) continue

                    val stack = slot.stack ?: continue

                    val completed = stack.getLore().any { it.contains("Completed!") }
                    if (completed) {
                        if (quest.state != QuestState.COLLECTED) {
                            quest.state = QuestState.COLLECTED
                            dailyQuestHelper.update()
                        }
                    }

                    val accepted = !stack.getLore().any { it.contains("Click to start!") }
                    if (accepted) {
                        if (quest.state == QuestState.NOT_ACCEPTED) {
                            quest.state = QuestState.ACCEPTED
                            dailyQuestHelper.update()
                        }
                    }
                }
            }
        }
    }

    fun loadConfig() {
        for (text in SkyHanniMod.feature.hidden.crimsonIsleQuests) {
            val split = text.split(":")
            val name = split[0]
            val state = QuestState.valueOf(split[1])
            val needAmount = split[2].toInt()
            val quest = addQuest(name, state, needAmount)
            if (quest is ProgressQuest) {
                val haveAmount = split[3].toInt()
                quest.haveAmount = haveAmount
            }
            dailyQuestHelper.quests.add(quest)
        }
    }
}