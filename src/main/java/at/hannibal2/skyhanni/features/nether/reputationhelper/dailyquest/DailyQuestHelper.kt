package at.hannibal2.hanni.features.nether.reputationhelper.dailyquest

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.storage.ProfileSpecificStorage
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.SackApi.getAmountInSacksOrNull
import at.hannibal2.hanni.data.model.TabWidget
import at.hannibal2.hanni.events.ConfigLoadEvent
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.WidgetUpdateEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.fishing.TrophyFishCaughtEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.features.nether.kuudra.KuudraTier
import at.hannibal2.hanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.hanni.features.nether.reputationhelper.FactionType
import at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest.DojoQuest
import at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest.FetchQuest
import at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest.KuudraQuest
import at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest.MiniBossQuest
import at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest.ProgressQuest
import at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest.Quest
import at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest.QuestCategory
import at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest.QuestState
import at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest.RescueMissionQuest
import at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest.TrophyFishQuest
import at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest.UnknownQuest
import at.hannibal2.hanni.features.nether.reputationhelper.miniboss.CrimsonMiniBoss
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ConditionalUtils
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NeuItems.getItemStack
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RenderUtils.highlight
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.StringUtils.removeWordsAtEnd
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import kotlin.time.Duration.Companion.seconds

@HanniModule
object DailyQuestHelper {

    private val questBoardMage = LorenzVec(-138, 92, -755)
    private val questBoardBarbarian = LorenzVec(-572, 100, -687)

    val quests = mutableListOf<Quest>()
    var greatSpook = false

    val patternGroup = RepoPattern.group("crimson.reputationhelper.quest")

    /**
     * REGEX-TEST: §7Kill the §cAshfang §7miniboss §a2 §7times!
     * REGEX-TEST: §7Kill the §cMage Outlaw §7miniboss §a1 §7time!
     * REGEX-TEST: §7miniboss §a1 §7time!
     * REGEX-TEST: §7Kill the §cBarbarian Duke X §7miniboss §a2
     */
    val minibossAmountPattern by patternGroup.pattern(
        "townboard.minibossamount",
        "(?:§7Kill the §c.+ §7|.*)miniboss §a(?<amount>\\d)(?: §7times?!)?",
    )

    /**
     * REGEX-TEST: §a§lCOMPLETE
     */
    val townBoardCompletedPattern by patternGroup.pattern(
        "townboard.completed",
        "(?:§.)*COMPLETE",
    )

    /**
     * REGEX-TEST: §aYou completed your Dojo quest! Visit the Town Board to claim the rewards.
     * REGEX-TEST: §aYou completed your rescue quest! Visit the Town Board to claim the rewards,
     *   (yes, that is a comma at the end)
     */
    val chatCompletedPattern by patternGroup.pattern(
        "chat.completed",
        "§aYou completed your (?<type>\\w+) quest! Visit the Town Board to claim the rewards.*",
    )

    private val config get() = HanniMod.feature.crimsonIsle.reputationHelper

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return

