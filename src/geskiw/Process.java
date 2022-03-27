package geskiw;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import extworder.Block;
import extworder.Common;
import extworder.Block.BodyBlockFilter;
import extworder.Common.StatGroup;
import extworder.Block.SubtitleBlockFilter;
import extworder.Content;
import extworder.Content.NontitleChecker;
import extworder.Page;
import extworder.Row;
import geskiw.Word.WordStatGroup;
import geskiw.Word.Words;

public class Process {
	static ArrayList<String> stopWords;
	static HashMap<String,String> irregulars;
	
	Content content;
	String bodyStr;
	List<Word> top0;
	List<Word> top1;
	List<Word> top2;
	public String extractResult;
	public String error;
	ArrayList<Pattern> abbrPatterns;
	StatGroup<Word> commonWords;
	private int originalWordCount;
	private int extractedWordCount;
	
	ArrayList<Section> outputSections;
	
	public Process(String pdfPath, 
					String stopWordFile, 
					String abbreviationFile,
					String irreNounFile) throws IOException {
		
		error=null;
		
		readStopWordsFromFile(stopWordFile);
		readAbbreviationsFromFile(abbreviationFile);

		readirregularsFromFile(irreNounFile);
			
		content = new Content(pdfPath, abbrPatterns, new IgnorePage(), true, true, false, new ScholarNontitleChecker());
		bodyStr=Common.joinLines(content.body());
		
		int top1Num=2;
		int top2Num=2;
		top0=topKeywords(top1Num+top2Num);
		
		if(top0==null) {
			error="Not able to get top word list.";
			return;
		}
		
		getAllKeySentences(top1Num,top2Num);
		
		extractResult=output();
	}
	
	List<Word> topKeywords(int number) throws IOException {
		ArrayList<String> keyWords=null;
		
		if(content.titleBlock!=null) {
			ArrayList<String> titleStrs=Common.getWords(Common.joinLines(content.titleBlock.string()),true);
			if(titleStrs!=null) {
				titleStrs=removeWords(titleStrs,stopWords);
				keyWords=titleStrs;
			}
		}
		
		ArrayList<String> abstractStrs;
		if(content.abstractBlock!=null)
			abstractStrs=Common.getWords(Common.joinLines(content.abstractBlock.string()),true);
		else
			abstractStrs=Common.getWords(Common.joinLines(bodyStr),true);
			
		if(abstractStrs!=null) {
			abstractStrs=removeWords(abstractStrs,stopWords);
			
			if(keyWords!=null)
				keyWords.addAll(abstractStrs);
			else
				keyWords=abstractStrs;
		}
		
		if(keyWords==null)
			return null;
		
		ArrayList<Word> list1=(new Words(keyWords)).list;
		
		ArrayList<String> bodyWordStrs=Common.getWords(bodyStr,true);
		originalWordCount=bodyWordStrs.size();
		
		WordStatGroup list2=new WordStatGroup(bodyWordStrs);
		
		WordStatGroup list3=getCommonWords(list2,list1);
		
		commonWords=list3;
		
		return list3.topsByValue(number);
	}
	
	private String output() throws IOException {
		String ret="";
		
		ret+="Most repeated keywords ==>\n\n";
		for(Word kw: top1) {
			ret+=kw.string()+"\n";
		}
		
		ret+="\nMedium repeated keywords ==>\n\n";
		for(Word kw: top2) {
			ret+=kw.string()+"\n";
		}
		
		ret+="\n\nExtracted Output ===>\n";
		
		ret+="\n\nKey sentences including at least one most repeated and one medium repeated keywords ===> \n\n";
		for(Section section:outputSections) {
			if(section.subtitleBlock!=null) {
				ret+=section.subtitleBlock.string()+"\n\n";
			}
			for(CharString keySentence: section.keySentences)
				ret+=keySentence.string()+"\n\n";
		}
			
		return restoreAbbreviation(ret);
	}
	
