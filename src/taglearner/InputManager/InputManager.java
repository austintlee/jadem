package taglearner.InputManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputManager {

	private File inputFile;
	private String csvFilename;
	private Map<String,String> labels = new HashMap<String,String>();
	private List<String> removeList = new ArrayList<String>();
	
	private Calendar begin, end;
	
	public InputManager(String input) {
		inputFile = new File(input);
		
		begin = new GregorianCalendar();
		end   = new GregorianCalendar();
	}
	
	public File getInputFile() { return this.inputFile; }
	public void setInputFile(String input) { this.inputFile = new File(input); }
	public void setInputFile(File f) { this.inputFile = f; }
	public void setCSVFilename(String f) { this.csvFilename = f; }
	
	public Map<String,String> getLabels() { return this.labels; }
	public List<String> getRemoveList() { return this.removeList; }
	
	public ArrayList<File> getArticleXML(File dir, int depth) {
		
		if (depth == 0)
			return null;
		
		ArrayList<File> xmlList = new ArrayList<File>();

		if (dir.isFile()) {
			xmlList.add(dir);
			return xmlList;
		}
		
		if (!dir.isDirectory())
			return null;
		
		File[] fileList = dir.listFiles();
		
		for (int i=0 ; i < fileList.length ; i++) {
			if (fileList[i].isFile()) {

				// Is this enough not to grab wrong files?  Probably
				if (fileList[i].getName().startsWith("articles_")
						&& fileList[i].getName().endsWith(".xml")) {
					
					if (null == xmlList)
						xmlList = new ArrayList<File>();
					
					xmlList.add(fileList[i]);
					continue;
				}
			}
			
			// If directory, then recursively get article XMLs
			if (fileList[i].isDirectory()) {
				ArrayList<File> temp = getArticleXML(fileList[i],(depth-1));
				if (null != temp) {
					xmlList.addAll(temp);
				}
			}
		}
		
		return xmlList;
	}
	
	public void updateDateRange(ArrayList<File> xmls) {
		Calendar d = new GregorianCalendar();
		Pattern p = Pattern.compile("(\\d\\d\\d\\d)(\\d\\d)(\\d\\d)(\\d\\d)");
		boolean first = true;
		for (File f : xmls) {
			String base = f.getParentFile().getName();
			Matcher m = p.matcher(base);
			
			String year, month, date;
			if (m.find()) {
				year = base.substring(m.start(1), m.end(1));
				month = base.substring(m.start(2), m.end(2));
				date = base.substring(m.start(3), m.end(3));
			
				d.set(Calendar.YEAR,Integer.parseInt(year));
				d.set(Calendar.MONTH,Integer.parseInt(month));
				d.set(Calendar.DAY_OF_MONTH,Integer.parseInt(date));
				
				System.out.println("Date: "+d.toString());
				
				if (first) {
					this.begin.set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DATE));
					this.end.set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DATE));
					first = false;
				}
				else {
					if (d.before(this.begin))
						this.begin.set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DATE));
					else if (d.after(this.end))
						this.end.set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DATE));
				}
			}
		}
	}
	
	public void readLabelsFromCSV() {
		File csv = new File(this.csvFilename);
		FileReader fr = null;
		LineNumberReader lineReader = null;
		String line = null;
		String[] tokens = null;
		
		try {
			fr = new FileReader(csv);
			lineReader = new LineNumberReader(fr);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// First line is the header so skip it
		try {
			line = lineReader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			while (null != (line = lineReader.readLine())) {
				tokens = line.split(",");
				this.labels.put(tokens[0], tokens[9]);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			fr.close();
			lineReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void readRemoveList(String f) {
		File list = new File(f);
		FileReader fr = null;
		LineNumberReader lineReader = null;
		String line = null;
		
		try {
			fr = new FileReader(list);
			lineReader = new LineNumberReader(fr);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			while (null != (line = lineReader.readLine())) {
				this.removeList.add(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			fr.close();
			lineReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
