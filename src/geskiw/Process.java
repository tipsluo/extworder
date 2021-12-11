package geskiw;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import extworder.Block;
import extworder.Common;
import extworder.Common.BodyBlockFilter;
import extworder.Common.StatGroup;
import extworder.Common.SubtitleBlockFilter;
import extworder.Content;
import extworder.Page;
import extworder.Row;

public class Process {
	Content content;
	String bodyStr;
	ArrayList<String> keySentences;
	ArrayList<String> keySubtitles;
	List<String> top1_3;
	List<String> top4_8;
	public String extractResult;
	ArrayList<String> stopWords;
	ArrayList<String> abbrSubses;
	ArrayList<Pattern> abbrPatterns;
	Block subtitleBlock;
	StatGroup<String> commonWords;
	
	public Process(String pdfPath, String stopWordFile, String abbreviationFile) throws IOException {
		readStopWordsFromFile(stopWordFile);
		readAbbreviationsFromFile(abbreviationFile);
			
		content = new Content(pdfPath, new IgnorePage(), true, true, false);
		bodyStr=content.body();
		getTopSentence();
		
		extractResult=output();
	}
	
	List<String> topKeywords(int number) throws IOException {
		ArrayList<String> titleWords=Common.getWords(content.titleBlock.string(),true);
		titleWords=removeWords(titleWords,stopWords);
		
		ArrayList<String> abstractWords=Common.getWords(content.abstractBlock.string(),true);
		abstractWords=removeWords(abstractWords,stopWords);
		
		StatGroup<String> kws=new StatGroup<String>(titleWords);
		kws.addAll(abstractWords);
		ArrayList<String> list1=kws.allKeys();
		Collections.sort(list1);
		
		ArrayList<String> bodyWords=Common.getWords(bodyStr,true);
		bodyWords=removeWords(bodyWords,stopWords);
		StatGroup<String> list2=new StatGroup<String>(bodyWords);
		
		StatGroup<String> list3=getCommonWords(list2,list1);
		
		commonWords=list3;
		
		return list3.topsByValue(number);
	}
	
	private String output() throws IOException {
		String ret="";
		
		ret+="Most repeated keywords ==>\n\n";
		for(String kw: top1_3)
			ret+=kw+"\n";
		
		ret+="\nMedium repeated keywords ==>\n\n";
		for(String kw: top4_8)
			ret+=kw+"\n";
		
		ret+="\n\nExtracted Output ===>\n";
				
		ret+="\n\nSection sentences followed by one of the key sentences below ===> \n\n";
		for(String keySubtitle: keySubtitles)
			ret+=keySubtitle.replaceAll("\\n"," ")+"\n";
		
		ret+="\n\nKey sentences including at least one most repeated and one medium repeated keywords ===> \n\n";
		for(String keySentence: keySentences)
			ret+=keySentence+"\n\n";
			
		return restoreAbbreviation(ret);
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
	
	private StatGroup<String> getCommonWords(StatGroup<String> whole, ArrayList<String> sortedTarget) {
		StatGroup<String> ret=new StatGroup<String>();
		
		for(Map.Entry<String,Integer> record: whole.records.entrySet()) {
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
		abbrSubses=new ArrayList<String>();
		abbrPatterns=new ArrayList<Pattern>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	abbrSubses.add(line.replaceAll("\\.",Consts._AbbrSubsStr));
		    	abbrPatterns.add(Pattern.compile(line));
		    }
		}
	}
	
	private String replaceAbbreviations(String str) {
		String s=str;
		for(int i=0;i<abbrPatterns.size();i++) {
			abbrPatterns.get(i).matcher(s).replaceAll(abbrSubses.get(i));
		}
		
		return s;
	}
	
	private String restoreAbbreviation(String str) {
		return str.replaceAll(Consts._AbbrSubsStr,".");
	}
	
	private void getTopSentence() throws IOException {
		keySentences=new ArrayList<String>();
		keySubtitles=new ArrayList<String>();
		
		List<String> top8=topKeywords(8);
		top1_3=top8.subList(0, 3);
		top4_8=top8.subList(3, 8);

		subtitleBlock=null;
		Common.BodyBlockFilter bodyBlockFilter=new BodyBlockFilter();
		Common.SubtitleBlockFilter subtitleBlockFilter=new SubtitleBlockFilter();
		
		ArrayList<Block> blocks=content.getMainBlocks();
		ArrayList<Block> bs=new ArrayList<Block>();
				
		for(Block block:blocks) {
			if(subtitleBlockFilter.filter(block)) {
				subtitleBlock=block;
				continue;
			}

			if(! bodyBlockFilter.filter(block)) {
				if(bs.size()!=0) {
					getkeySentences(joinBlocks(bs));
					bs=new ArrayList<Block>();
				}
				
				continue;
			}
			
			bs.add(block);
		}
		
		if(bs.size()!=0)
			getkeySentences(joinBlocks(bs));
	}
	
	private String joinBlocks(ArrayList<Block> mainBlocks) {
		ArrayList<String> strs=new ArrayList<String>();
		
		for(Block block:mainBlocks) {
			for(Row row:block.rows)
				strs.add(row.string());
		}
		
		return Common.joinLines(strs);
	}
	
	private void getkeySentences(String str){
		for(String sentence: Common.getOrigSentences(replaceAbbreviations(str))) {
			boolean found=false;
			for(String kw: top1_3)
				if(sentence.toLowerCase().contains(kw)) {
					found=true;
					break;
				}
			if(!found) continue;
			
			for(String kw:top4_8) {
				if(sentence.contains(kw)) {
					keySentences.add(sentence);
					
					if(subtitleBlock!=null) {
						keySubtitles.add(subtitleBlock.string());
						subtitleBlock=null;
					}
					break;
				}
			}
		}
	}
	
	public String outputCommonWords() {
		Map<java.lang.String, Integer> sortedRecords = new HashMap<>();
		
		sortedRecords=commonWords.reverseSortByValue();
		
		String ret="";
		
		for (Map.Entry<String,Integer> entry : sortedRecords.entrySet()) {
			ret+=entry.getKey() + ": " + String.valueOf(entry.getValue()) + "\n";
		}
		
		return ret;
	}
	
	class IgnorePage extends Common.IgnorePage {
		final String[] pstr=new String[]{
			"LENDER",
			"BORROWER",
			"SAGE Businesscases",
			"JSTOR is a not-for-profit service that helps scholars"
		};
		
		public boolean isIgnored(Page page) {
			String str=page.string();
			for(int i=0; i<pstr.length; i++)
				if (str.contains(pstr[i]))
					return true;

			return false;
		}
	}
}