	public void writePDF(String outputPDF, String originalFilename) {
		PDFWriter pw=new PDFWriter(outputPDF,content.bodyBlockformat.charfont);
		
		float h=content.bodyBlockformat.charfont.height + Consts._AddtionalFontHeight;
		
		pw.write(new CharString.VirtualCharString("PDF file name: ",PDFWriter.summaryBoldFont,h),PDFWriter.summaryBoldFont);
		pw.write(new CharString.VirtualCharString(originalFilename,PDFWriter.summaryItalicFont,h),PDFWriter.summaryFont);
		pw.newLine();

		pw.write(new CharString.VirtualCharString("Original article word count: ",PDFWriter.summaryBoldFont,h),PDFWriter.summaryBoldFont);
		pw.write(new CharString.VirtualCharString(String.valueOf(originalWordCount),PDFWriter.summaryItalicFont,h),PDFWriter.summaryItalicFont);
		pw.newLine();
		
		String s=String.format("%d (%d%% of the original word count)",
					extractedWordCount,100*extractedWordCount/originalWordCount);
		pw.write(new CharString.VirtualCharString("Extracted content word count: ",PDFWriter.summaryBoldFont,h),PDFWriter.summaryBoldFont);
		pw.write(new CharString.VirtualCharString(s,PDFWriter.summaryItalicFont,h),PDFWriter.summaryItalicFont);
		pw.newLine();
		
		if(content.titleBlock !=null)
			s=content.titleBlock.string();
		else
			s=" ";
		pw.write(new CharString.VirtualCharString("Title of the article: ",PDFWriter.summaryBoldFont,h),PDFWriter.summaryBoldFont);
		pw.write(new CharString.VirtualCharString(s,PDFWriter.summaryItalicFont,h),PDFWriter.summaryItalicFont);
		pw.newLine();
		pw.newLine();
		
		pw.write(new CharString.VirtualCharString("The content extracted is as follows: ",PDFWriter.summaryBoldFont,h),PDFWriter.summaryBoldFont);
		pw.newLine();
		pw.drawHorizenLine();
		pw.newLine();
		
		for(Section section:outputSections) {
			if(section.subtitleBlock!=null && section.keySentences.size()>0) {
				pw.newLine();
				pw.write(new CharString(section.subtitleBlock,abbrPatterns).unmarkAbbreviation(),PDFWriter.resultBoldFont);
				pw.newLine();
				pw.newLine();
			}
			for(CharString keySentence: section.keySentences) {
				pw.write(keySentence,PDFWriter.resultFont);
				pw.newLine();
				pw.newLine();
			}
		}
		
		pw.save();
		System.out.println("PDF is created.");
	}
	
	private ArrayList<String> removeWords(ArrayList<String> source, ArrayList<String> list) {
		ArrayList<String> words=new ArrayList<String>();
		
		for(String s: source) {
			int i=Collections.binarySearch(list,s);
			if(i<0) {
				words.add(s);
			}
		}
		
		return words;
	}
	
	private WordStatGroup getCommonWords(StatGroup<Word> whole, ArrayList<Word> sortedTarget) {
		WordStatGroup ret=new WordStatGroup();
		
		for(Map.Entry<Word,Integer> record: whole.records.entrySet()) {
			int i=Collections.binarySearch(sortedTarget,record.getKey());
			if(i>=0)
				ret.add(record);
		}
		return ret;
	}
	
