package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternDump
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

object RepoPatternTest {

    private val abc = generateSequence('a') { it + 1 }.take(26).joinToString("")

    private var counter: Int = 0

    private fun nextKey(): String = "testonly.${getChar(counter++)}"
    private fun prevKey(n: Int = 0): String = "testonly.${getChar(counter - n - 1)}"

    private fun getChar(count: Int): String = (if (count > 26) getChar(count / 26) else "") + abc[count % 26]

    @Test
    fun testRemoteLoad() {
        val simpleLocalePattern = "I'm a test value"

        val simpleRepoPattern = RepoPattern.pattern(this.nextKey(), simpleLocalePattern)
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
            val pattern1 by RepoPattern.pattern(this.nextKey(), "")
            val pattern2 by RepoPattern.pattern(this.prevKey(), "")
            pattern1
            pattern2
        }
        assertDoesNotThrow {
            val pattern1 by RepoPattern.pattern(this.nextKey(), "")
            val pattern2 by RepoPattern.pattern(this.prevKey() + ".a", "")
            pattern1
            pattern2
        }

        RepoPatternManager.inTestDuplicateUsage = true
    }

    @Test
    fun testRemoteLoadList() {
        val simpleLocalePattern1 = "I'm a test value"
        val simpleLocalePattern2 = "I'm a test value 2"

        val listPatterns = RepoPattern.list(this.nextKey(), simpleLocalePattern1, simpleLocalePattern2)
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
            val pattern1 by RepoPattern.list(this.nextKey(), "")
            val pattern2 by RepoPattern.list(this.prevKey(), "")
            pattern1
            pattern2
        }

        assertThrows<RuntimeException> {
            val pattern1 by RepoPattern.list(this.nextKey(), "")
            val pattern2 by RepoPattern.pattern(this.prevKey(), "")
            pattern1
            pattern2
        }

        assertThrows<RuntimeException> {
            val pattern1 by RepoPattern.list(this.nextKey(), "")
            val pattern2 by RepoPattern.pattern(this.prevKey() + ".1", "")
            pattern1
            pattern2
        }

        assertThrows<RuntimeException> {
            val pattern2 by RepoPattern.pattern(this.nextKey() + ".1", "")
            val pattern1 by RepoPattern.list(this.prevKey(), "")
            pattern2
            pattern1
        }

        assertThrows<RuntimeException> {
            val pattern1 by RepoPattern.list(this.nextKey(), "")
            val pattern2 by RepoPattern.pattern(this.prevKey() + ".a", "")
            pattern1
            pattern2
        }

        assertThrows<RuntimeException> {
            val pattern2 by RepoPattern.pattern(this.nextKey() + ".a", "")
            val pattern1 by RepoPattern.list(this.prevKey(), "")
            pattern2
            pattern1
        }

        assertDoesNotThrow {
            val pattern1 by RepoPattern.list(this.nextKey(), "")
            val pattern2 by RepoPattern.list(this.nextKey(), "")
            val pattern3 by RepoPattern.pattern(this.nextKey(), "")
            pattern1
            pattern2
            pattern3
        }

        assertThrows<RuntimeException> {
            val pattern1 by RepoPattern.list(this.nextKey(), "")
            val pattern2 by RepoPattern.pattern(this.prevKey() + ".1.2", "")
            pattern1
            pattern2
        }

        assertThrows<RuntimeException> {
            val pattern2 by RepoPattern.pattern(this.nextKey() + ".1.2", "")
            val pattern1 by RepoPattern.list(this.prevKey(), "")
            pattern2
            pattern1
        }

        RepoPatternManager.inTestDuplicateUsage = true
    }

    @Test
    fun testExclusivityForExclusiveGroup() {
        RepoPatternManager.inTestDuplicateUsage = false

        assertThrows<RuntimeException> {
            val group1 by RepoPattern.exclusiveGroup(nextKey())
            val group2 by RepoPattern.exclusiveGroup(prevKey())
            group1
            group2
        }

        assertDoesNotThrow {
            val group1 by RepoPattern.exclusiveGroup(nextKey())
            val group2 by RepoPattern.exclusiveGroup(nextKey())
            group1
            group2
        }

        assertThrows<RuntimeException> {
            val group1 by RepoPattern.exclusiveGroup(nextKey())
            val pattern1 by RepoPattern.pattern(prevKey(), "")
            group1
            pattern1
        }

        assertThrows<RuntimeException> {
            val group1 by RepoPattern.exclusiveGroup(nextKey())
            val pattern1 by RepoPattern.pattern(prevKey() + ".a", "")
            group1
            pattern1
        }

        assertDoesNotThrow {
            val group1 by RepoPattern.exclusiveGroup(nextKey() + ".a.a")
            val pattern1 by RepoPattern.pattern(prevKey() + ".a", "")
            group1
            pattern1
        }

        assertThrows<RuntimeException> {
            val group1 by RepoPattern.exclusiveGroup(nextKey())
            val pattern1 by group1.pattern("a", "")
            val pattern2 by RepoPattern.pattern(prevKey() + ".a.c", "")
            group1
            pattern1
            pattern2
        }

        assertThrows<RuntimeException> {
            val pattern2 by RepoPattern.pattern(nextKey() + ".a.c", "")
            pattern2
            val group1 by RepoPattern.exclusiveGroup(prevKey())
            group1
        }

        assertDoesNotThrow {
            val group1 by RepoPattern.exclusiveGroup(nextKey())
            val group2 by group1.exclusiveGroup("1")
            val pattern1 by group2.pattern("a", "")
            group1
            group2
            pattern1
        }

        assertThrows<RuntimeException> {
            val group1 by RepoPattern.exclusiveGroup(nextKey())
            val group2 by group1.exclusiveGroup("1")
            val pattern1 by group1.pattern("1", "")
            group1
            group2
            pattern1
        }


        RepoPatternManager.inTestDuplicateUsage = true
    }

}
