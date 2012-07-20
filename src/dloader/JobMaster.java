package dloader;

import java.util.concurrent.*;


import dloader.page.AbstractPage;
import dloader.pagejob.*;

/**
 * Class to initiate and maintain jobs' queue and intermediate result reporting
 * between jobs and GUI/caller.
 * 
 * After constructing, call goGoGo() to start actual working
 * @author Acerbic
 */
public class JobMaster {
	
	/**
	 * number of threads running in parallel
	 */
	static int CORETHREADS_NUMBER = 4; 
	
	/**
	 * Associated job executor
	 */
	private ExecutorService executor;
	
	/**
	 * All submitted jobs have their Future's here. For result reporting/error checking/job resubmitting... etc
	 */
	private ConcurrentLinkedQueue<Future<?>> results; 
	
	/**
	 * READCACHEPAGES - get pages tree ONLY from cache (prefetch)
	 * UPDATEPAGES - updated 1st page from the Internet and others too if needed (otherwise tries reading from cache)
	 *   			updates cache too
	 * UPDATEDATA - download missing/corrupt files.
	 * 
	 * It is vital that different kinds of jobs are not ran on the same page concurrently.
	 * @author Acerbic
	 */
	public enum JobType { READCACHEPAGES, UPDATEPAGES, UPDATEDATA};

	/**
	 *  i'm not sure about this. It is a temporary variable to the rootPage. 
	 *  i just did not want to start the jobs running within a constructor 
	 *  (also since creating jobs means sharing a reference to this JobMaster 
	 *    it is not wise to leak "this" reference before construction is finished) 
	 */
	private AbstractPage rootPage; 
	private JobType whatToDo; 
	
	/**
	 * Initiate JobMaster with rootJob (one which can generate and submit other jobs)
	 * @param whatToDo - job type
	 * @param rootPage - page to process 
	 */
	public
	JobMaster (JobType whatToDo, AbstractPage rootPage) {
		executor = Executors.newFixedThreadPool(CORETHREADS_NUMBER);
		assert (executor instanceof ThreadPoolExecutor);
		results = new ConcurrentLinkedQueue<Future<?>>();
		
		this.whatToDo = whatToDo;
		this.rootPage = rootPage; 
	}

	/**
	 * The main execution starter. All working is started with calling this function. May be called only once per JobMaster.
	 * subsequent call would return with no results.
	 */
	public
	void goGoGo() {
		
		synchronized (this) {
			// can be ran only once;
			if (rootPage == null) return;

			switch (whatToDo) {
			case READCACHEPAGES: submit(new ReadCacheJob(rootPage, this)); break;
			case UPDATEPAGES: submit(new DownloadPageJob(rootPage, this, false)); break;
			case UPDATEDATA: submit(new SaveDataJob(rootPage, this)); break;
			}
		}
		
		// wait until all the jobs are finished before terminating this thread.
		while (!results.isEmpty())
			try {
				results.poll().get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		executor.shutdown();
	}
	
	/**
	 * Entry point for Jobs to add more children jobs into a queue
	 * @param job - new job to be executed some moment in the future.
	 */
	synchronized public
	void submit (Runnable job) {
		results.add(executor.submit(job));
	}


	/**
	 * Entry point for Jobs to send reports on the progress/results.
	 * 
	 * This job master reports to GUI, but the method can be overloaded to support console or network, for example
	 * @param page - page is being (been) worked on.
	 * @param type - custom string argument
	 * @param report - custom integer argument
	 */
	public void report(AbstractPage page, String type, int report) {
		// TODO SEND message to GUI thread
		
	}
	
	

}