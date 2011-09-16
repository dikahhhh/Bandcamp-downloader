package dloader;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingWorker;

import java.awt.BorderLayout;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class GUI extends JFrame {
	public final class PageProcessorWorker extends
			SwingWorker<PageJob, PageJob> {
		// Worker thread
		@Override
		public PageJob doInBackground() {
			try {
				PageJob res = Main.sharedPageProcessor.doSingleJob();
				if (PageProcessor.hasMoreJobs()) {
					PageProcessorWorker worker = new PageProcessorWorker();
					worker.execute(); // next job GO
				}
				return res;
//				Main.sharedPageProcessor.hostWorker = this;
//				Main.sharedPageProcessor.acquireData();
			} catch (Throwable e) {
				Main.logger.log(Level.SEVERE, "", e);
			}
			return null;
		}

		// GUI Event Dispatch thread
		@Override
		protected void done() {
			try {
				updateTreeByJob(get());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		// GUI Event Dispatch thread
		@Override
		public void process(List<PageJob> pjList) {
			assert (pjList != null);

			while (!pjList.isEmpty()) {
				PageJob pj = pjList.remove(0);
				assert (pj != null);
				
				// remove all consequent reports of this job from current list (duplications)
				pjList.remove(pj);
				
				updateTreeByJob(pj);
			}
		}

		// worker thread
//		public void subPublish(PageJob pj) {
//			publish(pj);
//		}
	}
	
	Map<PageJob, DefaultMutableTreeNode> jobToTreenode; 
	JTree tree;
	
	public GUI() {
		//FIXME: replace with some weak references to PageJobs and deal with depleted nodes.
		jobToTreenode = new Hashtable<>();

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle("Bandcamp downloader");
		
		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		tree = new JTree();
		tree.setRootVisible(false);
		tree.setModel(new DefaultTreeModel(
			new DefaultMutableTreeNode("JTree") 
		));
		tree.setShowsRootHandles(true);
		scrollPane.setViewportView(tree);
		tree.setEditable(true);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.EAST);
		panel.setLayout(new BorderLayout(0, 0));
		
		JButton btnStart = new JButton("Start!");
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (PageProcessor.hasMoreJobs()) {
					PageProcessorWorker worker = new PageProcessorWorker();
					worker.execute(); // next job GO
				}				
			}
		});
		btnStart.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(btnStart, BorderLayout.NORTH);
	}

	@Override
	public void dispose() {
		// TODO: insert *interrupt workers and wait on them closing* code here.
		frame = null;
		EventDispatchThread = null;
		super.dispose();
	}

	private static final long serialVersionUID = -919422625610867342L;
	public static Thread EventDispatchThread = null;
	static GUI frame = null;
	static boolean showGUIWindow() {
		if (frame == null) {
			frame = new GUI();
			// FIXME: yeah, ugly coupling here;
			frame.updateTreeByJob(PageProcessor.getJobQ().get(0));
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//			PageProcessorWorker worker = frame.new PageProcessorWorker();
//			worker.execute();
		}
		frame.setVisible(true);
		
		return true;
	}
	
	public void updateTreeByJob(PageJob pj) {
		// FIXME: should've create DefaultTreeModel explicitly in the 1st place
		DefaultTreeModel tm = (DefaultTreeModel)tree.getModel();
		
		DefaultMutableTreeNode node = jobToTreenode.get(pj);
		if (node == null) {
			// no such node in a tree: ADDING
			//  check if there is a parent job in a tree;
			PageJob parentJob = PageProcessor.getJobForPage(pj.page.parent);
			DefaultMutableTreeNode parentNode = null; 
			String value = (pj.page.getTitle() != null)? pj.page.getTitle() : pj.page.url.toString();
			node = new DefaultMutableTreeNode(value, true); 
			if (parentJob == null) {
				//add new top element
				parentNode = (DefaultMutableTreeNode) tm.getRoot();
				tm.insertNodeInto(node, parentNode, parentNode.getChildCount());
				jobToTreenode.put(pj, node); // XXX: should be a weak reference;
				
				// expand root element to show this one.
				tree.expandPath(new TreePath(parentNode));
			} else {
				// add new leaf element
				
				// XXX: Strong assumption here - parent job is already in a tree (was at least recon'd)
				parentNode = jobToTreenode.get(parentJob);
				assert (parentNode != null);
				tm.insertNodeInto(node, parentNode, parentNode.getChildCount());
				jobToTreenode.put(pj, node); // XXX: should be a weak reference;
			}
		} else {
			// update element visuals
			node.setUserObject(pj.page.getTitle());
			tm.reload(node);
		}		
	}
}
