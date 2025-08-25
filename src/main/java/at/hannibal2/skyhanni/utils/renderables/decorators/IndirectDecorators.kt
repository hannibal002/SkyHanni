package at.hannibal2.skyhanni.utils.renderables.decorators

import at.hannibal2.skyhanni.utils.renderables.Renderable

// This file is only here to bridge the gap between actual moves of those decorators

fun Renderable.tips(tips: List<String>): Renderable = Renderable.hoverTips(this, tips)
