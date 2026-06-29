package com.friday.assistant

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Mikrofon ile dinleme (STT) ve sesli okuma (TTS). Windows surumundeki
 * voice_engine.py'nin Android karsiligi -- ama burada Android'in kendi
 * yerlesik SpeechRecognizer/TextToSpeech API'lerini kullaniyoruz (ek
 * kutuphane veya internet-bagimli ucuncu parti servis gerekmez, Google'in
 * kendi konusma tanima motoru cihazda/Google Play Services uzerinden calisir).
 */
class VoiceController(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onStatusChange: (Status) -> Unit,
    private val onError: (String) -> Unit
) {
    enum class Status { IDLE, LISTENING, SPEAKING }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isSpeaking = false

    fun initialize() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("tr", "TR")
            } else {
                onError("Sesli okuma motoru başlatılamadı.")
            }
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Bu cihazda ses tanıma desteklenmiyor.")
            return
        }
        if (isSpeaking) {
            onError("Friday konuşurken dinleyemem, biraz bekle.")
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                onStatusChange(Status.LISTENING)
            }

            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                onStatusChange(Status.IDLE)
                if (!text.isNullOrBlank()) {
                    onResult(text)
                }
            }

            override fun onError(error: Int) {
                onStatusChange(Status.IDLE)
                // SpeechRecognizer.ERROR_NO_MATCH / ERROR_SPEECH_TIMEOUT siklikla
                // olusur (sessizlik vb.), bunlari hata olarak gostermeye gerek yok.
                if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    onError("Ses tanıma hatası: kod $error")
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        onStatusChange(Status.IDLE)
    }

    /** Verilen metni sesli okur. Okuma boyunca mikrofon devre disi kalir
     * (kendi sesini duyup komut sanmasin diye). */
    fun speak(text: String) {
        isSpeaking = true
        onStatusChange(Status.SPEAKING)
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                onStatusChange(Status.IDLE)
            }
            @Deprecated("Deprecated in API")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                onStatusChange(Status.IDLE)
            }
        })
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "friday_utterance")
    }

    fun destroy() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