	private void readStopWordsFromFile(String filename) throws IOException {
		stopWords=new ArrayList<String>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       stopWords.addAll(Arrays.asList(line.split(" ")));
		    }
		}
		
		Collections.sort(stopWords);
	}
	
	private void readAbbreviationsFromFile(String filename) throws IOException {
		abbrPatterns=new ArrayList<Pattern>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	abbrPatterns.add(Pattern.compile(line));
		    }
		}
	}
	
	private void readirregularsFromFile(String filename) throws IOException {
		irregulars=new HashMap<String,String>();
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	String[] ss=line.split(" ");
		    	for(int i=1;i<ss.length;i++)
		    		irregulars.put(ss[i],ss[0]);
		    }
		}
	}
	
	/*private String replaceAbbreviations(String str) {
		String s=str;
		for(int i=0;i<abbrPatterns.size();i++) {
			abbrPatterns.get(i).matcher(s).replaceAll(abbrSubses.get(i));
		}
		
		return s;
	}*/
	
	private String restoreAbbreviation(String str) {
		return str.replaceAll(Consts._AbbrSubsStr,".");
	}
	
	private void getAllKeySentences(int top1Num,int top2Num) throws IOException {
		top1=top0.subList(0, top1Num);
		top2=top0.subList(top1Num, top1Num+top2Num);
		
		System.out.printf("Processing with %d of top and %d of medium keywords...\n",top1Num, top2Num);
		
		extractedWordCount=0;

		ArrayList<Section> sections=getSections(content.getMainBlocks());
		
		for(Section s:sections) {
			if(s.bodyBlocks.size()>0) {
				extractedWordCount+=getKeySentences(s);
			}
		}
		
		outputSections=sections;
	}
	
	private ArrayList<Section> getSections(ArrayList<Block> blocks) {
		BodyBlockFilter bodyBlockFilter=new BodyBlockFilter();
		SubtitleBlockFilter subtitleBlockFilter=new SubtitleBlockFilter();
		
		ArrayList<Section> sections=new ArrayList<Section>();
		Section section=null;
				
		for(Block block:blocks) {
			if(subtitleBlockFilter.filter(block)) {
				if(section!=null && section.subtitleBlock!=null && 
						block.format.compareTo(section.subtitleBlock.format)<0)
					continue;
				
				section=new Section(block);
				sections.add(section);
				continue;
			}

			if(! bodyBlockFilter.filter(block)) {
				continue;
			}
			
			if(section==null) {
				section=new Section(null);
				sections.add(section);
			}
			section.addBody(block);
		}
	
		return sections;
	}
	
	private int getKeySentences(Section section) {
		int keySentenceWordCount=0;
		
		CharString bodyCharString=new CharString(section.bodyBlocks,abbrPatterns);
				
		ArrayList<CharString> css=bodyCharString.splitSentences();
		
		for(CharString cs:css) {
			boolean found=false;
			for(Word kw: top1)
				for(String s:kw.forms)
					if(cs.string().toLowerCase().contains(s)) {
						found=true;
						break;
					}
			if(!found) continue;
			
			for(Word kw:top2) {
				found=false;
				for(String s:kw.forms)
					if(cs.string().contains(s)) {
						found=true;
						break;
					}
				if(found) {
					cs.unmarkAbbreviation();
					section.keySentences.add(cs);
					keySentenceWordCount+=Common.getWords(cs.string(),false).size();
					break;
				}
			}
		}
		return keySentenceWordCount;
	}
	
	public String outputCommonWords() {
		Map<Word, Integer> sortedRecords = new HashMap<>();
		
		sortedRecords=commonWords.reverseSortByValue();
		
		String ret="";
		
		for (Map.Entry<Word,Integer> entry : sortedRecords.entrySet()) {
			ret+=entry.getKey().string() + ": " + String.valueOf(entry.getValue()) + "\n";
		}
		
		return ret;
	}
	
	class IgnorePage extends Content.IgnorePage {
		final String[][] pstrLists=new String[][]{
			{"LENDER"},
			{"BORROWER"},
			{"SAGE Businesscases"},
			{"JSTOR is a not-for-profit service that helps scholars"},
			{"^\s*CITATION"}
		};
		
		List<List<Pattern>> patternLists;
		
		public IgnorePage() {
			patternLists=new ArrayList<List<Pattern>>();
			
			for(int i=0; i<pstrLists.length; i++) {
				ArrayList<Pattern> patternList=new ArrayList<Pattern>();
				
				for(int j=0; j<pstrLists[i].length; j++)
					patternList.add(Pattern.compile(pstrLists[i][j]));
				
				patternLists.add(patternList);
			}
		}
		
		public boolean isIgnored(Page page) {
			boolean matched=false;
			
			for(List<Pattern> patternList: patternLists) {
				for(Pattern pattern: patternList) {
					matched=false;
					
					for(Block block:page.blocks) {
						if(pattern.matcher(block.string()).find()) {
							matched=true;
							break;
						}
					}
					
					if(! matched)
						break;
				}	
					
				if(matched)
					return true;
			}

			return false;
		}
	}
	
	public class Section {
		Block subtitleBlock;
		public ArrayList<Block> bodyBlocks;
		ArrayList<CharString> keySentences;
		
		public Section(Block subtitleBlock) {
			this.subtitleBlock=subtitleBlock;
			this.bodyBlocks=new ArrayList<Block>();
			keySentences=new ArrayList<CharString>();
		}
		
		public void addBody(Block block) {
			bodyBlocks.add(block);
		}
		
		public void addKeySentence(CharString sentence) {
			keySentences.add(sentence);
		}
	}
	
	public class ScholarNontitleChecker extends NontitleChecker {
		boolean firstTitle=true;
		boolean contentMatched=false;

		public ScholarNontitleChecker() {
		}
		
		public void check(Content c) {
			for(Block block:c.pages.get(0).blocks)
				for(String str: Consts.nontitleStrings)
					if(block.string().contains(str)) {
						contentMatched=true;
						return;
					}
		}
		
		public boolean select(Block block) {
			if(contentMatched && firstTitle) {
				firstTitle=false;
				return false;
			}
			return true;
		}
	}
}
