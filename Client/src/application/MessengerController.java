package application;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller of messengerview
 */
public class MessengerController {
	/**
	 * JavaFX list of online users
	 */
	@FXML private ListView<String> onlineList;
	/**
	 * Indicates current server status, e.g. Sending file... or Downloading file...
	 */
	@FXML private Label infoLabel;
	/**
	 * TextField for input messages
	 */
	@FXML private TextField messageTF;
	/**
	 * Box to display messages from users
	 */
	@FXML private VBox conversationBox;
	/**
	 * Displays current selected user from online list
	 */
	@FXML private Label usernameLabel;
	/**
	 * Displays username and directory name
	 */
	@FXML private Label metaInfoLabel;
	/**
	 * Indicates current selected user from online list
	 */
	String activeUserConv;
	/**
	 * Timeline used to refresh GUI
	 */
	Timeline timeline;
	/**
	 * Refresh/2 is the infoLabel cleaning label
	 */
	private int refresh;
	
	/**
	 * Initialize online list and start Timeline 
	 */
	@FXML
    public void initialize() {
		refresh = 0;
		metaInfoLabel.setText(Connection.username + "    " + Connection.directoryName);
		updateClientsList();
		timeline = new Timeline(	
			    new KeyFrame(Duration.seconds(0.2), e -> {
					if( !Connection.clientRunning )
						timeline.stop();
			    	if( ++refresh > 10) {	// refresh every 5s
			    		infoLabel.setText("");
			    		refresh=0;
			    	}
			    	
			    	if(Connection.onlineListUpdate) {
			    		Connection.onlineListUpdate = false;
			    		updateClientsList();
			    	}
			    	
			        if(Connection.sendingFile)
			        	infoLabel.setText("Sending file...");
			        else if(Connection.fileRejected)
			        	infoLabel.setText("File rejected");
			        else if(Connection.downloadingFile)
			        	infoLabel.setText("Downloading file...");
			        else infoLabel.setText("");
			        
			        if(Connection.newMessage) {
			        	Connection.newMessage = false;
			        	updateConversationBox();
			        }
			    })
			);
		timeline.setCycleCount(Timeline.INDEFINITE); //is it closed in onClose event?
		timeline.play();
	}
	
	/**
	 * Update conversation box
	 */
	void updateConversationBox() {
		if( activeUserConv == null )
			return;
    	ArrayList<String> userConv = Connection.usersConv.get(activeUserConv);
    	conversationBox.getChildren().clear();
    	for(String message : userConv) {
    		conversationBox.getChildren().add(new Label(message)); }
    }
	
	/**
	 * Update online list
	 */
	void updateClientsList() {
		onlineList.getItems().clear();
		for(String user : Connection.onlineUsersList) 
			onlineList.getItems().add(user);
		
		if( activeUserConv == null && !Connection.onlineUsersList.isEmpty() ) 
			activeUserConv = Connection.onlineUsersList.get(0);
		else if( activeUserConv != null && !Connection.onlineUsersList.contains(activeUserConv) ) {
			if( !Connection.onlineUsersList.isEmpty() )
				activeUserConv = Connection.onlineUsersList.get(0);
			else activeUserConv = null;
		}
		
		if( activeUserConv == null ) {
			usernameLabel.setText("No available users");
			conversationBox.getChildren().clear();
		}
		else
			usernameLabel.setText(activeUserConv);
	}
	
	/**
	 * Handle MouseEvent of switching icons (messengerview/fileview)
	 * @param ev MouseEvent parameter
	 * @throws IOException if an I/O error occurs
	 */
	@FXML
	void handleSceneSwitch(MouseEvent ev) throws IOException {
		timeline.stop();
		BorderPane fileWindow = (BorderPane) FXMLLoader.load(getClass().getResource(".."+File.separator+"gui"+File.separator+"fileview.fxml"));
		fileWindow.getStylesheets().add(getClass().getResource(".."+File.separator+"gui"+File.separator+"fileview.css").toExternalForm());
		Scene newScene = new Scene(fileWindow);
		Stage primStage = (Stage) ((Node)ev.getSource()).getScene().getWindow();
		primStage.setScene(newScene);
	}
	
	/**
	 * Handle MouseEvent of send file button 
	 * @param ev MouseEvent parameter
	 * @throws IOException IOException if an I/O error occurs
	 */
	@FXML
	void handleSendFile(MouseEvent ev) throws IOException {
	    if( activeUserConv == null ) {
	    	infoLabel.setText("No user selected");
	    	return;
	    }
	    System.out.println("activeUserConv " + activeUserConv);
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(".."+File.separator+"gui"+File.separator+"choosefileview.fxml"));
		System.out.println("fxmlLoader " + fxmlLoader);
		VBox chooseWindow = (VBox) fxmlLoader.load();
		System.out.println("chooseWindow " + chooseWindow);
		Stage window = new Stage();
		System.out.println("window " + window);
		window.initModality(Modality.APPLICATION_MODAL);
		Scene scene = new Scene(chooseWindow);
		System.out.println("scene " + scene);
		scene.getStylesheets().add(getClass().getResource(".."+File.separator+"gui"+File.separator+"choosefileview.css").toExternalForm());
		window.setScene(scene);
		window.show();
		/* Set window position on pulpit to center */
	    Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
	    window.setX((primScreenBounds.getWidth() - window.getWidth()) / 2);
	    window.setY((primScreenBounds.getHeight() - window.getHeight()) / 2);
	    ChooseFileController controller = fxmlLoader.getController();
	    controller.setReceiver(activeUserConv);
	}
	
	/**
	 * Handle MouseEvent of send message button 
	 * @param ev MouseEvent parameter
	 */
	@FXML
	void handleSendMessage(MouseEvent ev) {
		if( activeUserConv == null ) {
	    	infoLabel.setText("No user selected");
	    	return;
		}
		String content = messageTF.getText();
		if( content.isEmpty() )
			return;
		messageTF.clear();
		Connection.usersConv.get(activeUserConv).add("<" + Connection.username + "> " + content);
		conversationBox.getChildren().add(new Label("<" + Connection.username + "> " + content));
		Connection.sendBuff("<toUser> " + "<" + activeUserConv + "> " + content);
		
		System.out.println(Connection.username + " -> " + "<" + activeUserConv + "> " + content);
	}
	
	/**
	 * Handle online list MouseEvent
	 * @param ev MouseEvent parameter
	 */
	@FXML
	void handleSelection(MouseEvent ev) {
		activeUserConv = onlineList.getSelectionModel().selectedItemProperty().getValue();
		usernameLabel.setText(activeUserConv);
		updateConversationBox();
	}
}
