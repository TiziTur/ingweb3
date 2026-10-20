package ar.edu.iua.iw3.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración de WebSockets (STOMP), igual que en la clase del profesor.
 * Todavía no se usa: queda preparada para el final, donde el frontend se suscribe
 * a /topic/... para ver las órdenes y las alarmas en tiempo real.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// Broker en memoria: los suscriptores escuchan destinos que empiezan con /topic
		config.enableSimpleBroker("/topic");
		// Prefijo para los mensajes que mandan los clientes al servidor
		config.setApplicationDestinationPrefixes("/ws");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// Punto de conexión del frontend. SockJS es una alternativa si el navegador no soporta WebSocket.
		registry.addEndpoint("/realtime");
		registry.addEndpoint("/realtime").withSockJS();
	}
}
