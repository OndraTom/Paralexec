package paralexec;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author oto
 */
final public class BufferVomitor implements Runnable
{
	private String streamName;


	private InputStream stream;


	private Thread thread;


	public BufferVomitor(String streamName, InputStream stream)
	{
		this.streamName = streamName;
		this.stream		= stream;
	}


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
