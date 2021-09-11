package extworder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import extworder.Block.BlockFormat;

public class Common {
	/*public final static int _AbstractWordPageRatio=10;
	public final static int _KeywordWordPageRatio=10;
	public final static int _MinAbstractWordNum=50;
	public final static int _MinKeywordWordNum=50;*/
	public final static int _KeyBlockWordPageRation=10;
	public final static int _MinKeyBlockWordNum=50;	
	
	final static float _MinAbstractFreqencyRatio=0.65f;
	
	final static String _TestDataDir="data/";
	
	final static int _CharHGapSpaceTimes=2;
	final static int _CharHGap=10;
	//final static float _CharHGapRatio=1f;
	final static float _CharVGapRatio=1.5f;
	final static float _HSpaceMin=0.3f;
	final static float _SameBlockRowWidthDiff=0.1f;

	
	final static float _MaxHeaderFooterWidthRatio=0.5f;
	final static float _BlockDisplaceRatio=1f;
	final static float _MaxHeaderFooterHeightRatio=0.1f;
	
	final static float _IgnoredColoredBlockRatio=2.0f;
	
	final static String _TitleBlock="TITLE";
	final static String _AbstractBlock="ABSTRACT";
	final static String _KeywordBlock="KEYWORD";
	final static String _PageHeaderBlock="PAGEHEADER";
	final static String _PageFooterBlock="PAGEFOOTER";
	final static String _BeforeFirstText="BEFOREFIRSTTEXT";
	final static String _FirstText="FIRSTTEXT";
	final static String _SubtitlePrefix="SUBTITLE_";
	final static String _IgnoredBlockPrefix="IGNORED_";
	final static String _IgnoredBlockIntraText=_IgnoredBlockPrefix+"INTRATEXT";
	final static String _IgnoredBlockColored=_IgnoredBlockPrefix+"COLORED";
	
	
	final static float _ColumnWidthAdjustment=0.05f;
	//final static float _ColumnMinWidthRatio=0.3f;
	
	final static float _MinTextCharBlockWidth=0.70f;
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
	final static int _CENTERALIGNEDWIINDENT=2;
	final static int _UNKNOWNALIGNED=-98;
	final static int _NOALIGNED=-99;
	
	final static int _UNKNOWNINDENT=-98;
	final static int _UNKNOWNALLUPPERCASE=-98;
	
	final static boolean __DEBUG=false;
	
	final static Pattern infinishedTextBlock;
	final static Pattern lowercaseExisting;
	final static Pattern uppercase;
	final static Pattern leading2Uppercase;
	final static Pattern scarceRow;
	
	/* May need it later
	final static Pattern bulletPart1;
	final static Pattern bulletPart2;*/
	
	static {
		infinishedTextBlock=Pattern.compile("[a-zA-Z0-9,]$");
		lowercaseExisting=Pattern.compile("[a-z]");
		uppercase=Pattern.compile("[A-Z]");
		leading2Uppercase=Pattern.compile("^\\s*[A-Z]{2,}");
		scarceRow=Pattern.compile("\\S+\s{3,}\\S");
		
		/* May need it later
		bulletPart1=Pattern.compile("^\\s*([a-zA-Z][.])?(.*)");
		bulletPart2=Pattern.compile("^\s*([ivxIVX]*[.])?(.*)");*/
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
					block.page.content.charfontIndexes.get(block.format.charfont));
	}
	
	static ArrayList<String> getLetterWords(String str) {
		String[] ws = str.replaceAll("[\n\r,.\"':;\\?&]"," ").split("\\s+");
		ArrayList<String> ret=new ArrayList<String>();
		Pattern alphabet=Pattern.compile("[a-zA-Z]");
		
		for (int i = 0; i < ws.length; i++) {
			if(alphabet.matcher(ws[i]).find())
				ret.add(ws[i].toLowerCase());
		}
			
		return ret;
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
	

	
	interface BlockFilter {
		public boolean filter(Block block);
	}
	
	static class TextBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block block) {
			if(block.column==null)
				return false;
			
			if(block.format.charfont.equals(block.page.content.textCharfont)) {
				if(block.type==Common._BeforeFirstText)
					return false;
				else {
					if (block.rows.size()==1) {
						String lastStr=block.rows.get(block.rows.size()-1).string();
						
						return ((! infinishedTextBlock.matcher(lastStr.trim()).find()) &&
								/*(! block.isTrivial()) &&*/
								(block.format.alignment==Common._LEFTALIGNED) &&
								(! scarceRow.matcher(lastStr).find())
							   );
					} else {
						return (! block.isNotTextBlock()) &&
							   (! block.isAllScarce());
					}
				}
			} else {
				return Block.subtitleBlockFilter.filter(block);
			}
			
			/*return ((block.format.charfont.compareTo(block.page.content.textCharfont) == 0) &&
						block.type!=Common._BeforeFirstText &&
						(! (block.isTrivial(block.page.content,block.column) || 
						    (block.rows.size()==1 && infinishedTextBlock.matcher(lastStr.trim()).find())
						   )
						)
					) ||
				   	Block.subtitleBlockFilter.filter(block); */
			
			
		}
	}
	
	static class BigBlockFilter implements BlockFilter {
		final static Pattern pattern;
		
		static {
			pattern=Pattern.compile("^"+_IgnoredBlockPrefix);
		}
		
		@Override
		public boolean filter(Block block) {
			int charfontDiff=block.format.charfont.compareTo(block.page.content.textCharfont);
			
			if(charfontDiff>0 && block.isNonTitle())
				return false;
			
			return  ( charfontDiff >= 0 || 
						charfontDiff == 0 && 
						( block.format.alignment==Common._CENTERALIGNED ||
						  block.format.alignment==Common._CENTERALIGNEDWIINDENT)
					)
					&&
					! pattern.matcher(block.type).find();
		}
	}
	
	static class SubtitleBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block block) {
			return block.format.charfont.compareTo(block.page.content.textCharfont) > 0 &&
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
}