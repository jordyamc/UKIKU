package knf.kuma.player

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import androidx.media.AudioAttributesCompat
import androidx.media3.exoplayer.ExoPlayer

/**
 * Wrapper around a [SimpleExoPlayer] simplifies playback by automatically handling
 * audio focus using [AudioFocusRequest] on Oreo+ devices, and an
 * [AudioManager.OnAudioFocusChangeListener] on previous versions.
 */
class AudioFocusWrapper(
    private val audioAttributes: AudioAttributesCompat,
    private val audioManager: AudioManager,
    private val player: ExoPlayer
) : ExoPlayer by player {
    private var shouldPlayWhenReady = false

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (shouldPlayWhenReady || player.playWhenReady) {
                    player.playWhenReady = true
                    player.volume = MEDIA_VOLUME_DEFAULT
                }
                shouldPlayWhenReady = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (player.playWhenReady) {
                    player.volume = MEDIA_VOLUME_DUCK
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                shouldPlayWhenReady = player.playWhenReady
                player.playWhenReady = false
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                abandonAudioFocus()
            }
        }
    }
    private val audioFocusRequest by lazy { buildFocusRequest() }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) requestAudioFocus() else abandonAudioFocus()
    }

    private fun requestAudioFocus() {
        val result = requestAudioFocusOreo()

        // Call the listener whenever focus is granted - even the first time!
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            shouldPlayWhenReady = true
            audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        } else {
            Log.e("Player", "Playback not started: Audio focus request denied")
        }
    }

    private fun abandonAudioFocus() {
        player.playWhenReady = false
        abandonAudioFocusOreo()
    }

    private fun requestAudioFocusOreo(): Int = audioManager.requestAudioFocus(audioFocusRequest)

    private fun abandonAudioFocusOreo() = audioManager.abandonAudioFocusRequest(audioFocusRequest)

    private fun buildFocusRequest(): AudioFocusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes.unwrap() as AudioAttributes)
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .build()
}

private const val MEDIA_VOLUME_DEFAULT = 1.0f
private const val MEDIA_VOLUME_DUCK = 0.2f