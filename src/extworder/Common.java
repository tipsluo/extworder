package extworder;

import extworder.Char.CharFont;
import extworder.Common.BlockFilter;

public class Common {
	final static String _TestDataDir="data/";
	
	final static float _CharHGapRatio=1.5f;
	final static float _CharVGapRatio=2.5f;
	final static float _HSpaceMin=0.3f;
	final static float _SameBlockFontHeightDiff=0.4f;
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
	
	final static boolean __DEBUG=false;
	
	public Common() {
		// TODO Auto-generated constructor stub
	} 
	
	static String prepareOut(String str) {
		str = str.replaceAll("[\r\n]+", "\n");
		str = str.replaceAll("\s+", "\s");
		
		return str;
	}
	
	interface BlockFilter {
		public boolean filter(Block block);
	}
	
	static class TextBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block block) {
			return block.charfont.compareTo(block.page.content.textCharfont) >= 0 &&
			   block.type!=Common._BeforeFirstText;
		}
	}
	
	static class SubtitleBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block block) {
			return block.charfont.compareTo(block.page.content.textCharfont) > 0 &&
			   block.type!=Common._BeforeFirstText;
		}
	}
}
