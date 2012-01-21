package taglearner.ResultAnalyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import taglearner.ArticleManager.ArticleManager;
import taglearner.DataCollection.DocumentManager;

public class ChronologicalAnalyzer implements Analyzer {

	private File m_ResDir;
	//private boolean m_CenterFileLoaded = false;
	//private boolean m_DMFileLoaded = false;
	private DocumentManager       m_DocManager;
	private Set<ExperimentResult> m_ExpList = new TreeSet<ExperimentResult>(new ExperimentResultComparator());
	
	public ChronologicalAnalyzer(String dir) {
		setResDir(dir);
	}
	
	public ChronologicalAnalyzer() {
		// TODO Auto-generated constructor stub
	}

	public void setResDir(String dir) {
		m_ResDir = new File(dir);
	}
	
	public void loadFromFile(File expFile) {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		ExperimentResult er = null;
		
		try {
			fis = new FileInputStream(expFile);
			ois = new ObjectInputStream(fis);
			er = (ExperimentResult) ois.readObject();
			m_ExpList.add(er);
			ois.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void loadExpResults(File resDir) {
		File [] resFileList = resDir.listFiles();
		for (int i=0; i<resFileList.length; i++) {
			if (resFileList[i].getName().startsWith("cluster-") && resFileList[i].getName().endsWith(".res"))
				loadFromFile(resFileList[i]);
		}
	}
	
	public void loadArticleManager(File file) {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		File dmFile = new File(file,"am.res");
		
		try {
			fis = new FileInputStream(dmFile);
			ois = new ObjectInputStream(fis);
			this.m_DocManager = (ArticleManager) ois.readObject();
			ois.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void load() {
		loadExpResults(this.m_ResDir);
		//loadArticleManager(this.m_ResDir);
	}
	
	public void analyze() {
		Iterator<ExperimentResult> iter = m_ExpList.iterator();
		TopicModeler modeler = new TopicModeler();
		
		while (iter.hasNext()) {
			ExperimentResult result = iter.next();
			//System.out.println(result.getExpIter());
			result.printCentroids();
			TopicSet ts = new TopicSet(result.getCentroids(),result.getIdxToTerms());
			modeler.addTopicSet(ts);
		}
		
		modeler.runModeler();
		System.out.println(modeler.toString());
	}

}
