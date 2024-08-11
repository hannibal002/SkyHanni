package at.hannibal2.skyhanni.data.hypixel.location

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.modapi.HypixelLocationChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.JsonObject
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import net.hypixel.data.type.ServerType
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.concurrent.thread
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HypixelLocationData {
    private var lastLocRaw = SimpleTimeMark.farPast()
    private var previousLocation: HypixelLocation? = null

    var location: HypixelLocation? = null
        private set

    @HandleEvent
    fun onDisconnect(event: ClientDisconnectEvent) {
        location = null
        previousLocation = null
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (location != null) {
            previousLocation = location
            location = null
            lastLocRaw = SimpleTimeMark.farPast()
        }
    }

    fun update(newLocation: HypixelLocation) {
        HypixelLocationChangeEvent(newLocation, previousLocation).post()
        location = newLocation
    }

    /* backup logic - neu locraw */

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (LorenzUtils.onHypixel && location == null && lastLocRaw.passedSince() > 15.seconds) {
            sendNEULocraw()
        }
    }

    // Modified from NEU.
    // NEU does not send locraw when not in SkyBlock.
    // So, as requested by Hannibal, use locraw from
    // NEU and have NEU send it.
    // Remove this when NEU dependency is removed
    private fun sendNEULocraw() {
        lastLocRaw = SimpleTimeMark.now()
        thread(start = true) {
            Thread.sleep(1000)
            NotEnoughUpdates.INSTANCE.sendChatMessage("/locraw")
        }
    }

    // This code is modified from NEU, and depends on NEU (or another mod) sending /locraw.
    private val jsonBracketPattern = "^\\{.+}".toPattern()

    fun parseLocraw(message: String) {
        if (location != null) return
        jsonBracketPattern.matchMatcher(message.removeColor()) {
            try {
                val obj: JsonObject = ConfigManager.gson.fromJson(group(), JsonObject::class.java)
                if (obj.has("server")) {

                    val server = obj["server"].asString

                    // note: lobbyType was removed from locraw at some point;
                    // it just returns the server type but still calls it gametype
                    val gameType = obj["gametype"]?.asString
                    val lobbyName = obj["lobbyname"]?.asString
                    val mode = obj["mode"]?.asString
                    val map = obj["map"]?.asString
                    val serverType = gameType?.let { ServerType.valueOf(gameType).getOrNull() }

                    update(HypixelLocation(
                        server,
                        serverType,
                        lobbyName,
                        mode,
                        map
                    ))
                }
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Failed to parse locraw data")
            }
        }
    }

}
