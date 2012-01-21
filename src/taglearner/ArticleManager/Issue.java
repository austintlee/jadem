package taglearner.ArticleManager;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/*
 * Represents a newspaper
 */
public class Issue implements Serializable {

	private String name;
	private String filename;
	private Map<String,Article> articleMap = new HashMap<String,Article>();
	private List<Article> articleList = new ArrayList<Article>();
	private List<File> ocrList = new ArrayList<File>();
	private List<File> tiffList = new ArrayList<File>();
	private ArrayList<HashMap<String,TextBlock>> listOfTextBlocks = new ArrayList<HashMap<String,TextBlock>>();
	
	public Issue() {}
	
	public Issue(String issueName) {
		setName(issueName);
	}
	
	public Issue(Issue src) {
		this.name = src.getName();
		this.articleMap.putAll(src.getArticleMap());
		this.articleList.addAll(src.getArticles());
		this.ocrList.addAll(src.getOCRs());
		this.tiffList.addAll(src.getTiffs());
		this.listOfTextBlocks.addAll(src.listOfTextBlocks);
		this.filename = src.getFilename();
	}
	
	public void insertNewTextBlockMap(int pageNum, HashMap<String,TextBlock> tb) {
		//this.listOfTextBlocks.ensureCapacity(pageNum+1);
		this.listOfTextBlocks.add(pageNum, tb);
	}
	
	public void insertNewTextBlock(int pageNum, TextBlock tb) {
		HashMap<String,TextBlock> tmp = this.listOfTextBlocks.get(pageNum);
		tmp.put(tb.getID(), tb);
		this.listOfTextBlocks.set(pageNum, tmp);
	}
		
	public void setName(String issueName) { 
		String newName = issueName;
		newName = newName.toLowerCase();
		newName = newName.replaceAll(",", "_");
		newName = newName.replaceAll("-", "_");
		newName = newName.replaceAll(" ", "_");
		this.name = newName; 
	}
	public String getName() { return this.name; }
	
	public void setFilename(String name) { this.filename = name; }
	public String getFilename() { return this.filename; }
	
	public Article getArticle(String id) {
		return this.articleMap.get(id);
	}
	
	public Map<String,Article> getArticleMap() {
		return this.articleMap;
	}
	public List<Article> getArticles() {
		return this.articleList;
	}
	public List<File> getOCRs() {
		return this.ocrList;
	}
	public List<File> getTiffs() {
		return this.tiffList;
	}
	public Article getArticle(int index) {
		return this.articleList.get(index);
	}
	
	public ListIterator<Article> getArticleList() {
		return this.articleList.listIterator();
	}
	
	public ListIterator<File> getOCRListIterator() {
		return this.ocrList.listIterator();
	}
	
	public void addToOCRList(File ocr) {
		this.ocrList.add(ocr);
		String tif = ocr.getAbsolutePath().replace(".xml", ".tif");
		this.tiffList.add(new File(tif));
	}
	
	/*
	public void addToArticleMap(String id, Article a) {
		this.articleMap.put(id, a);
	}
	
	public void addToArticleList(Article a) {
		this.articleList.add(a);
	}
	*/
	
	public void addNewArticle(Article a) {
		this.articleMap.put(a.getID(), a);
		this.articleList.add(a);
	}
	/*
	 *  @return # of articles in this issue
	 */
	public int size() {
		return this.articleList.size();
	}
	
	public int pageIndexOfTextBlock(String id) {
		int pg = -2;
		HashMap<String,TextBlock> map;
		for (int i=0; i < this.listOfTextBlocks.size(); i++) {
			map = this.listOfTextBlocks.get(i);
			if (map.containsKey(id))
				pg = i;
		}
		return pg;
	}
	
	public void clear() {
		this.name = "";
		this.ocrList.clear();
		this.articleMap.clear();
		this.articleList.clear();
	}
}
