package de.hype.bingonet.shared.objects

class RenderInformation {
    var namespace: String = ""
    var pathToFile: String
    var spaceToNext: Int = 5

    /**
     * @param namespace       namespace like bingonet
     * @param textureFilePath path to file from assets fully with file ending → example: textures/gui/sprites/customitems/splash_hub.png
     */
    constructor(namespace: String = "", textureFilePath: String) {
        this.namespace = namespace
        this.pathToFile = textureFilePath
    }

    constructor(namespace: String = "", textureFilePath: String, spaceToNext: Int) {
        this.namespace = namespace
        this.pathToFile = textureFilePath
        this.spaceToNext = spaceToNext
    }

    val texturePath: String?
        get() {
            if (namespace.isEmpty()) return pathToFile
            return "$namespace:$pathToFile"
        }
}
