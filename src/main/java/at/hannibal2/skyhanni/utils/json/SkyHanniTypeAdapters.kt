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
import at.hannibal2.skyhanni.features.misc.update.ModrinthVersionType
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.Stopwatch
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.system.MCVersion
import at.hannibal2.skyhanni.utils.system.ModVersion
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SessionUptimeTypeAdapter
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.mojang.serialization.JsonOps
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.world.item.ItemStack
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// Because we have an enum named UUID, we have to skirt the clash
private typealias J_UUID = UUID

/**
 * All entries are automatically registered in [BaseGsonBuilder.gson] via [GsonBuilder.registerSkyHanniAdapters]
 */
enum class SkyHanniTypeAdapters(
    val clazz: Class<*>,
    val adapter: TypeAdapter<*>,
) {
    UUID(
        J_UUID::class.java,
        SimpleStringTypeAdapter({ this.toString() }, { StringUtils.parseUUID(this) }),
    ),
    NBT_BOOLEAN(
        NbtBoolean::class.java,
        SimpleStringTypeAdapter({ this.asString() }, { NbtBoolean.fromString(this) }),
    ),
    VEC(
        LorenzVec::class.java,
        SimpleStringTypeAdapter(LorenzVec::asStoredString, LorenzVec::decodeFromString),
    ),
    TROPHY_RARITY(
        TrophyRarity::class.java,
        SimpleStringTypeAdapter(
            { name },
            { TrophyRarity.getByName(this) ?: error("Could not parse TrophyRarity from '$this'") },
        ),
    ),
    NEU_RECIPE_COMPONENT(
        NeuRecipeComponent::class.java,
        SimpleStringTypeAdapter(
            { this.toJsonString() },
            { NeuRecipeComponent.fromJsonStringOrNull(this) ?: NeuRecipeComponent.EMPTY },
        ),
    ),
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
        SimpleStringTypeAdapter(
            { neuRepoId.orEmpty() },
            { NeuRecipeType.fromNeuId(this) },
        ),
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
    INTERNAL_NAME(
        NeuInternalName::class.java,
        object : TypeAdapter<NeuInternalName>() {
            override fun write(writer: JsonWriter, value: NeuInternalName?) {
                if (value == null) writer.nullValue() else writer.value(value.asString())
            }

            override fun read(reader: JsonReader): NeuInternalName? {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    return null
                }
                return reader.nextString().toInternalName()
            }
        },
    ),
    RARITY(
        LorenzRarity::class.java,
        SimpleStringTypeAdapter.forEnum<LorenzRarity>(),
    ),
    ISLAND_TYPE(
        IslandType::class.java,
        SimpleStringTypeAdapter.forEnum<IslandType>(IslandType.UNKNOWN),
    ),
    CROP_TYPE(
        CropType::class.java,
        SimpleStringTypeAdapter.forEnum<CropType>(CropType.WHEAT),
    ),
    PEST_TYPE(
        PestType::class.java,
        SimpleStringTypeAdapter.forEnum<PestType>(PestType.UNKNOWN),
    ),
    MOD_VERSION(
        ModVersion::class.java,
        SimpleStringTypeAdapter(ModVersion::asString, ModVersion::fromString),
    ),
    MC_VERSION(
        MCVersion::class.java,
        SimpleStringTypeAdapter(MCVersion::asString, MCVersion::fromString),
    ),
    ELITE_LEADERBOARD_TYPE(
        EliteLeaderboardType::class.java,
        EliteLeaderboardTypeAdapter(),
    ),
    TRACKER_DISPLAY_MODE(
        SkyHanniTracker.DefaultDisplayMode::class.java,
        SimpleStringTypeAdapter.forEnum<SkyHanniTracker.DefaultDisplayMode>(),
    ),
    TIME_MARK(
        SimpleTimeMark::class.java,
        object : TypeAdapter<SimpleTimeMark>() {
            override fun write(out: JsonWriter, value: SimpleTimeMark) {
                out.value(value.toMillis())
            }

            override fun read(reader: JsonReader) = reader.nextLong().asTimeMark()
        },
    ),
    DURATION(
        Duration::class.java,
        object : TypeAdapter<Duration>() {
            override fun write(out: JsonWriter, value: Duration) {
                out.value(value.inWholeMilliseconds)
            }

            override fun read(reader: JsonReader) = reader.nextLong().milliseconds
        },
    ),
    STOPWATCH(
        Stopwatch::class.java,
        SimpleStringTypeAdapter(
            { this.getDuration().inWholeMilliseconds.toString() },
            {
                this.toLongOrNull()?.milliseconds?.let { Stopwatch(it) }
                    ?: error("Could not parse Stopwatch duration from '$this'")
            },
        ),
    ),
    LOCALE_DATE(
        LocalDate::class.java,
        object : TypeAdapter<LocalDate>() {
            override fun write(out: JsonWriter, value: LocalDate) {
                out.value(value.toString())
            }

            override fun read(reader: JsonReader): LocalDate = LocalDate.parse(reader.nextString())
        },
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
    GRAPH(Graph::class.java, Graph.typeAdapter),
    MODRINTH_VERSION_TYPE(
        ModrinthVersionType::class.java,
        SimpleStringTypeAdapter.forEnum<ModrinthVersionType>(),
    ),
}

@Suppress("UNCHECKED_CAST")
fun GsonBuilder.registerSkyHanniAdapters(): GsonBuilder = apply {
    SkyHanniTypeAdapters.entries.forEach {
        val adapter = (it.adapter as TypeAdapter<Any>).nullSafe()
        registerTypeAdapter(it.clazz, adapter)
    }
}
