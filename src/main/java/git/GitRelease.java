package git;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import data.Metrics;
import data.ProjectClass;
import utils.Parameters;
import utils.PathHandler;

public class GitRelease {
		static Logger logger = Logger.getLogger(GitRelease.class.getName());
	
		private Git git;
		private int id;				// id incrementale della release
		private GitCommit commit; 	// Riferimento al commit della release
		private String name;
		private Date date;
		private List<ProjectClass> classList;
		
		public GitRelease(Git git, GitCommit commit, String name, Date date) {
			this.git = git;
			this.commit = commit;
			this.name = name;
			this.date = date;
		}

		/**
		 * Ritorna l'oggetto ProjectClass tramite il suo path
		 */
		public ProjectClass getProjectClass(String path) {
			for (ProjectClass p:this.classList) {
				if (p.getPath().equals(path)) {
					return p;
				}
			}
			return null;
		}
		
		
		/**
		 * Cerca tutte le classi presenti su Git al momento della release ed effettua il
		 * set della lista nel parametro di classe classList
		 */
		public void fetchClassList(){
			this.classList = new ArrayList<>();
			
	    	ObjectId objectId = null;
			
	    	ObjectReader reader = git.getRepository().newObjectReader();
			RevWalk revWalk = new RevWalk(git.getRepository()); 
			TreeWalk treeWalk = new TreeWalk(git.getRepository()); 
			
			ObjectId commitId = this.getCommit().getId();
			
			try {
				RevCommit revCommit = revWalk.parseCommit(commitId);
				ObjectId treeId = revCommit.getTree();
		
				treeWalk.reset(treeId);
				treeWalk.setRecursive(true);
				
				while (treeWalk.next()) {
					String classPath = treeWalk.getPathString();
				    if (classPath.contains(Parameters.FILTER_FILE_TYPE)) {
				    	String className = PathHandler.getNameFromPath(classPath);
				    	ProjectClass projectClass = new ProjectClass(classPath,className,this);	    

				    	objectId = treeWalk.getObjectId(0);
				    	
				    	// Calcolo e setto la size della classe
				    	Metrics metrics = new Metrics();
				    	metrics.calculateSize(objectId, reader);
				    	projectClass.setMetrics(metrics);
				    	classList.add(projectClass);
				    }  	    	
				}	   
			} catch (Exception e){
				e.printStackTrace();
			} finally {
				reader.close();
				revWalk.dispose();
			}
			this.setClassList(classList);	
		}
		
		
		/**
		 * [DEBUG] Stampa le informazioni sulla GitRelease
		 */
		public void print() {
			String output = String.format("ID: %s%nName: %s%ndate: %s%nCommit: %s", this.id, this.name,this.date, this.commit.getId());
			
			logger.log(Level.INFO,output);
		}
		
		/*===============================================================================================
		 * Getters & Setters
		 */
		
		public void setClassList(List<ProjectClass> classList) {
			this.classList = classList;
		}
		
		public List<ProjectClass> getClassList() {
			return this.classList;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}
		
		public GitCommit getCommit() {
			return this.commit;
		}
		
		public void setCommit(GitCommit commit) {
			this.commit = commit;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}
}
