package geskiw;

import java.io.IOException;
import java.awt.Color;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;


import extworder.Common;

public class PDFWriter {
/*	private String filename;
	private PDDocument doc;
	PDPage page;
	static PDFont summaryFont=PDType1Font.TIMES_ROMAN;
	static PDFont summaryBoldFont=PDType1Font.TIMES_BOLD;
	static PDFont summaryItalicFont=PDType1Font.TIMES_ITALIC;
	static PDFont resultFont=PDType1Font.COURIER;
	static PDFont resultBoldFont=PDType1Font.COURIER_BOLD;
	private float x,y;
	private int left;
	private int upper;
	private int right;
	private int lower;
	private float leading;
	private PDPageContentStream stream;
	private int rowSpace;
	private CharFont bodyCharfont;
	

	public PDFWriter(String filename, CharFont bodyCharfont) {
		x=0;y=0;
		this.filename=filename;
		this.bodyCharfont=bodyCharfont;
        rowSpace=Consts._RowSpace;
        leading=rowSpace+bodyCharfont.height + Consts._AddtionalFontHeight;  //init value only
        
		doc=new PDDocument();

        newPage();
	}
	
	public void write(CharString cs,PDFont pdFont) {
		printCharString(cs,pdFont);
	}
	
	public void newPage() {
		if(stream!=null) {
			try {
				stream.endText();
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				
			}
		}
		page=new PDPage(PDRectangle.A4);
        doc.addPage(page);
        
        PDRectangle mediabox = page.getMediaBox();
        left=(int)mediabox.getLowerLeftX()+Consts._Margin;
        lower=(int)Consts._Margin;
        right=(int)mediabox.getWidth()-Consts._Margin;
        upper=(int)mediabox.getHeight()-Consts._Margin;
        
        try {
			stream=new PDPageContentStream(doc, page);
	        stream.beginText();
	        
	        stream.newLineAtOffset(left,upper);
	        
	        x=left;
	        y=upper;

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int newLine() {
		float y1=y-leading;

		if(y1-leading<=lower) {
			newPage();
			return Consts._NewPage; 
		} else {
			try {
				stream.newLineAtOffset(left-x,-leading);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			x=left;
			y=y1;
		}
		
		return Consts._NewLine;
	}
	
	private void printCharString(CharString line,PDFont pdFont) {
		leading=line.height+Consts._AddtionalFontHeight;
				
		CharString word=new CharString();
		
		for(int i=0; i<line.chars.size();i++) {
			Char c=line.chars.get(i);
			
			word.addChar(replaceUnknownCharacter(c));
			
			if(Pattern.compile(Common._WordDelimeter).matcher(c.str).find()) {
				printWord(word,pdFont);
				word=new CharString();
			}
		}
		
		if(word.chars.size()>0)
			printWord(word,pdFont);
	}
	
	private void printWord(CharString word,PDFont pdFont) {
		float xSum=0;
		
		for(Char ch: word.chars) {
			xSum+=getCharPrintWidth(ch,pdFont);
		}
		
		if(x+xSum>=right) {
			newLine();
		}
		
		for(Char ch: word.chars) {
			printChar(ch,pdFont);
		}
	}
	
	private void printChar(Char ch,PDFont pdFont) {
		float h=ch.height + Consts._AddtionalFontHeight;
		
		setFontSize(pdFont,h);
		float xp=0;
		try {
			xp = h * pdFont.getStringWidth(ch.str) / 1000;

			stream.newLineAtOffset(0,ch.row.lower-ch.lower);
			stream.showText(ch.str);
			x+=xp;
			stream.newLineAtOffset(xp,ch.lower-ch.row.lower);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private float getCharPrintWidth(Char ch, PDFont pdFont) {
		float h=Consts._AddtionalFontHeight + ch.height;
		setFontSize(pdFont,h);
		float xp=0;
		try {
			xp = h * pdFont.getStringWidth(ch.str) / 1000;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return xp;
	}
	
	public void drawHorizenLine() {
		newLine();
		try {
			stream.endText();
			stream.setNonStrokingColor(Color.DARK_GRAY);
			stream.addRect(x, y, right-x, 0.2f);
			stream.fill();
			stream.beginText();
			y-=1;
			x=left;
	        stream.newLineAtOffset(left,y);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private float setFontSize(PDFont font, float height) {
		float fontSize=((height ) * 1000f) /
				(font.getFontDescriptor().getFontBoundingBox().getHeight());
		try {
			stream.setFont(font,fontSize);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return fontSize;
	}
	
	private Char replaceUnknownCharacter(Char c) {
		String s="";

		for (int i = 0; i < c.str.length(); i++) {
			char ch=c.str.charAt(i);
			if(! WinAnsiEncoding.INSTANCE.contains(ch)) {
				s=s+" ";
			} else 
				s=s+ch;
		}

		return new Char(c,s,c.width,c.height);
	}

	public void save(){
		try {
			stream.endText();
		    stream.close();
			doc.save(filename);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (doc != null) {
		            doc.close();
		        }			
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}*/
}
