package geskiw;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import extworder.Char;
import extworder.Row.CharFont;

public class PDFWriter {
	private String filename;
	private PDDocument doc;
	PDPage page;
	PDFont font;
	int x,y;
	static private int left;
	static private int upper;
	static private int right;
	static private int lower;
	private PDPageContentStream  stream;
	private int rowSpace;
	private int charSpace;
	private CharFont bodyCharfont;

	public PDFWriter(String filename, CharFont bodyCharfont) throws IOException {
		this.filename=filename;
		this.bodyCharfont=bodyCharfont;
		doc=new PDDocument();

        font=PDType1Font.TIMES_ROMAN;

        newPage();
        
        rowSpace=Consts._RowSpace;
        charSpace=Consts._CharSpace;
        left=Consts._Left;
        upper=Consts._Upper;
        right=Consts._Right;
        lower=Consts._Lower;
	}
	
	public void write(CharString cs) throws IOException {
		for(Char c:cs.chars) {
        	stream.setFont(font, c.height);
        	stream.newLineAtOffset(x, y);
 
        	print(c);
        }
	}
	
	public void newPage() throws IOException {
		if(stream!=null) {
			stream.endText();
			stream.close();
		}
		page=new PDPage();
        doc.addPage(page);
        stream= new PDPageContentStream(doc, page);
        stream.beginText();
        
        x=left;
        y=upper;
	}
	
	public int newLine() throws IOException {
		y=(int) (y+bodyCharfont.height+rowSpace);
		x=left;
		
		if(y>=lower) {
			newPage();
			return Consts._NewPage; 
		}
		
		return Consts._NewLine;
	}
	
	private int print(Char c) throws IOException {
		stream.showText(c.str);
		
		x+=c.width+charSpace;
		
		if(x<right) {
			return Consts._LineNotEnd;
		}
		
		y+=bodyCharfont.height;
		
		if(y<lower) {
			return newLine();
		}
		
		newPage();
		return Consts._NewPage;
	}

	public void save() {
		try {
			stream.endText();
	    	stream.close();
			doc.save(filename);
			doc.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
	        
		}
	}
}
