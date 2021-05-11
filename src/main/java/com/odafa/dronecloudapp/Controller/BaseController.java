package com.odafa.dronecloudapp.Controller;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;


import com.odafa.dronecloudapp.configuration.ConfigReader;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//http endpoints
@Slf4j
@Controller
@RequiredArgsConstructor 
public class BaseController{

    private final ConfigReader configurations;//@Component used. Spring will inject object.

    @GetMapping("/")
    public String indexPage()
    {
        //log.debug("Index Page Opened");
        return "index";
    }


    //video streaming server endpoint.Server call when we access the mapped url from browser
    @GetMapping("/v/{droneId}")
    public String getVideoFeed(Model model, @PathVariable("droneId") String droneId)
    {
        //SET MODEL PROPERTIES to be used in front end
        model.addAttribute("publicIp", getPublicIpAddress());
		model.addAttribute("droneId", droneId);
		model.addAttribute("videoEndpoint", configurations.getVideoWsEndpoint());


        //log.debug("Index Page Opened");
        return "video";
    }

    //Helper method to get the host server ip address dynamically.Can also be configured through app yml, but this a better impl
    private String getPublicIpAddress() {
		String ip = "";
		try {
			final URL whatismyip = new URL("http://checkip.amazonaws.com");//amazon free service to find ip

            /*The openStream() method of the URL class opens a connection to the URL which the class represents 
and then returns an InputStream for reading from that connection.*/
			try(final BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()))){
				ip = in.readLine();
			}

		} catch (Exception e) {
			//log.error(e.getMessage());
		}
		return ip;
	}
}