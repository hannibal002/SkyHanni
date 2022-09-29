package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.test.GriffinJavaUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils
import net.minecraft.network.play.server.S30PacketWindowItems
import net.minecraft.network.play.server.S47PacketPlayerListHeaderFooter
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.regex.Pattern

class NonGodPotEffectDisplay {

    private var checkFooter = false
    private val activeEffects = mutableMapOf<String, Long>()
    private val textToRender = mutableListOf<String>()
    private var lastTick = 0L

    private var nonGodPotEffects = mapOf(
        "smoldering_polarization" to "§aSmoldering Polarization I",
        "mushed_glowy_tonic" to "§2Mushed Glowy Tonic I",
        "wisp_ice" to "§bWisp's Ice-Flavored Water I",
    )

    private var patternEffectsCount = Pattern.compile("§7You have §e(\\d+) §7non-god effects\\.")
    private var totalEffectsCount = 0

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (event.message == "§aYou ate a §r§aRe-heated Gummy Polar Bear§r§a!") {
            checkFooter = true
            activeEffects["§aSmoldering Polarization I"] = System.currentTimeMillis() + 1000 * 60 * 60
            format()
        }

        if (event.message == "§a§lBUFF! §fYou have gained §r§2Mushed Glowy Tonic I§r§f! Press TAB or type /effects to view your active effects!") {
            checkFooter = true
            activeEffects["§2Mushed Glowy Tonic I"] = System.currentTimeMillis() + 1000 * 60 * 60
            format()
        }

        if (event.message == "§a§lBUFF! §fYou splashed yourself with §r§bWisp's Ice-Flavored Water I§r§f! Press TAB or type /effects to view your active effects!") {
            checkFooter = true
            activeEffects["§bWisp's Ice-Flavored Water I"] = System.currentTimeMillis() + 1000 * 60 * 5
            format()
        }
    }

    private fun format() {
        val now = System.currentTimeMillis()
        textToRender.clear()
        if (activeEffects.values.removeIf { now > it }) {
            //to fetch the real amount of active pots
            totalEffectsCount = 0
            checkFooter = true
        }
        for (effect in GriffinJavaUtils.sortByValue(activeEffects)) {
            val label = effect.key
            val until = effect.value
            val seconds = (until - now) / 1000
            val format = StringUtils.formatDuration(seconds)

            val color = colorForTime(seconds)

            textToRender.add("$label $color$format")
        }
        val diff = totalEffectsCount - activeEffects.size
        if (diff > 0) {
            textToRender.add("§eOpen the /effects inventory")
            textToRender.add("§eto show the missing $diff effects!")
            checkFooter = true
        }
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

        format()
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
                            val duration = readDuration(line.split("§f")[1])
                            activeEffects[name] = System.currentTimeMillis() + duration
                            format()
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
                    val duration = readDuration(line.split("§f")[1])
                    activeEffects["§2Mushed Glowy Tonic I"] = System.currentTimeMillis() + duration
                    format()
                }
                val matcher = patternEffectsCount.matcher(line)
                if (matcher.matches()) {
                    val group = matcher.group(1)
                    effectsCount = group.toInt()
                }
            }
            totalEffectsCount = effectsCount
        }
    }

    private fun readDuration(text: String): Int {
        val split = text.split(":")
        return when (split.size) {
            3 -> {
                val hours = split[0].toInt() * 1000 * 60 * 60
                val minutes = split[1].toInt() * 1000 * 60
                val seconds = split[2].toInt() * 1000
                seconds + minutes + hours
            }

            2 -> {
                val minutes = split[0].toInt() * 1000 * 60
                val seconds = split[1].toInt() * 1000
                seconds + minutes
            }

            1 -> {
                split[0].toInt() * 1000
            }

            else -> {
                throw RuntimeException("Invalid format: '$text'")
            }
        }
    }

    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!isEnabled()) return

        SkyHanniMod.feature.misc.nonGodPotEffectPos.renderStrings(textToRender)
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
            val old = activeEffects.getOrDefault(label, 0)
            val diff = newValue - old
            activeEffects[label] = newValue
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.misc.nonGodPotEffectDisplay && !LorenzUtils.inDungeons && !LorenzUtils.inKuudraFight
    }
}