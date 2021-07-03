package extworder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import extworder.Char.CharFont;
import extworder.Common.CharfontFilter;
import extworder.Page.BlockGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Content extends PDFTextStripper {
	ArrayList<Page> pages;
    TreeMap<CharFont,Integer> charfonts;
    Map<CharFont,Integer> charfontIndexes;
    CharFont textCharfont;
    Block titleBlock,abstractBlock;
    String abstractStr;
    int currPid;
    Page currPage=null;
	IsTextBlock isTextBlock=new IsTextBlock();
	PDProcessor pdProcessor;
	
	public Content(String fn)  throws IOException{
		pages=new ArrayList<Page>();
		charfonts=new TreeMap<>();
		
		pdProcessor=new PDProcessor(this);
		
		File file = new File(Common._TestDataDir+fn+".pdf");
		PDDocument document = PDDocument.load(file);
		
		setSortByPosition( true ); 
		
		for (currPid=1; currPid<=document.getNumberOfPages(); currPid++) {
			setStartPage(currPid);
			setEndPage(currPid);
			
			Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
			try {
				writeText(document, dummy);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(currPage!=null) {
				currPage.complete(document.getPage(currPid-1));
				pages.add(currPage);
			}
		}
		
		if( document != null )
             document.close();
		
		textCharfont = charfonts.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		charfontIndexes=makeCharfontIndexes();
		
		titleBlock=getTitleBlock();
		abstractBlock=getAbstractBlock();
	}

	@Override
	protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
		if (currPage==null || currPage.id != currPid) {
			currPage=new Page(this,currPid);
		}
		
        for (TextPosition text : textPositions) {
        	currPage.writeString(text);
        }
    }
		
	private Map<CharFont,Integer> makeCharfontIndexes() {
		CharFont[] cfs = new CharFont[charfonts.size()];
		int i=0;
		for (CharFont cf : charfonts.keySet()) {
	        cfs[i++]=cf;
		}
		Arrays.sort(cfs);
		
		int textCharfontIndex=-1;
		for(i = 0; i<cfs.length;i++ )
            if(cfs[i] == textCharfont) {
            	textCharfontIndex = i;
                break;
            }
		
		Map<CharFont,Integer> cfIndexes=new HashMap<>();
		for(i=0; i<cfs.length;i++ ) {
			cfIndexes.put(cfs[i],i-textCharfontIndex);
		}
		
		return cfIndexes;
	}

	public void print(FileWriter fw) throws IOException {
		for(Page page:pages) {
			page.print(fw);
		}
	}
	
	String text() {
		String str="";
		
		for(Page page:pages) {
			str+=page.text()+"\n";
		}
		
		str=Common.prepareOut(str);
		
		return str;
	}
	
	String title() {
		return titleBlock.string();
	}
	
	private Block getTitleBlock() {
		int i=0;
		Page page=null;
		Block titleBlock=null;
		
		for(; i<pages.size(); i++) {
			page=pages.get(i);
			
			String str=page.string();
			if (! str.contains(Common._LenderStr) && ! str.contains(Common._BorrowerStr))		
				break;
		}
		
		if (i>=pages.size()) {
			return null;
		} else {
			titleBlock=page.blocks.get(0);
			
			for(int j=0; j<page.blocks.size();j++) {
				if(page.blocks.get(j).charfont.compareTo(titleBlock.charfont)>0)
					titleBlock=page.blocks.get(j);
			}
		}
			
		return titleBlock;
	}

	
	private Block getAbstractBlock() {
		String str;
		Pattern p = Pattern.compile("^\\s*[Aa][Bb][Ss][Tt][Rr][Aa][Cc][Tt]\\s*[\\s:\n]?");
		
		int minAbstractWordNum=Common._MinAbstractWordNum + 
					pages.size() * Common._AbstractWordPageRatio;
		
		for (Page page:pages)
			for (BlockGroup blockgroup: page.blockgroups)
				for (int i=0; i<blockgroup.blocks.size();i++) {
					Block block=blockgroup.blocks.get(i);
					
					str=block.string();
					str=str.replaceAll("[\\r\\n]+", " ");
					str=str.replaceAll("\\s+", " ");
					
					String[] words=str.split("[\\s\n]");
					
					Matcher m = p.matcher(str);
					if (m.find()) {
						abstractStr=m.replaceFirst("");
						if (abstractStr.isBlank()) {
							Block block1=block.closestBlock();
							abstractStr=block1.string();
							return block1;
						}
						return block;
					} else {
						if (words.length >= minAbstractWordNum && 
							! block.charfont.equals(textCharfont)) {
							abstractStr=str;
							return block;
						}
					}
				}
			
		return null;
	}
	
	class IsTextBlock implements Common.CharfontFilter {
		@Override
		public boolean filter(CharFont charfont) {
			return charfont.equals(textCharfont);
		}
	}
}