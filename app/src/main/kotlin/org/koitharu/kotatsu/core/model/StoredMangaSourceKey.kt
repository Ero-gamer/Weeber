package org.koitharu.kotatsu.core.model

import org.koitharu.kotatsu.parsers.model.MangaSource

fun mangaSourceFromStoredKey(key: String?): MangaSource = MangaSource(key)
