package edu.sunypoly.cypher.backend.service;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ProgCompSubmissionHandler implements Runnable
{
	public ProgCompSubmissionHandler() 
	{
		subs = new LinkedBlockingQueue<Runnable>();
		int Cores = Runtime.getRuntime().availableProcessors();
		
		executor = new ThreadPoolExecutor(Cores, Cores*2, 10, TimeUnit.SECONDS, ProgCompSubmissionHandler.subs);	
	}
	
	public ProgCompSubmission addSubmission(ProgCompSubmission sub)
	{
		ProgCompSubmissionHandler.subs.offer(sub);
		while(sub.threadWait) 
		{
			
		}
		sub.threadWait = true;
		
		return sub;
	}
	
	public void run() 
	{
		while(true)
		{
			if(!subs.isEmpty()) 
			{
				try
				{
					executor.execute(subs.take());
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	
	private ThreadPoolExecutor executor;
	private static BlockingQueue<Runnable> subs;	
}
