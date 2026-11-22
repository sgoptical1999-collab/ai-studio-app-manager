package com.example

import com.example.security.SafeZipAndApkValidator
import com.example.security.UrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SecurityAndValidationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `url validator strictly enforces HTTPS and parses GitHub repositories`() {
        assertTrue(UrlValidator.isValidHttpsUrl("https://aistudio.google.com/"))
        assertFalse(UrlValidator.isValidHttpsUrl("http://aistudio.google.com/"))
        assertFalse(UrlValidator.isValidHttpsUrl("javascript:alert(1)"))
        assertFalse(UrlValidator.isValidHttpsUrl("file:///etc/passwd"))

        val parsed = UrlValidator.parseGitHubRepo("https://github.com/octocat/ai-studio-app.git")
        assertNotNull(parsed)
        assertEquals("octocat", parsed?.owner)
        assertEquals("ai-studio-app", parsed?.repo)
        assertEquals("https://github.com/octocat/ai-studio-app", parsed?.canonicalHttpsUrl)

        val shorthand = UrlValidator.parseGitHubRepo("myorg/my-android-app")
        assertNotNull(shorthand)
        assertEquals("myorg", shorthand?.owner)
        assertEquals("my-android-app", shorthand?.repo)

        assertNull(UrlValidator.parseGitHubRepo("https://evil.example.com/owner/repo"))
        assertNull(UrlValidator.parseGitHubRepo("https://github.com/../etc/passwd"))
    }

    @Test
    fun `folder and file name sanitizers strip traversal characters`() {
        val folder = UrlValidator.sanitizeFolderName("../../VIRAL REEL AI?!")
        assertFalse(folder.contains(".."))
        assertFalse(folder.contains("/"))
        assertEquals("VIRAL REEL AI", folder)

        val fileName = UrlValidator.sanitizeFileName("../../app-release.apk", "fallback.apk")
        assertEquals("app-release.apk", fileName)
    }

    @Test
    fun `zip extraction blocks zip slip path traversal entries`() {
        val maliciousZip = tempFolder.newFile("malicious.zip")
        ZipOutputStream(FileOutputStream(maliciousZip)).use { zos ->
            zos.putNextEntry(ZipEntry("../../outside_escape.txt"))
            zos.write("malicious".toByteArray())
            zos.closeEntry()
        }

        val targetDir = tempFolder.newFolder("extract_target")
        val result = SafeZipAndApkValidator.extractZipSafely(maliciousZip, targetDir)

        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("Blocked", ignoreCase = true))
        assertFalse(File(tempFolder.root, "outside_escape.txt").exists())
    }

    @Test
    fun `artifact zip extraction discovers valid apk and validates signature`() {
        // Create a structurally valid APK (PK header + AndroidManifest.xml + classes.dex)
        val innerApk = tempFolder.newFile("app-release.apk")
        ZipOutputStream(FileOutputStream(innerApk)).use { zos ->
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write(ByteArray(32) { 1 })
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write(ByteArray(64) { 2 })
            zos.closeEntry()
        }

        val artifactZip = tempFolder.newFile("android-apk-artifact.zip")
        ZipOutputStream(FileOutputStream(artifactZip)).use { zos ->
            zos.putNextEntry(ZipEntry("output/app-release.apk"))
            zos.write(innerApk.readBytes())
            zos.closeEntry()
        }

        val extractDir = tempFolder.newFolder("extracted_artifact")
        val extraction = SafeZipAndApkValidator.extractZipSafely(artifactZip, extractDir)

        assertTrue(extraction.success)
        assertEquals(1, extraction.apkFiles.size)
        assertEquals(0, extraction.aabFiles.size)

        val validation = SafeZipAndApkValidator.validateApkFile(extraction.apkFiles.first())
        assertTrue(validation.isValidApk)
        assertFalse(validation.isAabBundle)
    }

    @Test
    fun `validator detects Android App Bundle aab and refuses to treat it as apk`() {
        val aabFile = tempFolder.newFile("app-release.aab")
        ZipOutputStream(FileOutputStream(aabFile)).use { zos ->
            zos.putNextEntry(ZipEntry("BundleConfig.pb"))
            zos.write(ByteArray(64) { 3 })
            zos.closeEntry()
        }

        val validation = SafeZipAndApkValidator.validateApkFile(aabFile)
        assertFalse(validation.isValidApk)
        assertTrue(validation.isAabBundle)
        assertTrue(validation.errorMessage.orEmpty().contains(".aab"))
    }

    @Test
    fun `validator rejects corrupted non-zip file masquerading as apk`() {
        val fakeApk = tempFolder.newFile("fake.apk")
        fakeApk.writeText("NOT A REAL APK FILE CONTENT THAT IS LONG ENOUGH TO PASS LENGTH CHECK 1234567890")

        val validation = SafeZipAndApkValidator.validateApkFile(fakeApk)
        assertFalse(validation.isValidApk)
        assertFalse(validation.isAabBundle)
    }
}
