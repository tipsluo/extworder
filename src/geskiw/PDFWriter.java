package geskiw;

import java.io.IOException;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;

import extworder.Char;
import extworder.Row.CharFont;

public class PDFWriter {
	private String filename;
	private PDDocument doc;
	PDPage page;
	static PDFont defaultFont=PDType1Font.COURIER;
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
        leading=(int) (rowSpace+bodyCharfont.height);
        
		doc=new PDDocument();

        newPage();
	}
	
	public void write(CharString cs) {
		//print(cs.chars.get(0));
		for(Char c:cs.chars) {
        	print(c);
        }
		//stream.setFont(defaultFont,12);
		//stream.showText("abcdefg");
	}
	
	public void newPage() {
		if(stream!=null) {
			try {
				stream.endText();
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				
			}
		}
		page=new PDPage();
        doc.addPage(page);
        
        PDRectangle mediabox = page.getMediaBox();
        //float width = mediabox.getWidth() - 2*Consts._Margin;
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
		y=(int) (y-bodyCharfont.height-rowSpace);
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
	
	private int print(Char c) {
		try {
			//if(c.font==null)
				//stream.setFont(defaultFont,c.height);
			//else
			//	stream.setFont(c.font, c.height);
			for (int i = 0; i < c.str.length(); i++) {
				char ch=c.str.charAt(i);
			
				if(! WinAnsiEncoding.INSTANCE.contains(ch)) {
					return -1;
				}
			}
			
			stream.setFont(defaultFont,c.height);
			stream.showText(c.str);
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		/*} catch (IOException e) {
			e.printStackTrace();
			try {
				stream.setFont(c.font, c.height);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}*/
		
		x+=c.width+charSpace;
		
		if(x<right) {
			return Consts._LineNotEnd;
		} else {
			return newLine();
		}
		/*if(y>=lower) {
			return newLine();
		} else {
			newPage();
			return Consts._NewPage;
		}*/
	}

	public void save(){
		try {
			stream.endText();
		    stream.close();
			doc.save("temp.pdf");
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
