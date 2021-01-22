metadata {
    definition(name: "Ambient Weather Device", namespace: "mikee385", author: "Alden Howard and Michael Pierce") {
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Pressure Measurement"
        capability "Illuminance Measurement"
        capability "Refresh"
        capability "Sensor"
		capability "Actuator"
        
		//Current Conditions
        attribute "weather", "string"
        attribute "weatherIcon", "string"
        attribute "dewPoint", "number"
        attribute "comfort", "number"
        attribute "feelsLike", "number"
		attribute "pressure", "string"
		
		//Precipitation
        attribute "precip_today", "number"
		attribute "precip_1hr", "number"
		
        
		//Wind
		attribute "wind", "number"
        attribute "wind_gust", "number"
        attribute "wind_degree", "number"
        attribute "wind_dir", "string"
		attribute "wind_direction", "string"
		
		//Light
		attribute "solarradiation", "number"
        attribute "uv", "number"
    }
	preferences {
        section("Preferences") {
            input "showLogs", "bool", required: false, title: "Show Debug Logs?", defaultValue: false
        }
    }
}

def refresh() {
	parent.fetchNewWeather()
}

def setWeather(weather){
	logger("debug", "Weather: "+weather)
	
	//Set temperature
	sendEvent(name: "temperature", value: weather.tempf, unit: '°F')
	
	//Set Humidity
	sendEvent(name: "humidity", value: weather.humidity, unit: '%')
    
	//Set DewPoint
	sendEvent(name: "dewPoint", value: weather.dewPoint, unit:'°F')
	
	//Set Comfort Level 
	float temp = 0.0
   
	temp = (weather.dewPoint - 35)
    if (temp <= 0) {
        temp = 0.0
    } else if (temp >= 40.0) {
        temp = 100.0
    } else {
        temp = (temp/40.0)*100.0
    }
    temp = temp.round(1)
    sendEvent(name: "comfort", value: temp)
	
	//Set Barometric Pressure
	sendEvent(name: "pressure", value: weather.baromrelin, unit: 'in')
	
	//Set Feels Like Temperature
	sendEvent(name: "feelsLike", value: weather.feelsLike, unit: '°F')
    
    //Rain
	sendEvent(name: "precip_today", value: weather.dailyrainin, unit: 'in')
	sendEvent(name: "precip_1hr", value: weather.hourlyrainin, unit: 'in')
	
	//Wind
	sendEvent(name: "wind", value: weather.windspeedmph, unit: 'mph')
	sendEvent(name: "wind_gust", value: weather.windgustmph, unit: 'mph')
	sendEvent(name: "wind_degree", value: weather.winddir, unit: '°')
	
	temp = weather.winddir
	if (temp < 22.5) { 		sendEvent(name:  "wind_direction", value: "North")
					            sendEvent(name:  "wind_dir", value: "N")
	} else if (temp < 67.5) {  sendEvent(name:  "wind_direction", value: "Northeast")
					    		sendEvent(name:  "wind_dir", value: "NE")
	} else if (temp < 112.5) {  sendEvent(name: "wind_direction", value: "East")
					    		sendEvent(name:  "wind_dir", value: "E")
	} else if (temp < 157.5) {  sendEvent(name: "wind_direction", value: "Southeast")
					    		sendEvent(name:  "wind_dir", value: "SE")
	} else if (temp < 202.5) {  sendEvent(name: "wind_direction", value: "South")
					    		sendEvent(name:  "wind_dir", value: "S")
	} else if (temp < 247.5) {  sendEvent(name: "wind_direction", value: "Southwest")
					    		sendEvent(name:  "wind_dir", value: "SW")
	} else if (temp < 292.5) {  sendEvent(name: "wind_direction", value: "West")
					    		sendEvent(name:  "wind_dir", value: "W")
	} else if (temp < 337.5) {  sendEvent(name: "wind_direction", value: "Northwest")
					    		sendEvent(name:  "wind_dir", value: "NW")
	} else 					 {  sendEvent(name:  "wind_direction", value: "North")
					    		sendEvent(name:  "wind_dir", value: "N")
	}
	
	//UV and Light
	sendEvent(name: "solarradiation", value: weather.solarradiation)
	sendEvent(name: "illuminance", value: weather.solarradiation)
        sendEvent(name: "uv", value: weather.uv)
}

private logger(type, msg){
	 if(type && msg && settings?.showLogs) {
        log."${type}" "${msg}"
    }
}
	
