package org.koitharu.kotatsu.core.network.imageproxy

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.koitharu.kotatsu.core.parser.mihon.MihonMangaSource
import coil3.Uri as CoilUri

/**
 * Coil3 Fetcher that uses a Mihon HttpSource's own OkHttpClient + headers
 * for image loading. Required for Mihon extension cover images that need
 * source-specific headers (Referer, X-Requested-With, etc.).
 *
 * Only activates when the image data is an http(s) URL AND the request
 * carries a [MihonMangaSource] in its extras. All other requests pass through.
 */
class ExternalSourceFetcher(
	private val httpSource: HttpSource,
	private val url: String,
	private val options: Options,
) : Fetcher {

	override suspend fun fetch(): FetchResult {
		val response = withContext(Dispatchers.IO) {
			httpSource.client.newCall(
				Request.Builder()
					.url(url)
					.headers(httpSource.headers)
					.build(),
			).awaitSuccess()
		}
		return SourceFetchResult(
			source = ImageSource(response.body.source(), options.fileSystem),
			mimeType = response.body.contentType()?.toString(),
			dataSource = DataSource.NETWORK,
		)
	}

	class Factory : Fetcher.Factory<CoilUri> {

		override fun create(data: CoilUri, options: Options, imageLoader: ImageLoader): Fetcher? {
			if (data.scheme != "http" && data.scheme != "https") return null
			// Only activate for Mihon sources that have an HttpSource catalog
			val mihonSource = options.extras[MIHON_SOURCE_KEY] as? MihonMangaSource ?: return null
			val httpSource = mihonSource.httpSource ?: return null
			return ExternalSourceFetcher(httpSource, data.toString(), options)
		}
	}

	companion object {
		/** Extras key used to pass a MihonMangaSource through Coil image requests */
		val MIHON_SOURCE_KEY = coil3.request.Options.Extras.Key<MihonMangaSource?>()
	}
}
