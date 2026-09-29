// Naming is intentional
@file:Suppress("FunctionName")

package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket

interface ParticleSuppressionStore {
    fun `skyhanni$shouldSuppress`(): Boolean = throw UnsupportedOperationException("Implemented via mixin")

    fun `skyhanni$setShouldSuppress`(value: Boolean) {
        throw UnsupportedOperationException("Implemented via mixin")
    }

    companion object {
        var ClientboundLevelParticlesPacket.shouldSuppress: Boolean
            get() = `skyhanni$shouldSuppress`()
            set(value) {
                `skyhanni$setShouldSuppress`(value)
            }
    }
}
