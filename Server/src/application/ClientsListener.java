package application;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Listener for all available clients. Listener uses socket timeout to be able to listening all of clients at one thread.
 * Possible message from clients: <br>
 * 	client sending file    | format: sendFile filename filesize receiver <br>
 * 	client received file   | format: received filepath <br>
 * 	client deleting file   | format: delete filepath <br>
 * 	client sending message | fromat: toUser receiver message
 */
public class ClientsListener implements Runnable{
	/**
	 * Listening clients till server is running
	 */
	@Override
	public void run() {		
		Pattern downloadingFilePatter = Pattern.compile("<sendFile> [^\\s]* \\w+ \\w+");	//format: <sendFile> filename filesize receiver
		Pattern sendSuccesfullyPattern = Pattern.compile("<received> [^\\s]*"); 			//format: <received> filepath
		Pattern deleteFilePattern = Pattern.compile("<delete> [^\\s]*"); 					//format: <delete> filepath
		Pattern sendMessageToClient = Pattern.compile("<toUser> <\\w+> .*");				//format: <toUser> <receiver> message
		Matcher matcher;
		
		while(ServerInfo.serverRunning) {
			for(Map.Entry<String, ClientInfo> entry : ServerInfo.clients.entrySet() ) {
				try {
					ClientInfo client = entry.getValue();
					String username = client.getUsername();
					if( client.isDownloading() ) 							//waiting to the end of receiving file
						continue;
					
					String message = client.readBuff();					
					
					if( message == null || message.equals("exit") ) {		//client disconnected
						System.out.println(username + " disconnected");
						client.close();
						continue;
					} 
					matcher = downloadingFilePatter.matcher(message);
					if( matcher.matches() ) {								//client sending file
						ServerInfo.fileTransfer.receiveFile(client,message);
						continue;
					}
					if( message.equals("<acceptFile>") ) {					//client accept sending file
						client.setSendingFile(true);
						continue;
					}
					matcher = sendSuccesfullyPattern.matcher(message);
					if( matcher.matches() ) {								//client received file
						System.out.println(username + " " + message + " file");
						continue;
					}
					matcher = deleteFilePattern.matcher(message);
					if( matcher.matches() ) {								//client deleted file
						String[] splittedCommand = message.split("\\s");
						if( client.deleteFile(splittedCommand[1]) ) {
							System.out.println(username + " " + message + " file");
							ServerInfo.deletingFile = true;
						}
						else System.out.println("Deleting " + splittedCommand[1] + " failed");
						continue;
					}
					matcher = sendMessageToClient.matcher(message);
					if( matcher.matches() ) {								//client send message to another client
						String[] splittedCommand = message.split("\\s");
						String messageToPass = "";
						for(int i=2; i<splittedCommand.length; ++i)
							messageToPass += splittedCommand[i] + " ";
						String receiverName = splittedCommand[1].substring(1,splittedCommand[1].length()-1);
						ClientInfo sendTo = ServerInfo.clients.get(receiverName);
						messageToPass = "<fromUser> <" + client.getUsername() + "> " + messageToPass;
						sendTo.sendBuff(messageToPass);
						continue;
					}
					
					System.out.println(username + ": |" + message + "|");
					
				} catch( IOException e ) { } // client just timed out
			}
		}
	}
}
