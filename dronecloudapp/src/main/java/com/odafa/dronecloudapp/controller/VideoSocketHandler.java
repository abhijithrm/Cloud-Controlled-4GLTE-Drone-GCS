package com.odafa.dronecloudapp.controller;
import java.io.IOException;

import com.odafa.dronecloudapp.service.VideoStreamManager;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//Create the WebSocket Handler
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoSocketHandler extends AbstractWebSocketHandler {
	//first 2 handlers are overridden just for debugging purposes 
	
	private final VideoStreamManager videoStreamManager;

	//when a socket connection is made, here it recieves a session object. We can send data back to the caller(video.html/library.js) through this session obj
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		log.debug("WebSocket Connection OPEN. Session {} IP {}", session.getId(), session.getRemoteAddress());//remote ip address of the calling machine
	}
    
    @Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.debug("WebSocket Connection CLOSED. Session {} IP {} {}", 
                       session.getId(), session.getRemoteAddress(), closeStatus);
	}
	
    //handler called when we sent a message to the socket.check video.html/lib.js. From lib.js, for open event, we call send, ans sends drone id.
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage droneId) throws IOException {
    	videoStreamManager.setVideoWebSocketSessionForDroneId(session, droneId.getPayload());// we use drone id to extract udp data coming from ras pi and send it back in session obj
    } //here we recieve the drone id from front end, we use it to split and get related the udp message from raspi and send img data back to client in session obj.
	
	/*AbstractWebSocketHandler requires you to implement two methods, handleTextMessage and 
	handleBinaryMessage which are called when a new text or binary message are received.*/
    
	/*In order to use the WebSocketHandler, it must be registered in Spring's WebSocketHandlerRegistry*/
}