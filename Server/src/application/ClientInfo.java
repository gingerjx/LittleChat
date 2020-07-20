package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

/**
 * Contains information about connection with specific client
 */
public class ClientInfo {
	/**
	 * Clients socket
	 */
	Socket socket;
	/**
	 * Path to clients directory 
	 */
	String directoryName;	
	/**
	 * Clients directory
	 */
	File directory;
	/**
	 * Clients username
	 */
	String username;
	/**
	 * Downloading file is in progress
	 */
	boolean downloadingFile;		
	/**
	 * Sending file is in progress
	 */
	boolean sendingFile;			
	/**
	 * Buffer to sending strings to server
	 */
	PrintWriter sendBuff;			
	/**
	 * Buffer to reading strings to server
	 */
	BufferedReader readBuff;		
	
	/**
	 * @param _socket socket connected to server
	 * @param _username username of client
	 */
	ClientInfo(Socket _socket, String _username){
		socket = _socket;
		try { socket.setSoTimeout(ServerInfo.SOCKETS_TIMEOUT); } 
		catch (SocketException e) {e.printStackTrace();}
		username = _username;
		directoryName = ServerInfo.DIRS_PATH + File.separator + username;
		downloadingFile = false;
		sendingFile = false;
		directory = new File(directoryName);
		
		try {
			sendBuff = new PrintWriter(socket.getOutputStream());
			InputStreamReader isr = new InputStreamReader(socket.getInputStream());
			readBuff = new BufferedReader(isr);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Send passed message to server as String using PrintWriter
	 * @param message content of message
	 */
	public void sendBuff(String message) {
		sendBuff.println(message);
		sendBuff.flush();
	}
	/**
	 * Function to read messages from server, using BufferedReader
	 * @return  message from server as String 
	 * @throws IOException throws if reading from server failed
	 */
	public String readBuff() throws IOException {
		String message = readBuff.readLine();
		return message;
	}
	/**
	 * Deletes 'filename' form local directory, if it's possible
	 * @param filename name of file to delete
	 * @return true if delete finished succesful, otherwise false
	 */
	public boolean deleteFile(String filename) {
		File file = new File(directoryName+File.separator+filename);
		return file.delete();
	}
	/**
	 * Close connection between this specific client and server, and clean all information
	 * about client in {@link application.ServerInfo} 
	 */
	public void close() {
		try { socket.close(); } catch ( IOException e ) { e.printStackTrace(); } 
		ServerInfo.clients.remove(username);
		ServerInfo.sendOnlineList();
		ServerInfo.onlineListUpdate = true;
	}
	/**
	 * Set timeout of clients socket
	 * @param time amount of timeout in miliseconds
	 */
	public void setTimeout(int time) {
		try { socket.setSoTimeout(time); } 
		catch (SocketException e) {e.printStackTrace();}
	}
	/**
	 * Set 'downloadingFile' status of client
	 * @param bool true if client downloading some file, otherwise false
	 */
	public void setDownloadingFile(boolean bool) {
		downloadingFile = bool;
	}
	/**
	 * Set 'sendingFile' status of client
	 * @param bool true if client sending some file, otherwise false
	 */
	public void setSendingFile(boolean bool) {
		sendingFile = bool;
	}
	
	/**
	 * Return name of local directory entered in login window
	 * @return name of local directory entered in login window
	 */
	public String getDirectoryName() {
		return directoryName;
	}
	/**
	 * Return name of username entered in login window
	 * @return name of username entered in login window
	 */
	public String getUsername() {
		return username;
	}
	/**
	 * Return socket connected with server
	 * @return socket connected with server
	 */
	public Socket getSocket() {
		return socket;
	}
	/**
	 * Says about file transfer status of client
	 * @return true if client downloading some file, otherwise false
	 */
	public boolean isDownloading() {
		return downloadingFile;
	}
	/**
	 * Says about file transfer status of client
	 * @return true if client sending some file, otherwise false
	 */
	public boolean isSending(){
		return sendingFile;
	}
}
