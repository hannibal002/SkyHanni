package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest

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
//        println(" ")
//        println("#####")
//        println(" ")

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

//        println("line: '$line'")
//        println("green: '$green'")
//        println("name: '$name'")
//        println("amount: '$amount'")
//        println(" ")
    }

    private fun checkQuest(name: String, green: Boolean, amount: Int) {
        val oldQuest = getQuestByName(name)
        if (oldQuest != null) {
            if (green) {
                if (oldQuest.state != QuestState.READY_TO_COLLECT && oldQuest.state != QuestState.COLLECTED) {
                    oldQuest.state = QuestState.READY_TO_COLLECT
                    dailyQuestHelper.update()
                    LorenzUtils.debug("Tablist updated ${oldQuest.internalName} (This should not happen)")
                }
            }
            return
        }

        val state = if (green) QuestState.READY_TO_COLLECT else QuestState.NOT_ACCEPTED
        dailyQuestHelper.update()
        dailyQuestHelper.quests.add(addQuest(name, state, amount))
    }

    private fun addQuest(
        name: String,
        state: QuestState,
        amount: Int
    ): Quest {

        //TODO add repo

        //Trophy Fish
        if (name == "Lavahorse") return TrophyFishQuest(name, state, amount)
        if (name == "Gusher") return TrophyFishQuest(name, state, amount)
        if (name == "Volcanic Stonefish") return TrophyFishQuest(name, state, amount)

        //Rescue Mission
        if (name == "Rescue Mission") return RescueMissionQuest(state)

        //Boss
        if (name == "Magma Boss") return BossQuest(name, state, amount)
        if (name == "Mage Outlaw") return BossQuest(name, state, amount)
        if (name == "Barbarian Duke X") return BossQuest(name, state, amount)

        //Fetch
        if (name == "Magmag") return FetchQuest(name, state, amount)
        if (name == "Spectre Dust") return FetchQuest(name, state, amount)
        if (name == "Tentacle Meat") return FetchQuest(name, state, amount)

        if (name.startsWith("Mastery Rank ") || name.startsWith("Tenacity Rank ") || name.startsWith("Stamina Rank ")) {
            val split = name.split(" Rank ")
            val dojoName = split[0]
            val dojoRankGoal = split[1]
            return DojoQuest(dojoName, dojoRankGoal, state)
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
}