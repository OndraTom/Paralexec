package paralexec;

/**
 * Paralexec monitor.
 *
 * Monitor is permanent checker of the running file.
 * When running file disappear, the monitor will tell Paralexec.
 *
 * @author oto
 */
final public class ParalexecMonitor implements Runnable
{
	/**
	 * Mister Paralexec.
	 */
	private Paralexec paralexec;


	/**
	 * Getting mister Paralexec through construnctor.
	 *
	 * @param paralexec
	 */
	public ParalexecMonitor(Paralexec paralexec)
	{
		this.paralexec = paralexec;
	}


	@Override
	public void run()
	{
		// Loop checking of the running file.
		while (this.paralexec.isRunning())
		{
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e) {}
		}

		Logger.log("Running file disappeared. Stopping Paralexec.");

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
