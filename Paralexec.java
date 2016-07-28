package paralexec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import org.json.*;

/**
 * Main class for parallel executing of the tree structure processes.
 *
 * Example of Paralexec call:
 *
 *	java -jar Paralexec.jar "execution-scenario.json" "processes-scripts/" 10
 *
 * Parameters:
 *
 *	- processes execution scenario tree
 *  - processes folders and files path
 *  - maximum number of runnin threads (optional)
 *
 * @author oto
 */
public class Paralexec implements ExecManager
{
	/**
	 * Number of running threads.
	 */
	protected int runningThreads = 0;


	/**
	 * Maximum number of running threads.
	 *
	 * 0 = unlimited
	 */
	protected int runningThreadsMaxCount = 0;


	/**
	 * Path to the running flag file.
	 */
	protected Path runningFlagFilePath;


	/**
	 * Execs queue.
	 *
	 * Paralexec is using this queue in case of limited number of executed threads.
	 */
	protected Queue execQueue;


	public Paralexec()
	{
		this.runningFlagFilePath	= Paths.get(this.getCurrentDir() + File.separator + "running");
		this.execQueue				= new LinkedList();

		this.createRunningFile();
	}


	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		Paralexec paralexec = new Paralexec();

		try
		{
			// Mandatory arguments validation.
			paralexec.checkArgs(args);

			// Parse the scenario.
			String jsonString	= paralexec.readFile(args[0]);
			JSONObject json		= new JSONObject(jsonString);
			JSONObject scenario = json.getJSONObject("scenario");

			// Set the maximum number of running threads if provided.
			if (args.length >= 3)
			{
				paralexec.setRunningThreadsMaxCount(Integer.parseInt(args[2]));
			}

			// Execute the process tree.
			paralexec.processSettings(scenario, args[1]);
		}
		// Handle error.
		catch (Exception e)
		{
			Paralexec.handleError(e);
			paralexec.deleteRunningFile();
		}
	}


	/**
	 * Sets the maximum number of running threads.
	 *
	 * @param	count
	 * @throws	Exception
	 */
	protected void setRunningThreadsMaxCount(int count) throws Exception
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
	protected String getCurrentDir()
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
	protected void checkArgs(String[] args) throws Exception
	{
		if (args.length < 1)
		{
			throw new Exception("Execution scenario not provided.");
		}

		if (args.length < 2)
		{
			throw new Exception("Processes scripts directory not provided.");
		}
	}


	/**
	 * Process the zero level settings.
	 *
	 * @param settings
	 * @param scriptsDir
	 */
	protected void processSettings(JSONObject settings, String scriptsDir)
	{
		Iterator keys = settings.keys();

		// We will fill the queue with the zero level processes.
		while (keys.hasNext())
		{
			String settingId = (String) keys.next();

			try
			{
				this.addExecToQeue(new Exec(settings.getJSONObject(settingId), scriptsDir, this));
			}
			catch (JSONException e)
			{
				this.handleError(e);
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
	 * Returns the content of the json execution scenario file.
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	protected String readFile(String path) throws IOException
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));

		return new String(encoded);
	}


	/**
	 * Creates the running file.
	 */
	protected void createRunningFile()
	{
		try
		{
			if (Files.exists(this.runningFlagFilePath))
			{
				throw new Exception("Paralexec is running.");
			}

			Files.createFile(this.runningFlagFilePath);
		}
		catch (Exception e)
		{
			this.handleError(e);
			System.exit(0);
		}
	}


	/**
	 * Deletes the running file.
	 */
	protected void deleteRunningFile()
	{
		try
		{
			if (Files.exists(this.runningFlagFilePath))
			{
				Files.delete(this.runningFlagFilePath);
			}
		}
		catch (IOException e)
		{
			this.handleError(e);
		}
	}


	/**
	 * Handles the exception.
	 *
	 * @param e
	 */
	protected static void handleError(Exception e)
	{
		Paralexec.writeErrorMessage(e.getMessage());
	}


	/**
	 * Write error message on output.
	 *
	 * @param msg
	 */
	protected static void writeErrorMessage(String msg)
	{
		System.out.println("Exec error: " + msg);
	}


	/**
	 * Inserts the Exec into the Exec queue.
	 *
	 * @param exec
	 */
	protected void addExecToQeue(Exec exec)
	{
		this.execQueue.add(exec);
	}


	/**
	 * Processes the Exec queue.
	 */
	protected void processQueue()
	{
		while ((this.runningThreadsMaxCount == 0 || this.runningThreads < this.runningThreadsMaxCount) && !this.execQueue.isEmpty())
		{
			this.runningThreads++;

			Exec exec = (Exec) this.execQueue.poll();

			exec.start();
		}
	}


	@Override
	public void manageExecStart(Exec exec)
	{
		this.addExecToQeue(exec);
		this.processQueue();
	}


	@Override
	public void manageExecEnd()
	{
		this.runningThreads--;

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