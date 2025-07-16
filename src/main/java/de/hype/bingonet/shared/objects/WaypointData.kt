package de.hype.bingonet.shared.objects

import java.awt.Color

open class WaypointData(
    var position: Position,
    jsonTextToRender: String,
    var renderDistance: Int,
    var visible: Boolean,
    var deleteOnServerSwap: Boolean,
    var render: MutableList<RenderInformation>?,
    var color: Color,
    doTracer: Boolean
) {
    var jsonToRenderText: String? = null
    var doTracer: Boolean = true

    constructor(
        pos: Position,
        jsonTextToRender: String,
        renderDistance: Int,
        visible: Boolean,
        deleteOnServerSwap: Boolean,
        render: RenderInformation,
        doTracer: Boolean
    ) : this(
        pos,
        jsonTextToRender,
        renderDistance,
        visible,
        deleteOnServerSwap,
        mutableListOf<RenderInformation>(render),
        Color(1f, 1f, 1f),
        doTracer
    )

    constructor(
        pos: Position,
        jsonTextToRender: String,
        renderDistance: Int,
        visible: Boolean,
        deleteOnServerSwap: Boolean,
        render: RenderInformation,
        color: Color,
        doTracer: Boolean
    ) : this(
        pos,
        jsonTextToRender,
        renderDistance,
        visible,
        deleteOnServerSwap,
        mutableListOf<RenderInformation>(render),
        color,
        doTracer
    )

    constructor(
        pos: Position,
        jsonTextToRender: String,
        renderDistance: Int,
        visible: Boolean,
        deleteOnServerSwap: Boolean,
        render: MutableList<RenderInformation>?,
        doTracer: Boolean
    ) : this(pos, jsonTextToRender, renderDistance, visible, deleteOnServerSwap, render, Color(1f, 1f, 1f), doTracer)

    init {
        this.doTracer = doTracer
        if (jsonTextToRender.isEmpty()) {
            this.jsonToRenderText = "{\"text\":\"Unnamed\"}"
        } else {
            this.jsonToRenderText = jsonTextToRender
        }
    }
}
