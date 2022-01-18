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
import extworder.Common.BodyBlockFilter;
import extworder.Common.StatGroup;
import extworder.Common.SubtitleBlockFilter;
import extworder.Content;
import extworder.Page;
import extworder.Row;
import geskiw.Word.WordStatGroup;
import geskiw.Word.Words;

public class Process {
	static ArrayList<String> stopWords;
	static ArrayList<String> abbrSubses;
	static HashMap<String,String> irregulars;
	
	Content content;
	String bodyStr;
	List<Word> top1_3;
	List<Word> top4_8;
	public String extractResult;
	ArrayList<Pattern> abbrPatterns;
	StatGroup<Word> commonWords;
	
	public Process(String pdfPath, 
					String stopWordFile, 
					String abbreviationFile,
					String irreNounFile) throws IOException {
		readStopWordsFromFile(stopWordFile);
		readAbbreviationsFromFile(abbreviationFile);
		readirregularsFromFile(irreNounFile);
			
		content = new Content(pdfPath, new IgnorePage(), true, true, false);
		bodyStr=Common.joinLines(content.body());
		
		ArrayList<Section> sections=getTopSentence();
		
		extractResult=output(sections);
	}
	
	List<Word> topKeywords(int number) throws IOException {
		ArrayList<String> titleStrs=Common.getWords(Common.joinLines(content.titleBlock.string()),true);
		titleStrs=removeWords(titleStrs,stopWords);
		
		ArrayList<String> abstractStrs=Common.getWords(Common.joinLines(content.abstractBlock.string()),true);
		abstractStrs=removeWords(abstractStrs,stopWords);
		
		ArrayList<String> keyWords=titleStrs;
		keyWords.addAll(abstractStrs);
		
		ArrayList<Word> list1=(new Words(keyWords)).list;
		
		ArrayList<String> bodyWordStrs=Common.getWords(bodyStr,true);
		WordStatGroup list2=new WordStatGroup(bodyWordStrs);
		
		WordStatGroup list3=getCommonWords(list2,list1);
		
		commonWords=list3;
		
		return list3.topsByValue(number);
	}
	
	private String output(ArrayList<Section> sections) throws IOException {
		String ret="";
		
		ret+="Most repeated keywords ==>\n\n";
		for(Word kw: top1_3) {
			ret+=kw.string()+"\n";
		}
		
		ret+="\nMedium repeated keywords ==>\n\n";
		for(Word kw: top4_8) {
			ret+=kw.string()+"\n";
		}
		
		ret+="\n\nExtracted Output ===>\n";
				
		/*ret+="\n\nSection sentences followed by one of the key sentences below ===> \n\n";
		for(String keySubtitle: keySubtitles)
			ret+=keySubtitle.replaceAll("\\n"," ")+"\n";*/
		
		ret+="\n\nKey sentences including at least one most repeated and one medium repeated keywords ===> \n\n";
		for(Section section:sections) {
			if(section.subtitleBlock!=null) {
				ret+=section.subtitleBlock.string()+"\n\n";
			}
			for(String keySentence: section.keySentences)
				ret+=keySentence+"\n\n";
		}
			
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
	
	private ArrayList<Section> getTopSentence() throws IOException {
		List<Word> top8=topKeywords(8);
		top1_3=top8.subList(0, 3);
		top4_8=top8.subList(3, 8);

		ArrayList<Section> sections=getSections(content.getMainBlocks());
		for(Section s:sections) {
			if(s.bodyBlocks.size()>0)
				getKeySentences(s);
		}
		
		return sections;
	}
	
	private String joinBlocks(ArrayList<Block> blocks) {
		ArrayList<String> strs=new ArrayList<String>();
		
		for(Block block:blocks) {
			for(Row row:block.rows)
				strs.add(row.string());
		}
		
		return Common.joinLines(strs);
	}
	
	private ArrayList<Section> getSections(ArrayList<Block> blocks) {
		Common.BodyBlockFilter bodyBlockFilter=new BodyBlockFilter();
		Common.SubtitleBlockFilter subtitleBlockFilter=new SubtitleBlockFilter();
		
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
	
	private void getKeySentences(Section section) {
		CharString bodyCharString=new CharString(section.bodyBlocks,abbrPatterns);
				
		ArrayList<CharString> css=bodyCharString.splitSentences();
		
		for(CharString cs:css) {
			boolean found=false;
			for(Word kw: top1_3)
				for(String s:kw.forms)
					if(cs.string().toLowerCase().contains(s)) {
						found=true;
						break;
					}
			if(!found) continue;
			
			for(Word kw:top4_8) {
				found=false;
				for(String s:kw.forms)
					if(cs.string().contains(s)) {
						found=true;
						break;
					}
				if(found) {
					section.keySentences.add(cs);
					break;
				}
			}
		}
	}
	
	/*private void getKeySentences(Section section) {
		String str=joinBlocks(section.bodyBlocks);
		
		for(String sentence: Common.getOrigSentences(replaceAbbreviations(str))) {
			boolean found=false;
			for(Word kw: top1_3)
				for(String s:kw.forms)
					if(sentence.toLowerCase().contains(s)) {
						found=true;
						break;
					}
			if(!found) continue;
			
			for(Word kw:top4_8) {
				found=false;
				for(String s:kw.forms)
					if(sentence.contains(s)) {
						found=true;
						break;
					}
				if(found) {
					section.keySentences.add(sentence);
					break;
				}
			}
		}
	}*/
	
	public String outputCommonWords() {
		Map<Word, Integer> sortedRecords = new HashMap<>();
		
		sortedRecords=commonWords.reverseSortByValue();
		
		String ret="";
		
		for (Map.Entry<Word,Integer> entry : sortedRecords.entrySet()) {
			ret+=entry.getKey().string() + ": " + String.valueOf(entry.getValue()) + "\n";
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
}
