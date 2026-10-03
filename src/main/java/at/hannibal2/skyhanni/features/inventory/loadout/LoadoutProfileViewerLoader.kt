package at.hannibal2.skyhanni.features.inventory.loadout

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.MaxwellApi
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.other.DisplayInfo
import at.hannibal2.skyhanni.data.jsonobjects.other.HypixelApiEncodedItems
import at.hannibal2.skyhanni.data.jsonobjects.other.HypixelApiLoadout
import at.hannibal2.skyhanni.data.jsonobjects.other.HypixelApiPet
import at.hannibal2.skyhanni.data.jsonobjects.other.HypixelApiPlayer
import at.hannibal2.skyhanni.data.jsonobjects.other.NeuNbtInfoJson
import at.hannibal2.skyhanni.data.jsonobjects.other.PropertiesInfo
import at.hannibal2.skyhanni.data.jsonobjects.other.SkullOwnerInfo
import at.hannibal2.skyhanni.data.jsonobjects.other.TextureInfo
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.ProfileViewerDataLoadedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ComponentUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getStringList
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.compat.NbtCompat
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@SkyHanniModule
object LoadoutProfileViewerLoader {

    private val ARMOR_KEYS = listOf("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS")
    private val EQUIPMENT_KEYS = listOf("EQUIPMENT_SLOT_1", "EQUIPMENT_SLOT_2", "EQUIPMENT_SLOT_3", "EQUIPMENT_SLOT_4")
    private const val EQUIPPED_SET_KEY = "equipped_set"

    private val TUNING_VALUE_PER_POINT = mapOf(
        SkyblockStat.HEALTH to 5.0,
        SkyblockStat.DEFENSE to 1.0,
        SkyblockStat.SPEED to 1.5,
        SkyblockStat.STRENGTH to 1.0,
        SkyblockStat.INTELLIGENCE to 2.0,
        SkyblockStat.CRIT_DAMAGE to 1.0,
        SkyblockStat.CRIT_CHANCE to 0.2,
        SkyblockStat.BONUS_ATTACK_SPEED to 0.3,
    )

    private var loadedPV = false

    @HandleEvent
    private fun onProfileJoin() {
        loadedPV = false
    }

