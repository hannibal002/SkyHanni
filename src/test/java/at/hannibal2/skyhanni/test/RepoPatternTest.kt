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

    @Test
    fun testRemoteLoadList() {
        val simpleLocalePattern1 = "I'm a test value"
        val simpleLocalePattern2 = "I'm a test value 2"

        val listPatterns = RepoPattern.list("testonly.list.a", simpleLocalePattern1, simpleLocalePattern2)
        val list by listPatterns

        val remoteValue = "I'm remote."

        val isLocalWorking = list[0].pattern() == simpleLocalePattern1 && list[1].pattern() == simpleLocalePattern2

        assert(isLocalWorking)

        RepoPatternManager.loadPatternsFromDump(
            RepoPatternDump(
                regexes = mapOf(
                    listPatterns.key to remoteValue
                )
            )
        )

        val isRemoteWorking = list.size != 0 && list.all { it.pattern() == remoteValue }

        assert(isRemoteWorking)

    }

    /* @Test
    fun testExclusivityList() {
        RepoPatternManager.inTestDuplicateUsage = false

        assertThrows<RuntimeException> {
            val pattern1 by RepoPattern.pattern("testonly.b", "")
            val pattern2 by RepoPattern.pattern("testonly.b", "")
            pattern1
            pattern2
        }

        RepoPatternManager.inTestDuplicateUsage = true
    } */
}
