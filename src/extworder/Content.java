package extworder;

import java.io.IOException;

import org.apache.pdfbox.text.PDFTextStripper;

public class Content extends PDFTextStripper {

	public Content() throws IOException {
		super();
		// TODO Auto-generated constructor stub
	}
 /*   public Block activeBlock;
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
	BlockFormatChain2 subtitleFormatChain;
	int firstRowIndent=-1;
	int bgRGB;
	ArrayList<Pattern> abbrPatterns;
	List<NontitleChecker> nontitleCheckers;
    IgnorePage ignorePage;
	
	public Content(String fn,
				ArrayList<Pattern> abbrPatterns,
				IgnorePage ignorePage,
				boolean ignoreIntraBlock,
				boolean ignoreColoredBlock,
				boolean ignoreSubtitle,
				List<NontitleChecker> nontitleCheckers)  throws IOException {
		
		this.abbrPatterns=abbrPatterns;
		this.ignoreSubtitle=ignoreSubtitle;
		this.ignorePage=ignorePage;
		this.nontitleCheckers=nontitleCheckers;
		
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
			
			if((page.id==1 && page.ignored()) || page.chars.size()==0) {
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
		if(! makeColumns())
			makeColumn2();
		
		for(Page page:pages) {
			page.separateAllUppers();
			
			// The next two lines should have been able to be removed, but "Wiley-Early..." will fail with "Kessler, & Shaver..." crossing the columns"
			page.updateBlockFormats();
			page.renderStrings();
		}
		
		mergeBlocksByGap();
		
		getFirstRowIndent();
		
		getBodyFormat();
		markAllBodyBlocks();
		
		allWords=scanTextAlphabetWords();
		
		titleBlock=getTitleBlock();
		abstractBlock=getAbstractBlock();
		
		if(ignoreIntraBlock)
			markIntraBodyBlocks();
		
		if(! ignoreSubtitle)
			markSubtitleBlocks2();
	}

	@Override
	protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        for (TextPosition text : textPositions) {
        	currPage.writeString(text);
        }
    }
	
	private void getBodyFormat() {
		StatGroup<BlockFormat> blockformats=new StatGroup<BlockFormat>();
		
		for(Page page:pages)
			for(Column column:page.columns)
				for(Block block:column.blocks) {

					if(block.format.alignment==Common._FULLALIGNED)
						blockformats.add(block.format, (pages.size()-page.id) * block.rows.size());  // Blocks on the earlier pages get higher weigh than those on late pages.
				}
		
		bodyBlockformat=blockformats.maxByValue();
		
		blockformatIndexes=makeBlockformatIndexes();
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
				
				int n=blockWidths.compute(w, (k,v) -> (v == null ? 0 : v) + block.rows.size());
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
						
						row1.merge(row2);
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
        StatGroup<Rectangle> rects=new StatGroup<Rectangle>();
		
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
						
						if(addSimilar(hbls,tb1,tb2))
							rects.add(new Rectangle(tb1));
					}
			}
		}
		
		List<Rectangle> footerRects=rects.topsByMore(pages.size()-Common._MaxMissingHeaderFooterPageNum);
		
		for(ArrayList<Block> bl:hbls) {
			for(Block b:bl)
                for(Rectangle fr: footerRects)
                    if(b.samePosition(fr)) {
                        b.type=Common._PageHeaderBlock;
                        break;
                    }
		}
	}
	
	private void markFooterBlock() {
		ArrayList<ArrayList<Block>> fbls=new ArrayList<ArrayList<Block>>();
        StatGroup<Rectangle> rects=new StatGroup<Rectangle>();
		
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
						
						if(addSimilar(fbls,bb1,bb2))
							rects.add(new Rectangle(bb1));
					}
				}
			}
		}
		
        List<Rectangle> footerRects=rects.topsByMore(pages.size()-Common._MaxMissingHeaderFooterPageNum);
        
        for(ArrayList<Block> bl:fbls) {
            for(Block b:bl)
                for(Rectangle fr: footerRects)
                    if(b.samePosition(fr)) {
                        b.type=Common._PageFooterBlock;
                        break;
                    }
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
	
	private void markSubtitleBlocks2() {
		lastSubtitleBlock=null;
		
		ArrayList<Block> bigBlockList=getBigBlockList2();
		
		subtitleFormatChain=getSubtitleFormatChain2(bigBlockList);
		
		if(subtitleFormatChain==null)
			return;
		
		int i=0;
		List<BlockFormat> formats=subtitleFormatChain.blockformats;
		for(;i<bigBlockList.size();i++) {	
			Block block=bigBlockList.get(i);
			
			boolean foundSubtitle=false;
			if(block.likeBodyBlock1()>=Common._ParaSentDefaultTrue) {
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
	
	private boolean makeColumns() {
		final int _PageNumber=3;
		boolean columned=true;
		
		int columnedCount=0;
		for(int i=0;i<pages.size();i++) {
			Page page=pages.get(i);
			int columnedCount1=page.makeColumns();
			
			columnedCount+=columnedCount1;
			
			if(i>_PageNumber && columnedCount<Common._MinFirst3PageCount) {
				for(int j=0; j<i+1; j++) {
					Page page1=pages.get(j);
					page1.columns=new ArrayList<Column>();
					for(Block block:page1.blocks)
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
		
		for(Block block:getBlockList())
			lefts.add(block.left);
		
		int columnLeft=lefts.maxByValue();
		
		for(int i=0;i<pages.size();i++) {
			Page page=pages.get(i);
			
			Column column=page.new Column(columnLeft,page.headerY,page.right,page.footerY);
			page.columns.add(column);
		}
	}
	
	private boolean addSimilar(ArrayList<ArrayList<Block>> bls, Block b1, Block b2) {
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
			return true;
		}
		return false;
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

		ArrayList<Block> bs=filterBlocks(bodyBlockFilter,subtitleBlockFilter);
		
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
		
		for(NontitleChecker nontitleChecker: nontitleCheckers)
			nontitleChecker.check(this);
		
		boolean broken=false;
		
		for(Page page:pages) {
			if(page.ignored())
				continue;
			
			for(int j=0; j<page.blocks.size();j++) {
				broken=false;
				block=page.blocks.get(j);
                if(block.trivial())
                	continue;

				if(block.type==Common._Body) {
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
					if(Common.hits(allWords,strs) < Common._MinTitleFreqencyRatio)
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
			titleBlock.type=Common._TitleBlock;
		
		return titleBlock;
	}
	
	private Block getAbstractBlock() {
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
			activeBlock.type=Common._AbstractBlock;
			return activeBlock;	
		}
			
			return null;
		//} 
	}
	
	public String getKeyBlockStr(int skipBlockNumber, Pattern pattern) {
		String str;
		String ret;
		
		for (Page page:pages) {
			for(int i=0; i<page.blocks.size();i++) {
				if(i<skipBlockNumber)
					continue;
				
				Block block=page.blocks.get(i);
									
				str=block.string();
				
				str=str.replaceAll("\\s+", "");
				str=str.replaceAll("[\\r\\n]+", " ");
				
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
		}
			
		activeBlock=null;
		return "";
	}
	
	private BlockFormatChain2 getSubtitleFormatChain2(ArrayList<Block> bigBlockList) {
		StatGroup<BlockFormatChain2> candidates=new StatGroup<BlockFormatChain2>();
		
		int i;
		for(i=0;i<bigBlockList.size();i++) {
			Block block0=bigBlockList.get(i);

			if(block0.type != Common._Body)
				continue;
			
			BlockFormatChain2 bfChain=new BlockFormatChain2();
			
			Block block1=block0;
			int j=i-1;
			for(; j>0; j--) {
				Block block=bigBlockList.get(j);
				// Only statistic subtitle row number<=1
				if(block.format.compareTo(block1.format)>0 && block.rows.size()<=2) {
					BlockFormat bf=new BlockFormat(block.format);
					if(bf.alignment==Common._FULLALIGNED) {
						bf.alignment=Common._LEFTALIGNED;
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
	
	private ArrayList<Block> getBigBlockList2() {
		ArrayList<Block> blocklist=new ArrayList<Block>();
		
		for(Page page:pages)
			blocklist.addAll(page.getBigBlockList());
		
		return blocklist;
	}
	
	private void markAllBodyBlocks() {
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
	
	private void markAllBodyBlocks2() {
		for(Page page: pages) {
			for(Column column:page.columns)
				for(Block block:column.blocks) {
					if(block.type!="")
						continue;
					if(page.content.bodyBlockformat!=null &&
							! block.format.equals(page.content.bodyBlockformat))
						continue;
					
				}
		}
	}
	
	
	private void mergeBlocksByGap() {
		Map<Float,Integer> gaps=getRowGaps2();
		
		for(Page page:pages) {
			Block block0=null;
			int i;
			
			List<Block> blocklist=page.getBlockList2();
			for(i=0; i<blocklist.size(); i++) {
				Block block=blocklist.get(i);
	
				Integer gap=gaps.get(block.format.charfont.height);
				if(gap==null)
					continue;
				
				if(block0==null ||
						(!block.format.same(block0.format) ||
						block.upper-block0.lower > gap ||
						block.hOverlap(block0) < Common._MinMergeOverlapRatio)
						)
					block0=block;
				else {
					block0.merge(block);
					blocklist.remove(block);
					i--;
				}
			}
			page.sortBlocks();
		}
	}
	
	private Map<Float,Integer> getRowGaps2() {
		HashMap<Float,Integer> gaps=new HashMap<Float,Integer>();
		
		HashMap<Float,StatGroup<Integer>> allGaps=new HashMap<Float,StatGroup<Integer>>();
		
		for(Page page:pages) {
			Row row0=null;
			Collections.sort(page.rows,Row.compareRowLefts);
			for(Row row:page.rows) {
				if(row0==null || row0.left!=row.left || row0.charfont.height!=row.charfont.height)
					row0=row;
				else {
					StatGroup<Integer> sg=allGaps.get(row.charfont.height);
					if(sg!=null)
						sg.add(row.upper-row0.lower);
					else {
						sg=new StatGroup<Integer>();
						sg.add(row.upper-row0.lower);
						allGaps.put(row.charfont.height,sg);
					}
					row0=row;
				}
			}
			Collections.sort(page.rows,Row.compareRows);
		}
		
        for(Map.Entry<Float,StatGroup<Integer>> entry : allGaps.entrySet()) {
        	gaps.put(entry.getKey(),entry.getValue().maxByValue());
        }
        
        return gaps;
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
	
	private void getFirstRowIndent() {
		List<Block> blocks=getBlockList();
		StatGroup<Integer> indents=new StatGroup<Integer>();
		
		for(Block b:blocks) {
			if(b.rows.size()==0)
				continue;
			
			int i=b.rows.get(0).left - b.left;
			
			if(i<=0)
				continue;
			
			indents.add(i);
		}
		
		Integer ind=indents.maxByValue();
		if(ind==null)
			return;
		else 
			firstRowIndent=ind;
		
		for(Block b:blocks) {
			b.updateFormat();
		}
	}
	
	private List<Block> getBlockList() {
		ArrayList<Block> bl=new ArrayList<Block>();
		
		for(Page page:pages)
			bl.addAll(page.getBlockList());
		
		return bl;
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
	
*/
}