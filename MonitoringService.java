package com.jakak.particleAcceleratorSimulationClientServerVer2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class MonitoringService implements Runnable {
	private int port;
	private List<PrintWriter> clientWriters;
	private AtomicInteger outOfRangeCount = new AtomicInteger(0);
	private List<SensorMonitor> sensors;
	private List<SensorDevice> connectedSensors;
	
	public MonitoringService(int port) {
		this.port = port;
		this.clientWriters = new CopyOnWriteArrayList<>();
		this.sensors = new CopyOnWriteArrayList<>();
		this.connectedSensors = new CopyOnWriteArrayList<>();
	}
	
	public void addSensor(String name, double minValue, double maxValue, SensorDevice sensor) {
		sensors.add(new SensorMonitor(name, minValue, maxValue));
		connectedSensors.add(sensor);
	}

	@Override
	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			
			System.out.println("Monitoring Service started on port " + port);
			
			new Thread(() -> listenForClients(serverSocket)).start();
			
			while (true) {
				try (Socket clientSocket = serverSocket.accept();
						BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
					
					String input;
					
					while((input = in.readLine()) != null) {
						System.out.println(input);
						if (input.equals("STOP")) {
							stopAllSensors();
						} else {
							String[] parts = input.split(" measured: ");
							String sensorName = parts[0];
							double value = Double.parseDouble(parts[1]);
						
							for (SensorMonitor sensor : sensors) {
								if (sensor.getName().equals(sensorName)) {
									sensor.update(value);
									broadcastUpdate(sensorName, value);
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void listenForClients(ServerSocket serverSocket) {
		while (true) {
			try {
				Socket clientSocket = serverSocket.accept();
				PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
				clientWriters.add(writer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
		
	private void broadcastUpdate(String sensorName, double value) {
		for (PrintWriter writer : clientWriters) {
			writer.println(sensorName + " measured: " + value);
		}
	}
	
	private void stopAllSensors() {
		for (SensorDevice sensor : connectedSensors) {
			sensor.stopMeasuring();
		}
		
		for (PrintWriter writer : clientWriters) {
			writer.println("STOPPED");
			writer.flush();
		}
	}

	class SensorMonitor {
		private String name;
		private double minValue;
		private double maxValue;
		boolean isOutOfRange;
		
		public SensorMonitor(String name, double minValue, double maxValue) {
			this.name = name;
			this.minValue = minValue;
			this.maxValue = maxValue;
			this.isOutOfRange = false;
		}
		
		public String getName() {
			return name;
		}
		
		public void update(double value) {
			boolean isInRange = value >= minValue && value <= maxValue;
			if (isInRange) {
				if (isOutOfRange) {
					isOutOfRange = false;
					if (outOfRangeCount.decrementAndGet() < 0) {
						outOfRangeCount.set(0);
					}
				}
			} else {
				if (!isOutOfRange) {
					isOutOfRange = true;
					outOfRangeCount.incrementAndGet();
				}
			}
			broadcastOverallStatus();
		}
		
		private void broadcastOverallStatus() {
			String status;
			int count = outOfRangeCount.get();
			if (count == 0) {
				status = "GREEN";
			} else if (count == 1) {
				status = "ORANGE";
			} else {
				status = "RED";
			}
			for (PrintWriter writer : clientWriters) {
				writer.println("System Status: " + status);
			}
		}
	}	
}

@FunctionalInterface
interface MonitoringServiceCallback {
	void updateOverallIndicator(int outOfRangeCount);
}
