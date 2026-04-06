package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardTypeAdapter
import at.hannibal2.skyhanni.data.jsonobjects.other.NbtBoolean
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NEURaritySpecificPetNums
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetNums
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuAbstractRecipe
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeComponent
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeType
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.data.model.SkyblockStatList
import at.hannibal2.skyhanni.data.model.graph.Graph
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.Stopwatch
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.system.ModVersion
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SessionUptimeTypeAdapter
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.mojang.serialization.JsonOps
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.world.item.ItemStack
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.full.companionObjectInstance
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// Because we have an enum named UUID, we have to skirt the clash
private typealias J_UUID = UUID

/**
 * Implement on types used as [SkyHanniTypeAdapter] entries via the `(Class<out SkyHanniAdaptable<*>>)` constructor.
 * The companion object must implement [Factory] to provide the deserialization half.
 */
interface SkyHanniAdaptable<T : Any> {
    fun toJsonString(): String

    interface Factory<T : Any> {
        fun fromJsonString(json: String): T
    }
}

@Suppress("UNCHECKED_CAST")
private fun adaptableAdapter(clazz: Class<out SkyHanniAdaptable<*>>): TypeAdapter<*> {
    val factory = clazz.kotlin.companionObjectInstance as? SkyHanniAdaptable.Factory<Any>
        ?: error("${clazz.simpleName} companion must implement SkyHanniAdaptable.Factory")
    return object : TypeAdapter<Any>() {
        override fun write(writer: JsonWriter, value: Any) {
            writer.value((value as SkyHanniAdaptable<*>).toJsonString())
        }

        override fun read(reader: JsonReader): Any = factory.fromJsonString(reader.nextString())
    }
}

@Suppress("UNCHECKED_CAST")
private fun enumAdapter(clazz: Class<out Enum<*>>, default: Enum<*>? = null): TypeAdapter<*> {
    val constants = clazz.enumConstants as Array<Enum<*>>
    return SimpleStringTypeAdapter<Any>(
        { (this as Enum<*>).name },
        { constants.firstOrNull { it.name == this } ?: default ?: error("Unknown ${clazz.simpleName} value: '$this'") },
    )
}

/**
 * All entries are automatically registered in [BaseGsonBuilder.gson] via [GsonBuilder.registerSkyHanniAdapters]
 */
