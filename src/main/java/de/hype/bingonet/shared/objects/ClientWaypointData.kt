package de.hype.bingonet.shared.objects

import de.hype.bingonet.environment.packetconfig.EnvironmentPacketConfig
import java.awt.Color


open class ClientWaypointData @JvmOverloads constructor(
    pos: Position,
    jsonTextToRender: String,
    renderDistance: Int,
    visible: Boolean,
    deleteOnServerSwap: Boolean,
    render: MutableList<RenderInformation>?,
    color: Color = EnvironmentPacketConfig.defaultWaypointColor,
    doTracer: Boolean = EnvironmentPacketConfig.waypointDefaultWithTracer
) : WaypointData(pos, jsonTextToRender, renderDistance, visible, deleteOnServerSwap, render, color, doTracer) {
    var waypointId: Int = counter++
        protected set

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
        render: RenderInformation
    ) : this(
        pos,
        jsonTextToRender,
        renderDistance,
        visible,
        deleteOnServerSwap,
        mutableListOf<RenderInformation>(render),
        EnvironmentPacketConfig.defaultWaypointColor,
        EnvironmentPacketConfig.waypointDefaultWithTracer
    )


    companion object {
        var counter: Int = 0
    }
}
