package at.hannibal2.skyhanni.features.misc.reminders

object Base62 {

    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private const val BASE = 62

    fun encode(num: Int): String {
        var n = num
        val sb = StringBuilder()
        while (n > 0) {
            sb.append(ALPHABET[(n % BASE)])
            n /= BASE
        }
        return sb.reverse().toString()
    }

    fun decode(str: String): Int {
        var num = 0
        for (c in str) {
            num = num * BASE + ALPHABET.indexOf(c)
        }
        return num
    }
}
