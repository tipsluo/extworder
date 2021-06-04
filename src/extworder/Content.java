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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Content extends PDFTextStripper {
	protected FileWriter myWriter;
    public TreeMap<Float,Integer> charHeights;
    ArrayList<Char> chars;
    public String title;
	
	public Content(String fn) throws IOException {
		myWriter = new FileWriter(fn+"_char.txt");
		charHeights=new TreeMap<>();
		chars=new ArrayList<Char>();
		
		File file = new File(fn+".pdf");
		PDDocument document = PDDocument.load(file);
		
		setSortByPosition( true );
		setStartPage( 0 );
		setEndPage( document.getNumberOfPages() );
		 
		Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
		try {
			writeText(document, dummy);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if( document != null ) {
                document.close();
            }
        }
	}

	@Override
	protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        for (TextPosition text : textPositions) {
        	Float h;
        	h=text.getHeightDir();
        	Integer n;
        	n=charHeights.compute(h, (k,v) -> (v == null ? 0 : v) + 1);
        	charHeights.put(h,n);
        	String str;
        	str=text.toString();
        	chars.add(new Char(str, text.getXDirAdj(),text.getYDirAdj(),text.getHeightDir(),text.getWidthDirAdj()));
        	try {
	        	myWriter.write(str + " [(X=" + text.getXDirAdj() + ",Y=" +
	                    text.getYDirAdj() + ") height=" + text.getHeightDir() + " width=" +
	                    text.getWidthDirAdj() + "]\n");
        	} catch (IOException e) {
    			e.printStackTrace();
        	}
        }
    }
	
	public String getTitle() {
		/*Map<Float, Integer> sortedMap = charHeights.entrySet().stream()
                .sorted(Entry.comparingByValue())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));*/
		
		return getContent(charHeights.lastKey());
	}
	
	
	public String getText() {
		Float maxHeight = charHeights.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		
		return getContent(maxHeight);
	}
	
	public String getContent(float heightToFilter) {
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
}