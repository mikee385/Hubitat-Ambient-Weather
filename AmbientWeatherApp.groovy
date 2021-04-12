definition(name: "Ambient Weather API", namespace: "mikee385", author: "Alden Howard and Michael Pierce", description: "A simple api for providing ambient weather access", iconUrl: "", iconX2Url: "")

preferences {
    page(name: "page1", title: "Log In", nextPage: "page2", uninstall: true) {
        section {
            input(name: "applicationKey", title: "Application Key", type: "text", required: true)
            input(name: "apiKey", title: "API Key", type: "text", required: true)
        }
    }
    
    page(name: "page2")
    page(name: "page3")
}

def page2() {
    def stations = []
    def stationMacs = []
    try {
        stations = getStations()
        
        stations.each { stationMacs << it.macAddress }
    } catch(groovyx.net.http.HttpResponseException e) {
        //then unauthorized
        return dynamicPage(name: "page2", title: "Error", nextPage: "page1", uninstall: true) {
            section {
                paragraph("There was an error authorizing you. Please try again.")
            }
        }
    }
    
   	log.debug("Got stations: " + stations)
    
	return dynamicPage(name: "page2", title: "Select Station", nextPage: "page3", uninstall: true) {
		section {
			input(name: "station", title: "Station", type: "enum", options: stationMacs, required: true)
            input(name: "refreshInterval", title: "Refresh Interval (in minutes)", type: "number", range: "1..3600", defaultValue: 1, required: true)
		}
		
		section {
            input name: "alertOffline", type: "bool", title: "Alert when offline?", defaultValue: false
            input "offlineDuration", "number", title: "Minimum time before offline (in minutes)", required: true, defaultValue: 60
        }
        
        section {
            input "notifier", "capability.notification", title: "Notification Device", multiple: false, required: true
        }
	}
}

def page3() {
    dynamicPage(name: "page3", title: "Confirm Settings", install: true, uninstall: true) {
        section {
            paragraph("Selected station: $station")
            paragraph("Refresh interval: $refreshInterval minute(s)")
            paragraph("Offine duration: $offlineDuration minute(s)")
            paragraph("Device to notify: $notifier")
        }
        
        section {
            paragraph("Press done to finish")
        }
    }
}

//lifecycle functions
def installed() {
    log.debug("Installed")
    
    addDevice()
    
    initialize()
    
    runEvery5Minutes(fetchNewWeather)
}

def updated() {
    log.debug("Updated")
    
    unsubscribe()
    unschedule()
    installed()
    initialize()
}

def initialize() {
    fetchNewWeather()
    
    //chron schedule, refreshInterval is int
    def m = refreshInterval
    def h = Math.floor(m / 60)
    m -= h * 60
    
    m = m == 0 ? "*" : "0/" + m.toInteger()
    h = h == 0 ? "*" : "0/" + h.toInteger()
    
    log.debug("Set CHRON schedule with m: $m and h: $h")
    
    schedule("0 $m $h * * ? *", fetchNewWeather)
    
    heartbeat()
}

//children
def addDevice() {
    def deviceExists = false
    for (device in getChildDevices()) {
        if (device.deviceNetworkId == "AWTILE-$station") {
            deviceExists = true
        }
    }
    if (!deviceExists) {
        addChildDevice("CordMaster", "Ambient Weather Device", "AWTILE-$station", null, [completedSetup: true])
    }
}

//fetch functions
def getStations() throws groovyx.net.http.HttpResponseException {
    def data = []
    
    def params = [
        uri: "https://api.ambientweather.net/",
        path: "/v1/devices",
        query: [applicationKey: applicationKey, apiKey: apiKey]
    ]
    
    requestData("/v1/devices", [applicationKey: applicationKey, apiKey: apiKey]) { response ->
        data = response.data
    }
        
    return data
}

def getWeather() throws groovyx.net.http.HttpResponseException {
    def data = []
    
    requestData("/v1/devices/$station", [applicationKey: applicationKey, apiKey: apiKey, limit: 1]) { response ->
        data = response.data
    }
        
	return data[0]
}

def requestData(path, query, code) {
    def params = [
        uri: "https://api.ambientweather.net/",
        path: path,
        query: query
    ]
    
    httpGet(params) { response ->
        code(response)
    }
}

//loop
def fetchNewWeather() {
        
    def weather = getWeather()
    
    //log.debug("Weather: " + weather)
	
    if (weather) {
        heartbeat()
        childDevices[0].setWeather(weather)
	} else {
	    state.healthStatus = "unhealthy"
	    log.error("Unable to fetch weather data")
	}
}

def heartbeat() {
    unschedule("healthCheck")
    state.healthStatus = "online"
    runIn(60*offlineDuration, healthCheck)
}

def healthCheck() {
    state.healthStatus = "offline"
    if (alertOffline) {
        notifier.deviceNotification("${getLabel()} is offline!")
    }
}

