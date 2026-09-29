package br.com.ticket.companion.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {
    @Test fun `a higher release is newer, equal or lower is not`() {
        assertTrue(AppUpdate.isNewer("1.5.2", "v1.5.3"))
        assertTrue(AppUpdate.isNewer("1.5.9", "1.6.0"))
        assertTrue(AppUpdate.isNewer("1.9.0", "v1.10.0")) // numérico, não lexicográfico
        assertFalse(AppUpdate.isNewer("1.5.2", "v1.5.2"))
        assertFalse(AppUpdate.isNewer("1.5.2", "v1.5.1"))
        assertFalse(AppUpdate.isNewer("1.5.2", "lixo"))
    }

    @Test fun `reads the tag and the apk asset from a GitHub release`() {
        val json = """{"tag_name":"v1.6.0","body":"Corrige a captura","draft":false,"prerelease":false,
          "assets":[{"name":"notes.txt","browser_download_url":"https://github.com/x/notes.txt"},
                    {"name":"ticket-v1.6.0.apk","browser_download_url":"https://github.com/n0sd3/ticket-companion/releases/download/v1.6.0/ticket-v1.6.0.apk"}]}"""
        val info = AppUpdate.parseRelease(json)!!
        assertEquals("1.6.0", info.version)
        assertEquals("https://github.com/n0sd3/ticket-companion/releases/download/v1.6.0/ticket-v1.6.0.apk", info.apkUrl)
        assertEquals("Corrige a captura", info.notes)
    }

    @Test fun `a release without apk, a draft or a foreign host is ignored`() {
        assertNull(AppUpdate.parseRelease("""{"tag_name":"v1.6.0","assets":[]}"""))
        assertNull(AppUpdate.parseRelease("""{"tag_name":"v1.6.0","draft":true,"assets":[{"name":"a.apk","browser_download_url":"https://github.com/a.apk"}]}"""))
        assertNull(AppUpdate.parseRelease("""{"tag_name":"v1.6.0","assets":[{"name":"a.apk","browser_download_url":"https://evil.example.com/a.apk"}]}"""))
        assertNull(AppUpdate.parseRelease("nao é json"))
    }
}
