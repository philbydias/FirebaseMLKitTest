package com.test.firebasemlkit.services

interface TextResultConsumer {
    fun registerCompletionResponder(responder: () -> Unit)
    fun consume(text: String)
}