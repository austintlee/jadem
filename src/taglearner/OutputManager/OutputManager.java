package taglearner.OutputManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import taglearner.ArticleManager.*;

/**
 * Handles outputs of different formats
 * Supported output formats:
 * txt - GATE
 * ARFF - Weka
 * 
 * Not yet supported formats:
 * XML
 * CSV
 * 
 * @author Austin Lee
 *
 */
public class OutputManager {

	public enum OutMode {HEADLINE_ONLY , FULL_TEXT , BOTH }
	public enum SortBy {DATE , CATEGORY , BINARY , BINARY_SVM , ALL}
	public enum OutFileType {TEXT , CSV , ARFF , XML}
	
	private String hack = "XXXXXXXXxxxxxxxxQQQQQQQRSRDETSskdnjeialskeirEW";
	
	private OutMode omode;
	private SortBy sortBy;
	private File   rootDir;
	private OutFileType otype;
	
	// balance = true will make # positive examples = # negative examples
	// only used for training data and binary classification
	private boolean balance;
	
	private boolean wekaHack = false;
	
	private String sep = File.separator;
	private String ext = ".txt";
	private String newline = System.getProperty("line.separator"); 
	
	private String hackIt(int num) {
		
		if (!this.wekaHack)
			return "";
		
		String ret = this.hack;
		for (int i=1; i < num ; i++) {
			ret = ret + " " + this.hack;
		}
		return ret;
	}
	
	public OutputManager(String outMode, String sort, String outType, boolean balance) {
		if (outMode.equals("headline"))
			omode = OutMode.HEADLINE_ONLY;
		if (outMode.equals("fulltext"))
			omode = OutMode.FULL_TEXT;
		if (outMode.equals("both"))
			omode = OutMode.BOTH;
		
		if (sort.equals("date"))
			sortBy = SortBy.DATE;
		if (sort.equals("category"))
			sortBy = SortBy.CATEGORY;
		if (sort.equals("binary"))
			sortBy = SortBy.BINARY;
		if (sort.equals("binary_svm"))
			sortBy = SortBy.BINARY_SVM;
		if (sort.equals("all"))
			sortBy = SortBy.ALL;
		
		
		// default
		if (outType == null || outType.equals("text")) 
			this.otype = OutFileType.TEXT;
		
		if (outType.equals("csv"))
			this.otype = OutFileType.CSV;
		
		this.balance = balance;
	}

	public boolean isBalanced() { return this.balance; }
	public void enableWekaHack() { this.wekaHack = true; }
	
	/**
	 * Set the path to the root of the output directory
	 * If this directory does not yet exist, try to create it
	 * @param root path to the root of the output directory
	 * @return true - the root is properly set up
	 *         false - unable to create/set up the root directory
	 */
	public boolean initialize(String root) {
		boolean ret = true;
		rootDir = new File(root);
		
		ret = rootDir.mkdirs();
		
		return ret;
	}
	
	public String getRootDirPathname() {
		return this.rootDir.getAbsolutePath();
	}
	
	public boolean headlinesOnly() {
		return (this.omode == OutMode.HEADLINE_ONLY);
	}
	
	/*
	public void setOutputFormat(String format) {
		this.outFormat = format;
	}*/
	
