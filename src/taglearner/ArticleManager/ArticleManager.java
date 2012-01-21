package taglearner.ArticleManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xerces.dom.NodeIteratorImpl;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;
import org.apache.commons.math.stat.*;

import taglearner.DataCollection.DocumentManager;
import taglearner.ExperimentManager.DateInterval;
import taglearner.ExperimentManager.ExperimentManager;
import taglearner.ExperimentManager.ExperimentManager.WekaCmd;
import taglearner.InputManager.InputManager;
import taglearner.OutputManager.ObjectDataManager;
import taglearner.OutputManager.OutputManager;
import taglearner.XMLManager.*;
import weka.clusterers.SimpleKMeans;
import weka.clusterers.XMeans;
import weka.core.Attribute;
import weka.core.CosineDistance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.InstanceComparator;
import weka.core.Instances;
import weka.core.SparseInstance;

public class ArticleManager extends DocumentManager implements Serializable {
	
	public enum DataType { TRAIN , TEST , BOTH }
	
	/*
	 * Terms that occur only once in the corpus will be left out of the BOW
	 */
	public static final int minAcceptableTermFrequency = 2;
	
	//private ArrayList<Article> articles; 
	transient private InputParser parser;
	//private File articleXML;    
	//private ArrayList< ArrayList<File> > ocrFileList;	
	transient private InputManager im;
	transient private OutputManager om;
	transient private ObjectDataManager odm;
	private boolean noOutput;
	private boolean processHLOnly;
	transient private Logger log;
	private String dataRootDir;
	private boolean runBOW;
	private boolean hasCSV;
	private boolean removeCategories;
	private boolean truncateText = false;
	private boolean isTrainingSet = false;
	private DataType inputType = DataType.TRAIN;
	private boolean noiseReduction = false;
	private boolean updated = false;
	//private HashSet<String> stopWordList = new HashSet<String>();
	
	private long tic, toc;
	
	//sprivate Map<String,Issue> issueMap = new HashMap<String,Issue>();
	private List<Issue> issueList = new ArrayList<Issue>();
	
	/*
	 * List of issues to be active/enabled for the current experiment
	 */
	private List<Issue> activeIssueList = new ArrayList<Issue>();
	private Issue curIssue; // = new Issue();
	private TreeMap<String, ArrayList<Article>> articleList = new TreeMap<String,ArrayList<Article>>();
	
	// Element at index 0 has the filename for Page 1 and so on
	//private ArrayList<File> ocrXML;
	
	// key = category, value = # instances in the category
	private static Map<String,Integer> categoryMap = new TreeMap<String,Integer>();
	
	// key = category, value = # numeric value of the category (1 .. k)
	private static Map<String,Integer> categoryIDMap = new HashMap<String,Integer>();
	
	// ordered set of categories
	private static Set<String>        categoryList = new TreeSet<String>();
	private static Map<String,Integer> binarySVMCategoryMap = new TreeMap<String,Integer>();
	private static Map<String,String>  binaryCategoryMap = new TreeMap<String,String>();
	private static int num_of_pos_ex = 0; 
	private static int num_of_neg_ex = 0;
	private static String pos_label = null;
	
	private Set<String> globalBOW = new HashSet<String>();
	private Set<String> globalBOW2 = new HashSet<String>();
	private Set<String> globalHLBOW = new HashSet<String>();
	private ArrayList<String> terms = new ArrayList<String>();
	private HashMap<String,Integer> termIndexes = new HashMap<String,Integer>();
	private HashMap<Integer,String> idxToTerm = new HashMap<Integer,String>();
	private TreeMap<String,Integer> df = new TreeMap<String,Integer>();
	//private Map<String,Double> m_Tf = new TreeMap<String,Double>();
	private Map<String,Integer> m_GlobalTF = new HashMap<String,Integer>();
	private int corpusSize;
	
	private boolean indexOnly = false;
	
	transient private ExperimentManager em;
	
	public static final int MIN_TERM_LEN = 4;
	private int kmeansNumRandInits = 1;  // default
	private int kmeansNumClusters = 10;    // default
	private boolean updateKmeans = false;
	private int maxClusterSize;
	private int numTopFeatures = 10; 
	private boolean printStats = false;
	private ExperimentManager.WekaCmd m_WekaCmd;
	private String[] m_WekaOpt;
	private boolean m_RunWeka;
	private Instances m_Dataset;
	private FastVector m_Attrs;
	private HashMap<Integer,Article> m_InstToArticleMap = new HashMap<Integer,Article>();
	private List<Article> m_DocList = new ArrayList<Article>();
	private boolean m_Stem = false;
	
	public boolean useSampling = false;
	
	private TreeSet<Float> wordConfidenceSet = new TreeSet<Float>();
	private HashMap<Float,Integer> wcHT = new HashMap<Float,Integer>();
	private final double wcThreshold = 0.00;
	
	public ArticleManager() {
		//ocrXML = new ArrayList<File>();
		//ocrFileList = new ArrayList< ArrayList<File> >();
	}
	
	public ArticleManager(InputManager in, OutputManager out, Logger logger) {
		//ocrXML = new ArrayList<File>();
		//ocrFileList = new ArrayList< ArrayList<File> >();
		setInputManager(in);
		if (null == out) { 
			noOutput = true;
		}
		else {	
			setOutputManager(out);
			this.processHLOnly = out.headlinesOnly();
		}
		
		this.log = logger;
	}
	
	public ArticleManager(String rootDir, InputManager in, OutputManager out, Logger logger) {
		//ocrXML = new ArrayList<File>();
		//ocrFileList = new ArrayList< ArrayList<File> >();
		setInputManager(in);
		if (null == out) { 
			noOutput = true;
		}
		else {	
			setOutputManager(out);
			this.processHLOnly = out.headlinesOnly();
		}
		
		this.log = logger;
		this.dataRootDir = rootDir;
		this.odm = new ObjectDataManager(new File(rootDir));
	}
	
	public ArticleManager(String rootDir, boolean bow, InputManager in, OutputManager out, Logger logger) {
		//ocrXML = new ArrayList<File>();
		//ocrFileList = new ArrayList< ArrayList<File> >();
		setInputManager(in);
		if (null == out) { 
			noOutput = true;
		}
		else {	
			setOutputManager(out);
			this.processHLOnly = out.headlinesOnly();
		}
		
		this.log = logger;
		this.dataRootDir = rootDir;
		
		this.runBOW = bow;
	}
	
	/*
	public void setExperimentDateRange(String sYear, String sMonth, String sDate, String fYear, String fMonth, String fDate) {
		this.startDate = new GregorianCalendar(Integer.parseInt(sYear),Integer.parseInt(sMonth),Integer.parseInt(sDate));
		this.endDate   = new GregorianCalendar(Integer.parseInt(fYear),Integer.parseInt(fMonth),Integer.parseInt(fDate));
		this.rangeSpecified = true;
	}
	
	public void setExperimentDateRange(DateInterval interval) {
		int year, month, date;
		year = interval.getStartDate().get(Calendar.YEAR);
		month = interval.getStartDate().get(Calendar.MONTH);
		date = interval.getStartDate().get(Calendar.DATE);
		this.startDate = new GregorianCalendar(year,month,date);
		
		year = interval.getEndDate().get(Calendar.YEAR);
		month = interval.getEndDate().get(Calendar.MONTH);
		date = interval.getEndDate().get(Calendar.DATE);
		this.endDate   = new GregorianCalendar(year,month,date);
		
		this.rangeSpecified = true;
	}
	*/
	
