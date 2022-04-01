package geskiw;

public class Consts {
	final static String _CatNSubBlock="CatNSub";
	final static String _StopWordFile="C:/usaLYF/projects/workspace/ExtractArticle/WebContent/assets/stopwords.txt";
	final static String _AbbreviationFile="C:/usaLYF/projects/workspace/ExtractArticle/WebContent/assets/abbreviations.txt";
	final static String _IrregularFile="C:/usaLYF/projects/workspace/ExtractArticle/WebContent/assets/irregulars.txt";
	final static String _AbbrSubsStr="__~~";
	final static String _AbbrDot="__~~";
	final static int _LineNotEnd=1;
	final static int _NewLine=2;
	final static int _NewPage=3;
	final static int _RowSpace=1;
	final static int _CharSpace=1;
	final static int _Margin=30;
	final static int _LineBaseUpper=0;
	final static int _CharBaseLeft=0;
	final static float _AddtionalFontHeight=4f;
	final static float _AddtionalSummaryFontHeight=0.6f;
	final static float _FontWidthRatio=1.10f;
	final static int _TOP1NUM=2;
	final static int _TOP2NUM=2;
	final static String _NontitlePageStrings[] = new String[] { 
			".*elsevier.com.*", 
			".*thegreenjournal.com.*"
		};
	final static String _NontitleBlockStrings[][] = new String[][] { 
			{"^www\\.nature\\.com/scientificreports$","^OPEN$"},
			{"^www\\.nature\\.com/scientificreports$","^www\\.nature\\.com/scientificreports$"},
		};
	final static String[][] _IgnorePageLists=new String[][]{
		{"LENDER"},
		{"BORROWER"},
		{"SAGE Businesscases"},
		{"JSTOR is a not-for-profit service that helps scholars"},
		{"^\s*CITATION"}
	};
	
	public Consts() {
	}
}
