package com.jakak.particleAcceleratorSimulationClientServerVer2;

import java.util.Arrays;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ParticleAcceleratorSimulation {
	
	private TemperatureSensor tempSensor;
	private PressureSensor pressureSensor;
	
	public static void main(String[] args) {
		new ParticleAcceleratorSimulation().startSimulation();
	}
	
	public void startSimulation() {
		String dateString = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String logFilePath = "sensor_data_log_" + dateString + ".txt";
		
		new Thread(new ArchivingService(5000, logFilePath)).start();
		
		MonitoringService monitoringService = new MonitoringService(5001);
		new Thread(monitoringService).start();

		List<String> serviceAddresses = Arrays.asList("localhost:5000", "localhost:5001");

		tempSensor = new TemperatureSensor(serviceAddresses);
		pressureSensor = new PressureSensor(serviceAddresses);
		monitoringService.addSensor("Temperature Sensor", 20, 30, tempSensor);
		monitoringService.addSensor("Pressure Sensor", 950, 1050, pressureSensor);
		
		tempSensor.startMeasuring();
		pressureSensor.startMeasuring();
	}
}
