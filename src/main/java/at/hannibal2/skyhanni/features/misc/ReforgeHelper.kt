package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.inventory.Container
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.EnumMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val specialSpaceNumber = "\u2001"
private const val specialSpaceSign = " "

fun Int.toStringWithPlus() = (if (this >= 0) "+" else "") + this.toString()

class ReforgeHelper {

    enum class ReforgeType {
        Swords, Bows, Armor, Chestplates, Helmets, Cloaks, Axes, Hoes, HoeAndAxes, Pickaxes, Equipments, FishingRods, FishingRodsAndSwords, SpecialItems, Vacuums
    }

    enum class StatType(val icon: String) {
        Damage("§c❁"),
        Health("§c❤"),
        Defence("§a❈"),
        Strength("§c❁"),
        Intelligence("§b✎"),
        Crit_Damage("§9☠"),
        Crit_Chance("§9☣"),
        Ferocity("§c⫽"),
        Attack_Speed("§e⚔"),
        Ability_Damage("§c๑"),
        Health_Regen("§c❣"),
        Vitality("§4♨"),
        Mending("§a☄"),
        True_Defence("§7❂"),
        Swing_Range("§eⓈ"),
        Speed("§f✦"),
        Sea_Creature_Chance("§3α"),
        MagicFind("§b✯"),
        PetLuck("§d♣"),
        Fishing_Speed("§b☂"),
        Bonus_Pest_Chance("§2ൠ"),
        Combat_Wisdom("§3☯"),
        Mining_Wisdom("§3☯"),
        Farming_Wisdom("§3☯"),
        Foraging_Wisdom("§3☯"),
        Fishing_Wisdom("§3☯"),
        Enchanting_Wisdom("§3☯"),
        Alchemy_Wisdom("§3☯"),
        Carpentry_Wisdom("§3☯"),
        Runecrafting_Wisdom("§3☯"),
        Social_Wisdom("§3☯"),
        Taming_Wisdom("§3☯"),
        Mining_Speed("§6⸕"),
        Breaking_Power("§2Ⓟ"),
        Pristine("§5✧"),
        Foraging_Fortune("§☘"),
        Farming_Fortune("§6☘"),
        Mining_Fortune("§6☘"),
        Fear("§a☠")
        ;

        val iconWithName = icon + " " + name.replace("_", " ")

        fun asString(value: Int) = (if (value > 0) "+" else "") + value.toString() + " " + this.icon
    }


    class StatList : EnumMap<StatType, Int>(StatType::class.java) {
        operator fun minus(other: StatList): StatList {
            return StatList().apply {
                for ((key, value) in this@StatList) {
                    this[key] = value - (other[key] ?: 0)
                }
                for ((key, value) in other) {
                    if (this[key] == null) {
                        this[key] = (this@StatList[key] ?: 0) - value
                    }
                }
            }
        }

        fun print(current: StatList?): List<String> {
            val numbersInSpaces = 2
            val fontRender = Minecraft.getMinecraft().fontRendererObj
            val diff = current?.let { this - it }
            return listOf("§6Reforge Stats") + (diff?.mapNotNull {
                val value = it.value
                val key = it.key
                if (key == null || value == null) return@mapNotNull null
                buildString {
                    append("§9")
                    append((this@StatList[key] ?: 0).toStringWithPlus())
                    LorenzDebug.log((fontRender.getStringWidth(this.toString()).toFloat() / fontRender.getStringWidth(" ").toFloat()).toString())
                    while (this.length < 8) {
                        append(specialSpaceNumber)
                    }
                    append(if (value < 0) "§c" else "§a+")
                    append(value)
                    while (this.length < 16) {
                        append(specialSpaceNumber)
                    }
                    append(" ")
                    append(key.iconWithName)
                }
            } ?: this.mapNotNull {
                val value = it.value
                val key = it.key
                if (key == null || value == null) return@mapNotNull null
                buildString {
                    append("§9")
                    append(value.toStringWithPlus())
                    while (this.length < 8) {
                        append(specialSpaceNumber)
                    }
                    append(" ")
                    append(key.iconWithName)
                }
            })
        }


        companion object {
            fun mapOf(vararg list: Pair<StatType, Int>) = StatList().apply {
                for ((key, value) in list) {
                    this[key] = value
                }
            }
        }
    }

