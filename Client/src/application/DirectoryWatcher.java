package application;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 * Implement watcher for passed directory
 */
public class DirectoryWatcher implements Runnable{
	/**
	 * Directory watcher
	 */
	WatchService watcher;
	
	/**
	 * Register directory to watch
	 * @throws IOException if I/O exceptions occurs
	 */
	DirectoryWatcher() throws IOException{
		watcher = FileSystems.getDefault().newWatchService();
		Path path = Paths.get(Connection.directoryName);
		try {
		    path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
		} catch (IOException ex) {
		    ex.printStackTrace();
		}
	}
	/**
	 * Watch directory and react for events till client is running
	 */
	@Override
	public void run() {
        try {
        	while(Connection.clientRunning) {
        		WatchKey key;
				while ((key = watcher.take()) != null) {
				    for (WatchEvent<?> event : key.pollEvents()) {
				    	if( event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) {
				    		System.out.println("Event create " +  event.context());
				    		Thread.sleep(1000);				//don't know why, but need to wait a while for sending
				    		Connection.fileTransfer.sendFile(event.context() + " server");
				    	} else if( event.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) {
				    		System.out.println("Event delete " +  event.context());
				    		Connection.sendBuff("<delete> " + event.context());
				    	}
				    }
				    key.reset();
				}
			} 
		} catch (InterruptedException e) {System.out.println("DirectoryWatcher: interrupted");}
		System.out.println("DirectoryWatcher: closed");
	}
	
}
