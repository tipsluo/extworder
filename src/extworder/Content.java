package extworder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import extworder.Char.CharFont;
import extworder.Common.CharfontFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Content extends PDFTextStripper {
	ArrayList<Page> pages;
    TreeMap<CharFont,Integer> charfonts;
    Map<CharFont,Integer> charfontIndexes;
    CharFont textCharfont;
    Block titleBlock;
    int currPid;
    Page currPage=null;
	//IsTitleBlock isTitleBlock=new IsTitleBlock();
	IsTextBlock isTextBlock=new IsTextBlock();
	
	public Content(String fn)  throws IOException{
		pages=new ArrayList<Page>();
		charfonts=new TreeMap<>();
		
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
				currPage.complete();
				pages.add(currPage);
			}
		}
		
		if( document != null )
             document.close();
		
		//titleCharfont=charfonts.lastKey();
		titleBlock=getTitleBlock();
		textCharfont = charfonts.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		charfontIndexes=makeCharfontIndexes();
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
		
		str = str.replaceAll("[\r\n]+", "\n");
		str = str.replaceAll("\s+", "\s");
		
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
	
	/*class IsTitleBlock implements Common.CharfontFilter {
		@Override
		public boolean filter(CharFont charfont) {
			return charfont.equals(titleCharfont);
		}
	}*/
	
	class IsTextBlock implements Common.CharfontFilter {
		@Override
		public boolean filter(CharFont charfont) {
			return charfont.equals(textCharfont);
		}
	}
}