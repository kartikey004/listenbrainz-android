package org.listenbrainz.android.viewmodel

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.system.Os.listen
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.listenbrainz.android.di.IoDispatcher
import org.listenbrainz.android.model.Listen
import org.listenbrainz.android.repository.listens.ListensRepository
import org.listenbrainz.android.repository.preferences.AppPreferences
import org.listenbrainz.android.repository.socket.SocketRepository
import org.listenbrainz.android.util.ImagePalette
import org.listenbrainz.android.util.Utils.getCoverArtUrl
import org.listenbrainz.android.util.getPaletteFromImage
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


data class ListeningNowUIState(
    val song: Listen? = null,
    val palette: ImagePalette? = null,
    val imageURL: String? = null
) {
    val isListeningNow: Boolean
        get() = song != null
}

@HiltViewModel
class ListeningNowViewModel @Inject constructor(
    private val socketRepository: SocketRepository,
    private val appPreferences: AppPreferences,
    private val listensRepository: ListensRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _listeningNowUIState = MutableStateFlow(ListeningNowUIState())
    val listeningNowUIState = _listeningNowUIState.asStateFlow()
    var dismissJob: Job? = null

    init {
        viewModelScope.launch(ioDispatcher) {
            appPreferences.username.getFlow().collectLatest { username ->
                fetchListenFromAPI(username)
                Log.d("Socket listening", "Listening for $username")
                socketRepository
                    .listen { username }
                    .collectLatest { listen ->
                        updateUIState(listen)
                    }
            }
        }
    }

    private suspend fun fetchListenFromAPI(username: String) {
        val result = listensRepository.getNowPlaying(username)
        if (result.isSuccess) {
            val listen = result.data?.payload?.listens?.firstOrNull()
            if (listen == null) {
                _listeningNowUIState.update {
                    ListeningNowUIState()
                }
                Log.d(TAG, "fetchListenFromAPI: No listen found")
                return
            }
            Log.d(TAG, "fetchListenFromAPI: $listen")

            updateUIState(listen)
        } else if (result.isFailed) {
            Log.d(TAG, "fetchListenFromAPI: ${result.error?.toast}")
        }
    }

    private suspend fun updateUIState(listen: Listen?) {
        if (listen == null) {
            _listeningNowUIState.value = ListeningNowUIState()
            return
        }

        _listeningNowUIState.update {
            ListeningNowUIState(
                imageURL = getCoverArtUrl(
                    caaReleaseMbid = listen.trackMetadata.mbidMapping?.caaReleaseMbid,
                    caaId = listen.trackMetadata.mbidMapping?.caaId,
                    size = 500
                ),
                song = listen
            )
        }

        dismissJob?.cancelAndJoin()
        dismissJob = viewModelScope.launch {
            delay(listen.dismissDurationMs)
            if (_listeningNowUIState.value.song == listen) {
                _listeningNowUIState.value = ListeningNowUIState()
            }
        }
    }

    fun updatePalette(context: Context) {
        val url = listeningNowUIState.value.imageURL ?: return
        viewModelScope.launch {
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()

                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    bitmap?.let { bitmap ->
                        _listeningNowUIState.update {
                            it.copy(palette = getPaletteFromImage(bitmap))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("ListeningNowLayout", "Error loading socket image palette: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "ListeningNowViewModel"

        /** Time left to dismiss this [Listen] if it was a listening now.*/
        val Listen.dismissDurationMs: Long
            get() {
                val listenDurationMs = trackMetadata
                    .additionalInfo
                    ?.durationMs
                    ?.toLong()
                    // Default to 6 minutes for now listening dismiss
                    ?: 6.minutes.inWholeMilliseconds

                val delayToDismiss = if (listenedAt != null) {
                    val durationCompleted = System.currentTimeMillis() - listenedAt.seconds.inWholeMilliseconds
                    (listenDurationMs - durationCompleted).coerceAtLeast(0)
                } else {
                    listenDurationMs
                }

                return delayToDismiss
            }
    }
}