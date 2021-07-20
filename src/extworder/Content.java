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

import extworder.Block.BlockFormat;
import extworder.Char.CharFont;
import extworder.Page.Column;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	PDProcessor pdProcessor;
    int contentLeft,contentRight,contentWidth;
	int columnNumber,columnWidth;
	float lowColumnWidth;
	float highColumnWidth;
	boolean hasFirstTextBlock=false;
	BlockFormatChain subtitleFormatChain;
	
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
		for(Page page:pages) {
			page.markHeaderFooter();
		}
		
		markContentX();
		makeColumns();
		getFirstTextBlock();
		markSubtitleBlocks();
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
	
	private int columnWidth() {
		Map<Integer,Integer> blockWidths=new TreeMap<Integer,Integer>();
		
		for(Page page:pages)
			for(Block block:page.blocks) {
				int w=block.right-block.left+1;
				int n=blockWidths.compute(w, (k,v) -> (v == null ? 0 : v) + 1);
				blockWidths.put(w,n);
			}
		
		Integer i=blockWidths.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return i;
	}
	
	private void markHeaderBlock() {
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
	
	void markContentX() {
		contentLeft=9999;
		contentRight=-1;
		
		for(Page page:pages)
			for(Block block:page.blocks) {
				if(contentLeft>block.left)
					contentLeft=block.left;
				if(contentRight<block.right)
					contentRight=block.right;
			}
		
		contentWidth=contentRight-contentLeft+1;
		
		columnWidth=columnWidth();
		lowColumnWidth=columnWidth*(1-Common._ColumnWidthAdjustment);
		highColumnWidth=columnWidth*(1+Common._ColumnWidthAdjustment);
		
		if(columnWidth+columnWidth+columnWidth < contentWidth)
			columnNumber=3;
		else if (columnWidth+columnWidth < contentWidth) 
			columnNumber=2;
		else
			columnNumber=1;
	}
	
	private void markFooterBlock() {
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
	 
	private void markSubtitleBlocks() {
		ArrayList<Block> bigBlockList=getBigBlockList();
		
		subtitleFormatChain=getSubtitleFormatChain(bigBlockList);
		
		if(subtitleFormatChain==null)
			return;
		
		int i=0;
		for(;i<bigBlockList.size();i++) {
			Block block=bigBlockList.get(i);
			
			if(Block.additionalSubtitleFormatFilter.filter(block)) {
				block.type=Common.subtitleBlockType(block);
				continue;
			}
			
			int n=getIncreasingFormatBlockNumber(bigBlockList,i);
				
			if(n>=1) {
				boolean allContained=true;
				for(int j=i-n; j<i; j++) {
					block=bigBlockList.get(j);
					if(subtitleFormatChain.blockformatIndex(block.blockformat()) < 0) {
						allContained=false;
						break;
					}
				}
				if(allContained)
					for(int j=i-n; j<i; j++)
						bigBlockList.get(j).type=Common.subtitleBlockType(bigBlockList.get(j));
			}
		}
	}

	private void makeColumns() {
		for(Page page:pages)
			page.makeColumns();
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
		fw.write(String.format("Content:\nColumnWidth %d ColumnNumber %d\n",
				columnWidth,columnNumber));
		fw.write(String.format("ContentWidth %d ContentLeft %d ContentRight %d\n",
				contentWidth,contentLeft,contentRight));
		
		if(subtitleFormatChain!=null) {
			fw.write(String.format("Subtitles: "));
			for(BlockFormat blockformat:subtitleFormatChain.blockformats)
				fw.write(String.format(" %d",charfontIndexes.get(blockformat)));
			fw.write("\n\n");
			
			for(Page page:pages) {
				page.print(fw);
			}
		}
	}
	
	String text() {		
		hasFirstTextBlock=false;
		
		String str="";
		
		for(Page page:pages) {
			str+=page.text()+"\n";
		}
		
		str=Common.prepareOut(str);
		
		return str;
	}
	
	String subtitles() {
		String str="";
		
		for(Page page:pages) {
			str+=page.subtitles()+"\n";
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
			for(int i=0; i<page.blocks.size();i++) {
				Block block=page.blocks.get(i);
				
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
	
	private BlockFormatChain getSubtitleFormatChain(ArrayList<Block> bigBlockList) {
		TreeMap<BlockFormatChain,Integer> candidates=new TreeMap<>();
		
		int i=0;
		for(;i<bigBlockList.size();i++) {
			int n=getIncreasingFormatBlockNumber(bigBlockList,i);
				
			if(n>=1) {
				BlockFormatChain blockformatChain=new BlockFormatChain();
				for(int j=i-n; j<i; j++)
					blockformatChain.blockformats.add(bigBlockList.get(j).blockformat());
				
				boolean found=false;
				Iterator<Entry<BlockFormatChain, Integer>> entryIt = candidates.entrySet().iterator();
				while (entryIt.hasNext()) {
				    Entry<BlockFormatChain, Integer> entry = entryIt.next();
				    BlockFormatChain chain=entry.getKey();
			        Integer num=entry.getValue();
			    
			        if(blockformatChain.equals(chain) || chain.contains(blockformatChain)) {
			        	candidates.put(chain,num+1);
			        	found=true;
			        	break;
			        } else if(blockformatChain.contains(chain)) {
			        	entryIt.remove();
			        	candidates.put(blockformatChain,num+1);
			        	found=true;
			        	break;
			        }
				}
				
				if(! found)
					candidates.put(blockformatChain,1);		
			}
		}
		
		for(;;) {
			int size=candidates.size();
			
			TreeMap<BlockFormatChain,Integer> candidatesNew=new TreeMap<>();
			
			Iterator<Entry<BlockFormatChain, Integer>> candidateIt = candidates.entrySet().iterator();
			while(candidateIt.hasNext()) {
				Entry<BlockFormatChain, Integer> candidate=candidateIt.next();
				BlockFormatChain chain=candidate.getKey();
				candidatesNew.put(chain,candidate.getValue());
				
				Iterator<Entry<BlockFormatChain, Integer>> candidateIt1=candidates.entrySet().iterator();
				while(candidateIt1.hasNext()) {
					Entry<BlockFormatChain, Integer> candidate1=candidateIt1.next();
					BlockFormatChain chain1=candidate1.getKey();
					
					if(chain==chain1)
						continue;
					
					if(chain.contains(chain1)) {
						candidatesNew.put(chain,candidatesNew.get(chain)+candidate1.getValue());
						candidateIt1.remove();
					}
				}
			}
			candidates=candidatesNew;
			
			if(candidates.size()==size)
				break;
		}
		
		if (candidates.size()==0)
			return null;
		else {
			BlockFormatChain retChain=candidates.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
			
			if(candidates.get(retChain)<Common._MinTimeSubtitle)
				return null;
			else {
				ArrayList<BlockFormat> additionalSubtitleFormatFilter=new ArrayList<BlockFormat>();
				Iterator<Entry<BlockFormatChain, Integer>> candidateIt = candidates.entrySet().iterator();
				while(candidateIt.hasNext()) {
					Entry<BlockFormatChain, Integer> candidate=candidateIt.next();
					BlockFormatChain chain=candidate.getKey();
					int count=candidate.getValue();
					
					if(chain.blockformats.size()>1 || 
							count<Common._MinTimeAdditionalSubtitle)
						continue;
					
					additionalSubtitleFormatFilter.add(chain.blockformats.get(0));
				}
				Block.additionalSubtitleFormatFilter=new Common.AdditionalSubtitleFormatFilter(additionalSubtitleFormatFilter);
				
				return retChain;
			}
		}
	}
	
	private int getIncreasingFormatBlockNumber(ArrayList<Block> blocks, int endBlockIndex) {
		Block block=blocks.get(endBlockIndex);
	
		if(block.charfont.compareTo(textCharfont)==0) {
			Block block0=block;
			
			int i1=endBlockIndex-1;
			for(; i1>=0; i1--) {
				Block block1=blocks.get(i1);
				if(block1.charfont.equals(textCharfont))
					break;
					
				if(block0.charfont.compareTo(block1.charfont)>=0)
					break;
				
				block0=block1;
			}
			
			int n=endBlockIndex-i1-1;
			
			if(n>=1)
				return n;
			else 
				return -1;
		} else
			return -1;
	}
	
	private ArrayList<Block> getBigBlockList() {
		ArrayList<Block> blocklist=new ArrayList<Block>();
		
		for(Page page:pages)
			blocklist.addAll(page.getBigBlockList());
		
		return blocklist;
	}
	
	private Block getFirstTextBlock() {
		for(Page page:pages)
			for(Column column:page.columns)
				for(Block block:page.blocks) {
					int blockWidth=block.right-block.left+1;
					if(block.charfont.equals(textCharfont) && 
							blockWidth >= lowColumnWidth &&
							blockWidth <= highColumnWidth) {
						block.type=Common._FirstText;
						return block;
					} else if(block.type.isEmpty())
						block.type=Common._BeforeFirstText;
				}
		return null;
	}
	
	static class HStretch implements Comparable<HStretch> {
		protected int left;
		protected int right;
		
		public HStretch(int left,int right) {
			this.left=left;
			this.right=right;
		}
		
	    @Override
	    public int hashCode() {
	        return left*100000 + right;
	    }
		
		@Override
		public boolean equals(Object obj) {
			if (getClass() != obj.getClass())
	            return false;
			HStretch other = (HStretch) obj;
			return hashCode()==other.hashCode();
		}

		@Override
		public int compareTo(HStretch s) {
			return hashCode()-s.hashCode();
		}
	}
	
	static class BlockFormatChain implements Comparable<BlockFormatChain> {
		ArrayList<BlockFormat> blockformats=new ArrayList<>();

		@Override
		public int compareTo(BlockFormatChain blockformatChain) {
			int n1=blockformats.size();
			int n2=blockformatChain.blockformats.size();
			
			return n1==n2 ? 
						this.contains(blockformatChain) || blockformatChain.contains(this) ? 
								0 : hashCode()-blockformatChain.hashCode()
						:
						n1-n2;
		}
		
		@Override 
		public int hashCode() {
			int hash=0;
			
	        for(BlockFormat blockformat:blockformats)
	        	hash+=blockformat.hashCode();
	        
	        return hash;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
	            return true;
	        if (obj == null)
	            return false;
	        if (getClass() != obj.getClass())
	            return false;
	        
	        BlockFormatChain other = (BlockFormatChain) obj;

	        return contains(other) && blockformats.size()==other.blockformats.size();
		}
		
		public boolean contains(BlockFormatChain blockformatChain) {
			if(blockformats.size()<blockformatChain.blockformats.size())
				return false;
			
			int i1=0;
			int i2=0;
			int matched=0;
			
			for(;i1<blockformats.size() && i2<blockformatChain.blockformats.size();) {
				BlockFormat blockformat1=blockformats.get(i1);
				BlockFormat blockformat2=blockformatChain.blockformats.get(i2);
				
				if(blockformat1.equals(blockformat2)) {
					matched++;
					i1++;
					i2++;
				} else {
					i1++;
				}
			}
			
			return matched==blockformatChain.blockformats.size();
		}
		
		public int blockformatIndex(BlockFormat blockformat) {
			int i=0;
			for(BlockFormat bf: blockformats) {
				if(blockformat.equals(bf))
					return i;
				
				i++;
			}
			return -1;
		}
	}
}