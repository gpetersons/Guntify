package com.music.spotui.ui.notification

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.media.MediaPlaybackManager
import androidx.car.app.media.model.MediaPlaybackTemplate
import androidx.car.app.validation.HostValidator
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.media3.session.MediaSession
import android.support.v4.media.session.MediaSessionCompat
import com.music.spotui.R
import com.music.spotui.di.CurrentSongState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Optional Car App Library media experience on hosts supporting Car API 8+.
 * The existing PlaybackService remains the media browser for older head units.
 */
@AndroidEntryPoint
class SprotifyCarAppService : CarAppService() {
    @Inject lateinit var playbackState: CurrentSongState

    override fun createHostValidator(): HostValidator =
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }

    override fun onCreateSession(sessionInfo: SessionInfo): Session = object : Session() {
        private var controller: MediaController? = null

        init {
            lifecycle.addObserver(LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_CREATE) connectMediaSession()
                if (event == Lifecycle.Event.ON_DESTROY) {
                    controller?.release()
                    controller = null
                }
            })
        }

        override fun onCreateScreen(intent: Intent): Screen =
            SprotifyPlaybackScreen(carContext, playbackState)

        private fun connectMediaSession() {
            val context = carContext
            val future = MediaController.Builder(
                context, SessionToken(context, ComponentName(context, PlaybackService::class.java))
            ).buildAsync()
            val main = ContextCompat.getMainExecutor(context)
            future.addListener({
                try {
                    val mediaController = future.get()
                    controller = mediaController
                    val result = mediaController.sendCustomCommand(
                        SessionCommand("com.music.spotui.action.GET_CAR_SESSION_TOKEN", Bundle.EMPTY),
                        Bundle.EMPTY
                    )
                    result.addListener({
                        try {
                            val reply = result.get()
                            if (reply.resultCode == SessionResult.RESULT_SUCCESS) {
                                @Suppress("DEPRECATION")
                                val platform = reply.extras.getParcelable<android.media.session.MediaSession.Token>(
                                    "sprotify.platform.token"
                                )
                                if (platform != null) {
                                    val compat = MediaSessionCompat.Token.fromToken(platform)
                                    (context.getCarService(CarContext.MEDIA_PLAYBACK_SERVICE)
                                        as MediaPlaybackManager).registerMediaPlaybackToken(compat)
                                }
                            }
                        } catch (err: Exception) {
                            Log.w("SprotifyCar", "Unable to register media token", err)
                        }
                    }, main)
                } catch (err: Exception) {
                    Log.w("SprotifyCar", "Unable to connect media session", err)
                }
            }, main)
        }
    }
}

private class SprotifyPlaybackScreen(
    context: CarContext,
    private val state: CurrentSongState,
) : Screen(context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watch: Job? = null

    init {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    watch?.cancel()
                    watch = scope.launch {
                        snapshotFlow { CarPlaybackContextPolicy.title(state.collection.value, state.title.value) }
                            .distinctUntilChanged()
                            .collect { invalidate() }
                    }
                }
                Lifecycle.Event.ON_STOP -> { watch?.cancel(); watch = null }
                Lifecycle.Event.ON_DESTROY -> scope.cancel()
                else -> Unit
            }
        })
    }

    override fun onGetTemplate(): Template {
        val source = CarPlaybackContextPolicy.title(state.collection.value, state.title.value)
        // Deliberately no title on this action: icon-only Queue in the header.
        val queue = Action.Builder()
            .setIcon(CarIcon.Builder(IconCompat.createWithResource(
                carContext, R.drawable.ic_car_queue_only
            )).build())
            .setOnClickListener {
                screenManager.push(SprotifyQueueScreen(carContext, state))
            }
            .build()
        return MediaPlaybackTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle(source)
                    .setStartHeaderAction(Action.BACK)
                    .addEndHeaderAction(queue)
                    .build()
            )
            .build()
    }
}

private class SprotifyQueueScreen(
    context: CarContext,
    private val state: CurrentSongState,
) : Screen(context) {
    override fun onGetTemplate(): Template {
        val source = CarPlaybackContextPolicy.title(state.collection.value, state.title.value)
        val builder = ItemList.Builder()
        val currentId = state.songId.value
        for (song in state.queue.value.take(100)) {
            builder.addItem(Row.Builder()
                .setTitle((if (song.id == currentId) "▶ " else "") + song.title)
                .addText(song.singer)
                .build())
        }
        return ListTemplate.Builder()
            .setHeader(Header.Builder().setTitle(source).setStartHeaderAction(Action.BACK).build())
            .setSingleList(builder.build())
            .build()
    }
}
