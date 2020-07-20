package application;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains all static useful information about server. Class fields: <br>
 */
public class ServerInfo {
	/**
	 * (default {@value #PORT}) indicate servers port
	 */
	static final int PORT = 5000;
	static final int SECOND = 1000;
	/**
	 * Path to clients directories
	 */
	static String DIRS_PATH = ".."+File.separator+"directories";
	/**
	 * (default {@value #SOCKETS_TIMEOUT}) Indicate time used in setTimeout as parameter
	 */
	static final int SOCKETS_TIMEOUT = SECOND/2;
	/**
	 * Server socket
	 */
	static ServerSocket serverSocket;
	/**
	 * IP of our server
	 */
	static String serverIP;
	/**
	 * Indicates if server is still running
	 */
	static boolean serverRunning;
	/**
	 * Indicates if downloading file is in progress
	 */
	static boolean downloadingFile;	
	/**
	 * Indicates if sending file is in progress
	 */
	static boolean sendingFile;		
	/**
	 * Indicates if server rejected to download file
	 */
	static boolean fileRejected;	
	/**
	 * Indicates if server send new online users list
	 */
	static boolean onlineListUpdate;	
	/**
	 * Indicates if client deleted file
	 */
	static boolean deletingFile;
	/**
	 * Clients map(key: username(String) value: info about clients(ClientInfo)
	 */
	static ConcurrentHashMap< String,ClientInfo > clients;		
	/**
	 * Filetransfer instance for sending and receiving files
	 */
	static FileTransfer fileTransfer;	
	/**
	 * Additional thread to accept new clients
	 */
	static Thread joinListener;	
	/**
	 * Additional thread to listen to clients messages 
	 */
	static Thread clientsListener;									
	
	/**
	 * Close server and send signal to close to every clients
	 */
	static void closeServer() {
		ServerInfo.serverRunning = false;
		if( ServerInfo.joinListener != null ) ServerInfo.joinListener.interrupt(); 
		try { ServerInfo.serverSocket.close(); } catch ( IOException e ) { e.printStackTrace(); } 
		closeClients();
		ServerInfo.fileTransfer.trasferThreadPool.shutdown();
		System.out.println("Server closed");
	}
	/**
	 * Send signal to close to every clients
	 */
	static void closeClients() {
		for(Map.Entry<String, ClientInfo> client: ServerInfo.clients.entrySet()) {
			ClientInfo clientDes = client.getValue();
			System.out.println("Closing " + clientDes.getUsername() );
			clientDes.sendBuff("exit");		
			clientDes.close();
		}
	}
	/**
	 * Return ArrayList of online users
	 * @return ArrayList of String of online users
	 */
	static ArrayList<String> onlineList() {
		ArrayList<String> list = new ArrayList<>();
		for(Map.Entry<String, ClientInfo> client: ServerInfo.clients.entrySet()) {
			list.add(client.getKey());
		}
		return list;
	}
	/**
	 * Send online list to every client
	 */
	static void sendOnlineList() {
		StringBuilder onlineList = new StringBuilder();
		for(String client: onlineList()) {
			onlineList.append(client+" ");
		}
		for(Map.Entry<String, ClientInfo> client: ServerInfo.clients.entrySet()) {
			client.getValue().sendBuff("<online> " + onlineList.toString());
		}
	}
}
