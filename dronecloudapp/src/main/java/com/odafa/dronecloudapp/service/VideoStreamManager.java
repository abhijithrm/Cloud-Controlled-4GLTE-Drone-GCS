package com.odafa.dronecloudapp.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.odafa.dronecloudapp.configuration.ConfigReader;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class VideoStreamManager implements Runnable {
    
	private static final int UDP_MAX_SIZE = 65507;

    private final int ID_LENGTH;

	private final Map<String, Set<WebSocketSession>> droneIdToWebSocketSession;

    private final DatagramSocket videoReceiverDatagramSocket;
	private final ExecutorService serverRunner;


    //cstr
    public VideoStreamManager(ConfigReader configuration)
    {//Spring will add the object to context and automatically inject ConfigReader dependency
try{
    videoReceiverDatagramSocket = new DatagramSocket(configuration.getVideoServerPort());//initialize datagram socket by taking value from config
}//Java DatagramSocket class represents a connection-less socket for sending and receiving datagram packets.
catch(IOException e)
{
    log.error(e.getMessage(), e);
    throw new RuntimeException(e);
}
serverRunner = Executors.newSingleThreadExecutor();//A single thread pool can be obtainted by calling the static newSingleThreadExecutor() method of Executors class.
//Where newSingleThreadExecutor method creates an executor that executes a single task at a time.
droneIdToWebSocketSession= new ConcurrentHashMap<>();

ID_LENGTH = configuration.getDroneIdLength();
activate();
    
}

    public void activate()
    {
        serverRunner.execute(this);//as 'this' points to the current obj and as it implements runable, i think it executes the run method of the object on object initialization by spring. run method will be executed by thread pool
        log.info(" Video stream manager is active");
    }


	//UNtil DataGRamSocket connection is open, recive the datagram packet from raspi video service, separate drone id and jpg img data from packet, get the sessions from the Map collection using the droneid, and send jpg data back in the session which is open, from those sessions corresponding to droneid 
    public void run() {
        while(!videoReceiverDatagramSocket.isClosed()){//until the datagramsocket conn is open
            try {
				byte[] buf = new byte[UDP_MAX_SIZE];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				
				videoReceiverDatagramSocket.receive(packet);

				String droneId = new String( packet.getData(), 0, ID_LENGTH);//first byte in packet from raspi is droneid
				String data = new String(packet.getData(), ID_LENGTH, packet.getLength());//rest are img data
                
				Set<WebSocketSession> droneIdWebSessions = droneIdToWebSocketSession.get(droneId);//get the hashset collectio of sessions for the droneid
				
				if (droneIdWebSessions == null || droneIdWebSessions.isEmpty()) {
                    continue;
				}
                
				Iterator<WebSocketSession> it = droneIdWebSessions.iterator();//use iterator to iterate through collection and remove closed sessions
				
				while(it.hasNext()) {
					WebSocketSession session = it.next();
					if (!session.isOpen()) {
						it.remove();
						continue;
					}
					session.sendMessage(new TextMessage(data));
				}

            } catch(Exception e) {
				log.error(e.getMessage());
            }
        }
    }

	//set websocket sessions for drone id in the map
    public void setVideoWebSocketSessionForDroneId(WebSocketSession session, String droneId) {
		/*this piece of code is designed to be thread safe. This method is called in 
        videosockethandler's handleTextMessage handler, which will be called every time there is a 
        servlet request(i.e every time a user access video.html). Each user/servlet request runs 
        in a separate thread, so this part should be 
        thread safe. Adding to collection is ts*/

        Set<WebSocketSession> droneIdSessions = droneIdToWebSocketSession.putIfAbsent(droneId, new HashSet<>());
		if(droneIdSessions == null) {
			droneIdSessions = droneIdToWebSocketSession.get(droneId);
		} 
		droneIdSessions.add(session);
		log.debug("Drone ID {} has {} active Web Socket Sessions", droneId, droneIdSessions.size());
	}

    public boolean isServerClosed() {
		return videoReceiverDatagramSocket.isClosed();
	}

	public void shutdown() {
		if (!videoReceiverDatagramSocket.isClosed()) {
			try {
				videoReceiverDatagramSocket.close();
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
		serverRunner.shutdown();
	}

}
