package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternManager
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.client.gui.screen.TitleScreen
import org.apache.logging.log4j.LogManager
import java.io.File

@Suppress("UnstableApiUsage")
object RepoPatternDumpTest: FabricClientGameTest {

    private val logger = LogManager.getLogger("SkyHanni")

    @GameTest
    override fun runTest(context: ClientGameTestContext) {
        val dumpDirective = System.getenv("SKYHANNI_DUMP_REGEXES")
        if (dumpDirective.isNullOrBlank()) return
        context.waitFor { mc ->
            mc.currentScreen is TitleScreen
        }
        val (sourceLabel, path) = dumpDirective.split(":", limit = 2)
        RepoPatternManager.dump(sourceLabel, File(path))
        logger.info("Exiting after dumping RepoPattern regex patterns to $path")
    }

}
