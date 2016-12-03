package paralexec;

import Database.Tables.ExecutedProcessesTable;
import Process.ProcessSetting;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
	 * @param	process
	 * @param	manager
	 * @throws	IOException
	 */
	public Exec(ProcessSetting process, Paralexec manager) throws IOException
	{
		this.process			= process;
		this.manager			= manager;
		this.scriptPath			= this.process.getScriptPath();
		this.processTable		= manager.getProcessTable();
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
	
	
	/**
	 * Deletes all files in given directory.
	 * 
	 * @param outputDirPath 
	 */
	private void cleanOutputDir(String outputDirPath)
	{
		File outputDir			= new File(outputDirPath);
		File[] outputDirFiles	= outputDir.listFiles();
		
		if (outputDirFiles != null)
		{
			for (File outputFile : outputDirFiles)
			{
				if (outputFile.isFile())
				{
					outputFile.delete();
				}
			}
		}
	}


	/**
	 * It executes the shell script which echoes the CLI command.
	 * 
	 * @param	file
	 * @return	Command for input file execution.
	 */
	private String getFileExecutionCommand(File file) throws IOException, InterruptedException
	{
		Path shellPath		= Paths.get(this.scriptPath);
		Charset charset		= StandardCharsets.UTF_8;
		String shellContent = new String(Files.readAllBytes(shellPath), charset);
		shellContent		= shellContent.replace("[input_file_name]", file.getAbsolutePath());
		
		Files.write(shellPath, shellContent.getBytes(charset));
		
		// BE AWARE! Vomitor is not working here! We need seqence processing
		// of the stream here (it's too quick for Vomitor to take it).
		Process shellProcess	= Runtime.getRuntime().exec(this.scriptPath);
		BufferedReader reader	= new BufferedReader(new InputStreamReader(shellProcess.getInputStream()));
		
		String line, command = "";
		
		while ((line = reader.readLine()) != null)
		{
			command += line;
		}
		
		shellProcess.waitFor();
		
		return command;
	}


	/**
	 * Executes process on given input file.
	 * 
	 * @param	inputFile
	 * @throws	IOException
	 * @throws	InterruptedException 
	 */
	private void runProcessOnFile(File inputFile) throws IOException, InterruptedException
	{
		String command = this.getFileExecutionCommand(inputFile);
		
		Logger.log("Executing cmd: " + command);
		
		Process process = Runtime.getRuntime().exec(command);
		
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
	}


	@Override
	public void run()
	{
		// Executes the script and waits until its finished.
		try
		{
			Logger.log("Running process: " + this.process.getId());

			// Lets iterate through all files in input directory.
			File inputDir			= new File(this.process.getInputDirPath());
			File[] inputDirFiles	= inputDir.listFiles();

			// We throw exception if we cannot load the files.
			if (inputDirFiles == null)
			{
				throw new Exception("Cannot load input dir files (" + this.process.getInputDirPath() + ")");
			}
			
			this.cleanOutputDir(this.process.getOutputDirPath());

			for (File inputFile : inputDirFiles)
			{
				// Input file must have also input extension.
				if (inputFile.getName().endsWith("." + this.process.getInputExt()))
				{
					this.runProcessOnFile(inputFile);
				}
			}

			Logger.log("Script " + this.process.getId() + " finished.");

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