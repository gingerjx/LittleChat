package application;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listener for server.
 * Possible message from server: <br>
 * 	server sending file             | format: sendFile filename filesize <br>
 * 	server sending file list        | format: online list.. <br>
 * 	server sending client message   | format: fromUser senderName message<br>
 */
class ServerListener implements Runnable{
	final long SECOND = 1000;
	/**
	 * Listening server till client is running
	 */	
	@Override
	public void run() {
		Pattern downloadFilePattern = 	Pattern.compile("<sendFile> [^\\s]* \\w+");	//format: <sendFile> <filename> <fileSize>
		Pattern onlineListPattern = 	Pattern.compile("<online> .*");				//format: <online> list...
		Pattern getUserMessagePattern = Pattern.compile("<fromUser> <\\w+> .*");	//format: <fromUser> <senderName> message
		Matcher matcher;
		
		try {
			while(Connection.clientRunning) {

				if ( Connection.downloadingFile ) {					//waiting to the end of downloading file
					try { Thread.sleep((SECOND)); } 
					catch(Exception e) { e.printStackTrace(); }
					continue;
				};
				
				String message = Connection.readBuff();
				
				if( message==null || message.equals("exit") ) {		//server closing client
					Connection.closeClient();
					continue;
				}
				if( message.equals("acceptFile") ) {				//server accept file
					Connection.sendingFile = true;
					continue;
				}
				if( message.equals("rejectFile") ) {				//server reject file
					Connection.fileRejected = true;
					continue;
				}
				matcher = onlineListPattern.matcher(message);		//server send users list
				if( matcher.matches() ) {
					updateClientsList(message);
					Connection.onlineListUpdate = true;
					continue;
				}
				matcher = downloadFilePattern.matcher(message);		//server sending some file
				if( matcher.matches() ) {
					Connection.downloadingFile = true;
					Connection.fileTransfer.receiveFile(message);
					continue;
				}
				matcher = getUserMessagePattern.matcher(message);	//server passed message from another user
				if( matcher.matches() ) {
					handleMessageFromUser(message);
					continue;
				}
				System.out.println("Smth: " + message);
			}
		} catch( IOException e ) {
			System.out.println("ServerListener: connection closed");
		} finally {
			Connection.clientRunning = false;
		}
		System.out.println("ServerListener: closed");
	}
	void updateClientsList(String message) {
		String[] splittedCommand = message.split("\\s");
		Connection.onlineUsersList.clear();
		for(int i=1; i<splittedCommand.length; ++i) {
			if( !splittedCommand[i].equals(Connection.username) ) {
				String user = splittedCommand[i];
				Connection.onlineUsersList.add(user);
				if( !Connection.usersConv.containsKey(user) )
					Connection.usersConv.put(user, new ArrayList<>());
			}
		}
	}
	void handleMessageFromUser(String message) {
		String[] splittedCommand = message.split("\\s");
		String fromUser = splittedCommand[1].substring(1,splittedCommand[1].length()-1);
		ArrayList<String> userConv = Connection.usersConv.get(fromUser);
		message = "";
		for(int i=1; i<splittedCommand.length; ++i)
			message += splittedCommand[i] + " ";
		userConv.add(message);
		Connection.newMessage = true;
	}
}
