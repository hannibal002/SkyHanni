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
import java.util.Locale
import java.util.jar.JarFile
import java.util.regex.Pattern
import java.util.zip.ZipEntry
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

    // UI Components
    private var logo: JLabel? = null
    private var versionInfo: JLabel? = null
    private var labelFolder: JLabel? = null
    private var panelCenter: JPanel? = null
    private var panelBottom: JPanel? = null
    private var totalContentPane: JPanel? = null
    private var descriptionText: JTextArea? = null
    private var forgeDescriptionText: JTextArea? = null
    private var textFieldFolderLocation: JTextField? = null
    private var buttonChooseFolder: JButton? = null
    private var buttonInstall: JButton? = null
    private var buttonOpenFolder: JButton? = null
    private var buttonClose: JButton? = null

    // Layout constants
    private val margin = 5
    private val frameWidth = TOTAL_WIDTH

    // We'll compute each row's Y position sequentially.
    private var rowY = 0

    init {
        try {
            name = "SkyHanniInstallerFrame"
            title = "SkyHanni Installer"
            isResizable = false
            setSize(TOTAL_WIDTH, TOTAL_HEIGHT)
            contentPane = getPanelContentPane()

            getButtonFolder().addActionListener(this)
            getButtonInstall().addActionListener(this)
            getButtonOpenFolder().addActionListener(this)
            getButtonClose().addActionListener(this)
            getForgeTextArea().addMouseListener(this)

            pack()
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

            getFieldFolder().text = getModsFolder().path
            getButtonInstall().isEnabled = true
            getButtonInstall().requestFocus()
        } catch (ex: Exception) {
            showErrorPopup(ex)
        }
    }

    private fun getPanelContentPane(): JPanel {
        if (totalContentPane == null) {
            try {
                totalContentPane = JPanel().apply {
                    name = "PanelContentPane"
                    layout = BorderLayout(5, 5)
                    preferredSize = Dimension(TOTAL_WIDTH, TOTAL_HEIGHT)
                    add(getPanelCenter(), BorderLayout.CENTER)
                    add(getPanelBottom(), BorderLayout.SOUTH)
                }
            } catch (ivjExc: Throwable) {
                showErrorPopup(ivjExc)
            }
        }
        return totalContentPane!!
    }

    private fun getPanelCenter(): JPanel {
        if (panelCenter == null) {
            try {
                panelCenter = JPanel(null).apply { name = "PanelCenter" }
                // Reset rowY before adding components.
                rowY = 0
                panelCenter!!.add(getPictureLabel())
                panelCenter!!.add(getVersionInfo())
                panelCenter!!.add(getTextArea())
                panelCenter!!.add(getForgeTextArea())
                panelCenter!!.add(getLabelFolder())
                panelCenter!!.add(getFieldFolder())
                panelCenter!!.add(getButtonFolder())
            } catch (ivjExc: Throwable) {
                showErrorPopup(ivjExc)
            }
        }
        return panelCenter!!
    }

    private fun getPictureLabel(): JLabel {
        if (logo == null) {
            try {
                val picHeight = frameWidth / 2
                val resource = javaClass.classLoader.getResourceAsStream("assets/skyhanni/logo.png")
                    ?: throw Exception("Logo not found.")
                val myPicture: BufferedImage = ImageIO.read(resource)
                val scaled = myPicture.getScaledInstance(frameWidth - margin * 2, picHeight - margin, Image.SCALE_SMOOTH)
                logo = JLabel(ImageIcon(scaled)).apply {
                    name = "Logo"
                    setBounds(margin, rowY + margin, frameWidth - margin * 2, picHeight - margin)
                    font = Font(Font.DIALOG, Font.BOLD, 18)
                    horizontalAlignment = SwingConstants.CENTER
                    preferredSize = Dimension(picHeight * 742 / 537, picHeight)
                }
                rowY += picHeight // move rowY below the image
            } catch (ivjExc: Throwable) {
                showErrorPopup(ivjExc)
            }
        }
        return logo!!
    }

    private fun getVersionInfo(): JLabel {
        if (versionInfo == null) {
            try {
                val compHeight = 25
                versionInfo = JLabel().apply {
                    name = "LabelMcVersion"
                    setBounds(0, rowY, frameWidth, compHeight)
                    font = Font(Font.DIALOG, Font.BOLD, 14)
                    horizontalAlignment = SwingConstants.CENTER
                    preferredSize = Dimension(frameWidth, compHeight)
                    text = "SkyHanni by hannibal2, Installer by Biscuit"
                }
                rowY += compHeight
            } catch (ivjExc: Throwable) {
                showErrorPopup(ivjExc)
            }
        }
        return versionInfo!!
    }

    private fun getTextArea(): JTextArea {
        if (descriptionText == null) {
            try {
                val compHeight = 60
                descriptionText = JTextArea().apply {
                    name = "TextArea"
                    setStandardFormatting(this, compHeight)
                    text = ("This installer will copy SkyHanni into your forge mods folder for you, " +
                        "and replace any old versions that already exist. Close this if you prefer to do this yourself!")
                    wrapStyleWord = true
                }
                rowY += compHeight
            } catch (ivjExc: Throwable) {
                showErrorPopup(ivjExc)
            }
        }
        return descriptionText!!
    }

    // Updated setStandardFormatting now takes the component height and uses the helper variable "margin"
    private fun setStandardFormatting(textArea: JTextArea, compHeight: Int) {
        val m = this@SkyHanniInstallerFrame.margin
        textArea.apply {
            setBounds(m, rowY + m, frameWidth - m * 2, compHeight - m)
            isEditable = false
            highlighter = null
            isEnabled = true
            font = Font(Font.DIALOG, Font.PLAIN, 12)
            lineWrap = true
            isOpaque = false
            preferredSize = Dimension(frameWidth - m * 2, compHeight - m)
        }
    }

    private fun getForgeTextArea(): JTextArea {
        if (forgeDescriptionText == null) {
            try {
                val compHeight = 55
                forgeDescriptionText = JTextArea().apply {
                    name = "TextAreaForge"
                    setStandardFormatting(this, compHeight)
                    text = ("However, you still need to install Forge client in order to be able to run this mod. " +
                        "Click here to visit the download page for Forge 1.8.9!")
                    foreground = Color.BLUE.darker()
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    wrapStyleWord = true
                }
                rowY += compHeight
            } catch (ivjExc: Throwable) {
                showErrorPopup(ivjExc)
            }
        }
        return forgeDescriptionText!!
    }

    private fun getLabelFolder(): JLabel {
        if (labelFolder == null) {
            try {
                val compHeight = 16
                // Use a local x offset for this row.
                val xPos = 10
                labelFolder = JLabel().apply {
                    name = "LabelFolder"
                    setBounds(xPos, rowY + 2, 65, compHeight)
                    preferredSize = Dimension(65, compHeight)
                    text = "Mods Folder"
                }
            } catch (ivjExc: Throwable) {
                showErrorPopup(ivjExc)
            }
        }
        return labelFolder!!
    }

    private fun getFieldFolder(): JTextField {
        if (textFieldFolderLocation == null) {
            try {
                val compHeight = 20
                // Position next to label.
                val xPos = 10 + 65
                textFieldFolderLocation = JTextField().apply {
                    name = "FieldFolder"
                    setBounds(xPos, rowY, 287, compHeight)
                    isEditable = false
                    preferredSize = Dimension(287, compHeight)
                }
            } catch (ivjExc: Throwable) {
                showErrorPopup(ivjExc)
            }
        }
        return textFieldFolderLocation!!
    }

    private fun getButtonFolder(): JButton {
        if (buttonChooseFolder == null) {
            try {
                val compHeight = 20
                // Position after folder text field
                val xPos = 10 + 65 + 287 + 10
                val resource = javaClass.classLoader.getResourceAsStream("assets/skyhanni/folder.png")
                    ?: throw Exception("Folder icon not found.")
                val myPicture = ImageIO.read(resource)
                val scaled = myPicture.getScaledInstance(25 - 8, compHeight - 6, Image.SCALE_SMOOTH)
                buttonChooseFolder = JButton(ImageIcon(scaled)).apply {
                    name = "ButtonFolder"
                    setBounds(xPos, rowY, 25, compHeight)
                    preferredSize = Dimension(25, compHeight)
                }
            } catch (ivjExc: Throwable) {
                showErrorPopup(ivjExc)
            }
        }
        return buttonChooseFolder!!
    }

    private fun getPanelBottom(): JPanel {
        if (panelBottom == null) {
            try {
                panelBottom = JPanel(FlowLayout(FlowLayout.CENTER, 15, 10)).apply {
                    name = "PanelBottom"
                    preferredSize = Dimension(390, 55)
                    add(getButtonInstall())
                    add(getButtonOpenFolder())
                    add(getButtonClose())
                }
            } catch (ivjExc: Throwable) {
                showErrorPopup(ivjExc)
            }
        }
        return panelBottom!!
    }

    private fun getButtonInstall(): JButton {
        if (buttonInstall == null) {
            try {
                buttonInstall = JButton("Install").apply {
                    name = "ButtonInstall"
                    preferredSize = Dimension(100, 26)
                }
            } catch (ivjExc: Throwable) {
                showErrorPopup(ivjExc)
            }
        }
        return buttonInstall!!
    }

    private fun getButtonOpenFolder(): JButton {
        if (buttonOpenFolder == null) {
            try {
                buttonOpenFolder = JButton("Open Mods Folder").apply {
                    name = "ButtonOpenFolder"
                    preferredSize = Dimension(130, 26)
                }
            } catch (ivjExc: Throwable) {
                showErrorPopup(ivjExc)
            }
        }
        return buttonOpenFolder!!
    }

    private fun getButtonClose(): JButton {
        if (buttonClose == null) {
            try {
                buttonClose = JButton("Cancel").apply {
                    name = "ButtonClose"
                    preferredSize = Dimension(100, 26)
                }
            } catch (ivjExc: Throwable) {
                showErrorPopup(ivjExc)
            }
        }
        return buttonClose!!
    }

    fun onFolderSelect() {
        val currentDirectory = File(getFieldFolder().text)
        val jFileChooser = JFileChooser(currentDirectory).apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        if (jFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val newDirectory = jFileChooser.selectedFile
            getFieldFolder().text = newDirectory.path
        }
    }

    override fun actionPerformed(e: ActionEvent) {
        when (e.source) {
            getButtonClose() -> {
                dispose()
                System.exit(0)
            }

            getButtonFolder() -> onFolderSelect()
            getButtonInstall() -> onInstall()
            getButtonOpenFolder() -> onOpenFolder()
        }
    }

    override fun mouseClicked(e: MouseEvent) {
        if (e.source == getForgeTextArea()) {
            try {
                Desktop.getDesktop().browse(URI("https://files.minecraftforge.net/maven/net/minecraftforge/forge/index_1.8.9.html"))
            } catch (ex: IOException) {
                showErrorPopup(ex)
            } catch (ex: URISyntaxException) {
                showErrorPopup(ex)
            }
        }
    }

    fun onInstall() {
        try {
            val modsFolder = File(getFieldFolder().text)
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
            if (findSkyHanniAndDelete(modsFolder.listFiles()) == true) deletingFailure = true
        }
        if (inSubFolder) {
            val parent = modsFolder.parentFile
            if (parent.isDirectory) {
                if (findSkyHanniAndDelete(parent.listFiles()) == true) deletingFailure = true
            }
        } else {
            val subFolder = File(modsFolder, "1.8.9")
            if (subFolder.exists() && subFolder.isDirectory) {
                if (findSkyHanniAndDelete(subFolder.listFiles()) == true) deletingFailure = true
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
        System.exit(0)
    }

    private fun findSkyHanniAndDelete(files: Array<File>?): Boolean? {
        if (files == null) return false
        for (file in files) {
            if (!file.isDirectory && file.path.endsWith(".jar")) {
                try {
                    JarFile(file).use { jarFile ->
                        val mcModInfo: ZipEntry? = jarFile.getEntry("mcmod.info")
                        if (mcModInfo != null) {
                            jarFile.getInputStream(mcModInfo).use { inputStream ->
                                val modID = getModIDFromInputStream(inputStream)
                                if (modID == "SkyHanni") {
                                    if (!file.delete()) {
                                        showErrorMessage(
                                            "Was not able to delete the other SkyHanni files found in your mods folder!" +
                                                    System.lineSeparator() +
                                                    "Please make sure that your minecraft is currently closed and try again, or feel" +
                                                    System.lineSeparator() +
                                                    "free to open your mods folder and delete those files manually.",
                                        )
                                        return true
                                    }
                                }
                            }
                        }
                    }
                } catch (ex: Exception) {
                    // Skip file
                }
            }
        }
        return false
    }

    fun onOpenFolder() {
        try {
            Desktop.getDesktop().open(getModsFolder())
        } catch (e: Exception) {
            showErrorPopup(e)
        }
    }

    fun getModsFolder(): File {
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

    fun getFile(userHome: String, minecraftPath: String): File {
        val workingDirectory: File = when (getOperatingSystem()) {
            OperatingSystem.LINUX, OperatingSystem.SOLARIS ->
                File(userHome, ".$minecraftPath/")

            OperatingSystem.WINDOWS -> {
                val appData = System.getenv("APPDATA")
                if (appData != null) File(appData, ".$minecraftPath/") else File(userHome, ".$minecraftPath/")
            }

            OperatingSystem.MACOS ->
                File(userHome, "Library/Application Support/$minecraftPath")

            else -> File(userHome, "$minecraftPath/")
        }
        return workingDirectory
    }

    fun getOperatingSystem(): OperatingSystem {
        val osName = System.getProperty("os.name").toLowerCase(Locale.US)
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

    fun showMessage(message: String) {
        JOptionPane.showMessageDialog(null, message, "SkyHanni", JOptionPane.INFORMATION_MESSAGE)
    }

    fun showErrorMessage(message: String) {
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
