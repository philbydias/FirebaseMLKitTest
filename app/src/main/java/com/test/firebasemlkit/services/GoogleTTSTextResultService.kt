package com.test.firebasemlkit.services

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

class GoogleTTSTextResultService(context: Context): TextResultConsumer {
    private var tts: TextToSpeech? = null
    private var responder: ( () -> Unit )? = null

    init {
        tts = TextToSpeech(context, TextToSpeech.OnInitListener {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onError(utteranceId: String?) {
                    responder?.invoke()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    responder?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    responder?.invoke()
                }

                override fun onStart(utteranceId: String?) {
                }

            })
        })
    }

    override fun registerCompletionResponder(responder: () -> Unit) {
        this.responder = responder
    }

    override fun consume(text: String) {
        val bundle = Bundle()
        bundle.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC.toString())
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, null)
    }
}