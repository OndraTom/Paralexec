package paralexec;

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
import java.util.Arrays;
import java.util.Random;

/**
 * Thread of the parallel execution.
 *
 * @author oto
 */
final public class Exec implements Runnable
{
	/**
	 * Maximum count of try to open a file.
	 */
	private static int FILE_OPENING_MAX_TRY_COUNT = 5;
	
	
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
	 * Execution error message.
	 */
	private String error = null;


	/**
	 * Number of processed input files.
	 */
	private int processedFilesCount = 0;


	/**
	 * Interrupted flag.
	 */
	private Boolean interrupted = false;


	/**
	 * Running process ID (PID).
	 */
	private int runningProcessId = 0;


	/**
	 * Running flag.
	 */
	private Boolean isRunning = true;


	/**
	 * Number of execution attempts.
	 */
	private int attempts = 1;


	/**
	 * Constructor.
	 *
	 * @param	process
	 * @param	manager
	 * @throws	IOException
	 */
	public Exec(ProcessSetting process, Paralexec manager) throws IOException
	{
		this.process	= process;
		this.manager	= manager;
		this.scriptPath	= this.process.getScriptPath();
	}


	/**
	 * Constructor for cloning the Exec.
	 *
	 * Using the Cloneable interface is not recommended.
	 *
	 * @param origin
	 */
	public Exec(Exec origin)
	{
		this.process				= origin.process;
		this.scriptPath				= origin.scriptPath;
		this.manager				= origin.manager;
		this.error					= origin.error;
		this.processedFilesCount	= origin.processedFilesCount;
		this.attempts				= origin.attempts + 1;
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
	 * @return Running process ID.
	 */
	public int getRunningProcessId()
	{
		return this.runningProcessId;
	}


	/**
	 * @return Attempts count.
	 */
	public int getAttemptsCount()
	{
		return this.attempts;
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
	
	
	private String getProcessChildrenIds()
	{
		String ids = "";
		
		for (ProcessSetting child : this.process.getChildren())
		{
			if (!ids.equals(""))
			{
				ids += ", ";
			}
			
			ids += Integer.toString(child.getId());
		}
		
		return ids;
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

			Random rand = new Random();
			pid			= rand.nextInt(Integer.MAX_VALUE);
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
		String tmpPath		= this.scriptPath + ".tmp.sh";
		Path shellPath		= Paths.get(this.scriptPath);
		Charset charset		= StandardCharsets.UTF_8;
		String shellContent = new String(Files.readAllBytes(shellPath), charset);
		shellContent		= shellContent.replace("[input_file_name]", file.getAbsolutePath());
		shellContent		= shellContent.replace("[file-number]", Integer.toString(this.processedFilesCount + 1));

		// TODO: this splitting is bad - it will also split the words ends with "echo"
		String[] echoSplit	= shellContent.split("echo ");

		// We will escape the last echo.
		if (echoSplit.length > 1)
		{
			echoSplit[echoSplit.length - 1] = this.getEscapedShellEcho(echoSplit[echoSplit.length - 1]);
		}

		// Implode script.
		shellContent = "";

		for (String p : echoSplit)
		{
			if (shellContent.equals(""))
			{
				shellContent = p;
			}
			else
			{
				shellContent += "echo " + p;
			}
		}

		Files.write(Paths.get(tmpPath), shellContent.getBytes(charset));

		// Setting the tmp file permissions.
		File tmpFile = new File(tmpPath);
		tmpFile.setReadable(true, false);
		tmpFile.setWritable(true, false);
		tmpFile.setExecutable(true, false);
		
		// command = template call output
		String command = this.getTmpFileOutput(tmpPath);
		
		tmpFile.delete();

		return command;
	}
	
	
	/**
	 * @param tmpPath
	 * @return Template file string output.
	 * @throws IOException 
	 */
	private String getTmpFileOutput(String tmpPath) throws IOException
	{
		String line, output = "";
		int tryToOpenTimes	= 0;
		boolean opened		= false;
		
		while (!opened && tryToOpenTimes < FILE_OPENING_MAX_TRY_COUNT)
		{
			tryToOpenTimes++;
			
			try
			{
				// BE AWARE! Vomitor is not working here! We need seqence processing
				// of the stream here (it's too quick for Vomitor to take it).
				Process shellProcess	= Runtime.getRuntime().exec(tmpPath);
				BufferedReader reader	= new BufferedReader(new InputStreamReader(shellProcess.getInputStream()));

				while ((line = reader.readLine()) != null)
				{
					output += line;
				}

				shellProcess.waitFor();

				opened = true;
				
				// Remove the temporary file.
				reader.close();
			}
			catch (Exception e)
			{
				try
				{
					Thread.sleep(1000);
				}
				catch (Exception ex) {}
			}
		}
		
		if (!opened)
		{
			throw new IOException("Cannot open tmp file");
		}

		return output;
	}


	/**
	 * @param	echoString
	 * @return	Escaped shell echo string.
	 */
	private String getEscapedShellEcho(String echoString)
	{
		// Escaping special characters in last echo.
		String[] escapeCharacters	= {">", "<", "|"};
		String[] echoSplit			= echoString.split("\"");

		// We will escape only parts which are not between quotation marsk.
		for (int i = 0; i < echoSplit.length; i += 2)
		{
			for (String escapeCharacter : escapeCharacters)
			{
				echoSplit[i] = echoSplit[i].replace(escapeCharacter, "\\" + escapeCharacter);
			}
		}

		// Implode last echo.
		echoString = "";
		for (String p : echoSplit)
		{
			if (echoString.equals(""))
			{
				echoString = p;
			}
			else
			{
				echoString += "\"" + p;
			}
		}

		return echoString;
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

		ProcessBuilder pb	= new ProcessBuilder("sh", "-c", command);
		Process process		= pb.start();

		int pid = this.addProcessToProcessList(process);

		this.runningProcessId = pid;

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
		ExecMonitor execMonitor = null;

		try
		{
			Logger.log("Running process: " + this.process.getId());

			// Lets iterate through all files in input directory.
			File inputDir			= new File(this.process.getInputDirPath());
			File[] inputDirFiles	= inputDir.listFiles();

			// We throw exception if we cannot load the files.
			if (inputDirFiles == null)
			{
				throw new Exception("Cannot load input dir files (" + inputDir.getAbsolutePath() + ")");
			}

			// We will clean the output file only if we'll not skip any input file.
			if (this.processedFilesCount == 0)
			{
				this.cleanOutputDir(this.process.getOutputDirPath());
			}

			// Monitoring of the running processes.
			execMonitor = new ExecMonitor(this);
			execMonitor.start();

			for (int i = 0; i < inputDirFiles.length; i++)
			{
				// Break the cycle if the exec is stopped.
				if (!this.isRunning)
				{
					break;
				}

				// If we start the Exec with positive count of processed files,
				// we will skip those.
				if (i < this.processedFilesCount)
				{
					continue;
				}

				// Input file must have also input extension.
				if (inputDirFiles[i].getName().endsWith("." + this.process.getInputExt()))
				{
					execMonitor.reset(inputDirFiles[i]);

					this.runProcessOnFile(inputDirFiles[i]);
				}

				if (this.interrupted)
				{
					throw new ExecInteruptedException("Exec has been interupted by monitor.");
				}

				this.processedFilesCount++;
			}

			// Closing monitor.
			execMonitor.stop();

			if (this.isRunning)
			{
				Logger.log("Exec for process " + this.process.getId() + " finished.");
			}
			else
			{
				Logger.log("Exec for process " + this.process.getId() + " has been stopped.");
			}
			
			Logger.log("Process " + this.process.getId() + " children: " + this.getProcessChildrenIds());

			// If we can run children, we'll do it.
			if (this.manager.isRunning() && this.isRunning)
			{
				Logger.log("Running process " + this.process.getId() + "children.");
				
				this.processChildren();
			}
		}
		// Monitor interruption.
		catch (ExecInteruptedException e)
		{
			Logger.log("Exec for process " + this.process.getId() + " has been interrupted: " + e.getMessage());
		}
		catch (Exception e)
		{
			this.error = e.getMessage();

			Logger.logError("Script " + this.scriptPath + " finished with error: " + e.getMessage() + " (" + Arrays.toString(e.getStackTrace()) + ")");
		}
		finally
		{
			if (execMonitor != null)
			{
				execMonitor.stop();
			}

			if (this.isRunning)
			{
				this.stop();
			}
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


	/**
	 * Restarts the exec.
	 */
	public void restart()
	{
		this.interrupt();

		Exec clone = new Exec(this);

		this.manager.manageExecRestart(clone);

		clone.start();
	}


	/**
	 * Stops the exec.
	 */
	public void stop()
	{
		this.isRunning = false;

		this.manager.manageExecEnd(this);
	}


	/**
	 * Interrupts the running Exec.
	 */
	public void interrupt()
	{
		this.isRunning		= false;
		this.interrupted	= true;

		this.manager.manageExecInterruption(this);
	}


	/**
	 * Checks if the exec is running.
	 *
	 * @return
	 */
	public Boolean isRunning()
	{
		return this.isRunning && this.manager.isRunning();
	}
}