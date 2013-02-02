
import java.net.*;

public class PingMonitorThread implements Runnable, MonitorThread
{
	private String hostToMonitor = "";
	private Log log = null;
	private int checkrate = 0;
	private int retryrate = 0;
	private boolean on = true;
	public Thread thisthread = null;
	private MonitorStatus status = null;
	private boolean lastcheck = true;
	public boolean l1alerted = false;
	public boolean l2alerted = false;
	public long l1alertstamp = 0;
	//private boolean enabled = false;
	private String[] l1addon;
	private String[] l2addon;
	private int l2thresholdoverride = -1;
	
	public PingMonitorThread(String host,Log setLog,int setcheckrate,int setretryrate,String setlabel)
	{
		hostToMonitor = host;
		log = setLog;
		checkrate = setcheckrate;
		retryrate = setretryrate;
		
		status = new MonitorStatus();
		status.host = host;
		status.retryrate = Integer.toString(setretryrate);
		status.successrate = Integer.toString(setcheckrate);
		status.checktype = "Ping";
		status.label = setlabel;
		status.enabled = false;
	}
	
	public void run()
	{
		status.enabled = true;
		//enabled = true;
		while (on)
		{
			try
			{
				InetAddress address = InetAddress.getByName(hostToMonitor);
				if (address.isReachable(5000))
				{
					status.passstamp();
					if (!lastcheck)
					{
						log.logit(hostToMonitor+" ping re-established","resolution",status);
						//l1alerted = false;
						//l2alerted = false;
						resolve(hostToMonitor+" ping re-established");
						lastcheck = true;
					}
					Thread.sleep(checkrate * 1000);
				}
				else
				{
					log.logit(hostToMonitor+" ping failed","failure",status);
					alert(hostToMonitor+" ping failed");
					status.failstamp(hostToMonitor+" ping failed");
					lastcheck = false;
					Thread.sleep(retryrate * 1000);
				}
			}
			catch (Exception e)
			{
				//e.printStackTrace();
				status.failstamp("Exception caught");
			}
		}
	}
	
	public void stopmonitor()
	{
		on = false;
		try
		{
			thisthread.interrupt();
			thisthread.join();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}		
	}

	public void disablemonitor()
	{
		status.enabled = false;
		//enabled = false;
		l1alerted = false;
		l2alerted = false;
		stopmonitor();
	}
	
	public void regenerate()
	{
		on = true;
		Thread t = new Thread(this);
		this.thisthread = t;
		t.start();
	}
	
	public Thread getThisThread()
	{
		return thisthread;
	}
	
	public MonitorStatus getStatus()
	{
		return status;
	}	
	
	public void alert(String errorMessage)
	{
		if (l1alerted)
		{
			long nowstamp = Math.round(Math.floor((new java.util.Date()).getTime() / 1000));
			//System.out.println((nowstamp - l1alertstamp) + " > " + log.l2threshold);
			
			if (l2thresholdoverride > -1 && (nowstamp - l1alertstamp) > l2thresholdoverride && !l2alerted)
			{
				l2alerted = true;
				log.sendl2Alert(status,errorMessage,l2addon);
			}
			else if ((nowstamp - l1alertstamp) > log.l2threshold && !l2alerted)
			{
				l2alerted = true;
				log.sendl2Alert(status,errorMessage,l2addon);
			}			
		}
		else
		{
			l1alerted = true;
			//System.out.println("Sending Alert");
			log.sendl1Alert(status,errorMessage,l1addon);
			l1alertstamp = Math.round(Math.floor((new java.util.Date()).getTime() / 1000));
		}
	}

	public void resolve(String errorMessage)
	{
		log.sendResolution(status, errorMessage, l1alerted, l2alerted, l1addon, l2addon);
		l1alerted = false;
		l2alerted = false;
	}
	
	public void setAlertOverride(int l2threshold,String[] l1mailto,String[] l2mailto)
	{
		if (l2threshold > -1 && l2mailto.length > 0)
		{
			l2thresholdoverride = l2threshold;
		}
		l1addon = l1mailto;
		l2addon = l2mailto;
	}	
}
