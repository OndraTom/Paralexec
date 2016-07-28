package paralexec;

/**
 * Execution threads manager.
 *
 * Manages starts and ends of the Execs.
 *
 * @author oto
 */
interface ExecManager
{
	/**
	 * Manages the exec start.
	 */
	public void manageExecStart(Exec exec);


	/**
	 * Manages the exec end.
	 */
	public void manageExecEnd();
}