enum class SkyHanniTypeAdapter(
    val clazz: Class<*>,
    val adapter: TypeAdapter<*>,
) {
    UUID(
        J_UUID::class.java,
        SimpleStringTypeAdapter(J_UUID::toString, StringUtils::parseUUID),
    ),
    NBT_BOOLEAN(NbtBoolean::class.java),
    VEC(LorenzVec::class.java),
    TROPHY_RARITY(TrophyRarity::class.java),
    NEU_RECIPE_COMPONENT(NeuRecipeComponent::class.java),
    NEU_ABSTRACT_RECIPE(
        NeuAbstractRecipe::class.java,
        object : TypeAdapter<NeuAbstractRecipe>() {
            override fun write(writer: JsonWriter, value: NeuAbstractRecipe) {
                writer.value(value.toString())
            }

            override fun read(reader: JsonReader): NeuAbstractRecipe {
                val obj = JsonParser.parseReader(reader).asJsonObject
                val recipeType = NeuRecipeType.fromNeuIdOrNull(obj.get("type").asString)
                    ?: throw IllegalArgumentException("Unknown recipe type: ${obj.get("type").asString}")
                return ConfigManager.gson.fromJson(obj, recipeType.castClazz)
            }
        },
    ),
    NEU_RECIPE_TYPE(
        NeuRecipeType::class.java,
        SimpleStringTypeAdapter(NeuRecipeType::repoIdOrEmpty, NeuRecipeType::fromNeuId),
    ),
    NEU_RARITY_SPECIFIC_PET_NUMS(
        NEURaritySpecificPetNums::class.java,
        object : TypeAdapter<NEURaritySpecificPetNums>() {
            override fun write(writer: JsonWriter, value: NEURaritySpecificPetNums) {
                writer.value(value.toString())
            }

            override fun read(reader: JsonReader): NEURaritySpecificPetNums {
                val obj = JsonParser.parseReader(reader).asJsonObject
                val neuPetNumsAdapter = ConfigManager.gson.getAdapter(NeuPetNums::class.java)
                return NEURaritySpecificPetNums(
                    min = neuPetNumsAdapter.fromJsonTree(obj.getAsJsonObject("1")),
                    max = neuPetNumsAdapter.fromJsonTree(obj.getAsJsonObject("100")),
                    levelCurveString = obj.get("stats_levelling_curve")?.asString,
                )
            }
        },
    ),
    NEU_ITEMSTACK(
        ItemStack::class.java,
        SimpleStringTypeAdapter(NeuItems::saveNBTData, NeuItems::loadNBTData),
    ),
    INTERNAL_NAME(NeuInternalName::class.java),
    RARITY(LorenzRarity::class.java),
    ISLAND_TYPE(IslandType::class.java, IslandType.UNKNOWN),
    CROP_TYPE(CropType::class.java, CropType.WHEAT),
    PEST_TYPE(PestType::class.java, PestType.UNKNOWN),
    MOD_VERSION(ModVersion::class.java),
    ELITE_LEADERBOARD_TYPE(
        EliteLeaderboardType::class.java,
        EliteLeaderboardTypeAdapter(),
    ),
    TRACKER_DISPLAY_MODE(SkyHanniTracker.DefaultDisplayMode::class.java),
    TIME_MARK(SimpleTimeMark::class.java),
    DURATION(
        Duration::class.java,
        SimpleLongTypeAdapter(Duration::inWholeMilliseconds) { milliseconds }
    ),
    STOPWATCH(Stopwatch::class.java),
    LOCALE_DATE(
        LocalDate::class.java,
        SimpleStringTypeAdapter(LocalDate::toString, LocalDate::parse),
    ),
    SESSION_UPTIME(SessionUptime::class.java, SessionUptimeTypeAdapter()),
    COMPONENT(
        Component::class.java,
        object : TypeAdapter<Component>() {
            override fun write(out: JsonWriter, value: Component) {
                out.jsonValue(ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, value).getOrThrow().toString())
            }

            override fun read(reader: JsonReader): Component =
                ComponentSerialization.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow().first
        },
    ),
    SKYBLOCK_STAT(
        SkyblockStat::class.java,
        SimpleStringTypeAdapter(
            { name.lowercase() },
            { SkyblockStat.getValue(this.uppercase()) },
        ),
    ),
    SKYBLOCK_STAT_LIST(
        SkyblockStatList::class.java,
        object : TypeAdapter<SkyblockStatList>() {
            override fun write(out: JsonWriter, value: SkyblockStatList) {
                out.beginObject()
                value.entries.forEach {
                    out.name(it.key.name.lowercase()).value(it.value)
                }
                out.endObject()
            }

            override fun read(reader: JsonReader): SkyblockStatList = SkyblockStatList().apply {
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    val value = reader.nextDouble()
                    val stat = SkyblockStat.getValueOrNull(name.uppercase()) ?: run {
                        ErrorManager.logErrorStateWithData(
                            "Unknown stat: '${name.uppercase()}'",
                            "Stat list could not parse stat",
                            "failed" to name.uppercase(),
                            betaOnly = true,
                        )
                        continue
                    }
                    this[stat] = value
                }
                reader.endObject()
            }
        },
    ),
    GRAPH(Graph::class.java),
    ;

    /** Shorthand constructor for plain enum adapters. Disambiguated from the primary by [Enum] vs [TypeAdapter] second arg. */
    constructor(enumClass: Class<out Enum<*>>, default: Enum<*>? = null) : this(enumClass, enumAdapter(enumClass, default))

    /** Shorthand constructor for [SkyHanniAdaptable] types. Companion must implement [SkyHanniAdaptable.Factory]. */
    constructor(adaptableClass: Class<out SkyHanniAdaptable<*>>) : this(adaptableClass, adaptableAdapter(adaptableClass))
}

@Suppress("UNCHECKED_CAST")
fun GsonBuilder.registerSkyHanniAdapters(): GsonBuilder = apply {
    SkyHanniTypeAdapter.entries.forEach {
        val adapter = (it.adapter as TypeAdapter<Any>).nullSafe()
        registerTypeAdapter(it.clazz, adapter)
    }
}
