package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.network.play.server.S47PacketPlayerListHeaderFooter
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class NonGodPotEffectDisplay {
    private val config get() = SkyHanniMod.feature.misc
    private var checkFooter = false
    private val effectDuration = mutableMapOf<NonGodPotEffect, Long>()
    private var display = emptyList<String>()
    private var lastTick = 0L

    enum class NonGodPotEffect(val apiName: String, val displayName: String, val isMixin: Boolean = false) {
        SMOLDERING("smoldering_polarization", "§aSmoldering Polarization I"),
        GLOWY("mushed_glowy_tonic", "§2Mushed Glowy Tonic I"),
        WISP("wisp_ice", "§bWisp's Ice-Flavored Water I"),
        GOBLIN("goblin_king_scent", "§2King's Scent I"),

        INVISIBILITY("invisibility", "§8Invisibility I"), // when wearing sorrow armor

        REV("ZOMBIE_BRAIN", "§9Zombie Brain Mixin", true),
        TARA("SPIDER_EGG", "§9Spider Egg Mixin", true),
        SVEN("WOLF_FUR", "§9Wolf Fur Mixin", true),
        VOID("END_PORTAL_FUMES", "§9End Portal Fumes", true),
        BLAZE("GABAGOEY", "§9Gabagoey", true),

        DEEP_TERROR("DEEPTERROR", "§4Deepterror", true),
        ;
    }

    private var patternEffectsCount = "§7You have §e(?<name>\\d+) §7non-god effects\\.".toPattern()
    private var totalEffectsCount = 0

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (event.message == "§aYou cleared all of your active effects!") {
            effectDuration.clear()
            update()
        }

        if (event.message == "§aYou ate a §r§aRe-heated Gummy Polar Bear§r§a!") {
            checkFooter = true
            effectDuration[NonGodPotEffect.SMOLDERING] = System.currentTimeMillis() + 1000 * 60 * 60
            update()
        }

        if (event.message == "§a§lBUFF! §fYou have gained §r§2Mushed Glowy Tonic I§r§f! Press TAB or type /effects to view your active effects!") {
            checkFooter = true
            effectDuration[NonGodPotEffect.GLOWY] = System.currentTimeMillis() + 1000 * 60 * 60
            update()
        }

        if (event.message == "§a§lBUFF! §fYou splashed yourself with §r§bWisp's Ice-Flavored Water I§r§f! Press TAB or type /effects to view your active effects!") {
            checkFooter = true
            effectDuration[NonGodPotEffect.WISP] = System.currentTimeMillis() + 1000 * 60 * 5
            update()
        }


        if (event.message == "§e[NPC] §6King Yolkar§f: §rThese eggs will help me stomach my pain.") {
            checkFooter = true
            effectDuration[NonGodPotEffect.GOBLIN] = System.currentTimeMillis() + 1000 * 60 * 20
            update()
        }
        if (event.message == "§cThe Goblin King's §r§afoul stench §r§chas dissipated!") {
            checkFooter = true
            effectDuration.remove(NonGodPotEffect.GOBLIN)
            update()
        }
    }

    private fun update() {
        val now = System.currentTimeMillis()
        if (effectDuration.values.removeIf { now > it }) {
            //to fetch the real amount of active pots
            totalEffectsCount = 0
            checkFooter = true
        }

        display = drawDisplay(now)
    }

    private fun drawDisplay(now: Long): MutableList<String> {
        val newDisplay = mutableListOf<String>()
        for ((effect, time) in effectDuration.sorted()) {
            if (effect == NonGodPotEffect.INVISIBILITY) continue

            if (effect.isMixin && !config.nonGodPotEffectShowMixins) continue

            val seconds = time - now
            val format = TimeUtils.formatDuration(seconds, TimeUnit.HOUR)
            val color = colorForTime(seconds)

            val displayName = effect.displayName
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

    private fun colorForTime(seconds: Long): String {
        return if (seconds <= 60) {
            "§c"
        } else if (seconds <= 60 * 3) {
            "§6"
        } else if (seconds <= 60 * 10) {
            "§e"
        } else "§f"
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return
        if (lastTick + 1_000 > System.currentTimeMillis()) return
        lastTick = System.currentTimeMillis()

        update()
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        checkFooter = true
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!event.inventoryName.endsWith("Active Effects")) return

        for (stack in event.inventoryItems.values) {
                val name = stack.name ?: continue
                for (effect in NonGodPotEffect.values()) {
                    if (name == effect.displayName) continue
                    for (line in stack.getLore()) {
                        if (line.contains("Remaining") &&
                            line != "§7Time Remaining: §aCompleted!" &&
                            !line.contains("Remaining Uses")
                        ) {
                            val duration = try {
                                TimeUtils.getMillis(line.split("§f")[1])
                            } catch (e: IndexOutOfBoundsException) {
                                CopyErrorCommand.logError(
                                    Exception("'§f' not found in line '$line'", e),
                                    "Error while reading Non God-Potion effects from tab list"
                                )
                                continue
                            }
                            effectDuration[effect] = System.currentTimeMillis() + duration
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
                for (effect in NonGodPotEffect.values()) {
                    if (line.startsWith(effect.displayName)) {
                        try {
                            val duration = TimeUtils.getMillis(line.split("§f")[1])
                            effectDuration[effect] = System.currentTimeMillis() + duration
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
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        config.nonGodPotEffectPos.renderStrings(
            display,
            extraSpace = 3,
            posLabel = "Non God Pot Effects"
        )
    }

    @SubscribeEvent
    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
        val profileData = event.profileData
        val effects = profileData["active_effects"]?.asJsonArray ?: return
        for (element in effects) {
            val effectJson = element.asJsonObject
            val name = effectJson["effect"].asString
            val effect = NonGodPotEffect.values().find { it.apiName == name } ?: continue

            val time = effectJson["ticks_remaining"].asLong / 20
            val newValue = System.currentTimeMillis() + time * 1000

            effectDuration[effect] = if (effect == NonGodPotEffect.INVISIBILITY) {
                System.currentTimeMillis() + 1000 * 60 * 60 * 24
            } else newValue
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && config.nonGodPotEffectDisplay && !LorenzUtils.inDungeons && !LorenzUtils.inKuudraFight
    }
}