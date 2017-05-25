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
	 * Maximum attempts.
	 *
	 * No restart after that.
	 */
	private static int MAX_ATTEMPTS_COUNT = 3;


	/**
	 * Maximum presumed finish time (in seconds).
	 */
	private static int MAX_PRESUMED_FINISH_TIME = 60 * 60 * 1; // 1 hour
	
	
	/**
	 * Minimal allowed running time without output file change.
	 */
	private static int MINIMAL_ALLOWED_RUNNING_TIME = 30; // 30 seconds


	/**
	 * Life cycle timeout (miliseconds).
	 */
	private static int LOOP_TIMEOUT = 1000;


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
	private long presumedMaxFinishedTime = MAX_PRESUMED_FINISH_TIME;


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
		this.startTime				= System.currentTimeMillis();
	}


	@Override
	public void run()
	{
		// We will not block the exec which across the maximum number of attempts.
		if (this.exec.getAttemptsCount() <= MAX_ATTEMPTS_COUNT)
		{
			while (this.isRunning())
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

					this.exec.restart();

					break;
				}
			}
		}
		else
		{
			Logger.log("Process " + this.exec.getProcess().getId() + " accrossed maximum number of restarts (" + MAX_ATTEMPTS_COUNT + ").");
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
		// The script can run at least for the minimal allowed running time.
		if (this.getRunningTime() < MINIMAL_ALLOWED_RUNNING_TIME)
		{
			return false;
		}

		//Logger.log("checking running time: " + this.getRunningTime() + " > " + this.presumedMaxFinishedTime);

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
		if (ExecStatistics.isProcessRegistered(this.getProcessSettingId()))
		{
			try
			{
				double fileAvarageRate	= ExecStatistics.getProcessFileAvarageRate(this.getProcessSettingId());
				long fileRate			= (long) Math.ceil(fileAvarageRate * FILE_RATE_RESERVE_MULTIPLE);
				long finishTime			= fileRate * this.monitoredFileSize;

				Logger.log("Calculated finish time for process setting " + this.getProcessSettingId() + " is " + finishTime);

				return finishTime;
			}
			catch (ExecStatisticsException e) {}
		}

		return MAX_PRESUMED_FINISH_TIME;
	}


	/**
	 * Resets the state and sets new Exec current file.
	 *
	 * @param	newMonitoredFile
	 * @throws	ExecMonitorException
	 */
	public void reset(File newMonitoredFile) throws ExecMonitorException
	{
		// If this is not first reset then save the last completed file rate.
		if (this.monitoredFile != null && this.monitoredFileSize > 0)
		{
			Logger.log("last monitored file size = " + this.monitoredFileSize);
			
			double runningTime	= this.getRunningTime();
			double fileRate		= runningTime / this.monitoredFileSize;

			Logger.log("Adding process setting " + this.getProcessSettingId()
					+ " file rate: " + fileRate
					+ " (running time: " + runningTime + ", file size: " + this.monitoredFileSize + ")");

			ExecStatistics.addProcessFileRate(this.getProcessSettingId(), fileRate);
		}

		this.monitoredFile	= newMonitoredFile;
		long fileSize		= newMonitoredFile.length() / 1024;

		if (fileSize == 0)
		{
			Logger.logError("Monitored Exec input file " + newMonitoredFile.getAbsolutePath() + " is empty.");
		}
		else
		{
			this.monitoredFileSize			= fileSize;
			this.presumedMaxFinishedTime	= this.getPresumedFinishTime();
		}

		this.resetTime();
	}


	/**
	 * Ends the monitor.
	 */
	public void stop()
	{
		this.isRunning = false;

		ExecStatistics.deleteProcess(this.getProcessSettingId());
	}


	/**
	 * Checks if it's running.
	 *
	 * @return
	 */
	public Boolean isRunning()
	{
		return this.isRunning && this.exec.isRunning();
	}
}