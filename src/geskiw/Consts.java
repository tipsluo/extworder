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
		{"^\\s*LENDER","^\\s*BORROWER"},
		{"^\\s*SAGE Businesscases"},
		{"Your use of the JSTOR archive indicates your acceptance of the Terms & Conditions of Use"},
		{"^\\s*CITATION"},
		{"^\\s*ILL Number:"}
	};
	final static String[] _SkipSectionList=new String[]{
		"ACM Reference Format",
		"Publisher’s Note",
		"Author Contributions",
		"Authors' contributions",
		"Funding",
		"Citation",
		"INDEX TERMS",
		"Disclosures of Conflicts of Interest",
		"Activities related to the present article",
		"Keywords",
		"DATA AVAILABILITY STATEMENT",
		"Correspondence",
		"ETHICS STATEMENT",
		"Conflicts of Interest",
		"List of abbreviations",
		"ACKNOWLEDGEMENTS",
		"Acknowledgments",
		"About the Author",
		"About the authors",
		"Authors Notes",
		"Author’s Note",
		"Author Information",
		"References",
		"Reference List",
		"REFERENCES",
		"Notes",
		"NOTES",
		"Endnote",
		"ENDNOTE",
		"Endnotes",
		"Works Cited",
		"Appendix",
		"APPENDIX",
		"Appendix A",
		"Appendix B",
		"Bibliography",
		"BIBLIOGRAPHY",
		"External resources",
		"Further reading"
	};
	
	public Consts() {
	}
}
