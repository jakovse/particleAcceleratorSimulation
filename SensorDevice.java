package com.jakak.particleAcceleratorSimulationClientServerVer2;

import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;


public abstract class SensorDevice {
	private boolean measuring;
	private List<String> serviceAddresses;
	
	public SensorDevice(List<String> serviceAddresses) {
		this.measuring = false;
		this.serviceAddresses = serviceAddresses;
	}


	public void startMeasuring() {
		measuring = true;
		new Thread(this::measure).start();
	}
	
	public void stopMeasuring() {
		measuring = false;
	}
	
	protected abstract double measureValue();
	
	private void measure() {
		while (measuring) {
			double value = measureValue();
			notifyServices(value);
			
			try {
				Thread.sleep(1000); 
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void notifyServices(double value) {
		for (String serviceAddress : serviceAddresses) {
			String[] parts = serviceAddress.split(":");
			String host = parts[0];
			int port = Integer.parseInt(parts[1]);
			
			try (Socket socket = new Socket(host, port);
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
				out.println(getName() + " measured: " + value);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	protected abstract String getName();
}

class SensorData implements Comparable<SensorData> {
	private String sensorName;
	private double value;
	private long timestamp;
	
	public SensorData(String sensorName, double value, long timestamp) {
		this.sensorName = sensorName;
		this.value = value;
		this.timestamp = timestamp;
	}
	
	public String getSensorName() {
		return sensorName;
	}
	
	public double getValue() {
		return value;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	@Override
	public int compareTo(SensorData other) {
		return Long.compare(this.timestamp, other.timestamp);
	}
	
	@Override
	public String toString() {
		Date date = new Date(timestamp);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		return "[" + sdf.format(date) + "] " + sensorName + ": " + value;
	}
}

class TemperatureSensor extends SensorDevice {
	public TemperatureSensor(List<String> serviceAddresses) {
		super(serviceAddresses);
	}
	
	@Override
	protected double measureValue() {
		return 22 + Math.random() * 10;
	}


	@Override
	protected String getName() {
		return "Temperature Sensor";
	}
}

class HumiditySensor extends SensorDevice {
	public HumiditySensor(List<String> serviceAddresses) {
		super(serviceAddresses);
	}
	
	@Override
	protected double measureValue() {
		return 78 + Math.random() * 10;
	}
	
	@Override
	protected String getName() {
		return "Humidity sensor";
	}
}

class PressureSensor extends SensorDevice {
	public PressureSensor(List<String> serviceAddresses) {
		super(serviceAddresses);
	}
	
	@Override
	protected double measureValue() {
		return 900 + Math.random() * 200;
	}
	
	@Override
	protected String getName() {
		return "Pressure Sensor";
	}
}

class RadiationSensor extends SensorDevice {
	public RadiationSensor(List<String> serviceAddresses) {
		super(serviceAddresses);
	}
	
	@Override
	protected double measureValue() {
		return 4.5 + Math.random() * 3;
	}
	
	@Override
	protected String getName() {
		return "Radiation Sensor";
	}
}
