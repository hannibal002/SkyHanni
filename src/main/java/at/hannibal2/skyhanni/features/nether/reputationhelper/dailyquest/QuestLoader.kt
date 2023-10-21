package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest

import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.DojoQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.FetchQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.KuudraQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.MiniBossQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.ProgressQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.Quest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.QuestState
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.RescueMissionQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.TrophyFishQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.UnknownQuest
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.jsonobjects.CrimsonIsleReputationJson.ReputationQuest

class QuestLoader(private val dailyQuestHelper: DailyQuestHelper) {

    companion object {
        val quests = mutableMapOf<String, Pair<String, ReputationQuest>>()
        fun loadQuests(data: Map<String, ReputationQuest>, questType: String) {
            for ((questName, questInfo) in data) {
                quests[questName] = Pair(questType, questInfo)
            }
        }
    }

    fun loadFromTabList() {
        var i = -1
        for (line in TabListData.getTabList()) {
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
        // TODO use regex
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
            if (green && oldQuest.state != QuestState.READY_TO_COLLECT && oldQuest.state != QuestState.COLLECTED) {
                oldQuest.state = QuestState.READY_TO_COLLECT
                dailyQuestHelper.update()
                LorenzUtils.debug("Reputation Helper: Tab-List updated ${oldQuest.internalName} (This should not happen)")
            }
            return
        }

        val state = if (green) QuestState.READY_TO_COLLECT else QuestState.NOT_ACCEPTED
        dailyQuestHelper.update()
        addQuest(addQuest(name, state, needAmount))
    }

    private fun addQuest(name: String, state: QuestState, needAmount: Int): Quest {
        for (miniBoss in dailyQuestHelper.reputationHelper.miniBossHelper.miniBosses) {
            if (name == miniBoss.displayName) {
                return MiniBossQuest(miniBoss, state, needAmount)
            }
        }
        for (kuudraTier in dailyQuestHelper.reputationHelper.kuudraBossHelper.kuudraTiers) {
            val kuudraName = kuudraTier.name
            if (name == "Kill Kuudra $kuudraName Tier") {
                return KuudraQuest(kuudraTier, state)
            }
        }
        var questName = name
        var dojoGoal = ""

        if (name.contains(" Rank ")) {
            val split = name.split(" Rank ")
            questName = split[0]
            dojoGoal = split[1]
        }

        if (questName in quests) {
            val questInfo = quests[questName] ?: return UnknownQuest(name)
            val locationInfo = questInfo.second.location
            val location = dailyQuestHelper.reputationHelper.readLocationData(locationInfo)
            val displayItem = questInfo.second.item

            when (questInfo.first) {
                "FISHING" -> return TrophyFishQuest(name, location, displayItem, state, needAmount)
                "RESCUE" -> return RescueMissionQuest(displayItem, location, state)
                "FETCH" -> return FetchQuest(name, location, displayItem, state, needAmount)
                "DOJO" -> return DojoQuest(questName, location, displayItem, dojoGoal, state)
            }
        }
        LorenzUtils.chat("§c[SkyHanni] Unknown Crimson Isle quest: '$name'")
        return UnknownQuest(name)
    }

    private fun getQuestByName(name: String): Quest? {
        return dailyQuestHelper.quests.firstOrNull { it.internalName == name }
    }

    fun checkInventory(event: InventoryFullyOpenedEvent) {
        val inMageRegion = LorenzUtils.skyBlockArea == "Community Center"
        val inBarbarianRegion = LorenzUtils.skyBlockArea == "Dragontail"
        if (!inMageRegion && !inBarbarianRegion) return

        val name = event.inventoryName
        for (quest in dailyQuestHelper.quests) {
            val categoryName = quest.category.name
            if (!categoryName.equals(name, ignoreCase = true)) continue
            val stack = event.inventoryItems[22] ?: continue

            val completed = stack.getLore().any { it.contains("Completed!") }
            if (completed && quest.state != QuestState.COLLECTED) {
                quest.state = QuestState.COLLECTED
                dailyQuestHelper.update()
            }

            val accepted = !stack.getLore().any { it.contains("Click to start!") }
            if (accepted && quest.state == QuestState.NOT_ACCEPTED) {
                quest.state = QuestState.ACCEPTED
                dailyQuestHelper.update()
            }
        }
    }

    fun loadConfig(storage: Storage.ProfileSpecific.CrimsonIsleStorage) {
        for (text in storage.quests.toList()) {
            val split = text.split(":")
            val name = split[0]
            val state = QuestState.valueOf(split[1])
            val needAmount = split[2].toInt()
            val quest = addQuest(name, state, needAmount)
            if (quest is ProgressQuest && split.size == 4) {
                try {
                    val haveAmount = split[3].toInt()
                    quest.haveAmount = haveAmount
                } catch (e: IndexOutOfBoundsException) {
                    println("text: '$text'")
                    e.printStackTrace()
                }
            }
            addQuest(quest)
        }
    }

    private fun addQuest(element: Quest) {
        dailyQuestHelper.quests.add(element)
        if (dailyQuestHelper.quests.size > 5) {
            dailyQuestHelper.reputationHelper.reset()
        }
    }
}
