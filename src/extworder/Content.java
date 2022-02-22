package extworder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import extworder.Block.AdditionalSubtitleFormatFilter;
import extworder.Block.BlockFilter;
import extworder.Block.BlockFormat;
import extworder.Block.BodyBlockFilter;
import extworder.Block.SectionBlockFilter;
import extworder.Block.SubtitleBlockFilter;
import extworder.Page.Column;

import java.util.ArrayList;
import java.util.Collections;
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
    public Block titleBlock;
	public Block abstractBlock;
	Block lastSubtitleBlock=null;
    
	public ArrayList<Page> pages;
	TreeMap<BlockFormat,Integer> blockformats;
	Map<BlockFormat,Integer> blockformatIndexes;
    public BlockFormat bodyBlockformat;
    int currPid;
    Page currPage=null;
	PDFRenderer renderer;
    int contentLeft,contentRight,contentWidth;
    float lowContentWidth, highContentWidth;
	int columnNumber,columnWidth;
	float lowColumnWidth,highColumnWidth,minBodyColumnBlockWidth;
	int centralAlignmentAdjustment=0;
	private boolean ignoreSubtitle=true;
	private ArrayList<String> allWords;
	BlockFormatChain subtitleFormatChain;
	int bgRGB;
	ArrayList<Pattern> abbrPatterns;
	
    Common.IgnorePage ignorePage;
	
	public Content(String fn,
				ArrayList<Pattern> abbrPatterns,
				Common.IgnorePage ignorePage,
				boolean ignoreIntraBlock,
				boolean ignoreColoredBlock,
				boolean ignoreSubtitle)  throws IOException {
		
		this.abbrPatterns=abbrPatterns;
		this.ignoreSubtitle=ignoreSubtitle;
		this.ignorePage=ignorePage;
		
		pages=new ArrayList<Page>();
		blockformats=new TreeMap<>();
		
		File file = new File(fn);
		PDDocument document = PDDocument.load(file);
		
		if(! ignoreColoredBlock)
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
				writeText(document,dummy);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if(! ignoreColoredBlock) {
				currPage.pageImg=currPage.new PageImg(renderer.renderImage(currPid-1));
				if(!currPage.pageImg.normal())
					currPage.pageImg=null;
			}
			
			pages.add(currPage);
		}
		
		if( document != null )
             document.close();
		
		if(!ignoreColoredBlock)
			bgRGB=getBackgroundColor();
		

		PDPageTree allPages = document.getDocumentCatalog().getPages();
		
		for(int i=0; i<pages.size();) {
			Page page=pages.get(i);
			
			if(page.chars.size()==0) {
				pages.remove(i);
				continue;
			}
			
			page.complete(allPages.get(i),ignoreColoredBlock);
			
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
		
		joinFrameSameRow();
		
		markContentX();
		makeColumns();
		
		for(Page page:pages) {
			page.separateAllUppers();
			page.updateBlockFormats();
			page.renderStrings();
		}
		
		// Page contents should not be changed after this point.
		
		getBodyFormat();
		getAllBodyBlocks();
		
		allWords=scanTextAlphabetWords();
		
		titleBlock=getTitleBlock();
		abstractBlock=getAbstractBlock();
	
		if(ignoreIntraBlock)
			markIntraBodyBlocks();
		
		if(! ignoreSubtitle)
			markSubtitleBlocks();
	}

	@Override
	protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        for (TextPosition text : textPositions) {
        	currPage.writeString(text);
        }
    }
	
	private void getBodyFormat() {
		for(Page page:pages)
			for(Column column:page.columns)
				for(Block block:column.blocks) {
					if(block.likeBodyBlock1()>=Common._ParaSentDefaultTrue &&
							! blockformats.containsKey(block.format))
						blockformats.put(block.format,evaluateBodyBlockformat(block.format));
				}
		
		bodyBlockformat=blockformats.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		
		blockformatIndexes=makeBlockformatIndexes();
	}
	
	private int evaluateBodyBlockformat(BlockFormat blockformat) {
		int value=0;
		
		int currValue=pages.size();
		
		for(Page page:pages) {
			for(Block block:page.blocks) {
				if(block.likeBodyBlock1()<Common._ParaSentDefaultTrue)
					continue;
				
				if(block.format.equals(blockformat)) {
					value+=currValue * block.rows.size();
					break;
				}
			}
			currValue--;
		}
		
		return value;
	}
	
	private Map<BlockFormat,Integer> makeBlockformatIndexes() {
		ArrayList<BlockFormat> bfs=new ArrayList<BlockFormat>();
		
		for(Page page:pages)
			for(Block block:page.blocks) {
				if(! bfs.contains(block.format))
					bfs.add(block.format);
			}
		
		Collections.sort(bfs);
		int bodyBlockformatIndex=-1;
		for(int i = 0; i<bfs.size();i++ )
            if(bfs.get(i).equals(bodyBlockformat)) {
            	bodyBlockformatIndex = i;
                break;
            }
		
		Map<BlockFormat,Integer> bfIndexes=new HashMap<>();
		for(int i=0; i<bfs.size();i++ ) {
			bfIndexes.put(bfs.get(i),i-bodyBlockformatIndex);
		}
		
		return bfIndexes;
	}
	
	private int columnWidth() {
		Map<Integer,Integer> blockWidths=new TreeMap<Integer,Integer>();
		
		int minColumnWidth=(int)(Common._ColumnWidthAdjustment * contentWidth);
		
		for(Page page:pages)
			for(Block block:page.blocks) {
				if(block.isParagraphBlock(null)<=Common._ParaSentDefaultFalse)
					continue;
					
				int w=block.right-block.left+1;
				
				if(w<minColumnWidth)
					continue;
				
				int n=blockWidths.compute(w, (k,v) -> (v == null ? 0 : v) + 1);
				blockWidths.put(w,n);
			}
		
		Integer i=blockWidths.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return i;
	}
	
	private void joinFrameSameRow() {
		for(Page page:pages) {
			Collections.sort(page.rows,Row.compareRowHeights);
			Collections.reverse(page.rows);
			
			for(int i=0;i<page.rows.size();i++) {
				Row row1=page.rows.get(i);

				int j;
				for(j=i+1;j<page.rows.size();j++) {
					Row row2=page.rows.get(j);
					
					if(row1==row2)
						continue;
					
					if(! row1.vContains(row2))
						continue;

					if(row1.block==row2.block || 
							( ! page.checkSeparatingBorder(row1,row2) &&
							row1.hDistance(row2)<=(int)(row1.height*Common._MaxSameRowDistanceRatio))) {
						
						row1.merge(row2,true);
						j--;

						Block b=row2.block;
						if(b.rows.size()==0) {
							if(b.column!=null)
								b.column.blocks.remove(b);
							page.blocks.remove(b);
						}
					}
				}
					
			}
			Collections.sort(page.rows,Row.compareRows);
			Collections.sort(page.blocks,Block.compareBlocks);
		}
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
		
		columnWidth=columnWidth();
		lowColumnWidth=columnWidth*(1-Common._ColumnWidthAdjustment);
		highColumnWidth=columnWidth*(1+Common._ColumnWidthAdjustment);
		minBodyColumnBlockWidth=columnWidth*Common._MinBodyCharBlockWidth;
		
		if(columnWidth+columnWidth+columnWidth < contentWidth)
			columnNumber=3;
		else if (columnWidth+columnWidth < contentWidth) 
			columnNumber=2;
		else
			columnNumber=1;
		
		centralAlignmentAdjustment=(int) (columnWidth*Common._CenterAlignAdjustment);
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
			
			if(i<bigBlockList.size()-1 && bigBlockList.get(i+1).likeBodyBlock1()>=Common._ParaSentDefaultTrue)
				if(Block.additionalSubtitleFormatFilter.filter(block)) {
					//block.type=Common.subtitleBlockType(block);
					block.type=Common._SectionPrefix;
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
					for(int j=i-n; j<i; j++) {
						bigBlockList.get(j).type=Common.subtitleBlockType(bigBlockList.get(j));
						lastSubtitleBlock=bigBlockList.get(j);
					}
			}
		}
	}
	
	private void markIntraBodyBlocks() {
		boolean textInfinished=false;
		
		for(Page page:pages)
			for(Column column:page.columns)
				for(Block block:column.blocks) {
					if(block.isBodyInfinished()) {
						textInfinished=true;
						continue;
					}
					
					if(textInfinished)
						if(block.isBodyCharfontFullBlock()) {
							textInfinished=false;
							continue;
						} else if(block.type.isEmpty()) {
							block.type=Common._IgnoredBlockIntraBody;
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
		if(b1.isSimilar(b2,false)) {							
			boolean found=false;
			
			for(ArrayList<Block> bl:bls)
				if(bl.size()>0)
					if (b1.isSimilar(bl.get(0),false)) {
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
	
	private ArrayList<String> scanTextAlphabetWords() {
		ArrayList<String> words;
		
		words=new ArrayList<String>();
		
		for(Page page:pages)
			for(Block block:page.blocks) {
				if(! Block.bodyBlockFilter.filter(block))
					continue;
				
				ArrayList<String> ws = Common.getLetterWords(block.string(),true);
				
				for(String s: ws) {
					int i=Collections.binarySearch(words,s);
					if(i<0) {
						i = -i - 1;
						words.add(i,s);
					}
				}
			}
		
		return words;
	}

	public void print(FileWriter fw) throws IOException {
		fw.write(String.format("Content:\nColumnWidth %d ColumnNumber %d\n",
				columnWidth,columnNumber));
		fw.write(String.format("ContentWidth %d ContentLeft %d ContentRight %d\n",
				contentWidth,contentLeft,contentRight));
		
		if(subtitleFormatChain!=null) {
			fw.write(String.format("Subtitles: "));
			if(ignoreSubtitle) {
				fw.write("Skipped due to ignoreSubtitle is set");
				return;
			}
			for(BlockFormat blockformat:subtitleFormatChain.blockformats)
				fw.write(String.format(" %d",blockformatIndexes.get(blockformat)));
			fw.write("\n\n");
		}
		
		for(Page page:pages) {
			page.print(fw);
		}
	}
	
	public ArrayList<Block> getMainBlocks() {
		Block.BodyBlockFilter bodyBlockFilter=new BodyBlockFilter();
		Block.SubtitleBlockFilter subtitleBlockFilter=new SubtitleBlockFilter();
		Block.SectionBlockFilter sectionBlockFilter=new SectionBlockFilter();

		ArrayList<Block> bs=filterBlocks(bodyBlockFilter,subtitleBlockFilter,sectionBlockFilter);
		bs=removeTailingSections(bs);
		
		return bs;
	}
	
	public String body() {
		String str="";
		
		for(Block block:getMainBlocks()) {
			str+=block.string()+"\n";
		}
		
		str=Common.prepareOut(str);
		
		return str;
	}
	
	public ArrayList<Block> filterBlocks(BlockFilter ...blockFilters) {
		ArrayList<Block> bs=new ArrayList<Block>();
		
		for(Page page:pages) {
			bs.addAll(page.filterBlocks(blockFilters));
		}
		
		return bs;
	}
	
	public ArrayList<Block> removeTailingSections(ArrayList<Block> inputBlocks) {
		boolean reachedLastSubtitle=false;
		boolean bodyFinished=false;
		
		ArrayList<Block> outputBlocks=new ArrayList<Block>();
		
		for(Block block:inputBlocks) {
			if(reachedLastSubtitle && block.type==Common._SectionPrefix)
				bodyFinished=true;
			
			if(bodyFinished)
				break;
			
			if(block==lastSubtitleBlock)
				reachedLastSubtitle=true;
			
			outputBlocks.add(block);
		}
		
		return outputBlocks;
	}
	
	public String subtitles() {
		if(ignoreSubtitle) {
			return "(Not proessed. Skipped due to ignoreSubtitle is set)";
		}
		
		String str="";
		
		for(Page page:pages) {
			str+=page.subtitles()+"\n";
		}
		
		str=Common.prepareOut(str);
		
		return str;
	}
	
	public String title() {
		if(titleBlock==null)
			return "";
		return titleBlock.string();
	}
	
	private Block getTitleBlock() {
		Block titleBlock=null;
		Block block=null;
		BlockFormat titleBlockformat=bodyBlockformat;
		
		for(Page page:pages) {
			if(page.ignored())
				continue;
			
			for(int j=0; j<page.blocks.size();j++) {
				block=page.blocks.get(j);
				
				if(block.type==Common._Body)
					break;
								
				int c=block.format.compareTo(titleBlockformat);
				
				if(c<0)
					continue; 
					
				if(c==0 && titleBlock==null)  {
					if(block.likeTitleBlock()<0)
						continue;
					
					ArrayList<String> strs=Common.getLetterWords(block.string(),true);
					if(Common.hits(allWords,strs) < Common._MinTitleFreqencyRatio)
						continue;
				}
				
				if(c==0 && titleBlock!=null)
					continue;
					
				titleBlock=block;
				titleBlockformat=titleBlock.format;
			}
			
			if(block.type==Common._Body)
				break;
		}
			
		if(titleBlock!=null)
			titleBlock.type=Common._TitleBlock;
		
		return titleBlock;
	}
	
	private Block getAbstractBlock() {
		ArrayList<String> patterns=new ArrayList<String>();
		
		patterns.add("^\\s*[Aa][Bb][Ss][Tt][Rr][Aa][Cc][Tt]\\s*[.:\n]?");
		patterns.add("^\\s*[Ii][Nn][Tt][Rr][Oo][Dd][Uu][Cc][Tt][Ii][Oo][Nn]\\s*[.:\n]?");
		patterns.add("^\\s*[Oo][Vv][Ee][Rr][Vv][Ii][Ee][Ww]\\s*[.:\n]?");
		patterns.add("^\\s*[Ss][Un][Mm][Mm][Aa][Rr][Yy]\\s*[.:\n]?");
		patterns.add("^\\s*[Cc][Oo][Nn][Cc][Ll][Uu][Ss][Ii][Oo][Nn]\\s*[.:\n]?");
		
		for(String pattern:patterns) {
			getKeyBlockStr(
					0,
					Pattern.compile(pattern));
			
			if(activeBlock!=null)
				break;
		}
		if(activeBlock!=null && activeBlock.isParagraphBlock(null)>=Common._ParaSentDefaultUno) {
			activeBlock.type=Common._AbstractBlock;
			return activeBlock;	
		} else {
			for (Page page:pages) {
				for(int i=0; i<page.blocks.size();i++) {
					Block block=page.blocks.get(i);

					if(block.type==Common._Body) {
						return null;
					} 

					if(block.type==Common._PageHeaderBlock || block.type==Common._PageFooterBlock)
						continue;
					
					if(block.isParagraphBlock(null)<Common._ParaSentDefaultTrue)
						continue;
					
					String blockStr=block.string();
					ArrayList<String> strs=Common.getLetterWords(blockStr,true);
						
					if(strs.size()<Common._MinKeyBlockWordNum)
						continue;
					if((Common.hits(allWords,strs) < Common._MinAbstractFreqencyRatio) ||
						(Common.sentenceRatio(blockStr) < Common._MinAbstractSentenceRatio))
						continue;
					
					activeBlock=block;
					activeBlock.type=Common._AbstractBlock;
					//abstractStr=block.string();
					return activeBlock;	
				}
			}
			
			return null;
		}
	}
	
	public String getKeyBlockStr(int skipBlockNumber, Pattern pattern) {
		String str;
		String ret;
	
		boolean stopped=false;
		
		for (Page page:pages) {
			for(int i=0; i<page.blocks.size();i++) {
				if(i<skipBlockNumber)
					continue;
				
				Block block=page.blocks.get(i);
				
				str=block.string();
				str=str.replaceAll("[\\r\\n]+", " ");
				str=str.replaceAll("\\s+", " ");
				
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
					//keep it
					additionalSubtitleFormatFilter.add(chain.blockformats.get(0));
				}
				Block.additionalSubtitleFormatFilter=new AdditionalSubtitleFormatFilter(additionalSubtitleFormatFilter);
				
				return retChain;
			}
		}
	}
	
	private int getIncreasingFormatBlockNumber(ArrayList<Block> blocks, int endBlockIndex) {
		Block block=blocks.get(endBlockIndex);
	
		if(block.likeBodyBlock1()>=Common._ParaSentDefaultTrue) {

			Block block0=block;
			
			int i1=endBlockIndex-1;
			for(; i1>=0; i1--) {
				Block block1=blocks.get(i1);
				
				if(block0.format.compareTo(block1.format)>=0)
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
	
	private void getAllBodyBlocks() {
		ArrayList<Block> bodyBlocks=new ArrayList<Block>();
		
		for(Page page: pages) {
			for(Column column:page.columns)
				for(Block block:column.blocks) {
					if(block.type!="")
						continue;
					
					int lbb=block.likeBodyBlock1();
					if(lbb>=Common._ParaSentDefaultUno)
						bodyBlocks.add(block);
					else if(lbb<Common._ParaSentDefaultFalse)
						continue;
					else {
						if(block.column==null)
							continue;
						
						lbb=block.likeBodyBlock2();
						if(lbb>=Common._ParaSentDefaultUno && block.likeBodyBlock3())
							bodyBlocks.add(block);
					}
				}
		}
		
		for(Block block:bodyBlocks) {
			block.type=Common._Body;
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
	
	@Override
	protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, String unicode, Vector displacement) throws IOException
	{
	    super.showGlyph(textRenderingMatrix, font, code, unicode, displacement);
	    if (unicode == null || unicode.isEmpty())
	    {
	        // do stuff
	    }
	}
}