package taglearner.OutputManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import taglearner.ArticleManager.ArticleManager;

public class ObjectDataManager {
	
	private File root;
	
	public ObjectDataManager(File rootDir) {
		this.root = rootDir;
	}
	
	public void saveAM(ArticleManager am) {
		FileOutputStream fos = null;
		ObjectOutputStream oout = null;
		File amFile = new File(root.getAbsolutePath()+File.separator+"db"+File.separator+"am.dbobj");
		if (amFile.exists()) {
			File old = new File(root.getAbsolutePath()+File.separator+"db"+File.separator+"am.dbobj.old");
			amFile.renameTo(old);
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
}
