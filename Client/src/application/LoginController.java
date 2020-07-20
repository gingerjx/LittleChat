package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Controller of loginview
 */
public class LoginController {
	@FXML private TextField usernameTF;
	@FXML private TextField directoryTF;
	@FXML private TextField serverIPTF;
	@FXML private TextField portTF;
	@FXML private Label errorLabel;
	private DirectoryChooser directoryChooser;
	
	/**
	 * Initialize {@link javafx.stage.DirectoryChooser}
	 */
	@FXML
	void initialize(){
	    directoryChooser = new DirectoryChooser();
	    directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
	}
	
	/**
	 * Handle MouseEvent of login button
	 * @param ev MouseEvent parameter
	 * @throws IOException IOException if an I/O error occurs
	 */
	@FXML
	void login(MouseEvent ev) throws IOException{
		String username = usernameTF.getText();
		String directoryName = directoryTF.getText();
		String serverIP = serverIPTF.getText();
		int serverPort = Integer.parseInt(portTF.getText());
		System.out.println(username + " "  +directoryName+ " " +serverIP+   " " +serverPort  );
		errorLabel.setText("");
		establishConnection(username,directoryName,serverIP,serverPort,ev);
	}
	
	/**
	 * Handle MouseEvent of choose directory button
	 * @param ev MouseEvent parameter
	 */
	@FXML
	void chooseDirectory(MouseEvent ev){
		Stage primStage = (Stage) ((Node)ev.getSource()).getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(primStage);
        if( selectedDirectory == null ) {
        	errorLabel.setText("No directory have been choosen.");
        } else {
        	errorLabel.setText("");
        	directoryTF.setText(selectedDirectory.getAbsolutePath());
        }
	}
	
	/**
	 * Tries to establish connections based on passed parameters
	 * @param username clients username
	 * @param directoryName selected local directory name
	 * @param serverIP ip of server where client want to join in
	 * @param serverPort port of server where client want to join in
	 * @param ev MouseEvent parameter
	 */
	void establishConnection(String username, String directoryName, String serverIP, int serverPort, MouseEvent ev) {
		Connection.username = username;
		Connection.directoryName = directoryName;
		Connection.serverIP = serverIP;
		Connection.serverPort = serverPort;
		
		File directory = new File(directoryName);
		if( !directory.exists() ) {
			errorLabel.setText("Passed path \'" + directoryName + "\' doesn't exist.");
			return;
		} else if( !directory.isDirectory() ){
			errorLabel.setText("Passed path is not a directory path.");
			return;
		}else if( !directory.canRead() || !directory.canWrite() ) {
			errorLabel.setText("Application has no access to passed directory.");
			return;
		}
		Connection.directory = directory;
		
		String downloadDirectoryName = directoryName + File.separator + "AppDownloads";
		File downloadDirectory = new File(downloadDirectoryName);
		if( downloadDirectory.exists() && ( !downloadDirectory.canRead() || !downloadDirectory.canWrite()) ) {
			errorLabel.setText("Cannot set \'" + downloadDirectoryName + "\' as downloads destination directory.");
			return;		
		} else if( !downloadDirectory.exists() && !downloadDirectory.mkdir() ) {
			errorLabel.setText("Cannot create \'" + downloadDirectoryName + "\' directory.");
			return;	
		}
		Connection.directoryDownloadsName = downloadDirectoryName;
		Connection.directoryDownloads = downloadDirectory;
		
		try {
			System.out.println("Im here");
			Connection.serverSocket = new Socket(serverIP,serverPort);
			System.out.println("And herte");
			InputStreamReader serverInput = new InputStreamReader(Connection.serverSocket.getInputStream());
			Connection.readBuff = new BufferedReader(serverInput);
			Connection.sendBytesBuff = Connection.serverSocket.getOutputStream();
			Connection.sendBuff = new PrintWriter(Connection.sendBytesBuff);
			if( !checkUsername() ) 
				return;

			Connection.clientRunning = true;
			Connection.fileRejected = false;
			Connection.downloadingFile = false;
			Connection.sendingFile = false;
			Connection.onlineListUpdate = false;
			Connection.newMessage = false;
			Connection.newFile = false;
			Connection.fileTransfer = new FileTransfer();
			Connection.serverListener = new Thread(new ServerListener());
			Connection.serverListener.start();
			Connection.onlineUsersList = new ArrayList<>();
			Connection.usersConv = new HashMap<>();
			Connection.directoryWatcher = new Thread(new DirectoryWatcher());
			Connection.directoryWatcher.start();
			
			switchScene(ev);
		} catch( IOException ex ) {
			errorLabel.setText("Cannot connect to server, check correctness of passed server ip and port.");
		}
	}

	/**
	 * Switch scene to fileview
	 * @param ev MouseEvent parameter
	 * @throws IOException if an I/O error occurs
	 */
	void switchScene(MouseEvent ev) throws IOException {
		BorderPane fileWindow = (BorderPane) FXMLLoader.load(getClass().getResource(".."+File.separator+"gui"+File.separator+"fileview.fxml"));
		fileWindow.getStylesheets().add(getClass().getResource(".."+File.separator+"gui"+File.separator+"fileview.css").toExternalForm());
		Scene newScene = new Scene(fileWindow);
		Stage primStage = (Stage) ((Node)ev.getSource()).getScene().getWindow();
		primStage.setScene(newScene);
	/* Set window position on desktop to center */
	    Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
	    primStage.setX((primScreenBounds.getWidth() - primStage.getWidth()) / 2);
	    primStage.setY((primScreenBounds.getHeight() - primStage.getHeight()) / 2);
	}
	
	/**
	 * Send username to server and wait to answer about validation of this username
	 * @return true if name is correct, otherwise false
	 */
	boolean checkUsername() {
		Connection.sendBuff(Connection.username);
		try {
			String isValid = Connection.readBuff();
			if( isValid.equals("<valid>") )
				return true;
			else if ( isValid.equals("<dirFail>") )
				errorLabel.setText("Creating of your directory on server failed.");
			else if( isValid.equals("<taken>") )
				errorLabel.setText("This username is already taken.");
			else if( isValid.equals("<invalid>") )
				errorLabel.setText("Invalid username. You cannot be named \'server\' and do not use whitespaces.");
			return false;
		} catch( IOException ex ) {
			ex.printStackTrace();
			return false;
		}
	}
}
