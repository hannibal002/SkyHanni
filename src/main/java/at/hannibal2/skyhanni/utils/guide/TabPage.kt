package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.utils.renderables.Renderable

interface TabPage {
    fun buildRenderable(): Renderable
    fun onEnter() {}
    fun onLeave() {}
    fun refresh() {
        onLeave()
        onEnter()
    }
}
