package com.odafa.dronecloudapp.utils;

import java.io.InputStream; 
import java.net.Socket;
import java.util.List;

import com.odafa.dronecloudapp.dto.DataPoint; 
import com.odafa.dronecloudapp.dto.DroneInfo;

public class DataMapper {

private static final int START_MISSION_CODE = 14;

public static String extractDroneIdFromNetwork(Socket droneSocket) throws Exception {
	return new String( NetworkFormatter.readNetworkMessage( droneSocket.getInputStream()));
}

public static DroneInfo fromNetworkToDroneInfo(InputStream streamIn) throws Exception {
	byte[] result = NetworkFormatter.readNetworkMessage(streamIn);
	final ProtoData.DroneData droneData = ProtoData.DroneData.parseFrom(result);
	final float speedInKmH = droneData.getSpeed() * 3.6f;

	return new DroneInfo(droneData.getDroneId(), droneData.getLatitude(), droneData.getLongitude(), speedInKmH,
			                droneData.getAltitude(), droneData.getVoltage(), droneData.getState());
}

public static String fromNetworkMessageToDroneConsoleLog(InputStream clientLogInputStream) throws Exception
{
	byte[] result = NetworkFormatter.readNetworkMessage(clientLogInputStream);
	return result.toString();
}

public static byte[] toNetworkMessage(List<DataPoint> dataPoints) {

	ProtoData.MissionData.Builder missionData = ProtoData.MissionData.newBuilder();

	for (DataPoint point : dataPoints) {
		missionData.addPoint( ProtoData.DataPoint.newBuilder()
				                                 .setLatitude(point.getLat())
				                                 .setLongitude(point.getLng())
				                                 .setSpeed(point.getSpeed())
				                                 .setAltitude(point.getHeight())
				                                 .setAction(point.getAction())
				                                 .build());
	}

	byte[] missionDataArr = ProtoData.Command.newBuilder().setCode(START_MISSION_CODE)
	                                         .setPayload( missionData.build().toByteString())
	                                         .build().toByteArray();

	return NetworkFormatter.createNetworkMessage(missionDataArr);
}

public static byte[] toNetworkMessage(int commandCode) {
	byte[] command = ProtoData.Command.newBuilder().setCode(commandCode).build().toByteArray();
	return NetworkFormatter.createNetworkMessage(command);
}

}