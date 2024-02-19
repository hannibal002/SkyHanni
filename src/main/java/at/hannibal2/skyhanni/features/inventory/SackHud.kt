package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SackAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getNBTName
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import io.github.moulberry.moulconfig.common.IItemStack
import io.github.moulberry.moulconfig.common.MyResourceLocation
import io.github.moulberry.moulconfig.forge.ForgeItemStack
import io.github.moulberry.moulconfig.gui.GuiContext
import io.github.moulberry.moulconfig.gui.GuiScreenElementWrapperNew
import io.github.moulberry.moulconfig.observer.ObservableList
import io.github.moulberry.moulconfig.xml.Bind
import io.github.moulberry.moulconfig.xml.XMLUniverse
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object SackHud {

    private val config get() = SkyHanniMod.feature.inventory.sackHud
    private var display: List<List<Any?>>? = null

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        val d = display ?: return
        config.position.renderStringsAndItems(d, posLabel = "Sack HUD")
    }

    @SubscribeEvent
    fun onSackChange(event: SackChangeEvent) {
        rebuildDisplay()
    }

    @SubscribeEvent
    fun onProfileSwitchEvent(event: ProfileJoinEvent) {
        rebuildDisplay()
    }

    fun rebuildDisplay() {
        display =
            config.trackedItems.map {
                val itemStack = it.getItemStack()
                val sackData = SackAPI.fetchSackItem(it)
                listOf(itemStack, "${itemStack.getNBTName()}§7: §b${sackData.amount}")
            }
    }

    @JvmStatic
    fun openConfigureScreen() {
        val editor = SackHudEditor()
        editor.entries.addAll(config.trackedItems.map {
            SackHudEntry(it, editor, it.getItemStack())
        })
        SkyHanniMod.screenToOpen = GuiScreenElementWrapperNew(
            GuiContext(
                XMLUniverse.getDefaultUniverse()
                    .load(editor, MyResourceLocation("skyhanni", "gui/sackhudeditor.xml"))
            )
        )
    }

    class SackHudEntry(
        val neuItem: NEUInternalName,
        val editor: SackHudEditor,
        item: ItemStack,
    ) {
        @field: Bind
        val icon: IItemStack = ForgeItemStack.of(item)

        @field: Bind
        val name: String = item.getNBTName() ?: "null"

        @Bind
        fun moveDown() {
            val idx = editor.entries.indexOf(this)
            if (idx == editor.entries.size - 1) return
            editor.entries.removeAt(idx)
            editor.entries.add(idx + 1, this)
            editor.save()
        }

        @Bind
        fun moveUp() {
            val idx = editor.entries.indexOf(this)
            if (idx == 0) return
            editor.entries.removeAt(idx)
            editor.entries.add(idx - 1, this)
            editor.save()
        }

        @Bind
        fun delete() {
            editor.entries.remove(this)
            editor.save()
        }
    }

    class PotentialEntry(
        val neuItem: NEUInternalName,
        val editor: SackHudEditor,
        val item: ItemStack,
    ) {

        @field: Bind
        val icon: IItemStack = ForgeItemStack.of(item)

        @field: Bind
        val name: String = item.getNBTName() ?: "null"

        @Bind
        fun addToList() {
            editor.entries.add(SackHudEntry(neuItem, editor, item))
            editor.save()
        }
    }


    class SackHudEditor {
        @field: Bind
        val entries = ObservableList(mutableListOf<SackHudEntry>())

        @field: Bind
        var searchField = ""
        var lastSearch: String? = null

        @field: Bind
        var results = ObservableList(mutableListOf<PotentialEntry>())

        @Bind
        fun update(): String {
            if (lastSearch == searchField)
                return ""
            lastSearch = searchField
            results.delegate.clear()
            for (itemId in SackAPI.getAllKnownSackItems()) {
                val item = itemId.getItemStackOrNull() ?: continue
                results.delegate.add(PotentialEntry(itemId, this, item))
            }
            results.delegate.retainAll {
                it.name.contains(searchField, ignoreCase = true)
            }
            results.update()
            return ""
        }

        fun save() {
            config.trackedItems = entries.map { it.neuItem }
            rebuildDisplay()
        }
    }
}
