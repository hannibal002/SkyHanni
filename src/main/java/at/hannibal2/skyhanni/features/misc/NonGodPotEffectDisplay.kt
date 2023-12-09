package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.Timer
import net.minecraft.network.play.server.S47PacketPlayerListHeaderFooter
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class NonGodPotEffectDisplay {
    private val config get() = SkyHanniMod.feature.misc.potionEffect
    private var checkFooter = false
    private val effectDuration = mutableMapOf<NonGodPotEffect, Timer>()
    private var display = emptyList<String>()

    // TODO move the whole list into the repo
    enum class NonGodPotEffect(
        val tabListName: String,
        val isMixin: Boolean = false,
        val inventoryItemName: String = tabListName
    ) {
        SMOLDERING("§aSmoldering Polarization I"),
        GLOWY("§2Mushed Glowy Tonic I"),
        WISP("§bWisp's Ice-Flavored Water I"),
        GOBLIN("§2King's Scent I"),

        INVISIBILITY("§8Invisibility I"), // when wearing sorrow armor

        REV("§cZombie Brain Mixin", true),
        TARA("§6Spider Egg Mixin", true),
        SVEN("§bWolf Fur Mixin", true),
        VOID("§6Ender Portal Fumes", true),
        BLAZE("§fGabagoey", true),

        DEEP_TERROR("§4Deepterror", true),

        GREAT_SPOOK("§fGreat Spook I", inventoryItemName = "§fGreat Spook Potion"),
        ;
    }

    // TODO USE SH-REPO
    private var patternEffectsCount = "§7You have §e(?<name>\\d+) §7non-god effects\\.".toPattern()
    private var totalEffectsCount = 0

    // todo : cleanup and add support for poison candy I, and add support for splash / other formats
    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (event.message == "§aYou cleared all of your active effects!") {
            effectDuration.clear()
            update()
        }

        if (event.message == "§aYou ate a §r§aRe-heated Gummy Polar Bear§r§a!") {
            effectDuration[NonGodPotEffect.SMOLDERING] = Timer(1.hours)
            update()
        }

        if (event.message == "§a§lBUFF! §fYou have gained §r§2Mushed Glowy Tonic I§r§f! Press TAB or type /effects to view your active effects!") {
            effectDuration[NonGodPotEffect.GLOWY] = Timer(1.hours)
            update()
        }

        if (event.message == "§a§lBUFF! §fYou splashed yourself with §r§bWisp's Ice-Flavored Water I§r§f! Press TAB or type /effects to view your active effects!") {
            effectDuration[NonGodPotEffect.WISP] = Timer(5.minutes)
            update()
        }

        if (event.message == "§eYou consumed a §r§fGreat Spook Potion§r§e!") {
            effectDuration[NonGodPotEffect.GREAT_SPOOK] = Timer(24.hours)
            update()
        }


        if (event.message == "§e[NPC] §6King Yolkar§f: §rThese eggs will help me stomach my pain.") {
            effectDuration[NonGodPotEffect.GOBLIN] = Timer(20.minutes)
            update()
        }

        if (event.message == "§cThe Goblin King's §r§afoul stench §r§chas dissipated!") {
            effectDuration.remove(NonGodPotEffect.GOBLIN)
            update()
        }
    }

    private fun update() {
        if (effectDuration.values.removeIf { it.ended }) {
            //to fetch the real amount of active pots
            totalEffectsCount = 0
            checkFooter = true
        }

        display = drawDisplay()
    }

    private fun drawDisplay(): MutableList<String> {
        val newDisplay = mutableListOf<String>()
        for ((effect, time) in effectDuration.sorted()) {
            if (time.ended) continue
            if (effect == NonGodPotEffect.INVISIBILITY) continue

            if (effect.isMixin && !config.nonGodPotEffectShowMixins) continue

            val remaining = time.remaining.coerceAtLeast(0.seconds)
            val format = TimeUtils.formatDuration(remaining.inWholeMilliseconds, TimeUnit.HOUR)
            val color = colorForTime(remaining)

            val displayName = effect.tabListName
            newDisplay.add("$displayName $color$format")
        }
        val diff = totalEffectsCount - effectDuration.size
        if (diff > 0) {
            newDisplay.add("§eOpen the /effects inventory")
            newDisplay.add("§eto show the missing $diff effects!")
            checkFooter = true
        }
        return newDisplay
    }

    private fun colorForTime(duration: Duration) = when (duration) {
        in 0.seconds..60.seconds -> "§c"
        in 60.seconds..3.minutes -> "§6"
        in 3.minutes..10.minutes -> "§e"
        else -> "§f"
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(1)) return

        update()
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        checkFooter = true
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!event.inventoryName.endsWith("Active Effects")) return

        for (stack in event.inventoryItems.values) {
            val name = stack.name ?: continue
            for (effect in NonGodPotEffect.entries) {
                if (!name.contains(effect.inventoryItemName)) continue
                for (line in stack.getLore()) {
                    if (line.contains("Remaining") &&
                        line != "§7Time Remaining: §aCompleted!" &&
                        !line.contains("Remaining Uses")
                    ) {
                        val duration = try {
                            TimeUtils.getMillis(line.split("§f")[1])
                        } catch (e: IndexOutOfBoundsException) {
                            ErrorManager.logError(
                                Exception("'§f' not found in line '$line'", e),
                                "Error while reading Non God-Potion effects from tab list"
                            )
                            continue
                        }
                        effectDuration[effect] = Timer(duration.milliseconds)
                        update()
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        if (!checkFooter) return
        if (packet is S47PacketPlayerListHeaderFooter) {
            val formattedText = packet.footer.formattedText
            val lines = formattedText.replace("§r", "").split("\n")

            if (!lines.any { it.contains("§a§lActive Effects") }) return
            checkFooter = false

            var effectsCount = 0
            for (line in lines) {
                for (effect in NonGodPotEffect.entries) {
                    val tabListName = effect.tabListName
                    if (line.startsWith(tabListName)) {
                        val string = line.substring(tabListName.length)
                        try {
                            val duration = TimeUtils.getMillis(string.split("§f")[1])
                            effectDuration[effect] = Timer(duration.milliseconds)
                            update()
                        } catch (e: IndexOutOfBoundsException) {
                            LorenzUtils.debug("Error while reading non god pot effects from tab list! line: '$line'")
                        }
                    }
                }
                patternEffectsCount.matchMatcher(line) {
                    val group = group("name")
                    effectsCount = group.toInt()
                }
            }
            totalEffectsCount = effectsCount
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (RiftAPI.inRift()) return

        config.nonGodPotEffectPos.renderStrings(
            display,
            extraSpace = 3,
            posLabel = "Non God Pot Effects"
        )
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.nonGodPotEffectDisplay", "misc.potionEffect.nonGodPotEffectDisplay")
        event.move(3, "misc.nonGodPotEffectShowMixins", "misc.potionEffect.nonGodPotEffectShowMixins")
        event.move(3, "misc.nonGodPotEffectPos", "misc.potionEffect.nonGodPotEffectPos")
    }

    private fun isEnabled() =
        LorenzUtils.inSkyBlock && config.nonGodPotEffectDisplay && !LorenzUtils.inDungeons && !LorenzUtils.inKuudraFight
}
