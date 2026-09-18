package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.features.misc.update.GitHubRelease
import at.hannibal2.skyhanni.utils.json.fromJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GitHubReleaseTest {
    @Test
    fun `github release is parsed`() {
        val json = """
            [
              {
                "tag_name": "1.2.3",
                "name": "1.2.3 Release",
                "body": "changelog",
                "draft": false,
                "prerelease": true,
                "html_url": "https://github.com/hannibal002/SkyHanni/releases/tag/1.2.3",
                "assets": [
                  {
                    "name": "SkyHanni-1.2.3-mc26.1.jar",
                    "browser_download_url": "https://example.com/SkyHanni-1.2.3-mc26.1.jar"
                  }
                ]
              }
            ]
        """.trimIndent()

        val release = ConfigManager.gson.fromJson<List<GitHubRelease>>(json).single()

        assertEquals("1.2.3", release.tagName)
        assertEquals("1.2.3 Release", release.name)
        assertEquals("changelog", release.body)
        assertFalse(release.draft)
        assertTrue(release.prerelease)
        assertEquals("https://github.com/hannibal002/SkyHanni/releases/tag/1.2.3", release.htmlUrl)

        val asset = release.assets?.single()
        assertEquals("SkyHanni-1.2.3-mc26.1.jar", asset?.name)
        assertEquals("https://example.com/SkyHanni-1.2.3-mc26.1.jar", asset?.browserDownloadUrl)
    }

    @Test
    fun `github release without optional fields is parsed`() {
        val json = """
            [
              {
                "tag_name": "1.2.3",
                "html_url": "https://github.com/hannibal002/SkyHanni/releases/tag/1.2.3"
              }
            ]
        """.trimIndent()

        val release = ConfigManager.gson.fromJson<List<GitHubRelease>>(json).single()

        assertEquals("1.2.3", release.tagName)
        assertNull(release.name)
        assertNull(release.body)
        assertNull(release.assets)
        assertFalse(release.draft)
        assertFalse(release.prerelease)
    }
}
