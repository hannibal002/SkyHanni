package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternDump
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

object RepoPatternTest {

    @Test
    fun testRemoteLoad() {
        val simpleLocalePattern = "I'm a test value"

        val simpleRepoPattern = RepoPattern.pattern("testonly.a", simpleLocalePattern)
        val simplePattern by simpleRepoPattern

        val remoteValue = "I'm remote."

        val isLocalWorking = simplePattern.pattern() == simpleLocalePattern

        assert(isLocalWorking)

        RepoPatternManager.loadPatternsFromDump(
            RepoPatternDump(
                regexes = mapOf(
                    simpleRepoPattern.key to remoteValue
                )
            )
        )

        val isRemoteWorking = simplePattern.pattern() == remoteValue

        assert(isRemoteWorking)

    }

    @Test
    fun testExclusivity() {
        RepoPatternManager.inTestDuplicateUsage = false

        assertThrows<RuntimeException> {
            val pattern1 by RepoPattern.pattern("testonly.b", "")
            val pattern2 by RepoPattern.pattern("testonly.b", "")
            pattern1
            pattern2
        }

        RepoPatternManager.inTestDuplicateUsage = true
    }
}