	public void setIndexMode(boolean indexOnly) {
		this.indexOnly = indexOnly;
	}
	
	public void setPrintStats(boolean stats) {
		this.printStats = stats;
	}
	
	//public void setStopWordList(HashSet<String> swl) {
	//	this.stopWordList.addAll(swl);
	//}
	
	public void setDataRootDir(String root) {
		this.dataRootDir = root;
	}
	
	public void setCSVMode(boolean csv) {
		this.hasCSV = csv;
	}
	
	public void setRemoveCategories(boolean remove) {
		this.removeCategories = remove;
	}
	
	public void setTruncateText(boolean truncate) {
		this.truncateText = truncate;
	}
	
	public void setAsTrainingSet() {
		this.isTrainingSet = true;
	}
	
	public void setAsTestSet() {
		this.isTrainingSet = false;
	}
	
	public void setDataType(String type) {
		if (type.equals("train"))
			this.inputType = DataType.TRAIN;
		else if (type.equals("test"))
			this.inputType = DataType.TEST;
		else if (type.equals("both"))
			this.inputType = DataType.BOTH;
	}
	
	public void setNoiseReduction(boolean noise) {
		this.noiseReduction = noise;
	}
	
	public void setKmeansNumClusters(int num) {
		this.kmeansNumClusters = num;
	}
	
	public void setKmeansNumRandInits(int num) {
		this.kmeansNumRandInits = num;
	}
	
	public void setUpdateKmeans(boolean b) {
		this.updateKmeans = b;
	}
	
	public void setupWekaExperiment(String cmd, String[] opt) {
		this.m_RunWeka = true;
		this.m_WekaOpt = opt;
		if (cmd.equals("kmeans"))
			this.m_WekaCmd = WekaCmd.KMEANS;
		else if (cmd.equals("xmeans"))
			this.m_WekaCmd = WekaCmd.XMEANS;
	}
	
	public void setStemming(boolean stem) {
		this.m_Stem = stem;
	}
	
	private static void incCategoryCount(String cat) {
		int cnt = 0;
		
		if (ArticleManager.categoryMap.containsKey(cat))
			cnt = ArticleManager.categoryMap.get(cat);
		
		if (!ArticleManager.categoryList.contains(cat))
			ArticleManager.categoryList.add(cat);
		
		ArticleManager.categoryMap.put(cat, (cnt+1));
	}
	
	/*
	private static int getCategorySize(String cat) {
		return ArticleManager.categoryMap.get(cat);
	}*/

	// GUI Support
	
	private String[] listToStringArray(List<String> list) {
		String[] strList = new String[list.size()];
		int i=0;
		for (String str : list) {
			strList[i] = str;
			i++;
		}
		return strList;
	}
	
	public String[] getIssueList() {
		//List<String> issues = new ArrayList<String>();
		String[] issues = new String[this.issueList.size()];
		int i=0;
		for (Issue issue : this.issueList) {
			issues[i] = issue.getName();
			i++;
		}
		return issues;
	}
	
	public String[] getPages(String issue) {
		List<String> pages = new ArrayList<String>();
		int i=1;
		for (Issue is : this.issueList) {
			if (is.getName().equals(issue)) {
				for (File f : is.getOCRs()) {
					pages.add(Integer.toString(i));
					i++;
				}
			}
		}
		return (String[]) listToStringArray(pages);
	}
	
	public int getPageIndexOfTextBlock(String issue, String id) {
		for (Issue is : this.issueList)
			if (is.getName().equals(issue))
				return is.pageIndexOfTextBlock(id);
		return -1;
	}
	
	public List<File> getTiffFileList(String issue) {
		for (Issue is : this.issueList) {
			if (is.getName().equals(issue)) {
				return is.getTiffs();
			}
		}
		return null;
	}
	
	public String[] getArticleList(String issue) {
		List<String> articles = new ArrayList<String>();
		for (Issue is : this.issueList) {
			if (is.getName().equals(issue)) {
				for (Article a : is.getArticles()) {
					articles.add(a.getID());
				}
			}
		}
		return (String[]) listToStringArray(articles);
	}
	
	public String[] getTiffList(String issue) {
		for (Issue is : this.issueList) {
			if (is.getName().equals(issue)) {
				String[] list = new String[is.getTiffs().size()];
				int i=0;
				for (File f : is.getTiffs()) {
					list[i] = this.dataRootDir + File.separator + f.getName();
					i++;
				}
				return list;
			}
		}
		return null;
	}
	
	public String[] getTextBlockList(String issue, String article) {
		for (Issue is : this.issueList) {
			if (is.getName().equals(issue)) {
				for (Article a : is.getArticles()) {
					if (a.getID().equals(article)) {
						return (String []) listToStringArray(a.getTextBlocks());
					}
				}
			}
		}
		return null;
	}

	public Article getArticle(String issue, String article) {
		for (Issue is : this.issueList) {
			if (is.getName().equals(issue)) {
				for (Article a : is.getArticles()) {
					if (a.getID().equals(article)) {
						return a;
					}
				}
			}
		}
		return null;
	}
	
	public void updateArticle(String issue, Article article) {
		for (Issue is : this.issueList) {
			if (is.getName().equals(issue)) {
				for (Article a : is.getArticles()) {
					if (a.getID().equals(article.getID())) {
						a = new Article(article);
					}
				}
			}
		}
	}
	
	public TextBlock getTextBlock(String issue, String article, String tb) {
		for (Issue is : this.issueList) {
			if (is.getName().equals(issue)) {
				for (Article a : is.getArticles()) {
					if (a.getID().equals(article)) {
						return a.getTextBlock(tb);
					}
				}
			}
		}
		return null;
	}

	public InputManager getInputManager() { return this.im; }
	public OutputManager getOutputManager() { return this.om; }
	public boolean getIndexMode() { return this.indexOnly; }
	
	public void setExperimentManager(ExperimentManager expM) {
		this.em = expM;
	}
	
	public ExperimentManager getExperimentManager() { return this.em; }
	
	public Instances getInstances() { return this.m_Dataset; }
	public Map<Integer,Article> getInstToArticleMap() { return this.m_InstToArticleMap; }
	public List<Issue> getActiveIssueList() { return this.activeIssueList; }
	public List<Article> getActiveDocList() { return this.m_DocList; }
	public int getKmeansNumRandInits() { return this.kmeansNumRandInits; }
	public Map<Integer,String> getIdxToTerms() { return this.idxToTerm; }
	
	public boolean isUpdated() { return this.updated; }
	
