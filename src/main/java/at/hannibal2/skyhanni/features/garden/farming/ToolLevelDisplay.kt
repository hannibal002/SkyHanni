package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.UserLuckCalculateEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.achievements.AchievementManager
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getToolExp
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getToolLevel
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.bold
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

@SkyHanniModule
object ToolLevelDisplay {

    private const val OVERCLOCK_THRESHOLD = 40
    private const val MAX_LEVEL = 50

    private var toolLevels: List<Int>? = null
    private var toolOverflow = 200000
    private var display: List<Renderable>? = null
    private val gardenStorage get() = GardenApi.storage
    private val config get() = SkyHanniMod.feature.garden.toolLevelDisplay

    private val patternGroup = RepoPattern.group("tool.levels")

    /**
     * REGEX-TEST: OVERFLOW! Your Turing Sugar Cane Hoe Mk. III has just dropped a Tool Exp Capsule!
     */
    val levelUpPattern by patternGroup.pattern(
        "levelup-nocolor",
        "OVERFLOW! Your (?<tool>.+) has just dropped a Tool Exp Capsule!",
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick() {
        if (!isEnabled()) return
        display = getDisplay()?.map(Renderable::text)
    }

    private fun getDisplay(): List<String>? = buildList {
        add("§6Tool Levels")
        val heldItem = InventoryUtils.getItemInHand() ?: return null
        val toolExp = heldItem.getToolExp() ?: return null
        var toolLevel = heldItem.getToolLevel() ?: return null
        val toolLevels = toolLevels ?: return null
        val next = if (toolLevel <= toolLevels.size) toolLevels[toolLevel - 1] else toolOverflow

        if (toolLevel > toolLevels.size && config.overflow) {
            val uuid = heldItem.getItemUuid()
            val overflowLevel = getOverflowToolLevel(uuid)
            if (overflowLevel != null) {
                toolLevel += overflowLevel
            }
        }
        add("§7Level §8$toolLevel➜§3${toolLevel + 1}")

        var colorPrefix = "§e"
        if (toolExp > next) {
            colorPrefix = "§c§l"
            if (toolLevel >= OVERCLOCK_THRESHOLD) add("§3§lOVERCLOCK REQUIRED!")
            else add("§c§lUPGRADE REQUIRED!")
        }
        val formattedXp = toolExp.addSeparators()
        val formattedXpToNext = next.addSeparators()
        add("$colorPrefix$formattedXp§8/§e$formattedXpToNext")

        GardenApi.lastBrokenCropType?.takeIf { it != GardenApi.cropInHand }?.let {
            add("§cNot gaining XP! (Wrong crop)")
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent.Modify) {
        val heldItem = InventoryUtils.getItemInHand() ?: return
        val leveledUpTool = levelUpPattern.matchMatcher(event.cleanMessage) {
            group("tool")
        } ?: return
        val heldItemName = heldItem.hoverName.string.removeColor()
        if (!heldItemName.contains(leveledUpTool)) return
        val overflowLevel = addOverflowToolLevel(heldItem.getItemUuid())
        if (isEnabled() && config.overflow && overflowLevel != null) {
            val currentLevel = heldItem.getToolLevel() ?: return
            val newComponent = event.chatComponent.copy().append(" §8(§3Level ${currentLevel + overflowLevel}§8)")
            event.replaceComponent(newComponent, "tool_level")
        }
    }

    private fun getOverflowToolLevel(uuid: String?): Int? {
        uuid ?: return null
        val storage = gardenStorage?.overflowToolLevels ?: return null
        if (storage.contains(uuid)) {
            return storage[uuid]
        } else {
            storage[uuid] = 0
            return 0
        }
    }

    private fun addOverflowToolLevel(uuid: String?): Int? {
        uuid ?: return null
        val storage = gardenStorage?.overflowToolLevels ?: return null
        val currentLevel = getOverflowToolLevel(uuid) ?: return null
        val newLevel = currentLevel + 1
        storage[uuid] = newLevel
        if (newLevel >= 1000) AchievementManager.completeAchievement(HOE_ACHIEVEMENT)
        return newLevel
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnIsland = IslandType.GARDEN)
    fun onRender() {
        if (!isEnabled()) return
        val renderable = display ?: return
        config.position.renderRenderables(renderable, posLabel = "Tool Level Display")
    }

    @HandleEvent
    fun onUserLuck(event: UserLuckCalculateEvent) {
        if (!config.overflow) return
        val luck = calculateLuck()
        if (luck < 1) return
        event.addLuck(luck)
        val stack = ItemUtils.createItemStack(
            Items.NETHERITE_HOE,
            Component.literal("✴ Overflow Tool Levels").withColor(ChatFormatting.GREEN),
            listOf(
                Component.literal("Items").withColor(ChatFormatting.DARK_GRAY),
                Component.empty(),
                componentBuilder {
                    appendWithColor("Value: ", ChatFormatting.GRAY)
                    appendWithColor("$luck✴", ChatFormatting.GREEN)
                },
                Component.empty(),
                Component.literal("Gain more by leveling up your farming tools!").withColor(ChatFormatting.DARK_GRAY),
            ),
        )
        event.addItem(stack)
    }

    private fun calculateLuck(): Float {
        val map = gardenStorage?.overflowToolLevels ?: return 0f
        var luck = 0f
        for (entry in map) {
            luck += entry.value / 10
        }
        return luck
    }

    private fun errorNoTool() {
        ChatUtils.userError("You must hold a specialized farming tool to use this command!")
    }

    private fun errorStorage(item: ItemStack): Nothing {
        ErrorManager.skyHanniError(
            "Error getting overflow tool level storage",
            "item" to item,
        )
    }

    private const val HOE_ACHIEVEMENT = "Hoe Level 1000"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Hoe Expert".asComponent(),
            componentBuilder {
                append("Get a hoe to level 1000. ") {
                    withColor(ChatFormatting.YELLOW)
                }
                append("This requires you to get ") {
                    withColor(ChatFormatting.GREEN)
                }
                append("524,322,000 ") {
                    withColor(ChatFormatting.AQUA)
                    bold = true
                }
                append("XP!") {
                    withColor(ChatFormatting.GREEN)
                }
            },
            30f,
        )
        event.register(achievement, HOE_ACHIEVEMENT)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shsettoollevel") {
            category = CommandCategory.USERS_BUG_FIX
            description = "Manually sets your overflow tool level"
            aliases = listOf("shsethoelevel", "shsetaxelevel")
            arg("level", BrigadierArguments.integer()) { newLevelArg ->
                callback {
                    val item = InventoryUtils.getItemInHand() ?: return@callback errorNoTool()
                    val uuid = item.getItemUuid() ?: return@callback errorNoTool()

                    val realLevel = item.getToolLevel() ?: return@callback errorNoTool()
                    if (realLevel < MAX_LEVEL) {
                        ChatUtils.userError("Tools below level $MAX_LEVEL cannot have overflow levels!")
                        return@callback
                    }

                    val oldLevel = MAX_LEVEL + (getOverflowToolLevel(uuid) ?: errorStorage(item))

                    val newLevel = getArg(newLevelArg)
                    if (newLevel < MAX_LEVEL) {
                        ChatUtils.userError("Overflow level cannot be below $MAX_LEVEL!")
                        return@callback
                    }
                    val storage = gardenStorage?.overflowToolLevels ?: errorStorage(item)
                    storage[uuid] = newLevel - MAX_LEVEL
                    ChatUtils.chat(
                        componentBuilder {
                            append("Updated overflow level for ")
                            append(item.hoverName)
                            append(" from ")
                            appendWithColor(oldLevel.toString(), ChatFormatting.AQUA)
                            append(" to ")
                            appendWithColor(newLevel.toString(), ChatFormatting.AQUA)
                            append(".")
                        },
                    )
                }
            }
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<GardenJson>("Garden")
        toolLevels = data.toolExpLevels
        toolOverflow = data.toolExpOverflow
    }

    fun isEnabled() = config.enabled

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(130, "features.garden.hoeLevelDisplay", "features.garden.toolLevelDisplay")
    }
}
