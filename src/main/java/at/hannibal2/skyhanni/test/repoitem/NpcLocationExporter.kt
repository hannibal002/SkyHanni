package at.hannibal2.skyhanni.test.repoitem

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.dev.RepoItemEditorConfig
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobDebug
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.getEntityHelmet
import com.google.gson.JsonObject
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.BlockPos

@SkyHanniModule
object NpcLocationExporter {

    val config get(): RepoItemEditorConfig = SkyHanniMod.feature.dev.devTool.repoItemEditor

    private val defaultVillagerSkull: ItemStack by lazy {
        ItemUtils.createSkull(
            "villager",
            "7141ac94-8cd8-4a43-a8b4-48b16b19f2ea",
            "JERRY;5".toInternalName().getItemStack().getSkullTexture()!!,
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onKeyDown(event: KeyDownEvent) {
        if (!config.editModeEnabled) return
        if (event.keyCode != KeyboardManager.MIDDLE_MOUSE) return
        val apiIsland = SkyBlockUtils.currentIsland.islandData?.apiName ?: ""

        if (apiIsland.isBlank()) {
            ChatUtils.chat("§cCouldn't determine your current island!")
            return
        }

        val rayHitMob = MobDebug.lastRayHit ?: run {
            ChatUtils.chat("§cNo mob found under your crosshair!")
            return
        }
        if (rayHitMob.mobType != Mob.Type.DISPLAY_NPC) {
            ChatUtils.chat("§cYou can only export NPC locations!. Found Type: ${rayHitMob.mobType}, entity name: ${rayHitMob.name}")
            return
        }
        val repoNpc = processMobAsNpc(rayHitMob)
        RepoItemEditor.saveItemToRepo(repoNpc.npcInternalName, repoNpc.repoItemJson)
        ChatUtils.chat("§aSuccessfully exported NPC \"${repoNpc.npcName}\" to the repo!")
    }

    fun processMobAsNpc(mob: Mob): RepoNpcData {
        val island = SkyBlockUtils.currentIsland.islandData?.apiName ?: ""
        val npcEntity = mob.baseEntity
        val npcLocation = npcEntity.position

        if (npcEntity is EntityVillager) {
            return RepoNpcData(island, npcLocation, mob.name, defaultVillagerSkull)
        }
        if (npcEntity is AbstractClientPlayer) {
            val uuid = npcEntity.uniqueID.toString()
            //#if MC < 1.21
            val skin = npcEntity.locationSkin.resourcePath?.replace("skins/", "")
            //#else
            //$$ val skin = npcEntity.skinTextures.textureUrl?.replace("http://textures.minecraft.net/texture/", "")
            //#endif
            if (skin.isNullOrBlank()) {
                ErrorManager.skyHanniError("NPC has no skin", "mob" to mob)
            }
            return RepoNpcData(
                island,
                npcLocation,
                mob.name,
                ItemUtils.createSkullWithSkinUrl("", uuid, "https://textures.minecraft.net/texture/$skin"),
            )
        }
        if (npcEntity is net.minecraft.entity.EntityLiving) {
            return RepoNpcData(island, npcLocation, mob.name, npcEntity.getEntityHelmet() ?: ItemUtils.questionMarkSkull)
        }
        ErrorManager.skyHanniError("Unknown NPC type", "mob" to mob)
    }

    data class RepoNpcData(
        val island: String,
        val location: BlockPos,
        val npcName: String,
        val displayItem: ItemStack,
    ) {
        val npcInternalName = "${npcName.removeColor()} npc".toInternalName()

        val repoItemJson: JsonObject = createJson()

        private fun createJson(): JsonObject {
            val baseJson = EnoughUpdatesManager.getItemById(npcInternalName.asString()) ?: JsonObject()
            baseJson.addProperty("x", location.x)
            baseJson.addProperty("y", location.y)
            baseJson.addProperty("z", location.z)
            baseJson.addProperty("island", island)

            val npcDisplayName = "§9${npcName.removeColor()} (NPC)"

            val nbt = NBTTagCompound()
            //#if MC < 1.21
            nbt.setInteger("HideFlags", 254)

            val skullTexture = displayItem.tagCompound.getCompoundTag("SkullOwner")
            nbt.setTag("SkullOwner", skullTexture)

            val display = NBTTagCompound()
            display.setString("Name", npcDisplayName)
            display.setTag("Lore", NBTTagList())
            nbt.setTag("display", display)

            val extraAttributes = NBTTagCompound()
            extraAttributes.setString("id", npcInternalName.asString())
            nbt.setTag("ExtraAttributes", extraAttributes)
            //#else
            //$$ nbt.putInt("HideFlags", 254)
            //$$
            //$$ displayItem.components.get(net.minecraft.component.DataComponentTypes.PROFILE)?.let {
            //$$     val skullOwner = NbtCompound()
            //$$     skullOwner.putString("Id", it.id.get().toString())
            //$$     val properties = NbtCompound()
            //$$     val skullTexture = it.properties.get("textures").first()
            //$$     val textures = NbtCompound()
            //$$     if (skullTexture.hasSignature()) {
            //$$         textures.putString("Signature", skullTexture.signature)
            //$$     }
            //$$     textures.putString("Value", skullTexture.value)
            //$$
            //$$     properties.put(
            //$$         "textures",
            //$$         NbtList().apply {
            //$$             add(textures)
            //$$         },
            //$$     )
            //$$     skullOwner.put("Properties", properties)
            //$$     nbt.put("SkullOwner", skullOwner)
            //$$ }
            //$$
            //$$ val display = NbtCompound()
            //$$ display.putString("Name", npcDisplayName)
            //$$ display.put("Lore", NbtList())
            //$$ nbt.put("display", display)
            //$$
            //$$ val extraAttributes = NbtCompound()
            //$$ extraAttributes.putString("id", npcInternalName.asString())
            //$$ nbt.put("ExtraAttributes", extraAttributes)
            //#endif

            val newJson = RepoItemEditor.createRepoItemJson(
                baseJson,
                npcInternalName.asString(),
                "minecraft:skull",
                npcDisplayName,
                "",
                "",
                "",
                "WIKI_URL",
                "TODO",
                "",
                3,
                nbt,
            )
            return newJson
        }
    }
}
