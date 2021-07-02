package git;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;

import utils.Parameters;

public class GitDiff {
	DiffEntry diff;
	String content;
	String type;
	String path;
	List<String> renamePaths;
	
	
	public GitDiff (DiffEntry diff) {
		this.diff = diff;
		this.content = diff.toString();
		parseDiffType();
		if (isRename()) {
			parseRename();
		}
		else {
			parseDiffPath();
		}
	}
	
	
	/**
	 * Imposta il vecchio ed il nuovo path di GitDiff per un Diff di tipo RENAME
	 */
	public void parseRename() {
		String separator = "->";
		String strDiff = diff.toString();
		int start1 = strDiff.indexOf(Parameters.RENAME) + Parameters.RENAME.length() +1;
		int end1 = strDiff.lastIndexOf(separator);
		String path1 = strDiff.substring(start1,end1);
		int start2 = strDiff.indexOf(separator) + separator.length();
		int end2 = strDiff.lastIndexOf("]");
		String path2 = strDiff.substring(start2,end2);
		
		List<String> paths = new ArrayList<>(Arrays.asList(path1,path2));
		this.setRenamePaths(paths);
	}
	
	
	/**
	 * Imposta il Path del GitDiff in base al contenuto del Diff
	 */
	public void parseDiffPath() {
		if (isModify()) {
			String strDiff = diff.toString();
			int start = strDiff.indexOf(Parameters.MODIFY) + Parameters.MODIFY.length() +1;
			int end = strDiff.lastIndexOf("]");
			this.setPath(strDiff.substring(start,end));
		}
		else if (isAdd()) {
			String strDiff = diff.toString();
			int start = strDiff.indexOf(Parameters.ADD) + Parameters.ADD.length() +1;
			int end = strDiff.lastIndexOf("]");
			this.setPath(strDiff.substring(start,end));
		}
		else if (isDelete()) {
			String strDiff = diff.toString();
			int start = strDiff.indexOf(Parameters.DELETE) + Parameters.DELETE.length() +1;
			int end = strDiff.lastIndexOf("]");
			this.setPath(strDiff.substring(start,end));
		}
	}
	
	
	/**
	 * Imposta il tipo di GitDiff in base al contenuto del Diff
	 */
	public void parseDiffType() {
		if (isModify()) {
			this.setType(Parameters.MODIFY);
		}
		else if (isAdd()) {
			this.setType(Parameters.ADD);
		}
		else if (isDelete()) {
			this.setType(Parameters.DELETE);
		}
		else if (isRename()) {
			this.setType(Parameters.RENAME);
		}
	}
	
	
	/**
	 * [DEBUG] Stampa tutte le informazioni su un GitDiff
	 */
	public void print() {
		System.out.println("CONT: "+this.content);
		System.out.println("TYPE: "+this.type);
		if (isRename()) {
			System.out.println("PAT1: "+this.renamePaths.get(0));
			System.out.println("PAT2: "+this.renamePaths.get(1));
		}
		else {
			System.out.println("PATH: "+this.path);
		}
	}
	

	/**
	 * Ritorna soltanto i DiffEntry relativi ad un file Java
	 */
	public static List<DiffEntry> filterJavaDiff(List<DiffEntry> diffEntries) {
		List<DiffEntry> filtered = new ArrayList<>();
		for (DiffEntry d:diffEntries) {
			if (isJava(d)) {
				filtered.add(d);
			}
		}
		return filtered;
	}
	
	public static boolean isJava(DiffEntry diff) {
		return diff.toString().contains(Parameters.FILTER_FILE_TYPE);
	}
	
	
	
	
	/*===============================================================================================
	 * Getters & Setters
	 */
	public DiffEntry getDiff() {
		return diff;	
	}

	public void setDiff(DiffEntry diff) {
		this.diff = diff;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public void setRenamePaths(List<String> renamePaths) {
		this.renamePaths = renamePaths;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public List<String> getRenamePaths() {
		return renamePaths;
	}
	
	public boolean isModify() {
		return this.content.contains(Parameters.MODIFY);
	}
	public boolean isRename() {
		return this.content.contains(Parameters.RENAME);
	}
	public boolean isAdd() {
		return this.content.contains(Parameters.ADD);
	}
	public boolean isDelete() {
		return this.content.contains(Parameters.DELETE);
	}
}
