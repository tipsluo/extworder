package extworder;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Validation {
/*	Content content;
	String patternFilename;
	DocStat patternDocStat;
	
	public Validation(Content content, DocStat patternDocStat) {
		this.content=content;
		this.patternDocStat=patternDocStat;
	}
	
	public Validation(Content content, String patternFilename) {
		this.content=content;
		this.patternFilename=patternFilename;
		
		DocStat docStat=new DocStat(content);
	}

	static boolean verifyRow(Row row) {
		if(row.chars.size()<2)
			return true;
		for(Char ch1:row.chars)
			for(Char ch2:row.chars) {
				if(ch1==ch2 || ch1.vIntersected(ch2))
					continue;
				if(row.charfont.height!=ch1.height || row.charfont.height != ch2.height)
					continue;
				if(ch1.hIntersected(ch2))
					return false;
			}
			
		return true;
	}
	
	static public class DocStat implements Serializable  {
		Content content;
		public int bodyWordCount;
		public int totalWordCount;
		public int titleOffset;
		public int firstBodyOffset;
		public int bodyWordRatio;
		
		public DocStat(Content content) {
			this.content=content;
			
			bodyWordCount=totalWordCount=0;
			titleOffset=firstBodyOffset=-1;
			bodyWordRatio=-1;
			
			stat();
		}
		
		public void save(String filename) {	
			try {		    
				 ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
				 out.writeObject(this);
				 out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void load(String filename) throws FileNotFoundException, IOException, ClassNotFoundException {
			FileInputStream fis = new FileInputStream(filename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			DocStat ds=(DocStat) ois.readObject();
			ois.close();
			
			copyDocStat(ds);
		}
		
		private void copyDocStat(DocStat ds) {
			bodyWordCount=ds.bodyWordCount;
			totalWordCount=ds.totalWordCount;
			titleOffset=ds.titleOffset;
			firstBodyOffset=ds.firstBodyOffset;
			bodyWordRatio=ds.bodyWordRatio;
		}
		
		void stat() {
			int firstBodyIndex=-1,titleIndex=-1;
			
			for(Page page: content.pages)
				for(Block block:page.blocks) {
					if(block==content.titleBlock) {
						//titleInde equal to the word count so far.
						titleIndex=totalWordCount;
					}

					String[] words = block.string().split("\\s+");
					
					if(Block.bodyBlockFilter.filter(block)) {
						if(firstBodyIndex<0)
							firstBodyIndex=totalWordCount;
						
						bodyWordCount+=words.length;
					}
					
					totalWordCount+=words.length;
				}
			
			if( totalWordCount<=0 || bodyWordCount<=0 )
				return;
			
			titleOffset=(int)(titleIndex*10000 / totalWordCount);
			firstBodyOffset=(int)(firstBodyIndex * 10000 / totalWordCount);
			bodyWordRatio=(int)(bodyWordCount * 10000 / totalWordCount);
		}
	}*/
}
