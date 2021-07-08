package git;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import data.Metrics;
import data.ProjectClass;
import utils.DateHandler;
import utils.Parameters;
import utils.PathHandler;

public class GitRelease {
		private Git git;
		private int id;				// id incrementale della release
		private GitCommit commit; 	// Riferimento al commit della release
		private String name;
		private Date date;
		private List<ProjectClass> classList;
		private boolean inconsistent;
		
		public GitRelease(Git git, GitCommit commit, String name, Date date) {
			this.git = git;
			this.commit = commit;
			this.name = name;
			this.date = date;
//			this.fetchClassList();
		}

		public boolean isInconsistent() {
			return inconsistent;
		}

		public void setInconsistent(boolean inconsistent) {
			this.inconsistent = inconsistent;
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
				Date commitDate = DateHandler.getDateFromEpoch(revCommit.getCommitTime() * 1000L);
				GitCommit classCommit = new GitCommit(revCommit.getId(), commitDate, revCommit.getFullMessage());
		
				treeWalk.reset(treeId);
				treeWalk.setRecursive(true);
				
				while (treeWalk.next()) {
					String classPath = treeWalk.getPathString();
				    if (classPath.contains(Parameters.FILTER_FILE_TYPE)) {
				    	String className = PathHandler.getNameFromPath(classPath);
				    	ProjectClass projectClass = new ProjectClass(classPath,className,this);	    
				    	projectClass.setCommit(classCommit);

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
			
			System.out.println(output);
//			System.out.println("Classi: " + this.classList.size());
			System.out.println("=".repeat(200));
		}
		
		
		/**
		 * [DEBUG] Stampa la lista delle classi della GitRelease
		 */
		public void printClassList() {
			System.out.println(this.name);
			System.out.println("=".repeat(200)+"\n");
			int count = 0;
			for (ProjectClass c:this.classList) {
				System.out.println(c.getPath() + " " + c.isBuggy());
				count++;
			}
			System.out.println(count);
			System.out.println("=".repeat(200)+"\n");
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
