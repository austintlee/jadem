package taglearner.ExperimentManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math.stat.StatUtils;

import taglearner.ArticleManager.Article;
import taglearner.ArticleManager.ArticleManager;
import taglearner.ArticleManager.Issue;
import taglearner.DataCollection.Document;
import taglearner.ResultAnalyzer.ExperimentResult;
import taglearner.Sampler.RandomSampler;
import weka.clusterers.AbstractClusterer;
import weka.clusterers.RandomizableClusterer;
import weka.clusterers.SimpleKMeans;
import weka.clusterers.XMeans;
import weka.core.CosineDistance;
import weka.core.Instance;
import weka.core.Instances;

public class ExperimentManager {
	
	public enum WekaCmd { KMEANS , XMEANS }
	
	private String id;
	private ArrayList<DateInterval> dateIntervals = new ArrayList<DateInterval>();
	private ArrayList<File> fullList = new ArrayList<File>();
	private ArrayList<File> runList  = new ArrayList<File>();
	private TreeMap<Calendar,Integer> dateList = new TreeMap<Calendar,Integer>(new DateComparator());
	private File outputFile;
	private FileWriter writer;
	private Instances m_Inst;
	private WekaCmd m_WekaCmd;
	private String[] m_WekaOpt;
	private List<Experiment> m_Experiments = new ArrayList<Experiment>();
	private ArticleManager m_AM;
	private int m_topN = 10;
	
	public ExperimentManager() {
		
	}

	public ExperimentManager(ArrayList<File> xmls) {
		setRunList(xmls);
	}

	public void setAM(ArticleManager am) {
		this.m_AM = am;
	}
	
