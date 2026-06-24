package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object AudioSynth {
    private var isSoundEnabled = true

    fun setSoundEnabled(enabled: Boolean) {
        isSoundEnabled = enabled
    }

    private fun playTone(
        startFreq: Float,
        endFreq: Float,
        durationMs: Int,
        type: String = "sine"
    ) {
        if (!isSoundEnabled) return

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val buffer = ShortArray(numSamples)

                var currentFreq = startFreq
                val freqStep = (endFreq - startFreq) / numSamples

                var angle = 0f
                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val volume = when (type) {
                        "explosion" -> (1f - progress) * 0.7f
                        else -> (1f - progress) * 0.5f // smooth fade out
                    }

                    val sampleValue = if (type == "explosion") {
                        val noise = (Math.random() * 2.0 - 1.0).toFloat()
                        val rumble = sin(angle)
                        (noise * 0.4f + rumble * 0.6f) * volume * Short.MAX_VALUE
                    } else if (type == "triangle") {
                        val v = (angle % (2 * Math.PI)) / (2 * Math.PI)
                        val triang = if (v < 0.5f) v * 4 - 1 else 3 - v * 4
                        triang * volume * Short.MAX_VALUE
                    } else {
                        sin(angle) * volume * Short.MAX_VALUE
                    }

                    buffer[i] = sampleValue.toInt().toShort()

                    // Advance tone frequency
                    currentFreq += freqStep
                    angle += (2 * Math.PI * currentFreq / sampleRate).toFloat()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                // Wait for playback and release
                kotlinx.coroutines.delay(durationMs.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playPop() {
        playTone(600f, 1200f, 80, "sine")
    }

    fun playPerfect(comboCount: Int) {
        val baseFreq = 261.63f // C4
        val scale = listOf(1f, 1.125f, 1.25f, 1.5f, 1.667f, 2.0f, 2.25f, 2.5f, 3.0f, 3.33f, 4.0f)
        val mult = scale[(comboCount - 1).coerceIn(0, scale.lastIndex)]
        val freq = baseFreq * mult
        playTone(freq, freq * 1.02f, 180, "sine")
    }

    fun playSlice() {
        playTone(450f, 90f, 130, "triangle")
    }

    fun playStack() {
        playTone(140f, 80f, 120, "sine")
    }

    fun playBomb() {
        playTone(250f, 30f, 400, "explosion")
    }

    fun playWin() {
        playTone(523.25f, 523.25f, 150, "sine") // C5
        CoroutineScope(Dispatchers.Default).launch {
            kotlinx.coroutines.delay(120)
            playTone(659.25f, 659.25f, 300, "sine") // E5
        }
    }

    fun playPowerUp() {
        playTone(300f, 800f, 250, "triangle")
    }

    fun playGameOver() {
        playTone(300f, 100f, 600, "triangle")
    }

    fun playTicking() {
        playTone(800f, 800f, 30, "triangle")
    }
}
