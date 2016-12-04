package paralexec;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Directory monitor.
 * 
 * It can tell if the is the directory content changing.
 * 
 * @author oto
 */
final public class DirectoryMonitor
{
	/**
	 * Examined directory.
	 */
	private File directory;
	
	
	/**
	 * Directory files sizes.
	 */
	private Map<String, Long> directoryFiles = new HashMap<>();
	
	
	/**
	 * Constructor:
	 * - load directory
	 * 
	 * @param directory 
	 */
	public DirectoryMonitor(File directory)
	{
		this.directory = directory;
		
		this.loadState();
	}
	
	
	/**
	 * Loads directory file sizes.
	 * 
	 * @return TRUE if any file is new or changed the size.
	 */
	private Boolean loadState()
	{
		Boolean isDirectoryChanged	= false;
		File[] directoryFiles		= this.directory.listFiles();
		
		if (directoryFiles != null)
		{
			for (File file : directoryFiles)
			{
				String fileName = file.getName();
				long fileSize	= file.length();
				
				if (!this.directoryFiles.containsKey(fileName) || this.directoryFiles.get(fileName) < fileSize)
				{
					isDirectoryChanged = true;
					
					// We can call put() on insert and also on update.
					this.directoryFiles.put(fileName, fileSize);
				}
				
			}
		}
		
		return isDirectoryChanged;
	}
	
	
	/**
	 * @return TRUE if any file is new or changed the size.
	 */
	public Boolean hasDirectoryChanged()
	{
		return this.loadState();
	}
}