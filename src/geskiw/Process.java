package geskiw;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import extworder.Block;
import extworder.Common;
import extworder.Common.StatGroup;
import extworder.Content;
import extworder.Page;

public class Process {
	static Content content;
	String bodyStr;
	ArrayList<String> keySentences;
	ArrayList<String> keySubtitles;
	List<String> top1_3;
	List<String> top4_8;
	
	public Process(String fn) throws IOException {
		content = new Content(fn, new IgnorePage(), false, true, false);
		bodyStr=content.body();
		getTopSentence();
		
		print(fn);
	}
	
	List<String> topKeywords(int number) throws IOException {
		ArrayList<String> stopWords=readWordsFromFile(Consts._StopWordFile);
		
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
		
		return list3.topsByValue(number);
	}
	
	private void print(String fn) throws IOException {
		FileWriter myWriter= new FileWriter(Common._TestDataDir+fn+".out");
		
		myWriter.write("Most repeated keywords ==>\n\n");
		for(String kw: top1_3)
			myWriter.write(kw+"\n");
		
		myWriter.write("\nMedium repeated keywords ==>\n\n");
		for(String kw: top4_8)
			myWriter.write(kw+"\n");
		
		myWriter.write("\n\nExtracted Output ===>\n");
				
		myWriter.write("\n\nSection sentences followed by one of the key sentences below ===> \n\n");
		for(String keySubtitle: keySubtitles)
			myWriter.write(keySubtitle.replaceAll("\\n"," ")+"\n");
		
		myWriter.write("\n\nKey sentences including at least one most repeated and one medium repeated keywors ===> \n\n");
		for(String keySentence: keySentences)
			myWriter.write(keySentence+"\n\n");
			
		myWriter.close();
	}
	
	/*private void ignoreCatNSubBlock() {
		content.getKeyBlockStr(0,
				Pattern.compile("^\\s*Categories\s+and\s+Subject\s+Descriptors\\s*[\\s:\n]?"));
		content.activeBlock.setIgnored(Consts._CatNSubBlock);
	}*/
	
	/*private void removeStopWords(StatGroup<String> statGroup, ArrayList<String> stopWords) {
		for(String word: statGroup.records.keySet()) {
			if(Collections.binarySearch(stopWords,word.toLowerCase()) > 0)
				statGroup.remove(word);
		}
	}*/

	
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
	
	private ArrayList<String> readWordsFromFile(String filename) throws IOException {
		ArrayList<String> stopWords=new ArrayList<String>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       stopWords.addAll(Arrays.asList(line.split(" ")));
		    }
		}
		
		Collections.sort(stopWords);
		
		return stopWords;
	}
	
	private void getTopSentence() throws IOException {
		keySentences=new ArrayList<String>();
		keySubtitles=new ArrayList<String>();
		
		List<String> top8=topKeywords(8);
		top1_3=top8.subList(0, 3);
		top4_8=top8.subList(3, 8);

		Block subtitleBlock=null;
		
		for(Page page: content.pages)
			for(Page.Column column: page.columns)
				for(Block block:column.blocks) {
					if(Block.subtitleBlockFilter.filter(block)) {
						subtitleBlock=block;
						continue;
					}
					
					if(! (new Common.BodyBlockFilter()).filter(block))
						continue;
					
					for(String sentence: Common.getSentences(block.string())) {
						boolean found=false;
						for(String kw: top1_3)
							if(sentence.contains(kw)) {
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
	}
	
	/*private ArrayList<String> getTopSentence() {
		ArrayList<String> top5=topKeywords(8);
		ArrayList<String> allSentences=(ArrayList<String>) Arrays.asList(Common.getSentences(bodyStr));
		
		ArrayList<String> top1_3=(ArrayList<String>) top5.subList(0, 3);
		ArrayList<String> top4_8=(ArrayList<String>) top5.subList(3, 5);
		
		ArrayList<String> ret=new ArrayList<String>();
		
		for(ArrayList<String> sentence: allSentences)
			for(String kw: keywords)
				if(sentence.contains(kw))
					ret.add(sentence);
		return ret;
	}*/
	
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
