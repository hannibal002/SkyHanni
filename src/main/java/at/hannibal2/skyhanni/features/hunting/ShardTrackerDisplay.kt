package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.ItemResolutionQuery
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.other.SkyShardsExportData
import at.hannibal2.skyhanni.data.jsonobjects.other.SkyShardsExportJson
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeShardsData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemNameCompact
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.stackUnderCursor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import org.lwjgl.input.Keyboard
import java.util.zip.GZIPInputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@SkyHanniModule
object ShardTrackerDisplay {

    val config get() = SkyHanniMod.feature.hunting.shardTracker
    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled

    private var renderables: List<Renderable>? = null
    private val trackedShards get() = ProfileStorageData.profileSpecific?.hunting?.trackedAttributeShards ?: mutableMapOf()

    private fun toggleShard(neuId: NeuInternalName) {
        if (!AttributeShardsData.isAttributeShard(neuId)) {
            ErrorManager.logErrorStateWithData(
                "Error Getting Attribute Shard",
                "$neuId is not a valid attribute shard"
            )
        }
        val id = neuId.asString()
        if (trackedShards.contains(id)) {
            trackedShards.remove(id)
        } else {
            trackedShards[id] = -1
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if (!isEnabled()) return
        if (trackedShards.isEmpty()) {
            renderables = null
            return
        }
        val invCurrentlyOpen = InventoryUtils.inAnyInventory()
        val renderable = mutableListOf<Renderable>()
        for (shard in trackedShards) {
            val shardId = shard.key.toInternalName()
            val shardDisplayName = shardId.repoItemNameCompact.replace(" Shard", "")

            val shardName = AttributeShardsData.shardInternalNameToShardName(shardId)
            val amountInHuntingBox = AttributeShardsData.getAmountInHuntingBox(shardName)
            val amountUntilMax = if (shard.value == -1) AttributeShardsData.getAmountUntilMax(shardName) else shard.value

            if (amountUntilMax == 0) {
                renderable += Renderable.clickable(
                    " $shardDisplayName§7: §a$amountInHuntingBox",
                    onLeftClick = { toggleShard(shardId) },
                    tips = listOf("§cClick to remove from tracker")
                )
            } else {
                val color = if (amountInHuntingBox >= amountUntilMax) "§a" else if (amountInHuntingBox == 0) "§c" else "§e"
                renderable += Renderable.clickable(
                    " $shardDisplayName§7: $color$amountInHuntingBox§7/§a$amountUntilMax",
                    onLeftClick = { toggleShard(shardId) },
                    tips = listOf("§cClick to remove from tracker")
                )
            }
        }

        val list = mutableListOf<Renderable>(Renderable.text("§e§lAttribute Shard Tracker"))
        list += renderable
        if (invCurrentlyOpen) {
            list += Renderable.clickable(
                "§e[Import From SkyShards]",
                onLeftClick = ::importFromSkyShards,
                tips = listOf(
                    "Imports shard recipe exported from SkyShards",
                    "This will reset the currently tracked shards",
                    "You can also do §e/shimportskyshards§f to import shards"
                )
            )

            list += Renderable.clickable(
                "§c[Reset Display]",
                onLeftClick = ::clearTrackedShards,
                tips = listOf("This will reset the currently tracked shards and hide the display")
            )
        }
        renderables = list
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRender(event: GuiRenderEvent.GuiOnTopRenderEvent) {
        if (!isEnabled()) return
        renderables?.let {
            config.position.renderRenderables(it, posLabel = "Shard Tracker")
        }
    }

    private fun isInsideShardsMenu(): Boolean {
        return AttributeShardsData.attributeMenuInventory.isInside() ||
            AttributeShardsData.huntingBoxInventory.isInside() ||
            AttributeShardsData.isInFusionMachine()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onKeyPress(event: GuiKeyPressEvent) {
        if (!isEnabled()) return
        if (!config.selectShardKeybind.isKeyHeld()) return
        if (!isInsideShardsMenu()) return
        val stack = stackUnderCursor() ?: return
        val internalName = stack.getInternalName()
        if (internalName == NeuInternalName.NONE) return
        toggleShard(internalName)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltip(event: ToolTipEvent) {
        if (!isEnabled()) return
        if (!isInsideShardsMenu()) return
        if (config.selectShardKeybind == Keyboard.KEY_NONE) return
        if (!AttributeShardsData.isAttributeShard(event.itemStack.getInternalName())) return
        event.toolTip.add("§ePress ${KeyboardManager.getKeyName(config.selectShardKeybind)} to track this shard.")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shimportskyshards") {
            description = "Imports SkyShards material export"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback {
                importFromSkyShards()
            }
        }
    }

    private fun clearTrackedShards() {
        trackedShards.clear()
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun importFromSkyShards() {
        if (!isEnabled()) {
            ChatUtils.chatAndOpenConfig(
                "§cAttribute Shard Tracker is disabled. §eClick here to enable it",
                SkyHanniMod.feature.hunting.shardTracker::enabled,
            )
        }
        SkyHanniMod.launchCoroutine("reading skyshards data from clipboard") {
            val clipboard = OSUtils.readFromClipboard()
            if (clipboard == null) {
                ChatUtils.chat("Import from SkyShards failed, make sure you have a valid recipe copied.")
                return@launchCoroutine
            }
            val split = clipboard.split(":")
            val prefix = split.first()
            val suffix = split.getOrNull(1)
            if (!(prefix.startsWith("<NoFrillsRecipe>(V") || prefix.startsWith("<SkyHanniRecipe>(V")) || suffix == null) {
                ChatUtils.chat("Import from SkyShards failed, make sure you have a valid recipe copied.")
                return@launchCoroutine
            }
            val base = Base64.decode(suffix.trim())
            val data = GZIPInputStream(base.inputStream()).use { it.readBytes() }.decodeToString()
            val skyShardsData: List<SkyShardsExportData> = ConfigManager.gson.fromJson(data, SkyShardsExportJson.TYPE)
            clearTrackedShards()
            for (shardData in skyShardsData) {
                if (shardData.source != "Direct" && shardData.source != null) continue
                val shardName = ItemResolutionQuery.attributeNameToInternalName(shardData.name) ?: continue
                trackedShards[shardName] = shardData.needed
            }
        }
    }
}
