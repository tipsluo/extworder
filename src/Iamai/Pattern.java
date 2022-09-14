package Iamai;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import extverifier.Extverifier;
import extworder.Validation.DocStat;

public class Pattern {
	DocStat patternDocStat;
	ArrayList<DocStat> docStats;
	
	public Pattern() {
		docStats=new ArrayList<DocStat>();
	}
	
	public void addDocStat(DocStat docStat) {
		docStats.add(docStat);
	}
	
	public void savePattern() throws FileNotFoundException, IOException {
		patternDocStat.save(Iamai._DocStatFilename);
	}
	
	public void loadPattern() throws FileNotFoundException, IOException, ClassNotFoundException {
		patternDocStat.load(Iamai._DocStatFilename);
	}
	
	public void updateDocStat() {
		patternDocStat=new DocStat();
		
		if(docStats.size()==0)
			return;
		
		int titleCount=0;
		int bodyCount=0;
		
		for(DocStat docStat:docStats) {
			if( docStat.totalWordCount<=0 || docStat.bodyWordCount<=0 )
				continue;
			
			patternDocStat.bodyWordCount+=docStat.bodyWordCount;
			patternDocStat.totalWordCount+=docStat.totalWordCount;
			patternDocStat.titleOffset+=docStat.titleOffset;
			patternDocStat.firstBodyOffset+=docStat.firstBodyOffset;
			patternDocStat.bodyWordRatio+=docStat.bodyWordRatio;
			
			if(docStat.titleOffset>=0)
				titleCount++;
			if(docStat.firstBodyOffset>=0)
				bodyCount++;
		}
		
		patternDocStat.bodyWordCount = patternDocStat.bodyWordCount / docStats.size();
		patternDocStat.totalWordCount = patternDocStat.totalWordCount / docStats.size();
		
		if(titleCount>0)
			patternDocStat.titleOffset = patternDocStat.titleOffset / titleCount;
		if(bodyCount>0) {
			patternDocStat.firstBodyOffset = patternDocStat.firstBodyOffset / bodyCount;
			patternDocStat.bodyWordRatio = patternDocStat.bodyWordRatio / docStats.size();
		}
	}
}
