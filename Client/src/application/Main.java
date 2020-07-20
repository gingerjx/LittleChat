package application;

import java.io.File;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;


public class Main extends Application {
	
	public static void main(String[] args) {
		launch(args);
	}
	
	/**
	 * Load all GUI resources and starts loginview
	 */
	@Override
	public void start(Stage primaryStage) {
		try {
			primaryStage.setOnCloseRequest(e -> Connection.closeClient());
			VBox loginWindow = (VBox) FXMLLoader.load(getClass().getResource(".."+File.separator+"gui"+File.separator+"loginview.fxml"));
			loginWindow.getStylesheets().add(getClass().getResource(".."+File.separator+"gui"+File.separator+"login.css").toExternalForm());
			primaryStage.setTitle("Little Chat");
			primaryStage.setScene(new Scene(loginWindow));
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	

}
