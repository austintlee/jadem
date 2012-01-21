package taglearner.ArticleManager;

import java.io.Serializable;

//import java.util.ArrayList;
//import java.util.List;

public class Category implements Serializable {
	private String name;
	private int    id;
	
	//private static List<String> categories = new ArrayList<String>();
	//private static int id_ = 1;
	
	public void clear() {
		this.name = "";
		this.id = 0;
	}
	
	public void setName(String cat) {
		this.name = cat;
		/*if (Category.categories.contains(cat))
			this.id = Category.categories.indexOf(cat) + 1;
		else {
			Category.categories.add(cat);
			Category.id_++;
			this.id = Category.id_;
		}*/
	}
	public void setId(int i) {
		this.id = i;
	}
	
	public String getName() {
		return this.name;
	}
	public int getId() {
		return this.id;
	}
	
	@Override
	public final String toString() {
		return name;
	}
}
