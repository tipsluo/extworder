package extworder;

import extworder.Char.CharFont;
import extworder.Common.CharfontFilter;

public class Common {
	final static String _TestDataDir="data/";
	final static float _CharHGapRatio=1.5f;
	final static float _CharVGapRatio=2.5f;
	final static float _HSpaceMin=0.3f;
	
	final static String _LenderStr="LENDER";
	final static String _BorrowerStr="BORROWER";
	
	final static int _AbstractWordPageRatio=3;
	final static int _MinAbstractWordNum=30;
	
	final static float _MaxHeaderFooterWidthRatio=0.5f;
	final static float _BlockDisplaceRatio=1f;
	
	final static String _TitleBlock="TITLE";
	final static String _AbstractBlock="ABSTRACT";
	final static String _PageHeaderBlock="PAGEHEADER";
	final static String _PageFooterBlock="PAGEFOOTER";
	
	final static boolean __DEBUG=false;
	
	public Common() {
		// TODO Auto-generated constructor stub
	} 
	
	static String prepareOut(String str) {
		str = str.replaceAll("[\r\n]+", "\n");
		str = str.replaceAll("\s+", "\s");
		
		return str;
	}
	
	interface CharfontFilter {
		public boolean filter(CharFont charfont);
	}
}