	private static void buildCategoryMaps() {
		int max = -1;
		String prime = null;
		for (String name : ArticleManager.categoryList) {
			if (max < ArticleManager.categoryMap.get(name)) {
				max = ArticleManager.categoryMap.get(name);
				prime = name;
			}
		}
		
		ArticleManager.pos_label = prime;
		
		int idx = 1;
		for (String name : ArticleManager.categoryList) {
			if (prime.equals(name)) {
				ArticleManager.binaryCategoryMap.put(name,"positive");
				ArticleManager.binarySVMCategoryMap.put(name, 1);
				num_of_pos_ex += ArticleManager.categoryMap.get(name);
			}
			else {
				ArticleManager.binaryCategoryMap.put(name,"negative");
				ArticleManager.binarySVMCategoryMap.put(name, -1);
				num_of_neg_ex += ArticleManager.categoryMap.get(name);
			}
			System.out.println("New category: "+name);
			ArticleManager.categoryIDMap.put(name, idx);
			idx++;
		}
	}
	
	/*
	 *  Builds a map of categories and articles with category as key and list of articles as value 
	 */
	private void buildArticleList() {
	
		for (String str : ArticleManager.categoryList) {
			this.articleList.put(str, new ArrayList<Article>());
		}
		
		ListIterator<Article> iter = null;
		String label = null;
		ArrayList<Article> foo = null;
		Article curr = null;
		
		for (Issue issue : this.issueList) {
			iter = issue.getArticleList();
			while (iter.hasNext()) {
				curr = (Article) iter.next();
				label = curr.getCategory().getName();
				foo = this.articleList.get(label);
				foo.add(curr);
			}
		}
		
		for (String str : ArticleManager.categoryList) {
			foo = this.articleList.get(str);
			System.out.println("Label = "+str+", size = "+foo.size());
		}
	}
	
	private void buildBinaryArticleList() {
		
		this.articleList.put("1", new ArrayList<Article>());
		this.articleList.put("-1", new ArrayList<Article>());
		
		ListIterator<Article> iter = null;
		String label = null;
		ArrayList<Article> foo = null;
		Article curr = null;
		
		for (Issue issue : this.issueList) {
			iter = issue.getArticleList();
			while (iter.hasNext()) {
				curr = (Article) iter.next();
				label = curr.getCategory().getName();
				foo = this.articleList.get(label);
				foo.add(curr);
			}
		}
		
		foo = this.articleList.get("1");
		System.out.println("Label = +1, size = "+foo.size());
		foo = this.articleList.get("-1");
		System.out.println("Label = -1, size = "+foo.size());
	}
	
	public static String getBinaryCategory(String cat) {
		return ArticleManager.binaryCategoryMap.get(cat);
	}
	public static int getBinarySVMCategory(String cat) {
		return ArticleManager.binarySVMCategoryMap.get(cat);
	}
	public static int getNumericCategory(String cat) {
		return ArticleManager.categoryIDMap.get(cat);
	}
	
	/*
	private void init() {
		articles = new ArrayList<Article>();
		parser   = new InputParser(this.articleXML);
	}*/
	
	/*
	 * Set up internal XML parser
	 * parser can parse any XML (SAX parser)
	 */
	private void init(File f) {
		//if (articles == null)
		//	articles = new ArrayList<Article>();
		parser   = new InputParser(f);
		//articleXML = im.getInputFile();		
	}
	
	public void setInputManager(InputManager in) {
		this.im = in;
	}
	
	public void setOutputManager(OutputManager out) {
		this.om = out;
	}
	
	private boolean isInIssueList(String id) {
		for (Issue issue : this.issueList)	 {
		
			if (issue.getName().equals(id))
				return true;
		}
		
		return false;
	}
	/*
	 * Extracts metadata for all articles in a single issue
	 * @param  iter - Iterator on the sequence of XML Nodes for an article
	 * @return true - success, false - failure
	 */
	public boolean getArticleMetadata(NodeIteratorImpl iter, Article a) {
		
		//boolean success = true;
		boolean isHeadline, isClassification;
		isHeadline = isClassification = false;
		File article = a.getFileReference();

		// Here each "article" corresponds to a articles_xxxx.xml file
		// Thus it represents an entire issue

		Node n;
		//boolean getText = false;
		String articleUID = "";
		String issueLabel;
		String articleID = "";

		// This must be the root node, but do this just to be sure
		n = iter.nextNode();
		while (!n.getNodeName().equals("mets"))
			n = iter.nextNode();

		issueLabel = ((Element) n).getAttribute("LABEL");
		articleUID = issueLabel.substring(0, issueLabel.indexOf(",")).toLowerCase() + "_";
		articleUID = articleUID.concat(issueLabel.substring(issueLabel.indexOf(",")+2)) + "_";
		articleUID = articleUID.replace(" ","_");
		articleUID = articleUID.replace("-","_");

		// No need to process again
		if (isInIssueList(issueLabel))
			return false;
		
		// Do this for each articles_xxxx.xml (i.e. one for each issue)
		//Issue currIssue = new Issue(issueLabel);		
		this.curIssue.setName(issueLabel);
		
		boolean doneWithDmdSec = false;

		while ((n = iter.nextNode()) != null) {

			if (n.getNodeName().equals("fileSec"))
				break;

			// dmdSec
			//System.out.println(n.getNodeName());


			if (n.getNodeName().equals("dmdSec"))
				// Start of a new article
				if (((Element) n).getAttribute("ID").startsWith("artModsBib_")) {
					articleID = ((Element) n).getAttribute("ID").substring(((Element) n).getAttribute("ID").indexOf("_")+1);
					//articleUID = articleUID.concat(articleID);
					a.setID(articleID);
					a.setUID(articleUID.concat(articleID));
					//this.log.info(articleUID.concat(articleID));
					//log.getHandlers()[0].flush();
				}

			if (n.getNodeName().equals("mods:detail")) {

				// Create a new instance of Article iff it has a headline
				if (((Element) n).getAttribute("type").equals("headline")) {
					isHeadline = true;
				}

				if (((Element) n).getAttribute("type").equals("classification"))
					isClassification = true;
			}

			if (isHeadline == true && n.getNodeName().equals("mods:text")) {
				//System.out.println(n.getTextContent());		
				String headline = n.getTextContent().toLowerCase();
				headline = removeNoisyText(headline);
				a.setHeadline(headline);
				isHeadline = false;
			}
			String label = null;
			// Set this iff headline was already set (so ordering matters)
			if (isClassification == true && n.getNodeName().equals("mods:text")) {
				label = n.getTextContent().replaceAll("/", "_").replaceAll(" ", "_").replaceAll("-", "_");
				a.setCategory(label);
				ArticleManager.incCategoryCount(label);
				isClassification = false;
				doneWithDmdSec = true;
			}

			if (doneWithDmdSec) {
				//this.articles.add(new Article(a));
				//a.clear();
				//articleID = "";
				this.curIssue.addNewArticle(new Article(a));
				//this.curIssue.addToArticleMap(articleID, a);
				doneWithDmdSec = false;
				a.clear();
			}
		}

		if (iter.nextNode() == null) {
			return false;
		}

		// File section
		while ((n = iter.nextNode()) != null) {

			if (n.getNodeName().equals("structMap"))
				break;

			if (n.getNodeName().equals("file") && ((Element) n).getAttribute("USE").equals("ocr")) {
				n = iter.nextNode();
				String ocrFilename = article.getParent() + File.separator + ((Element) n).getAttribute("xlink:href");
				File ocrFile = new File(ocrFilename);
				if (ocrFile.exists())
					//ocrXML.add(new File(ocrFilename));
					this.curIssue.addToOCRList(new File(ocrFilename));
				else
					System.err.println("ERROR: No such ocrFile: "+ocrFilename);
			}
		}

		if (iter.nextNode() == null) {
			return false;
		}

		while ((n = iter.nextNode()) != null) {

			if (n.getNodeName().equals("structMap")
					&& ((Element) n).getAttribute("LABEL").equals("Logical Structure"))
				break;

		}

		if (iter.nextNode() == null) {
			return false;
		}

		Article aa = null;
		String attr, articleID2;
		TextBlock tmpTB = null;
		String coord = null;
		boolean doneWithArticle = false;
		// Logical map
		while ((n = iter.nextNode()) != null) {
			if (n.getNodeName().equals("div") && 				
				((Element) n).getAttribute("DMDID").startsWith("artModsBib_")) {
				attr = ((Element) n).getAttribute("DMDID");
				articleID2 = attr.substring(attr.indexOf("_")+1);
				aa = this.curIssue.getArticle(articleID2);

				if (null == aa)
					System.err.println("ERROR: Article not found: "+article.getName()+","+attr);

				doneWithArticle = false;
				while (!doneWithArticle) {
					// get fptr
					n = iter.nextNode();
					// get area
					n = iter.nextNode();

					String startTB = ((Element) n).getAttribute("BEGIN");
					String endTB   = ((Element) n).getAttribute("END");
					String page    = ((Element) n).getAttribute("FILEID").substring(7);

					if (startTB.equals(endTB)) {
						if (aa.getTextBlocks() == null)
							aa.setTextBlocks(new ArrayList<String>());
						//aa.getTextBlocks().add(startTB);
						
						if (aa.getPages() == null)
							aa.setPages(new ArrayList<Integer>());

						aa.getPages().add(Integer.parseInt(page));
					}
					else {
						System.err.println("Unexpected TextBlock range "+article.getName()+","+attr);
					}

					// get fptr
					n = iter.nextNode();
					// get area
					n = iter.nextNode();
					
					//aa.getCoordList().add(((Element) n).getAttribute("COORDS"));
					coord = ((Element) n).getAttribute("COORDS");
					tmpTB = new TextBlock(startTB);
					tmpTB.setCoordinates(coord);
					aa.addTextBlock(startTB, new TextBlock(tmpTB));
					tmpTB.clear();
					
					// Look ahead
					n = iter.nextNode();
					
					if (null == n)
						break;
					
					if (n.getNodeName().equals("div")) {
						doneWithArticle = true;
					}
					n = iter.previousNode();
				}
			}
		}
		
		return true;
	}
		
