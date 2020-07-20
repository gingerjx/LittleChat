package application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for servers GUI
 */
public class ServerController {
	/**
	 * JavaFX list of online users
	 */
	@FXML private ListView<String> onlineList;
	/**
	 * JavaFX tree of files of selected user directory
	 */
	@FXML private TreeView<String> filesTree;
	/**
	 * Indicates current server status, e.g. Sending file... or Downloading file...
	 */
	@FXML private Label infoLabel; 
	/**
	 * Displays which user is selected 
	 */
	@FXML private Label selectedLabel;
	/**
	 * Box to display information about file
	 */
	@FXML private VBox infoBox;
	/**
	 * Display servers IP
	 */
	@FXML private Label metaInfoLabel;
	/**
	 * Root node of files tree
	 */
	private TreeItem<String> rootNode;
	/**
	 * Timeline used to refresh GUI
	 */
	private Timeline timeline;
	/**
	 * Indicates which user is selected 
	 */
	private String selectedUser;
	/**
	 * Refresh/2 is the infoLabel cleaning label
	 */
	private int refresh;
	
	/**
	 * Initialize files tree and start Timeline 
	 */
	@FXML
	void initialize() {
		refresh = 0;
		metaInfoLabel.setText(ServerInfo.serverIP);
		rootNode = new TreeItem<>();
		rootNode.setExpanded(true);
		filesTree.setRoot(rootNode);
		filesTree.setShowRoot(false);
		updateFileTree();
		timeline = new Timeline(					
			    new KeyFrame(Duration.seconds(0.5), e -> {
			    	if( !ServerInfo.serverRunning )
			    		timeline.stop();
			    	if( ++refresh > 10) {	// refresh every 5s
			    		infoLabel.setText("");
			    		refresh=0;
			    	}
			    	
			    	if(ServerInfo.onlineListUpdate) {
			    		ServerInfo.onlineListUpdate = false;
			    		updateClientsList();
			    	}
			    	
			        if(ServerInfo.sendingFile)
			        	infoLabel.setText("Sending file...");
			        else if(ServerInfo.fileRejected) {
			        	infoLabel.setText("File rejected");
			        	ServerInfo.fileRejected = false;
			        } 
			        else if(ServerInfo.downloadingFile)
			        	infoLabel.setText("Downloading file...");
			        else if(ServerInfo.deletingFile) {
			        	infoLabel.setText("Deleting file...");
			        	ServerInfo.deletingFile = false;
			        	updateFileTree();
			        }
			    })
			);
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.play();
	}
	/**
	 * Updates files tree
	 */
	void updateFileTree() {	
		rootNode.getChildren().clear();		
		if( selectedUser == null )
			return;
		File rootDir = new File(ServerInfo.DIRS_PATH +File.separator+ selectedUser);
		File[] fileList = rootDir.listFiles();
		
		for(int i=0; i<fileList.length; ++i) {	
			TreeItem<String> newNode = null;
			if( fileList[i].isDirectory() )
				newNode = createNode(fileList[i]);
			else
				newNode = new TreeItem<String>(fileList[i].getName());
			rootNode.getChildren().add(newNode);
		}
	}
	/**
	 * Create node from files and directories of passed directory in argument
	 * @param directory files and directories from passed directory are used to create node
	 * @return node named like passed directory
	 */
	TreeItem<String> createNode(File directory){	
		TreeItem<String> node = new TreeItem<String>(directory.getName());
		File[] fileList = directory.listFiles();

		for(int i=0; i<fileList.length; ++i) {	
			TreeItem<String> newNode = null;
			if( fileList[i].isDirectory() )
				newNode = createNode(fileList[i]);
			else
				newNode = new TreeItem<String>(fileList[i].getName());
			node.getChildren().add(newNode);
		}
		
		return node;
	}
	/**
	 * Updates clients list
	 */
	void updateClientsList() {
		onlineList.getItems().clear();
		ArrayList<String> onList = ServerInfo.onlineList();
		for(String user : onList) 
			onlineList.getItems().add(user);
		
		if( selectedUser == null && !onList.isEmpty() ) 
			selectedUser = onList.get(0);
		else if( selectedUser != null && !onList.contains(selectedUser) ) {
			if( !onList.isEmpty() )
				selectedUser = onList.get(0);
			else selectedUser = null;
		}
		
		if( selectedUser == null ) {
			selectedLabel.setText("Noone selected");
			infoBox.getChildren().clear();
		}
		else
			selectedLabel.setText(selectedUser);
		
		updateFileTree();
	}
	
	/**
	 * Handle online list MouseEvent
	 * @param ev MouseEvent parameter
	 */
	@FXML
	void handleListSelection(MouseEvent ev) {
		selectedUser = onlineList.getSelectionModel().selectedItemProperty().getValue();
		selectedLabel.setText(selectedUser);
		//updateConversationBox();
		updateFileTree();
	}
	
	/**
	 * Handle files tree MouseEvent
	 * @param ev MouseEvent parameter
	 * @throws IOException if an I/O error occurs
	 */
	@FXML
	void handleTreeSelection(MouseEvent ev) throws IOException {
		TreeItem<String> node = filesTree.getSelectionModel().getSelectedItem();
		if( node == null )
			return;
		selectedLabel.setText(selectedUser + ": " + node.getValue());
		infoBox.getChildren().clear();
		
		String relativePath = getRelativePath(node);
		String fullPath = ServerInfo.DIRS_PATH +File.separator+ selectedUser + relativePath;
		
		File file = new File(fullPath);
		infoBox.getChildren().addAll(
				new Label("Filename: " + node.getValue()),
				new Label("Relative path: " + selectedUser + relativePath),
				new Label("Full path: " + file.getAbsolutePath()),
				new Label("File: " + file.isFile()),
				new Label("Executable: " + file.canExecute()),
				new Label("Readable: " + file.canRead()),
				new Label("Writeable: " + file.canWrite()),
				new Label("Size: " + file.length() + "B"),
				new Label("Last modified: " + (Files.getLastModifiedTime(Paths.get(fullPath))).toString().substring(0,20))
				);
	}
	/**
	 * Returns relative path of passed file in node as String
	 * @param node node from file tree 
	 * @return relative path of passed file in node
	 */
	String getRelativePath(TreeItem<String> node) {
		String relativePath = new String("");
		while(node != rootNode){
			relativePath = File.separator + node.getValue() + relativePath;
			node = node.getParent();
		}
		return relativePath;
	}
}
