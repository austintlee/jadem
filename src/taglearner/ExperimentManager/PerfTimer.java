package taglearner.ExperimentManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PerfTimer {
	public static long tic, toc;
	public static boolean logging = false;
	public static File perfLogFile;
	public static FileWriter logWriter;
	public static double time;
	
	public static void ToggleLogging(boolean on) {
		logging = on;
	}
	
	public static void SetPerfLog(File f) {
		perfLogFile = f;
		try {
			logWriter = new FileWriter(perfLogFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void ClosePerfLogStream() {
		
		if (logging) {
			try {
				logWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void Start() {
		tic = System.currentTimeMillis();
	}
	
	public static void Stop() {
		toc = System.currentTimeMillis();
	}
	
	public static void PrintPerf(String msg) {
		time = (double) (toc-tic)/1000;
		String output = msg+" time: "+Double.toString(time)+"s";
		System.out.println(output);
		
		if (logging) {
			try {
				logWriter.append(output+"\n");
				logWriter.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void PrintMsg(String msg) {
		System.out.println(msg);
		if (logging) {
			try {
				logWriter.append(msg+"\n");
				logWriter.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
