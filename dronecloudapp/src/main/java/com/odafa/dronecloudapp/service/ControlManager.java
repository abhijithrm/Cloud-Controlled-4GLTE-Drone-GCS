package com.odafa.dronecloudapp.service;

import java.io.IOException; 
import java.net.ServerSocket; 
import java.net.Socket; 
import java.util.ArrayList; 
import java.util.List; 
import java.util.Map; 
import java.util.concurrent.ConcurrentHashMap; 
import java.util.concurrent.ExecutorService; 
import java.util.concurrent.Executors;

import com.odafa.dronecloudapp.configuration.ConfigReader; 
import com.odafa.dronecloudapp.dto.DataPoint; 
import com.odafa.dronecloudapp.dto.DroneInfo; 
import com.odafa.dronecloudapp.utils.DataMapper;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j @Component public class ControlManager implements Runnable { private final ServerSocket serverSocket; private final ServerSocket logStreamerSocket; private final ExecutorService serverRunner;

private final Map<String, DroneHandler> droneIdToHandler;

public ControlManager(ConfigReader configurations) {
	try {
		serverSocket = new ServerSocket( configurations.getControlServerPort());
		logStreamerSocket = new ServerSocket(configurations.getLogStreamerPort());
	} catch (IOException e) {
		log.error(e.getMessage());
		throw new RuntimeException(e);
	}
	serverRunner = Executors.newSingleThreadExecutor();
	droneIdToHandler = new ConcurrentHashMap<>();

	serverRunner.execute(this);
} 

public void run() {
	while (!serverSocket.isClosed() && !logStreamerSocket.isClosed()) {
		try 
		{
			Socket clientSocket = serverSocket.accept();
			Socket clientLogStreamerSocket = logStreamerSocket.accept();//Listens for a connection to be made to this socket and accepts it. The method blocks until a connection is made. 

			final DroneHandler handler = new DroneHandler(this, clientSocket, clientLogStreamerSocket);
			handler.activate();
			String droneIdFromClientLogStreamer = DataMapper.extractDroneIdFromNetwork(clientLogStreamerSocket);
			while(true)
			{
				if(droneIdToHandler.containsKey(droneIdFromClientLogStreamer))
				{
					droneIdToHandler.get(droneIdFromClientLogStreamer).startLogStreaming();
					break;
				}
			}

		}
		catch (Exception e) 
		{
			log.error(e.getMessage());
		}
	}
}

public void sendMissionDataToDrone(String droneId, List<DataPoint> dataPoints) {
	final DroneHandler handler = droneIdToHandler.get(droneId);
	if(handler != null) {
		handler.sendMissionData(dataPoints);
	}
}

public void sendMessageFromUserIdToDrone(String droneId, int commandCode) {
	final DroneHandler handler = droneIdToHandler.get(droneId);
	if(handler != null) {
		handler.sendCommand(commandCode);
	}
}

public List<DroneInfo> getDroneStatusAll() {
	List<DroneInfo> drones = new ArrayList<>();

	droneIdToHandler.values().forEach( handler -> {
		drones.add(handler.getDroneLastStatus());
	});
	return drones;
}

public void setControlHadlerForDroneId(String droneId, DroneHandler handler) {
	droneIdToHandler.put(droneId, handler);
}

public void removeControlHadlerForDroneId(String droneId) {
	droneIdToHandler.remove(droneId);
}

public String getLatestConsoleLogByDroneID(String droneId)
{
	String droneConsoleLog = null;
	final DroneHandler handler = droneIdToHandler.get(droneId);
	if(handler != null)
	{
		droneConsoleLog = handler.getLatestClientConsoleLogMessage();
	}
	return droneConsoleLog;
}

public String[] getConsoleLogsCollectionByDroneID(String droneId)
{
    String[] s = null;
	final DroneHandler handler = droneIdToHandler.get(droneId);
    if(handler != null)
	 s = handler.getClientConsoleLogMessageBuffer();
	return s; 
}

}