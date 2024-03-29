package com.komarov.osmgraphapp.components

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler


@Component
class TextSocketHandler(
    private val objectMapper: ObjectMapper
): TextWebSocketHandler() {
    private val sessions: MutableList<WebSocketSession> = mutableListOf()
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions.add(session)
        logger.info("session $session")
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session)
    }

    fun sendTextMessage(message: Any) {
        val msg = TextMessage(objectMapper.writeValueAsString(message))

        sessions.forEach {
            it.sendMessage(msg)
        }
    }
}