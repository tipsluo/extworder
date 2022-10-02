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

public class Content2 extends PDFTextStripper {
	private ArrayList<Pattern> abbrPatterns;
	private List<Common.NontitleChecker> nontitleCheckers;
	public IgnorePage ignorePage;
	private boolean ignoreSubtitle=true;
	
	public ArrayList<Page2> pages;
    public BlockFormat bodyBlockformat;
    private int currPid;
    private Page2 currPage=null;
	Map<BlockFormat,Integer> blockformatIndexes;
    
    //to review if it is needed for content2
	int centralAlignmentAdjustment=0;
	int firstRowIndent=-1;
    float lowContentWidth, highContentWidth;
	float lowColumnWidth,highColumnWidth,minBodyColumnBlockWidth;
    
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