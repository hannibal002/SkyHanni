package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.ErrorManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.UUID

private typealias KeyTypes = RegistryUtil.KeyTypes
private typealias RegPaths = RegistryUtil.RegistryPaths

object NotificationUtil {
    private fun getBase(template: String) : String =
        """[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null 
           [Windows.UI.Notifications.ToastNotification, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
           [Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] | Out-Null 
           ${'$'}template = $template 
           ${'$'}xml = New-Object Windows.Data.Xml.Dom.XmlDocument 
           ${'$'}xml.LoadXml(${'$'}template) 
           ${'$'}toast = [Windows.UI.Notifications.ToastNotification]::new(${'$'}xml) 
           ${'$'}toast.Tag = "SkyHanni" 
           ${'$'}toast.Group = "SkyHanni"
           [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier("hannibal02.SkyHanni").Show(${'$'}toast)
        """.trimMargin().trimIndent()


    fun create(title: String = "", desc: String = "", image: String = "") {
        if (title.isEmpty() || desc.isEmpty()) {
            throw IllegalArgumentException("title or desc can't be empty!")
        }
        // Maybe we want to add more styles in the future
        val type = if (image.isNotEmpty()) {
            SkyHanniNotification.TEXT_WITH_IMAGE
        } else {
            SkyHanniNotification.TEXT
        }

        val fileName: String = UUID.randomUUID().toString()
        val tempFile = File.createTempFile(fileName, ".ps1")

        val script = getBase(type.template)
            .replace("{title}", title).replace("{desc}", desc).replace("{src}", image)
        tempFile.writeText(script)

        // If someone wonders why I use so many parameters. (It's the official way to bypass the execution policy :) )
        val process = Runtime.getRuntime()
            .exec("""powershell.exe -ExecutionPolicy Bypass -NoLogo -NonInteractive -NoProfile -WindowStyle Hidden Get-Content("${tempFile.absolutePath}") -Raw | Invoke-Expression """)

        val stderr = BufferedReader(InputStreamReader(process.errorStream))

        val errorLines: List<String> = stderr.readLines()
        if (errorLines.isNotEmpty()) {
            ErrorManager.skyHanniError(errorLines.joinToString("\n"))
        }

        stderr.close()
        process.destroy()
        tempFile.delete()
    }

    private val regKey = "SkyHanni"
    private val regValues: List<Values> = listOf(
        Values("DisplayName", KeyTypes.STRING, "SkyHanni"),
        Values("IconUri", KeyTypes.EXPANDABLE_STRING, "C:\\Users\\mattz\\Desktop\\logo.ico"),
        Values("ShowInSettings", KeyTypes.INTEGER, "1")
    )

    private fun checkRegistry() {
        val runtime: Runtime = Runtime.getRuntime()
        if (!RegistryUtil.keyExists(runtime, RegPaths.NOTIFICATIONS, regKey)) {
            RegistryUtil.addKey(runtime, RegPaths.NOTIFICATIONS, regKey)
        }

        for (data: Values in regValues) {
            if (!RegistryUtil.valueExists(runtime, RegPaths.NOTIFICATIONS, regKey, data.name))
                RegistryUtil.addValue(runtime, RegPaths.NOTIFICATIONS, regKey, data.name, data.type, data.value)
        }
    }

    enum class SkyHanniNotification(val template: String) {
        // "@ at the end has to stay there! Otherwise, the script won't work
        // TODO: Maybe fix it in a better way
        TEXT("""@"
            <toast>
                <visual>
                    <binding template="ToastText02">
                       <text id="1">{title}</text>
                       <text id="2">{desc}</text>
                    </binding>
                </visual>
            </toast>
"@""".trimMargin().trimIndent()),
        // Same thing as above
        TEXT_WITH_IMAGE("""@"
            <toast>
                <visual>
                    <binding template="ToastImageAndText02">
                       <text id="1">{title}</text>
                       <text id="2">{desc}</text>
                       <image id="1" placement="appLogoOverride" hint-crop="circle" src="{src}"/>
                    </binding>
                </visual>
            </toast>
"@""".trimMargin().trimIndent()),
        ;

    }

    private class Values(var name: String, var type: KeyTypes, var value: String) {}

}
