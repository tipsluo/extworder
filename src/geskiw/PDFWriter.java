package geskiw;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;

import extworder.Char;
import extworder.Common;
import extworder.Row;
import extworder.Row.CharFont;

public class PDFWriter {
	private String filename;
	private PDDocument doc;
	PDPage page;
	static PDFont defaultFont=PDType1Font.TIMES_ROMAN;
	int x,y;
	private int left;
	private int upper;
	private int right;
	private int lower;
	private int leading;
	private PDPageContentStream stream;
	private int rowSpace;
	private int charSpace;
	private CharFont bodyCharfont;

	public PDFWriter(String filename, CharFont bodyCharfont) {
		this.filename=filename;
		this.bodyCharfont=bodyCharfont;
        rowSpace=Consts._RowSpace;
        charSpace=Consts._CharSpace;
        leading=(int) ((rowSpace+bodyCharfont.height)*Consts._FontHeightRatio);
        
		doc=new PDDocument();

        newPage();
	}
	
	public void write(CharString cs) {
		for(CharString cs1: prepare(cs,right-left)) {
			printLine(cs1);
			newLine();
		}
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
	        stream.setLeading(leading);
	        
	        x=left;
	        y=upper;

	        stream.newLineAtOffset(left,upper);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int newLine() {
		//y=(int) (y-bodyCharfont.height-rowSpace);
		y=y-leading;
		x=left;
		
		if(y<=lower) {
			newPage();
			return Consts._NewPage; 
		} else {
			try {
				stream.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return Consts._NewLine;
	}
	
	/*private int printChar(Char c) {
		try {
			for (int i = 0; i < c.str.length(); i++) {
				char ch=c.str.charAt(i);
			
				if(! WinAnsiEncoding.INSTANCE.contains(ch)) {
					return -1;
				}
			}
			
			stream.setFont(defaultFont,c.height);
			stream.showText(c.str);
				
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		x+=c.width+charSpace;
		
		if(x<right) {
			return Consts._LineNotEnd;
		} else {
			return newLine();
		}
	}*/
	
	private void printLine(CharString line) {
		float xpSum=0f;
		
		PDFont font=defaultFont;
		
		for(Char c:line.chars) {
			String s="";
			try {
				for (int i = 0; i < c.str.length(); i++) {
					char ch=c.str.charAt(i);
					if(! WinAnsiEncoding.INSTANCE.contains(ch)) {
						s=s+"?";
					} else 
						s=s+ch;
				}
				float fontSize=(float) ((float)c.height * 1000f) /
						(float)(font.getFontDescriptor().getFontBoundingBox().getHeight());
				stream.setFont(font,fontSize);

				float xp=c.height * font.getStringWidth(s) / 1000;
				xpSum+=xp;
				
				stream.newLineAtOffset(0,-c.height);
				
				stream.showText(s);
				
				stream.newLineAtOffset(xp,c.height);
					
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			stream.newLineAtOffset(-xpSum,0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		newLine();
	}
	
	public ArrayList<CharString> prepare(CharString input, int pageWidth) {
		ArrayList<CharString> output=new ArrayList<CharString>();
		
		CharString lineString=new CharString();
		CharString wordString=new CharString();
		
		Char ch;

		float wordLen=0;
		float lastWordX=0;
		
		for(int i=0;i<input.chars.size();i++) {
			ch=input.chars.get(i);
			
			int upper=(int) ((float)(ch.upper-ch.row.upper)*Consts._FontHeightRatio);
			float w=(float)ch.width*Consts._FontWidthRatio;
			wordString.addChar(
					new Char(ch.str,
							Consts._CharBaseLeft,
							upper,
							Consts._CharBaseLeft+w,
							(int)((float)ch.height*Consts._FontHeightRatio),
							ch.font));
			wordLen+=w;
			
			if(ch.str.matches(Common._WordDelimeter)) {
				if(lastWordX+wordLen>=pageWidth) {
					output.add(lineString);
					lineString=new CharString();
					lastWordX=0;
				} 
					
				lineString.addAllChars(wordString);
				lastWordX+=wordLen;
				
				wordLen=0;
				wordString=new CharString();
			}
		}
		
		if(wordString.chars.size()!=0)
			lineString.addAllChars(wordString);
		
		if(lineString.chars.size()!=0)
			output.add(lineString);
		
		return output;
	}

	public void save(){
		try {
			stream.endText();
		    stream.close();
			doc.save(filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
	}
}
