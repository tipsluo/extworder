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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Content extends PDFTextStripper {
	ArrayList<Page> pages;
    float textCharHeight,titleCharHeight;
	public TreeMap<Float,Integer> charHeights;
    Map<Float,Integer> charHeightIndexes;
    int currPid;
    Page currPage=null;
	
	public Content(String fn)  throws IOException{
		pages=new ArrayList<Page>();
		charHeights=new TreeMap<>();
		
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
		
		titleCharHeight=charHeights.lastKey();
		textCharHeight = charHeights.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		charHeightIndexes=makeCharHeightIndexes();
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
	
	private Map<Float,Integer> makeCharHeightIndexes() {
		float[] heights = new float[charHeights.size()];
		int i=0;
		for (Float h : charHeights.keySet()) {
	        heights[i++]=h;
		}
		Arrays.sort(heights);
		
		int textHeightIndex=-1;
		for(i = 0; i<heights.length;i++ )
            if(heights[i] == textCharHeight) {
            	textHeightIndex = i;
                break;
            }
		
		Map<Float,Integer> heightIndexes=new HashMap<>();
		for(i=0; i<heights.length;i++ ) {
			heightIndexes.put(heights[i],i-textHeightIndex);
		}
		
		return heightIndexes;
	}
	
	public void print(FileWriter fw) throws IOException {
		for(Page page:pages) {
			page.print(fw);
		}
	}

	/*public String getTitle() {
		return getContent(charHeights.lastKey());
	}
	
	
	public String getText() {
		Float maxHeight = charHeights.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		
		return getContent(maxHeight);
	}*/
	
	/*public String getContent(float heightToFilter) {
		ArrayList<Char> titleChars=getContentWiHeight(heightToFilter);
		
		if (hasSpace(titleChars))
			return getContentWiSpace(titleChars);
		else 
			return getContentWoSpace(titleChars);
	}
	
	private String getContentWiSpace(ArrayList<Char> titleChars) {
		String ret="";
		
		for (Char c: titleChars) {
			ret=ret+c.str;
		}
		
		return ret;
	}
	
	private String getContentWoSpace(ArrayList<Char> titleChars) {
		float IgnoredSpaceWidthRatio=0.0f;
		
		String ret="";
		float lastX=-1;
		float lastY=-1;
		float space;
		
		for (Char c: titleChars) {
			if (lastX<0 || lastY<0 || c.y!=lastY) {
				space=-1;
			} else
				space=c.x - lastX;
			
			if(space<0 || space>IgnoredSpaceWidthRatio*c.width)
				if(ret.length()>0)
					ret=ret+" ";
			
			ret=ret+c.str;
			
			lastX=c.x+c.width;
			lastY=c.y;
		}
		
		return ret;
	}
	
	public ArrayList<Char> getContentWiHeight(float height) {
		ArrayList<Char> contentChars=new ArrayList<Char>();
		
		for (Char c: chars) {
			if (c.height==height) {
				contentChars.add(c);
			}
		}
		
		return contentChars;
	}
	
	private boolean hasSpace(ArrayList<Char> titleChars) {
		for (Char c: titleChars) {
			if (c.str.contains(" "))
				return true;
		}
		
		return false;
	}
	
	public boolean isInChars(float x,float y) {
		for (Char ch:chars) {
			if (ch.coverPoint(x,y)) return true;
		}
		return false;
	}
	
	 */
}