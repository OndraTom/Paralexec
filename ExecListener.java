package paralexec;

/**
 * Execution threads listener.
 *
 * Manages informations about start and end of thread.
 *
 * @author oto
 */
interface ExecListener
{
	/**
	 * Manages the thread start.
	 */
	public void manageThreadStart();


	/**
	 * Manages the thread end.
	 */
	public void manageThreadEnd();
}