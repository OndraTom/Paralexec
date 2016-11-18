package paralexec;

import Database.Drivers.DbDriverException;
import Database.Tables.DbTableException;
import Database.Tables.ExecutedProcessesTable;
import Process.ProcessSetting;
import Process.ProcessSettingException;
import Process.ProcessSettingTree;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Main class for parallel executing of the tree structure processes.
 *
 * Example of Paralexec call:
 *
 *	java -jar Paralexec.jar "processes-scripts/" 10
 *
 * Parameters:
 *
 *  - processes folders and files path
 *  - maximum number of running threads (optional)
 *
 * @author oto
 */
final public class Paralexec
{
	/**
	 * Executed processes table.
	 */
	private ExecutedProcessesTable processTable;
	
	
	/**
	 * Process tree.
	 */
	private ProcessSettingTree processTree;
	
	
	/**
	 * Executed processes list.
	 */
	private Map<Integer, Process> processList = new HashMap<>();
	
	
	/**
	 * Current directory path.
	 */
	private String currentDir;
	
	
	/**
	 * Process scripts directory.
	 */
	private String scriptsDir;
	
	
	/**
	 * Number of running threads.
	 */
	private int runningThreads = 0;


	/**
	 * Maximum number of running threads.
	 *
	 * 0 = unlimited
	 */
	private int runningThreadsMaxCount = 0;


	/**
	 * Path to the running flag file.
	 */
	private Path runningFlagFilePath;
	
	
	/**
	 * Processing queue flag.
	 */
	private boolean processingQueue = false;


	/**
	 * Execs queue.
	 *
	 * Paralexec is using this queue in case of limited number of executed threads.
	 */
	private Queue execQueue;


	public Paralexec() throws Exception
	{
		this.processTable			= this.getExecutedProcessesTableInstance();
		this.currentDir				= this.getCurrentDir();
		this.runningFlagFilePath	= Paths.get(this.currentDir + File.separator + "running");
		this.execQueue				= new LinkedList();

		this.loadProcessTree();
	}


	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		Paralexec paralexec = null;
		
