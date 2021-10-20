package extworder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import extworder.Block.BlockFormat;

public class Common {
	public final static int _KeyBlockWordPageRation=10;
	public final static int _MinKeyBlockWordNum=50;	
	
	final static float _MinAbstractFreqencyRatio=0.65f;
	final static float _MinAbstractSentenceRatio=0.3f;
	final static float _MinTitleFreqencyRatio=0.65f;
	
	final static String _TestDataDir="data/";
	
	final static float _SpaceAdjustment=0.8f;
	final static int _CharHSpaceAddGap=2;
	final static float _CharHGapRatio=3.5f;
	final static float _CharVGapRatio=2.0f;
	final static float _MaxBorderExpandRatio=2.0f;
	final static float _HSpaceMin=0.3f;
	final static float _SameBlockRowWidthDiff=0.1f;
	final static float _MaxSameRowDistanceRatio=2f;
	final static int _CharLeftAdjustment=2;
	final static int _MinBigTextBlockFirstRowLength=5;
	final static float _FirstLineIndentRatio=5f;
	
	final static float _MaxHeaderFooterWidthRatio=0.5f;
	final static float _BlockDisplaceRatio=1f;
	final static float _MaxHeaderFooterHeightRatio=0.1f;
	
	final static float _IgnoredColoredBlockRatio=2.0f;
	
	final static String _TitleBlock="TITLE";
	final static String _AbstractBlock="ABSTRACT";
	final static String _KeywordBlock="KEYWORD";
	final static String _PageHeaderBlock="PAGEHEADER";
	final static String _PageFooterBlock="PAGEFOOTER";
	final static String _BeforeFirstBody="BEFOREFIRSTBODY";
	//final static String _FirstBody="FIRSTBODY";
	final static String _Body="BODY";
	final static String _SubtitlePrefix="SUBTITLE_";
	final static String _IgnoredBlockPrefix="IGNORED_";
	final static String _IgnoredBlockIntraBody=_IgnoredBlockPrefix+"INTRABODY";
	final static String _IgnoredBlockColored=_IgnoredBlockPrefix+"COLORED";
	final static int _MaxCharVGapAdj=1;
	final static int _MinCharVGapAdj=-2;
	final static float _MaxInterBodyBlockGapRatio=4;
	final static float _MaxIntraBlockRowGapRatio=1.5f;
	
	final static int _ParaSentUnoNoTerm=1;
	final static int _ParaSentDefaultTrue=100;
	final static int _ParaSentDefaultFalse=-100;
	final static int _ParaSentDefaultUno=0;
	
	final static float _ColumnWidthAdjustment=0.05f;
	//final static float _ColumnMinWidthRatio=0.3f;
	
	final static float _MinBodyCharBlockWidth=0.70f;
	final static float _MinTitleLength=4;
	
	final static float _CenterAlignAdjustment=0.05f;
	
	final static int _MinTimeSubtitle=3;
	final static int _MinTimeAdditionalSubtitle=2;
	
	final static int _MaxTrivialColoredLength=5;
	
	final static float _MinUppercaseBlockRatio=0.5f;
	final static int _MinUppercaseBlockCount=5;
	
	final static int _LEFTALIGNED=-1;
	final static int _CENTERALIGNED=0;
	final static int _RIGHTALIGNED=1;
	final static int _UNKNOWNALIGNED=-98;
	final static int _NOALIGNED=-99;
	
	final static int _UNKNOWNINDENT=-98;
	final static int _ALLUPPERCASE=1;
	final static int _NOTALLUPPERCASE=0;
	
	final static int _LEFTORIENTED=-1;
	final static int _RIGHTORIENTED=1;
	
	final static boolean __DEBUG=false;
	
	
	final static Pattern infinishedBodyBlock;
	final static Pattern lowercaseExisting;
	final static Pattern uppercase;
	final static Pattern leading2Uppercase;
	final static Pattern scarceRow;
	final static Pattern likeSentence1,likeSentence2;
	final static Pattern terminated;
	
	/* May need it later
	final static Pattern bulletPart1;
	final static Pattern bulletPart2;*/
	
	static {
		infinishedBodyBlock=Pattern.compile("[a-zA-Z0-9,]$");
		lowercaseExisting=Pattern.compile("[a-z]");
		uppercase=Pattern.compile("[A-Z]");
		leading2Uppercase=Pattern.compile("^\\s*[A-Z]{2,}");
		scarceRow=Pattern.compile("^(\\S+\\s+){0,2}\\S*$");
		likeSentence1=Pattern.compile("(\\s+\\w+){2,}\\s*$");
		likeSentence2=Pattern.compile("(is|are|was|were|am|arn't|" +
				"wasn't|weren't|has|have|had|did|do|didn't|don't|doesn't" +
				"hadn't|hasn't|havn't|may|might|must|could|can|should|will|would)\\s+");
		terminated=Pattern.compile("[.,;?:\")]$");
	}
	