	public void getArticleBody() {
		
		NodeIteratorImpl ocrIter;
		int lineCnt;
		Node n, n2, n3;
		//ArrayList<TextBlock> tbList = new ArrayList<TextBlock>();
		Map<String,TextBlock> tbMap = new HashMap<String,TextBlock>();
		HashMap<String,TextBlock> pageTBMap = new HashMap<String,TextBlock>();
		NodeList tlNodeList, strNodeList;
		TextBlock textBlock = new TextBlock("");
		String line = "";
		String block = "";
		String term = "";
		Stemmer stemmer = new Stemmer();
		
		OCRXMLCustomFilter filter = new OCRXMLCustomFilter();
		
		ListIterator<File> pageIter = this.curIssue.getOCRListIterator();
		
		int pageNum = 0;
		ArrayList<String> terms = new ArrayList<String>();
		while (pageIter.hasNext()) {

			init((File) pageIter.next());
			ocrIter = parser.parse((NodeFilter) filter);

			while (null != (n = ocrIter.nextNode())) {
				if (n.getNodeName().equals("TextBlock")) {
					lineCnt = 0;
					block = "";
					textBlock.setID(((Element) n).getAttribute("ID"));
					tlNodeList = n.getChildNodes();
					for (int i = 0; i < tlNodeList.getLength() ; i++) {
						n2 = tlNodeList.item(i);
						if (n2.getNodeName().equals("TextLine")) {
							lineCnt++;
							strNodeList = n2.getChildNodes();
							line = "";
							for (int j = 0; j < strNodeList.getLength() ; j++) {
								n3 = strNodeList.item(j);
								if (n3.getNodeName().equals("String")) {
									term = ((Element) n3).getAttribute("CONTENT").toLowerCase();
									
									// collect these values for some stat gathering
									float wc = Float.parseFloat(((Element) n3).getAttribute("WC"));
									
									if (wc < this.wcThreshold) {
										continue;
									}

									//if (this.wordConfidenceSet.contains(wc))
									//	System.out.println("Repeated WC: "+wc);
									//else
										this.wordConfidenceSet.add(wc);
									int cnt = 0;
										
										
									if (!this.noiseReduction) {
										terms.add(term);
										line = line.concat(term+" ");
									}
									else if ((term.length() >= MIN_TERM_LEN) 
											//&& (!this.stopWordList.contains((String) term))
											&& (!weka.core.Stopwords.isStopword(term))
											&& !isNoisyText(term)) {
										
										//terms.add(term);
										if (this.m_Stem) {
											stemmer.add(term.toCharArray(), term.length());
											stemmer.stem();
											term = stemmer.toString();
										}
										terms.add(term);
										line = line.concat(term+" ");
										
										if (this.wcHT.containsKey(wc)) {
											cnt = this.wcHT.get(wc);
											this.wcHT.put(wc, (cnt+1));
										}
										else
											this.wcHT.put(wc, 1);
									}

									//if (line.length() > 0 && j != (strNodeList.getLength()-1))
									//	line = line + " ";
								}
							}
							
							if (line.length() > 0) {
								//line = line.toLowerCase();
								//line = removeNoisyText(line);
								//System.out.println("Before: "+line);
								//System.out.println("After: "+terms.toString());
								block = block.concat(line + System.getProperty("line.separator"));
							}
						}
					}

					textBlock.setLineCnt(lineCnt);
					textBlock.setText(block);
					textBlock.setTerms(terms);
					terms.clear();
					tbMap.put(textBlock.getID(), new TextBlock(textBlock));
					pageTBMap.put(textBlock.getID(), new TextBlock(textBlock));
					textBlock.clear();
					//System.out.println(line);
				}
				
			}// end of while
			HashMap<String,TextBlock> copyMap = new HashMap<String,TextBlock>();
			copyMap.putAll(pageTBMap);
			this.curIssue.insertNewTextBlockMap(pageNum, copyMap);
			pageTBMap.clear();
			pageNum++;
		}// end of for - all pages for one issue

		String text = "";
		String tbID;
		int totalLineCnt = 0;
		ListIterator<Article> aIter = this.curIssue.getArticleList();
		ListIterator<String> tbIter;
		Article aaa;
		TextBlock tmpTB = null;
		TextBlock cachedTB = null;
		
		while (aIter.hasNext()) {
			aaa = aIter.next();
			tbIter = aaa.getTextBlocks().listIterator();
			while (tbIter.hasNext()) {
				//int tbIndex = tbList.indexOf(new TextBlock(aaa.getTextBlocks().get(p)));
				tbID = tbIter.next();
				cachedTB = tbMap.get(tbID);
				
				if (cachedTB != null) {		
					text = text.concat(cachedTB.getText());
					totalLineCnt += tbMap.get(tbID).getLineCnt();
					
					tmpTB = aaa.getTextBlock(tbID);
					tmpTB.setText(tbMap.get(tbID).getText());
					tmpTB.setLineCnt(tbMap.get(tbID).getLineCnt());
				}
				else {
					System.out.println("** Block ID: "+aaa.getID()+":"+tbID);
				}				
				terms.addAll(cachedTB.getTerms());
			}
			
			aaa.setText(text);
			aaa.setLineNum(totalLineCnt);
			aaa.setTerms(terms);
			
			//System.out.println("Num of terms: "+terms.size());
			terms.clear();
			
			text = "";
			totalLineCnt = 0;
		}
	}
	
