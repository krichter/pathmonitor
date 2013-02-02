import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
//import java.io.*;
import javax.mail.*;
import javax.mail.internet.*;

public class Log
{
	public String logfiledir = "";
	public BufferedWriter out = null;
	public boolean online = true;
	public FileWriter logwriter = null;
	public ThreadPing pinger = null;
	public String nodelabel = "";
	Properties mailconfig = new Properties();
	public Vector<String> l1mailto = new Vector<String>();
	public Vector<String> l2mailto = new Vector<String>();
	public int l2threshold = -1;
	public boolean l1mailerset = false;
	public boolean l2mailerset = false;
	public boolean mailerset = false;
	
	public Log(String setLogfiledir)
	{
		logfiledir = setLogfiledir;
	}

	public void setMailer(String setmailhost,String setl1mailto,String setl2mailto,int setl2threshold)
	{
		if (setmailhost != null && !setmailhost.equals(""))
		{
			mailconfig = new Properties();
			mailconfig.setProperty("mail.host", setmailhost);
			mailconfig.setProperty("mail.from", "kyle.richter@sap.com");
			mailerset = true;
		}
				
		if (mailerset && setl1mailto != null && !setl1mailto.equals(""))
		{
			
			l1mailto = new Vector<String>();
			String[] rawto = setl1mailto.split(",");
			for (int x=0; x < rawto.length;x++)
			{
				l1mailto.add(rawto[x]);
			}
			l1mailerset = true;
		}
		if (mailerset && setl2mailto != null && !setl2mailto.equals("") && setl2threshold >= 0)
		{
			//System.out.println("Setting up L2 mailer");
			l2threshold = setl2threshold;
			l2mailto = new Vector<String>();
			String[] rawto2 = setl2mailto.split(",");
			for (int x=0; x < rawto2.length;x++)
			{
				l2mailto.add(rawto2[x]);
			}
			l2mailerset = true;
		}		
	}
	
	public void ping(boolean start,int interval)
	{
		if (start)
		{
			logit("Log Ping Start","pingstart-"+interval,null);
		}
		else
		{
			logit("Log Ping","ping",null);
		}
	}
	
	public void logit(String message,String tag,MonitorStatus monitor)
	{
		try
		{
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
			DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd");
	        java.util.Date date = new java.util.Date();
			logwriter = new FileWriter(logfiledir+"monitor-"+dateFormat2.format(date)+".log",true);
			BufferedWriter out = new BufferedWriter(logwriter);
			String check = "";
			if (monitor != null)
			{
				check = monitor.label;
			}
			String finalmessage = "\""+dateFormat.format(date)+"\",\""+tag+"\",\""+check+"\",\"" + message + "\"\r\n";
			out.write(finalmessage);
			//System.out.print(finalmessage);
			out.flush();
			out.close();
			logwriter.close();
		}
		catch (Exception e)
		{
			System.out.println("Problem writing to log");
			e.printStackTrace();
		}
	}
	
