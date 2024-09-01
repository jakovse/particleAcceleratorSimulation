package com.jakak.particleAcceleratorSimulationClientServerVer2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class OperatorClient extends Application {

    private Circle tempIndicator;
    private Circle pressureIndicator;
    private Circle overallIndicator;
    private Label tempLabel;
    private Label pressureLabel;
    private PrintWriter out;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Operator Client");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        // Temperature sensor UI components
        tempLabel = new Label("Waiting...");
        tempLabel.setPrefWidth(200);
        tempIndicator = new Circle(10, Color.GRAY);
        grid.add(new Label("Temperature Sensor:"), 0, 0);
        grid.add(tempLabel, 1, 0);
        grid.add(tempIndicator, 2, 0);

        // Pressure sensor UI components
        pressureLabel = new Label("Waiting...");
        pressureLabel.setPrefWidth(200);
        pressureIndicator = new Circle(10, Color.GRAY);
        grid.add(new Label("Pressure Sensor:"), 0, 1);
        grid.add(pressureLabel, 1, 1);
        grid.add(pressureIndicator, 2, 1);

        // Overall system status indicator
        overallIndicator = new Circle(15, Color.GRAY);
        grid.add(new Label("System Status:"), 0, 2);
        grid.add(overallIndicator, 1, 2);
        
        //Button stopButton = new Button("Stop All Measurements");
        //stopButton.setOnAction(e -> stopAllMeasurements());
        //grid.add(stopButton, 0, 3);

        Scene scene = new Scene(grid, 400, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        primaryStage.setOnCloseRequest(event -> {
        	Platform.exit();
        });

        // Connect to the MonitoringService
        new Thread(() -> connectToMonitoringService("localhost", 5001)).start();
    }

    private void connectToMonitoringService(String host, int port) {
      try (Socket socket = new Socket(host, port);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
      	out = new PrintWriter(socket.getOutputStream(), true);
      	
        String input;
        while ((input = in.readLine()) != null) {
          processMessage(input);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    private void processMessage(String message) {
      Platform.runLater(() -> {
        if (message.startsWith("Temperature Sensor")) {
        	double value = Double.parseDouble(message.split(": ")[1]);
          tempLabel.setText(String.format("%.2f", value) + " °C");
          updateIndicator(tempIndicator, message);
        } else if (message.startsWith("Pressure Sensor")) {
        	double value = Double.parseDouble(message.split(": ")[1]);
        	pressureLabel.setText(String.format("%.2f", value) + " mbar");
          updateIndicator(pressureIndicator, message);
        } else if (message.startsWith("System Status")) {
          updateOverallIndicator(message.split(": ")[1]);
        } else if (message.equals("STOPPED")) {
         	tempLabel.setText("STOPPED");
         	pressureLabel.setText("STOPPED");
         	tempIndicator.setFill(Color.GRAY);
        	pressureIndicator.setFill(Color.GRAY);
        	overallIndicator.setFill(Color.GRAY);
        }
      });
    }
    
    private void stopAllMeasurements() {
    	if (out != null) {  
    		out.println("STOP");
    		out.flush();
    	}
    }

    private void updateIndicator(Circle indicator, String message) {
        String[] parts = message.split(" measured: ");
        double value = Double.parseDouble(parts[1]);
        if (parts[0].contains("Temperature")) {
            if (value < 20 || value > 30) {
                indicator.setFill(Color.RED);
            } else {
                indicator.setFill(Color.GREEN);
            }
        } else if (parts[0].contains("Pressure")) {
            if (value < 950 || value > 1050) {
                indicator.setFill(Color.RED);
            } else {
                indicator.setFill(Color.GREEN);
            }
        }
    }

    private void updateOverallIndicator(String status) {
        switch (status) {
            case "GREEN":
                overallIndicator.setFill(Color.GREEN);
                break;
            case "ORANGE":
                overallIndicator.setFill(Color.ORANGE);
                break;
            case "RED":
                overallIndicator.setFill(Color.RED);
                break;
        }
    }
}

