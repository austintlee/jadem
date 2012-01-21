package taglearner.ArticleManager;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import taglearner.DataCollection.Document;
import weka.core.Instance;
import weka.core.SparseInstance;

public class Article extends Document implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6178581664199794983L;
	
	// extracted from artModBib_X_Y, id = "X_Y" which is unique within the same issue
	String              id;
	// Unique across all issues, contains the name of the newspaper and the issue date (e.g. the_sun_1984_11_02_5_2)
	String				uid;
	String              headline;
	Category            category;
	ArrayList<Integer>  pages;
	ArrayList<String>   textBlocks = new ArrayList<String>();
	Map<String,TextBlock> textBlockMap = new HashMap<String,TextBlock>();
	String              text;
	int                 lineNum;
	ArrayList<String> terms = new ArrayList<String>();
	//ArrayList<Feature> featureList = new ArrayList<Feature>();
	File				fRef;
	List<String>		tags = new ArrayList<String>();
	
	// one for each textblock
	private List<String>        coordList = new ArrayList<String>();
	
	public TreeMap<String,Integer> tf = new TreeMap<String,Integer>();
	private TreeMap<Integer,Double> tfidf = new TreeMap<Integer,Double>();
	
	private Instance instance;
	private boolean hlCleaned = false;
	private int m_InstID = -1;
	
	private List<TreeMap<Double,String>> m_Topics = new ArrayList<TreeMap<Double,String>>();
	private List<TreeMap<Integer,Integer>> m_CenterAssignments = new ArrayList<TreeMap<Integer,Integer>>();
	
	public Article() {
		//
		this.category = new Category();
		this.pages = null;
		//this.textBlocks = null;
		//this.featureList = null;
	}
	
	// Deep-copy constructor
	public Article(Article src) {
		this.setID(src.getID());
		this.setUID(src.getUID());
		this.setHeadline(src.getHeadline());
		this.category = new Category();
		this.setCategory(src.getCategory().getName());
		if (null == src.getPages())
			this.pages = null;
		else
			this.pages = new ArrayList<Integer>(src.getPages());
		
		this.textBlocks.addAll(src.getTextBlocks());
		this.setText(src.getText());
		this.setLineNum(src.getLineNum());
		//if (null == src.getFeatureList())
		//	this.featureList = null;
		//else
		//	this.featureList = new ArrayList<ArticleFeature>(src.getFeatureList());
		this.textBlockMap.putAll(src.getTextBlockMap());
		this.tags.addAll(src.getTags());
		this.fRef = new File(src.getFileReference().getAbsolutePath());
		this.terms.addAll(src.getTerms());
		this.m_InstID = src.getInstID();
	}
	
	public void setInstID(int id) { this.m_InstID = id; }
	public int  getInstID() { return this.m_InstID; }
	
	public void addNewTopics(TreeMap<Double,String> topics) {
		this.m_Topics.add(topics);
	}
	
	public List<TreeMap<Double,String>> getAllTopics() { return this.m_Topics; }
	public TreeMap<Double,String> getTopics(int index) {
		return this.m_Topics.get(index);
	}
	
	/*
	 *  cSize = size of the corpus
	 *  vSize = size of vocabulary 
	 */
	public void computeTfIdf(int cSize, int dSize, TreeMap<String,Integer> df, HashMap<String, Integer> indexList) {
		Iterator<String> iter = tf.keySet().iterator();
		String term = null;
		double t1, t2;
		int index = -1;
		double normalizer = 0.0;
		
		for (; iter.hasNext(); ) {
			term = (String) iter.next();
			t1 = (double) tf.get(term);
			t2 = Math.log(cSize/df.get(term));
			index = indexList.get(term);
			this.tfidf.put(index, t1*t2);
			normalizer += (t1*t2) * (t1*t2);
		}
		
		normalizer = Math.sqrt(normalizer);
		iter = tf.keySet().iterator();
		double weight = 0.0;
		for (; iter.hasNext(); ) {
			term = (String) iter.next();
			index = indexList.get(term);
			weight = this.tfidf.get(index);
			weight = weight / normalizer;
			//System.out.println("Weight,norm"+weight+","+normalizer);
			this.tfidf.put(index,weight);
		}
		
		generateInstance(dSize);
	}
	
	private void generateInstance(int maxAttrNum) {
		int dSize = this.tf.size();
		double[] vals = new double[dSize];
		int[] ints = new int[dSize];
		
		Iterator<Integer> iter = this.tfidf.keySet().iterator();
		for (int i=0; iter.hasNext() ; i++) {
			ints[i] = (int) iter.next();
			vals[i] = (double) this.tfidf.get(ints[i]);
		}
		
		this.instance = new SparseInstance(1.0,vals,ints,maxAttrNum);
	}
	
	public void clear() {
		this.id = "";
		this.uid = "";
		this.headline = "";
		if (null != this.category)
			this.category.clear();
		this.pages = null;
		this.textBlocks.clear();
		this.text = "";
		this.lineNum = 0;
		//this.featureList.clear();
		this.textBlockMap.clear();
		this.tags.clear();
	}
	
	public void clearWeights() {
		this.tf.clear();
		this.tfidf.clear();
	}
	
	private String cleanText(String txt) {
		String cleaned = txt;
		String[] regex = {"\\[", "\\]", ";", "\"", "\'", ",", ":"};
		
		for (int i=0 ; i < regex.length ; i++) {
			cleaned = cleaned.replaceAll(regex[i], " ");
		}
		
		return cleaned;
	}
	
	private void cleanHeadline() {
		String[] regex = {"\\[illegible\\]"};
		
		for (int i=0 ; i < regex.length ; i++) {
			headline = headline.replaceAll(regex[i], " ");
		}

		this.headline = cleanText(this.headline);

	}
	// Setters
	public void setID(String newID) { this.id = newID; }
	public void setUID(String id) { this.uid = id; }
	public void setHeadline(String hl) { this.headline = hl; }
	public void setCategory(String cat) { this.category.setName(cat); }
	public void setText(String txt) { this.text = txt; }
	public void setLineNum(int num) { this.lineNum = num; }
	public void setPages(ArrayList<Integer> newPages) { this.pages = new ArrayList<Integer>(newPages); }
	public void setTextBlocks(ArrayList<String> tb) { this.textBlocks = new ArrayList<String>(tb); }
	//public void setFeatureList(ArrayList<ArticleFeature> fl) { this.featureList = new ArrayList<ArticleFeature>(fl); }
	public void setFileReference(File f) { this.fRef = f; }
	public void setTags(List<String> newTags) { this.tags.clear(); this.tags.addAll(newTags); }
	public void addNewTag(String tag) { this.tags.add(tag); }
	public void setTerms(ArrayList<String> newTerms) {
		this.terms.clear();
		this.terms.addAll(newTerms);
	}
	// Getters
	public String getID() { return this.id; }
	public String getUID() { return this.uid; }
	public String getHeadline() { return this.headline; }
	public List<String> getTags() { return this.tags; }
	public String getTagString() {
		String tag = "";
		for (String str : this.tags)
			tag = tag + " " + str;
		return tag;
	}
	
	public TextBlock getTextBlock(String id) {
		return this.textBlockMap.get(id);
	}
	
	public Map<String,TextBlock> getTextBlockMap() { return this.textBlockMap; }
	
	public String getCleanedHeadline() { 
		
		if (this.headline != null && !this.hlCleaned) {
			cleanHeadline();
			this.hlCleaned = true;
		}
			
		return this.headline; 
	}
	
	public Category getCategory() { return this.category; }
	public String getText() { return this.text; }
	public int getCategoryId() { return this.category.getId(); }
	public int getLineNum()	{ return this.lineNum; }
	public ArrayList<Integer> getPages() { return this.pages; }
	public ArrayList<String> getTextBlocks() { return this.textBlocks; }
	//public ArrayList<ArticleFeature> getFeatureList() { return this.featureList; }
	public File getFileReference() { return this.fRef; }
	public ArrayList<String> getCoordList() { return (ArrayList<String>) this.coordList; }
	public Instance getInstance() { return this.instance; }
	public ArrayList<String> getTerms() { return this.terms; }
	
	@Override
	public final String toString() {
		return this.getText();
	}
	
	public void initialize() {
		this.pages = new ArrayList<Integer>();
		this.textBlocks = new ArrayList<String>();
	}
	
	public void addTextBlock(String id, TextBlock tb) {
		if (!this.textBlockMap.containsKey(id)) {
			this.textBlockMap.put(id, tb);
			this.textBlocks.add(id);
		}
	}
}
