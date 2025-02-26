import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Image
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.util.jar.JarFile
import java.util.regex.Pattern
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.UIManager
import javax.swing.WindowConstants
import kotlin.system.exitProcess

class SkyHanniInstallerFrame : JFrame(), ActionListener, MouseListener {

    companion object {
        private val IN_MODS_SUBFOLDER = Pattern.compile("1\\.8\\.9[/\\\\]?$")
        private const val TOTAL_HEIGHT = 435
        private const val TOTAL_WIDTH = 404

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
                val frame = SkyHanniInstallerFrame()
                frame.centerFrame(frame)
                frame.isVisible = true
            } catch (ex: Exception) {
                showErrorPopup(ex)
            }
        }

        private fun getStacktraceText(ex: Throwable): String {
            val sw = StringWriter()
            ex.printStackTrace(PrintWriter(sw))
            return sw.toString().replace("\t", "  ")
        }

        fun showErrorPopup(ex: Throwable) {
            ex.printStackTrace()
            val textArea = JTextArea(getStacktraceText(ex)).apply {
                isEditable = false
                font = Font(Font.MONOSPACED, font.style, font.size)
            }
            val errorScrollPane = JScrollPane(textArea).apply {
                preferredSize = Dimension(600, 400)
            }
            JOptionPane.showMessageDialog(null, errorScrollPane, "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    // Layout constants
    private val margin = 5
    private val frameWidth = TOTAL_WIDTH

    // We'll compute each row’s Y position sequentially.
    private var rowY = 0

    // UI Components as lazy properties (non-null)
    private val logo: JLabel by lazy {
        val picHeight = frameWidth / 2
        val resource = javaClass.classLoader.getResourceAsStream("assets/skyhanni/logo.png") ?: throw Exception("Logo not found.")
        val myPicture: BufferedImage = ImageIO.read(resource)
        val scaled = myPicture.getScaledInstance(frameWidth - margin * 2, picHeight - margin, Image.SCALE_SMOOTH)
        val lbl = JLabel(ImageIcon(scaled)).apply {
            name = "Logo"
            setBounds(margin, rowY + margin, frameWidth - margin * 2, picHeight - margin)
            font = Font(Font.DIALOG, Font.BOLD, 18)
            horizontalAlignment = SwingConstants.CENTER
            preferredSize = Dimension(picHeight * 742 / 537, picHeight)
        }
        rowY += picHeight // move rowY below the image
        lbl
    }

    private val versionInfo: JLabel by lazy {
        val compHeight = 25
        val lbl = JLabel().apply {
            name = "LabelMcVersion"
            setBounds(0, rowY, frameWidth, compHeight)
            font = Font(Font.DIALOG, Font.BOLD, 14)
            horizontalAlignment = SwingConstants.CENTER
            preferredSize = Dimension(frameWidth, compHeight)
            text = "SkyHanni by hannibal2, Installer by Biscuit"
        }
        rowY += compHeight
        lbl
    }

    private val descriptionText: JTextArea by lazy {
        val compHeight = 60
        val ta = JTextArea().apply {
            name = "TextArea"
            setStandardFormatting(compHeight)
            text = "This installer will copy SkyHanni into your forge mods folder for you, " +
                "and replace any old versions that already exist. Close this if you prefer to do this yourself!"
            wrapStyleWord = true
        }
        rowY += compHeight
        ta
    }

    private val forgeDescriptionText: JTextArea by lazy {
        val compHeight = 55
        val ta = JTextArea().apply {
            name = "TextAreaForge"
            setStandardFormatting(compHeight)
            text = "However, you still need to install Forge client in order to be able to run this mod. " +
                "Click here to visit the download page for Forge 1.8.9!"
            foreground = Color.BLUE.darker()
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            wrapStyleWord = true
        }
        rowY += compHeight
        ta
    }

    private val labelFolder: JLabel by lazy {
        val compHeight = 16
        // x position is fixed for folder label
        val xPos = 10
        JLabel().apply {
            name = "LabelFolder"
            setBounds(xPos, rowY + 2, 65, compHeight)
            preferredSize = Dimension(65, compHeight)
            text = "Mods Folder"
        }
    }

    private val textFieldFolderLocation: JTextField by lazy {
        val compHeight = 20
        // Positioned next to folder label
        val xPos = 10 + 65
        JTextField().apply {
            name = "FieldFolder"
            setBounds(xPos, rowY, 287, compHeight)
            isEditable = false
            preferredSize = Dimension(287, compHeight)
        }
    }

    private val buttonChooseFolder: JButton by lazy {
        val compHeight = 20
        // Positioned after text field
        val xPos = 10 + 65 + 287 + 10
        val resource = javaClass.classLoader.getResourceAsStream("assets/skyhanni/folder.png") ?: throw Exception("Folder icon not found.")
        val myPicture = ImageIO.read(resource)
        val scaled = myPicture.getScaledInstance(25 - 8, compHeight - 6, Image.SCALE_SMOOTH)
        JButton(ImageIcon(scaled)).apply {
            name = "ButtonFolder"
            setBounds(xPos, rowY, 25, compHeight)
            preferredSize = Dimension(25, compHeight)
        }
    }

    private val panelCenter: JPanel by lazy {
        rowY = 0 // reset rowY for panel center layout
        JPanel(null).apply {
            name = "PanelCenter"
            add(logo)
            add(versionInfo)
            add(descriptionText)
            add(forgeDescriptionText)
            add(labelFolder)
            add(textFieldFolderLocation)
            add(buttonChooseFolder)
        }
    }

    private val buttonInstall: JButton by lazy {
        JButton("Install").apply {
            name = "ButtonInstall"
            preferredSize = Dimension(100, 26)
        }
    }

    private val buttonOpenFolder: JButton by lazy {
        JButton("Open Mods Folder").apply {
            name = "ButtonOpenFolder"
            preferredSize = Dimension(130, 26)
        }
    }

    private val buttonClose: JButton by lazy {
        JButton("Cancel").apply {
            name = "ButtonClose"
            preferredSize = Dimension(100, 26)
        }
    }

    private val panelBottom: JPanel by lazy {
        JPanel(FlowLayout(FlowLayout.CENTER, 15, 10)).apply {
            name = "PanelBottom"
            preferredSize = Dimension(390, 55)
            add(buttonInstall)
            add(buttonOpenFolder)
            add(buttonClose)
        }
    }

    private val totalContentPane: JPanel by lazy {
        JPanel().apply {
            name = "PanelContentPane"
            layout = BorderLayout(5, 5)
            preferredSize = Dimension(TOTAL_WIDTH, TOTAL_HEIGHT)
            add(panelCenter, BorderLayout.CENTER)
            add(panelBottom, BorderLayout.SOUTH)
        }
    }

    init {
        try {
            name = "SkyHanniInstallerFrame"
            title = "SkyHanni Installer"
            isResizable = false
            setSize(TOTAL_WIDTH, TOTAL_HEIGHT)
            contentPane = totalContentPane

            buttonChooseFolder.addActionListener(this)
            buttonInstall.addActionListener(this)
            buttonOpenFolder.addActionListener(this)
            buttonClose.addActionListener(this)
            forgeDescriptionText.addMouseListener(this)

            pack()
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

            textFieldFolderLocation.text = getModsFolder().path
            buttonInstall.isEnabled = true
            buttonInstall.requestFocus()
        } catch (ex: Exception) {
            showErrorPopup(ex)
        }
    }

    // Helper that sets standard formatting; uses the given height for the component.
    private fun JTextArea.setStandardFormatting(compHeight: Int) {
        val m = this@SkyHanniInstallerFrame.margin
        setBounds(m, rowY + m, frameWidth - m * 2, compHeight - m)
        isEditable = false
        highlighter = null
        isEnabled = true
        font = Font(Font.DIALOG, Font.PLAIN, 12)
        lineWrap = true
        isOpaque = false
        preferredSize = Dimension(frameWidth - m * 2, compHeight - m)
    }

    override fun actionPerformed(e: ActionEvent) {
        when (e.source) {
            buttonClose -> {
                dispose()
                exitProcess(0)
            }

            buttonChooseFolder -> onFolderSelect()
            buttonInstall -> onInstall()
            buttonOpenFolder -> onOpenFolder()
        }
    }

    override fun mouseClicked(e: MouseEvent) {
        if (e.source == forgeDescriptionText) {
            try {
                Desktop.getDesktop().browse(URI("https://files.minecraftforge.net/maven/net/minecraftforge/forge/index_1.8.9.html"))
            } catch (ex: IOException) {
                showErrorPopup(ex)
            } catch (ex: URISyntaxException) {
                showErrorPopup(ex)
            }
        }
    }

    private fun onFolderSelect() {
        val currentDirectory = File(textFieldFolderLocation.text)
        val jFileChooser = JFileChooser(currentDirectory).apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        if (jFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            textFieldFolderLocation.text = jFileChooser.selectedFile.path
        }
    }

    private fun onInstall() {
        try {
            val modsFolder = File(textFieldFolderLocation.text)
            when {
                !modsFolder.exists() -> {
                    showErrorMessage("Folder not found: ${modsFolder.path}")
                    return
                }

                !modsFolder.isDirectory -> {
                    showErrorMessage("Not a folder: ${modsFolder.path}")
                    return
                }
            }
            tryInstall(modsFolder)
        } catch (e: Exception) {
            showErrorPopup(e)
        }
    }

    private fun tryInstall(modsFolder: File) {
        val thisFile = getThisFile() ?: return
        val inSubFolder = IN_MODS_SUBFOLDER.matcher(modsFolder.path).find()
        var deletingFailure = false

        if (modsFolder.isDirectory) {
            if (findSkyHanniAndDelete(modsFolder.listFiles())) deletingFailure = true
        }
        if (inSubFolder) {
            val parent = modsFolder.parentFile
            if (parent.isDirectory) {
                if (findSkyHanniAndDelete(parent.listFiles())) deletingFailure = true
            }
        } else {
            val subFolder = File(modsFolder, "1.8.9")
            if (subFolder.exists() && subFolder.isDirectory) {
                if (findSkyHanniAndDelete(subFolder.listFiles())) deletingFailure = true
            }
        }
        if (deletingFailure) return

        if (thisFile.isDirectory) {
            showErrorMessage("This file is a directory... Are we in a development environment?")
            return
        }
        try {
            Files.copy(thisFile.toPath(), File(modsFolder, thisFile.name).toPath())
        } catch (ex: Exception) {
            showErrorPopup(ex)
            return
        }
        showMessage("SkyHanni has been successfully installed into your mods folder.")
        dispose()
        exitProcess(0)
    }

    private fun findSkyHanniAndDelete(files: Array<File>?): Boolean {
        if (files == null) return false
        for (file in files) {
            if (file.isDirectory || !file.path.endsWith(".jar")) continue
            try {
                JarFile(file).use { jarFile ->
                    val mcModInfo = jarFile.getEntry("mcmod.info") ?: continue
                    jarFile.getInputStream(mcModInfo).use { inputStream ->
                        val modID = getModIDFromInputStream(inputStream)
                        if (modID != "SkyHanni" || file.delete()) continue
                        val newLine = System.lineSeparator()
                        showErrorMessage(
                            "Was not able to delete the other SkyHanni files found in your mods folder!" + newLine +
                                "Please make sure that your minecraft is currently closed and try again, or feel" + newLine +
                                "free to open your mods folder and delete those files manually.",
                        )
                        return true
                    }
                }
            } catch (ex: Exception) {
                // Skip file
            }
        }
        return false
    }

    private fun onOpenFolder() {
        try {
            Desktop.getDesktop().open(getModsFolder())
        } catch (e: Exception) {
            showErrorPopup(e)
        }
    }

    private fun getModsFolder(): File {
        val userHome = System.getProperty("user.home", ".")
        var modsFolder = getFile(userHome, "minecraft/mods/1.8.9")
        if (!modsFolder.exists()) {
            modsFolder = getFile(userHome, "minecraft/mods")
        }
        if (!modsFolder.exists() && !modsFolder.mkdirs()) {
            throw RuntimeException("The working directory could not be created: $modsFolder")
        }
        return modsFolder
    }

    private fun getFile(userHome: String, minecraftPath: String): File {
        val workingDirectory: File = when (getOperatingSystem()) {
            OperatingSystem.LINUX, OperatingSystem.SOLARIS -> File(userHome, ".$minecraftPath/")

            OperatingSystem.WINDOWS -> {
                val appData = System.getenv("APPDATA")
                if (appData != null) File(appData, ".$minecraftPath/") else File(userHome, ".$minecraftPath/")
            }

            OperatingSystem.MACOS -> File(userHome, "Library/Application Support/$minecraftPath")

            else -> File(userHome, "$minecraftPath/")
        }
        return workingDirectory
    }

    private fun getOperatingSystem(): OperatingSystem {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> OperatingSystem.WINDOWS
            osName.contains("mac") -> OperatingSystem.MACOS
            osName.contains("solaris") || osName.contains("sunos") -> OperatingSystem.SOLARIS
            osName.contains("linux") || osName.contains("unix") -> OperatingSystem.LINUX
            else -> OperatingSystem.UNKNOWN
        }
    }

    fun centerFrame(frame: JFrame) {
        val rectangle = frame.bounds
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val screenRectangle = Rectangle(0, 0, screenSize.width, screenSize.height)
        var newX = screenRectangle.x + (screenRectangle.width - rectangle.width) / 2
        var newY = screenRectangle.y + (screenRectangle.height - rectangle.height) / 2
        if (newX < 0) newX = 0
        if (newY < 0) newY = 0
        frame.setBounds(newX, newY, rectangle.width, rectangle.height)
    }

    private fun showMessage(message: String) {
        JOptionPane.showMessageDialog(null, message, "SkyHanni", JOptionPane.INFORMATION_MESSAGE)
    }

    private fun showErrorMessage(message: String) {
        JOptionPane.showMessageDialog(null, message, "SkyHanni - Error", JOptionPane.ERROR_MESSAGE)
    }

    private fun getModIDFromInputStream(inputStream: InputStream): String {
        var version = ""
        try {
            BufferedReader(InputStreamReader(inputStream)).use { bufferedReader ->
                while (bufferedReader.readLine().also { version = it } != null) {
                    if (version.contains("\"modid\": \"")) {
                        version = version.split("\"modid\": \"")[1].dropLast(2)
                        break
                    }
                }
            }
        } catch (ex: Exception) {
            // Couldn't find modid
        }
        return version
    }

    private fun getThisFile(): File? {
        return try {
            File(javaClass.protectionDomain.codeSource.location.toURI())
        } catch (ex: URISyntaxException) {
            showErrorPopup(ex)
            null
        }
    }

    override fun mousePressed(e: MouseEvent) {}
    override fun mouseReleased(e: MouseEvent) {}
    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}

    enum class OperatingSystem {
        LINUX,
        SOLARIS,
        WINDOWS,
        MACOS,
        UNKNOWN
    }
}
