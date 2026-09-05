package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.enoughupdates.NeuItemOverlayCandidate
import at.hannibal2.skyhanni.data.repo.RepoLogger
import at.hannibal2.skyhanni.data.repo.filesystem.MemoryRepoFileSystem
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import io.mockk.mockk
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.nbt.NbtOps
import net.minecraft.resources.RegistryOps
import net.minecraft.world.item.DyeColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File

class NeuItemOverlayTest {

    companion object {
        // Stands in for the registries the client receives from the server.
        private val registryOps by lazy { RegistryOps.create(NbtOps.INSTANCE, VanillaRegistries.createLookup()) }
    }

    @Test
    fun `selects highest overlay version not newer than world version`() {
        val selected = EnoughUpdatesManager.selectNeuItemOverlayCandidate(
            candidates = listOf(
                overlay("TEST", 100),
                overlay("TEST", 200),
                overlay("TEST", 300),
            ),
            worldDataVersion = 250,
        )

        assertEquals(200, selected?.version)
    }

    @Test
    fun `falls back to oldest overlay version when all versions are newer`() {
        val selected = EnoughUpdatesManager.selectNeuItemOverlayCandidate(
            candidates = listOf(
                overlay("TEST", 300),
                overlay("TEST", 100),
                overlay("TEST", 200),
            ),
            worldDataVersion = 50,
        )

        assertEquals(100, selected?.version)
    }

    @Test
    fun `overlay version selection is independent per item`() {
        val fileSystem = repoFileSystem(
            "itemsOverlay/100/ITEM_A.snbt" to validOverlay("minecraft:stone"),
            "itemsOverlay/300/ITEM_A.snbt" to validOverlay("minecraft:diamond"),
            "itemsOverlay/200/ITEM_B.snbt" to validOverlay("minecraft:gold_ingot"),
        )

        val candidates = EnoughUpdatesManager.run { fileSystem.findNeuItemOverlayCandidates() }
        val selectedA = EnoughUpdatesManager.selectNeuItemOverlayCandidate(candidates.getValue("ITEM_A"), 250)
        val selectedB = EnoughUpdatesManager.selectNeuItemOverlayCandidate(candidates.getValue("ITEM_B"), 250)

        assertEquals("itemsOverlay/100/ITEM_A.snbt", selectedA?.path)
        assertEquals("itemsOverlay/200/ITEM_B.snbt", selectedB?.path)
    }

    @Test
    fun `parses item id and component patch from overlay snbt without creating a stack`() {
        val overlay = parseOverlay(
            """
            {
                id:"minecraft:diamond",
                components:{
                    "minecraft:custom_data":{id:"TEST_ITEM",answer:42}
                }
            }
            """.trimIndent(),
        )

        val tag = overlay.componentPatch(registryOps).component(DataComponents.CUSTOM_DATA)?.copyTag()

        assertEquals("minecraft:diamond", overlay.itemId)
        assertNotNull(tag)
        assertEquals("TEST_ITEM", tag?.getString("id")?.orElse(""))
        assertEquals(42, tag?.getInt("answer")?.orElse(-1))
    }

    @Test
    fun `resolves components referencing the enchantment registry`() {
        val overlay = parseOverlay(
            """
            {
                id:"minecraft:enchanted_book",
                components:{
                    "minecraft:enchantments":{"minecraft:respiration":3}
                }
            }
            """.trimIndent(),
        )

        val enchantments = overlay.componentPatch(registryOps).component(DataComponents.ENCHANTMENTS)
        val enchantment = enchantments?.keySet()?.singleOrNull()

        assertEquals("minecraft:respiration", enchantment?.unwrapKey()?.get()?.identifier().toString())
        assertEquals(3, enchantment?.let { enchantments.getLevel(it) })
    }

    @Test
    fun `resolves components referencing the banner pattern registry`() {
        val overlay = parseOverlay(
            """
            {
                id:"minecraft:white_banner",
                components:{
                    "minecraft:banner_patterns":[
                        {color:"black",pattern:"minecraft:base"},
                        {color:"magenta",pattern:"minecraft:cross"}
                    ]
                }
            }
            """.trimIndent(),
        )

        val layers = overlay.componentPatch(registryOps).component(DataComponents.BANNER_PATTERNS)?.layers()

        assertEquals(2, layers?.size)
        assertEquals("minecraft:base", layers?.first()?.pattern()?.unwrapKey()?.get()?.identifier().toString())
        assertEquals(DyeColor.BLACK, layers?.first()?.color())
    }

    @Test
    fun `malformed overlay does not prevent another overlay from being parsed`() {
        val fileSystem = repoFileSystem(
            "itemsOverlay/100/BAD.snbt" to """{id:"minecraft:stone",components:{""",
            "itemsOverlay/100/GOOD.snbt" to validOverlay("minecraft:diamond"),
        )
        val candidates = EnoughUpdatesManager.run { fileSystem.findNeuItemOverlayCandidates() }

        val parsed = candidates.mapNotNull { (internalName, itemCandidates) ->
            val selected = EnoughUpdatesManager.selectNeuItemOverlayCandidate(itemCandidates, 100) ?: return@mapNotNull null
            val result = runCatching {
                parseOverlay(String(fileSystem.readAllBytes(selected.path), Charsets.UTF_8), selected.path)
            }.getOrNull()
            result?.let { internalName to it }
        }.toMap()

        assertFalse(parsed.containsKey("BAD"))
        assertEquals("minecraft:diamond", parsed["GOOD"]?.itemId)
    }

    private fun parseOverlay(snbt: String, path: String = "itemsOverlay/100/TEST.snbt") =
        EnoughUpdatesManager.parseNeuItemOverlay(snbt, path)

    // Patch values are stored under their own component type, so the value always matches the requested type.
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> DataComponentPatch?.component(type: DataComponentType<T>): T? =
        this?.entrySet()?.firstOrNull { it.key == type }?.value?.orElse(null) as? T

    private fun overlay(internalName: String, version: Int) = NeuItemOverlayCandidate(
        internalName = internalName,
        version = version,
        path = "itemsOverlay/$version/$internalName.snbt",
    )

    private fun validOverlay(itemId: String) = """
        {
            id:"$itemId",
            components:{
                "minecraft:custom_data":{id:"TEST_ITEM"}
            }
        }
    """.trimIndent()

    private fun repoFileSystem(vararg entries: Pair<String, String>) = MemoryRepoFileSystem(
        root = File("."),
        logger = mockk<RepoLogger>(relaxed = true),
        coroutineSettings = CoroutineSettings("neu-item-overlay-test"),
    ).also { fileSystem ->
        entries.forEach { (path, value) ->
            fileSystem.write(path, value.toByteArray(Charsets.UTF_8))
        }
    }
}
