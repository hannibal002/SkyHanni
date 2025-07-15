package at.hannibal2.skyhanni.utils

/**
 * Taken with love (and permission), and adapted from, the NEU source code.
 * https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/src/main/java/io/github/moulberry/notenoughupdates/miscfeatures/BetterContainers.java#L424
 */
@Suppress("CyclomaticComplexMethod")
object CTMUtils {

    data class CTMData(
        val up: Boolean,
        val right: Boolean,
        val down: Boolean,
        val left: Boolean,
        val upLeft: Boolean,
        val upRight: Boolean,
        val downRight: Boolean,
        val downLeft: Boolean
    )

    fun getCTMIndex(data: CTMData): Int = when (data.up) {
        true -> data.getCTMIndexUp()
        false -> data.getCTMIndexNotUp()
    }

    private fun CTMData.getCTMIndexUp(): Int = when (right) {
        true -> getCTMIndexUpRight()
        false -> getCTMIndexUpNotRight()
    }

    private fun CTMData.getCTMIndexUpRight(): Int = when {
        down -> getCTMIndexUpRightDown()
        !down && left && !upLeft && !upRight -> 18
        !down && left && !upLeft -> 40
        !down && left && !upRight -> 42
        !down && left -> 38
        !upRight -> 16
        else -> 37
    }

    private fun CTMData.getCTMIndexUpRightDown(): Int = when {
        left -> getCTMIndexUpRightDownLeft()
        !upRight && !downRight -> 6
        !upRight -> 28
        !downRight -> 30
        else -> 25
    }

    private fun CTMData.getCTMIndexUpRightDownLeft(): Int = when {
        upLeft && upRight && downRight && downLeft -> 26
        upLeft && upRight && downRight && !downLeft -> 33
        upLeft && upRight && !downRight && downLeft -> 32
        upLeft && upRight && !downRight && !downLeft -> 11
        upLeft && !upRight && downRight && downLeft -> 44
        upLeft && !upRight && downRight && !downLeft -> 35
        upLeft && !upRight && !downRight && downLeft -> 10
        upLeft && !upRight && !downRight && !downLeft -> 20
        !upLeft && upRight && downRight && downLeft -> 45
        !upLeft && upRight && downRight && !downLeft -> 23
        !upLeft && upRight && !downRight && downLeft -> 34
        !upLeft && upRight && !downRight && !downLeft -> 8
        downRight && downLeft -> 22
        downRight && !downLeft -> 9
        downLeft -> 21
        else -> 46
    }

    private fun CTMData.getCTMIndexUpNotRight(): Int = when {
        down && left && !upLeft && !downLeft -> 19
        down && left && !upLeft -> 43
        down && left && !downLeft -> 41
        down && left -> 27
        down && !left -> 24
        !down && left && !upLeft -> 17
        !down && left -> 39
        else -> 36
    }

    private fun CTMData.getCTMIndexNotUp(): Int = when {
        right && down && left && !downLeft && !downRight -> 7
        right && down && left && !downLeft -> 31
        right && down && left && !downRight -> 29
        right && down && left -> 14
        right && down && !left && !downRight -> 4
        right && down && !left -> 13
        right && !down && left -> 2
        right && !down && !left -> 1
        !right && down && left && !downLeft -> 5
        !right && down && left -> 15
        !right && down && !left -> 12
        left -> 3
        else -> 0
    }
}
