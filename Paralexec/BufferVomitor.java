package paralexec;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Buffer vomitor.
 *
 * Vomitor is cleaning the executed processes outputs.
 * This is prevention against the OS buffer overflow.
 * 
 * It can be also set as a stream collector with saveOutput() called.
 *
 * @author oto
 */
final public class BufferVomitor implements Runnable
{
	/**
	 * Given stream name.
	 */
	private String streamName;


	/**
	 * Given stream - the vomit subject.
	 */
	private InputStream stream;


	/**
	 * Vomitor thread.
	 */
	private Thread thread;
	
	
	/**
	 * Saved output.
	 */
	private String output = "";
	
	
	/**
	 * Vomitor will save output if this property is TRUE.
	 */
	private Boolean saveOutput = false;


	/**
	 * Getting stream through constructor.
	 *
	 * @param streamName
	 * @param stream
	 */
	public BufferVomitor(String streamName, InputStream stream)
	{
		this.streamName = streamName;
		this.stream		= stream;
	}
	
	
	/**
	 * Switch the Vomitor as a colletor.
	 */
	public void saveOutput()
	{
		this.saveOutput = true;
	}
	
	
	/**
	 * It will vomit all!
	 */
	public void trashOutput()
	{
		this.saveOutput = false;
	}
	
	
	/**
	 * @return Collected output.
	 */
	public String getOutput()
	{
		return this.output;
	}


	/**
	 * The thread creation.
	 */
	public void start()
	{
		this.thread = new Thread(this);
		this.thread.start();
	}


	@Override
	public void run()
	{
		try
		{
			InputStreamReader inputReader	= new InputStreamReader(this.stream);
			BufferedReader bufferReader		= new BufferedReader(inputReader);

			// Cleaning buffer in loop.
			while (true)
			{
				String s = bufferReader.readLine();
				
				if (this.saveOutput)
				{
					this.output += s;
				}

				if (s == null)
				{
					break;
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Stream " + this.streamName + " vomitor problem: " + e.getMessage());
		}
	}
}
