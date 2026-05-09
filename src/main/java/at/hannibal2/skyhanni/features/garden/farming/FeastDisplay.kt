package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.getItemStackCopy
import at.hannibal2.skyhanni.features.garden.GardenNextJacobContest
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Duration.Companion.minutes
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal

@SkyHanniModule
object FeastDisplay {

    private val config get() = GardenApi.config.feastDisplay

    private val fetchMutex = Mutex()
    private var lastFetch = SimpleTimeMark.farPast()
    private var feastCrops: List<CropType> = listOf()

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (!GardenApi.inGarden()) return
        if (GardenApi.hideExtraGuis()) return

        maybeFetchFeastCrops()

        if (feastCrops.isEmpty()) return

        val display = getDisplay()
        config.position.renderRenderables(display, posLabel = "Feast Crops")
    }

    fun getDisplay(): List<Renderable> {
        if (feastCrops.isEmpty()) return listOf()

        val crops = feastCrops.take(3)

        return buildList {
            addString("§aFeast Crops")
            add(Renderable.horizontal {
                for (crop in crops) {
                    val cropStack = crop.getItemStackCopy("garden_feast:$crop")
                    add(Renderable.item(cropStack){
                        scale = 1.0
                    })
                }
            })
        }
    }

    private fun maybeFetchFeastCrops() {
        if (lastFetch.passedSince() < 10.minutes) return
        lastFetch = SimpleTimeMark.now()
        SkyHanniMod.launchIOCoroutineWithMutex("fetch feast crops", fetchMutex) {
            val fetched = EliteDevApi.fetchCurrentFeastCrops()
            if (fetched.isNotEmpty()) feastCrops = fetched
            lastFetch = SimpleTimeMark.now()
        }
    }
}
