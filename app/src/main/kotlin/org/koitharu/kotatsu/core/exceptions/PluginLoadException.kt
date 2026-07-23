package org.koitharu.kotatsu.core.exceptions

data class PluginLoadException(
	val name: String,
	val e: Throwable,
)
