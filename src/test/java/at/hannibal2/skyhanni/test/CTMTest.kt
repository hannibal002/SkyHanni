package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.CTMUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

object CTMTest {

    @Test
    fun testOurCTM() {
        for (mask in 0 until (1 shl 8)) {
            val up = (mask shr 7 and 1) == 1
            val right = (mask shr 6 and 1) == 1
            val down = (mask shr 5 and 1) == 1
            val left = (mask shr 4 and 1) == 1
            val upLeft = (mask shr 3 and 1) == 1
            val upRight = (mask shr 2 and 1) == 1
            val downRight = (mask shr 1 and 1) == 1
            val downLeft = (mask and 1) == 1

            // If they're all false, we skip this case
            if (mask == 0) continue

            val expected = neuGetCTMIndex(
                up, right, down, left,
                upLeft, upRight, downRight, downLeft
            )

            val ctmData = CTMUtils.CTMData(
                up, right, down, left,
                upLeft, upRight, downRight, downLeft
            )
            val actual = CTMUtils.getCTMIndex(ctmData)

            Assertions.assertEquals(
                expected, actual,
                "CTM index mismatch for mask $mask: expected $expected, got $actual"
            )
        }
    }

    /**
     * This is just a kotlin-ported version of NEU's CTMIndex function.
     * We're using it to test the correctness of our CTMIndex function.
     */
    fun neuGetCTMIndex(
        up: Boolean,
        right: Boolean,
        down: Boolean,
        left: Boolean,
        upleft: Boolean,
        upright: Boolean,
        downright: Boolean,
        downleft: Boolean,
    ): Int {
        if (up && right && down && left) {
            if (upleft && upright && downright && downleft) {
                return 26;
            } else if (upleft && upright && downright && !downleft) {
                return 33;
            } else if (upleft && upright && !downright && downleft) {
                return 32;
            } else if (upleft && upright && !downright && !downleft) {
                return 11;
            } else if (upleft && !upright && downright && downleft) {
                return 44;
            } else if (upleft && !upright && downright && !downleft) {
                return 35;
            } else if (upleft && !upright && !downright && downleft) {
                return 10;
            } else if (upleft && !upright && !downright && !downleft) {
                return 20;
            } else if (!upleft && upright && downright && downleft) {
                return 45;
            } else if (!upleft && upright && downright && !downleft) {
                return 23;
            } else if (!upleft && upright && !downright && downleft) {
                return 34;
            } else if (!upleft && upright && !downright && !downleft) {
                return 8;
            } else if (!upleft && !upright && downright && downleft) {
                return 22;
            } else if (!upleft && !upright && downright && !downleft) {
                return 9;
            } else if (!upleft && !upright && !downright && downleft) {
                return 21;
            } else {
                return 46;
            }
        } else if (up && right && down && !left) {
            if (!upright && !downright) {
                return 6;
            } else if (!upright) {
                return 28;
            } else if (!downright) {
                return 30;
            } else {
                return 25;
            }
        } else if (up && right && !down && left) {
            if (!upleft && !upright) {
                return 18;
            } else if (!upleft) {
                return 40;
            } else if (!upright) {
                return 42;
            } else {
                return 38;
            }
        } else if (up && right && !down && !left) {
            if (!upright) {
                return 16;
            } else {
                return 37;
            }
        } else if (up && !right && down && left) {
            if (!upleft && !downleft) {
                return 19;
            } else if (!upleft) {
                return 43;
            } else if (!downleft) {
                return 41;
            } else {
                return 27;
            }
        } else if (up && !right && down && !left) {
            return 24;
        } else if (up && !right && !down && left) {
            if (!upleft) {
                return 17;
            } else {
                return 39;
            }
        } else if (up && !right && !down && !left) {
            return 36;
        } else if (!up && right && down && left) {
            if (!downleft && !downright) {
                return 7;
            } else if (!downleft) {
                return 31;
            } else if (!downright) {
                return 29;
            } else {
                return 14;
            }
        } else if (!up && right && down && !left) {
            if (!downright) {
                return 4;
            } else {
                return 13;
            }
        } else if (!up && right && !down && left) {
            return 2;
        } else if (!up && right && !down && !left) {
            return 1;
        } else if (!up && !right && down && left) {
            if (!downleft) {
                return 5;
            } else {
                return 15;
            }
        } else if (!up && !right && down && !left) {
            return 12;
        } else if (!up && !right && !down && left) {
            return 3;
        } else {
            return 0;
        }
    }
}
