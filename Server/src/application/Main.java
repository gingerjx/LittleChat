package application;

import java.io.File;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;

/**
 * Initialize {@link application.ServerInfo} and load all resources for javafx GUI
 * Default path to users direcotries is './directories', if you want to change it pass it as argument in cmd
 */
public class Main extends Application {	
	public static void main(String[] args) {
		if( args.length == 1 ) 
			ServerInfo.DIRS_PATH = args[0];
		File f = new File(ServerInfo.DIRS_PATH);
		if( !f.exists() || !f.isDirectory() ) {
			System.out.println("Passed path \'" + ServerInfo.DIRS_PATH + "\' doesn't exist or it is not directory");
			System.out.println("pwd: " + System.getProperty("user.dir"));
			System.exit(1);
		} else
			launch(args);
	}
	@Override
	public void start(Stage primaryStage) {
		try {
			ServerInfo.serverSocket = new ServerSocket(ServerInfo.PORT);
			System.out.println(ServerInfo.serverSocket);
			System.out.println("Server opened");
			ServerInfo.serverRunning = true;
			ServerInfo.downloadingFile = false;
			ServerInfo.sendingFile = false;
			ServerInfo.fileRejected = false;
			ServerInfo.onlineListUpdate = false;
			ServerInfo.deletingFile = false;
			ServerInfo.serverIP = "Server";
			
			ServerInfo.clients = new ConcurrentHashMap<>();
			ServerInfo.fileTransfer = new FileTransfer();
			ServerInfo.fileTransfer.trasferThreadPool = Executors.newCachedThreadPool();
			ServerInfo.joinListener = new Thread(new JoinListener());
			ServerInfo.joinListener.start();
			ServerInfo.clientsListener = new Thread(new ClientsListener());
			ServerInfo.clientsListener.start();
			
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(".."+File.separator+"gui"+File.separator+"serverview.fxml"));
			BorderPane serverWindow = (BorderPane) fxmlLoader.load();
			
			Scene scene = new Scene(serverWindow);
			scene.getStylesheets().add(getClass().getResource(".."+File.separator+"gui"+File.separator+"serverview.css").toExternalForm());
			primaryStage.setOnCloseRequest(e -> ServerInfo.closeServer());
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
			ServerInfo.closeServer();
		}
	}

}

