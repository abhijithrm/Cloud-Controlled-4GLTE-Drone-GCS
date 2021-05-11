class VideoStreamClient {

    constructor(droneId, hostname, port, endpoint) {
        this.droneId = droneId;
        this.webSocket = null;
        this.hostname = hostname;
        this.port = port;
        this.endpoint = endpoint;
    }

    activateStream() {
        this.webSocket = new WebSocket(this.getServerUrl());

        var activeDroneId = this.droneId;//this has different meaning when used inside func

        this.webSocket.onopen = function (event) {
            this.send(activeDroneId);//event handler fired when connection is made to backend ws endpoint
        }

        this.webSocket.onmessage = function (event) {
            $('#video' + activeDroneId).attr("src", "data:image/jpg;base64," + event.data);
        }//websocket enables two way comm between front end and backend endpoint. Here onmessage event handler get fired continuously as we will be recieving continuos data from backend.Updated src image data to give a video feel
    }

    send(message) {
        if (this.webSocket != null && this.webSocket.readyState == WebSocket.OPEN) {
            this.webSocket.send(message);
        }
    }

    getServerUrl() {
        return "ws://" + this.hostname + ":" + this.port + this.endpoint;
    }
}

//The Web Socket specification defines an API establishing "socket" connections between a web browser and a server. 
//In layman terms, there is a persistent connection between the client and the server and both parties 
//can start sending data at any time
//ws is the new URL schema for WebSocket connections. There is also wss, for secure WebSocket 
//connection the same way https is used for secure HTTP connections.
//Using WebSocket creates a whole new usage pattern for server side applications. 

//While traditional server stacks such as LAMP are designed around the HTTP request/response 
//cycle they often do not deal well with a large number of open WebSocket connections. 
//Keeping a large number of connections open at the same time requires an architecture that 
//receives high concurrency at a low performance cost.