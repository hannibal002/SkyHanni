package at.hannibal2.skyhanni.utils.repopatterns

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.features.dev.RepoPatternConfig
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.events.PreInitFinishedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import net.minecraft.launchwrapper.Launch
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.io.File
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Manages [RepoPattern]s.
 */
object RepoPatternManager {

    val allPatterns: Collection<RepoPatternImpl> get() = usedKeys.values

    /**
     * Remote loading data that will be used to compile regexes from, once such a regex is needed.
     */
    private var regexes: RepoPatternDump? = null

    /**
     * Map containing the exclusive owner of a regex key
     */
    private var exclusivity: MutableMap<String, RepoPatternKeyOwner> = mutableMapOf()

    /**
     * Map containing all keys and their repo patterns. Used for filling in new regexes after an update, and for
     * checking duplicate registrations.
     */
    private var usedKeys = mutableMapOf<String, RepoPatternImpl>()

    private var wasPreinitialized = false
    private val isInDevEnv = try {
        Launch.blackboard["fml.deobfuscatedEnvironment"] as Boolean
    } catch (_: Exception) {
        true
    }

    private val insideTest = Launch.blackboard == null

    var inTestDuplicateUsage = true

    private val config
        get() = if (!insideTest) {
            SkyHanniMod.feature.dev.repoPattern
        } else {
            RepoPatternConfig().apply {
                tolerateDuplicateUsage = inTestDuplicateUsage
            }
        }

    val localLoading: Boolean get() = config.forceLocal.get() || (!insideTest && LorenzUtils.isInDevEnvironment())

    /**
     * Crash if in a development environment, or if inside a guarded event handler.
     */
    fun crash(reason: String) {
        if (isInDevEnv || LorenzEvent.isInGuardedEventHandler)
            throw RuntimeException(reason)
    }

    /**
     * Check that the [owner] has exclusive right to the specified [key], and locks out other code parts from ever
     * using that [key] again. Thread safe.
     */
    fun checkExclusivity(owner: RepoPatternKeyOwner, key: String) {
        synchronized(exclusivity) {
            val previousOwner = exclusivity.get(key)
            if (previousOwner != owner && previousOwner != null) {
                if (!config.tolerateDuplicateUsage)
                    crash("Non unique access to regex at \"$key\". First obtained by ${previousOwner.ownerClass} / ${previousOwner.property}, tried to use at ${owner.ownerClass} / ${owner.property}")
            } else {
                exclusivity[key] = owner
            }
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        loadPatternsFromDump(event.getConstant<RepoPatternDump>("regexes"))
    }

    fun loadPatternsFromDump(dump: RepoPatternDump) {
        regexes = null
        regexes = dump
        reloadPatterns()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.forceLocal.afterChange { reloadPatterns() }
    }

    /**
     * Reload patterns in [usedKeys] from [regexes] or their fallbacks.
     */
    private fun reloadPatterns() {
        val remotePatterns =
            if (localLoading) mapOf()
            else regexes?.regexes ?: mapOf()

        for (it in usedKeys.values) {
            val remotePattern = remotePatterns[it.key]
            try {
                if (remotePattern != null) {
                    it.compiledPattern = Pattern.compile(remotePattern)
                    it.wasLoadedRemotely = true
                    it.wasOverridden = remotePattern != it.defaultPattern
                    continue
                }
            } catch (e: PatternSyntaxException) {
                SkyHanniMod.logger.error("Error while loading pattern from repo", e)
            }
            it.compiledPattern = Pattern.compile(it.defaultPattern)
            it.wasLoadedRemotely = false
            it.wasOverridden = false
        }
    }

    val keyShape = Pattern.compile("^(?:[a-z0-9]+\\.)*[a-z0-9]+$")

    /**
     * Verify that a key has a valid shape or throw otherwise.
     */
    fun verifyKeyShape(key: String) {
        require(keyShape.matches(key)) { "pattern key: \"$key\" failed shape requirements" }
    }

    /**
     * Dump all regexes labeled with the label into the file.
     */
    fun dump(sourceLabel: String, file: File) {
        val data =
            ConfigManager.gson.toJson(
                RepoPatternDump(
                    sourceLabel,
                    usedKeys.values.associate { it.key to it.defaultPattern })
            )
        file.parentFile.mkdirs()
        file.writeText(data)
    }

    @SubscribeEvent
    fun onPreInitFinished(event: PreInitFinishedEvent) {
        wasPreinitialized = true
        val dumpDirective = System.getenv("SKYHANNI_DUMP_REGEXES")
        if (dumpDirective.isNullOrBlank()) return
        val (sourceLabel, path) = dumpDirective.split(":", limit = 2)
        dump(sourceLabel, File(path))
        if (System.getenv("SKYHANNI_DUMP_REGEXES_EXIT") != null) {
            SkyHanniMod.logger.info("Exiting after dumping RepoPattern regex patterns to $path")
            FMLCommonHandler.instance().exitJava(0, false)
        }
    }

    fun of(key: String, fallback: String): RepoPattern {
        verifyKeyShape(key)
        if (wasPreinitialized && !config.tolerateLateRegistration) {
            crash("Illegal late initialization of repo pattern. Repo pattern needs to be created during pre-initialization.")
        }
        if (key in usedKeys) {
            exclusivity[key] = RepoPatternKeyOwner(null, null)
            usedKeys[key]?.hasObtainedLock = false
        }
        return RepoPatternImpl(fallback, key).also { usedKeys[key] = it }
    }
}
