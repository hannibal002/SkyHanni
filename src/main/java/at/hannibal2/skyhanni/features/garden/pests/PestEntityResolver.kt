package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.compat.getAllEquipment
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.time.Duration.Companion.seconds

object PestEntityResolver {

    data class LoadedPest(val entity: ArmorStand, val type: PestType) {
        val location: LorenzVec get() = entity.getLorenzVec().add(y = MODEL_HEIGHT_OFFSET)

        fun canBeSeen(viewDistance: Number): Boolean =
            entity.canBeSeen(viewDistance = viewDistance, vecYOffset = MODEL_HEIGHT_OFFSET)
    }

    private val pestTypeByTexture by RecalculatingValue(1.seconds) {
        PestType.filterableEntries.mapNotNull { type ->
            val texture = type.internalName.getItemStackOrNull()?.getSkullTexture() ?: return@mapNotNull null
            texture.normalizedBase64() to type
        }.toMap()
    }

    fun getPestType(entity: ArmorStand): PestType? {
        val type = entity.getAllEquipment()
            .firstNotNullOfOrNull { item ->
                item?.getSkullTexture()?.normalizedBase64()?.let(pestTypeByTexture::get)
            }
            ?: return null
        return type.takeUnless { entity.hasPetNameNearby(it) }
    }

    @OptIn(AllEntitiesGetter::class)
    fun getLoadedPests(): List<LoadedPest> = EntityUtils.getEntities<ArmorStand>()
        .mapNotNull { entity -> getPestType(entity)?.let { LoadedPest(entity, it) } }
        .toList()

    private fun ArmorStand.hasPetNameNearby(type: PestType): Boolean =
        getLorenzVec().getEntitiesNearby<ArmorStand>(PET_NAME_SEARCH_RADIUS).any {
            PET_NAME_PATTERN.matchEntire(it.cleanName)?.groups?.get("name")?.value == type.displayName
        }

    private fun String.normalizedBase64() = trim().trimEnd('=')

    private val PET_NAME_PATTERN = Regex("^\\[Lv ?[\\d,]+] ?(?<name>.+?)(?: ✦)?$")
    private const val MODEL_HEIGHT_OFFSET = 1.5
    private const val PET_NAME_SEARCH_RADIUS = 3.0
}