        QuestLoader.checkInventory(event)
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.enabled) {
            if (IslandType.CRIMSON_ISLE.isCurrent()) {
                QuestLoader.loadFromTabList()
            }
        }
    }

    @HandleEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.FACTION_QUESTS)) return
        if (!isEnabled()) return

        QuestLoader.loadFromTabList()
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        if (event.repeatSeconds(3)) {
            checkInventoryForFetchItem()
        }
    }

    fun update() {
        CrimsonIsleReputationHelper.update()
    }

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return

        if (event.gui !is GuiChest) return
        val chest = event.container as ContainerChest
        val chestName = InventoryUtils.openInventoryName()

        if (chestName == "Challenges") {
            if (SkyBlockUtils.graphArea != "Dojo") return
            val dojoQuest = getQuest<DojoQuest>() ?: return
            if (dojoQuest.state != QuestState.ACCEPTED) return

            for ((slot, stack) in chest.getUpperItems()) {
                if (stack.displayName.contains(dojoQuest.dojoName)) {
                    slot.highlight(LorenzColor.AQUA)
                }
            }
        }
    }

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return

        val type = chatCompletedPattern.matchMatcher(event.message) {
            group("type").lowercase()
        } ?: return
        when (type) {
            "dojo" -> {
                val dojoQuest = getQuest<DojoQuest>() ?: return
                dojoQuest.state = QuestState.READY_TO_COLLECT
                update()
            }

            "rescue" -> {
                val rescueMissionQuest = getQuest<RescueMissionQuest>() ?: return
                rescueMissionQuest.state = QuestState.READY_TO_COLLECT
                update()
            }

            else -> ChatUtils.debug("Unhandled quest completion type: $type")
        }
    }

    @HandleEvent
    fun onTrophyFishCaught(event: TrophyFishCaughtEvent) {
        val fishQuest = getQuest<TrophyFishQuest>() ?: return
        if (fishQuest.state != QuestState.ACCEPTED && fishQuest.state != QuestState.READY_TO_COLLECT) return
        val fishName = fishQuest.fishName

        if (event.trophyFishName == fishName) {
            updateProcessQuest(fishQuest, fishQuest.haveAmount + 1)
        }
    }

    inline fun <reified T : Quest> getQuest() = quests.filterIsInstance<T>().firstOrNull()

    private fun checkInventoryForFetchItem() {
        val fetchQuest = getQuest<FetchQuest>() ?: return
        if (fetchQuest.state != QuestState.ACCEPTED && fetchQuest.state != QuestState.READY_TO_COLLECT) return

        val itemName = fetchQuest.itemName

        val count = InventoryUtils.countItemsInLowerInventory { it.displayName.removeColor() == itemName }
        updateProcessQuest(fetchQuest, count)
    }

    private fun updateProcessQuest(quest: ProgressQuest, newAmount: Int) {
        var count = newAmount
        val needAmount = quest.needAmount
        if (count > needAmount) {
            count = needAmount
        }
        if (quest.haveAmount == count) return
        ChatUtils.chat("${quest.displayName} progress: $count/$needAmount")

        quest.haveAmount = count
        quest.state = if (count == needAmount) QuestState.READY_TO_COLLECT else QuestState.ACCEPTED
        update()
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (!CrimsonIsleReputationHelper.showLocations()) return

        for (quest in quests) {
            if (quest is MiniBossQuest) continue
            if (quest.state != QuestState.ACCEPTED) continue
            val location = quest.location ?: continue

            event.drawWaypointFilled(location, LorenzColor.WHITE.toColor())
            event.drawDynamicText(location, quest.displayName, 1.5)
        }

        renderTownBoard(event)
    }

    fun getQuestBoardLocation(): LorenzVec {
        val factionType = CrimsonIsleReputationHelper.factionType ?: ErrorManager.hanniError("faction type is unknown")
        return when (factionType) {
            FactionType.BARBARIAN -> questBoardBarbarian
            FactionType.MAGE -> questBoardMage
        }
    }

    private fun renderTownBoard(event: HanniRenderWorldEvent) {
        if (!quests.any { it.needsTownBoardLocation() }) return

        // we do not call getQuestBoardLocation in the first few seconds when faction type is null, since this will show an error
        if (CrimsonIsleReputationHelper.factionType == null && SkyBlockUtils.lastWorldSwitch.passedSince() < 5.seconds) return
        val location = getQuestBoardLocation()
        event.drawWaypointFilled(location, LorenzColor.WHITE.toColor())
        event.drawDynamicText(location, "Town Board", 1.5)
    }

    private fun Quest.needsTownBoardLocation() =
        state == QuestState.READY_TO_COLLECT ||
            (state == QuestState.ACCEPTED && (this is FetchQuest || this is RescueMissionQuest))

    fun MutableList<Renderable>.addQuests() {
        if (greatSpook) {
            addString("")
            addString("§7Daily Quests (§cdisabled§7)")
            addString(" §5§lThe Great Spook §7happened :O")
            return
        }
        val done = quests.count { it.state == QuestState.COLLECTED }
        addString("")
        addString("§7Daily Quests (§e$done§8/§e5 collected§7)")
        if (done != 5) {
            val filteredQuests = quests.filter { !config.hideComplete.get() || it.state != QuestState.COLLECTED }
            addAll(filteredQuests.map { renderQuest(it) })
        }
    }

    private fun renderQuest(quest: Quest): Renderable {
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
            quest.displayItem.getAmountInSacksOrNull()?.let {
                val color = if (it >= quest.needAmount) {
                    "§a"
                } else {
                    "§c"
                }
                " §7($color${it.addSeparators()} §7in sacks)"
            } ?: " §7(§eSack data outdated/missing§7)"
        } else {
            ""
        }

        val stateText = if (quest !is UnknownQuest && quest.state != QuestState.ACCEPTED) {
            "$stateColor[$state] §f"
        } else {
            ""
        }

        val item = quest.displayItem.getItemStack()

        val displayName = if (category == QuestCategory.FETCH || category == QuestCategory.FISHING) {
            val name = item.displayName
            if (category == QuestCategory.FISHING) {
                name.removeWordsAtEnd(1)
            } else name
        } else quest.displayName

        val categoryName = category.displayName

        return Renderable.horizontal {
            addString("  $stateText$categoryName: ")
            addItemStack(item)
            addString("§f$displayName$progressText$sacksText")
        }
    }

    fun finishMiniBoss(miniBoss: CrimsonMiniBoss) {
        val miniBossQuest = getQuest<MiniBossQuest>() ?: return
        if (miniBossQuest.miniBoss == miniBoss && miniBossQuest.state == QuestState.ACCEPTED) {
            updateProcessQuest(miniBossQuest, miniBossQuest.haveAmount + 1)
            if (miniBossQuest.haveAmount == 1) {
                fixMiniBossByTabWidget(miniBossQuest)
            }
        }
    }

    private fun fixMiniBossByTabWidget(oldQuest: MiniBossQuest) {
        oldQuest.state = QuestState.ACCEPTED
        DelayedRun.runDelayed(5.seconds) {
            if (oldQuest.state == QuestState.ACCEPTED) {
                ChatUtils.debug(
                    "Daily Minibosss Quest is still not ready to accept even though we have one miniboss kill," +
                        "we now assume there are two to kill.",
                )
                val newQuest = MiniBossQuest(oldQuest.miniBoss, oldQuest.state, 2)
                newQuest.haveAmount = oldQuest.haveAmount
                DelayedRun.runNextTick {
                    quests.remove(oldQuest)
                    quests.add(newQuest)
                    ChatUtils.chat("Fixed wrong miniboss amount from Tab Widget.")
                    update()
                }
            } else {
                oldQuest.state = QuestState.READY_TO_COLLECT
            }
            CrimsonIsleReputationHelper.update()
        }
    }

    fun finishKuudra(kuudraTier: KuudraTier) {
        val kuudraQuest = getQuest<KuudraQuest>() ?: return
        // TODO make inline method for this two lines
        if (kuudraQuest.kuudraTier == kuudraTier && kuudraQuest.state == QuestState.ACCEPTED) {
            kuudraQuest.state = QuestState.READY_TO_COLLECT
        }
    }

    fun reset() {
        quests.clear()
    }

    fun load(storage: ProfileSpecificStorage.CrimsonIsleStorage) {
        reset()
        QuestLoader.loadConfig(storage)
    }

    fun saveConfig(storage: ProfileSpecificStorage.CrimsonIsleStorage) {
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

    private fun isEnabled() = IslandType.CRIMSON_ISLE.isCurrent() && config.enabled.get()
}
