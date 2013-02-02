
import java.net.*;

public class PortMonitorThread implements Runnable, MonitorThread
{
	private String hostToMonitor = "";
	private int portToCheck = 0;
	private Log log = null;
	private int checkrate = 0;
	private int retryrate = 0;
	private boolean on = true;
	public Thread thisthread = null;
	private MonitorStatus status = null;
	private boolean lastcheck = true;
	//private boolean enabled = false;
	public boolean l1alerted = false;
	public boolean l2alerted = false;
	public long l1alertstamp = 0;
	private String[] l1addon;
	private String[] l2addon;
	private int l2thresholdoverride = -1;

	public PortMonitorThread(String host,int port,Log setLog,int setcheckrate,int setretryrate,String setlabel)
	{
		hostToMonitor = host;
		portToCheck = port;
		log = setLog;
		checkrate = setcheckrate;
		retryrate = setretryrate;
		
		status = new MonitorStatus();
		status.host = host + ":" + port;
		status.retryrate = Integer.toString(setretryrate);
		status.successrate = Integer.toString(setcheckrate);
		status.checktype = "Port Check";
		status.label = setlabel;
		status.enabled = false;
	}
	
	public void run()
	{
		//enabled = true;
		status.enabled = true;
		while (on)
		{
			boolean retrysleep = false;
			try
			{
				//Socket s = new Socket(hostToMonitor,portToCheck);
				Socket s = new Socket();
				s.setKeepAlive(false);
				s.setTcpNoDelay(true);
				s.setReuseAddress(true);
				s.connect(new InetSocketAddress(hostToMonitor,portToCheck),5000);
				s.close();
				status.passstamp();
				if (!lastcheck)
				{
					log.logit("Port connection ("+hostToMonitor+":"+portToCheck+") reestablished","resolution",status);
					//l1alerted = false;
					//l2alerted = false;
					resolve("Port connection ("+hostToMonitor+":"+portToCheck+") reestablished");
					lastcheck = true;
				}
			}
			catch (Exception e)
			{
				//System.out.println("Socket failure");
				log.logit("Port check failed ("+hostToMonitor+":"+portToCheck+")","failure",status);
				alert("Port check failed ("+hostToMonitor+":"+portToCheck+")");
				status.failstamp("Port check failed");
				lastcheck = false;
				try
				{
					retrysleep = true;
					Thread.sleep(retryrate * 1000);
				}
				catch (Exception e2)
				{
				}
			}
			try
			{
				if (!retrysleep)
				{
					Thread.sleep(checkrate * 1000);
				}
			}
			catch (Exception e)
			{
				
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
	
	public Thread getThisThread()
	{
		return thisthread;
	}
	
	public void regenerate()
	{
		on = true;
		Thread t = new Thread(this);
		this.thisthread = t;
		t.start();
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
		log.sendResolution(status, errorMessage, l1alerted, l2alerted,l1addon,l2addon);
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