	public Common() {
	} 
	
	static int compareValue(float i1,float i2) {
		if (i1<i2)
			return -1;
		else if (i1>i2)
			return 1;
		else return 0;
	}
	
	static int compareValue(int i1,int i2) {
		return compareValue((float)i1,(float)i2);
	}
	
	static String prepareOut(String str) {
		str = str.replaceAll("[\r\n]+", "\n");
		str = str.replaceAll("\s+", "\s");
		
		return str;
	}
	
	static int subtitleLevel(String type) {
		Pattern p = Pattern.compile( _SubtitlePrefix+"(.*)" );
		Matcher m = p.matcher( type );
		if ( m.find() ) {
		   String s=m.group(1);
		   return Integer.parseInt(s);
		} else
			return -1;
	}
	
	static String subtitleBlockType(Block block) {
		return Common._SubtitlePrefix+Integer.toString(
					block.page.content.blockformatIndexes.get(block.format));
	}
	
	static ArrayList<String> getLetterWords(String str) {
		String[] ws = str.replaceAll("[\n\r,.\":;\\?&]"," ").split("\\s+");
		ArrayList<String> ret=new ArrayList<String>();
		Pattern alphabet=Pattern.compile("[a-zA-Z]");
		
		for (int i = 0; i < ws.length; i++) {
			if(alphabet.matcher(ws[i]).find())
				ret.add(ws[i].toLowerCase());
		}
			
		return ret;
	}
	
	static String[] getSentences(String str){
		String[] ws = str.toLowerCase().split("[\\.?;]");
		return ws;
	}
	
	static float hits(ArrayList<String> allWords, ArrayList<String> words) {
		if(words.size()==0)
			return -1f;
		
		int sum=0;
				
		for(int i=0; i<words.size(); i++)
			if(Collections.binarySearch(allWords,words.get(i)) >= 0)
				sum++;

		float ret=(float)sum/(float)words.size();
		return ret;
	}
	
	static float sentenceRatio(String str) {
		int likeSentence=0;
		String[] sents=Common.getSentences(str);
		for(String sent:sents) {
			if(Common.likeSentence1.matcher(sent).find() &&
					Common.likeSentence2.matcher(sent).find())
				likeSentence++;
		}
		return (float)likeSentence / sents.length;
	}
	
	static class RangeGroup {
		static class Range {
			int min,max;
			
			public Range(int value) {
				min=max=value;
			}
			
			public Range(int min,int max) {
				this.min=min;
				this.max=max;
			}
			
			boolean isIn(int value) {
				return value>=min && value<=max;
			}
			
			void add(int value) {
				if(min>value)
					min=value;
				if(max<value)
					max=value;
			}
			
			int length() {
				return max-min+1;
			}
		}
		
		ArrayList<Range> ranges;
		
		public RangeGroup(ArrayList<Integer> elements) {
			//elements should have been sorted
			
			ranges=new ArrayList<Range>();
			
			boolean bMin=false;
			Range range=new Range(-9999);
			int element1=-9999;
			for(int i=0; i<elements.size(); i++) {
				int element=elements.get(i);
				if(! bMin) {
					range.min=element;
					bMin=true;
				} else {
					if(element-element1 > 1) {
						range.max=element1;
						ranges.add(range);
						
						range=new Range(element);
					}
				}
				
				element1=element;
			}
			range.max=element1;
			ranges.add(range);
		}
		
		Range getRange(int value) {
			for(Range range:ranges)
				if(range.isIn(value))
					return range;
			
			return null;
		}
	}
	
	static class StatGroup<T> {
		HashMap<T,Integer> records;
		
		StatGroup() {
			records=new HashMap<T,Integer>();
		}
		
		void add(T t) {
			if(records.containsKey(t))
				records.put(t,records.get(t)+1);
			else
				records.put(t,1);
		}
		
		T maxByValue() {
			return records.entrySet().stream().
					max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).
					get().getKey();
		}
		
		Map<T,Integer> reverseSortByValue() {
			LinkedHashMap<T, Integer> reverseSortedRecords = new LinkedHashMap<>();
			
			records.entrySet()
		    .stream()
		    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) 
		    .forEachOrdered(x -> reverseSortedRecords.put(x.getKey(), x.getValue()));
			
