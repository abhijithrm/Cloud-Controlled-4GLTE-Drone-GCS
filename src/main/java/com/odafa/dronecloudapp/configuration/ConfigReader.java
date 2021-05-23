package com.odafa.dronecloudapp.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

//By using getter /setter annotation, we can have getters and setters pre-defined for ConfigReader obj props
@Getter
@Setter
@Component
public class ConfigReader {

    @Value("${app.drone.video-ws-endpoint}")
	private String videoWsEndpoint;

    @Value("${app.drone.control-server-port}")
	private int controlServerPort;

    @Value("${app.drone.video-server-port}")
	private int videoServerPort;

    @Value("${app.drone.drone-id-length}")
	private int droneIdLength;

    @Value("${app.drone.default-speed}")
	private int defaultSpeed;

    @Value("${app.drone.default-altitude}")
	private int defaultAltitude;
    
}

//when app is initialized, app.yml file is parsed and we can map values to any class property we like using Value annotation

/*@Component is an annotation that allows Spring to automatically detect our custom beans.

In other words, without having to write any explicit code, Spring will:

    Scan our application for classes annotated with @Component
    Instantiate them and inject any specified dependencies into them
    Inject them wherever needed

However, most developers prefer to use the more specialized stereotype annotations to serve this 
function.*/