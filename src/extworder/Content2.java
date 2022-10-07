package extworder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import extworder.Block2.BlockFormat;
import extworder.Common.IgnorePage;
import extworder.Common.StatGroup;

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
    public Block2 titleBlock;
	public Block2 abstractBlock;
    
    //to review if it is needed for content2
	int centralAlignmentAdjustment=0;
	int firstRowIndent=-1;
    float lowContentWidth, highContentWidth;
	float lowColumnWidth,highColumnWidth,minBodyColumnBlockWidth;
	
	final static float _MaxHeaderFooterWidthRatio=0.5f;
	final static float _MaxHeaderFooterHeightRatio=0.1f;
    
	public Content2(String fn,
			ArrayList<Pattern> abbrPatterns,
			IgnorePage ignorePage,
			boolean ignoreIntraBlock,
			boolean ignoreColoredBlock,
			boolean ignoreSubtitle,
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
		
		List<Rectangle> footerRects=rects.topsByMore(pages.size()-Common._MaxMissingHeaderFooterPageNum);
		
		for(ArrayList<Block2> bl:hbls) {
			for(Block2 b:bl)
                for(Rectangle fr: footerRects)
                    if(b.samePosition(fr)) {
                        b.type=Common._PageHeaderBlock;
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
		
        List<Rectangle> footerRects=rects.topsByMore(pages.size()-Common._MaxMissingHeaderFooterPageNum);
        
        for(ArrayList<Block2> bl:fbls) {
            for(Block2 b:bl)
                for(Rectangle fr: footerRects)
                    if(b.samePosition(fr)) {
                        b.type=Common._PageFooterBlock;
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
	
	@Override
	protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        for (TextPosition text : textPositions) {
        	currPage.writeString(text);
        }
    }
	
	public void print(FileWriter fw) throws IOException {
		/*fw.write(String.format("Content:\nColumnWidth %d ColumnNumber %d\n",
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
		}*/
		
		for(Page2 page:pages) {
			page.print(fw);
		}
	}
}