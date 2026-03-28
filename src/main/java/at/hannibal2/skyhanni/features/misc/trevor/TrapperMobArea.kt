package at.hannibal2.skyhanni.features.misc.trevor

import net.minecraft.world.phys.Vec3

// TODO use repo
enum class TrapperMobArea(val location: String, val coordinates: Vec3) {
    OASIS("Oasis", Vec3(126.0, 77.0, -456.0)),
    GORGE("Mushroom Gorge", Vec3(300.0, 80.0, -509.0)),
    OVERGROWN("Overgrown Mushroom Cave", Vec3(242.0, 60.0, -389.0)),
    SETTLEMENT("Desert Settlement", Vec3(184.0, 86.0, -384.0)),
    GLOWING("Glowing Mushroom Cave", Vec3(199.0, 50.0, -512.0)),
    MOUNTAIN("Desert Mountain", Vec3(255.0, 148.0, -518.0)),
    FOUND("    ", Vec3.ZERO),
    NONE("   ", Vec3.ZERO),
}
