package paralexec;

import java.io.File;

/**
 * Exec monitor is monitoring the Exec activity.
 * 
 * It will restart the Exec if it's not working.
 * 
 * @author oto
 */
final public class ExecMonitor implements Runnable
{
	/**
	 * Life cycle timeout.
	 */
	private static int LOOP_TIMEOUT;
	
	
	/**
	 * File rate reserve multiple.
	 */
	private static int FILE_RATE_RESERVE_MULTIPLE = 3;
	
	
	/**
	 * Monitored Exec.
	 */
	private Exec exec;
	
	
	/**
	 * Flag for monitor run.
	 * 
	 * If it's FALSE, the monitor will end its life cycle.
	 */
	private Boolean isRunning = true;
	
	
	/**
	 * Exec file start time.
	 */
	private long startTime;
	
	
	/**
	 * Suspicious running time without output file change.
	 */
	private long suspiciousRunningTime = 60 * 60; // 60 minutes
	
	
	/**
	 * Minimal allowed running time without output file change.
	 */
	private long minimalAllowedRunningTime = 60 * 1; // 1 minute
	
	
	/**
	 * Exec running attempts.
	 */
	private int execAttempts = 1;
	
	
	/**
	 * Exec current processing input file.
	 */
	private File monitoredFile = null;
	
	
	/**
	 * Exec current processing input file size (in KB).
	 */
	private long monitoredFileSize = 0;
	
	
	/**
	 * Calculated estimate of finish time.
	 */
	private long presumedMaxFinishedTime = 0;
	
	
	/**
	 * Exec output directory monitor.
	 */
	private DirectoryMonitor outputDirectoryMonitor;
	
	
	/**
	 * Constructor - getting Exec and resetting the state.
	 * 
	 * @param exec 
	 */
	public ExecMonitor(Exec exec)
	{
		this.exec					= exec;
		this.outputDirectoryMonitor = new DirectoryMonitor(
				new File(this.exec.getProcess().getOutputDirPath())
		);
		
		this.reset();
	}
	
	
	@Override
	public void run()
	{
		while (this.isRunning)
		{
			try
			{
				Thread.sleep(LOOP_TIMEOUT);
			}
			catch (InterruptedException e) {}
			
			// If data are changing, we ceep continue.
			if (this.outputDirectoryMonitor.hasDirectoryChanged())
			{
				continue;
			}
			
			// Restart the Exec if it's stucked.
			if (this.isExecOverTime())
			{
				Logger.log("Process setting " + this.getProcessSettingId() + " is stucked. Paralexec is restarting it.");
				
				Exec execClone = new Exec(this.exec);
				
				this.exec.interrupt();
				this.exec = execClone;
				this.execAttempts++;
				
				this.exec.start();
				
				this.reset();
			}
		}
	}
	
	
	/**
	 * @return Exec process setting ID.
	 */
	private int getProcessSettingId()
	{
		return this.exec.getProcess().getId();
	}
	
	
	/**
	 * Checks if the Exec is processing the current file over maximum allowed time.
	 * 
	 * @return 
	 */
	private Boolean isExecOverTime()
	{
		// - If the process is not registered in statistics, it means that it's
		// first processing file. The first time in row has no execution limits.
		// - The script can run at least for the minimal allowed running time.
		if (!ExecStatistics.isProcessRegistered(this.getProcessSettingId()) || this.getRunningTime() < this.minimalAllowedRunningTime)
		{
			return false;
		}
		
		// Return TRUE if the running funning time is bigger then presumed finish time.
		return this.getRunningTime() > this.presumedMaxFinishedTime;
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
	 * Resets the time of Exec processing.
	 */
	private void resetTime()
	{
		this.startTime = System.currentTimeMillis();
	}
	
	
	/**
	 * @return Running time in seconds.
	 */
	private long getRunningTime()
	{
		return (System.currentTimeMillis() - this.startTime) / 1000;
	}
	
	
	/**
	 * @return Presumed finish time of the running process (in seconds).
	 */
	private long getPresumedFinishTime()
	{
		if (!ExecStatistics.isProcessRegistered(this.getProcessSettingId()))
		{
			try
			{
				long fileAvarageRate	= ExecStatistics.getProcessFileAvarageRate(this.getProcessSettingId());
				long fileRate			= fileAvarageRate * FILE_RATE_RESERVE_MULTIPLE;
				long finishTime			= fileRate * this.monitoredFileSize;

				Logger.log("Calculated finish time for process setting " + this.getProcessSettingId() + " is " + finishTime);
				
				return finishTime;
			}
			catch (ExecStatisticsException e) {}
		}
		
		return 0;
	}
	
	
	/**
	 * Completely resets the state.
	 */
	public void reset()
	{
		this.resetTime();
		
		this.monitoredFile				= null;
		this.monitoredFileSize			= 0;
		this.presumedMaxFinishedTime	= 0;
	}
	
	
	/**
	 * Resets the state and sets new Exec current file.
	 * 
	 * @param	newMonitoredFile 
	 * @throws	ExecMonitorException 
	 */
	public void reset(File newMonitoredFile) throws ExecMonitorException
	{
		// Save the last completed file fate.
		if (this.monitoredFileSize > 0)
		{
			double runningTime	= this.getRunningTime();
			long fileRate		= (long) Math.ceil(runningTime / this.monitoredFileSize);
			
			Logger.log("Adding process setting " + this.getProcessSettingId() 
					+ " file rate: " + fileRate 
					+ " (running time: " + runningTime + ", file size: " + this.monitoredFileSize + ")");
			
			ExecStatistics.addProcessFileRate(this.getProcessSettingId(), fileRate);
		}
		
		long fileSize = newMonitoredFile.length() / 1024;
		
		if (fileSize == 0)
		{
			throw new ExecMonitorException("Monitored Exec input file " + newMonitoredFile.getAbsolutePath() + " is empty.");
		}
		
		this.reset();
		
		this.monitoredFile				= newMonitoredFile;
		this.monitoredFileSize			= fileSize;
		this.presumedMaxFinishedTime	= this.getPresumedFinishTime();
	}
	
	
	/**
	 * Ends the monitor.
	 */
	public void end()
	{
		this.isRunning = false;
		
		ExecStatistics.deleteProcess(this.getProcessSettingId());
	}
}