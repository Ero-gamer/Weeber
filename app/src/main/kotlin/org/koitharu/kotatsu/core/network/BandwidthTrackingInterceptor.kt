package org.koitharu.kotatsu.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp network interceptor that passively tracks per-response download bandwidth.
 *
 * Design principles for constrained devices:
 * - NO background coroutines or scheduled work — purely reactive
 * - NO polling or periodic network probes
 * - Wraps response body in a lightweight [ForwardingSource]; overhead is a few
 *   nanoseconds per [read] call (one AtomicLong add + a null check)
 * - Reports only when a response body is fully consumed (or closed); small/failed
 *   responses below [MIN_BYTES_TO_TRACK] / [MIN_DURATION_MS] are ignored to
 *   avoid noise from metadata requests
 *
 * Callers can observe [currentQuality] to react to network speed changes.
 * The quality bucket is updated synchronously on the OkHttp thread that closes
 * the response body — no additional dispatcher needed.
 */
@Singleton
class BandwidthTrackingInterceptor @Inject constructor() : Interceptor {

	private val _currentQuality = MutableStateFlow(NetworkQuality.GOOD)

	/** Observable network quality derived from recent download measurements. */
	val currentQuality: StateFlow<NetworkQuality> = _currentQuality.asStateFlow()

	/** Total bytes downloaded across all tracked responses (session-scoped). */
	private val totalBytes = AtomicLong(0L)

	/** Sliding-window sum of bandwidth samples in kbps (last [MAX_SAMPLES]). */
	private val samples = ArrayDeque<Int>(MAX_SAMPLES)
	private val samplesLock = Any()

	override fun intercept(chain: Interceptor.Chain): Response {
		val response = chain.proceed(chain.request())
		val body = response.body
		if (!response.isSuccessful || body == null) return response

		val contentLength = body.contentLength()
		val startTime = System.currentTimeMillis()

		val trackingBody = TrackingSource(
			source = body.source(),
			contentLength = contentLength,
			startTime = startTime,
			onComplete = { bytes, durationMs -> recordSample(bytes, durationMs) },
		).buffer().asResponseBody(body.contentType(), contentLength)

		return response.newBuilder().body(trackingBody).build()
	}

	private fun recordSample(bytes: Long, durationMs: Long) {
		if (bytes < MIN_BYTES_TO_TRACK || durationMs < MIN_DURATION_MS) return
		totalBytes.addAndGet(bytes)
		val kbps = ((bytes * 8L) / durationMs).toInt().coerceAtLeast(0) // bits/ms = kbps
		val avg = synchronized(samplesLock) {
			if (samples.size >= MAX_SAMPLES) samples.removeFirst()
			samples.addLast(kbps)
			samples.average().toInt()
		}
		_currentQuality.value = qualityFromKbps(avg)
	}

	private fun qualityFromKbps(kbps: Int): NetworkQuality = when {
		kbps <= 0     -> NetworkQuality.OFFLINE
		kbps < 64     -> NetworkQuality.POOR
		kbps < 512    -> NetworkQuality.MODERATE
		kbps < 4_096  -> NetworkQuality.GOOD
		else           -> NetworkQuality.EXCELLENT
	}

	private class TrackingSource(
		source: Source,
		private val contentLength: Long,
		private val startTime: Long,
		private val onComplete: (bytes: Long, durationMs: Long) -> Unit,
	) : ForwardingSource(source) {

		private var totalRead = 0L
		private var reported = false

		override fun read(sink: Buffer, byteCount: Long): Long {
			val n = super.read(sink, byteCount)
			if (n != -1L) totalRead += n
			if (!reported && (n == -1L || (contentLength > 0 && totalRead >= contentLength))) {
				reported = true
				onComplete(totalRead, System.currentTimeMillis() - startTime)
			}
			return n
		}

		override fun close() {
			if (!reported && totalRead > 0) {
				reported = true
				onComplete(totalRead, System.currentTimeMillis() - startTime)
			}
			super.close()
		}
	}

	companion object {
		private const val MIN_BYTES_TO_TRACK = 10_000L // 10 KB — ignore tiny metadata calls
		private const val MIN_DURATION_MS    = 50L     // 50 ms — ignore instant cache hits
		private const val MAX_SAMPLES        = 8       // rolling window size
	}
}
