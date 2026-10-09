package at.hannibal2.skyhanni.config.core.config

import at.hannibal2.skyhanni.test.command.ErrorManager
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import kotlin.enums.EnumEntries

class PositionList() : ArrayList<Position>(), MutableList<Position> {

    constructor(init: Collection<Position>) : this() {
        this.addAll(init)
    }

    constructor(size: Int) : this() {
        this.addAll(List(size) { Position(10, 80) })
    }
    // This default position is entirely arbitrary so that elements don't appear in top left corner of the screen.

    fun setLink(configLink: ConfigLink) {
        this.configLink = configLink
        forEach {
            it.setLink(configLink)
        }
    }

    private var configLink: ConfigLink? = null

    override fun add(element: Position): Boolean {
        configLink?.let {
            element.setLink(it)
        }
        return super.add(element)
    }

    override fun addAll(elements: Collection<Position>): Boolean {
        configLink?.let { link ->
            elements.forEach {
                it.setLink(link)
            }
        }
        return super.addAll(elements)
    }

    override fun set(index: Int, element: Position): Position {
        configLink?.let {
            element.setLink(it)
        }
        return super.set(index, element)
    }

    companion object {

        fun updateConfigPositionList(configPositionList: PositionList, enumEntries: EnumEntries<*>, errorString: String): PositionList {
            val sizeDiff = enumEntries.size - configPositionList.size
            if (sizeDiff == 0) return configPositionList
            if (sizeDiff < 0) {
                ErrorManager.skyHanniError(
                    "Invalid Config State of $errorString",
                    "Display" to enumEntries,
                    "Positions" to configPositionList
                )
            } else {
                configPositionList.addAll(List(sizeDiff) { Position(10, 80) })
            } // This default position is entirely arbitrary so that elements don't appear in top left corner of the screen.
            return configPositionList
        }
    }
}
