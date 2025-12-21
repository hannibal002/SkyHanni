package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternManager
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.gui.screens.TitleScreen
import org.apache.logging.log4j.LogManager
import java.io.File

@Suppress("UnstableApiUsage")
object RepoPatternDumpTest : FabricClientGameTest {

    private val logger = LogManager.getLogger("SkyHanni")

    override fun runTest(context: ClientGameTestContext) {
        val dumpDirective = PlatformUtils.getRepoPatternDumpLocation() ?: return
        context.waitFor { mc ->
            mc.screen is TitleScreen
        }
        val (sourceLabel, path) = dumpDirective.split(":", limit = 2)
        RepoPatternManager.dump(sourceLabel, File(path))
        logger.info("Exiting after dumping RepoPattern regex patterns to $path")
    }

}
