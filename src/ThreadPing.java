

public class ThreadPing implements Runnable
{
	private Log log;
	private boolean go = true;
	public Thread thisThread;
	private int sleepcount = 86400;
	private boolean init = true;
	
	public ThreadPing(Log setLog,boolean isreload)
	{
		log = setLog;
		if (isreload)
		{
			init = false;
		}
	}
	
	public void run()
	{
		while (true && go)
		{
			log.ping(init,sleepcount);
			if (init)
			{
				init = false;
			}
			try
			{
				Thread.sleep(sleepcount * 1000);
			}
			catch (Exception e)
			{
			}
		}
	}
	
	public void stopPing()
	{
		go = false;
		if (thisThread != null)
		{
			try
			{
				thisThread.interrupt();
				thisThread.join();
			}
			catch (Exception e)
			{
			}
		}
	}
}
