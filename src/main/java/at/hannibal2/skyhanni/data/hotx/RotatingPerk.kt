package at.hannibal2.hanni.data.hotx

import java.util.regex.Pattern

interface RotatingPerk {
    val perkDescription: String
    val chatPattern: Pattern
    val itemPattern: Pattern
}
