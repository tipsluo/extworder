package extworder;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import extworder.Block.BlockFormat;

public class Common {
	final static String _TestDataDir="data/";
	
	final static float _CharHGapRatio=1.5f;
	final static float _CharVGapRatio=2.5f;
	final static float _HSpaceMin=0.3f;
	//final static float _SameBlockFontHeightDiff=0.4f;
	final static float _SameBlockRowWidthDiff=0.1f;
	
	final static String _LenderStr="LENDER";
	final static String _BorrowerStr="BORROWER";
	
	final static int _AbstractWordPageRatio=10;
	final static int _MinAbstractWordNum=50;
	
	final static float _MaxHeaderFooterWidthRatio=0.5f;
	final static float _BlockDisplaceRatio=1f;
	final static float _MaxHeaderFooterHeightRatio=0.1f;
	
	final static String _TitleBlock="TITLE";
	final static String _AbstractBlock="ABSTRACT";
	final static String _PageHeaderBlock="PAGEHEADER";
	final static String _PageFooterBlock="PAGEFOOTER";
	final static String _BeforeFirstText="BEFOREFIRSTTEXT";
	final static String _FirstText="FIRSTTEXT";
	final static String _SubtitlePrefix="SUBTITLEPREFIX_";
	
	final static float _ColumnWidthAdjustment=0.05f;
	
	final static int _MinTimeSubtitle=3;
	final static int _MinTimeAdditionalSubtitle=2;
	
	final static int _BOLD=2;
	final static int _SEMIBOLD=1;
	final static int _NOBOLD=0;

	final static CheckBold checkBold=new CheckBold();
	
	final static boolean __DEBUG=false;
	
	public Common() {
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
					block.page.content.charfontIndexes.get(block.charfont));
	}
	
	interface BlockFilter {
		public boolean filter(Block block);
		
	}
	
	static class TextBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block block) {
			return (block.charfont.compareTo(block.page.content.textCharfont) == 0 &&
			   block.type!=Common._BeforeFirstText ) ||
					block.subtitleBlockFilter.filter(block);
		}
	}
	
	static class BigBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block block) {
			return block.charfont.compareTo(block.page.content.textCharfont) >= 0;
		}
	}
	
	static class SubtitleBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block block) {
			return block.charfont.compareTo(block.page.content.textCharfont) > 0 &&
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
				if(blockformat.equals(new BlockFormat(block.charfont,block.indentColumn())))
					return true;
				
			return false;
		}
	}
	
	final static class CheckBold {
		final Pattern LastPart;
		final Pattern Bold;
		final Pattern Semibold;
		
		public CheckBold() {
			LastPart=Pattern.compile("[\\.-](.*)$");
			Bold=Pattern.compile("Bold");
			Semibold=Pattern.compile("Semibold");
		}
		
		public int check(String s) {
			Matcher m=LastPart.matcher(s);
			
			if(m.find()) {
				String lastPart=m.group(1);
				
				if(lastPart.equals("B"))
					return _BOLD;
				else {
					m=Bold.matcher(lastPart);
					if(m.find())
						return _BOLD;
					else {
						m=Semibold.matcher(lastPart);
						if(m.find())
							return _SEMIBOLD;
						else
							return _NOBOLD;
					}
				}
			}
			
			return _NOBOLD;
		}
	}
}
