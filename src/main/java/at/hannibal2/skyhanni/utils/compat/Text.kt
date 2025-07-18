package at.hannibal2.skyhanni.utils.compat

class Text private constructor(val text: String) {

    companion object {
        fun of(string: String): Text {
            return Text(string)
        }
    }

    override fun toString(): String {
        return this.text
    }
}