    class Reforge(val name: String, val type: ReforgeType, val stats: Map<LorenzRarity, StatList>, val specialItems: List<NEUInternalName> = emptyList()) {

        fun isValid(itemStack: ItemStack) = isValid(itemStack.getItemCategoryOrNull(), itemStack.getInternalName())

        fun isValid(itemCategory: ItemCategory?, internalName: NEUInternalName) =
            when (type) {
                ReforgeType.Swords -> setOf(ItemCategory.SWORD, ItemCategory.GAUNTLET, ItemCategory.LONGSWORD, ItemCategory.FISHING_WEAPON).contains(itemCategory)
                ReforgeType.Bows -> itemCategory == ItemCategory.BOW || itemCategory == ItemCategory.SHORT_BOW
                ReforgeType.Armor -> setOf(ItemCategory.HELMET, ItemCategory.CHESTPLATE, ItemCategory.LEGGINGS, ItemCategory.BOOTS).contains(itemCategory)
                ReforgeType.Chestplates -> itemCategory == ItemCategory.CHESTPLATE
                ReforgeType.Helmets -> itemCategory == ItemCategory.HELMET
                ReforgeType.Cloaks -> itemCategory == ItemCategory.CLOAK
                ReforgeType.Axes -> itemCategory == ItemCategory.AXE
                ReforgeType.Hoes -> itemCategory == ItemCategory.HOE
                ReforgeType.HoeAndAxes -> itemCategory == ItemCategory.HOE || itemCategory == ItemCategory.AXE
                ReforgeType.Pickaxes -> itemCategory == ItemCategory.PICKAXE || itemCategory == ItemCategory.DRILL || itemCategory == ItemCategory.GAUNTLET
                ReforgeType.Equipments -> setOf(ItemCategory.CLOAK, ItemCategory.BELT, ItemCategory.NECKLACE, ItemCategory.BRACELET, ItemCategory.GLOVES).contains(itemCategory)
                ReforgeType.FishingRods -> itemCategory == ItemCategory.FISHING_ROD || itemCategory == ItemCategory.FISHING_WEAPON
                ReforgeType.FishingRodsAndSwords -> setOf(ItemCategory.SWORD, ItemCategory.GAUNTLET, ItemCategory.LONGSWORD, ItemCategory.FISHING_ROD, ItemCategory.FISHING_WEAPON).contains(itemCategory)
                ReforgeType.Vacuums -> itemCategory == ItemCategory.VACUUM
                ReforgeType.SpecialItems -> specialItems.contains(internalName)
            }

    }

