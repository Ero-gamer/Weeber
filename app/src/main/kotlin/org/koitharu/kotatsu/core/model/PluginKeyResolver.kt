package org.koitharu.kotatsu.core.model

import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.filter.data.SavedFiltersRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves and normalises legacy plugin source keys that were stored in a
 * short-form format (just sourceName, without the plugin JAR prefix).
 *
 * Legacy migration is a best-effort operation: if the DAO methods for
 * rewriting stored keys are not yet present, the function is a no-op and
 * no data is lost (new installs have no legacy keys to migrate).
 */
@Singleton
class PluginKeyResolver @Inject constructor() {

	fun uniqueLegacyShortToCompound(): Map<String, String> {
		val sources = MangaSourceRegistry.snapshot.sources
		if (sources.isEmpty()) return emptyMap()
		val plugins = sources.filterIsInstance<PluginMangaSource>()
		val byShort = plugins.groupBy { it.sourceName }
		val out = LinkedHashMap<String, String>()
		for ((short, list) in byShort) {
			if (list.size != 1) continue
			if (':' in short) continue
			val compound = list.single().name
			if (compound != short) out[short] = compound
		}
		return out
	}

	/**
	 * Attempts to normalize legacy short plugin source keys to their full
	 * compound form. This is a no-op if there are no legacy keys or if
	 * the required DAO queries are unavailable in this schema version.
	 */
	suspend fun normalize(database: MangaDatabase, savedFiltersRepository: SavedFiltersRepository) {
		// Legacy key normalization requires DAO methods (mergeLegacyPluginSourceKeys,
		// rewriteStoredSourceKey) that are not present in the current schema.
		// This is intentionally a no-op until those migrations are applied.
		// New installs produce compound keys from the start; existing installs
		// with short-form keys retain them without breaking functionality.
	}
}
