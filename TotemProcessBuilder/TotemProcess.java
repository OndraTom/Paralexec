package TotemProcessBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Totem process.
 * 
 * @author Ondřej Tom <info@ondratom.cz>
 */
public class TotemProcess implements Runnable
{
	/**
	 * Executing command parts.
	 */
	private String[] command;
	
	
	/**
	 * Process builder (caller).
	 */
	private TotemProcessBuilder builder;
	
	
	/**
	 * Process input lines.
	 */
	private List<String> input = new ArrayList<>();
	
	
	/**
	 * Process standard output lines.
	 */
	private List<String> standardOutput = new ArrayList<>();
	
	
	/**
	 * Process error output lines.
	 */
	private List<String> errorOutput = new ArrayList<>();
	
	
	/**
	 * Standard output file.
	 */
	private PrintWriter standardOutputFile = null;
	
	
	/**
	 * Error output file.
	 */
	private PrintWriter errorOutputFile = null;
	
	
	/**
	 * Executed process.
	 */
	private Process process = null;
	
	
	/**
	 * @param command Command for execution.
	 * @param builder The caller of the TotemProcess.
	 */
	public TotemProcess(String[] command, TotemProcessBuilder builder)
	{
		this.command = command;
		this.builder = builder;
	}
	
	
	/**
	 * Sets the process input.
	 * 
	 * @param input 
	 */
	public void setInput(List<String> input)
	{
		this.input = input;
	}
	
	
	/**
	 * Sets the standard output file.
	 * 
	 * @param standardOutputFilePath
	 * @throws IOException 
	 */
	public void setStandardOutputFile(String standardOutputFilePath) throws IOException
	{
		this.standardOutputFile = new PrintWriter(standardOutputFilePath, "UTF-8");
	}
	
	
	/**
	 * Sets the error output file.
	 * 
	 * @param errorOutputFilePath
	 * @throws IOException 
	 */
	public void setErrorOutputFile(String errorOutputFilePath) throws IOException
	{
		this.errorOutputFile = new PrintWriter(errorOutputFilePath, "UTF-8");
	}
	
	
	/**
	 * @return Standard output.
	 */
	public List<String> getStandardOutput()
	{
		return this.standardOutput;
	}
	
	
	/**
	 * @return Error output.
	 */
	public List<String> getErrorOutput()
	{
		return this.errorOutput;
	}
	
	
	@Override
	public void run()
	{
		ProcessBuilder processBuilder = new ProcessBuilder(this.command);
		
		try
		{
			this.process = processBuilder.start();
			
			// Write standard output if the input has been provided.
			if (!this.input.isEmpty())
			{
				this.writeProcessInput();
			}
			
			this.prepareOutputs();
			this.process.waitFor();
			this.closeFiles();
			
			// Contact builder about process end - continue signal.
			this.builder.processNextProgramCall();
		}
		catch (Exception e)
		{
			
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
	 * Stops the process and closes all open files.
	 */
	public void stop()
	{
		this.process.destroy();
		this.closeFiles();
	}
	
	
	/**
	 * Writes process input to the output stream (that's where process input should be).
	 * 
	 * @throws IOException 
	 */
	private void writeProcessInput() throws IOException
	{
		BufferedWriter standardOutputWriter = new BufferedWriter(new OutputStreamWriter(this.process.getOutputStream()));
				
		for (String inputLine : this.input)
		{
			standardOutputWriter.write(inputLine + "\n");
		}

		standardOutputWriter.flush();
		standardOutputWriter.close();
	}
	
	
	/**
	 * Prepares the process output saving.
	 * 
	 * @throws IOException 
	 */
	private void prepareOutputs() throws IOException
	{
		BufferedReader standardOutputReader = new BufferedReader(new InputStreamReader(this.process.getInputStream()));
		BufferedReader errorOutputReader	= new BufferedReader(new InputStreamReader(this.process.getErrorStream()));

		String line;

		while ((line = standardOutputReader.readLine()) != null)
		{
			this.saveStandardOutputLine(line);
		}

		while ((line = errorOutputReader.readLine()) != null)
		{
			this.saveErrorOutputLine(line);
		}
	}
	
	
	/**
	 * Saves standard output line.
	 * 
	 * @param line 
	 */
	private void saveStandardOutputLine(String line)
	{
		// Saves it into the file, if provided.
		if (this.standardOutputFile != null)
		{
			this.standardOutputFile.println(line);
		}
		// Otherwise saves it to the memory.
		else
		{
			this.standardOutput.add(line);
		}
	}
	
	
	/**
	 * Saves error output line.
	 * 
	 * @param line 
	 */
	private void saveErrorOutputLine(String line)
	{
		// Saves it into the file, if provided.
		if (this.errorOutputFile != null)
		{
			this.errorOutputFile.println(line);
		}
		// Otherwise saves it to the memory.
		else
		{
			this.errorOutput.add(line);
		}
	}
	
	
	/**
	 * Closes all open files.
	 */
	private void closeFiles()
	{
		if (this.standardOutputFile != null)
		{
			this.standardOutputFile.close();
		}
		
		if (this.errorOutputFile != null)
		{
			this.errorOutputFile.close();
		}
	}
}