    @HandleEvent
    private fun onProfileViewerDataLoaded(event: ProfileViewerDataLoadedEvent) {
        if (loadedPV || LoadoutApi.loadedFromMenu) return
        val member = event.getCurrentPlayerData() ?: return
        val apiLoadout = member.loadout ?: return
        if (LoadoutApi.storage == null) return
        loadedPV = true

        try {
            loadLoadouts(member, apiLoadout)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e, "Error loading loadouts from profile viewer data",
                "loadouts" to apiLoadout.loadouts,
            )
        }
    }

    private fun loadLoadouts(member: HypixelApiPlayer, apiLoadout: HypixelApiLoadout) {
        val savedLoadouts = apiLoadout.loadouts?.mapNotNull { (key, loadout) ->
            val number = loadout.id?.takeIf { it != 0 } ?: key.toIntOrNull() ?: return@mapNotNull null
            number to loadout
        }.orEmpty()
        if (savedLoadouts.isEmpty()) return

        // Loadout numbers are 1-based, unless the api starts counting at 0
        val offset = if (savedLoadouts.any { it.first == 0 }) 0 else 1
        val equippedArmor = member.inventory?.armor.decodeItems()?.reversed()
        val equippedEquipment = member.inventory?.equipment.decodeItems()
        val apiPets = member.petsData?.pets.orEmpty()
        val tuningSlots = member.accessoryBagStorage?.tuning

        for ((number, loadout) in savedLoadouts) {
            val data = LoadoutApi.slots.find { it.id == number - offset }?.getData() ?: continue
            data.locked = false
            if (data.name == null) loadout.name?.takeIf { it.isNotBlank() }?.let { data.name = it }

            apiLoadout.armor.resolveSet(loadout.armorSetId, ARMOR_KEYS, equippedArmor)?.let { data.armor = it }
            apiLoadout.equipment.resolveSet(loadout.equipmentSetId, EQUIPMENT_KEYS, equippedEquipment)?.let { data.equipment = it }
            data.pet = loadout.petUuid?.takeIf { it.isNotBlank() }?.let { findPetStack(it, apiPets) }
            data.powerstone = loadout.powerStone?.takeIf { it.isNotBlank() }?.let {
                MaxwellApi.getPowerByApiIdOrNull(it) ?: it.replace('_', ' ').allLettersFirstUppercase()
            }
            data.tunings = tuningSlots?.getAsJsonObject("slot_${loadout.tuningPointsSlot ?: 0}")?.toTuningLines()
            data.hotm = loadout.miningCoreSelectedSlot?.takeIf { it > 0 }?.let { "Heart of the Mountain $it" }
            data.hotf = loadout.foragingCoreSelectedSlot?.takeIf { it > 0 }?.let { "Heart of the Forest $it" }
        }
    }

    private fun JsonObject?.resolveSet(setId: Int?, keys: List<String>, equippedItems: List<SafeItemStack?>?): List<SafeItemStack?>? {
        if (setId == null || setId == 0) return keys.map { null }
        if (this == null) return null
        if (get(EQUIPPED_SET_KEY)?.asInt == setId) return equippedItems?.takeIf { it.size == keys.size }

        val set = getAsJsonObject(setId.toString()) ?: return keys.map { null }
        return keys.map { key ->
            val encoded = set.getAsJsonObject(key)?.get("data")?.asString
            HypixelApiEncodedItems(type = null, data = encoded).decodeItems()?.firstOrNull()
        }
    }

    private fun findPetStack(petUuid: String, apiPets: List<HypixelApiPet>): SafeItemStack? {
        val uuid = runCatching { UUID.fromString(petUuid) }.getOrNull()
        ProfileStorageData.petProfiles?.pets?.firstOrNull { uuid != null && it.uuid == uuid }?.let {
            return it.getItemStackOrNull()
        }

        val apiPet = apiPets.firstOrNull { it.uniqueId == petUuid || it.uuid == petUuid } ?: return null
        val type = apiPet.type ?: return null
        val rarity = apiPet.tier?.let { LorenzRarity.getByName(it) } ?: return null
        return PetData(
            "$type;${rarity.id}".toInternalName(),
            skinInternalName = apiPet.skin?.let { "PET_SKIN_$it".toInternalName() },
            heldItemInternalName = apiPet.heldItem?.toInternalName(),
            exp = apiPet.exp,
            uuid = uuid,
        ).getItemStackOrNull()
    }

    private fun JsonObject.toTuningLines(): List<String>? = entrySet().mapNotNull { (key, value) ->
        val stat = SkyblockStat.getValueOrNull(key.uppercase()) ?: return@mapNotNull null
        val perPoint = TUNING_VALUE_PER_POINT[stat] ?: return@mapNotNull null
        val points = value.asInt.takeIf { it > 0 } ?: return@mapNotNull null
        val amount = (points * perPoint).roundTo(1).addSeparators()
        "${stat.color.getChatColor()}+$amount${stat.hypixelIcon} ${stat.displayName}"
    }.takeIf { it.isNotEmpty() }

    private fun HypixelApiEncodedItems?.decodeItems(): List<SafeItemStack?>? {
        val encoded = this?.data ?: return null
        val bytes = Base64.getDecoder().decode(encoded)
        val root = NbtIo.readCompressed(ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap())
        return NbtCompat.getCompoundTagList(root, "i").map { (it as? CompoundTag)?.toSkyBlockItemOrNull() }
    }

    private fun CompoundTag.toSkyBlockItemOrNull(): SafeItemStack? {
        val tag = getCompound("tag").getOrNull() ?: return null
        val extraAttributes = tag.getCompound("ExtraAttributes").getOrNull() ?: return null
        val internalName = extraAttributes.getString("id").getOrNull()?.toInternalName() ?: return null
        val stack = internalName.getItemStackOrNull()?.copy() ?: return null
        ComponentUtils.convertToComponents(stack, tag.toNeuNbtInfo(extraAttributes))
        return stack
    }

    private fun CompoundTag.toNeuNbtInfo(extraAttributes: CompoundTag): NeuNbtInfoJson {
        val display = getCompound("display").getOrNull()
        val skullOwner = getCompound("SkullOwner").getOrNull()?.let { owner ->
            val texture = owner.getCompound("Properties").getOrNull()
                ?.let { NbtCompat.getCompoundTagList(it, "textures").firstOrNull() as? CompoundTag }
            SkullOwnerInfo(
                uuid = owner.getString("Id").getOrNull(),
                properties = texture?.let {
                    PropertiesInfo(listOf(TextureInfo(it.getString("Value").getOrNull(), it.getString("Signature").getOrNull())))
                },
                hypixelPopulated = null,
                name = owner.getString("Name").getOrNull(),
            )
        }
        return NeuNbtInfoJson(
            hideFlags = null,
            unbreakable = null,
            skullOwner = skullOwner?.takeIf { it.uuid != null },
            display = display?.let {
                DisplayInfo(it.getString("Name").getOrNull(), it.getStringList("Lore"), it.getInt("color").getOrNull())
            },
            extraAttributes = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, extraAttributes).asJsonObject,
            explosion = null,
            customPotionEffects = null,
            enchantments = if (contains("ench")) listOf(JsonObject()) else null,
            itemModel = null,
            overrideMeta = null,
            generation = null,
            resolved = null,
        )
    }
}
