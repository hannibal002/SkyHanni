package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.JsonSyntaxException
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import java.io.IOException
import kotlin.jvm.optionals.getOrNull

class ResourcePackReloadEvent(
    val resourceManager: ResourceManager,
) : SkyHanniEvent() {
    inline fun <reified T : Any> getJsonResource(location: Identifier): T? {
        return try {
            val packOverridesStream = resourceManager.getResource(location).getOrNull()?.open() ?: return null

            ConfigManager.gson.fromJson<T>(packOverridesStream.reader())
        } catch (exception: JsonSyntaxException) {
            val message = "Invalid resource Json at $location"
            ErrorManager.logErrorWithData(exception, message)

            null
        } catch (_: IOException) {
            null
        }
    }
}