			return reverseSortedRecords;
		}
		
		Map<T,Integer> sortByValue() {
			LinkedHashMap<T, Integer> sortedRecords = new LinkedHashMap<>();
			 
			records.entrySet()
			    .stream()
			    .sorted(Map.Entry.comparingByValue())
			    .forEachOrdered(x -> sortedRecords.put(x.getKey(), x.getValue()));
			
			return sortedRecords;
		}
	}
	
	interface BlockFilter {
		public boolean filter(Block block);
	}
	
	static class BodyBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block block) {
			if(block.column==null)
				return false;
			return block.type==Common._Body;
			//return block.likeBodyBlock()>=Common._ParaSentDefaultTrue;
			
			/*if(block.format.equals(block.page.content.bodyBlockformat)) {
				if(block.type==Common._BeforeFirstBody || block.column==null)
					return false;
				else {
					if(block.isParagraphBlock()<Common._ParaSentDefaultTrue)
						return false;
					
					if(block.isAllScarce())
						return false;
					
					if (block.rows.size()==1) {
						String lastStr=block.rows.get(block.rows.size()-1).string();
						
						if(infinishedBodyBlock.matcher(lastStr.trim()).find())
							return false;
						if( block.format.alignment==Common._NOALIGNED )
							return false;
						if(scarceRow.matcher(lastStr).find())
							return false;
					}
					
					return true;
				}
			}*/
			
			//return false;
		}
	}
	
	static class BigBlockFilter implements BlockFilter {
		final static Pattern pattern;
		
		static {
			pattern=Pattern.compile("^"+_IgnoredBlockPrefix);
		}
		
		@Override
		public boolean filter(Block block) {
			int charfontDiff=block.format.compareTo(block.page.content.bodyBlockformat);
			
			//if(charfontDiff>0 && block.isNonTitle())
			if(charfontDiff>0 && block.likeTitleBlock()<0)
				return false;
			
			return  ( charfontDiff >= 0 || 
						charfontDiff == 0 && 
						( block.format.alignment==Common._CENTERALIGNED)
					)
					&&
					! pattern.matcher(block.type).find();
		}
	}
	
	static class SubtitleBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block block) {
			return block.format.compareTo(block.page.content.bodyBlockformat) > 0 &&
					block.type.contains(_SubtitlePrefix);
		}
	}
	
	static class AdditionalSubtitleFormatFilter implements BlockFilter {
		private ArrayList<BlockFormat> blockformats;
		
		public AdditionalSubtitleFormatFilter(ArrayList<BlockFormat> blockformats) {
			this.blockformats=blockformats;
		}
		
		@Override
		public boolean filter(Block block) {
			for(BlockFormat blockformat: blockformats)
				if(blockformat.equals(block.format))
					return true;
				
			return false;
		}
	}
	
    static ArrayList<String> getAllPDFs() {
        ArrayList<String> files=new ArrayList<String>();
        
        File directoryPath = new File(_TestDataDir);
        String contents[] = directoryPath.list();
          
        for(int i=0; i<contents.length; i++) {
             if(contents[i].endsWith(".pdf")) {
                 files.add(contents[i].substring(0, contents[i].lastIndexOf('.')));
             }
        }
        
        return files;
    }
    
	static abstract public class IgnorePage {
		public abstract boolean isIgnored(Page page);
	}
	
	static class Stretch implements Comparable<Stretch> {
		protected int start;
		protected int end;
		
		public Stretch(int start,int end) {
			this.start=start;
			this.end=end;
		}
		
	    @Override
	    public int hashCode() {
	        return start<<10 + end;
	    }
	    
	    public int length() {
	    	return end-start;
	    }
		
		@Override
		public boolean equals(Object obj) {
			if (getClass() != obj.getClass())
	            return false;
			Stretch other = (Stretch) obj;
			return hashCode()==other.hashCode();
		}

		@Override
		public int compareTo(Stretch s) {
			return hashCode()-s.hashCode();
		}
		
		public void extend(int point) {
			if(point>end)
				end=point;
			if(point<start)
				start=point;
		}
		
		public void extend(Stretch stretch) {
			if(stretch.end>end)
				end=stretch.end;
			if(stretch.start<start)
				start=stretch.start;
		}
		
		public boolean contains(Stretch stretch) {
			return start<=stretch.start && end>=stretch.end;
		}
		
		public boolean contains(Stretch stretch,float adjustment) {
			return contains(stretch) && (length()-stretch.length())>adjustment;
		}
		
		public boolean intersected(Stretch stretch) {
			return (start >= stretch.start && start <= stretch.end) ||
					   (stretch.start >= start && stretch.end <= end);
		}
	}
}