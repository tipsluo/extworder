package extworder;

import java.io.File;
import java.util.ArrayList;
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
	
	final static String _TestDataDir="data/";
	
	final static float _CharHGapRatio=1.5f;
	final static float _CharVGapRatio=2.5f;
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
	final static float _ColumnMinWidthRatio=0.3f;
	
	final static float _CenterAlignAdjustment=0.05f;
	
	final static int _MinTimeSubtitle=3;
	final static int _MinTimeAdditionalSubtitle=2;
	
	final static int _MaxTrivialLength=5;
	
	final static int _LEFTALIGNED=-1;
	final static int _CENTERALIGNED=0;
	final static int _RIGHTALIGNED=1;
	final static int _UNKNOWNALIGNED=-98;
	final static int _NOALIGNED=-99;
	
	final static int _UNKNOWNINDENT=-98;
	final static int _UNKNOWNALLUPPERCASE=-98;
	
	final static boolean __DEBUG=false;
	
	final static Pattern infinishedTextBlock;
	final static Pattern lowercaseExisting;
	final static Pattern leading2Uppercase;
	
	/* May need it later
	final static Pattern bulletPart1;
	final static Pattern bulletPart2;*/
	
	static {
		infinishedTextBlock=Pattern.compile("[a-zA-Z0-9,]$");
		lowercaseExisting=Pattern.compile("[a-z]");
		leading2Uppercase=Pattern.compile("^\\s*[A-Z]{2,}");
		
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

	
	interface BlockFilter {
		public boolean filter(Block block);
	}
	
	static class TextBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block block) {
			return (block.format.charfont.compareTo(block.page.content.textCharfont) == 0 &&
			   block.type!=Common._BeforeFirstText ) ||
					Block.subtitleBlockFilter.filter(block);
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
			
			return  charfontDiff >= 0 &&
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