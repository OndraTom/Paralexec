package paralexec;

/**
 *
 * @author ODIS
 */
final public class ParalexecMonitor implements Runnable
{	
	private Paralexec paralexec;
	
	
	public ParalexecMonitor(Paralexec paralexec)
	{
		this.paralexec = paralexec;
	}
	

	@Override
	public void run()
	{
		while (this.paralexec.isRunning())
		{
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e) {}
		}
		
		this.paralexec.stopProcessing();
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