		try
		{
			paralexec = new Paralexec();
			
			// Mandatory arguments validation.
			paralexec.checkArgs(args);
			
			// Setting the processes scripts directory.
			paralexec.setScriptsDir(args[0]);

			// Set the maximum number of running threads if it's provided.
			if (args.length >= 2)
			{
				paralexec.setRunningThreadsMaxCount(Integer.parseInt(args[1]));
			}

			// Execute the process tree.
			paralexec.processSettings();
		}
		// Handle error.
		catch (Exception e)
		{
			writeErrorMessage("Paralexec execution error: " + e.getMessage());
			
			if (paralexec != null)
			{
				paralexec.deleteRunningFile();
			}
		}
	}
	
	
	/**
	 * @return True if running file exists.
	 */
	public boolean isRunning()
	{
		return Files.exists(this.runningFlagFilePath);
	}
	
	
	/**
	 * @return Processes scripts direcotry.
	 */
	public String getScriptsDir()
	{
		return this.scriptsDir;
	}
	
	
	/**
	 * @return Executed processes table instance.
	 * @throws Exception 
	 */
	private ExecutedProcessesTable getExecutedProcessesTableInstance() throws Exception
	{
		try
		{
			return new ExecutedProcessesTable();
		}
		catch (DbDriverException e)
		{
			throw new Exception("DB error: " + e.getMessage());
		}
	}
	
	
	/**
	 * @return Executed processes table.
	 */
	public ExecutedProcessesTable getProcessTable()
	{
		return this.processTable;
	}
	
	
	/**
	 * Loads the process setting tree.
	 * 
	 * @throws Exception 
	 */
	private void loadProcessTree() throws Exception
	{
		System.out.println("Loading process tree.");
		
		try
		{
			this.processTree = new ProcessSettingTree(this.processTable);
		}
		catch (ProcessSettingException e)
		{
			throw new Exception(e.getMessage());
		}
	}
	
	
	/**
	 * Sets the process scripts directory.
	 * 
	 * @param scriptsDir 
	 */
	private void setScriptsDir(String scriptsDir)
	{
		this.scriptsDir = scriptsDir;
	}


	/**
	 * Sets the maximum number of running threads.
	 *
	 * @param	count
	 * @throws	Exception
	 */
	private void setRunningThreadsMaxCount(int count) throws Exception
	{
		// It has to be positive number or zero.
		if (count < 0)
		{
			throw new Exception("Maximum number of running threads has to be positive number or 0 for infinity.");
		}

		this.runningThreadsMaxCount = count;
	}


	/**
	 * @return Current dir location.
	 */
	private String getCurrentDir()
	{
		File currentDir = new File(".");

		return currentDir.getAbsolutePath();
	}


	/**
	 * Checks the CLI arguments.
	 *
	 * @param	args
	 * @throws	Exception
	 */
	private void checkArgs(String[] args) throws Exception
	{
		if (args.length < 1)
		{
			throw new Exception("Processes scripts directory not provided.");
		}
	}


	/**
	 * Process root processes.
	 */
	private void processSettings()
	{
		this.createRunningFile();
		
		this.startMonitor();
		
		// We will fill the queue with the root processes.
		for (ProcessSetting process : this.processTree.getRootItems())
		{	
			this.addExecToQeue(new Exec(process, this));
		}

		// If the queue is empty, we are done.
		if (this.execQueue.isEmpty())
		{
			this.deleteRunningFile();
		}
		// Otherwise, let's start the process.
		else
		{
			this.processQueue();
		}
	}
	
	
	private void startMonitor()
	{
		ParalexecMonitor monitor = new ParalexecMonitor(this);
		
		monitor.start();
	}


	/**
	 * Creates the running file.
	 */
	private void createRunningFile()
	{
		System.out.println("Creating running file: " + this.runningFlagFilePath);
		
		try
		{
			if (this.isRunning())
			{
				throw new Exception("Paralexec is running.");
			}

			Files.createFile(this.runningFlagFilePath);
		}
		catch (Exception e)
		{
			writeErrorMessage("Paralexec error while creating the running file: " + e.getMessage());
			System.exit(0);
		}
	}


	/**
	 * Deletes the running file.
	 */
	private void deleteRunningFile()
	{	
		try
		{
			if (this.isRunning())
			{
				System.out.println("Deleting running file.");
				
				Files.delete(this.runningFlagFilePath);
			}
		}
		catch (IOException e)
		{
			writeErrorMessage("Paralexec error while deleting running file: " + e.getMessage());
		}
	}


	/**
	 * Write error message on output.
	 *
	 * @param msg
	 */
	private static void writeErrorMessage(String msg)
	{
		System.out.println(msg);
	}


	/**
	 * Inserts the Exec into the Exec queue.
	 *
	 * @param exec
	 */
	private void addExecToQeue(Exec exec)
	{
		this.execQueue.add(exec);
	}


	/**
	 * Processes the Exec queue.
	 */
	private void processQueue()
	{
		if (!this.isRunning())
		{
			this.stopProcessing();
			return;
		}
		
		if (!this.processingQueue)
		{
			this.processingQueue = true;
			
			while ((this.runningThreadsMaxCount == 0 || this.runningThreads < this.runningThreadsMaxCount) && !this.execQueue.isEmpty() && this.isRunning())
			{	
				this.runningThreads++;
				
//				try
//				{
//					Thread.sleep(10000);
//				}
//				catch (InterruptedException ex) {}

				System.out.println("Executing process (threads count = " + this.runningThreads + ").");

				Exec exec = (Exec) this.execQueue.poll();

				this.markProcessAsRunning(exec.getProcess());

				exec.start();
			}
			
			this.processingQueue = false;
		}
	}
	
	
	/**
	 * Stops processing.
	 */
	public void stopProcessing()
	{
		this.killAllProcesses();

		try
		{
			this.processTable.stopRunningProcesses();
		}
		catch (DbTableException e)
		{
			System.out.println("Unable to mark running processes as waiting: " + e.getMessage());
		}
	}
	
	
	/**
	 * Kills all processes in the list.
	 */
	private void killAllProcesses()
	{
		System.out.println("Killing all running processes.");
		
		for (Map.Entry<Integer, Process> item : this.processList.entrySet())
		{
			try
			{
//				System.out.println("Killing process: kill -9 " + item.getKey());
//				
//				//Process p = Runtime.getRuntime().exec("kill -9 " + item.getKey());
//				ProcessBuilder ps = new ProcessBuilder(new String[]{"kill", "-9", item.getKey().toString()});
//				
//				ps.redirectErrorStream(true);
//				
//				Process p = ps.start();
//				
//				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
//				String line;
//				while ((line = in.readLine()) != null)
//				{
//					System.out.println("Output line: " + line);
//				}
//				
//				p.waitFor();
//				
//				System.out.println("Exit value: " + p.exitValue());
//				
//				item.getValue().destroy();
//				
//				in.close();
				
				System.out.println("Destroying the process.");
				
				item.getValue().destroy();
			}
			catch (Exception e)
			{
				System.out.println("Cannot kill with PID " + item.getKey() + ": " + e.getMessage());
			}
		}
		
		this.processList.clear();
	}
	
	
	/**
	 * Marks selected process as RUNNING.
	 * 
	 * @param process 
	 */
	private void markProcessAsRunning(ProcessSetting process)
	{
		try
		{
			this.processTable.markProcessAsRunning(process.getId());
		}
		catch (DbTableException e)
		{
			writeErrorMessage("Unable to mark process " + process.getId() + " as running: " + e.getMessage());
		}
	}
	
	
	/**
	 * Marks selected process as FINISHED.
	 * 
	 * @param process 
	 */
	private void markProcessAsFinished(ProcessSetting process, String error)
	{
		try
		{
			this.processTable.markProcessAsFinished(process.getId(), error);
		}
		catch (DbTableException e)
		{
			writeErrorMessage("Unable to mark process " + process.getId() + " as finished: " + e.getMessage());
		}
	}


	/**
	 * Manages start of the new process execution.
	 * 
	 * @param exec 
	 */
	public void manageExecStart(Exec exec)
	{
		this.addExecToQeue(exec);
		this.processQueue();
	}
	
	
	/**
	 * Adds process to the process list.
	 * 
	 * @param process 
	 */
	public void addProcessToList(int pid, Process process)
	{
		this.processList.put(pid, process);
	}
	
	
	/**
	 * Removes process from the process list.
	 * 
	 * @param pid 
	 */
	public void deleteProcessFromList(int pid)
	{
		this.processList.remove(pid);
	}


	/**
	 * Manages end of the process execution.
	 * 
	 * @param exec 
	 */
	public void manageExecEnd(Exec exec)
	{
		this.runningThreads--;
		
		this.markProcessAsFinished(exec.getProcess(), exec.getError());
		
		System.out.println("Ending process (threads count = " + this.runningThreads + ").");

		if (this.runningThreads == 0 && this.execQueue.isEmpty())
		{
			this.deleteRunningFile();
		}
		else
		{
			this.processQueue();
		}
	}
}