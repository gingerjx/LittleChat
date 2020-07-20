package application;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class serving tools to sending and receiving files from client. Every downloading or sending file is realized in thread pool.
 */
class FileTransfer {
	/**
	 * ExecutorService instance use to sending and downloading files in thread pool
	 */
	ExecutorService trasferThreadPool = Executors.newCachedThreadPool();
	/**
	 * Firstly {@link application.ClientsListener} received message from client that he want to send some file and then
	 * listener invoke this function to do rest of job.
	 * @param senderClient {@link application.ClientInfo} instance, which is sending client
	 * @param command message from sender client contains information such as filename, fileSize and receiver client (or server)
	 */
	void receiveFile(ClientInfo senderClient, String command) {
		senderClient.setDownloadingFile(true);
		ServerInfo.downloadingFile = true;
		String[] splittedCommand = command.split("\\s");
		String filename = splittedCommand[1];
		String fileSize = splittedCommand[2];
		String receiverName = splittedCommand[3];
		trasferThreadPool.execute(new ReceiveFileTask(senderClient,filename,fileSize,receiverName));
	}
	/**
	 * Function used to sending file to clients
	 * @param command message from server to client contains information such as filename receiver client
	 */
	void sendFile(String command) {
		String[] splittedCommand = command.split("\\s");
		String filename = splittedCommand[1];
		String receiverName = splittedCommand[2];
		ClientInfo receiverClient = ServerInfo.clients.get(receiverName);
		trasferThreadPool.execute(new SendFileTask(filename,receiverClient));
	}
	
	class ReceiveFileTask implements Runnable{
		String filename;
		int fileSize;
		String receiverName;
		ClientInfo receiverClient;
		ClientInfo senderClient;
		ReceiveFileTask(ClientInfo _senderClient, String _filename, String _fileSize, String _receiverName) {
			filename = _filename;
			fileSize = Integer.parseInt(_fileSize);
			senderClient = _senderClient;
			receiverName = _receiverName;
			if( !receiverName.equals("server") )
				receiverClient = ServerInfo.clients.get(receiverName);
		}
		@Override
		public void run() {
/* Establishment */				
			if( receiverName.equals("server") || ServerInfo.clients.containsKey(receiverName) ) {
				senderClient.sendBuff("acceptFile");						// sending acknowledgement to client
			} else {
				System.out.println("Receiver " + receiverName + " is not online");
				senderClient.sendBuff("rejectFile");						// sending rejection to client
				ServerInfo.fileRejected = true;
				return;
			}

/* Receiving file */	
			String senderPathToFile = senderClient.getDirectoryName() + File.separator + filename;
			System.out.println("Download to: " + senderClient.getDirectoryName() + File.separator + filename);
			try ( FileOutputStream fos = new FileOutputStream(senderPathToFile); ){
				System.out.println("Downloading " + filename + " from " + senderClient.getUsername());
				Socket socket = senderClient.getSocket();
				senderClient.setTimeout(0);
				InputStream is = socket.getInputStream();
			    int allBytesRead=0;
			    int bytesRead;
			    byte[] fileContent = new byte[fileSize];
			    
			    while(allBytesRead < fileSize) {
			    	bytesRead = is.read(fileContent,0,fileSize-allBytesRead);
			    	if( bytesRead == -1 )
			    		break;
			    	allBytesRead += bytesRead;
			    }
			    System.out.println(fileSize + " Received: " + fileContent.toString());
			    fos.write(fileContent,0,fileContent.length);
			    senderClient.setTimeout(ServerInfo.SOCKETS_TIMEOUT);
			    
			    System.out.println("File " + filename + " saved at " + senderClient.getUsername());
/* Sending file to receiver */
			    //...... TODO
			    if( receiverName.equals("server") )
			    	return;
			    trasferThreadPool.execute(new SendFileTask(senderClient.getUsername() + File.separator + filename,receiverClient));
			    
			} catch( IOException e ) {
				e.printStackTrace();
				senderClient.close();
			} finally {
				ServerInfo.downloadingFile = false;
				senderClient.setDownloadingFile(false);
			}
			
		}
		
	}
	class SendFileTask implements Runnable{
		String filename;
		ClientInfo receiverClient;
		SendFileTask(String _filename, ClientInfo _receiverClient){
			filename = _filename;
			receiverClient = _receiverClient;
		}
		@Override
		public void run() {
			ServerInfo.sendingFile = true;
			String filePath = ServerInfo.DIRS_PATH +File.separator+ filename;
			File file = new File(filePath);
			if( !file.exists() ) {
				System.out.println("File " + filePath + " doesn't exist");
				return;
			}
			
			filename = filename.substring(filename.indexOf(File.separator)+1);
			String receiverName = receiverClient.getUsername();
			String receiverPathToFile = ServerInfo.DIRS_PATH + File.separator + receiverName + File.separator + filename;
			int fileSize = (int) file.length();
			byte[] fileContent = new byte[fileSize];	
			Socket socket = receiverClient.getSocket();
			FileOutputStream fos = null;
			
			try ( FileInputStream fis = new FileInputStream(file);
				  BufferedInputStream bis = new BufferedInputStream(fis); ){
				
				OutputStream os = socket.getOutputStream();
				receiverClient.setTimeout(0);	
				
				bis.read(fileContent,0,fileSize);
				System.out.println("Sending " + filename + " to " + receiverName);
				receiverClient.sendBuff("<sendFile> " + filename + " " + fileSize);		//announcement for client

				while( !receiverClient.isSending() ) {		//wait for client acknowledgement
					try { Thread.sleep((long)(ServerInfo.SECOND)); } 
					catch(Exception e) { e.printStackTrace(); }
				}
					
				os.write(fileContent,0,fileSize);			//writing to client
				os.flush();
				receiverClient.setTimeout(ServerInfo.SOCKETS_TIMEOUT);
				System.out.println("File sent to " + receiverName);
				
				fos = new FileOutputStream(receiverPathToFile); //saving in local directory
				fos.write(fileContent,0,fileSize);
				fos.flush();				
			} catch( IOException e ) {
				System.out.println("Cannot send file to client. " + receiverName + " disconnected");
				e.printStackTrace();
				receiverClient.close();
			} finally {
				ServerInfo.sendingFile = false;
				try{ if( fos != null ) fos.close(); } catch( IOException e ) { e.printStackTrace(); }
				receiverClient.setSendingFile(false);
			}

		}
		
	}
}
