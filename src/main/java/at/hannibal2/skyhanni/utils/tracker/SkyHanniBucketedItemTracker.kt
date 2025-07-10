package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addNullableButton
import at.hannibal2.skyhanni.utils.renderables.Searchable

@Suppress("SpreadOperator")
class SkyHanniBucketedItemTracker<E : Enum<E>, BucketedData : BucketedItemTrackerData<E>>(
    name: String,
    createNewSession: () -> BucketedData,
    getStorage: (ProfileSpecificStorage) -> BucketedData,
    drawDisplay: (BucketedData) -> List<Searchable>,
    extraDisplayModes: Map<DisplayMode, (ProfileSpecificStorage) -> BucketedData> = emptyMap(),
) : SkyHanniItemTracker<BucketedData>(name, createNewSession, getStorage, extraDisplayModes, drawDisplay = drawDisplay) {

    @Deprecated(
        "Use addCoins(bucket, coins, command) instead",
        ReplaceWith("addCoins(bucket, coins, command)")
    )
    override fun addCoins(amount: Int, command: Boolean) =
        throw UnsupportedOperationException("Use addCoins(bucket, coins, command) instead")

    fun addCoins(bucket: E, coins: Int, command: Boolean) {
        addItem(bucket, SKYBLOCK_COIN, coins, command)
    }

    override fun ItemAddEvent.addItemFromEvent() {
        val command = source == ItemAddManager.Source.COMMAND
        lateinit var bucket: E
        modify { data ->
            bucket = data.selectedBucket ?: run {
                ChatUtils.userError(
                    "No bucket selected for §b$name§c.\n§cSelect one in the §b$name §cGUI, then try again.",
                )
                cancel()
                return@modify
            }
            data.addItem(bucket, internalName, amount, command)
            logCompletedAddEvent()
        }
    }

    @Deprecated(
        "Use addItem(bucket, internalName, amount, command, message) instead",
        ReplaceWith("addItem(bucket, internalName, amount, command, message)"),
    )
    override fun addItem(internalName: NeuInternalName, amount: Int, command: Boolean, message: Boolean) =
        throw UnsupportedOperationException("Use addItem(bucket, internalName, amount, command, message) instead")

    fun addItem(bucket: E, internalName: NeuInternalName, amount: Int, command: Boolean, message: Boolean = true) {
        modify {
            it.addItem(bucket, internalName, amount, command)
        }
        getSharedTracker()?.let {
            val totalProp = it.get(DisplayMode.TOTAL).selectedBucketItems.getOrPut(internalName) {
                ItemTrackerData.TrackedItem()
            }
            val sessionProp = it.get(DisplayMode.SESSION).selectedBucketItems.getOrPut(internalName) {
                ItemTrackerData.TrackedItem()
            }
            sessionProp.hidden = totalProp.hidden
        }

        if (command) logCommandAdd(internalName, amount)
        handlePossibleRareDrop(internalName, amount, message)
    }

    fun addBucketSelector(
        lists: MutableList<Searchable>,
        data: BucketedData,
        sourceLabel: String,
        nullBucketLabel: String = "All",
    ) {
        if (isInventoryOpen()) {
            lists.addNullableButton(
                label = sourceLabel,
                current = data.selectedBucket,
                onChange = { new ->
                    modifyEachMode {
                        it.selectedBucket = new
                    }
                },
                universe = data.selectableBuckets,
                nullLabel = nullBucketLabel,
            )
        }
    }

    override fun drawItems(
        data: BucketedData,
        filter: (NeuInternalName) -> Boolean,
        lists: MutableList<Searchable>,
        itemsAccessor: () -> Map<NeuInternalName, ItemTrackerData.TrackedItem>,
        getCoinName: (ItemTrackerData.TrackedItem) -> String,
        itemRemover: (NeuInternalName, String) -> Unit,
        itemHider: (NeuInternalName, Boolean) -> Unit,
        getLoreList: (NeuInternalName, ItemTrackerData.TrackedItem) -> List<String>,
    ): Double = super.drawItems(
        data = data,
        filter = filter,
        lists = lists,
        itemsAccessor = { data.selectedBucketItems },
        getCoinName = { item ->
            data.getCoinName(data.selectedBucket, item)
        },
        itemRemover = { internalName, cleanName ->
            modify {
                it.removeItem(data.selectedBucket, internalName)
            }
            ChatUtils.chat("Removed $cleanName §efrom $name.")
        },
        itemHider = { internalName, currentlyHidden ->
            modify {
                it.toggleItemHide(data.selectedBucket, internalName, currentlyHidden)
            }
        },
        getLoreList = { internalName, item ->
            val selectedBucket = data.selectedBucket
            if (internalName == SKYBLOCK_COIN) data.getCoinDescription(selectedBucket, item)
            else data.getDescription(selectedBucket, item.timesGained)
        },
    )
}
