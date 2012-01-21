package taglearner.ResultAnalyzer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import taglearner.DataCollection.Document;
import taglearner.ExperimentManager.Experiment;
import weka.clusterers.Clusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.matrix.DoubleVector;

public class ExperimentResult implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2239401930739907845L;
	
	private Instances m_Dataset;
	/**
	 * Reference to the clusterer built as a result of this clustering
	 */
	private Clusterer m_Clusterer;
	
	/**
	 *  List of documents that were clustered
	 */
	private List<? extends Document> m_DocList;
	
	/**
	 * Set of Instances that correspond to the centroids of the clusters
	 */
	private Instances      m_Centroids;
	
	/**
	 * Contains a hashmap of attribute index to corresponding term (e.g. index 2 = "court")
	 */
	private HashMap<Integer,String> idxToTerms;
	
	/**
	 * Contains information about the experiment to which this iteration belongs
	 */
	private Experiment     m_Experiment;
	
	/**
	 * ith clustering time-window, i = m_ExpIter
	 */
	private int            m_ExpIter;

	/*
	 *  Setters and Getters
	 */
	
	public void setDataset(Instances dataset) {
		this.m_Dataset = dataset;
	}
	
	public Instances getDataset() {
		return this.m_Dataset;
	}
	
	public void setClusterer(Clusterer clusterer) {
		this.m_Clusterer = (SimpleKMeans) clusterer;
	}
	
	public Clusterer getClusterer() {
		return this.m_Clusterer;
	}
	
	public void setDocList(List<? extends Document> docList) {
		this.m_DocList = docList;
	}
	
	public List<? extends Document> getDocList() {
		return this.m_DocList;
	}
	
	public void setCentroids(Instances centers) {
		this.m_Centroids = centers;
	}
	
	public Instances getCentroids() {
		return this.m_Centroids;
	}
	
	public void setIdxToTerms(Map<Integer,String> map) {
		this.idxToTerms = (HashMap<Integer,String>) map;
	}
	
	public Map<Integer,String> getIdxToTerms() {
		return this.idxToTerms;
	}
	
	public void setExperiment(Experiment exp) {
		this.m_Experiment = exp;
	}
	
	public Experiment getExperiment() {
		return this.m_Experiment;
	}
	
	public void setExpIter(int cnt) {
		this.m_ExpIter = cnt;
	}
	
	public int getExpIter() {
		return this.m_ExpIter;
	}

	public ArrayList<String> topicWordList(Instance instance, int topN) {
		ArrayList<String> wordList = new ArrayList<String>();
		TreeMap<Double,String> ranking = new TreeMap<Double,String>();
		double val = 0.0;
		int dim = instance.numAttributes();
		for (int j=0; j<dim; j++) {
			val = instance.value(j);
			if (ranking.size() < topN)
				ranking.put(val, this.idxToTerms.get(j));
			else {
				if (ranking.firstKey() < val) {
					ranking.remove(ranking.firstKey());
					ranking.put(val, this.idxToTerms.get(j));
				}
			}
		}

		Iterator<String> iter = ranking.values().iterator();
		for (; iter.hasNext(); ) {
			wordList.add(iter.next());
		}
		
		//System.out.println(ranking.toString());
		
		/*
		Iterator<Double> iter2 = ranking.keySet().iterator();
		DoubleVector v = new DoubleVector();
		double key = 0.0;
		//String term = null;
		//Iterator<String> iter = ranking.values().iterator();
		for (; iter.hasNext(); ) {
			key = iter2.next();
			//term = ranking.get(key);
			//term = iter.next();
			//wordList.add(term);
			v.addElement(key);
		}
		
		System.out.println("L1-norm = "+Double.toString(v.norm1()));
		System.out.println("L2-norm = "+Double.toString(v.norm2()));
		*/
		
		return wordList;
	}
	
	public void printCentroids() {
		int [] sizes = ((SimpleKMeans) this.m_Clusterer).getClusterSizes();
		System.out.println("Cluster sizes");
		for (int i=0; i<sizes.length; i++)
			System.out.print(sizes[i]+" ");
		System.out.println();
		
		for (int i=0; i<this.m_Centroids.numInstances(); i++) {
			Instance inst = this.m_Centroids.instance(i);
			ArrayList<String> topics = topicWordList(inst,10);
			System.out.println(topics.toString());
		}
	}
}
