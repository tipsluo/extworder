package extworder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import extworder.Block.BlockFormat;
import extworder.Content.IgnorePage;
import extworder.Content.NontitleChecker;
import extworder.Page.PageImg;

public class Content2 extends PDFTextStripper {
	private ArrayList<Pattern> abbrPatterns;
	private List<NontitleChecker> nontitleCheckers;
	public IgnorePage ignorePage;
	private boolean ignoreSubtitle=true;
	
	public ArrayList<Page2> pages;
    public BlockFormat bodyBlockformat;
    private int currPid;
    private Page2 currPage=null;
    
    
    //to review if it is needed for content2
	int centralAlignmentAdjustment=0;
	int firstRowIndent=-1;
    
	public Content2(String fn,
			ArrayList<Pattern> abbrPatterns,
			IgnorePage ignorePage,
			boolean ignoreIntraBlock,
			boolean ignoreColoredBlock,
			boolean ignoreSubtitle,
			List<NontitleChecker> nontitleCheckers)  throws IOException {
		
		PDFRenderer renderer;
	
		this.abbrPatterns=abbrPatterns;
		this.ignoreSubtitle=ignoreSubtitle;
		this.ignorePage=ignorePage;
		this.nontitleCheckers=nontitleCheckers;
		
		pages=new ArrayList<Page2>();
		
		File file = new File(fn);
		PDDocument document = PDDocument.load(file);
		
		if(! ignoreColoredBlock)
			renderer = new PDFRenderer(document);
		
		setSortByPosition( true ); 
		
		for (currPid=1; currPid<=document.getNumberOfPages(); currPid++) {
			
			setStartPage(currPid);
			setEndPage(currPid);
			
			if (currPage==null || currPage.id != currPid) {
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
			
			if((page.id==1 && page.ignored()) || page.chars.size()==0) {
				pages.remove(i);
				continue;
			}
			
			i++;
		}
	}
	
	
	@Override
	protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        for (TextPosition text : textPositions) {
        	currPage.writeString(text);
        }
    }
	
	static abstract public class IgnorePage {
		public abstract boolean isIgnored(Page2 page);
	}
}