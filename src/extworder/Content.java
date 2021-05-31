package extworder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Content extends PDFTextStripper {
	protected FileWriter myWriter;
    public TreeMap<Float,Integer> charHeights;
    ArrayList<Char> chars;
	
	public Content(String fn) throws IOException {
		myWriter = new FileWriter(fn+"_char.txt");
		charHeights=new TreeMap<>();
		chars=new ArrayList<Char>();
	}

	@Override
	protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        for (TextPosition text : textPositions) {
            /*System.out.println(text.getUnicode()+ " [(X=" + text.getXDirAdj() + ",Y=" +
                    text.getYDirAdj() + ") height=" + text.getHeightDir() + " width=" +
                    text.getWidthDirAdj() + "]");*/
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
        	//chars.add(new Char(text.getUnicode(), text.getXDirAdj(),text.getYDirAdj(),text.getHeightDir(),text.getWidthDirAdj()));
        	//myWriter.write(text.getUnicode() + " [(X=" + text.getXDirAdj() + ",Y=" +
            //        text.getYDirAdj() + ") height=" + text.getHeightDir() + " width=" +
             //       text.getWidthDirAdj() + "]\n");
        }
        
       // myWriter.close();
    }
	
	public String getTitle() {
		ArrayList<Char> titleChars=new ArrayList<Char>();
		
		for (Char c: chars) {
			if (c.height==charHeights.lastKey()) {
				titleChars.add(c);
			}
		}
		
		if (hasSpace(titleChars)) {
			return getTitleWiSpace(titleChars);
		} else 
			return getTitleWoSpace(titleChars);
	}
	
	private String getTitleWiSpace(ArrayList<Char> titleChars) {
		String ret="";
		
		for (Char c: titleChars) {
			ret=ret+c.str;
		}
		
		return ret;
	}
	
	private String getTitleWoSpace(ArrayList<Char> titleChars) {
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
	
	private boolean hasSpace(ArrayList<Char> titleChars) {
		for (Char c: titleChars) {
			if (c.str.contains(" "))
				return true;
		}
		
		return false;
	}
}