
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
//import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.mina.transport.socket.SocketConnector;

public class HeartbeatMonitorThread implements Runnable, MonitorThread
{
	private String hostToMonitor = "";
	private int portToCheck = 0;
	private Log log = null;
	private int checkrate = 0;
	private int retryrate = 0;
	private boolean on = true;
	public Thread thisthread = null;
	public SocketConnector connector = null;
	public MonitorStatus status = null;
	public boolean lastcheck = true;
	public boolean enabled = false;
	public boolean l1alerted = false;
	public boolean l2alerted = false;
	public long l1alertstamp = 0;
	private String[] l1addon;
	private String[] l2addon;
	private int l2thresholdoverride = -1;
	
	public HeartbeatMonitorThread(String host,int port,Log setLog,int setcheckrate,int setretryrate,String setlabel)
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
		status.checktype = "Heartbeat";
		status.label = setlabel;
		status.enabled = false;
	}
	
	public void run()
	{
		enabled = true;
		status.enabled = true;
		while (on)
		{
			IoSession session = null;
			boolean connected = false;
			
			while (!connected && on)
			{
				try
				{
					connector = new NioSocketConnector();
					//connector.setConnectTimeout(10);
					connector.setConnectTimeoutMillis(10000);
			        //connector.getFilterChain().addLast("logger", new LoggingFilter());
			        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
					connector.setDefaultRemoteAddress(new InetSocketAddress(hostToMonitor,portToCheck));
					connector.setHandler(new CheckHandler(log,status,this));
					ConnectFuture cf = connector.connect();
					cf.awaitUninterruptibly();
					session = cf.getSession();
					connected = true;
					log.logit("Heartbeat Connection to "+hostToMonitor+":"+portToCheck+" opened","resolution",status);
					resolve("Heartbeat Connection to "+hostToMonitor+":"+portToCheck+" opened");
					status.passstamp();
				}
				catch (Exception e)
				{
					//System.out.println("Problem connecting to server");
					log.logit("Initial heartbeat connection to "+hostToMonitor+":"+portToCheck+" failed","failure",status);
					alert("Initial heartbeat connection to "+hostToMonitor+":"+portToCheck+" failed");
					status.failstamp("Initial heartbeat connection failed");
					try
					{
						Thread.sleep(retryrate * 1000);
						//break;
					}
					catch (Exception e2)
					{
					}
					connector.dispose();
				}
			}
			
			try
			{
				while (true && connected && on)
				{
					try
					{
						//System.out.println(hostToMonitor + ": Check!");
						session.write("Test");
						if (!lastcheck)
						{
							log.logit("Heartbeat Connection reconnected","resolution",status);
							resolve("Heartbeat Connection reconnected");
							lastcheck = true;
						}
						status.passstamp();
					}
					catch (Exception e2)
					{
						System.out.println("Cause exception in checker");
						session.close();
						e2.printStackTrace();
						connector.dispose();
					}
					Thread.sleep(checkrate * 1000);
					
					if (session.isClosing() || !session.isConnected())
					{
						connected = false;
						connector.dispose();
					}
				}
			}
			catch (Exception e)
			{
				//e.printStackTrace();
				session.close();
				connected = false;
				connector.dispose();
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
		try
		{
			connector.dispose();
		}
		catch (Exception e)
		{
		}
	}
	
	public void disablemonitor()
	{
		status.enabled = false;
		enabled = false;
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
			//System.out.println(l1alertstamp);
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
