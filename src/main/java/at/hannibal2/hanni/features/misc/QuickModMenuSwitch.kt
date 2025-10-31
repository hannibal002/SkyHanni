package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.enums.OutsideSBFeature
import at.hannibal2.hanni.data.jsonobjects.repo.ModGuiSwitcherJson
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.events.render.gui.ScreenDrawnEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ReflectionUtils.makeAccessible
import at.hannibal2.hanni.utils.RenderUtils.renderRenderables
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.hanni.utils.compat.DrawContextUtils
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.addLine
import at.hannibal2.hanni.utils.renderables.primitives.text
import net.minecraft.client.Minecraft
//#if FORGE
import net.minecraftforge.client.ClientCommandHandler
//#endif

@HanniModule
object QuickModMenuSwitch {

    private val config get() = HanniMod.feature.misc.quickModMenuSwitch
    private var display = emptyList<Renderable>()
    private var latestGuiPath = ""

    private var mods: List<Mod>? = null

    private var currentlyOpeningMod = ""
    private var lastGuiOpen = 0L

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val modsJar = event.getConstant<ModGuiSwitcherJson>("ModGuiSwitcher")
        mods = modsJar.mods.filter { mod ->
            mod.value.guiPath.any { runCatching { Class.forName(it) }.isSuccess }
        }.map { (name, mod) ->
            Mod(name, mod.description, mod.command, mod.guiPath)
        }
    }

    @HandleEvent
    fun onTick(event: HanniTickEvent) {
        if (!isEnabled()) return

        if (event.isMod(5)) {
            update()
        }
    }

    class Mod(val name: String, val description: List<String>, val command: String, private val guiPath: List<String>) {

        fun isInGui() = guiPath.any { latestGuiPath.startsWith(it) }
    }

    private fun update() {
        var openGui = Minecraft.getMinecraft().currentScreen?.javaClass?.name ?: "none"
        openGui = handleAbstractGuis(openGui)
        if (latestGuiPath != openGui) {
            latestGuiPath = openGui

            if (HanniMod.feature.dev.debug.modMenuLog) {
                ChatUtils.debug("Open GUI: $latestGuiPath")
            }
        }
        val mods = mods ?: return

        display = if (!shouldShow(mods)) {
            emptyList()
        } else {
            renderDisplay(mods)
        }
    }

    private fun shouldShow(mods: List<Mod>): Boolean {
        if (config.insideEscapeMenu && isEscapeMenu(latestGuiPath)) return true
        if (config.insidePlayerInventory && latestGuiPath == "net.minecraft.client.gui.inventory.GuiInventory") return true

        return mods.any { it.isInGui() }
    }

    private fun isEscapeMenu(path: String) = when (path) {
        "net.minecraft.client.gui.GuiIngameMenu" -> true
        "me.powns.togglesneak.gui.screens.GuiOptionsReplace" -> true

        else -> false
    }

    private fun handleAbstractGuis(openGui: String): String {
        if (openGui == "gg.essential.vigilance.gui.SettingsGui") {
            val clazz = Class.forName("gg.essential.vigilance.gui.SettingsGui")
            val titleBarDelegate = clazz.getDeclaredField("titleBar\$delegate").makeAccessible()
                .get(Minecraft.getMinecraft().currentScreen)
            val titleBar =
                titleBarDelegate.javaClass.declaredFields[0].makeAccessible().get(titleBarDelegate)
            val gui = titleBar.javaClass.getDeclaredField("gui").makeAccessible().get(titleBar)
            val config = gui.javaClass.getDeclaredField("config").makeAccessible().get(gui)

            return config.javaClass.name
        }
        if (openGui == "cc.polyfrost.oneconfig.gui.OneConfigGui") {
            val actualGui = Minecraft.getMinecraft().currentScreen ?: return openGui
            val currentPage = actualGui.javaClass.getDeclaredField("currentPage")
                .makeAccessible()
                .get(actualGui)
            if (currentPage.javaClass.simpleName == "ModConfigPage") {
                val optionPage = currentPage.javaClass.getDeclaredField("page")
                    .makeAccessible()
                    .get(currentPage)
                val mod = optionPage.javaClass.getField("mod")
                    .makeAccessible()
                    .get(optionPage)
                val modName = mod.javaClass.getField("name")
                    .get(mod) as String
                return "cc.polyfrost.oneconfig.gui.OneConfigGui:$modName"
            }
        }

        return openGui
    }

    private fun renderDisplay(mods: List<Mod>) = buildList {
        for (mod in mods) {
            val currentlyOpen = mod.isInGui()
            val nameFormat = if (currentlyOpen) "§c" else ""
            var opening = mod.name == currentlyOpeningMod
            if (currentlyOpen && opening) {
                currentlyOpeningMod = ""
                opening = false
            }
            val nameSuffix = if (opening) " §7(opening...)" else ""
            val renderable = Renderable.link(
                Renderable.text(nameFormat + mod.name),
                bypassChecks = true,
                onLeftClick = { open(mod) },
                condition = { System.currentTimeMillis() > lastGuiOpen + 250 },
            )
            addLine {
                add(renderable)
                addString(nameSuffix)
            }
        }
    }

    private fun open(mod: Mod) {
        lastGuiOpen = System.currentTimeMillis()
        currentlyOpeningMod = mod.name
        update()
        try {
            //#if FORGE
            ClientCommandHandler.instance.executeCommand(MinecraftCompat.localPlayer, "/" + mod.command)
            //#endif
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error trying to open the gui for mod " + mod.name)
        }
    }

    @HandleEvent
    fun onScreenDrawn(event: ScreenDrawnEvent) {
        if (!isEnabled()) return

        DrawContextUtils.pushMatrix()
        config.pos.renderRenderables(display, posLabel = "Quick Mod Menu Switch")
        DrawContextUtils.popMatrix()
    }

    private fun isEnabled() = (SkyBlockUtils.inSkyBlock || OutsideSBFeature.QUICK_MOD_MENU_SWITCH.isSelected()) && config.enabled

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.modMenuLog", "dev.debug.modMenuLog")
    }
}
