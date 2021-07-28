package com.odafa.dronecloudapp.service;

import java.io.InputStream; 
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List; 
import java.util.concurrent.ArrayBlockingQueue; 
import java.util.concurrent.BlockingQueue; 
import java.util.concurrent.ExecutorService; 
import java.util.concurrent.Executors;

import com.odafa.dronecloudapp.dto.DataPoint; 
import com.odafa.dronecloudapp.dto.DroneInfo; 
import com.odafa.dronecloudapp.utils.DataMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public class DroneHandler { private static final long MAX_WAIT_TIME = 10_000L; private final String droneId;

private volatile long lastUpdateTime;
private DroneInfo lastStatus;
private  String latestDroneConsoleLogMessage;

private final Socket droneSocket;
private final InputStream streamIn;
private final OutputStream streamOut;
private final InputStream clientLogStreamIn;
private final Socket clientLogStreamerSocket;

private final BlockingQueue<byte[]> indoxMessageBuffer;
private final BlockingQueue<String> clientConsoleLogMessageBuffer;
private final ExecutorService handlerExecutor;
private final ControlManager manager;

public DroneHandler(ControlManager controlManager, Socket clientSocket, Socket clientLogStreamer) {
	this.manager = controlManager;
	this.droneSocket = clientSocket;
	this.clientLogStreamerSocket = clientLogStreamer;
	this.indoxMessageBuffer = new ArrayBlockingQueue<>(1024);
	this.clientConsoleLogMessageBuffer = new ArrayBlockingQueue<>(1024);
	this.handlerExecutor = Executors.newFixedThreadPool(3);

	try {
		this.streamIn  = droneSocket.getInputStream();
		this.streamOut = droneSocket.getOutputStream();
		this.clientLogStreamIn = clientLogStreamerSocket.getInputStream();
		droneId = DataMapper.extractDroneIdFromNetwork(droneSocket);
	} catch (Exception e) {
		close();
		throw new RuntimeException(e);
	}
	manager.setControlHadlerForDroneId(droneId, this);
	log.info("Control Connection Established ID {}, IP {} ", droneId, droneSocket.getInetAddress().toString());
}

public void activate() {
	
	handlerExecutor.execute( () -> {
		while (!droneSocket.isClosed()) {
			try {
				this.lastStatus = DataMapper.fromNetworkToDroneInfo(streamIn);
				this.lastUpdateTime = System.currentTimeMillis();
			} catch (Exception e) {
				log.info("Control Connection with {} Closed, reason: {}", droneSocket.getInetAddress().toString(), e.getMessage());
				close();
			}
		}
		close();
	});

	handlerExecutor.execute( () -> {
		while (!droneSocket.isClosed()) {
			try {
				streamOut.write( indoxMessageBuffer.take());
				streamOut.flush();
			} catch (SocketException se) {
				log.info("Socket has been closed: {}", se.getMessage());
				close();
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
	});
}

public void startLogStreaming()
{
    handlerExecutor.execute(()->{
		while (!clientLogStreamerSocket.isClosed()) 
		{
			try 
			{
				String consoleMessage = DataMapper.fromNetworkMessageToDroneConsoleLog(clientLogStreamIn);//internally calls InputStream class's read() method which is blocking until next byte of data is available.
				this.latestDroneConsoleLogMessage = new String(consoleMessage);
				this.clientConsoleLogMessageBuffer.put(this.latestDroneConsoleLogMessage);
				//this.lastUpdateTime = System.currentTimeMillis();
			} 
			catch (Exception e) 
			{
				log.info("Log streaming socket connection with {} closed, reason: {}", clientLogStreamerSocket.getInetAddress().toString(), e.getMessage());
				close();
			}
		}
		close();
	});

}

public void sendMissionData(List<DataPoint> dataPoints) {
	final byte[] message = DataMapper.toNetworkMessage(dataPoints);
	this.indoxMessageBuffer.add(message);

	log.debug("Sending Mission Data: {}", dataPoints);
}

public void sendCommand(int commandCode) {
	final byte[] message = DataMapper.toNetworkMessage(commandCode);
	this.indoxMessageBuffer.add(message);
	
	log.debug("Sending Command Code: {} For Drone ID {}", commandCode, droneId);
}

public DroneInfo getDroneLastStatus() {
	if (isMaxWaitTimeExceeded()) {
		log.warn("Maximum Wait Time for Drone ID {} exceeded. Control socket closed", droneId);
		close();
	}
	return this.lastStatus;
}

//Gets the latest console log from the Drone flight computer
public String getLatestClientConsoleLogMessage()
{
	String s = "EMPTY";
	if(this.latestDroneConsoleLogMessage != null)
	 s = new String(this.latestDroneConsoleLogMessage); 
	this.latestDroneConsoleLogMessage = null;
	return s;
}

//Gets the collection of drone console log message until now
public String[] getClientConsoleLogMessageBuffer() 
{ 
	String logQueueBuffer[] = (String[])this.clientConsoleLogMessageBuffer.toArray();
	return logQueueBuffer; 
}

private boolean isMaxWaitTimeExceeded() {
	return System.currentTimeMillis() - lastUpdateTime > MAX_WAIT_TIME;
}

private void close() 
{
	try 
	{
		droneSocket.close();
	} catch (Exception e) {
		log.error(e.getMessage());
	} 
	try 
	{
		streamIn.close();
	} catch (Exception e) {
		log.error(e.getMessage());
	} 
	try 
	{
		streamOut.close();
	} catch (Exception e) {
		log.error(e.getMessage());
	} 
	try 
	{
		clientLogStreamerSocket.close();
	} catch (Exception e) {
		log.error(e.getMessage());
	}
	try 
	{
		clientLogStreamIn.close();
	} catch (Exception e) {
		log.error(e.getMessage());
	}
	manager.removeControlHadlerForDroneId(droneId);
	handlerExecutor.shutdownNow(); 
}

}