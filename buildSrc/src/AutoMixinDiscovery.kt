import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.apache.tools.ant.filters.BaseParamFilterReader
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.tasks.SourceSet
import java.io.File
import java.io.Reader
import java.io.StringReader

class MixinFilterReader(reader: Reader) : BaseParamFilterReader() {
    lateinit var sourceRoots: Collection<File>
    val gson = GsonBuilder().setPrettyPrinting().create()
    val betterReader: StringReader by lazy {
        StringReader(run {
            val json = gson.fromJson(reader.readText(), JsonObject::class.java)
            val mixinPackage = (json["package"] as JsonPrimitive).asString
            val allMixins = (json["mixins"] as JsonArray?)?.map { it.asString }?.toMutableSet() ?: mutableSetOf()
            sourceRoots
                .forEach { base ->
                    base.walk()
                        .filter { it.isFile }
                        .forEach {
                            val relativeString = it.toRelativeString(base).replace("\\", "/")
                            if (relativeString.startsWith(mixinPackage.replace(".", "/") + "/")
                                && relativeString.endsWith(".java")
                                && it.readText().contains("@Mixin")
                            )
                                allMixins.add(
                                    relativeString.replace("/", ".").dropLast(5).drop(mixinPackage.length + 1)
                                )
                        }
                }
            json.add("mixins", JsonArray().also { jsonAllMixins ->
                allMixins.forEach { jsonAllMixins.add(it) }
            })
            gson.toJson(json)
        })

    }

    override fun read(): Int {
        return betterReader.read()
    }
}

fun FileCopyDetails.autoDiscoverMixins(sourceSet: SourceSet) {
    filter(mapOf("sourceRoots" to sourceSet.allSource.srcDirs), MixinFilterReader::class.java)
}
