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
		markHeaderBlock();
		markFooterBlock();
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
	
	void markHeaderBlock() {
		ArrayList<ArrayList<Block>> hbls=new ArrayList<ArrayList<Block>>();
		
		Page page0=pages.get(0);
		float minHeaderWidth=Common._MaxHeaderFooterWidthRatio * page0.width;
		float headerBottom=Common._MaxHeaderFooterHeightRatio * page0.height;
		
		for(int i=0; i<pages.size(); i++) {
			Page page1=pages.get(i);
			ArrayList<Block> tbs1=page1.topBlocks();
			
			for(int j=i+1; j<pages.size(); j++) {
				Page page2=pages.get(j);
				ArrayList<Block> tbs2=page2.topBlocks();
				
				for(Block tb1:tbs1)
					for(Block tb2:tbs2) {
						if( tb1.right-tb1.left > minHeaderWidth ||
							tb2.right-tb2.left > minHeaderWidth ||
							tb1.bottom > headerBottom || 
							tb2.bottom > headerBottom )
							
							continue;
						
						addSimilar(hbls,tb1,tb2);
					}
			}
		}
		
		for(ArrayList<Block> bl:hbls) {
			for(Block b:bl)
				b.type=Common._PageHeaderBlock;
		}
	}
	
	void markFooterBlock() {
		ArrayList<ArrayList<Block>> fbls=new ArrayList<ArrayList<Block>>();
		
		Page page0=pages.get(0);
		float minFooterWidth=Common._MaxHeaderFooterWidthRatio * page0.width;
		float footerTop=(1-Common._MaxHeaderFooterHeightRatio) * page0.height;
		
		for(int i=0; i<pages.size(); i++) {
			Page page1=pages.get(i);
			ArrayList<Block> bbs1=page1.bottomBlocks();
			
			for(int j=i+1; j<pages.size(); j++) {
				Page page2=pages.get(j);
				ArrayList<Block> bbs2=page2.bottomBlocks();
				
				for(Block bb1:bbs1)
					for(Block bb2:bbs2) {
						if( bb1.right-bb1.left > minFooterWidth ||
							bb2.right-bb2.left > minFooterWidth ||
							bb1.top < footerTop || 
							bb2.top < footerTop )
							
							continue;
						
						addSimilar(fbls,bb1,bb2);
					}
			}
		}
		
		for(ArrayList<Block> bl:fbls) {
			for(Block b:bl)
				b.type=Common._PageFooterBlock;
		}
	}

	
	private void addSimilar(ArrayList<ArrayList<Block>> bls, Block b1, Block b2) {
		if(b1.isSimilar(b2)) {							
			boolean found=false;
			
			for(ArrayList<Block> bl:bls)
				if(bl.size()>0)
					if (b1.isSimilar(bl.get(0))) {
						bl.add(b1);
						bl.add(b2);
						found=true;
						break;
					}
			
			if(!found) {
				ArrayList<Block> bl=new ArrayList<Block>();
				bl.add(b1);
				bl.add(b2);
				bls.add(bl);
			}
		}
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
			
		titleBlock.type=Common._TitleBlock;
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
							block1.type=Common._AbstractBlock;
							return block1;
						}
						return block;
					} else {
						if (words.length >= minAbstractWordNum && 
							! block.charfont.equals(textCharfont)) {
							abstractStr=str;
							block.type=Common._AbstractBlock;
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