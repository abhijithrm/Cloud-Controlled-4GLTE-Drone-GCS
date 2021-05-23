//global vars..in js ypu can access these vars in any file..literally global usage
var dronesCount = 0;
const DRONES_MAP = new Map();//key :value -droneid:drone abstraction obj

var SELECTED_DRONE;//reference to currently selected drone!!!!initialized on clicking cooresponding drone item in the list on ui.
var WORLD_MAP;// REFERNCE TO MAP

google.maps.event.addDomListener(window, 'load', initializeApp);//The google.maps.event.addDomListener adds a DOM event listener, in this case to the window object, for the 'load' event, and specifies a function to run.
//ONCE WE LOAD WINDOW, INITIALIZE APP

function initializeApp() {

	WORLD_MAP = new google.maps.Map( document.getElementById('map'), {
		zoom: 2,
		center: { lat: 0, lng: 0 }
	});//set google maps to the dom element and center it to default co-ordinates

	google.maps.event.addListener(WORLD_MAP, 'click', function (event) {
		addMarker(event.latLng);//on selecting drone and clicking map, it adds marker points on the map for that drone...see lib.js for impl
	});

	document.addEventListener('keyup', function (event) {
		executeKeyboardCommand(event);//drone keyboard control
	});

	setInterval( updateSystemData, 1000);//The setInterval() method calls a function or evaluates an expression at specified intervals (in milliseconds).
}//so updateSytemData is called every 1000ms to update data on the connected drones or newly connected drone(raspi) in html page 

//get json array of drone dto objects and execute logic.
const updateSystemData = function () {
	$.ajax({
		type: 'GET',
		url: '/updateSystemInfo',
	})
		.done(function (response) {

			loadDronesData(response);//respnse will be array of droneinfo objects

			if (SELECTED_DRONE != undefined) {
				WORLD_MAP.setCenter({ lat: SELECTED_DRONE.lat, lng: SELECTED_DRONE.lng });//update map for slected drone
			}

		})
		.fail(function (data) {
			loadDronesData('[{}]');
		});
}

//executed every 1000ms if get request to /updateSystemInfo is successfull
const loadDronesData = function(data) {
	var droneDTOs = JSON.parse(data);
	$("p[id*='onlineStatus']").html('OFFLINE');

	droneDTOs.forEach(function(droneDTO){
		
		if (droneDTO == undefined || droneDTO.id == undefined) {
			return;
		}

		if(DRONES_MAP.has(droneDTO.id))
		{//if droneid already availble in the collection, that is it is already available in the list of drones, then just update html elements corresponding to that drone
			$('#onlineStatus' + droneDTO.id).html('ONLINE');
			$('#armedStatus' + droneDTO.id).html(droneDTO.state);

			var drone = DRONES_MAP.get(droneDTO.id);//get the particular dto
			drone.setPosition(droneDTO.lattitude, droneDTO.longitude, droneDTO.alt);//update map marker

			$('#infoAlt' + droneDTO.id).val(droneDTO.alt);//update altitude
			$('#infoSpeed' + droneDTO.id).val(droneDTO.speed);//update speed
			$('#infoBat' + droneDTO.id).val(droneDTO.battery);//update battery level
		}
		else
		{//in case of a new raspi/drone connection, add it to the list of connected drones and render related ui
			var drone = new Drone(droneDTO.id, droneDTO.lattitude, droneDTO.longitude);
			drone.speed = droneDTO.speed;
			drone.altitude = droneDTO.alt;//initialize drone abstraction object

			DRONES_MAP.set(droneDTO.id, drone);//add drone obj to collection

			$('.dronesList').append( renderDroneUIComponent(droneDTO));//add to drone list in html page

			//while adding html for new drone ,add click event handler too..that is whenever we find a drone with id not on the collection, initialize drone dto obj, add it to collection, render ui, attach click handler
			$('.dronesList').on("click", ".dronesList-header", function (){
				if ($(this).hasClass("active")) {
					return;
				}

				//close video feed and remove mission point marker for currently selected drone
				$(".dronesList > .active").each(function (index) {
					$(this).removeClass("active").next().slideToggle();

					var drone = DRONES_MAP.get( $(this).attr('droneId'));
					drone.stopVideoFeed();
					drone.hidePoints();
				});

				//show videofeed and mission point marker on map for newly clicked drone
				SELECTED_DRONE = DRONES_MAP.get( $(this).attr('droneId'));
				SELECTED_DRONE.showPoints();
				SELECTED_DRONE.startVideoFeed();
				
				WORLD_MAP.setZoom(18);
								
				activateViewFPV(droneDTO.id);

				$(this).toggleClass("active").next().slideToggle();

			});

			initializeDronesControls(drone.id);//atach handlers for control buttons
		}
	});//for properties and methods of Drone abstraction object, see library.js
}