	public void setID(String ID) { this.id = ID; }
	public String getID() { return this.id; }
	public void setInstances(Instances inst) { this.m_Inst = inst; }
	public void setOutputFile(String filename) {
		outputFile = new File(filename);
		
		try {
			writer = new FileWriter(outputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setWekaCmd(String cmd) {
		if (cmd.equals("kmeans"))
			this.m_WekaCmd = WekaCmd.KMEANS;
		else if (cmd.equals("xmeans"))
			this.m_WekaCmd = WekaCmd.XMEANS;
	}
	
	public void setWekaOpt(File optFile) {
		Reader reader = null;
		String line = "";
		
		try {
			reader = new FileReader(optFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LineNumberReader lineReader = new LineNumberReader(reader);
		try {
			line = lineReader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.m_WekaOpt = line.split(" ");
	}
	
	public void output(String str) {
		try {
			writer.write(str);
			writer.write("\n");
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void closeOutputStream() {
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setRunList(ArrayList<File> xmls) {
		this.fullList.addAll(xmls);
		setDateList(xmls);
	}
	
	public ArrayList<File> getRunList() { return this.runList; }
	
	/*
	 * From the list of XMLs, generate an ordered list of dates in chronological order
	 */
	public void setDateList(ArrayList<File> xmls) {
		
		Pattern p = Pattern.compile("(\\d\\d\\d\\d)(\\d\\d)(\\d\\d)(\\d\\d)");
		int idx = 0;
		
		for (File f : xmls) {
			String base = f.getParentFile().getName();
			Matcher m = p.matcher(base);
			
			String year, month, date;
			if (m.find()) {
				year = base.substring(m.start(1), m.end(1));
				month = base.substring(m.start(2), m.end(2));
				date = base.substring(m.start(3), m.end(3));
			
				Calendar d = new GregorianCalendar();
				
				d.set(Calendar.YEAR,Integer.parseInt(year));
				d.set(Calendar.MONTH,Integer.parseInt(month));
				d.set(Calendar.DAY_OF_MONTH,Integer.parseInt(date));
				
				//System.out.println("Date: "+d.toString());
				
				this.dateList.put(d,idx);
				idx++;
			}
		}
	}

	/*
	 * index = index of the starting date
	 * duration = # days to include in the experiment, -1 to use the full range
	 */
	public void setDates(int index, int duration) {
		if (duration < 0) {
			this.runList.addAll(this.fullList);
		}
		else {
			Iterator<Calendar> iter = this.dateList.keySet().iterator();
			int start = 0;
			int count = 0;
			Calendar ca = null;
			int idx = 0;

			for (; iter.hasNext(); start++) {
				if (start == index) {
					while (count < duration && iter.hasNext()) {
						ca = (Calendar) iter.next();
						idx = this.dateList.get(ca);
						this.runList.add(this.fullList.get(idx));
						count++;
					}
				}
				if (iter.hasNext())
					iter.next();
			}
		}
	}
	
	/*
	 *  Use this to clear the current list of dates under experiment
	 */
	public void clearRunList() { this.runList.clear(); }
	
	public static ArrayList<File> getFilesWithinRange(ArrayList<File> files, Calendar d1, Calendar d2) {
		ArrayList<File> ret = new ArrayList<File>();
		
		for (File f : files) {
			String dirname = f.getParentFile().getName();
			Calendar d = DateInterval.convertToDate(dirname);
			if (ExperimentManager.equal(d,d1) || ExperimentManager.equal(d,d2) || (d.after(d1) && d.before(d2))) 
				ret.add(f);
		}
		return ret;
	}
	
	public static boolean equal(Calendar d1, Calendar d2) {
		boolean ret = false;
		
		if (d1.get(Calendar.YEAR) == d2.get(Calendar.YEAR)
				&& d1.get(Calendar.MONTH) == d2.get(Calendar.MONTH)
				&& d1.get(Calendar.DATE) == d2.get(Calendar.DATE))
			ret = true;
		
		return ret;
	}
	
	private RandomizableClusterer runKmeans(Instances dataset, int numRandomStarts, int numClusters, boolean updateKmeans) {
		File kmeansFile = new File(this.outputFile.getParentFile().getAbsolutePath()+File.separator+"db"+File.separator+"kmeans.ser");
		SimpleKMeans myKmeans = null;
		//Instances centers = null;
		CosineDistance cd = new CosineDistance();
		
		if (kmeansFile.exists() && !updateKmeans) {
			FileInputStream fis = null;
			ObjectInputStream ois = null;
			try {
				fis = new FileInputStream(kmeansFile);
				ois = new ObjectInputStream(fis);
				myKmeans = (SimpleKMeans) ois.readObject();
				ois.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			SimpleKMeans[] kmeans = new SimpleKMeans[numRandomStarts];
			double sqError = Double.POSITIVE_INFINITY;
			String[] opt = new String[3];
			Date d = null;
			int bestKmeans = 0;

			try {
				for (int k_i = 0; k_i < numRandomStarts; k_i++) {
					d = new Date();
					opt[0] = "-S";
					opt[1] = Integer.toString((int) d.getTime());
					opt[2] = "-O";

					System.out.println("Running with a new seed");

					kmeans[k_i] = new SimpleKMeans();
					kmeans[k_i].setOptions(opt);
					kmeans[k_i].setNumClusters(numClusters);
					//System.out.println("# Clusters: "+kmeans.numberOfClusters());
					kmeans[k_i].setDistanceFunction(cd);
					kmeans[k_i].buildClusterer(dataset);
					if (kmeans[k_i].getSquaredError() < sqError && kmeans[k_i].numberOfClusters() == numClusters) {
						sqError = kmeans[k_i].getSquaredError();
						bestKmeans = k_i;
					}
					//System.out.println("# Clusters: "+kmeans.numberOfClusters());
					//centers = kmeans.getClusterCentroids();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			output("Sum of squared errors: "+sqError+System.getProperty("line.separator"));
			myKmeans = kmeans[bestKmeans];

			FileOutputStream fos = null;
			ObjectOutputStream oout = null;
			
			if (kmeansFile.exists()) {
				kmeansFile.renameTo(new File(kmeansFile.getAbsolutePath()+".old"));
			}
			else {
				kmeansFile.getParentFile().mkdirs();
			}
			
			try {
				fos = new FileOutputStream(kmeansFile);
				oout = new ObjectOutputStream(fos);
				oout.writeObject(myKmeans);
				oout.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		return myKmeans;
	}
	
	private RandomizableClusterer runXmeans(Instances dataset, String centerFile, String cutOff) {
		XMeans xmeans = new XMeans();
		CosineDistance cd = new CosineDistance();
		String[] xmeansOpt = new String[10];
		xmeansOpt[0] = "-S";
		Date date = new Date();
		xmeansOpt[1] = Integer.toString((int) date.getTime());
		xmeansOpt[2] = "-N";
		xmeansOpt[3] = centerFile; //this.outputFile.getParentFile().getAbsolutePath()+File.separator+"centers.arff";
		xmeansOpt[4] = "-C";
		xmeansOpt[5] = cutOff; // "0.8";
		xmeansOpt[6] = "-L";
		xmeansOpt[7] = "15";
		xmeansOpt[8] = "-I";
		xmeansOpt[9] = "5";
		//xmeans.setMinNumClusters(this.kmeansNumClusters+5);
		xmeans.setMaxNumClusters(200);
		xmeans.setDistanceF(cd);
		//xmeans.setCutOffFactor(0.5);
		//xmeans.setInputCenterFile(centerFile);
		//xmeans.setOutputCenterFile(xmeansOutput);
		try {
			xmeans.setOptions(xmeansOpt);
			xmeans.buildClusterer(dataset);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int k = xmeans.numberOfClusters();
		
		System.out.println("Xmeans:");
		System.out.println("Num of clusters found: "+k);
		
		File xmeansSummary = new File(this.outputFile.getParentFile().getAbsolutePath()+File.separator+"xmeans.out");
		FileWriter xmeansSummaryOut = null;
		
		try {
			xmeansSummaryOut = new FileWriter(xmeansSummary);
			xmeansSummaryOut.write(xmeans.toString());
			xmeansSummaryOut.flush();
			xmeansSummaryOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return xmeans;
	}
	
	private File convertToARFF(Instances inst, String arffFilename) {
		FileWriter centerArff = null;
		//FileWriter xmeansOut = null;
		File arff = new File(arffFilename);
		//File xmeansOutput = new File(this.dataRootDir+File.separator+"xmeansOut.arff");
		
		try {
			centerArff = new FileWriter(arff);
			//xmeansOut = new FileWriter(xmeansOutput);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
		try {
			centerArff.write(inst.toString());
			centerArff.flush();
			centerArff.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return arff;
	}
	
	public void runWeka(RandomizableClusterer myCluster, Instances centers) {
		
		Date date = new Date();
		int seed = (int) date.getTime();
		boolean randomize = true;
		File centerArff = null;
		
		if (centers != null) {
			randomize = false;
			centerArff = convertToARFF(centers,this.outputFile.getParentFile().getAbsolutePath()+File.separator+"temp.arff");
		}
		
		try {
			String[] temp = m_WekaOpt.clone();
			myCluster.setOptions(m_WekaOpt);
			m_WekaOpt = temp;
			
			if (randomize)
				myCluster.setSeed(seed);
			else if (myCluster instanceof XMeans)
				((XMeans) myCluster).setInputCenterFile(centerArff);
			
			myCluster.buildClusterer(m_Inst);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run(ArticleManager am) {
		int start,end, j, k, cnt;
		Map<Integer,Article> instToArticleMap = null;
		//RandomizableClusterer myClusterer = null;
		Instances initCentroids = null;
		Instances centroids = null;
		ClusterAnalyzer ca = new ClusterAnalyzer();
		ca.setOutputDir(this.outputFile.getParentFile().getAbsolutePath()+File.separator+"clustering_results");
		List<Issue> issueList = null;
		int numClusterers = am.getKmeansNumRandInits();
		ArrayList<Article> docList = null;
		String expID;
		double minSqErr = Double.POSITIVE_INFINITY;
		
		setAM(am);
		
		for (Experiment e : this.m_Experiments) {
			j=e.getDuration();
			k=e.getIncrement();
			start = e.getStart();
			end = e.getIteration();
			cnt = 0;
			int expIndex=0;
			for (int i=start; cnt < end; i+=k, cnt++) {
				//ArrayList<RandomizableClusterer> randClusterers = new ArrayList<RandomizableClusterer>();
				RandomizableClusterer randClusterer = null;
				RandomizableClusterer myClusterer = null;
				ExperimentResult result = new ExperimentResult();
				result.setExperiment(e);
				result.setExpIter(cnt);
				result.setIdxToTerms(am.getIdxToTerms());
				result.setDataset(m_Inst);
				
				setDates(i,j);
				//setOutputFile(this.curWorkingDir+File.separator+"kmeans-"+i+"-"+j);
				Date d = new Date();
				expID = Integer.toString(start)+"-"+Integer.toString(j)
						+"-"+Integer.toString(k) +"-"+Integer.toString(cnt)
						+"-"+Integer.toString(expIndex)+"-"+d.getTime();
				ca.setExpID(expID);
				
				PerfTimer.Start();
				am.run(this);
				PerfTimer.Stop();
				PerfTimer.PrintPerf("Input parsing and vectorization");
				
				docList = (ArrayList<Article>) am.getActiveDocList();
				ca.setActiveDocList(docList);
				
				this.m_Inst = am.getInstances();
				instToArticleMap = am.getInstToArticleMap();
				issueList = am.getActiveIssueList();
				
				/*
				for (int c=0; c<numClusterers; c++)
					if (m_WekaCmd == WekaCmd.KMEANS)
						randClusterers.add(new SimpleKMeans());
					else if (m_WekaCmd == WekaCmd.XMEANS)
						randClusterers.add(new XMeans());
					else {}
				*/
				
				double [] runTimes = new double[numClusterers];
				
				// Used for sampling purposes
				// sample again if any one of the clusters is too small
				boolean tryAgain = false;
				
				for (int idx=0; idx<numClusterers || tryAgain; idx++) {
					if (m_WekaCmd == WekaCmd.KMEANS)
						randClusterer = new SimpleKMeans();
					else if (m_WekaCmd == WekaCmd.XMEANS)
						randClusterer = new XMeans();
					else {}
					PerfTimer.Start();
					runWeka(randClusterer,initCentroids);
					PerfTimer.Stop();
					PerfTimer.PrintPerf("Weka running");
					runTimes[idx] = PerfTimer.time;
					double sqErr = ((SimpleKMeans) randClusterer).getSquaredError();
					if (sqErr < minSqErr) {
						try {
							myClusterer = (SimpleKMeans) AbstractClusterer.makeCopy(randClusterer);
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						minSqErr = sqErr;
					}
					int[] sizes = ((SimpleKMeans) randClusterer).getClusterSizes();

					if (am.useSampling) {
						for (int x=0; x<5; ++x)
							if (sizes[x] < 5) {
								tryAgain = true;
								System.out.println("Cluster too small, running KMeans again...");
								--idx;
								break;
							}
					}
				}
				double avgRuntime = StatUtils.mean(runTimes);
				PerfTimer.PrintMsg("Avg running time: "+Double.toString(avgRuntime));
				
				//myClusterer = selectBestClusterer(randClusterers);
				result.setClusterer(myClusterer);
				
				int[] assignments = null;
				if (myClusterer instanceof SimpleKMeans) {
					SimpleKMeans kmeans = (SimpleKMeans) myClusterer;
					centroids = kmeans.getClusterCentroids();
					
					try {
						assignments = kmeans.getAssignments();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					for (int idx=0; idx<assignments.length; idx++) {
						int cluster = assignments[idx];
						Article a = instToArticleMap.get(idx);
						Instance instance = centroids.instance(cluster);
						a.addNewTopics((TreeMap<Double,String>) am.topicWordRanking(instance, m_topN));
					}
				}
				
				System.out.println("Experiment "+cnt);
				//ca.printSummary(myClusterer);
				ca.computeClusterDensities(m_Inst, centroids, assignments);
				ca.addNewClusterCentroids(expIndex, centroids);
				printCentroids(centroids,m_topN);
				ca.analyze();
				//ca.serializeDataset();
				ca.serializeExperimentResult(result);
				
				for (Issue issue : issueList)
					for (Article a : issue.getArticles())
						a.clearWeights();
				
				clearRunList();
				
				/* Used only for random sampling
				int users = 5;
				for (int u=0; u<users; ++u) {
					RandomSampler sampler = new RandomSampler();
					ArrayList<ArrayList<Integer>> samples = sampler.getSamples(myClusterer, this.m_Inst, 5);
					ArrayList<ArrayList<String>> sampleArticles = new ArrayList<ArrayList<String>>(5);
					int ix,jx;
					for (ix=0; ix<5; ++ix) {
						ArrayList<String> tmp = new ArrayList<String>();
						sampleArticles.add(tmp);
					}

					for (ix=0; ix<5; ++ix) {
						for (jx = 0; jx < samples.get(ix).size(); ++jx) {
							sampleArticles.get(ix).add(docList.get(samples.get(ix).get(jx)).getID());
						}
					}

					System.out.println(sampleArticles);

					TreeSet<Integer> set1 = new TreeSet<Integer>();
					for (int y=0; y<5; ++y)
						set1.addAll(samples.get(y));
					ArrayList<Integer> set2 = new ArrayList<Integer>();
					set2.addAll(set1);
					ArrayList<String> set3 = new ArrayList<String>();
					for (int z=0; z<25; ++z)
						set3.add(docList.get(set2.get(z)).getID());
					System.out.println(set3);
				}
				*/
			}
			
			expIndex++;
			ca.analyzeTopicTrend((ArrayList<Issue>) issueList);	
		}
		//ca.serializeCentroids();
		//ca.serializeDocManager(am);
		ca.closeOutputStream();
	}
	
	// Pick the one with the smallest squared error
	private RandomizableClusterer selectBestClusterer(ArrayList<RandomizableClusterer> clusterers) {
		int i=0;
		int num = clusterers.size();
		double minError = ((SimpleKMeans) clusterers.get(0)).getSquaredError();
		for (int idx=1; idx<num; idx++) {
			if (minError > ((SimpleKMeans) clusterers.get(idx)).getSquaredError()) {
				i = idx;
				minError = ((SimpleKMeans) clusterers.get(idx)).getSquaredError();
			}
		}
		return clusterers.get(i);
	}
	
	public void parseExperimentFile(File expFile) {
		Reader reader = null;
		String line = "";
		try {
			reader = new FileReader(expFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LineNumberReader lineReader = new LineNumberReader(reader);
		try {
			line = lineReader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int i,j,k,s;
		while (line != null) {
			// Skip comments
			if(!line.trim().startsWith("#")) {
				String[] params = line.split(" ");
				i = Integer.parseInt(params[0]);
				j = Integer.parseInt(params[1]);
				k = Integer.parseInt(params[2]);
				if (params.length > 3)
					s = Integer.parseInt(params[3]);
				else
					s = 0;
				Experiment exp = new Experiment();
				exp.setExperiment(s,i, j, k);
				this.m_Experiments.add(exp);
			}
			try {
				line = lineReader.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void printCentroids(Instances centroids, int topN) {
		int cnt = centroids.numInstances();
		for (int i=0; i<cnt; ++i) {
			Instance inst = centroids.instance(i);
			Map<Double,String> topics = m_AM.topicWordRanking(inst,topN);
			output("Topic "+i+":\n"+topics.toString());
		}
	}
}
