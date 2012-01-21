package taglearner.ExperimentManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.stat.StatUtils;

import taglearner.ArticleManager.Article;
import taglearner.ArticleManager.Issue;
import taglearner.DataCollection.Document;
import taglearner.DataCollection.DocumentManager;
import taglearner.ResultAnalyzer.ExperimentResult;
import weka.clusterers.RandomizableClusterer;
import weka.core.CosineDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class ClusterAnalyzer {

	private File m_OutputFile;
	private FileWriter summaryOut;
	private File m_ResDir;
	
	private ArrayList<Article> m_ActiveDocList = null;
	private Map<Integer,Instances> m_Centers = new TreeMap<Integer,Instances>();
	private Instances m_CurrCenters;
	
	private String m_ExpID;
	
	public ClusterAnalyzer() {
		super();
		setExpID("0-0-0-0-0");
	}
	
	public void setOutputDir(String dir) {
		this.m_ResDir = new File(dir);
		this.m_OutputFile = new File(dir+File.separator+"cluster.txt");
		
		if (!m_ResDir.exists())
			m_ResDir.mkdirs();
		
		try {
			summaryOut = new FileWriter(this.m_OutputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setActiveDocList(List<Article> docList) {
		this.m_ActiveDocList = (ArrayList<Article>) docList;
	}
	public void setExpID(String id) {
		this.m_ExpID = id;
	}
	public void addNewClusterCentroids(int id, Instances centers) {
		this.m_Centers.put(id, centers);
		this.m_CurrCenters = centers;
	}
	
	public void analyze() {
		
	}
	
	public void serializeDataset() {
		FileOutputStream fos1 = null;
		ObjectOutputStream oout1 = null;
		File res1 = new File(m_ResDir,"cluster-"+this.m_ExpID+".res");
		
		if (res1.exists()) {
			res1.renameTo(new File(res1.getAbsolutePath()+".old"));
		}
		else {
			res1.getParentFile().mkdirs();
		}
		
		try {
			fos1 = new FileOutputStream(res1);
			oout1 = new ObjectOutputStream(fos1);
			oout1.writeObject(this.m_ActiveDocList);
			oout1.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void serializeExperimentResult(ExperimentResult result) {
		FileOutputStream fos = null;
		ObjectOutputStream oout = null;
		File res = new File(m_ResDir,"cluster-"+this.m_ExpID+".res");
		
		result.setDocList(this.m_ActiveDocList);
		result.setCentroids(m_CurrCenters);
		
		if (res.exists()) {
			res.renameTo(new File(res.getAbsolutePath()+".old"));
		}
		else {
			res.getParentFile().mkdirs();
		}
		
		try {
			fos = new FileOutputStream(res);
			oout = new ObjectOutputStream(fos);
			oout.writeObject(result);
			oout.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void serializeCentroids() {
		FileOutputStream fos2 = null;
		ObjectOutputStream oout2 = null;
		File res2 = new File(m_ResDir,"centers.res");
		
		if (res2.exists()) {
			res2.renameTo(new File(res2.getAbsolutePath()+".old"));
		}
		else {
			res2.getParentFile().mkdirs();
		}
		
		try {
			fos2 = new FileOutputStream(res2);
			oout2 = new ObjectOutputStream(fos2);
			oout2.writeObject(this.m_Centers);
			oout2.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void serializeDocManager(DocumentManager dm) {
		FileOutputStream fos = null;
		ObjectOutputStream oout = null;
		File res = new File(m_ResDir,"am.res");
		
		if (res.exists()) {
			res.renameTo(new File(res.getAbsolutePath()+".old"));
		}
		else {
			res.getParentFile().mkdirs();
		}
		
		try {
			fos = new FileOutputStream(res);
			oout = new ObjectOutputStream(fos);
			oout.writeObject(dm);
			oout.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void printSummary(RandomizableClusterer clusterer) {
		//FileWriter summaryOut = null;
		
		try {
			//summaryOut = new FileWriter(this.m_OutputFile);
			//summaryOut.write(clusterer.toString());
			summaryOut.append(clusterer.toString());
			summaryOut.flush();
			//summaryOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void closeOutputStream() {
		try {
			summaryOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void analyzeTopicTrend(ArrayList<Issue> activeIssues) {
		for (Issue issue : activeIssues)
			for (Article a : issue.getArticles()) {
				ArrayList<TreeMap<Double,String>> topics = (ArrayList<TreeMap<Double,String>>) a.getAllTopics();
				//if (topics.size() >=3) {
				//	System.out.println(a.getTerms().toString());
				//}
				System.out.println("Article: "+a.getUID());
				for (int i=0; i<topics.size(); i++) {
					TreeMap<Double,String> map = topics.get(i);
					System.out.println(map.toString());
				}
			}
	}
	
	public boolean canSplit(Instances dataset, Instances centers, int[] assignments, double cutOff) {
		HashMap<Integer,ArrayList<SparseInstance>> clusters = new HashMap<Integer,ArrayList<SparseInstance>>();
		ArrayList<SparseInstance> cluster = null;
		CosineDistance cdist = new CosineDistance();
		
		for (int i=0; i<assignments.length; i++) {
			cluster = clusters.get(assignments[i]);
			if (null == cluster)
				cluster = new ArrayList<SparseInstance>();
			cluster.add((SparseInstance) dataset.instance(i));
			clusters.put(assignments[i], cluster);
		}
		
		Iterator<Integer> iter = clusters.keySet().iterator();
		double dist;
		
		for (int j=-1 ;iter.hasNext();) {
			j = iter.next();
			cluster = clusters.get(j);
			SparseInstance center = new SparseInstance(centers.instance(j));
			for (SparseInstance instance : cluster) {
				if (instance.numValues() > 0) {
					dist = cdist.distance(center, instance);
					if (dist > cutOff)
						return true;
				}
			}
			
		}
		
		return false;
	}
	
	public double[] computeClusterDensities(Instances dataset, Instances centers, int[] assignments) {
		double[] densities = new double[assignments.length];
		HashMap<Integer,ArrayList<SparseInstance>> clusters = new HashMap<Integer,ArrayList<SparseInstance>>();
		ArrayList<SparseInstance> cluster = null;
		CosineDistance cdist = new CosineDistance();
		
		boolean found = false;
		
		for (int i=0; i<assignments.length; i++) {
			cluster = clusters.get(assignments[i]);
			if (null == cluster)
				cluster = new ArrayList<SparseInstance>();
			cluster.add((SparseInstance) dataset.instance(i));
			clusters.put(assignments[i], cluster);
		}
		
		Iterator<Integer> iter = clusters.keySet().iterator();
		double dist;
		int count;
		double maxDist, minDist;
		double totalDist;
		ArrayList<Double> distances = new ArrayList<Double>();
		double mean, var;
		
		for (int j=-1 ;iter.hasNext();) {
			j = iter.next();
			cluster = clusters.get(j);
			totalDist = dist = 0.0;
			count = 0; // counter the centroid first
			SparseInstance center = new SparseInstance(centers.instance(j));
			maxDist = 0.0;
			minDist = 1.0;
			for (SparseInstance instance : cluster) {
				if (instance.numValues() > 0) {
					dist = cdist.distance(center, instance);
					if (dist > maxDist)
						maxDist = dist;
					if (dist < minDist)
						minDist = dist;
					
					totalDist += dist;
					count++;
					distances.add(dist);
				}
			}
			
			double[] distArray = new double[distances.size()];
			int idx=0;
			for (double d : distances)
				distArray[idx++] = d;
			mean = var = 0.0;
			mean = StatUtils.mean(distArray);
			var  = StatUtils.variance(distArray, mean);
			
			//System.out.println("Total distance for cluster: "+Double.toString(dist));
			//System.out.println("Total distance for cluster: "+Double.toString(totalDist));
			if (maxDist != 0.0)	
				densities[j] = count / maxDist;
			else
				densities[j] = 0.0;
			
			System.out.println("Count: "+count+", maxDist: "+Double.toString(maxDist)+", minDist: "+Double.toString(minDist)+", density = "+Double.toString(densities[j]));
			System.out.println("Mean: "+Double.toString(mean)+", Variance: "+Double.toString(var));
			//printHistogram(distances);
			
			//printForMatlab(distances);
			distances.clear();
		}
		
		return densities;
	}
	
	private void printHistogram(ArrayList<Double> distances) {
		int[] counts = {0,0,0,0,0,0,0,0,0,0};
		
		System.out.println(distances.toString());
		
		for (double d : distances) {
			if (d <= 0.1)
				counts[0]++;
			else if (d <= 0.2)
				counts[1]++;
			else if (d <= 0.3)
				counts[2]++;
			else if (d <= 0.4)
				counts[3]++;
			else if (d <= 0.5)
				counts[4]++;
			else if (d <= 0.6)
				counts[5]++;
			else if (d <= 0.7)
				counts[6]++;
			else if (d <= 0.8)
				counts[7]++;
			else if (d <= 0.9)
				counts[8]++;
			else 
				counts[9]++;
		}
		
		/*
		System.out.println();
		System.out.println("Distance histogram");
		System.out.println("-------------------------------------");
		for (int i=0; i<10; i++)
			System.out.print("|"+counts[i]+"|");
		System.out.println();
		System.out.println("-------------------------------------");
		System.out.println();
		System.out.println();
		*/
	}
	
	private void printForMatlab(int maxClusterSize, ArrayList<Double> distances) {
		
		int diff = maxClusterSize - distances.size();
		
		if (diff > 0)
			for (int i=0; i<diff; i++)
				distances.add(-1000.0);
		
		System.out.println(distances.toString());
		
	}

}
