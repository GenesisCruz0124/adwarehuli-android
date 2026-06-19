package com.genesiscruz.adwarehuli.data.net

import android.content.Context
import com.genesiscruz.adwarehuli.domain.Constants
import com.genesiscruz.adwarehuli.domain.model.DomainCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Loads the categorized domain blocklist (bundled as app assets) and matches
 * captured domains against it by exact match or subdomain-suffix match, e.g.
 * "ads.example.com" matches a blocklist entry of "example.com".
 *
 * A user can replace any category's list from device storage; the override
 * is persisted under [Context.getFilesDir] and takes priority over the
 * bundled asset on the next load. There is no remote/auto-updating list —
 * that's out of scope for an on-device-only tool.
 */
class BlocklistMatcher(private val context: Context) {

    @Volatile
    private var domainsByCategory: Map<DomainCategory, Set<String>> = emptyMap()

    suspend fun load() = withContext(Dispatchers.IO) {
        domainsByCategory = Constants.BLOCKLIST_ASSET_FILES.mapNotNull { (categoryName, assetPath) ->
            val category = runCatching { DomainCategory.valueOf(categoryName) }.getOrNull() ?: return@mapNotNull null
            val override = overrideFile(category)
            val lines = if (override.exists()) {
                override.readLines()
            } else {
                context.assets.open(assetPath).bufferedReader().readLines()
            }
            category to lines.parseDomainLines()
        }.toMap()
    }

    /** Returns the flagged category for [domain], or null if it isn't on any list. */
    fun classify(domain: String): DomainCategory? {
        val normalized = domain.trimEnd('.').lowercase()
        for ((category, domains) in domainsByCategory) {
            if (matches(normalized, domains)) return category
        }
        return null
    }

    suspend fun importOverride(category: DomainCategory, source: File) = withContext(Dispatchers.IO) {
        val parsed = source.readLines().parseDomainLines()
        overrideFile(category).apply {
            parentFile?.mkdirs()
            writeText(parsed.joinToString("\n"))
        }
        domainsByCategory = domainsByCategory + (category to parsed)
    }

    private fun overrideFile(category: DomainCategory): File =
        File(File(context.filesDir, "blocklist_overrides"), "${category.name}.txt")

    private fun matches(domain: String, blocklist: Set<String>): Boolean {
        if (domain in blocklist) return true
        var suffix = domain
        while (true) {
            val dotIndex = suffix.indexOf('.')
            if (dotIndex < 0) return false
            suffix = suffix.substring(dotIndex + 1)
            if (suffix.isEmpty()) return false
            if (suffix in blocklist) return true
        }
    }

    private fun List<String>.parseDomainLines(): Set<String> = this
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .toSet()
}
