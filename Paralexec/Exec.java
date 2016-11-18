package paralexec;

import Database.Tables.ExecutedProcessesTable;
import Process.ProcessSetting;
import java.lang.reflect.Field;

/**
 * Thread of the parallel execution.
 *
 * @author oto
 */
final public class Exec implements Runnable
{
	/**
	 * Actual process setting.
	 */
	private ProcessSetting process;


	/**
	 * Processes scripts directory.
	 */
	private String scriptsDir;


	/**
	 * Executing script path.
	 */
	private String scriptPath;


	/**
	 * Exec manager.
	 */
	private Paralexec manager;
	
	
	/**
	 * Executed processes table.
	 */
	private ExecutedProcessesTable processTable;
	
	
	/**
	 * Execution error message.
	 */
	private String error = null;


	/**
	 * Constructor.
	 *
	 * @param process
	 * @param manager
	 */
	public Exec(ProcessSetting process, Paralexec manager)
	{
		this.process		= process;
		this.manager		= manager;
		this.scriptsDir		= manager.getScriptsDir();
		this.scriptPath		= this.scriptsDir + this.process.getScriptPath();
		this.processTable	= manager.getProcessTable();
	}
	
	
	/**
	 * @return Process setting.
	 */
	public ProcessSetting getProcess()
	{
		return this.process;
	}
	
	
	/**
	 * @return Execution error message.
	 */
	public String getError()
	{
		return this.error;
	}


	/**
	 * Process the setting children.
	 */
	private void processChildren()
	{
		for (ProcessSetting child : this.process.getChildren())
		{
			this.manager.manageExecStart(new Exec(child, this.manager));
		}
	}
	
	
	private int getProcessPid(Process process) throws Exception
	{
		try
		{	
			Field f = process.getClass().getDeclaredField("pid");
			
			f.setAccessible(true);
			
			return f.getInt(process);
		}
		catch (Exception e)
		{
			throw new Exception("Cannot get PID from process.");
		}
	}
	
	
	private int addProcessToProcessList(Process process)
	{
		int pid = 0;
			
		try
		{
			pid = this.getProcessPid(process);
			
			System.out.println("Adding pid " + pid);

			this.manager.addProcessToList(pid, process);
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		return pid;
	}


	@Override
	public void run()
	{
		// Executes the script and waits until its finished.
		try
		{
			System.out.println("Running script: " + this.scriptPath);

			// Start the process and wait until its end.
			//Process process = new ProcessBuilder(this.scriptPath).start();
			Process process = Runtime.getRuntime().exec(this.scriptPath);
			
			int pid = this.addProcessToProcessList(process);
			
			process.waitFor();
			
			if (pid > 0)
			{
				this.manager.deleteProcessFromList(pid);
			}

			System.out.println("Script " + this.scriptPath + " finished.");
			
			if (this.manager.isRunning())
			{
				this.processChildren();
			}
		}
		catch (Exception e)
		{
			this.error = e.getMessage();
			
			System.out.println("Script " + this.scriptPath + " finished with error: " + e.getMessage());
		}
		finally
		{
			this.manager.manageExecEnd(this);
		}
	}


	/**
	 * Creates and starts the thread.
	 */
	public void start()
	{
		Thread t = new Thread(this);

		t.start();
	}
}