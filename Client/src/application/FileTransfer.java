package application;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class serving tools to sending and receiving files from client. Every downloading or sending file is realized in thread pool.
 */
class FileTransfer {
	final long SECOND = 1000;
	/**
	 * ExecutorService instance use to sending and downloading files in thread pool
	 */
	static ExecutorService trasferThreadPool = Executors.newCachedThreadPool();
	
	/**
	 * Sending file using thread pool
	 * @param command command = (relativePath to file) + (receiver name)
	 */
	void sendFile(String command) {
		String[] splittedCommand = command.split("\\s");
		String relativePath = splittedCommand[0];
		String receiverName = splittedCommand[1];
		trasferThreadPool.execute(new SendFileTask(relativePath,receiverName));
	}
	/**
	 * Function used to sending file to clients
	 * @param command message from server to client contains information such as filename receiver client
	 */
	void receiveFile(String command) {
		String[] splittedCommand = command.split("\\s");
		String relativePath = splittedCommand[1];
		String fileSize = splittedCommand[2];
		trasferThreadPool.execute(new ReceiveFileTask(relativePath,fileSize));
	}
	
	class SendFileTask implements Runnable{
		String relativePath;
		String receiverName;
		SendFileTask(String _relativePath, String _receiverName){
			relativePath = _relativePath;
			receiverName = _receiverName;
		}
		@Override
		public void run() {
			File file = new File(Connection.directoryName + File.separator + relativePath); 
			if( !file.exists() || !file.isFile() ) {
				System.out.println("File (" + relativePath + ") which you want to send, doesn't exist");
				return;
			}
			
			int fileSize = (int) file.length();
			byte[] fileContent = new byte[fileSize];
			try ( FileInputStream fis = new FileInputStream(file);
				  BufferedInputStream bis = new BufferedInputStream(fis); ){
				
				bis.read(fileContent,0,fileSize);
				System.out.println("Sending file...");
				Connection.sendBuff("<sendFile> " + file.getName() + " " + fileSize + " " + receiverName);	//announcement for server
				 
				while( !Connection.sendingFile ) {						//wait for server acknowledgement
					try { Thread.sleep((SECOND)); } 
					catch(Exception e) { e.printStackTrace(); }		
					if( Connection.fileRejected ) {
						System.out.println("File rejected by server");
						return;
					}
				}
				System.out.println(fileSize + " sending: " + fileContent.toString());
				Connection.sendBytesBuff(fileContent);
				System.out.println("File sent");
			} catch( IOException e ) {
				System.out.println("Sending file failed");
				e.printStackTrace();
			} finally {
				Connection.sendingFile = false;
				Connection.fileRejected = false;
			}
		}
		
	}
	class ReceiveFileTask implements Runnable{
		String relativePath;
		int fileSize;
		ReceiveFileTask(String _relativePath, String _fileSize){
			relativePath = _relativePath;
			fileSize = Integer.parseInt(_fileSize);
		}
		@Override
		public void run() {			
			try(FileOutputStream fos = new FileOutputStream(Connection.directoryDownloadsName + File.separator + relativePath)) {
				//client.skipWatch = true;
				Connection.sendBuff("<acceptFile>");
				System.out.println("Downloading " + relativePath + " file");
				
			    InputStream is = Connection.serverSocket.getInputStream();
			    int allBytesRead=0;
			    int bytesRead;
			    byte[] fileContent = new byte[fileSize];
			    while(allBytesRead < fileSize) {
			    	bytesRead = is.read(fileContent,0,fileSize-allBytesRead);
			    	if( bytesRead == -1 )
			    		break;
			    	allBytesRead += bytesRead;
			    }

			    fos.write(fileContent,0,fileContent.length);    
				
			    Connection.sendBuff("<received> " + relativePath);
			    System.out.println("File " + relativePath + " saved");
			} catch( IOException e ) {
				e.printStackTrace();
			} finally {
				Connection.downloadingFile = false;
				Connection.newFile = true;
				//client.skipWatch = false;
			}
		}
	}
}