	/*
	 * Set up directory structure
	 */
	public void output(Article a) {
		List<String> childPaths = new ArrayList<String>();
		List<FileWriter> fileWriters = new ArrayList<FileWriter>();
		List<File> files = new ArrayList<File>();
		String childPath = null;
		
		String toWrite = "";
		switch (this.omode) {
		case HEADLINE_ONLY:
			//toWrite = a.getHeadline();
			toWrite = a.getCleanedHeadline();
			if (toWrite == null)
				toWrite = "";
			
			//	if (toWrite.length() > 0)
			toWrite = toWrite + " " + hackIt(a.getLineNum());
			break;
		case FULL_TEXT:
			toWrite = a.getText();
			if (toWrite == null)
				toWrite = "";
			
			//	if (toWrite.length() > 0)
			toWrite = toWrite + " " + hackIt(a.getLineNum());
			break;
		default:
			break;
		}
		
		/*
		if (null == toWrite)
			return;
		
		if (toWrite.length() == 0)
			return;
		*/
		
		switch (this.sortBy) {
		case CATEGORY:
			childPath = "category" + this.sep + "string" + this.sep + a.getCategory().getName() + this.sep + a.getUID() + this.ext;
			childPaths.add(childPath);
			//childPath = "category" + this.sep + "numeric" + this.sep + ArticleManager.getNumericCategory(a.getCategory().getName()) + this.sep + a.getUID() + this.ext;
			//childPaths.add(childPath);
			break;
		case DATE:
			String issue, year, month, day;
			int idx = a.getUID().indexOf("1"); // the first character of the year
			issue = a.getUID().substring(0, idx-1);
			String[] tokens = a.getUID().substring(idx).split("_");
			year = tokens[0];
			month = tokens[1];
			day = tokens[2];
			childPath = "date" + this.sep + issue + this.sep + year + this.sep + month + this.sep + day + this.sep + a.getUID() + this.ext; 
			childPaths.add(childPath);
			break;
		case BINARY:
			childPath = "binary" + this.sep + ArticleManager.getBinaryCategory(a.getCategory().getName()) + this.sep + a.getUID() + this.ext;
			childPaths.add(childPath);
			break;
		case BINARY_SVM:
			childPath = "binary_svm" + this.sep + ArticleManager.getBinarySVMCategory(a.getCategory().getName()) + this.sep + a.getUID() + this.ext;
			childPaths.add(childPath);
			break;
		case ALL:
			childPath = "category" + this.sep + "string" + this.sep + a.getCategory().getName() + this.sep + a.getUID() + this.ext;
			childPaths.add(childPath);
			childPath = "category" + this.sep + "numeric" + this.sep + ArticleManager.getNumericCategory(a.getCategory().getName()) + this.sep + a.getUID() + this.ext;
			childPaths.add(childPath);
			childPath = "binary" + this.sep + ArticleManager.getBinaryCategory(a.getCategory().getName()) + this.sep + a.getUID() + this.ext;
			childPaths.add(childPath);
			childPath = "binary_svm" + this.sep + ArticleManager.getBinarySVMCategory(a.getCategory().getName()) + this.sep + a.getUID() + this.ext;
			childPaths.add(childPath);
			childPath = "date" + this.sep + a.getUID() + this.sep + a.getUID() + this.ext; 
			childPaths.add(childPath);
			break;
		default:
			break;			
		}
		
		//childPath = childPath + this.sep + a.getUID() + this.ext;
		
		for (String path : childPaths) {
			File f = new File(this.rootDir,path);
			if (!f.getParentFile().mkdirs()) {
				// Uh oh!
			}
			
			files.add(f);
		}
		
		try {
			for (File f : files)
				fileWriters.add(new FileWriter(f));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			for (FileWriter fw : fileWriters) {
				if (fw != null) {
					fw.write(toWrite);
					fw.flush();
					fw.close();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void output(Issue issue) {
		ListIterator<Article> iter = issue.getArticleList();
		
		if (this.otype == OutFileType.TEXT) {
			
			if (this.omode != OutMode.BOTH) {
				while (iter.hasNext())
					output((Article) iter.next());
			}
			//TODO: re-write this so that omode is not set this way
			//      maybe pass a second param to output
			else {
				String root = this.rootDir.getAbsolutePath();
				initialize(root + "-headlines");
				
				// this is an ugly way of doing this...
				this.omode = OutMode.HEADLINE_ONLY;
				while (iter.hasNext())
					output((Article) iter.next());
				
				initialize(root + "-fulltext");
				
				// Rewind
				iter = issue.getArticleList();
				// this is an ugly way of doing this
				this.omode = OutMode.FULL_TEXT;
				while (iter.hasNext())
					output((Article) iter.next());
				
				// restore the value
				this.omode = OutMode.BOTH;
				initialize(root);
			}
		}
		else {
			FileWriter fw = null;
			File f;
			String filename = issue.getName() + ".csv";
			
			f = new File(this.rootDir,filename);
			
			if (!f.getParentFile().mkdirs()) {
				// Uh oh!
			}
			
			try {
				fw = new FileWriter(f);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String csv_header = getCSVHeader();
			try {
				fw.write(csv_header);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//String toWrite = null;
			while (iter.hasNext()) {
				writeCSV(fw,(Article) iter.next());
				
			}
			try {
				fw.flush();
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public void output(ListIterator aIter) {
		ListIterator<Article> iter = aIter;
		
		if (this.otype == OutFileType.TEXT) {
			
			if (this.omode != OutMode.BOTH) {
				while (iter.hasNext())
					output((Article) iter.next());
			}
			//TODO: re-write this so that omode is not set this way
			//      maybe pass a second param to output
			else {
				String root = this.rootDir.getAbsolutePath();
				initialize(root + "-headlines");
				
				// this is an ugly way of doing this...
				this.omode = OutMode.HEADLINE_ONLY;
				while (iter.hasNext())
					output((Article) iter.next());
				
				initialize(root + "-fulltext");
				
				// Rewind
				while (iter.hasPrevious())
					iter.previous();

				// this is an ugly way of doing this
				this.omode = OutMode.FULL_TEXT;
				while (iter.hasNext())
					output((Article) iter.next());
				
				// restore the value
				this.omode = OutMode.BOTH;
				initialize(root);
			}
		}
		else {/*
			FileWriter fw = null;
			File f;
			String filename = issue.getName() + ".csv";
			
			f = new File(this.rootDir,filename);
			
			if (!f.getParentFile().mkdirs()) {
				// Uh oh!
			}
			
			try {
				fw = new FileWriter(f);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String csv_header = getCSVHeader();
			try {
				fw.write(csv_header);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//String toWrite = null;
			while (iter.hasNext()) {
				writeCSV(fw,(Article) iter.next());
				
			}
			try {
				fw.flush();
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		}
		
	}
	
	/*
	 * TODO:
	 */
	private void writeTxt(Article a) {

	}

	private void writeCSV(FileWriter fw, Article a) {
		String toWrite = null;
		toWrite = a.getUID() + "," + a.getCategory().getName() + ", , , , , ," + a.getLineNum() + "," + a.getTagString() + newline;
		try {
			fw.write(toWrite);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*
	 * TODO:
	 */
	private String getCSVHeader() {
		String header = "Article ID,Category,CCAT,ECAT,GCAT,MCAT,Fragmented,Number of Lines, Additional Tags" + newline;
		return header;
	}

	/*
	 * TODO:
	 */
	private void writeXML(Article a) {
		
	}
	
	/*
	 * TODO:
	 */
	private void writeARFF(Article a) {
		
	}
}
