package application;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

/**
 * Controller of sending file client
 */
public class ChooseFileController {
	/**
	 * JavaFX files tree
	 */
	@FXML private TreeView<String> filesTree;
	/**
	 * Displays information about selection, e.g. Selection is not file
	 */
	@FXML private Label infoLabel;
	/**
	 * Root node of files tree
	 */
	private TreeItem<String> rootNode;
	/**
	 * Refresh/2 period in second to clear infoLabel
	 */
	private String receiver;
	
	/**
	 * Initializes files tree
	 */
	@FXML
    public void initialize() {
		rootNode = new TreeItem<>();
		rootNode.setExpanded(true);
		filesTree.setRoot(rootNode);
		filesTree.setShowRoot(false);
		updateFileTree();
		filesTree.getSelectionModel().selectedItemProperty().addListener(
				(obv, oldVal, newVal) -> infoLabel.setText("Selected: " + newVal.getValue()) );
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
	 * Set receiver choosen file
	 * @param receiver receiver username
	 */
	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}
	
	/**
	 * Handle sending file
	 */
	@FXML
	void handleSendFile() {
		TreeItem<String> node = filesTree.getSelectionModel().getSelectedItem();
		if( node == null )
			return;
		String relativePath = getRelativePath(node);
		if( !(new File(Connection.directoryName + File.separator + relativePath)).isFile() ) {
			infoLabel.setText(node.getValue() + " is not a file. Sending rejected.");
			return;
		}
		Connection.fileTransfer.sendFile(relativePath + " " + receiver);
		System.out.println("Send " + relativePath + " to " + receiver );
		Stage stage = (Stage) filesTree.getScene().getWindow();
		stage.close();
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