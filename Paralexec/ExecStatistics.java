package paralexec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author oto
 */
final public class ExecStatistics
{
	/**
	 * <ProcessSettingId => <file1runRate, file2runRate, ...>>
	 */
	private static Map<Integer, List> statistics = new HashMap<>();
	
	
	/**
	 * Checks if give process setting is registered in statistics.
	 * 
	 * @param processSettingId
	 * @return 
	 */
	public static Boolean isProcessRegistered(int processSettingId)
	{
		return statistics.containsKey(processSettingId);
	}
	
	
	/**
	 * Adds new process setting file rate.
	 * 
	 * @param processSettingId
	 * @param fileRate 
	 */
	public static void addProcessFileRate(int processSettingId, long fileRate)
	{	
		if (!isProcessRegistered(processSettingId))
		{
			List<Long> fileRates = new ArrayList<>();
			
			fileRates.add(fileRate);
			
			statistics.put(processSettingId, fileRates);
		}
		else
		{
			statistics.get(processSettingId).add(fileRate);
		}
	}
	
	
	/**
	 * @param	processSettingId
	 * @return	Process setting file average rate.
	 * @throws	ExecStatisticsException 
	 */
	public static long getProcessFileAvarageRate(int processSettingId) throws ExecStatisticsException
	{
		if (!isProcessRegistered(processSettingId))
		{
			throw new ExecStatisticsException("Cannot get file avarage rate from non-existing process.");
		}
		
		int i				= 0;
		long avarageRate	= 0;
		
		for (Object rate : statistics.get(processSettingId))
		{
			avarageRate += (Long) rate;
			i++;
		}
		
		if (i == 0)
		{
			throw new ExecStatisticsException("Cannot get file avarage from empty avarages list.");
		}
		
		return avarageRate / i;
	}
	
	
	/**
	 * Deletes given process setting from the statistics.
	 * 
	 * @param processSettingId 
	 */
	public static void deleteProcess(int processSettingId)
	{
		if (isProcessRegistered(processSettingId))
		{
			statistics.remove(processSettingId);
		}
	}
}
