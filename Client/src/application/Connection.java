package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import javafx.application.Platform;

/**
 * Contains information about connection with server
 */
class Connection {
	static String serverIP; 
	/**
	 * Indicates servers port
	 */
	static int serverPort;
	/**
	 * Server socket
	 */
	static Socket serverSocket;
	/**
	 * Local directory name
	 */
	static String directoryName;				
	/**
	 * Destination name of directory for downloaded files
	 */
	static String directoryDownloadsName;		
	/**
	 * Local directory
	 */
	static File directory;						
	/**
	 * Destination directory for downloaded files
	 */
	static File directoryDownloads;				
	/**
	 * Clients username
	 */
	static String username;
	/**
	 * Indicates if client is running
	 */
	static boolean clientRunning;
	/**
	 * Downloading file is in progress
	 */
	static boolean downloadingFile;				
	/**
	 * Sending file is in progress
	 */
	static boolean sendingFile;					
	/**
	 * Server rejected to download file
	 */
	static boolean fileRejected;				
	/**
	 * Indicates that server send new online users list
	 */
	static boolean onlineListUpdate;			
	/**
	 * Indicates that there is new message
	 */
	static boolean newMessage;					
	/**
	 * Indicates that there is new file
	 */
	static boolean newFile;						
	/**
	 * Buffer to sending bytes to server
	 */
	static OutputStream sendBytesBuff;			
	/**
	 * Buffer to sending strings to server
	 */
	static PrintWriter sendBuff;				
	/**
	 * Buffer to reading strings from server
	 */
	static BufferedReader readBuff;				
	/**
	 * Class for sending and receiving files
	 */
	static FileTransfer fileTransfer;   		
	/**
	 * Additional thread for listening to server messages
	 */
	static Thread serverListener;				
	/**
	 * List of available users
	 */
	static ArrayList<String> onlineUsersList;   
	/**
	 * Store content(value) of conversation with user(key) 
	 */
	static HashMap<String,ArrayList<String>> usersConv;    
	/**
	 * Watcher for changes in local directory
	 */
	static Thread directoryWatcher;				
	
	/**
	 * Sending message to server as String. Use OutputStream.
	 * @param message message to send
	 */
	static void sendBuff(String message) {
		sendBuff.println(message);
		sendBuff.flush();
	}
	/**
	 * Sending message to server as bytes. Use PrintWriter
	 * @param bytes array of bytes to send
	 * @throws IOException if I/O exceptions occurs
	 */
	static void sendBytesBuff(byte[] bytes) throws IOException {
		sendBytesBuff.write(bytes,0,bytes.length);
		sendBytesBuff.flush();
	}
	/**
	 * Read String message from server
	 * @return return message from server
	 * @throws IOException if I/O exceptions occurs
	 */
	static String readBuff() throws IOException {
		return readBuff.readLine();
	}
	/**
	 * Close client and shutdown all threads
	 */
	static void closeClient() {
		if( serverListener != null ) {
			serverListener.interrupt();
		}
		if( directoryWatcher != null ) {
			directoryWatcher.interrupt();
		}
		if( Connection.sendBuff != null && !Connection.serverSocket.isClosed() )
			Connection.sendBuff("exit");
		
		Connection.clientRunning = false;
		try {if(Connection.serverSocket!=null) Connection.serverSocket.close();}
		catch (IOException e) {e.printStackTrace();}
		FileTransfer.trasferThreadPool.shutdown();
		Platform.exit();
	}
}