    private val reforges = listOf(
        Reforge("Clean", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to StatList.mapOf(StatType.Strength to 200))),
        Reforge("Fierce", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to StatList.mapOf(StatType.Strength to 8))),
        Reforge("Heavy", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to StatList.mapOf(StatType.Strength to 7))),
        Reforge("Light", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to StatList.mapOf(StatType.Strength to 6))),
        Reforge("Mythic", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to StatList.mapOf(StatType.Strength to 5))),
        Reforge("Pure", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to StatList.mapOf(StatType.Strength to 4))),
        Reforge("Smart", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to StatList.mapOf(StatType.Health to 4, StatType.Defence to 4, StatType.Intelligence to 20))),
        Reforge("Titanic", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to StatList.mapOf(StatType.Health to 10, StatType.Defence to 10))),
        Reforge("Wise", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to StatList.mapOf(StatType.Strength to 1))),
    )

    val reforgeMenu by RepoPattern.pattern("menu.reforge", "Reforge Item")
    val reforgeHexMenu by RepoPattern.pattern("menu.reforge.hex", "The Hex ➜ Reforges")
    val reforgeChatMessage by RepoPattern.pattern("chat.reforge.message", "§aYou reforged your .* §r§ainto a .*!")
    val reforgeChatFail by RepoPattern.pattern("chat.reforge.fail", "§cWait a moment before reforging again!")

    var isInReforgeMenu = false
    var isInHexReforgeMenu = false

    fun isReforgeMenu(chestName: String) = reforgeMenu.matches(chestName)
    fun isHexReforgeMenu(chestName: String) = reforgeHexMenu.matches(chestName)

    val posList: Position = Position(-200, 85, true, true)
    val posCurrent: Position = Position(280, 45, true, true)

    fun enable() = LorenzUtils.inSkyBlock && isInReforgeMenu

    var item: ItemStack? = null
    var inventory: Container? = null

    var currentReforge: String = ""
        set(value) {
            field = value
            currentReforgeCapitalized = value.replaceFirstChar { it.uppercase() }
            formattedCurrentReforge = if (value.isEmpty()) "" else "§7Now:  §3${currentReforgeCapitalized}"
        }
    var reforgeToSearch: String = ""
        set(value) {
            field = value
            formattedReforgeToSearch = if (value.isEmpty()) "" else "§7Goal: §9${value.replaceFirstChar { it.uppercase() }}"
        }

    var currentReforgeCapitalized = ""

    var formattedCurrentReforge = ""
    var formattedReforgeToSearch = ""

    val reforgeItem get() = if (isInHexReforgeMenu) 19 else 13
    val reforgeButton get() = if (isInHexReforgeMenu) 48 else 22

    var waitForChat = AtomicBoolean(false)
    var waitDelay = false

    fun itemUpdate() {
        item = inventory?.getSlot(reforgeItem)?.stack
        currentReforge = item?.getReforgeName() ?: ""
    }

    @SubscribeEvent
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!enable()) return
        if (event.slot?.slotNumber == reforgeButton) {
            if (event.slot.stack?.name == "§eReforge Item" || event.slot.stack?.name == "§cError!") return
            if (currentReforge == reforgeToSearch) {
                event.isCanceled = true
                waitForChat.set(false)
            } else
                if (waitForChat.get()) {
                    waitDelay = true
                    event.isCanceled = true
                } else {
                    if (event.clickedButton == 2) return
                    if (waitDelay) {
                        waitDelay = false
                    } else {
                        waitForChat.set(true)
                    }
                }
        }

        DelayedRun.runNextTick {
            itemUpdate()
            updateDisplay()
        }
    }

    inline val Int.tick get() = (this * 50 * 2).toDuration(DurationUnit.MILLISECONDS) // Remove the x2 when NeaTickEvent is implemented

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!enable()) return
        when {
            reforgeChatMessage.matches(event.message) -> {
                DelayedRun.runDelayed(2.tick) {
                    itemUpdate()
                    waitForChat.set(false)
                }
            }

            reforgeChatFail.matches(event.message) -> {
                DelayedRun.runDelayed(2.tick) {
                    waitForChat.set(false)
                }
            }
        }
    }

    @SubscribeEvent
    fun onOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        when {
            isHexReforgeMenu(event.inventoryName) -> {
                isInHexReforgeMenu = true
                DelayedRun.runDelayed(2.tick) {
                    itemUpdate()
                    updateDisplay()
                }
            }

            isReforgeMenu(event.inventoryName) -> {
                item = null
                currentReforge = ""
            }

            else -> return
        }
        isInReforgeMenu = true
        reforgeToSearch = ""
        waitForChat.set(false)
        DelayedRun.runNextTick {
            inventory = Minecraft.getMinecraft().thePlayer.openContainer
        }
    }

    @SubscribeEvent
    fun onClose(event: InventoryCloseEvent) {
        if (!enable()) return
        isInReforgeMenu = false
        isInHexReforgeMenu = false
    }

    fun generateDisplay() = buildList<Renderable> {
        this.add(Renderable.string("§6Reforge Overlay"))
        val item = item ?: return@buildList
        val itemType = ReforgeType.Armor
        val itemRarity = item.getItemRarityOrNull()
        val currentReforge = reforges.firstOrNull { it.name == currentReforgeCapitalized }
        val list = reforges.filter { it.type == itemType }.map { reforge ->
            Renderable.clickAndHover("§7" + reforge.name,
                itemRarity?.let { rarity -> reforge.stats[rarity]?.print(currentReforge?.stats?.get(rarity)) }
                    ?: listOf("")) { reforgeToSearch = reforge.name.replaceFirstChar { it.lowercase() } }
        }
        this.addAll(list)
        if (itemType == null) {
            reforgeToSearch = ""
        }
    }

    fun updateDisplay() {
        display = generateDisplay()
    }

    var display: List<Renderable> = generateDisplay()

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!enable()) return
        posCurrent.renderStrings(listOf(formattedReforgeToSearch, formattedCurrentReforge), posLabel = "Reforge Notify")
        posList.renderRenderables(display, posLabel = "Reforge Overlay")
    }

    @SubscribeEvent
    fun onRepo(event: RepositoryReloadEvent) {

    }
}

