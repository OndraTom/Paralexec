package Paralexec;

import Database.DatabaseException;
import Database.Drivers.DbDriverException;
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
	 * Executed execs list.
	 */
	private Map<Integer, Exec> runningExecs = new HashMap<>();
	
	
	/**
	 * Last change time.
	 */
	private long lastChangeTime;


	/**
	 * Constructor.
	 *
	 * @throws Exception
	 */
	public Paralexec() throws Exception
	{
		this.lastChangeTime			= System.currentTimeMillis();
		this.processTable			= this.getExecutedProcessesTableInstance();
		this.currentDir				= this.getCurrentDir();
		this.runningFlagFilePath	= Paths.get(this.currentDir + File.separator + "running");
		this.execQueue				= new LinkedList();

		this.loadProcessTree();
	}
	
	
	/**
	 * Restarts itself.
	 * 
	 * @param runMonitor
	 * @throws Exception 
	 */
	public void restart(boolean runMonitor) throws Exception
	{
		this.stopProcessing();
		this.deleteRunningFile();
		
		this.runningThreads				= 0;
		this.processingQueue			= false;
		this.execQueue					= new LinkedList();
		this.runningExecs				= new HashMap<>();
		this.lastChangeTime				= System.currentTimeMillis();
		
		this.loadProcessTree();
		this.processSettings(runMonitor);
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
			paralexec.processSettings(true);
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
	 */
	private ExecutedProcessesTable getExecutedProcessesTableInstance() throws DbDriverException
	{
		return new ExecutedProcessesTable();
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
	 * 
	 * @param runMonitor 
	 */
	private void processSettings(boolean runMonitor)
	{
		this.createRunningFile();
		
		if (runMonitor)
		{
			this.startMonitor();
		}

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

				this.addRunningExec(exec);
				this.markProcessAsRunning(exec.getProcess());

				exec.start();
			}

			this.processingQueue = false;
		}
	}


	/**
	 * Adds exec into the list of running execs.
	 *
	 * @param exec
	 */
	private void addRunningExec(Exec exec)
	{
		this.runningExecs.put(exec.getProcess().getId(), exec);
	}


	/**
	 * Deletes exec from the list of running execs.
	 *
	 * @param exec
	 */
	private void deleteRunningExec(Exec exec)
	{
		this.runningExecs.remove(exec.getProcess().getId());
	}


	/**
	 * Stops all execs from the list of running execs.
	 */
	private void interruptAllRunningExecs()
	{
		for (Map.Entry<Integer, Exec> item : this.runningExecs.entrySet())
		{
			item.getValue().interrupt();
		}
	}


	/**
	 * Stops processing.
	 */
	public void stopProcessing()
	{
		Logger.log("Interrupting Execs.");
		this.interruptAllRunningExecs();

		try
		{
			this.processTable.stopRunningProcesses();
		}
		catch (DatabaseException e)
		{
			Logger.log("Unable to mark running processes as waiting: " + e.getMessage());
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
		catch (DatabaseException e)
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
		catch (DatabaseException e)
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
	 * Manages the exec restart.
	 *
	 * @param newExec
	 */
	public void manageExecRestart(Exec newExec)
	{
		// Rewrite the old exec with the new one.
		this.addRunningExec(newExec);
	}


	/**
	 * Manages end of the process execution.
	 *
	 * @param exec
	 */
	public void manageExecEnd(Exec exec)
	{
		this.runningThreads--;

		this.deleteRunningExec(exec);
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


	/**
	 * Manages exec interruption.
	 *
	 * @param exec
	 */
	public void manageExecInterruption(Exec exec)
	{
		this.deleteRunningExec(exec);
	}
	
	
	/**
	 * @return Last change time (in milliseconds).
	 */
	public long getLastChangeTime()
	{
		return System.currentTimeMillis() - this.lastChangeTime;
	}
	
	
	/**
	 * Updates the last change time - action indicator.
	 */
	public void ping()
	{
		this.lastChangeTime = System.currentTimeMillis();
	}
}