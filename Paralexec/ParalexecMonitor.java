package paralexec;

/**
 * Paralexec monitor.
 *
 * Monitor is permanent checker of the Paralexec condition.
 *
 * @author oto
 */
final public class ParalexecMonitor implements Runnable
{
	/**
	 * Paralexec max restarts count.
	 */
	private static int PARALEXEC_RESTARTS_MAX_COUNT = 3;
	
	
	/**
	 * Paralexec minimal running time.
	 */
	private static long PARALEXEC_UNCHANGED_STATE_MAX_TIME = 1000 * 3600 * 2; // 2h
	
	
	/**
	 * Mister Paralexec.
	 */
	private Paralexec paralexec;
	
	
	/**
	 * Paralexec restarts count.
	 */
	private int paralexecRestarts = 0;


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
			if (this.paralexecRestarts <= PARALEXEC_RESTARTS_MAX_COUNT && this.isParalexecStucked())
			{
				Logger.log("Paralexec seems to be stucked. Restarting it.");
				
				try
				{
					this.paralexec.restart(false);
				}
				catch (Exception e)
				{
					Logger.log("Cannot restart Paralexec: " + e.getMessage());
					Logger.log("Stopping Paralexec.");
					
					this.paralexec.stopProcessing();
					return;
				}
				
				this.paralexecRestarts++;
			}
			
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e) {}
		}
		
		Logger.log("Running file disappeared. Stopping Paralexec.");

		this.paralexec.stopProcessing();
	}
	
	
	/**
	 * Checks if the Paralexec is stucked. 
	 *
	 * @return 
	 */
	private boolean isParalexecStucked()
	{	
		return this.paralexec.getLastChangeTime() > PARALEXEC_UNCHANGED_STATE_MAX_TIME;
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
