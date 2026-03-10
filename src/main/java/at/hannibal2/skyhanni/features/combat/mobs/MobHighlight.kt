package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.dynamicSuggestionProvider
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.combat.mobs.MobHighlight.onMobSpawn
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.getBlockInHand
import at.hannibal2.skyhanni.utils.EntityUtils.isCorrupted
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.level.block.Blocks

@SkyHanniModule
object MobHighlight {

    private val config get() = SkyHanniMod.feature.combat.mobs
    private var arachne: Mob? = null

    /** Custom highlights added via command: mob name → LorenzColor */
    private val customHighlights = mutableMapOf<String, LorenzColor>()

    /**
     * Apply a custom highlight to all already-spawned mobs matching [mobName].
     * Also used on spawn via [onMobSpawn].
     */
    private fun applyCustomHighlightToExisting(mobName: String, color: LorenzColor) {
        MobData.skyblockMobs
            .filter { it.name == mobName }
            .forEach { mob -> mob.highlight(color.toColor()) { customHighlights.containsKey(mobName) } }
    }

    /**
     * Remove any custom highlight from all already-spawned mobs matching [mobName].
     */
    private fun removeCustomHighlightFromExisting(mobName: String) {
        MobData.skyblockMobs
            .filter { it.name == mobName }
            .forEach { mob -> mob.removeHighlight() }
    }

    private val colorNames: List<String> = LorenzColor.entries
        .filter { it != LorenzColor.CHROMA }
        .map { it.name.lowercase().replace('_', '-') }

    private fun parseColor(input: String): LorenzColor? =
        LorenzColor.entries.firstOrNull { it.name.equals(input.replace('-', '_'), ignoreCase = true) }

    private fun getMobNameSuggestions(): Collection<String> =
        MobData.skyblockMobs.map { it.name }.filter { it.isNotBlank() }.distinct().sorted()

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        val name = mob.name

        // Apply custom highlights first (they take precedence)
        val customColor = customHighlights[name]
        if (customColor != null) {
            mob.highlight(customColor.toColor()) { customHighlights.containsKey(name) }
            return
        }

        val (color, isEnabled) = when {
            name == "Boss Corleone" ->
                LorenzColor.DARK_PURPLE to config::corleoneHighlighter

            name == "Arachne's Keeper" ->
                LorenzColor.DARK_BLUE to config::arachneKeeperHighlight

            name == "Arachne's Brood" ->
                LorenzColor.GOLD to config::arachneBossHighlighter

            name == "Arachne" -> {
                arachne = mob
                LorenzColor.RED to config::arachneBossHighlighter
            }

            mob.isRunic ->
                LorenzColor.LIGHT_PURPLE to config::runicMobHighlight

            else -> return
        }

