package at.hannibal2.skyhanni.data.hotx

import java.util.regex.Pattern

interface RotatingPerk {
    val displayDescription: String
    val chatPattern: Pattern
    val itemPattern: Pattern
}
