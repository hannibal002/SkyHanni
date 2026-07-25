package at.hannibal2.skyhanni.data.model

import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.WaterFluid

abstract class OpaqueWaterFluid : WaterFluid() {

    object Flowing : OpaqueWaterFluid() {

        override fun createFluidStateDefinition(builder: StateDefinition.Builder<Fluid, FluidState>) {
            super.createFluidStateDefinition(builder)
            builder.add(LEVEL)
        }

        override fun getAmount(fluidState: FluidState): Int = fluidState.getValue(LEVEL)

        override fun isSource(fluidState: FluidState): Boolean = false
    }

    object Source : OpaqueWaterFluid() {

        override fun getAmount(fluidState: FluidState): Int = 8

        override fun isSource(fluidState: FluidState): Boolean = true
    }
}
