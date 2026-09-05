package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.StringUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringUtilsTest {
    @Test
    fun `optionalAn returns the correct article`() {
        assertEquals("an", StringUtils.optionalAn("apple"))
        assertEquals("an", StringUtils.optionalAn("Apple"))
        assertEquals("a", StringUtils.optionalAn("pear"))
        assertEquals("a", StringUtils.optionalAn("Pear"))
        assertEquals("a", StringUtils.optionalAn("yeti"))
        assertEquals("a", StringUtils.optionalAn("Yeti"))
    }
}
