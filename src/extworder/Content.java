package extworder;

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import extworder.Block.BlockFormat;
import extworder.Page.Column;
import extworder.Row.CharFont;

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
    public Block activeBlock;
    Block titleBlock;
	Block abstractBlock;
	Block keywordBlock;
    String abstractStr;
    String keywordStr;
    
	ArrayList<Page> pages;
    TreeMap<CharFont,Integer> charfonts;
    Map<CharFont,Integer> charfontIndexes;
    CharFont textCharfont;
    int currPid;
    Page currPage=null;
	PDFRenderer renderer;
    int contentLeft,contentRight,contentWidth;
    float lowContentWidth, highContentWidth, maxContentTrivalBlockWidth;
	int columnNumber,columnWidth;
	float lowColumnWidth,highColumnWidth,maxColumnTrivalBlockWidth;
	
	boolean hasFirstTextBlock=false;
	BlockFormatChain subtitleFormatChain;
	int bgRGB;
	
    Common.IgnorePage ignorePage;
	
	public Content(String fn, 
				Common.IgnorePage ignorePage,
				boolean ignoreIntraBlock,
				boolean ignoreColoredBlock)  throws IOException {

		this.ignorePage=ignorePage;
		
		pages=new ArrayList<Page>();
		charfonts=new TreeMap<>();
		
		File file = new File(Common._TestDataDir+fn+".pdf");
		PDDocument document = PDDocument.load(file);
		renderer = new PDFRenderer(document);
		
		setSortByPosition( true ); 
		
		for (currPid=1; currPid<=document.getNumberOfPages(); currPid++) {
			setStartPage(currPid);
			setEndPage(currPid);
			
			if (currPage==null || currPage.id != currPid) {
				currPage=new Page(this,currPid);
			}
			
			Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
			try {
				writeText(document, dummy);
			} catch (IOException e) {
				e.printStackTrace();
			}

			currPage.pageImg=currPage.new PageImg(renderer.renderImage(currPid-1));
			if(!currPage.pageImg.normal())
				currPage.pageImg=null;
			
			pages.add(currPage);
		}
		
		if( document != null )
             document.close();
		
		if(!ignoreColoredBlock)
			bgRGB=getBackgroundColor();
		
		for(int i=0; i<pages.size();) {
			Page page=pages.get(i);
			
			page.complete(ignoreColoredBlock);
			
			if(page.ignored()) {
				pages.remove(i);
				continue;
			}
			
			i++;
		}
			
		markHeaderBlock();
		markFooterBlock();
		for(Page page:pages) {
			page.markHeaderFooter();
		}
		
		markContentX();
		makeColumns();
		
		getTextCharfont();
		getFirstTextBlock();
		
		for(Page page:pages) {
			page.updateBlockFormats();
			page.tuneBlocks(textCharfont);
		}
		
		titleBlock=getTitleBlock();
		abstractBlock=getAbstractBlock();
		keywordBlock=getKeywordBlock();
	
		if(ignoreIntraBlock)
			markIntraTextBlocks();
		
		markSubtitleBlocks();
	}

	@Override
	protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        for (TextPosition text : textPositions) {
        	currPage.writeString(text);
        }
    }
	
	/*private void getTextCharfont() {
		for(Page page:pages)
			//for(Column column:page.columns)
				//for(Block block:column.blocks)
					for(Row row:page.rows) {
						Integer n=charfonts.compute(row.charfont, (k,v) -> (v == null ? 0 : v) + 1);
						charfonts.put(row.charfont,n);

				}
		
		textCharfont = charfonts.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		charfontIndexes=makeCharfontIndexes();
	}*/
	
	private void getTextCharfont() {
		for(Page page:pages)
			for(Column column:page.columns)
				for(Block block:column.blocks) {
					//for(Block block:page.blocks) {
						if(! block.isTrivial(this,block.column) &&
								! charfonts.containsKey(block.format.charfont))
							charfonts.put(block.format.charfont,evaluateTextCharfont(block.format.charfont));
				}
		
		textCharfont = charfonts.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		
		charfontIndexes=makeCharfontIndexes();
	}
	
	private int evaluateTextCharfont(CharFont charfont) {
		int value=0;
		
		int currValue=pages.size();
		
		for(Page page:pages) {
			for(Block block:page.blocks) {
				if(block.isTrivial(this,block.column))
					continue;
				
				if(block.format.charfont.equals(charfont)) {
					value+=currValue;
					break;
				}
			}
			currValue--;
		}
		
		return value;
	}
	
	private Map<CharFont,Integer> makeCharfontIndexes() {
		ArrayList<CharFont> cfs=new ArrayList<CharFont>();
		
		for(Page page:pages)
			for(Row row:page.rows)
				if(! cfs.contains(row.charfont))
					cfs.add(row.charfont);
		
		int textCharfontIndex=-1;
		for(int i = 0; i<cfs.size();i++ )
            if(cfs.get(i).equals(textCharfont)) {
            	textCharfontIndex = i;
                break;
            }
		
		Map<CharFont,Integer> cfIndexes=new HashMap<>();
		for(int i=0; i<cfs.size();i++ ) {
			cfIndexes.put(cfs.get(i),i-textCharfontIndex);
		}
		
		return cfIndexes;
	}
		
	/*private Map<CharFont,Integer> makeCharfontIndexes() {
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
	}*/
	
	private int columnWidth() {
		Map<Integer,Integer> blockWidths=new TreeMap<Integer,Integer>();
		
		int minColumnWidth=(int)(Common._ColumnWidthAdjustment * contentWidth);
		
		for(Page page:pages)
			for(Block block:page.blocks) {
				int w=block.right-block.left+1;
				
				if(w<minColumnWidth)
					continue;
				
				int n=blockWidths.compute(w, (k,v) -> (v == null ? 0 : v) + 1);
				blockWidths.put(w,n);
			}
		
		Integer i=blockWidths.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return i;
	}
	
	private void markHeaderBlock() {
		ArrayList<ArrayList<Block>> hbls=new ArrayList<ArrayList<Block>>();
		
		Page page0=pages.get(0);
		float minHeaderWidth=Common._MaxHeaderFooterWidthRatio * page0.right;
		float headerLower=Common._MaxHeaderFooterHeightRatio * page0.lower;
		
		for(int i=0; i<pages.size(); i++) {
			Page page1=pages.get(i);
			ArrayList<Block> tbs1=page1.upperBlocks();
			
			for(int j=i+1; j<pages.size(); j++) {
				Page page2=pages.get(j);
				ArrayList<Block> tbs2=page2.upperBlocks();
				
				for(Block tb1:tbs1)
					for(Block tb2:tbs2) {
						if( tb1.right-tb1.left > minHeaderWidth ||
							tb2.right-tb2.left > minHeaderWidth ||
							tb1.lower > headerLower || 
							tb2.lower > headerLower )
							
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
		lowContentWidth=contentWidth*(1-Common._ColumnWidthAdjustment);
		highContentWidth=contentWidth*(1+Common._ColumnWidthAdjustment);
		maxContentTrivalBlockWidth=contentWidth*Common._MaxTrivialCharBlockWidth;
		
		columnWidth=columnWidth();
		lowColumnWidth=columnWidth*(1-Common._ColumnWidthAdjustment);
		highColumnWidth=columnWidth*(1+Common._ColumnWidthAdjustment);
		maxColumnTrivalBlockWidth=columnWidth*Common._MaxTrivialCharBlockWidth;
		
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
		float minFooterWidth=Common._MaxHeaderFooterWidthRatio * page0.right;
		float footerUpper=(1-Common._MaxHeaderFooterHeightRatio) * page0.lower;
		
		for(int i=0; i<pages.size(); i++) {
			Page page1=pages.get(i);
			ArrayList<Block> bbs1=page1.lowerBlocks();
			
			for(int j=i+1; j<pages.size(); j++) {
				Page page2=pages.get(j);
				ArrayList<Block> bbs2=page2.lowerBlocks();
				
				for(Block bb1:bbs1) {
					for(Block bb2:bbs2) {
						if( bb1.right-bb1.left > minFooterWidth ||
							bb2.right-bb2.left > minFooterWidth ||
							bb1.upper < footerUpper || 
							bb2.upper < footerUpper )
							
							continue;
						
						if(bb2.isFull(this,bb2.column))
							continue;
						
						addSimilar(fbls,bb1,bb2);
					}
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
			
			if(i<bigBlockList.size()-1 && bigBlockList.get(i+1).isTextFullBlock())
				if(Block.additionalSubtitleFormatFilter.filter(block)) {
					block.type=Common.subtitleBlockType(block);
					continue;
				}
				
			int n=getIncreasingFormatBlockNumber(bigBlockList,i);
				
			if(n>=1) {
				boolean allContained=true;
				for(int j=i-n; j<i; j++) {
					block=bigBlockList.get(j);
					if(subtitleFormatChain.blockformatIndex(block.format) < 0) {
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
	
	private void markIntraTextBlocks() {
		boolean textInfinished=false;
		
		for(Page page:pages)
			for(Column column:page.columns)
				for(Block block:column.blocks) {
					if(block.isTextInfinished()) {
						textInfinished=true;
						continue;
					}
					
					if(textInfinished)
						if(block.isTextFullBlock()) {
							textInfinished=false;
							continue;
						} else if(block.type.isEmpty()) {
							block.type=Common._IgnoredBlockIntraText;
						}	
				}
	}
	
	private int getBackgroundColor() {
	    TreeMap<Integer,Integer> pixelColors=new TreeMap<Integer,Integer>();
	    
	    for(Page page:pages) {
	    	if(page.pageImg==null)
	    		continue;
	    
	    	for(int x=0; x<page.pageImg.img.getWidth(); x++)
		    	for(int y=0; y<page.pageImg.img.getHeight(); y++) {
		    		int rgb=page.pageImg.img.getRGB(x,y);
		    		int n=pixelColors.compute(rgb, (k,v) -> (v == null ? 0 : v) + 1);
		    		pixelColors.put(rgb,n);
		    	}
	    }
	    
	    int rgb=pixelColors.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
	    
	    return rgb;
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
				fw.write(String.format(" %d",charfontIndexes.get(blockformat.charfont)));
			fw.write("\n\n");
		}
		
		for(Page page:pages) {
			page.print(fw);
		}
	}
	
	public String text() {		
		hasFirstTextBlock=false;
		
		String str="";
		
		for(Page page:pages) {
			str+=page.text()+"\n";
		}
		
		str=Common.prepareOut(str);
		
		return str;
	}
	
	public String subtitles() {
		String str="";
		
		for(Page page:pages) {
			str+=page.subtitles()+"\n";
		}
		
		str=Common.prepareOut(str);
		
		return str;
	}
	
	public String title() {
		return titleBlock.string();
	}
	
	private Block getTitleBlock() {
		Block titleBlock=pages.get(0).blocks.get(0);
		Block block=null;
		
		for(Page page:pages) {
			if(page.ignored())
				continue;
			
			for(int j=0; j<page.blocks.size();j++) {
				block=page.blocks.get(j);
				
				if(block.type==Common._FirstText)
					break;
				
				if(block.format.charfont.compareTo(titleBlock.format.charfont)>0)
					titleBlock=block;
			}
			
			if(block.type==Common._FirstText)
				break;
		}
			
		titleBlock.type=Common._TitleBlock;
		return titleBlock;
	}
	
	private Block getAbstractBlock() {
		abstractStr=getKeyBlockStr(
				Pattern.compile("^\\s*[Aa][Bb][Ss][Tt][Rr][Aa][Cc][Tt]\\s*[\\s:\n]?"));
		if(activeBlock!=null)
			activeBlock.type=Common._AbstractBlock;
		return activeBlock;	
	}
	
	private Block getKeywordBlock() {
		keywordStr=getKeyBlockStr(
				Pattern.compile("^\\s*[Kk][Ee][Yy][Ww][Oo][Rr][Dd]\\s*[\\s:\n]?"));
		if(activeBlock!=null)
			activeBlock.type=Common._KeywordBlock;
		return activeBlock;	
	}
	
	public String getKeyBlockStr(Pattern pattern) {
		int minKeyBlockWordNum=Common._MinKeyBlockWordNum + 
				pages.size() * Common._KeyBlockWordPageRation;
		
		String str;
		String ret;
	
		boolean stopped=false;
		
		for (Page page:pages) {
			for(int i=0; i<page.blocks.size();i++) {
				Block block=page.blocks.get(i);
				
				if(block.type==Common._FirstText) {
					stopped=true;
					break;
				}
				
				str=block.string();
				str=str.replaceAll("[\\r\\n]+", " ");
				str=str.replaceAll("\\s+", " ");
				
				String[] words=str.split("[\\s\n]");
				
				Matcher m = pattern.matcher(str);
				if (m.find()) {
					ret=m.replaceFirst("");
					if (ret.isBlank()) {
						Block block1=block.closestBlock();
						ret=block1.string();
						activeBlock=block1;
						return ret;
					}
					
					activeBlock=block;
					return ret;
				} else {
					if (words.length >= minKeyBlockWordNum && 
						! block.format.charfont.equals(textCharfont)) {

						activeBlock=block;
						return str;
					}
				}
			}
			
			if(stopped)
				break;
		}
			
		activeBlock=null;
		return "";
	}
	
	private BlockFormatChain getSubtitleFormatChain(ArrayList<Block> bigBlockList) {
		TreeMap<BlockFormatChain,Integer> candidates=new TreeMap<>();
		
		int i=0;
		for(;i<bigBlockList.size();i++) {
			int n=getIncreasingFormatBlockNumber(bigBlockList,i);
				
			if(n>=1) {
				BlockFormatChain blockformatChain=new BlockFormatChain();
				for(int j=i-n; j<i; j++)
					blockformatChain.blockformats.add(bigBlockList.get(j).format);
				
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
	
		if(block.isTextFullBlock()) {
			Block block0=block;
			
			int i1=endBlockIndex-1;
			for(; i1>=0; i1--) {
				Block block1=blocks.get(i1);
				if(block1.format.charfont.equals(textCharfont))
					break;
				
				if(block0.format.compareFormat(block1.format)>=0)
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
		for(Page page:pages) {
			for(Column column:page.columns)
				for(Block block:column.blocks) {
					if(block.isTextFullBlock()) {
						block.type=Common._FirstText;
						return block;
					} else if(block.type.isEmpty())
						block.type=Common._BeforeFirstText;
				}
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