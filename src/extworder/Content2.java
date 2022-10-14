package extworder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import extworder.Block2.BlockFormat;
import extworder.Common.IgnorePage;
import extworder.Common.NontitleChecker;
import extworder.Common.StatGroup;
import extworder.Page2.Column2;

public class Content2 extends PDFTextStripper {
	ArrayList<Pattern> abbrPatterns;
	private List<Common.NontitleChecker> nontitleCheckers;
	public IgnorePage ignorePage;
	private boolean ignoreSubtitle=true;
	
	public ArrayList<Page2> pages;
    public BlockFormat bodyBlockformat;
    private int currPid;
    private Page2 currPage=null;
	Map<BlockFormat,Integer> blockformatIndexes;
	private BlockFormatChain2 subtitleFormatChain;
	private Block2 activeBlock;
    public Block2 titleBlock;
	public Block2 abstractBlock;
	Block2 lastSubtitleBlock=null;
	private ArrayList<String> allWords;
    
    //to review if it is needed for content2
	int centralAlignmentAdjustment=0;
	int firstRowIndent=-1;
    float lowContentWidth, highContentWidth;
	float lowColumnWidth,highColumnWidth,minBodyColumnBlockWidth;
    int contentLeft,contentRight,contentWidth;
	int columnNumber,columnWidth;
	
	final static float _MaxHeaderFooterWidthRatio=0.5f;
	final static float _MaxHeaderFooterHeightRatio=0.1f;
	final static int _MaxMissingHeaderFooterPageNum=6;
	final static int _MinFirst3PageCount=1;
	final static float _MinTitleFreqencyRatio=0.65f;
    
	public Content2(String fn,
			ArrayList<Pattern> abbrPatterns,
			IgnorePage ignorePage,
			//boolean ignoreIntraBlock,
			boolean ignoreColoredBlock,
			//boolean ignoreSubtitle,
			List<Common.NontitleChecker> nontitleCheckers)  throws IOException {
		
		this.abbrPatterns=abbrPatterns;
		this.ignoreSubtitle=ignoreSubtitle;
		this.ignorePage=ignorePage;
		this.nontitleCheckers=nontitleCheckers;
		PDFRenderer renderer=null;
		if(! ignoreColoredBlock)
			renderer = new PDFRenderer(document);
		
		pages=new ArrayList<Page2>();
		
		generateChars(PDDocument.load(new File(fn)),renderer,ignoreColoredBlock);
		
		analyze();
	}
	
