package com.komarov.osmgraphapp.configs

import com.komarov.osmgraphapp.components.TextSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val textSocketHandler: TextSocketHandler
): WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(textSocketHandler, "/socket")
            .setAllowedOriginPatterns("*")
    }
}