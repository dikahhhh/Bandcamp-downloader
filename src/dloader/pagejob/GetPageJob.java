package dloader.pagejob;

import dloader.page.AbstractPage;

/**
 * Job to read page data from cache and if cache is unavailable - download. 
 * Starts the same jobs for children nodes.
 * 
 * NOTE the "package-default" visibility on class, as it is used only be other class - UpdatePageJob.
 * @author Acerbic
 *
 */
class GetPageJob extends PageJob {

	public GetPageJob(AbstractPage page, JobMaster owner) {
		super(page, owner);
	}

	/**
	 * summary of the messages reported by GetPageJob:
	 * "checking cache", 1
	 * "read from cache", 1
	 * "cache reading failed, submitting download job", 1
	 */
	@Override
	public void run() {
		try {
			report ("checking cache", 1);
			if (page.loadFromCache() && page.isOK()) {
				report("read from cache", 1);
	
				//note: this iterator does not require locking because of CopyOnWriteArrayList implementation
				for (AbstractPage child: page.childPages)
					jobMaster.submit(new GetPageJob(child,jobMaster));
			} else {
				report("cache reading failed, submitting download job", 1);
				jobMaster.submit(new UpdatePageJob(page,jobMaster, false));
			}
		} catch (InterruptedException e) {
		}
		
	}
}
