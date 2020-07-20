package application;
import java.io.*;
import java.net.*;

/**
 * Class used to join clients to server
 * Read username passed from socket output stream, validate that and send feedback to client about validation of his username.
 * Validation includes: <br>
 *  server contains clients directory or is able to create it <br>
 *  passed username is not already online <br>
 *  username don't contain whitespaces <br>
 */
public class JoinListener implements Runnable{
	/**
	 * Waits to accept client. If username is correct then add to server.
	 */
	@Override
	public void run() {	
		while( ServerInfo.serverRunning ) {
			try {
				Socket socket = ServerInfo.serverSocket.accept();
				String username = checkUsername(socket);
				if( username == null ) {
					socket.close();
					continue;
				}
				ServerInfo.clients.put(username,new ClientInfo(socket,username));
				ServerInfo.sendOnlineList();
				ServerInfo.onlineListUpdate = true;
				System.out.println(username + " joined");
			} catch( IOException e ) {}
		} 
		System.out.println("JoinListener: closed");
	}
	/**
	 * Validate username passed by user from 'socket'
	 * @param socket 'socket' used to get output stream of client
	 * @return username if username is correct, otherwise null
	 */
	private String checkUsername(Socket socket) {
		String username = null;
		try {
			InputStreamReader clientInput = new InputStreamReader(socket.getInputStream());
			BufferedReader read = new BufferedReader(clientInput);
			username = read.readLine();
		} catch( IOException e ) {
			e.printStackTrace();
			return null;		
		}
		System.out.println("User directory " + ServerInfo.DIRS_PATH + File.separator + username);
		File directory = new File(ServerInfo.DIRS_PATH + File.separator + username);
		
		try {
			PrintWriter send = new PrintWriter(socket.getOutputStream());
			
			if( ServerInfo.onlineList().contains(username) ) {	
				send.println("<taken>");				//send message to client that his username is already taken
				send.flush();	
				System.out.println(username + " tried to join. Rejection - this is user is already online");
				return null;
			} else if( username.contains(" ") ){
				send.println("<invalid>");				//send message to client that his username is invalid
				send.flush();	
				System.out.println(username + " tried to join. Rejection - invalid name");
				return null;
			} else if( !directory.exists() && !directory.mkdir() ){		
				send.println("<dirFail>");				//send message to client that his directory cannot be created
				send.flush();
				System.out.println(username + " tried to join. Rejection - Creating new directory for " + username + " failed");	
				return null;
			} else {		
				send.println("<valid>");					//send message to client that his username is valid
				send.flush();
				return username;
			}
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
