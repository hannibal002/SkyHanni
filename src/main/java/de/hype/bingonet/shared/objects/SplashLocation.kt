package de.hype.bingonet.shared.objects

class SplashLocation {
    @JvmField
    val coords: Position
    private val name: String?

    constructor(coords: Position, name: String?) {
        this.coords = coords
        this.name = name
    }

    constructor(name: String?, x: Int, y: Int, z: Int) {
        this.name = name
        coords = Position(x, y, z)
    }

    fun getName(): String {
        if (name == null) return coords.toString()
        return name
    }

    fun hasName(): Boolean {
        return !(name == null || name.isEmpty())
    }

    val splashLocation: SplashLocation
        get() = this

    val displayString: String
        get() {
            if (name.equals("bea", ignoreCase = true)) return "bea"
            return "$name (${coords.x} ${coords.y} ${coords.z})"
        }
}

