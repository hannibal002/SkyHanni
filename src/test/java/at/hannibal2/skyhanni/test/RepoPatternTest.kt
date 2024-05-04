package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternDump
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
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

        val remoteValue1 = "I'm remote."
        val remoteValue2 = "I'm remote 2."
        val remoteValue3 = "I'm remote 3."
        val remoteValue4 = "I'm remote 4."

        val isLocalWorking = list[0].pattern() == simpleLocalePattern1 && list[1].pattern() == simpleLocalePattern2

        assert(isLocalWorking)

        RepoPatternManager.loadPatternsFromDump(
            RepoPatternDump(
                regexes = mapOf(
                    listPatterns.key + ".1" to remoteValue1,
                    listPatterns.key + ".2" to remoteValue2
                )
            )
        )

        val isRemoteWorking = list[0].pattern() == remoteValue1 && list[1].pattern() == remoteValue2

        assert(isRemoteWorking)

        RepoPatternManager.loadPatternsFromDump(
            RepoPatternDump(
                regexes = mapOf(
                    listPatterns.key + ".1" to remoteValue3,
                )
            )
        )

        val isRemoteSingleWorking = list[0].pattern() == remoteValue3

        assert(isRemoteSingleWorking)
        assertThrows<IndexOutOfBoundsException> { list[1] }

        RepoPatternManager.loadPatternsFromDump(
            RepoPatternDump(
                regexes = mapOf(
                    listPatterns.key to remoteValue4,
                )
            )
        )

        val isRemoteListToSingleWorking = list.isEmpty()

        assert(isRemoteListToSingleWorking)
    }

    @Test
    fun testExclusivityList() {
        RepoPatternManager.inTestDuplicateUsage = false

        assertThrows<RuntimeException> {
            val pattern1 by RepoPattern.list("testonly.c", "")
            val pattern2 by RepoPattern.list("testonly.c", "")
            pattern1
            pattern2
        }

        assertThrows<RuntimeException> {
            val pattern1 by RepoPattern.list("testonly.d", "")
            val pattern2 by RepoPattern.pattern("testonly.d", "")
            pattern1
            pattern2
        }

        assertThrows<RuntimeException> {
            val pattern1 by RepoPattern.list("testonly.e", "")
            val pattern2 by RepoPattern.pattern("testonly.e.1", "")
            pattern1
            pattern2
        }

        assertThrows<RuntimeException> {
            val pattern1 by RepoPattern.list("testonly.f", "")
            val pattern2 by RepoPattern.pattern("testonly.f.a", "")
            pattern1
            pattern2
        }

        assertDoesNotThrow {
            val pattern1 by RepoPattern.list("testonly.g", "")
            val pattern2 by RepoPattern.list("testonly.h", "")
            val pattern3 by RepoPattern.pattern("testonly.i", "")
            pattern1
            pattern2
            pattern3
        }

        assertDoesNotThrow {
            val pattern1 by RepoPattern.list("testonly.j", "")
            val pattern2 by RepoPattern.pattern("testonly.j.1.2", "")
            pattern1
            pattern2
        }

        RepoPatternManager.inTestDuplicateUsage = true
    }

}
