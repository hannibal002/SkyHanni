package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.EntityUtils.getWornSkullTexture
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.world.entity.decoration.ArmorStand

object PestEntityResolver {

    data class LoadedPest(val entity: ArmorStand, val type: PestType) {
        val location: LorenzVec get() = entity.getLorenzVec().add(y = MODEL_HEIGHT_OFFSET)

        fun canBeSeen(viewDistance: Number): Boolean =
            entity.canBeSeen(viewDistance = viewDistance, vecYOffset = MODEL_HEIGHT_OFFSET)
    }

    private val pestTypeByTexture by lazy {
        PestType.filterableEntries.mapNotNull { type ->
            val texture = type.internalName.getItemStackOrNull()?.getSkullTexture() ?: return@mapNotNull null
            texture to type
        }.toMap()
    }

    fun getPestType(entity: ArmorStand): PestType? =
        entity.getWornSkullTexture()?.let(pestTypeByTexture::get)

    @OptIn(AllEntitiesGetter::class)
    fun getLoadedPests(): List<LoadedPest> = EntityUtils.getEntities<ArmorStand>()
        .mapNotNull { entity -> getPestType(entity)?.let { LoadedPest(entity, it) } }
        .toList()

    private const val MODEL_HEIGHT_OFFSET = 1.5
}
