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
 *	java -jar Paralexec.jar 10
 *
 * Parameters:
 *
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


	/**
	 * Constructor.
	 *
	 * @throws Exception
	 */
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

			// Set the maximum number of running threads if it's provided.
			if (args.length >= 1)
			{
				paralexec.setRunningThreadsMaxCount(Integer.parseInt(args[0]));
			}

			// Execute the process tree.
			paralexec.processSettings();
		}
		// Handle error.
		catch (Exception e)
		{
			Logger.logError("Paralexec execution error: " + e.getMessage());

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
		Logger.log("Loading process tree.");

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
	 * Process root processes.
	 */
	private void processSettings()
	{
		this.createRunningFile();

		this.startMonitor();

		// We will fill the queue with the root processes.
		for (ProcessSetting process : this.processTree.getRootItems())
		{
			try
			{
				this.addExecToQeue(new Exec(process, this));
			}
			catch (Exception e)
			{
				Logger.logError("Cannot create execution for process setting " + process.getId() + ": " + e.getMessage());
			}
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


	/**
	 * Starts the self-monitoring.
	 */
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
		Logger.log("Creating running file: " + this.runningFlagFilePath);

		try
		{
			if (this.isRunning())
			{
				throw new Exception("Paralexec is already running.");
			}

			Files.createFile(this.runningFlagFilePath);
		}
		catch (Exception e)
		{
			Logger.logError("Paralexec error while creating the running file: " + e.getMessage());
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
				Logger.log("Deleting running file.");

				Files.delete(this.runningFlagFilePath);
			}
		}
		catch (IOException e)
		{
			Logger.logError("Paralexec error while deleting running file: " + e.getMessage());
		}
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

				Logger.log("Executing process (threads count = " + this.runningThreads + ").");

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
			Logger.log("Unable to mark running processes as waiting: " + e.getMessage());
		}
	}


	/**
	 * Kills all processes in the list.
	 */
	private void killAllProcesses()
	{
		Logger.log("Killing all running processes.");

		for (Map.Entry<Integer, Process> item : this.processList.entrySet())
		{
			try
			{
				Logger.log("Destroying the process.");

				item.getValue().destroy();
			}
			catch (Exception e)
			{
				Logger.logError("Cannot kill with PID " + item.getKey() + ": " + e.getMessage());
			}
		}

		this.processList.clear();
	}
	
	
	/**
	 * Finds process in process list and kills it.
	 * 
	 * @param pid 
	 */
	public void killProcessById(int pid)
	{
		Process process = this.processList.get(pid);
		
		if (process != null)
		{
			process.destroy();
			this.deleteProcessFromList(pid);
		}
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
			Logger.logError("Unable to mark process " + process.getId() + " as running: " + e.getMessage());
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
			Logger.logError("Unable to mark process " + process.getId() + " as finished: " + e.getMessage());
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
	 * @param pid
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

		Logger.log("Ending process (threads count = " + this.runningThreads + ").");

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