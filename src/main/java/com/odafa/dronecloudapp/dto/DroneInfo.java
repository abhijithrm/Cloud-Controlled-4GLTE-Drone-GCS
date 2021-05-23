package com.odafa.dronecloudapp.dto;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

//Object will be sent from backend to updateSystemData() in app.js
@ToString
@RequiredArgsConstructor
public class DroneInfo {
	private final String id;
	private final double lattitude;
	private final double longitude;
	private final double speed;
	private final double alt;
	private final double battery;
	private final String state;
}