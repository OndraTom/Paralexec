package paralexec;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author oto
 */
final public class DirectoryMonitor
{
	private File directory;
	
	
	private Map<String, Long> directoryFiles = new HashMap<>();
	
	
	public DirectoryMonitor(File directory)
	{
		this.directory = directory;
		
		this.loadState();
	}
	
	
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
					
					this.directoryFiles.put(fileName, fileSize);
				}
				
			}
		}
		
		return isDirectoryChanged;
	}
	
	
	public Boolean hasDirectoryChanged()
	{
		return this.loadState();
	}
}