        mob.highlight(color.toColor()) { isEnabled() }
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (arachne == event.mob) arachne = null
    }

    // TODO: change to use nametags instead
    // as this method does not work for mobs that spawn corrupted naturally
    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!config.corruptedMobHighlight) return

        val entity = event.entity
        if (!entity.isCorrupted()) return

        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
            entity,
            LorenzColor.DARK_PURPLE.toColor().addAlpha(127),
        ) { config.corruptedMobHighlight }
    }

    // Mob detection isn't used here to allow for highlighting Zealots from further away.
    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (event.entity !is EnderMan) return

        val entity = event.entity

        val heldBlock = entity.getBlockInHand()?.block

        val (color, alpha, isEnabled) = when {
            heldBlock == Blocks.END_PORTAL_FRAME ->
                Triple(LorenzColor.DARK_RED, 50, config::specialZealotHighlighter)

            heldBlock == Blocks.ENDER_CHEST ->
                Triple(LorenzColor.GREEN, 127, config::chestZealotHighlighter)

            entity.isZealotOrBruiser() ->
                Triple(LorenzColor.DARK_AQUA, 127, config::zealotBruiserHighlighter)
            else -> return
        }

        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
            entity,
            color.toColor().addAlpha(alpha),
        ) { isEnabled() }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.lineToArachne) return

        val arachne = arachne ?: return

        if (!arachne.canBeSeen(10)) return

        event.drawLineToEye(
            arachne.centerCords,
            LorenzColor.RED.toChromaColor(),
            config.lineToArachneWidth,
            true,
        )
    }

    private fun LivingEntity.isZealotOrBruiser() = baseMaxHealth == 13_000 || baseMaxHealth == 65_000 ||
        baseMaxHealth == 13_000 * 4 || baseMaxHealth == 65_000 * 4 // runic


    @HandleEvent
    fun registerCommand(event: CommandRegistrationEvent) {
        event.registerBrigadier("shmobhighlight") {
            description = "Highlight specific mobs by name with a custom color"
            category = CommandCategory.USERS_ACTIVE

            // /shmobhighlight add <mob> [color]
            literal("add") {
                arg("mob", BrigadierArguments.string(), dynamicSuggestionProvider(::getMobNameSuggestions)) { mobArg ->
                    // with optional color argument
                    arg("color", BrigadierArguments.string(), colorNames) { colorArg ->
                        callback {
                            val mobName = getArg(mobArg)
                            val colorInput = getArg(colorArg)
                            val color = parseColor(colorInput)
                            if (color == null) {
                                ChatUtils.userError("Unknown color '$colorInput'. Valid colors: ${colorNames.joinToString()}")
                                return@callback
                            }
                            customHighlights[mobName] = color
                            applyCustomHighlightToExisting(mobName, color)
                            ChatUtils.chat("Now highlighting §a$mobName§7 with color ${color.getChatColor()}${color.name.lowercase().replace('_', '-')}§7.")
                        }
                    }
                    // without color – defaults to GREEN
                    callback {
                        val mobName = getArg(mobArg)
                        customHighlights[mobName] = LorenzColor.GREEN
                        applyCustomHighlightToExisting(mobName, LorenzColor.GREEN)
                        ChatUtils.chat("Now highlighting §a$mobName§7 with default color §agreen§7.")
                    }
                }
                simpleCallback { ChatUtils.userError("Usage: /shmobhighlight add <mob> [color]") }
            }

            // /shmobhighlight remove <mob>
            literal("remove") {
                arg("mob", BrigadierArguments.string(), dynamicSuggestionProvider { customHighlights.keys.sorted() }) { mobArg ->
                    callback {
                        val mobName = getArg(mobArg)
                        if (customHighlights.remove(mobName) != null) {
                            removeCustomHighlightFromExisting(mobName)
                            ChatUtils.chat("Removed highlight for §a$mobName§7.")
                        } else {
                            ChatUtils.userError("No custom highlight found for '$mobName'.")
                        }
                    }
                }
                simpleCallback { ChatUtils.userError("Usage: /shmobhighlight remove <mob>") }
            }

            // /shmobhighlight list
            literal("list") {
                simpleCallback {
                    if (customHighlights.isEmpty()) {
                        ChatUtils.chat("No custom mob highlights configured.")
                    } else {
                        ChatUtils.chat("Custom mob highlights:")
                        customHighlights.entries.sortedBy { it.key }.forEach { (name, color) ->
                            ChatUtils.chat("  ${color.getChatColor()}$name §7→ ${color.name.lowercase().replace('_', '-')}", prefix = false)
                        }
                    }
                }
            }

            // /shmobhighlight clear
            literal("clear") {
                simpleCallback {
                    val count = customHighlights.size
                    val names = customHighlights.keys.toSet()
                    customHighlights.clear()
                    names.forEach { removeCustomHighlightFromExisting(it) }
                    ChatUtils.chat("Cleared $count custom mob highlight(s).")
                }
            }

            simpleCallback {
                ChatUtils.userError("Usage: /shmobhighlight <add|remove|list|clear>")
            }
        }
    }
}