	public void writeLog(PrintWriter wout,String logfile)
	{
		if (online)
		{
			try
			{
				wout.println("<TABLE border='0'>");
			    FileInputStream fstream = new FileInputStream(logfile);
			    DataInputStream logIn = new DataInputStream(fstream);
			    BufferedReader br = new BufferedReader(new InputStreamReader(logIn));

			    String strLine;
			    while ((strLine = br.readLine()) != null)
			    {
			    	String[] parts = strLine.split("\",\"");
			    	if (parts.length > 1)
			    	{
				    	//wout.print(strLine);
				    	if (parts[1].length() > 3 && parts[1].substring(0,4).equals("ping"))
				    	{
				    		
				    	}
				    	else
				    	{
					    	wout.print("<TR><TD>");
					    	wout.print(parts[0].replace("\"", ""));
					    	wout.print("</TD><TD>&nbsp;&nbsp;&nbsp;</TD><TD>");
					    	wout.print(parts[3].replace("\"", ""));
					    	wout.print("</TD></TR>");
					    	//wout.println("<BR>");
					    	wout.flush();
				    	}
			    	}
			    }
			    logIn.close();
			    fstream.close();
			    wout.println("</TABLE>");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public String[] getLogs()
	{
		Vector<String> logs = new Vector<String>();
		File dir = new File(logfiledir);
		String[] files = dir.list();
		if (files != null)
		{
			for (int x=0;x < files.length;x++)
			{
				if (files[x].length() > 8 && files[x].substring(0,8).equals("monitor-"))
				{
					logs.add(files[x]);
				}
			}
		}
		String[] result = (String[])logs.toArray(new String[0]);
		Arrays.sort(result);
		return result;
	}

	public void writeRawLog(PrintWriter wout,String logfile)
	{
		if (online)
		{
			try
			{
			    FileInputStream fstream = new FileInputStream(logfile);
			    DataInputStream logIn = new DataInputStream(fstream);
			    BufferedReader br = new BufferedReader(new InputStreamReader(logIn));
			    String strLine;
			    while ((strLine = br.readLine()) != null)
			    {
			    	wout.println (strLine);
			    	wout.flush();
			    }
			    logIn.close();
			    fstream.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void close()
	{
	}
	
	// Active level 1 alerting system. In short, attempt to send an email immediately if configured
	public void sendl1Alert(MonitorStatus status,String alertmessage,String[] l1addon)
	{
		try
		{
			//System.out.println("Sending L1 Alert for Node: "+nodelabel+", Check: "+status.label+", Error:"+alertmessage);
			
			if (mailerset && (l1mailerset || (l1addon != null && l1addon.length > 0)))
			{
				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
		        java.util.Date date = new java.util.Date();
				//Properties mailconfig = new Properties();
				//mailconfig.setProperty("mail.host", "mailhost");
				//mailconfig.setProperty("mail.from", "kyle.richter@sap.com");
				Session session = Session.getDefaultInstance(mailconfig, null );
				MimeMessage message = new MimeMessage(session);
				
				int tocount = 0;
				for (int i = 0;i < l1mailto.size(); i++)
				{
					message.addRecipient(Message.RecipientType.TO, new InternetAddress(l1mailto.get(i)));
					//System.out.println("Adding Mail Recip.-"+l1mailto.get(i)+"-");
					tocount++;
				}
				if (l1addon != null && l1addon.length > 0)
				{
					for (int x=0;x < l1addon.length; x++)
					{
						message.addRecipient(Message.RecipientType.TO, new InternetAddress(l1addon[x]));
						//System.out.println("Adding Extra L1 Mail Recip.-"+l1addon[x]+"-");
						tocount++;
					}
				}				
				
				if (tocount > 0)
				{
					message.setSubject("Path Monitor L1 Error on node: "+nodelabel+", check: "+status.label);
					message.setText("Path Monitor L1 Error Detected:\n\nNode: "+nodelabel+"\nCheck: "+status.label+"\nDate Stamp: "+dateFormat.format(date)+"\nError: "+alertmessage);
					Transport.send(message);
				}
				//System.out.println("Email sent");
			}
		}
		catch (Exception e)
		{
			logit("Failure to send L1 email","error",status);
			e.printStackTrace();
		}
	}
	
	// Active level 2 alerting system. In short, attempt to send an email immediately if configured
	public void sendl2Alert(MonitorStatus status,String alertmessage,String[] l2addon)
	{
		try
		{
			//System.out.println("Sending L2 Alert for Node: "+nodelabel+", Check: "+status.label+", Error:"+alertmessage);
			
			if (mailerset && (l2mailerset || (l2addon != null && l2addon.length > 0)))
			{
				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
		        java.util.Date date = new java.util.Date();

		        //Properties mailconfig = new Properties();
				//mailconfig.setProperty("mail.host", "mailhost");
				//mailconfig.setProperty("mail.from", "kyle.richter@sap.com");
				Session session = Session.getDefaultInstance(mailconfig, null );
				MimeMessage message = new MimeMessage(session);
				
				int tocount = 0;
				for (int i = 0;i < l2mailto.size(); i++)
				{
					message.addRecipient(Message.RecipientType.TO, new InternetAddress(l2mailto.get(i)));
					//System.out.println("Adding Mail Recip.-"+l2mailto.get(i)+"-");
					tocount++;
				}
				if (l2addon != null && l2addon.length>0)
				{
					for (int x=0;x < l2addon.length; x++)
					{
						message.addRecipient(Message.RecipientType.TO, new InternetAddress(l2addon[x]));
						//System.out.println("Adding Extra L2 Mail Recip.-"+l2addon[x]+"-");
						tocount++;
					}
				}
				
				if (tocount > 0)
				{
					message.setSubject("Path Monitor L2 Error on node: "+nodelabel+", check: "+status.label);
					message.setText("Path Monitor L2 Error Detected:\n\nNode: "+nodelabel+"\nCheck: "+status.label+"\nDate Stamp: "+dateFormat.format(date)+"\nError: "+alertmessage);
					Transport.send(message);
				}
				//System.out.println("Email sent");
			}
		}
		catch (Exception e)
		{
			logit("Failure to send L2 email","error",status);
			e.printStackTrace();
		}
	}
	
	public void sendResolution(MonitorStatus status,String alertmessage,boolean l1enabled,boolean l2enabled,String[] l1addon,String[] l2addon)
	{
		try
		{
			//System.out.println("Sending Resolution Node: "+nodelabel+", Check: "+status.label+", Error:"+alertmessage);

			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
	        java.util.Date date = new java.util.Date();
			Session session = Session.getDefaultInstance(mailconfig, null );
			MimeMessage message = new MimeMessage(session);
			message.setSubject("Path Monitor Resolution on node: "+nodelabel+", check: "+status.label);
			message.setText("Path Monitor Resolution:\n\nNode: "+nodelabel+"\nCheck: "+status.label+"\nDate Stamp: "+dateFormat.format(date)+"\nPrevious Error: "+alertmessage);

			int tocount = 0;
			if (mailerset && ((l1mailerset || l1addon.length > 0) && l1enabled))
			{
				for (int i = 0;i < l1mailto.size(); i++)
				{
					message.addRecipient(Message.RecipientType.TO, new InternetAddress(l1mailto.get(i)));
					//System.out.println("Adding Mail Recip.-"+l1mailto.get(i)+"-");
					tocount++;
				}
				if (l1addon != null && l1addon.length>0)
				{
					for (int x=0;x < l1addon.length; x++)
					{
						message.addRecipient(Message.RecipientType.TO, new InternetAddress(l1addon[x]));
						//System.out.println("Adding Extra L1 Mail Recip.-"+l1addon[x]+"-");
						tocount++;
					}
				}
			}
			if (mailerset && ((l2mailerset || l2addon.length > 0) && l2enabled))
			{
				for (int i = 0;i < l2mailto.size(); i++)
				{
					message.addRecipient(Message.RecipientType.TO, new InternetAddress(l2mailto.get(i)));
					//System.out.println("Adding Mail Recip.-"+l2mailto.get(i)+"-");
					tocount++;
				}
				if (l2addon != null && l2addon.length>0)
				{
					for (int x=0;x < l2addon.length; x++)
					{
						message.addRecipient(Message.RecipientType.TO, new InternetAddress(l2addon[x]));
						//System.out.println("Adding Extra L2 Mail Recip.-"+l2addon[x]+"-");
						tocount++;
					}
				}				
			}
			if (mailerset && ((l1mailerset || l2mailerset) && (l1enabled || l2enabled)) && tocount > 0)
			{
				Transport.send(message);
				//System.out.println("Email sent");
				
			}
		}
		catch (Exception e)
		{
			logit("Failure to send resolution email","error",status);
			e.printStackTrace();
		}
	}
}
