package org.koitharu.kotatsu.core.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.util.ext.getParcelableExtraCompat
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.picker.ui.PageImagePickActivity

/**
 * ActivityResultContract that launches the manga picker and returns
 * the selected [Manga], or null if the user cancelled.
 *
 * [input]: optional query string pre-filled in the picker search bar.
 */
class PickMangaContract : ActivityResultContract<String, Manga?>() {

	override fun createIntent(context: Context, input: String): Intent =
		Intent(context, PageImagePickActivity::class.java).apply {
			if (input.isNotBlank()) {
				putExtra(KEY_QUERY, input)
			}
		}

	override fun parseResult(resultCode: Int, intent: Intent?): Manga? {
		if (resultCode != Activity.RESULT_OK || intent == null) return null
		return intent.getParcelableExtraCompat<ParcelableManga>(AppRouter.KEY_MANGA)?.manga
	}

	companion object {
		const val KEY_QUERY = "query"
	}
}