	/**
	 * Only used for splitting the positive example set for binary classification
	 * 
	 * @param size
	 * @return
	 */
	private List<Article> randomSplit(int size) {
	
		List<Article> randomList = new ArrayList<Article>();
		ArrayList<Article> baseList = this.articleList.get(ArticleManager.pos_label);
		Random r = new Random();
		int range = baseList.size();
		int rand = -1;
		int count = 0;
		int tries = 0;
		int limit = size * 100;
		List<Integer> idx = new ArrayList<Integer>();
		
		while (count < size) {
			rand = r.nextInt(range);
			if (!idx.contains(rand)) {
				idx.add(rand);
				count++;
			}
			tries++;
			
			if (tries > limit)
				break;
		}
		
		if (count != size) {
			System.out.println("Unable to obtain random split for training data");
			System.exit(2);
		}
		
		for (int i : idx)
			randomList.add(baseList.get(i));
		
		return randomList;
	}
	
	private ArrayList<ArrayList<Article>> splitList(ArrayList<Article> articles, float ratio) {
		ArrayList<ArrayList<Article>> ret = new ArrayList<ArrayList<Article>>();
		ArrayList<Article> randomList = new ArrayList<Article>();
		ArrayList<Article> other      = new ArrayList<Article>();
		
		ArrayList<Article> baseList = articles;
		Random r = new Random();
		int range = baseList.size();
		int rand = -1;
		int count = 0;
		int tries = 0;
		int size = (int) Math.floor(range * ratio);
		int limit = size * 100;
		
		List<Integer> idx = new ArrayList<Integer>();
		
		while (count < size) {
			rand = r.nextInt(range);
			if (!idx.contains(rand)) {
				idx.add(rand);
				count++;
			}
			tries++;
			
			if (tries > limit)
				break;
		}
		
		if (count != size) {
			System.out.println("Unable to obtain random split for training data");
			System.exit(2);
		}
		
		for (int i : idx)
			randomList.add(baseList.get(i));
		
		for (Article a : baseList) {
			if (!randomList.contains(a))
				other.add(a);
		}
		
		ret.add(randomList);
		ret.add(other);
		return ret;
	}
	
	private ListIterator<Article> getBalancedTrainingSet() {
	
		ArrayList<Article> retList = (ArrayList<Article>) randomSplit(ArticleManager.num_of_neg_ex);
		
		for (String str : ArticleManager.categoryList) {
			if (!str.equals(ArticleManager.pos_label))
				retList.addAll(this.articleList.get(str));
		}
		
		return retList.listIterator();
	}
	
	private void updateLabels(HashMap<String,String> labelMap) {
		List<Article> aList = null;
		String label = null;
		for (Issue issue : this.activeIssueList) {
			aList = issue.getArticles();
			for (Article a : aList) {
				label = labelMap.get(a.getUID());
				a.setCategory(label);
			}
		}
	}
	
	private void removeFromSet(ArrayList<String> list) {
		List<Article> aList = null;
		List<Article> toBeRemoved = new ArrayList<Article>();
		int totalRemoved = 0;
		
		for (String str : list) {
			ArticleManager.categoryList.remove(str);
		}
		
		for (Issue issue : this.activeIssueList) {
			aList = issue.getArticles();
			for (Article a : aList) {
				if (list.contains(a.getCategory().getName()))
					toBeRemoved.add(a);
			}
			totalRemoved += toBeRemoved.size();
			aList.removeAll(toBeRemoved);
			toBeRemoved.clear();
		}
		System.out.println("# articles removed: "+totalRemoved);
	}
	
