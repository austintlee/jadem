package taglearner.TextExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import taglearner.ArticleManager.ArticleManager;
import taglearner.ExperimentManager.ExperimentManager;
import taglearner.ExperimentManager.PerfTimer;
import taglearner.InputManager.InputManager;
import taglearner.OutputManager.OutputManager;
import taglearner.ResultAnalyzer.Analyzer;
import taglearner.ResultAnalyzer.ChronologicalAnalyzer;
import weka.core.Utils;

public class TextExtractor {

	private ExperimentManager em = new ExperimentManager();
	private String curWorkingDir;
	private boolean loadFromSaved = false;
	
	public ExperimentManager getEM() { return this.em; }
	
	public void setWorkingDir(String dir) {
		this.curWorkingDir = dir;
	}
	
	public void setLoadMode(boolean b) {
		this.loadFromSaved = b;
	}
	
	public void run(ArticleManager am) {
		
		InputManager im = am.getInputManager();
		em.setRunList(im.getArticleXML(im.getInputFile(), 2));
		am.setExperimentManager(em);
		
		if (am.getIndexMode()) {
			em.setDates(0,-1);
			am.run(em);
			return;
		}

		em.setOutputFile(curWorkingDir+File.separator+"experiment.txt");
		em.run(am);
		
		/*
		int j=3;
		int k=3;
		int i;
		for (i=0; i<=2; i+=k) {
			
			em.setDates(i,j);
			em.setOutputFile(this.curWorkingDir+File.separator+"kmeans-"+i+"-"+j);
			am.run(em);
			em.clearRunList();
		}
		
		j = 3;
		k = 2;
		for (i=0; i<=57; i+=k) {
			
			em.setDates(i,j);
			em.setOutputFile(this.curWorkingDir+File.separator+"kmeans-"+i+"-"+j);
			am.run(em);
			em.clearRunList();
		}
		*/
	}
	
	public static void printUsage() {
		System.out.println("Usage:");
		System.out.println("java -jar TextExtractor.jar taglearner.TextExtractor <param1> ...");
		System.out.println();
		System.out.println("Command line classification mode");
		System.out.println("param1 = <training set input directory");
		System.out.println("param2 = <training set output directory");
		System.out.println("param3 = <test set input directory");
		System.out.println("param4 = <test set output directory");
		System.out.println("param5 = category | date | binary | binary_svm | all (sort by)");
		System.out.println("param6 = headline | fulltext | both");
		System.out.println("param7 = text | csv (output type)");
		System.out.println("param8 = \"true\" (true = balance +1 and -1 examples in the training set");
		System.out.println("param9 = log filename (one chosen if none given");
	}
	
