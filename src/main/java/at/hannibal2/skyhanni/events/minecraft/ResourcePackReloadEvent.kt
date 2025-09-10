package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.JsonSyntaxException
import net.minecraft.client.resources.IResourceManager
import net.minecraft.util.ResourceLocation
import java.io.IOException

//#if MC > 1.21
//$$ import kotlin.jvm.optionals.getOrNull
//#endif

class ResourcePackReloadEvent(
    val resourceManager: IResourceManager,
) : SkyHanniEvent() {
    inline fun <reified T : Any> getJsonResource(location: ResourceLocation): T? {
        return try {
            val packOverridesStream = resourceManager.getResource(location)
                //#if MC > 1.21
                //$$ .getOrNull()
                //#endif
                ?.inputStream ?: return null

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
