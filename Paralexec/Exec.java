package paralexec;

import Database.Tables.ExecutedProcessesTable;
import Process.ProcessSetting;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

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
	 * File execution template path.
	 */
	private String executionTemplate;


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
	 * @param	process
	 * @param	manager
	 * @throws	IOException
	 */
	public Exec(ProcessSetting process, Paralexec manager) throws IOException
	{
		this.process			= process;
		this.manager			= manager;
		this.scriptsDir			= manager.getScriptsDir();
		this.scriptPath			= this.scriptsDir + this.process.getScriptPath();
		this.executionTemplate	= this.getExecutionTemplateString(this.scriptsDir + this.process.getExecutionTmpPath());
		this.processTable		= manager.getProcessTable();
	}


	/**
	 * @param	templatePath
	 * @return	Execution template string.
	 * @throws	IOException
	 */
	private String getExecutionTemplateString(String templatePath) throws IOException
	{
		byte[] encoded = Files.readAllBytes(Paths.get(templatePath));

		return new String(encoded);
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
			try
			{
				this.manager.manageExecStart(new Exec(child, this.manager));
			}
			catch (Exception e)
			{
				Logger.logError("Cannot create execution for process setting " + child.getId() + ": " + e.getMessage());
			}
		}
	}


	/**
	 * Returns given process PID.
	 *
	 * @param process
	 * @return
	 * @throws Exception
	 */
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


	/**
	 * Adds given process to the process list.
	 *
	 * @param	process
	 * @return	Process PID.
	 */
	private int addProcessToProcessList(Process process)
	{
		int pid = 0;

		try
		{
			pid = this.getProcessPid(process);

			Logger.log("Adding pid " + pid);

			this.manager.addProcessToList(pid, process);
		}
		catch (Exception e)
		{
			Logger.logError(e.getMessage());
		}

		return pid;
	}


	private String getFileBaseName(File file) throws Exception
	{
		String[] parts	= file.getName().split("\\.");

		if (parts.length <= 0)
		{
			throw new Exception("Invalid file name.");
		}

		if (parts.length == 1)
		{
			return file.getName();
		}

		if (parts.length == 2)
		{
			return parts[0];
		}

		String baseName = "";

		for (int i = 0; i < parts.length - 1; i++)
		{
			if (baseName.equals(""))
			{
				baseName = parts[i];
			}
			else
			{
				baseName += "." + parts[i];
			}
		}

		return baseName;
	}


	private String getFileExecutionCommand(String inputFilePath, String inputFileBaseName)
	{
		String command = new String(this.executionTemplate);

		command = command.replace("[file_name]", inputFilePath);
		command = command.replace("[file_base_name]", inputFileBaseName);

		return command + " >/dev/null 2>&1 &";
	}


	private void runProcessOnFile(String inputFilePath, String inputFileBaseName) throws IOException
	{
		Process process = Runtime.getRuntime().exec(this.getFileExecutionCommand(inputFilePath, inputFileBaseName));
	}


	@Override
	public void run()
	{
		// Executes the script and waits until its finished.
		try
		{
			String processId = process.getId() + " (" + this.scriptsDir + ")";

			Logger.log("Running process: " + processId);

			// Lets iterate through all files in input directory.
			File inputDir			= new File(this.process.getInputDirPath());
			File[] inputDirFiles	= inputDir.listFiles();

			// We throw exception if we cannot load the files.
			if (inputDirFiles == null)
			{
				throw new Exception("Cannot load input dir files (" + this.process.getInputDirPath() + ")");
			}

			for (File inputFile : inputDirFiles)
			{
				// Input file must have also input extension.
				if (inputFile.getName().endsWith("." + this.process.getInputExt()))
				{
					this.runProcessOnFile(inputFile.getAbsolutePath(), this.getFileBaseName(inputFile));
				}
			}





			// Start the process and wait until its end.
			Process process = Runtime.getRuntime().exec(this.scriptPath + " >/dev/null 2>&1 &");

			int pid = this.addProcessToProcessList(process);

			// We need to vomit outputs for prevent the OS buffer overflow.
			BufferVomitor inputStreamVomit = new BufferVomitor("stdin", process.getInputStream());
			BufferVomitor errorStreamVomit = new BufferVomitor("stderr", process.getErrorStream());

			inputStreamVomit.start();
			errorStreamVomit.start();

			process.waitFor();

			if (pid > 0)
			{
				this.manager.deleteProcessFromList(pid);
			}

			Logger.log("Script " + processId + " finished.");

			if (this.manager.isRunning())
			{
				this.processChildren();
			}
		}
		catch (Exception e)
		{
			this.error = e.getMessage();

			Logger.log("Script " + this.scriptPath + " finished with error: " + e.getMessage());
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