	public ArticleManager setUpAM(String dataRoot, InputManager in, OutputManager out, Logger log) {
		File amFile = new File(dataRoot+File.separator+"db"+File.separator+"am.dbobj");
		ArticleManager am = null;
		
		if (loadFromSaved && amFile.exists()) {
			FileInputStream fis = null;
			ObjectInputStream ois = null;
			Date t1, t2;
			long duration;
			System.out.println("Loading from saved serialized object");
			
			try {
				fis = new FileInputStream(amFile);
				ois = new ObjectInputStream(fis);
				t1 = new Date();
				am = (ArticleManager) ois.readObject();
				t2 = new Date();
				duration = t2.getTime() - t1.getTime();
				System.out.println("amObj file size: "+amFile.length()/1000+"KB");
				System.out.println("amObj load time: "+duration+"(ms)");
				ois.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			am.setInputManager(in);
			am.setOutputManager(out);
		}
		else {
			am = new ArticleManager(dataRoot,in,out,log);
		}
		
		return am;
	}
	
	/*
	 *  create = true to generate the .dbobj even if one with same name already exists
	 */
	public void saveAM(ArticleManager am, File amFile, boolean create) {
		FileOutputStream fos = null;
		ObjectOutputStream oout = null;
		
		if (amFile.exists()) {
			if (!create)
				return;
			amFile.renameTo(new File(amFile.getAbsolutePath()+".old"));
		}
		else {
			amFile.getParentFile().mkdirs();
		}
		
		try {
			fos = new FileOutputStream(amFile);
			oout = new ObjectOutputStream(fos);
			oout.writeObject(am);
			oout.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	/*
	private Set<String> getStopWords(String filename) {
		BufferedReader bufRdr = null;
		HashSet<String> stopList = new HashSet<String>();
		File stop = new File(filename);
		String line = null;

		try {
			bufRdr = new BufferedReader(new FileReader(stop));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			line = bufRdr.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		StringTokenizer st = new StringTokenizer(line,",");
		while (st.hasMoreElements())
			stopList.add(st.nextToken());
		
		System.out.println("Size of stopword list = "+stopList.size());
		
		return stopList;
	}*/
	
	/**
	 * 
	 * @param argv
	 * 			1 - article XML or directory that contains (subdirectories that contain) article XML(s)
	 * 			2 - Top level output directory
	 * 			3 - date = sort output by date
	 * 			  - category = sort output by category
	 * 			4 - headline = output headlines only
	 * 			  - fulltext = output full text of articles
	 * 			5 - log-on = turn logging on, writes taglearner.log in the working directory
	 * @throws Exception 
	 */
	public static void main(String[] argv) {
		
		ArticleManager am, am2;
		InputManager in = null;
		OutputManager out = null;
		InputManager in2 = null;
		OutputManager out2 = null;
		TextExtractor te = new TextExtractor();
		boolean balanced = false;        // do not balance positive examples and negative examples by default
		String log = null;
		File amFile = null; 
		String runMode = "index";
		String optFlag, optStr;
		String dataRoot = null;
		String savedInst = null;
		String toSave = null;
		String trainList = null;
		String testList  = null;
		boolean truncate = false;
		boolean useCSV = false;
		String csv = null;
		int noiseReduction = 0;  // 0 default, all levels of noise reduction
		boolean removeCategories = true;  
		String removeCategoryList = null;
		String experimentMode = "train";    // train only by default
		String sortBy = null;
		String oMode = null;
		String oType = null;
		String trainInputDir = null;
		String trainOutputDir = null;
		String testInputDir = null;
		String testOutputDir = null;
		//String stopWordList = null;
		boolean useCustomStopList = false;
		String outputDir = null;
		String dataType = "train";
		int numClusters = 0;
		boolean updateKmeans = false;
		boolean stats = false;
		boolean useSavedObj = false;
		String wekaCmd = null;
		String optFile = null;
		String expFile = null;
		File optFileFile = null;
		File expFileFile = null;
		boolean stem = false;
		int numRandInit = 1;
		String resDir = null;
		
		try {
			optStr = Utils.getOption('m', argv);
			if (optStr.length() != 0)
				runMode = optStr;
			
			optStr = Utils.getOption('n', argv);
			if (optStr.length() != 0)
				noiseReduction = Integer.parseInt(optStr);
		
			optStr = Utils.getOption("-log", argv);
			if (optStr.length() != 0)
				log = optStr;
			else
				log = "taglearner.log";
			
			optStr = Utils.getOption('r', argv);
			if (optStr.length() != 0) {
				dataRoot = optStr;
				outputDir = dataRoot.concat(File.separator+"out");  // default to this for now
				te.setWorkingDir(dataRoot);
			}
			
			optStr = Utils.getOption('l', argv);
			if (optStr.length() != 0) {
				savedInst = optStr;
				useSavedObj = true;
			}
			
			optStr = Utils.getOption('s', argv);
			if (optStr.length() != 0)
				toSave = optStr;
			else
				toSave = dataRoot + File.separator + "db" + File.separator + "am.dbobj";
			
			optStr = Utils.getOption("-out", argv);
			if (optStr.length() != 0)
				outputDir = optStr;
			
			// If not given specifically, then train set = data set
			optStr = Utils.getOption('t', argv);
			if (optStr.length() != 0)
				trainList = optStr;
			else
				trainInputDir = dataRoot;
			
			// If test set is also given, then we assume both train and test
			optStr = Utils.getOption('T', argv);
			if (optStr.length() != 0) {
				testList = optStr;
				dataType = "both";
			}
			
			truncate = Utils.getFlag('x', argv);
			
			optStr = Utils.getOption("-csv", argv);
			if (optStr.length() != 0) {
				csv = optStr;
				useCSV = true;
			}				
			
			optStr = Utils.getOption("-rc", argv);
			if (optStr.length() != 0)
				removeCategoryList = optStr;
			else
				removeCategories = false;
			
			optStr = Utils.getOption("-sort", argv);
			if (optStr.length() != 0)
				sortBy = optStr;
			
			optStr = Utils.getOption("-omode", argv);
			if (optStr.length() != 0)
				oMode = optStr;
			
			optStr = Utils.getOption("-otype", argv);
			if (optStr.length() != 0)
				oType = optStr;
			
			/*optStr = Utils.getOption("-stop", argv);
			if (optStr.length() != 0) {
				stopWordList = optStr;
				useCustomStopList = true;
			}*/
			
			/*optStr = Utils.getOption("-kmeansk", argv);
			if (optStr.length() != 0) {
				numClusters = Integer.parseInt(optStr);
				updateKmeans = true;
			}*/
			
			stats = Utils.getFlag("-stats", argv);
				
			optStr = Utils.getOption("-weka", argv);
			if (optStr.length() != 0) {
				wekaCmd = optStr;
			}
			
			optStr = Utils.getOption("-of", argv);
			if (optStr.length() != 0) {
				optFile = optStr;
				optFileFile = new File(optFile);
				if (!optFileFile.exists()) {
					System.out.println("Weka opt file not found");
					return;
				}
			}
			
			optStr = Utils.getOption("-ef", argv);
			if (optStr.length() != 0) {
				expFile = optStr;
				expFileFile = new File(expFile);
				if (!expFileFile.exists()) {
					System.out.println("Weka experiment file not found");
					return;
				}
			}
			
			// Performance information logging
			if (Utils.getFlag("-pl", argv)) {
				PerfTimer.SetPerfLog(new File(dataRoot+File.separator+"perf.log"));
				PerfTimer.ToggleLogging(true);
			}
			else
				PerfTimer.ToggleLogging(false);
			
			if (Utils.getFlag("-stem", argv))
				stem = true;
			
			optStr = Utils.getOption("-numInit", argv);
			if (optStr.length() != 0) {
				numRandInit = Integer.parseInt(optStr);
				if (numRandInit < 1)
					numRandInit = 1;
				// don't do this more than 10 times per run
				else if (numRandInit > 10)
					numRandInit = 10;
			}
			
			optStr = Utils.getOption("-res", argv);
			if (optStr.length() != 0) {
				resDir = optStr;
			}
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String curDir = System.getProperty("user.dir");
		log = curDir + File.separator + log;
		Logger logger = Logger.getLogger(log,null);
		Handler h = null;

		try {
			h = new FileHandler(log,true);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.addHandler((Handler) h);
		
		if (argv.length < 2) {
			System.out.println("Not enough parameters!");
			return;
		}

		// For testing GUI mode
		if (runMode.equals("gui")) {
			am = null;
			amFile = new File(dataRoot+File.separator+"db"+File.separator+"am.dbobj");
			if (amFile.exists()) {
				FileInputStream fis = null;
				ObjectInputStream ois = null;
				try {
					fis = new FileInputStream(amFile);
					ois = new ObjectInputStream(fis);
					am = (ArticleManager) ois.readObject();
					
					ois.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				System.out.println("# Issues: "+am.getIssueList().length);
			}
			else {
				in = new InputManager(dataRoot);

				am  = new ArticleManager(dataRoot,in,out,logger);
				te = new TextExtractor();
				te.run(am);

				FileOutputStream fos = null;
				ObjectOutputStream oout = null;
				try {
					fos = new FileOutputStream(amFile);
					oout = new ObjectOutputStream(fos);
					oout.writeObject(am);
					oout.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			return;
		}
		
		if (runMode.equals("bow")) {
			am = null;
			in = new InputManager(dataRoot);
			amFile = new File(dataRoot+File.separator+"db"+File.separator+"am.dbobj");
			boolean update = false;
			if (amFile.exists()) {
				FileInputStream fis = null;
				ObjectInputStream ois = null;
				Date t1, t2;
				long duration;
				try {
					fis = new FileInputStream(amFile);
					ois = new ObjectInputStream(fis);
					t1 = new Date();
					am = (ArticleManager) ois.readObject();
					t2 = new Date();
					duration = t2.getTime() - t1.getTime();
					System.out.println("amObj file size: "+amFile.length()/1000+"KB");
					System.out.println("amObj load time: "+duration+"(ms)");
					ois.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				am.setInputManager(in);
				am.setOutputManager(null);
			}
			else {
				am  = new ArticleManager(dataRoot,true,in,out,logger);
				update = true;
			}
			
			te = new TextExtractor();
			te.run(am);
			
			if (update) {
				FileOutputStream fos = null;
				ObjectOutputStream oout = null;
				try {
					fos = new FileOutputStream(amFile);
					oout = new ObjectOutputStream(fos);
					oout.writeObject(am);
					oout.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}			
			return;
		}
		
		if (runMode.equals("csv")) {
			
			boolean update = false;
			
			if (argv.length < 8)
				printUsage();

			if (argv.length > 8)
				log = argv[8];

			in = new InputManager(trainInputDir);
			in.setCSVFilename(csv);
			in.readRemoveList(removeCategoryList);
			out = new OutputManager(oMode,sortBy,oType,balanced);
			out.initialize(trainOutputDir);

			te = new TextExtractor();
			am = null;
			am  = te.setUpAM(trainInputDir, in, out, logger);
			am.setIndexMode(true);
			am.setCSVMode(false);
			am.setRemoveCategories(true);
			am.setTruncateText(false);
			am.setDataType("train");
			am.setNoiseReduction(true);
			BufferedReader bufRdr = null;
			File stop = new File("/Users/austin/classes/6901/news_data/common-english-words.txt");
			try {
				bufRdr = new BufferedReader(new FileReader(stop));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			HashSet<String> stopList = new HashSet<String>();
			String line = null;
			try {
				line = bufRdr.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			StringTokenizer st = new StringTokenizer(line,",");
			while (st.hasMoreElements())
				stopList.add(st.nextToken());
			//am.setStopWordList(stopList);
			
			System.out.println("Input: "+trainInputDir);
			System.out.println("Output: "+trainOutputDir);
			System.out.println("Sort by: "+sortBy);
			System.out.println("Output Mode: "+oMode);
			System.out.println("Logging mode: "+log);

			// Build training data
			te.run(am);
			
			//amFile = new File(trainInputDir+File.separator+"db"+File.separator+"am.dbobj");
			//if (am.isUpdated())
			//	te.saveAM(am, amFile);
			
			return;
		}
		
		if (runMode.equals("index")) {

			in = new InputManager(dataRoot);
			//in.setCSVFilename(csv);
			
			if (removeCategories)
				in.readRemoveList(removeCategoryList);
			
			//out = new OutputManager(oMode,sortBy,oType,balanced);
			//out.initialize(trainOutputDir);

			am  = te.setUpAM(dataRoot, in, null, logger);
			am.setIndexMode(true);
			am.setCSVMode(false);
			if (removeCategories)
				am.setRemoveCategories(true);
			am.setTruncateText(false);
			am.setDataType(dataType);
			am.setNoiseReduction(true);
			am.setPrintStats(stats);
			
			//HashSet<String> stopList = (HashSet<String>) te.getStopWords(stopWordList);
			//am.setStopWordList(stopList);
			
			System.out.println("Input: "+trainInputDir);
			System.out.println("Output: "+trainOutputDir);
			System.out.println("Sort by: "+sortBy);
			System.out.println("Output Mode: "+oMode);
			System.out.println("Logging mode: "+log);

			// Build training data
			te.run(am);
			
			if (toSave.length() > 0) {
				amFile = new File(toSave);
				amFile.getParentFile().mkdirs();
				te.saveAM(am, amFile, true);
			}			

			return;
		}

		if (runMode.equals("extract")) {

			String outDir = dataRoot+File.separator+"full_text";
			in = new InputManager(dataRoot);
			//in.setCSVFilename(csv);
			
			if (removeCategories)
				in.readRemoveList(removeCategoryList);
			
			out = new OutputManager("fulltext","date","text",false);
			out.initialize(outDir);

			am  = te.setUpAM(dataRoot, in, out, logger);
			am.setIndexMode(true);
			am.setCSVMode(false);
			if (removeCategories)
				am.setRemoveCategories(true);
			am.setTruncateText(false);
			am.setDataType(dataType);
			if (noiseReduction == 0)
				am.setNoiseReduction(false);
			else
				am.setNoiseReduction(true);
			am.setDataType("train");
			am.setPrintStats(stats);
			
			//HashSet<String> stopList = (HashSet<String>) te.getStopWords(stopWordList);
			//am.setStopWordList(stopList);
			
			System.out.println("Input: "+dataRoot);
			System.out.println("Output: "+outDir);
			System.out.println("Sort by: "+sortBy);
			System.out.println("Output Mode: "+oMode);
			System.out.println("Logging mode: "+log);

			// Build training data
			te.run(am);
			
			if (toSave.length() > 0) {
				amFile = new File(toSave);
				amFile.getParentFile().mkdirs();
				te.saveAM(am, amFile, true);
			}			

			return;
		}
		
		if (runMode.equals("weka")) {
			in = new InputManager(dataRoot);
			//in.setCSVFilename(csv);
			in.readRemoveList(removeCategoryList);
			//out = new OutputManager(oMode,sortBy,oType,balanced);
			//out.initialize(trainOutputDir);

			te.setLoadMode(useSavedObj);
			am  = te.setUpAM(dataRoot, in, null, logger);
			am.setIndexMode(false);
			am.setCSVMode(false);
			am.setRemoveCategories(true);
			am.setTruncateText(false);
			am.setDataType(dataType);
			am.setNoiseReduction(true);
			am.setKmeansNumClusters(numClusters);
			am.setUpdateKmeans(updateKmeans);
			am.setPrintStats(stats);
			am.setKmeansNumRandInits(numRandInit);
			
			//HashSet<String> stopList = (HashSet<String>) te.getStopWords(stopWordList);
			//am.setStopWordList(stopList);

			// Build training data
			te.setWorkingDir(dataRoot);
			
			ExperimentManager myEM = te.getEM();
			myEM.setWekaCmd(wekaCmd);
			myEM.setWekaOpt(optFileFile);
			myEM.parseExperimentFile(expFileFile);
			
			te.run(am);
		}
		
		if (runMode.equals("analyze")) {
			ChronologicalAnalyzer analyzer = new ChronologicalAnalyzer(resDir);
			//analyzer.setResDir(resDir);
			analyzer.load();
			analyzer.analyze();
		}
		PerfTimer.ClosePerfLogStream();
		return;
	}// end of main
}
