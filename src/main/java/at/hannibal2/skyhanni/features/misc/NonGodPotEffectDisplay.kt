package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.network.play.server.S30PacketWindowItems
import net.minecraft.network.play.server.S47PacketPlayerListHeaderFooter
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class NonGodPotEffectDisplay {

    private var checkFooter = false
    private val activeEffects = mutableMapOf<String, Long>()
    private var display = listOf<String>()
    private var lastTick = 0L

    private var nonGodPotEffects = mapOf(
        "smoldering_polarization" to "§aSmoldering Polarization I",
        "mushed_glowy_tonic" to "§2Mushed Glowy Tonic I",
        "wisp_ice" to "§bWisp's Ice-Flavored Water I",
        "goblin_king_scent" to "§2King's Scent I",

        "invisibility" to "§8Invisibility I", // when wearing sorrow armor

        // Bartender Mixins
        "ZOMBIE_BRAIN" to "§9Zombie Brain Mixin",
        "SPIDER_EGG" to "§9Spider Egg Mixin",
        "WOLF_FUR" to "§9Wolf Fur Mixin",
        "END_PORTAL_FUMES" to "§9End Portal Fumes",
        //TODO fix typo
        "GABAGOEY" to "§9Gabagoey Mixin",
    )

    private var patternEffectsCount = "§7You have §e(?<name>\\d+) §7non-god effects\\.".toPattern()
    private var totalEffectsCount = 0

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (event.message == "§aYou cleared all of your active effects!") {
            activeEffects.clear()
            update()
        }

        if (event.message == "§aYou ate a §r§aRe-heated Gummy Polar Bear§r§a!") {
            checkFooter = true
            activeEffects["§aSmoldering Polarization I"] = System.currentTimeMillis() + 1000 * 60 * 60
            update()
        }

        if (event.message == "§a§lBUFF! §fYou have gained §r§2Mushed Glowy Tonic I§r§f! Press TAB or type /effects to view your active effects!") {
            checkFooter = true
            activeEffects["§2Mushed Glowy Tonic I"] = System.currentTimeMillis() + 1000 * 60 * 60
            update()
        }

        if (event.message == "§a§lBUFF! §fYou splashed yourself with §r§bWisp's Ice-Flavored Water I§r§f! Press TAB or type /effects to view your active effects!") {
            checkFooter = true
            activeEffects["§bWisp's Ice-Flavored Water I"] = System.currentTimeMillis() + 1000 * 60 * 5
            update()
        }


        if (event.message == "§e[NPC] §6King Yolkar§f: §rThese eggs will help me stomach my pain.") {
            checkFooter = true
            activeEffects["§2King's Scent I"] = System.currentTimeMillis() + 1000 * 60 * 20
            update()
        }
    }

    private fun update() {
        val now = System.currentTimeMillis()
        if (activeEffects.values.removeIf { now > it }) {
            //to fetch the real amount of active pots
            totalEffectsCount = 0
            checkFooter = true
        }

        display = drawDisplay(now)
    }

    private fun drawDisplay(now: Long): MutableList<String> {
        val newDisplay = mutableListOf<String>()
        for (effect in activeEffects.sorted()) {
            val label = effect.key
            if (label.contains("Invisibility")) continue
            if (label.contains("Mixin")) continue
            if (label.contains("Fumes")) continue

            val until = effect.value
            val seconds = until - now
            val format = TimeUtils.formatDuration(seconds, TimeUnit.HOUR)

            val color = colorForTime(seconds)

            newDisplay.add("$label $color$format")
        }
        val diff = totalEffectsCount - activeEffects.size
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
        } else {
            "§f"
        }
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

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        if (packet is S30PacketWindowItems) {
            for (stack in packet.itemStacks) {
                val name = stack?.name ?: continue
                if (name in nonGodPotEffects.values) {
                    for (line in stack.getLore()) {
                        if (line.contains("Remaining")) {
                            val duration = TimeUtils.getMillis(line.split("§f")[1])
                            activeEffects[name] = System.currentTimeMillis() + duration
                            update()
                        }
                    }
                }
            }
        }

        if (!checkFooter) return
        if (packet is S47PacketPlayerListHeaderFooter) {
            val formattedText = packet.footer.formattedText
            val lines = formattedText.replace("§r", "").split("\n")

            if (!lines.any { it.contains("§a§lActive Effects") }) return
            checkFooter = false

            var effectsCount = 0
            for (line in lines) {
                if (line.startsWith("§2Mushed Glowy Tonic I")) {
                    val duration = TimeUtils.getMillis(line.split("§f")[1])
                    activeEffects["§2Mushed Glowy Tonic I"] = System.currentTimeMillis() + duration
                    update()
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

        SkyHanniMod.feature.misc.nonGodPotEffectPos.renderStrings(display, extraSpace = 3, posLabel = "Non God Pot Effects")
    }

    @SubscribeEvent
    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
        val profileData = event.profileData
        val effects = profileData["active_effects"]?.asJsonArray ?: return
        for (element in effects) {
            val effect = element.asJsonObject
            val name = effect["effect"].asString
            val label = nonGodPotEffects[name] ?: continue

            val time = effect["ticks_remaining"].asLong / 20
            val newValue = System.currentTimeMillis() + time * 1000
            if (label.contains("Invisibility")) {
                activeEffects[label] = System.currentTimeMillis() + 1000 * 60 * 60 * 24
            } else {
                activeEffects[label] = newValue
            }
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.misc.nonGodPotEffectDisplay && !LorenzUtils.inDungeons && !LorenzUtils.inKuudraFight
    }
}