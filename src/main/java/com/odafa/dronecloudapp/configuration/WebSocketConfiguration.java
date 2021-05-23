package com.odafa.dronecloudapp.configuration;

import com.odafa.dronecloudapp.controller.VideoSocketHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import lombok.RequiredArgsConstructor;

//Register the WebSocket Handler
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketConfigurer {
	
    private final VideoSocketHandler videoSocketHandler;
	private final ConfigReader configurations;

    /*since we will be dealing with binary messages in addition to text messages, it is a good idea to set the max binary message size. This is a value stored on the server container. You can override this value by injecting a new server container factory as part of your WebSocketConfiguration.*/
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(1024000);//This will allow for an image up to 1 MB in size to be uploaded.
        return container;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(videoSocketHandler, configurations.getVideoWsEndpoint()).setAllowedOrigins("*");
    }//register sockethandler for specified ws endpoint in configuarations
}