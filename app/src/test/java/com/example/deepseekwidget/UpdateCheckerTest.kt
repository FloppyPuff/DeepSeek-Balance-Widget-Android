package com.example.deepseekwidget

import com.example.deepseekwidget.data.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun testVersionComparison() {
        // Newer versions
        assertTrue(UpdateChecker.isNewerVersion("1.3.0", "1.3.1"))
        assertTrue(UpdateChecker.isNewerVersion("1.3.0", "1.4.0"))
        assertTrue(UpdateChecker.isNewerVersion("1.3.0", "2.0.0"))
        assertTrue(UpdateChecker.isNewerVersion("v1.3.0", "v1.3.1"))
        assertTrue(UpdateChecker.isNewerVersion("1.3", "1.3.1"))

        // Equal versions
        assertFalse(UpdateChecker.isNewerVersion("1.3.0", "1.3.0"))
        assertFalse(UpdateChecker.isNewerVersion("v1.3.0", "1.3.0"))

        // Older versions
        assertFalse(UpdateChecker.isNewerVersion("1.3.1", "1.3.0"))
        assertFalse(UpdateChecker.isNewerVersion("2.0.0", "1.9.9"))
    }

    @Test
    fun testParseReleaseJson() {
        val sampleJson = """
            {
                "tag_name": "v1.4.0",
                "name": "DeepSeek Balance Widget v1.4.0",
                "html_url": "https://github.com/FloppyPuff/DeepSeek-Balance-Widget-Android/releases/tag/v1.4.0",
                "body": "### 新功能\n- 支持检查更新",
                "assets": [
                    {
                        "name": "app-release.apk",
                        "browser_download_url": "https://github.com/FloppyPuff/DeepSeek-Balance-Widget-Android/releases/download/v1.4.0/app-release.apk"
                    },
                    {
                        "name": "app-release.apk.sha256",
                        "browser_download_url": "https://github.com/FloppyPuff/DeepSeek-Balance-Widget-Android/releases/download/v1.4.0/app-release.apk.sha256"
                    }
                ]
            }
        """.trimIndent()

        val info = UpdateChecker.parseReleaseJson(sampleJson, currentVersion = "1.3.0")
        assertEquals("1.4.0", info.latestVersion)
        assertEquals("v1.4.0", info.tagName)
        assertEquals("DeepSeek Balance Widget v1.4.0", info.releaseTitle)
        assertEquals("### 新功能\n- 支持检查更新", info.releaseNotes)
        assertEquals(
            "https://github.com/FloppyPuff/DeepSeek-Balance-Widget-Android/releases/download/v1.4.0/app-release.apk",
            info.downloadUrl
        )
        assertTrue(info.hasUpdate)
    }

    @Test
    fun testParseReleaseJsonWhenUpToDate() {
        val sampleJson = """
            {
                "tag_name": "v1.3.0",
                "name": "DeepSeek Balance Widget v1.3.0",
                "html_url": "https://github.com/FloppyPuff/DeepSeek-Balance-Widget-Android/releases/tag/v1.3.0",
                "body": "No changes",
                "assets": []
            }
        """.trimIndent()

        val info = UpdateChecker.parseReleaseJson(sampleJson, currentVersion = "1.3.0")
        assertFalse(info.hasUpdate)
        assertEquals("https://github.com/FloppyPuff/DeepSeek-Balance-Widget-Android/releases/tag/v1.3.0", info.downloadUrl)
    }
}
