package com.example.security

import java.net.URI

data class GitHubRepoRef(
    val owner: String,
    val repo: String
) {
    val fullName: String get() = "$owner/$repo"
    val canonicalHttpsUrl: String get() = "https://github.com/$owner/$repo"
}

object UrlValidator {

    private val allowedGitHubHosts = setOf("github.com", "www.github.com")
    private val validSegmentRegex = Regex("^[A-Za-z0-9._-]{1,100}$")
    private val folderUnsafeChars = Regex("[^A-Za-z0-9 _.-]")

    /**
     * Validates that a URL strictly uses HTTPS and has a valid host.
     * Blocks javascript:, data:, file:, content:, and cleartext http:// schemes.
     */
    fun isValidHttpsUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val uri = URI(trimmed)
            uri.scheme?.equals("https", ignoreCase = true) == true &&
                !uri.host.isNullOrBlank() &&
                !uri.host.contains("..")
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Parses a GitHub repository URL or "owner/repo" shorthand into a validated [GitHubRepoRef].
     */
    fun parseGitHubRepo(input: String): GitHubRepoRef? {
        val trimmed = input.trim().removeSuffix("/").removeSuffix(".git")
        if (trimmed.isEmpty()) return null

        // Support "owner/repo" shorthand
        if (!trimmed.contains("://") && trimmed.count { it == '/' } == 1) {
            val parts = trimmed.split('/')
            val owner = parts[0].trim()
            val repo = parts[1].trim()
            if (isValidRepoSegment(owner) && isValidRepoSegment(repo)) {
                return GitHubRepoRef(owner, repo)
            }
            return null
        }

        if (!isValidHttpsUrl(trimmed)) return null
        return try {
            val uri = URI(trimmed)
            val host = uri.host?.lowercase() ?: return null
            if (host !in allowedGitHubHosts) return null
            val segments = uri.path.orEmpty()
                .split('/')
                .filter { it.isNotBlank() }
            if (segments.size < 2) return null
            val owner = segments[0]
            val repo = segments[1].removeSuffix(".git")
            if (isValidRepoSegment(owner) && isValidRepoSegment(repo)) {
                GitHubRepoRef(owner, repo)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isValidRepoSegment(segment: String): Boolean {
        return segment.isNotBlank() &&
            segment != "." &&
            segment != ".." &&
            validSegmentRegex.matches(segment)
    }

    /**
     * Sanitizes an application name into a safe filesystem folder name free of traversal characters.
     */
    fun sanitizeFolderName(name: String): String {
        val cleaned = name.trim()
            .replace(folderUnsafeChars, "")
            .replace(Regex("\\s+"), " ")
            .trim('.', ' ')
        return if (cleaned.isBlank() || cleaned == "." || cleaned == "..") {
            "App_${System.currentTimeMillis()}"
        } else {
            cleaned.take(64)
        }
    }

    /**
     * Sanitizes an individual file name (stripping any directory separators or traversal tokens).
     */
    fun sanitizeFileName(rawName: String, fallbackName: String): String {
        val base = rawName.substringAfterLast('/').substringAfterLast('\\')
            .replace(folderUnsafeChars, "_")
            .trim('.', ' ')
        return if (base.isBlank() || base == "." || base == "..") fallbackName else base.take(120)
    }
}
