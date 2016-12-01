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
