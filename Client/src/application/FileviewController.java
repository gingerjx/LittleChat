package application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller of fileview
 */
public class FileviewController {
	/**
	 * JavaFX files tree
	 */
	@FXML private TreeView<String> filesTree;
	/**
	 * Box to display information about file
	 */
	@FXML private VBox infoBox;
	/**
	 * Indicates current server status, e.g. Sending file... or Downloading file... 
	 */
	@FXML private Label infoLabel;
	/**
	 * Displays username and directory name
	 */
	@FXML private Label metaInfoLabel;
	/**
	 * Displays selected file from files tree
	 */
	@FXML private Label selectedFileLabel;
	/**
	 * Root node of files tree
	 */
	TreeItem<String> rootNode;
	/**
	 * Timeline used to refresh GUI
	 */
	Timeline timeline;
	/**
	 * Refresh/2 is the infoLabel cleaning label
	 */
	private int refresh;
	
	/**
	 * Initialize files tree and start Timeline 
	 */
	@FXML
    public void initialize() {
		refresh = 0;
		metaInfoLabel.setText(Connection.username + "    " + Connection.directoryName);
		rootNode = new TreeItem<>();
		rootNode.setExpanded(true);
		filesTree.setRoot(rootNode);
		filesTree.setShowRoot(false);
		updateFileTree();
		timeline = new Timeline(
			    new KeyFrame(Duration.seconds(0.5), e -> {
					if( !Connection.clientRunning )
						timeline.stop();
			    	if( ++refresh > 10) {	// refresh every 5s
			    		infoLabel.setText("");
			    		refresh=0;
			    	}
			    	
			        if(Connection.sendingFile)
			        	infoLabel.setText("Sending file...");
			        else if(Connection.fileRejected)
			        	infoLabel.setText("File rejected");
			        else if(Connection.downloadingFile)
			        	infoLabel.setText("Downloading file...");
			        
			        if(Connection.newFile) {
			        	Connection.newFile = false;
			        	infoLabel.setText("New file downloaded");
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
		File rootDir = new File(Connection.directoryName);
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
	 * Handle MouseEvent of delete button
	 * @param ev MouseEvent parameter
	 */
	@FXML
	void handleDeletion(MouseEvent ev) {
		TreeItem<String> node = filesTree.getSelectionModel().getSelectedItem();
		if( node == null ) {
			infoLabel.setText("No file selected");
			return;
		}
		String fullPath = Connection.directoryName + getRelativePath(node);
		File toDelete = new File(fullPath);
		if( toDelete.delete() ){
			Connection.sendBuff("<delete> " + getRelativePath(node));
			infoLabel.setText("Succesful deleted");
			infoBox.getChildren().clear();
			updateFileTree();
		} else
			infoLabel.setText("Deletion failed");
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
	
	/**
	 * Handle files tree MouseEvent
	 * @param ev MouseEvent parameter
	 * @throws IOException if an I/O error occurs
	 */
	@FXML
	void handleSelection(MouseEvent ev) throws IOException {
		TreeItem<String> node = filesTree.getSelectionModel().getSelectedItem();
		if( node == null )
			return;
		selectedFileLabel.setText(node.getValue());
		infoBox.getChildren().clear();
		
		String relativePath = getRelativePath(node);
		String fullPath = Connection.directoryName + relativePath;
		
		File file = new File(fullPath);
		infoBox.getChildren().addAll(
				new Label("Filename: " + node.getValue()),
				new Label("Relative path: " + relativePath),
				new Label("Full path: " + fullPath),
				new Label("File: " + file.isFile()),
				new Label("Executable: " + file.canExecute()),
				new Label("Readable: " + file.canRead()),
				new Label("Writeable: " + file.canWrite()),
				new Label("Size: " + file.length() + "B"),
				new Label("Last modified: " + (Files.getLastModifiedTime(Paths.get(fullPath))).toString().substring(0,20))
				);
	}
	
	/**
	 * Handle MouseEvent of save button
	 * @param ev MouseEvent parameter
	 */
	@FXML
	void handleSaving(MouseEvent ev) {
		TreeItem<String> node = filesTree.getSelectionModel().getSelectedItem();
		if( node == null ) {
			infoLabel.setText("No file selected");
			return;
		}
		String relativePath = getRelativePath(node);
		Connection.fileTransfer.sendFile(relativePath + " server"); 
	}
	
	/**
	 * Handle MouseEvent of switching icons (messengerview/fileview)
	 * @param ev MouseEvent parameter
	 * @throws IOException if an I/O error occurs
	 */
	@FXML
	void handleSceneSwitch(MouseEvent ev) throws IOException {
		timeline.stop();
		BorderPane messengerWindow = (BorderPane) FXMLLoader.load(getClass().getResource(".."+File.separator+"gui"+File.separator+"messengerview.fxml"));
		messengerWindow.getStylesheets().add(getClass().getResource(".."+File.separator+"gui"+File.separator+"messengerview.css").toExternalForm());
		Scene newScene = new Scene(messengerWindow);
		Stage primStage = (Stage) ((Node)ev.getSource()).getScene().getWindow();
		primStage.setScene(newScene);
	}
}


















