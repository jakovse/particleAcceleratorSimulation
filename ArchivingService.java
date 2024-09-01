package com.jakak.particleAcceleratorSimulationClientServerVer2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.PriorityBlockingQueue;

public class ArchivingService implements Runnable {
	private int port;
	private PriorityBlockingQueue<SensorData> archiveQueue;
	private String logFilePath;
	
	public ArchivingService(int port, String logFilePath) {
		this.port = port;
		this.archiveQueue = new PriorityBlockingQueue<>();
		this.logFilePath = logFilePath;
	}
	
	@Override
	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(port);
				BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFilePath, true))) {
			
			System.out.println("Archiving Service started on port " + port);
			
			while (true) {
				try (Socket clientSocket = serverSocket.accept();
						BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
					
					String input;
					while ((input = in.readLine()) != null) {
						SensorData data = parseSensorData(input);
						if (data != null) {
							archiveQueue.offer(data);
							writeDataToFile(logWriter, data);
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
	
	private SensorData parseSensorData(String input) {
		try {
			String[] parts = input.split(" measured: ");
			String sensorName = parts[0];
			double value = Double.parseDouble(parts[1]);
			long timestamp = System.currentTimeMillis();
			
			return new SensorData(sensorName, value, timestamp);
		} catch (Exception e) {
			System.err.println("[ArchiveService] Failed to parse sensor data: " + input);
			return null;
		}
	}
	
	private void writeDataToFile(BufferedWriter logWriter, SensorData data) {
		try {
			logWriter.write(data.toString());
			logWriter.newLine();
			logWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