	private void generateChars(PDDocument document, PDFRenderer renderer, boolean ignoreColoredBlock) throws IOException {
		setSortByPosition( true ); 
		
		for (currPid=1; currPid<=document.getNumberOfPages(); currPid++) {
			
			setStartPage(currPid);
			setEndPage(currPid);
			
			if (currPage==null || currPage.pid != currPid) {
				currPage=new Page2(this,currPid);
			}
			
			Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
			try {
				writeText(document,dummy);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if(! ignoreColoredBlock) {
				renderer = new PDFRenderer(document);
				currPage.pageImg=currPage.new PageImg(renderer.renderImage(currPid-1));
				if(!currPage.pageImg.normal())
					currPage.pageImg=null;
			}
			
			pages.add(currPage);
		}
		
		if( document != null )
             document.close();

		PDPageTree allPages = document.getDocumentCatalog().getPages();
		
		for(int i=0; i<pages.size();) {
			Page2 page=pages.get(i);
			
			if(page.chars.size()==0) {
				pages.remove(i);
				continue;
			}
			
			page.complete(allPages.get(i),ignoreColoredBlock);
			
			if((page.pid==1 && page.ignored()) || page.chars.size()==0) {
				pages.remove(i);
				continue;
			}
			
			i++;
		}
	}
	
	private void analyze() {
		for(Page2 page: pages)
			page.analyze();
		
		markHeaderBlock();
		markFooterBlock();
		for(Page2 page:pages) {
			page.markHeaderFooter();
		}
		
		markContentX();
		if(! makeColumns())
			makeColumn2();
		
		for(Page2 page:pages) {
			// The next two lines should have been able to be removed, but "Wiley-Early..." will fail with "Kessler, & Shaver..." crossing the columns"
			page.updateBlockFormats();
			page.renderStrings();
		}
		
		getBodyFormat();
		markAllBodyBlocks2();
		
		allWords=scanTextAlphabetWords();
		
		titleBlock=getTitleBlock();
		abstractBlock=getAbstractBlock();
		
		markIntraBodyBlocks();
		markSubtitleBlocks2();
	}
	
	private void markHeaderBlock() {
		ArrayList<ArrayList<Block2>> hbls=new ArrayList<ArrayList<Block2>>();
        StatGroup<Rectangle> rects=new StatGroup<Rectangle>();
		
		Page2 page0=pages.get(0);
		float minHeaderWidth=_MaxHeaderFooterWidthRatio * page0.right;
		float headerLower=_MaxHeaderFooterHeightRatio * page0.lower;
		
		for(int i=0; i<pages.size(); i++) {
			Page2 page1=pages.get(i);
			ArrayList<Block2> tbs1=page1.upperBlocks();
			
			for(int j=i+1; j<pages.size(); j++) {
				Page2 page2=pages.get(j);
				ArrayList<Block2> tbs2=page2.upperBlocks();
				
				for(Block2 tb1:tbs1)
					for(Block2 tb2:tbs2) {
						if( tb1.right-tb1.left > minHeaderWidth ||
							tb2.right-tb2.left > minHeaderWidth ||
							tb1.lower > headerLower || 
							tb2.lower > headerLower )
							
							continue;
						
						if(addSimilar(hbls,tb1,tb2))
							rects.add(new Rectangle(tb1));
					}
			}
		}
		
		List<Rectangle> headerRects=rects.topsByMore(pages.size()-_MaxMissingHeaderFooterPageNum);
		
		for(ArrayList<Block2> bl:hbls) {
			for(Block2 b:bl)
                for(Rectangle hr: headerRects)
                    if(b.samePosition(hr)) {
                        b.type=Block2._PageHeaderBlock;
                        break;
                    }
		}
	}
	
	private void markFooterBlock() {
		ArrayList<ArrayList<Block2>> fbls=new ArrayList<ArrayList<Block2>>();
        StatGroup<Rectangle> rects=new StatGroup<Rectangle>();
		
		Page2 page0=pages.get(0);
		float minFooterWidth=_MaxHeaderFooterWidthRatio * page0.right;
		float footerUpper=(1-_MaxHeaderFooterHeightRatio) * page0.lower;
		
		for(int i=0; i<pages.size(); i++) {
			Page2 page1=pages.get(i);
			ArrayList<Block2> bbs1=page1.lowerBlocks();
			
			for(int j=i+1; j<pages.size(); j++) {
				Page2 page2=pages.get(j);
				ArrayList<Block2> bbs2=page2.lowerBlocks();
				
				for(Block2 bb1:bbs1) {
					for(Block2 bb2:bbs2) {
						if( bb1.right-bb1.left > minFooterWidth ||
							bb2.right-bb2.left > minFooterWidth ||
							bb1.upper < footerUpper || 
							bb2.upper < footerUpper )
							
							continue;
						
						if(bb2.isFull(this,bb2.column))
							continue;
						
						if(addSimilar(fbls,bb1,bb2))
							rects.add(new Rectangle(bb1));
					}
				}
			}
		}
		
        List<Rectangle> footerRects=rects.topsByMore(pages.size()-Content2._MaxMissingHeaderFooterPageNum);
        
        for(ArrayList<Block2> bl:fbls) {
            for(Block2 b:bl)
                for(Rectangle fr: footerRects)
                    if(b.samePosition(fr)) {
                        b.type=Block2._PageFooterBlock;
                        break;
                    }
        }
	}
	
	private boolean addSimilar(ArrayList<ArrayList<Block2>> bls, Block2 b1, Block2 b2) {
		if(b1.isSimilar(b2,false)) {							
			boolean found=false;
			
			for(ArrayList<Block2> bl:bls)
				if(bl.size()>0)
					if (b1.isSimilar(bl.get(0),false)) {
						bl.add(b1);
						bl.add(b2);
						found=true;
						break;
					}
			
			if(!found) {
				ArrayList<Block2> bl=new ArrayList<Block2>();
				bl.add(b1);
				bl.add(b2);
				bls.add(bl);
			}
			return true;
		}
		return false;
	}
	
	void markContentX() {
		contentLeft=9999;
		contentRight=-1;
		
		for(Page2 page:pages)
			for(Block2 block:page.blocks) {
				if(contentLeft>block.left)
					contentLeft=block.left;
				if(contentRight<block.right)
					contentRight=block.right;
			}
		
		contentWidth=contentRight-contentLeft+1;
		lowContentWidth=contentWidth*(1-Page2._ColumnWidthAdjustment);
		highContentWidth=contentWidth*(1+Page2._ColumnWidthAdjustment);
		
		columnWidth=columnWidth();
		lowColumnWidth=columnWidth*(1-Page2._ColumnWidthAdjustment);
		highColumnWidth=columnWidth*(1+Page2._ColumnWidthAdjustment);
		minBodyColumnBlockWidth=columnWidth*Block2._MinBodyCharBlockWidth;
		
		if(columnWidth+columnWidth+columnWidth < contentWidth)
			columnNumber=3;
		else if (columnWidth+columnWidth < contentWidth) 
			columnNumber=2;
		else
			columnNumber=1;
		
		centralAlignmentAdjustment=(int) (columnWidth*Page2._CenterAlignAdjustment);
	}
	
	private boolean makeColumns() {
		final int _PageNumber=3;
		boolean columned=true;
		
		int columnedCount=0;
		for(int i=0;i<pages.size();i++) {
			Page2 page=pages.get(i);
			int columnedCount1=page.makeColumns();
			
			columnedCount+=columnedCount1;
			
			if(i>_PageNumber && columnedCount<_MinFirst3PageCount) {
				for(int j=0; j<i+1; j++) {
					Page2 page1=pages.get(j);
					page1.columns=new ArrayList<Column2>();
					for(Block2 block:page1.blocks)
						block.column=null;
				}
				columned=false;
				break;
			}
		}
		
		return columned;
	}
	
	private void makeColumn2() {
		StatGroup<Integer> lefts=new StatGroup<Integer>();
		
		for(Block2 block:getBlockList())
			lefts.add(block.left);
		
		int columnLeft=lefts.maxByValue();
		
		for(int i=0;i<pages.size();i++) {
			Page2 page=pages.get(i);
			
			Column2 column=page.new Column2(columnLeft,page.headerY,page.right,page.footerY);
			page.columns.add(column);
		}
	}
	
	private int columnWidth() {
		Map<Integer,Integer> blockWidths=new TreeMap<Integer,Integer>();
		
		int minColumnWidth=(int)(Page2._ColumnWidthAdjustment * contentWidth);
		
		for(Page2 page:pages)
			for(Block2 block:page.blocks) {
				//if(block.isParagraphBlock(null)<=Common._ParaSentDefaultFalse)
				//	continue;
					
				int w=block.right-block.left+1;
				
				if(w<minColumnWidth)
					continue;
				
				int n=blockWidths.compute(w, (k,v) -> (v == null ? 0 : v) + block.rows.size());
				blockWidths.put(w,n);
			}
		
		Integer i=blockWidths.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return i;
	}
	
	private void getBodyFormat() {
		StatGroup<BlockFormat> blockformats=new StatGroup<BlockFormat>();
		
		for(Page2 page:pages)
			for(Column2 column:page.columns)
				for(Block2 block:column.blocks) {

					if(block.format.alignment==Block2._FULLALIGNED)
						blockformats.add(block.format, (pages.size()-page.pid) * block.rows.size());  // Blocks on the earlier pages get higher weigh than those on late pages.
				}
		
		bodyBlockformat=blockformats.maxByValue();
		
		blockformatIndexes=makeBlockformatIndexes();
	}
	
	private Block2 getTitleBlock() {
		Block2 titleBlock=null;
		Block2 block=null;
		BlockFormat titleBlockformat=bodyBlockformat;
		
		for(NontitleChecker nontitleChecker: nontitleCheckers)
			nontitleChecker.check(this);
		
		boolean broken=false;
		
		for(Page2 page:pages) {
			if(page.ignored())
				continue;
			
			for(int j=0; j<page.blocks.size();j++) {
				broken=false;
				block=page.blocks.get(j);
                if(block.trivial())
                	continue;

				if(block.type==Block2._BodyBlock) {
					broken=true;
					break;
				}
								
				int c=block.format.charfont.compareTo(titleBlockformat.charfont);
				
				if(c<0)
					continue; 
					
				if(c==0 && titleBlock==null)  {
					if(block.likeTitleBlock()<0)
						continue;
					
					ArrayList<String> strs=Common.getLetterWords(block.string(),true);
					if(Common.hits(allWords,strs) < _MinTitleFreqencyRatio)
						continue;
				}
				
				if(c==0 && titleBlock!=null)
					continue;
				
				boolean title=true;
				for(NontitleChecker nontitleChecker: nontitleCheckers) {
					if(! nontitleChecker.select(block)) {
						title=false;
						break;
					}
				}
				if(!title)
					continue;
					
				titleBlock=block;
				titleBlockformat=titleBlock.format;
			}
			
			if(broken)
				break;
		}
			
		if(titleBlock!=null)
			titleBlock.type=Block2._TitleBlock;
		
		return titleBlock;
	}
	
	private Block2 getAbstractBlock() {
		ArrayList<String> patterns=new ArrayList<String>();
		
		patterns.add("^\\s*[Aa][Bb][Ss][Tt][Rr][Aa][Cc][Tt]\\s*[.:\n]?\\s*");
		for(String pattern:patterns) {
			getKeyBlockStr(
					0,
					Pattern.compile(pattern));
			
			if(activeBlock!=null)
				break;
		}

		if(activeBlock!=null) {
			activeBlock.type=Block2._AbstractBlock;
			return activeBlock;	
		}
			
			return null;
	}
	
	public String getKeyBlockStr(int skipBlockNumber, Pattern pattern) {
		String str;
		String ret;
		
		for (Page2 page:pages) {
			for(int i=0; i<page.blocks.size();i++) {
				if(i<skipBlockNumber)
					continue;
				
				Block2 block=page.blocks.get(i);
									
				str=block.string();
				
				str=str.replaceAll("\\s+", "");
				str=str.replaceAll("[\\r\\n]+", " ");
				
				Matcher m = pattern.matcher(str);
				if (m.find()) {
					ret=m.replaceFirst("");
					if (ret.isBlank()) {
						Block2 block1=block.closestBlock();
						ret=block1.string();
						activeBlock=block1;
						return ret;
					}
					
					activeBlock=block;
					return ret;
				}
			}
		}
			
		activeBlock=null;
		return "";
	}
	
	private Map<BlockFormat,Integer> makeBlockformatIndexes() {
		ArrayList<BlockFormat> bfs=new ArrayList<BlockFormat>();
		
		for(Page2 page:pages)
			for(Block2 block:page.blocks) {
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
	
	private void markAllBodyBlocks2() {
		ArrayList<Block2> bodyBlocks=new ArrayList<Block2>();
		
		for(Page2 page: pages) {
			for(Column2 column:page.columns)
				for(Block2 block:column.blocks) {
					if(block.type!="")
						continue;
					
					if(! block.format.equals(bodyBlockformat))
						continue;
					
					bodyBlocks.add(block);
				}
		}
		
		for(Block2 block:bodyBlocks) {
			block.type=Block2._BodyBlock;
		}
	}
	
	private BlockFormatChain2 getSubtitleFormatChain2(ArrayList<Block2> bigBlockList) {
		StatGroup<BlockFormatChain2> candidates=new StatGroup<BlockFormatChain2>();
		
		int i;
		for(i=0;i<bigBlockList.size();i++) {
			Block2 block0=bigBlockList.get(i);

			if(block0.type != Block2._BodyBlock)
				continue;
			
			BlockFormatChain2 bfChain=new BlockFormatChain2();
			
			Block2 block1=block0;
			int j=i-1;
			for(; j>0; j--) {
				Block2 block=bigBlockList.get(j);
				// Only statistic subtitle row number<=1
				if(block.format.compareTo(block1.format)>0 && block.rows.size()<=2) {
					BlockFormat bf=new BlockFormat(block.format);
					if(bf.alignment==Block2._FULLALIGNED) {
						bf.alignment=Block2._LEFTALIGNED;
					}
					bfChain.add(bf);
					block1=block;
				} else {
					break;
				}
			}
				
			if(bfChain.blockformats.size()>0) {
				bfChain.add(0,block0.format);
				candidates.add(bfChain);
			}
		}
		
		ArrayList<BlockFormatChain2> chains=candidates.allKeys();
		for(BlockFormatChain2 chain1:chains) {
			if(candidates.value(chain1)<0)
				continue;
			for(BlockFormatChain2 chain2:chains) {
				if(chain1==chain2 || candidates.value(chain2)<0)
					continue;
				
				int c=chain1.contains(chain2);
				
				if(c>0) {
					candidates.add(chain1,candidates.value(chain2));
					candidates.setValue(chain2,-1);
				} else if(c<0) {
					candidates.add(chain2,candidates.value(chain1));
					candidates.setValue(chain1,-1);
					break;
				}
			}
		}
		
		return candidates.maxByValue();
	}
	
	private void markSubtitleBlocks2() {
		lastSubtitleBlock=null;
		
		ArrayList<Block2> bigBlockList=getBigBlockList2();
		
		subtitleFormatChain=getSubtitleFormatChain2(bigBlockList);
		
		if(subtitleFormatChain==null)
			return;
		
		int i=0;
		List<BlockFormat> formats=subtitleFormatChain.blockformats;
		for(;i<bigBlockList.size();i++) {	
			Block2 block=bigBlockList.get(i);
			
			boolean foundSubtitle=false;
			if(block.format.equals(bodyBlockformat)) {
				int j1=1;
				int j2=i-1;
				for(; j1<formats.size() && j2>=0;) {
					block=bigBlockList.get(j2);
					BlockFormat format=formats.get(j1);
					
					if(format.same(block.format)) {
						block.type=Common.subtitleBlockType(block);
						foundSubtitle=true;
						j1++;
						j2--;
						continue;
					} else if(format.compareTo(block.format)<0) {
						j1++;
						continue;
					} else {
						break;
					}
				}
				if(foundSubtitle && i>1)
					lastSubtitleBlock=bigBlockList.get(i-1);
			}
		}
	}
	
	private void markIntraBodyBlocks() {
		boolean textInfinished=false;
		
		for(Page2 page:pages)
			for(Column2 column:page.columns)
				for(Block2 block:column.blocks) {
					if(block.isBodyInfinished()) {
						textInfinished=true;
						continue;
					}
					
					if(textInfinished)
						if(block.isBodyCharfontFullBlock()) {
							textInfinished=false;
							continue;
						} else if(block.type.isEmpty()) {
							block.type=Block2._IgnoredBlockIntraBody;
						}	
				}
	}
	
	private List<Block2> getBlockList() {
		ArrayList<Block2> bl=new ArrayList<Block2>();
		
		for(Page2 page:pages)
			bl.addAll(page.getBlockList2());
		
		return bl;
	}
	
	private ArrayList<Block2> getBigBlockList2() {
		ArrayList<Block2> blocklist=new ArrayList<Block2>();
		
		for(Page2 page:pages)
			blocklist.addAll(page.getBigBlockList());
		
		return blocklist;
	}
	
	public ArrayList<Block2> getMainBlocks() {
		Block2.BodyBlockFilter bodyBlockFilter=new Block2.BodyBlockFilter();
		Block2.SubtitleBlockFilter subtitleBlockFilter=new Block2.SubtitleBlockFilter();

		ArrayList<Block2> bs=filterBlocks(bodyBlockFilter,subtitleBlockFilter);
		
		return bs;
	}
	
	public ArrayList<Block2> filterBlocks(Block2.BlockFilter ...blockFilters) {
		ArrayList<Block2> bs=new ArrayList<Block2>();
		
		for(Page2 page:pages) {
			bs.addAll(page.filterBlocks(blockFilters));
		}
		
		return bs;
	}
	
	private ArrayList<String> scanTextAlphabetWords() {
		ArrayList<String> words;
		
		words=new ArrayList<String>();
		
		for(Page2 page:pages)
			for(Block2 block:page.blocks) {
				if(! Block2.bodyBlockFilter.filter(block))
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
	
	public String title() {
		if(titleBlock==null)
			return "";
		return titleBlock.string();
	}
	
	public String subtitles() {
		if(ignoreSubtitle) {
			return "(Not proessed. Skipped due to ignoreSubtitle is set)";
		}
		
		String str="";
		
		for(Page2 page:pages) {
			str+=page.subtitles()+"\n";
		}
		
		str=Common.prepareOut(str);
		
		return str;
	}
	
	public String body() {
		String str="";
		
		for(Block2 block:getMainBlocks()) {
			str+=block.string()+"\n";
		}
		
		str=Common.prepareOut(str);
		
		return str;
	}
	
	@Override
	protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        for (TextPosition text : textPositions) {
        	currPage.writeString(text);
        }
    }
	
	public void print(FileWriter fw) throws IOException {
		fw.write(String.format("Content:\nColumnWidth %d ColumnNumber %d\n",
				columnWidth,columnNumber));
		fw.write(String.format("ContentWidth %d ContentLeft %d ContentRight %d\n",
				contentWidth,contentLeft,contentRight));
		
		/*if(subtitleFormatChain!=null) {
			fw.write(String.format("Subtitles: "));
			if(ignoreSubtitle) {
				fw.write("Skipped due to ignoreSubtitle is set");
				return;
			}
			for(BlockFormat blockformat:subtitleFormatChain.blockformats)
				fw.write(String.format(" %d",blockformatIndexes.get(blockformat)));
			fw.write("\n\n");
		}*/
		
		for(Page2 page:pages) {
			page.print(fw);
		}
	}
	
	static class BlockFormatChain2 implements Comparable<BlockFormatChain2> {
		ArrayList<BlockFormat> blockformats=new ArrayList<>();
		
		@Override
		public int compareTo(BlockFormatChain2 blockformatChain) {
			return hashCode() - blockformatChain.hashCode();
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
	        
	        BlockFormatChain2 other = (BlockFormatChain2) obj;

	        return compareTo(other)==0;
		}
		
		void add(int i, BlockFormat blockformat)  {
			blockformats.add(i,blockformat);
		}
		
		void add(BlockFormat blockformat)  {
			blockformats.add(blockformat);
		}
		
		int contains(BlockFormatChain2 chain) {
			int i1=0;
			int i2=0;
			int c=0;
			for(;i1<blockformats.size() && i2<chain.blockformats.size();) {
				c=blockformats.get(i1).compareTo(chain.blockformats.get(i2));
				if(c==0) {
					i1++;
					i2++;
				} else if(c<0)
					i1++;
				else
					i2++;
			}
			
			if(c==0)
				if(blockformats.size() >= chain.blockformats.size())
					return 1;
				else
					return -1;
			else
				return 0;
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