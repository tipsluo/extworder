package extworder;

public class Validation {
	Content content;
	DocStat docStat;
	
	public Validation(Content content) {
		this.content=content;
		docStat=new DocStat();
		
		stat();
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
	
	void stat() {
		int firstBodyIndex=-1,titleIndex=-1;
		
		for(Page page: content.pages)
			for(Block block:page.blocks) {
				if(block==content.titleBlock) {
					titleIndex=docStat.totalWordCount;
				}

				String[] words = block.string().split("\\s+");
				
				if(Block.bodyBlockFilter.filter(block)) {
					if(firstBodyIndex<0)
						firstBodyIndex=docStat.totalWordCount;
					
					docStat.bodyWordCount+=words.length;
				}
				
				docStat.totalWordCount+=words.length;
			}
		
		if( docStat.totalWordCount<=0 || docStat.bodyWordCount<=0 )
			return;
		
		docStat.titleOffset=(int)(titleIndex*100 / docStat.totalWordCount);
		docStat.firstBodyOffset=(int)(firstBodyIndex * 100 / docStat.totalWordCount);
		docStat.bodyWordRatio=(int)(docStat.bodyWordCount * 100 / docStat.totalWordCount);
	}
	
	static public class DocStat {
		public int bodyWordCount;
		public int totalWordCount;
		public int titleOffset;
		public int firstBodyOffset;
		public int bodyWordRatio;
		
		public DocStat() {
			bodyWordCount=totalWordCount=0;
			titleOffset=firstBodyOffset=-1;
			bodyWordRatio=-1;
		}
	}
}