	private String getFirstNLines(String text, int num) {
		StringReader reader = new StringReader(text);
		LineNumberReader lineReader = new LineNumberReader(reader);
		String ret = "";
		int n = 0;
		
		while (n < num) {
			try {
				ret = ret + " " + lineReader.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			n++;
		}
		
		return ret;
	}
	
	private String getJustEnoughText(String text, int firstN, int cutoff, int lineCnt) {
		int N = 0;
		
		if (cutoff >= lineCnt)
			return text;
		
		N = firstN;
		
		return getFirstNLines(text,N);
	}
	
	/*
	 *  Truncate each article to the first N lines
	 *  
	 *  TODO: parameterize N 
	 */
	private void truncate() {
		List<Article> aList = null;
		String truncatedText = null;
		for (Issue issue : this.activeIssueList) {
			aList = issue.getArticles();
			for (Article a : aList) {
				truncatedText = getJustEnoughText(a.getText(),10,10,a.getLineNum());
				a.setText(truncatedText);
			}
		}
	}
	
	/*
	private void reduceNoise() {
		
		// Generate BOW
		tic = System.currentTimeMillis();
		generateBOW(false,false);
		toc = System.currentTimeMillis();
		printPerf();
		
	}*/
	
	private void computeWeights() {
		
		//String[] tokens = null;
		List<Article> aList = null;
		String cleanedText = "";
		int cnt = 0;
		
		System.out.println("Computing tf-idf for the current dataset");
		
		for (Issue issue : this.activeIssueList) {
			aList = issue.getArticles();
			for (Article a : aList) {
				//tokens = a.getText().replaceAll(System.getProperty("line.separator"), " ").replaceAll("[ ]+"," ").split("[ ]+");
				//for (int i=0; i<tokens.length; i++) {
				for (String term : a.getTerms()) {
					if (this.globalBOW2.contains(term)) {
						cleanedText += " " + term;
						
						// Compute term frequency for each article (document)
						cnt = a.tf.get(term);
						a.tf.put(term,cnt+1);
					}
				}
				a.setText(cleanedText);
				cleanedText = "";				
			}
		}
		
		cnt = 0;
		HashSet<String> termSet = new HashSet<String>();
		Iterator<String> strIter = null;
		String tmpTerm = null;
		for (Issue issue : this.activeIssueList) {
			for (Article a : issue.getArticles()) {
				//tokens = a.getText().split(" ");
				//for (int k=0; k < tokens.length; k++) {
				for (String term : a.getTerms())
					if (this.globalBOW2.contains((String) term))
						termSet.add(term);
				
				// Compute document frequency
				strIter = termSet.iterator();
				for (; strIter.hasNext(); ) {
					tmpTerm = strIter.next();
					cnt = this.df.get(tmpTerm);
					this.df.put(tmpTerm, cnt+1);
				}
				termSet.clear();
			}
		}
		
		System.out.println("Done computing tf-idf for the current dataset");
	}
	
	private void populateInstances() {
		m_Attrs = new FastVector();
		for (int m = 0; m < this.globalBOW2.size(); m++)
			m_Attrs.addElement(new Attribute("attr"+m));
		
		m_Dataset = new Instances("ActiveDataset",m_Attrs,0);
		if (this.m_DocList.size() > 0)
			this.m_DocList.clear();
		
		/*
		FileWriter fw = null;
		FileWriter arff = null;
		
		try {
			//fw = new FileWriter(new File("/Users/austin/classes/6901/news_data/w_matlab.csv"));
			arff = new FileWriter(new File("/Users/austin/classes/6901/news_data/dataset.arff"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		
		int instID = 0;
		for (Issue issue : this.activeIssueList) {
			for (Article a : issue.getArticles()) {
				a.computeTfIdf(this.corpusSize,this.globalBOW2.size(),this.df,this.termIndexes);
				this.m_Dataset.add(a.getInstance());
				this.m_DocList.add(a);
				a.setInstID(instID);
				this.m_InstToArticleMap.put(instID, a);
				instID++;
				//System.out.println(a.getInstance().toString());
				/*
				try {
					fw.write(a.getInstance().toString());
					fw.write(System.getProperty("line.separator"));
					fw.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/
			}
		}
	}
	
	private Instances getInstances(ArrayList<Integer> instIDs) {
		Instances subset = new Instances("temp",m_Attrs,0);
		
		for (int i : instIDs)
			subset.add(this.m_Dataset.instance(i));
		
		return subset;
	}
	
	/*
	private void foo() {
		
		//int[] sizes = kmeans[bestKmeans].getClusterSizes();
		int[] assignments = {-1};
		
		try {
			assignments = myKmeans.getAssignments();
			if (assignments == null)
				System.out.println("No assignments were made!");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		centers = myKmeans.getClusterCentroids();
		Enumeration em = centers.enumerateInstances();
		String topWords = null;
		Instances xmeansCenters = new Instances("initCenters",attrs,0);
		for (int idx=1; em.hasMoreElements(); idx++) {
			Instance instance = (Instance) em.nextElement();
			//System.out.println("Instance # "+idx+": "+instance.toString());
			//System.out.println("Size: "+sizes[idx-1]);
			int N = this.numTopFeatures;
			topWords = topicWordList(instance,N).toString();
			System.out.println(topWords);
			this.em.output(topWords+System.getProperty("line.separator"));	
			
			xmeansCenters.add(instance);
		}
		
		this.em.closeOutputStream();
		
		int K=0;
		
		try {
			K = myKmeans.numberOfClusters();
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		System.out.println("Num of clusters formed: "+K);
		int[] sizes = myKmeans.getClusterSizes();
		
		System.out.println();
		System.out.println("Cluster sizes");
		for (int i=0; i<K; i++) 
			System.out.print(sizes[i]+" ");
		System.out.println();
		
		maxClusterSize = -1;
		for (int i=0; i<sizes.length; i++)
			if (sizes[i] > maxClusterSize)
				maxClusterSize = sizes[i];
		
		//computeClusterDensities(dataset,centers,assignments);
		
		//System.out.println(kmeans[bestKmeans].toString());
		
		ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>();
		for (int tmp=0; tmp < numClusters; tmp++) {
			clusters.add(new ArrayList<Integer>());
		}
		for (int tmp=0; tmp < assignments.length; tmp++) {
			clusters.get(assignments[tmp]).add(tmp);
		}
		for (int tmp=0; tmp < numClusters; tmp++) {
			System.out.println("Cluster "+tmp);
			System.out.println(clusters.get(tmp).toString());
			//System.out.println(topicWordList(dataset,clusters.get(tmp),5));
		}
	
	}
	*/
	
	private boolean compareSparseInstances(SparseInstance first, SparseInstance second) {
	
		System.out.println(second.numValues());
		if (first.numValues() != second.numValues())
			return false;
		
		int size = first.numValues();
		for (int i=0; i<size; i++) {
			int idx = first.index(i);
			int idx1, idx2;
			idx1 = first.index(i);
			idx2 = second.index(i);
			
			//if (idx1 == idx2)
				System.out.println("("+idx1+","+idx2+")");
			
			if (first.value(idx) != second.value(idx))
				return false;
		}
		
		return true;
	}
		
	public ArrayList<String> topicWordList(Instance instance, int topN) {
		ArrayList<String> wordList = new ArrayList<String>();
		TreeMap<Double,String> ranking = new TreeMap<Double,String>();
		double val = 0.0;
		int dim = instance.numAttributes();
		for (int j=0; j<dim; j++) {
			val = instance.value(j);
			if (ranking.size() < topN)
				ranking.put(val, this.idxToTerm.get(j));
			else {
				if (ranking.firstKey() < val) {
					ranking.remove(ranking.firstKey());
					ranking.put(val, this.idxToTerm.get(j));
				}
			}
		}
		
		Iterator<Double> iter = ranking.keySet().iterator();
		double key = 0.0;
		String term = null;
		for (; iter.hasNext(); ) {
			key = (double) iter.next();
			term = ranking.get(key);
			wordList.add(term);
			if (this.globalHLBOW.contains(term))
				term = term.concat("*");
			//System.out.println("("+key+","+term+")");
		}
		//System.out.println();
		
		return wordList;
	}
	
	public Map<Double,String> topicWordRanking(Instance instance, int topN) {
		//ArrayList<String> wordList = new ArrayList<String>();
		TreeMap<Double,String> ranking = new TreeMap<Double,String>();
		double val = 0.0;
		int dim = instance.numAttributes();
		for (int j=0; j<dim; j++) {
			val = instance.value(j);
			if (ranking.size() < topN)
				ranking.put(val, this.idxToTerm.get(j));
			else {
				if (ranking.firstKey() < val) {
					ranking.remove(ranking.firstKey());
					ranking.put(val, this.idxToTerm.get(j));
				}
			}
		}
			
		return ranking;
	}
	
	public ArrayList<String> topicWordList(Instances dataset, ArrayList<Integer> idx, int topN) {
		ArrayList<String> wordList = new ArrayList<String>();
		TreeMap<Double,String> ranking = new TreeMap<Double,String>();
		Instance instance = null;
		double val = 0.0;
		int dim = dataset.numAttributes();
		for (int i : idx) {
			instance = dataset.instance(i);
			for (int j=0; j<dim; j++) {
				val = instance.value(j);
				if (ranking.size() < topN)
					ranking.put(val, this.idxToTerm.get(j));
				else {
					if (ranking.firstKey() < val) {
						ranking.remove(ranking.firstKey());
						ranking.put(val, this.idxToTerm.get(j));
					}
				}
			}
		}
		
		Iterator<Double> iter = ranking.keySet().iterator();
		double key = 0.0;
		String term = null;
		for (; iter.hasNext(); ) {
			key = (double) iter.next();
			term = ranking.get(key);
			wordList.add(term);
			//if (this.globalHLBOW.contains(term))
			//	term = term.concat("*");
			System.out.println("("+key+","+term+")");
		}
		System.out.println();
		
		return wordList;
	}
	
	private String removeNoisyText(String orig, String pattern, String replacement) {
		String cleaned = null;
		//Pattern p = Pattern.compile(pattern);
		//Matcher m = p.matcher(orig);
		
		cleaned = orig.replaceAll("(?m)"+pattern,replacement);
		
		return cleaned;
	}
	
	private String removeNoisyText(String orig) {
		String cleaned = orig;
		
		// non-alphabet
		//cleaned = removeNoisyText(cleaned,"[^a-z\\n ]","");
		
		// terms of length one
		//cleaned = removeNoisyText(cleaned," . "," ");
		//cleaned = removeNoisyText(cleaned,"^. "," ");
		//cleaned = removeNoisyText(cleaned," .$"," ");

		// terms of length two
		//cleaned = removeNoisyText(cleaned," .. "," ");
		//cleaned = removeNoisyText(cleaned,"^.. "," ");
		//cleaned = removeNoisyText(cleaned," ..$"," ");

		// repeated characters
		cleaned = removeNoisyText(cleaned,"(.)(\\1){2,}"," ");
		
		// digits
		cleaned = removeNoisyText(cleaned,"[0-9]{1,}"," ");
		
		// additional stopwords
		cleaned = removeNoisyText(cleaned,"(mr|ms|mrs)[\\.]?"," ");
		
		// Austin's --> Austin
		//cleaned = removeNoisyText(cleaned,"'s "," ");
		
		return cleaned;
	}
	
	private boolean isNoisyText(String orig) {
		boolean noisy = false;
		
		// non-alphabet
		//cleaned = removeNoisyText(cleaned,"[^a-z\\n ]","");
		
		// terms of length one
		//cleaned = removeNoisyText(cleaned," . "," ");
		//cleaned = removeNoisyText(cleaned,"^. "," ");
		//cleaned = removeNoisyText(cleaned," .$"," ");

		// terms of length two
		//cleaned = removeNoisyText(cleaned," .. "," ");
		//cleaned = removeNoisyText(cleaned,"^.. "," ");
		//cleaned = removeNoisyText(cleaned," ..$"," ");

		// repeated characters
		noisy = orig.matches("(.)(\\1){2,}");
		
		if (noisy)
			return true;
		
		// digits
		noisy = orig.matches("[0-9]{1,}");
		
		if (noisy)
			return true;
		
		// additional stopwords
		noisy = orig.matches("(mr|ms|mrs)[\\.]?");
		if (noisy)
			return true;
		
		// Austin's --> Austin
		//cleaned = removeNoisyText(cleaned,"'s "," ");
		
		return false;
	}
	
	/*
	 * Generates BOW for the current dataset while removing all stop words
	 */
	public void generateBOW(boolean truncate, boolean write) {
		
		int i=1;
		File statsOutFile = null;
		FileWriter fw = null;
		int cSize = 0;
		
		if (write) {
			statsOutFile = new File(this.dataRootDir+File.separator+"stats.txt");
			
			statsOutFile.mkdirs();

			try {
				fw = new FileWriter(statsOutFile);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		this.globalBOW.clear();
		this.globalBOW2.clear();
		
		System.out.println("Generating the Bag of Words for the current dataset...");
		int freq = 0;
		for (Issue issue : this.activeIssueList) {
			for (Article a : issue.getArticles()) {
				
				//TODO: Re-implement the truncation code below
				//if (!truncate)
				//	tokens = a.getText().replaceAll(System.getProperty("line.separator"), " ").split(" ");
				//else
				//	tokens = getJustEnoughText(a.getText(),10,10,a.getLineNum()).replaceAll(System.getProperty("line.separator"), " ").split(" ");
				
				for (String term : a.getTerms()) {
					/*
					if (this.globalBOW.contains(term)) {
						globalBOW2.add(term);
					}
					else
						globalBOW.add(term);
					*/

					if (this.m_GlobalTF.containsKey(term)) {
						freq = this.m_GlobalTF.get(term);
						this.m_GlobalTF.put(term, (freq+1));
					}
					else
						this.m_GlobalTF.put(term,1);
				}
				
				cSize++;
				
				// Process headlines
				/*
				String [] tokens = a.getCleanedHeadline().split("[ ]+");
				for (int j=0; j<tokens.length; j++) {
					if (!weka.core.Stopwords.isStopword(tokens[j]))
						this.globalHLBOW.add(tokens[j]);
				}*/
			}
			
			Iterator<String> iter = m_GlobalTF.keySet().iterator();
			String term = null;
			while (iter.hasNext()) {
				term = (String) iter.next();
				if (m_GlobalTF.get(term) >= ArticleManager.minAcceptableTermFrequency)
					this.globalBOW2.add(term);
			}
			
			System.out.println("Issue #: "+i);
			i++;
			System.out.println("Global BOW size: "+globalBOW.size());
			System.out.println("Global BOW2 size: "+globalBOW2.size());
			
			if (write) {
				try {
					fw.write(globalBOW.size()+","+globalBOW2.size()+"\n");
					fw.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		this.corpusSize = cSize;
		
		for (Issue issue : this.activeIssueList) {
			for (Article a : issue.getArticles()) {
				for (String term : a.getTerms())
					if (this.globalBOW2.contains(term)) 
						a.tf.put(term, 0);
			}
		}
		
		// Populate the list of terms in the entire corpus
		Iterator<String> iter = this.globalBOW2.iterator();
		String term = null;
		int index = 0;
		for (;iter.hasNext();) {
			term = iter.next();
			this.terms.add(term);
			this.termIndexes.put(term, index);
			this.idxToTerm.put(index, term);
			this.df.put(term, 0);
			index++;
		}
		
		if (write) {
			try {
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/*
	 *  
	 */
	private ArrayList<File> getParseList(ArrayList<File> xmlList) {
		ArrayList<File> parseList = new ArrayList<File>();
		Set<String> issueSet = new HashSet<String>();
		
		for (Issue issue : this.issueList) {
			issueSet.add(issue.getFilename());
		}
		
		for (File f : xmlList) {
			if (!issueSet.contains(f.getName())) {
				parseList.add(f);
			}
		}
		
		return parseList;
	}
	
	private void setupActiveIssueList(ArrayList<File> xmlList) {
		Set<String> xmlSet = new HashSet<String>();
		
		if (!this.activeIssueList.isEmpty())
			this.activeIssueList.clear();
		
		for (File f : xmlList) {
			xmlSet.add(f.getName());
		}
		
		for (Issue issue : this.issueList) {
			if (xmlSet.contains(issue.getFilename()))
				this.activeIssueList.add(issue);
		}
	}
	
	private void printPerf() {
		System.out.println("Time: "+(toc-tic)+"ms");
	}
	
	public void printStats() {
		List<Article> aList = null;
		List<Integer> lineCounts = new ArrayList<Integer>();
		List<Integer> termCounts = new ArrayList<Integer>();
		File statFile = new File(this.dataRootDir+File.separator+"stats.txt");
		FileWriter statFileWriter = null;
		try {
			statFileWriter = new FileWriter(statFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (Issue issue : this.activeIssueList) {
			aList = issue.getArticles();
			for (Article a : aList) {
				lineCounts.add(a.getLineNum());
				termCounts.add(a.getTerms().size());
			}
		}
		//System.out.println("Article line counts:");
		//System.out.println(lineCounts.toString());
		
		try {
			System.out.println("Writing stats to the stats file...");
			statFileWriter.write("Article line counts:\n");
			statFileWriter.write(lineCounts.toString());
			statFileWriter.write("\n\nArticle term counts:\n");
			statFileWriter.write(termCounts.toString());
			statFileWriter.flush();
			statFileWriter.close();
			System.out.println("Done writing stats");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run(ExperimentManager em) {
		File article;
		Article a = new Article();
		ArrayList<File> runList = null;
		ArrayList<File> articleXMLFileList = null;
		
		// Get the list of XMLs that are needed for the current experiment/run
		runList = em.getRunList();
		
		if (runList == null)
			return;
		
		// If we have already parsed some of these XMLs
		// parse only what we have not parsed yet
		if (!this.issueList.isEmpty())
			articleXMLFileList = getParseList(runList);
		else
			articleXMLFileList = runList;
			
		setupActiveIssueList(runList);
		
		ListIterator<File> listIter = articleXMLFileList.listIterator();
		
		// Do the following for each of the article XMLs in the list
		while (listIter.hasNext()) {

			article = (File) listIter.next();

			this.curIssue = new Issue();

			init(article);
			a.setFileReference(article);
			this.curIssue.setFilename(article.getName());
			ArticleXMLCustomFilter filter = new ArticleXMLCustomFilter();
			tic = System.currentTimeMillis();
			NodeIteratorImpl iter = parser.parse((NodeFilter) filter);
			toc = System.currentTimeMillis();
			
			System.out.println("Parsing time: "+(toc-tic)+"ms");

			tic = System.currentTimeMillis();
			if (getArticleMetadata(iter,a)) {

				if (this.processHLOnly) {
					//om.headlinesOnly();
					//om.output(a);
					//om.output(this.curIssue);
					continue;
				}
				else {
					getArticleBody();
				}

				toc = System.currentTimeMillis();
				System.out.println("Extract time: "+(toc-tic)+"ms");
				
				//Issue issue = new Issue(this.curIssue);
				this.issueList.add(new Issue(this.curIssue));
				this.activeIssueList.add(new Issue(this.curIssue));

				// Clear the current instance for the next issue
				//this.curIssue.clear();
				a.clear();
			}
		}// end of while

		//ArticleManager.buildCategoryMaps();
		buildArticleList();
		
		// Remove all articles belonging to (pre-defined) categories that we do not want
		if (this.removeCategories) {
			List<String> removeList = im.getRemoveList();
			removeFromSet((ArrayList<String>) removeList);
		}
		
		// If we are indexing for later use, stop here and return
		// Otherwise, proceed
		//if (this.indexOnly)
		//	return;

		// Import labels from a CSV file
		HashMap<String,String> labelMap = null;
		if (this.hasCSV) {
			im.readLabelsFromCSV();
			labelMap = (HashMap<String, String>) im.getLabels();
			updateLabels(labelMap);
		}
		
		// Use only the first N lines of each article
		if (this.truncateText) {
			truncate();
		}
		
		// Eliminate terms that are considered noise
		//if (this.noiseReduction) {
		//	reduceNoise();
		//}
		
		// Generate BOW
		tic = System.currentTimeMillis();
		generateBOW(false,false);
		toc = System.currentTimeMillis();
		printPerf();
		
		//if (true)
		//{
			File bowOutFile = new File(this.dataRootDir + File.separator+"bow.txt");
			FileWriter fw = null;
			try {
				fw = new FileWriter(bowOutFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Printing BOW of size " + Integer.toString(this.globalBOW2.size()) + "...");
			
			Iterator<String> iter = this.globalBOW2.iterator();
			//for (String word : this.globalBOW2)
			String word = "";
			int cnt = 0;
			while (iter.hasNext())
			{
				word = iter.next();
				try {
					fw.write(word);
					fw.write("\n");
					++cnt;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println("What!!!");
				}
			
			}
			
			try {
				fw.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println("Total # words written: " + cnt);
			System.exit(0);
		//}
		
		computeWeights();
		
		if (this.indexOnly && this.noOutput)
			return;
		
		populateInstances();
		
		//if (m_RunWeka) {
		//	em.runWeka(m_Dataset,null,m_WekaCmd,m_WekaOpt);
		//}
		
		if (this.printStats)
			printStats();
		
		// Re-label categories as +1 or -1 for binary classification
		//ArticleManager.buildCategoryMaps();

		// Output
		if (!this.noOutput) {
			if (this.inputType != DataType.BOTH) {
				
				//buildArticleList();
				
				if (this.om.isBalanced()) {
					om.output(getBalancedTrainingSet());
				}
				else {
					for (Issue issue : this.activeIssueList) {
						om.output(issue);
					}
				}
			}
			else {
				buildBinaryArticleList();
				ArrayList<ArrayList<Article>> totalList;
				ArrayList<Article> pos, neg;
				ArrayList<Article> train = new ArrayList<Article>();
				ArrayList<Article> test  = new ArrayList<Article>();
				//int size;
				pos = this.articleList.get("1");
				neg = this.articleList.get("-1");
				if (pos.size() < neg.size()) {
					totalList = splitList(pos, (float) 0.5);
					train.addAll(totalList.get(0));
					test.addAll(totalList.get(1));
					totalList.clear();
					totalList = splitList(neg, (float) (0.5 * pos.size())/neg.size());
					//totalList = splitList(neg, (float) 0.5);
					train.addAll(totalList.get(0));
					test.addAll(totalList.get(1));
					totalList.clear();
				}
				else {
					totalList = splitList(neg, (float) 0.5);
					train.addAll(totalList.get(0));
					test.addAll(totalList.get(1));
					totalList.clear();
					totalList = splitList(pos, (float) (0.5 * neg.size())/pos.size());
					train.addAll(totalList.get(0));
					test.addAll(totalList.get(1));
					totalList.clear();
				}
				
				String root = om.getRootDirPathname();
				om.initialize(root+File.separator+"train");
				om.output(train.listIterator());
				om.initialize(root+File.separator+"test");
				om.output(test.listIterator());
			}
		}
		
		// Generate BOW
		if (this.runBOW) {
			generateBOW(true,true);
		}
	}// end of run()
}// end of class ArticleManager
