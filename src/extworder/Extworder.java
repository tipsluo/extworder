package extworder;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
//import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.FileWriter;   // Import the FileWriter class

public class Extworder {
	
	public static void main(String args[]) throws IOException  {
		//main_test1_gettext();
		//main_test2_printcharinfos();
		//main_test3_getTitle();
		//main_test4_getText();
		//main_test5_block_display();
		main_test6_block_print();
		main_test7_content_print();
	}

	static public void main_test1_gettext() throws IOException {
		extract("A model for estimating parameters of rotational landslide");
		//extract("Peace-Development and Peace Through");
		//extract("Broader perspective on ecosystem");
		//extract("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		//extract("ILL article-Impact of the KWL reading strategy");
	}
	
	/*static public void main_test2_printcharinfos() throws IOException {
		printCharInfos("A model for estimating parameters of rotational landslide");
		printCharInfos("Peace-Development and Peace Through");
		printCharInfos("Broader perspective on ecosystem");
		printCharInfos("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		printCharInfos("ILL article-Impact of the KWL reading strategy");
	}*/
	
	/*static public void main_test3_getTitle() throws IOException {
		getTitle("A model for estimating parameters of rotational landslide");
		getTitle("Peace-Development and Peace Through");
		getTitle("Broader perspective on ecosystem");
		getTitle("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		getTitle("ILL article-Impact of the KWL reading strategy");
    }*/
	
	/*static public void main_test4_getText() throws IOException {
		getText("A model for estimating parameters of rotational landslide");
		//getText("Peace-Development and Peace Through");
		//getText("Broader perspective on ecosystem");
		//getText("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		//getText("ILL article-Impact of the KWL reading strategy");
    }*/
	
	static public void main_test5_block_display() throws IOException {
		displayBlocks("A model for estimating parameters of rotational landslide");
		//displayBlocks("Peace-Development and Peace Through");
		//displayBlocks("Broader perspective on ecosystem");
		//displayBlocks("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		//displayBlocks("ILL article-Impact of the KWL reading strategy");
	}
	
	static public void main_test6_block_print() throws IOException {
		printBlocks("A model for estimating parameters of rotational landslide");
		printBlocks("Peace-Development and Peace Through");
		printBlocks("Broader perspective on ecosystem");
		printBlocks("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		printBlocks("ILL article-Impact of the KWL reading strategy");
	}
	
	static public void main_test7_content_print() throws IOException {
		printContent("A model for estimating parameters of rotational landslide");
		printContent("Peace-Development and Peace Through");
		printContent("Broader perspective on ecosystem");
		printContent("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		printContent("ILL article-Impact of the KWL reading strategy");
	}
	
	public static void extract(String fn) throws IOException {
			File file = new File(Common._TestDataDir+fn+".pdf");
			PDDocument document = PDDocument.load(file);
			
			PDDocumentInformation info = document.getDocumentInformation();
			System.out.println( "Title=" + info.getTitle() );
			System.out.println( "Author=" + info.getAuthor() );
			System.out.println( "Subject=" + info.getSubject() );
			System.out.println( "Keywords=" + info.getKeywords() );
			System.out.println( "Creator=" + info.getCreator() );
			System.out.println( "Producer=" + info.getProducer() );
			System.out.println( "Creation Date=" + info.getCreationDate() );
			System.out.println( "Modification Date=" + info.getModificationDate());
			System.out.println( "Trapped=" + info.getTrapped() );   

			PDFTextStripper pdfStripper = new PDFTextStripper();
			
			String text = pdfStripper.getText(document);
			
			try {
			      FileWriter myWriter = new FileWriter(Common._TestDataDir+fn+".txt");
			      
			      myWriter.write("Document Infomation ===========>\n");
			      myWriter.write("Title=" + info.getTitle() +"\n");
			      myWriter.write( "Author=" + info.getAuthor() +"\n");
			      myWriter.write( "Subject=" + info.getSubject() +"\n");
			      myWriter.write( "Keywords=" + info.getKeywords()+"\n" );
			      myWriter.write( "Creator=" + info.getCreator()+"\n" );
			      myWriter.write( "Producer=" + info.getProducer()+"\n" );
			      myWriter.write( "Creation Date=" + info.getCreationDate()+"\n" );
			      myWriter.write( "Modification Date=" + info.getModificationDate()+"\n");
			      myWriter.write( "Trapped=" + info.getTrapped() +"\n"); 
				
			      myWriter.write("\nContext===========>\n");
			      myWriter.write(text);
			      myWriter.close();
			      System.out.println("Successfully wrote to the file.");
			    } catch (IOException e) {
			      System.out.println("An error occurred.");
			      e.printStackTrace();
			    }
			
			document.close();
	}
	
	public static String getMetaTitle(String fn) throws IOException {
		File file = new File(Common._TestDataDir+fn+".pdf");
		PDDocument document = PDDocument.load(file);
		
		PDDocumentInformation info = document.getDocumentInformation();
		
		document.close();
		
		return info.getTitle();
	}
	
	public static void displayBlocks(String fn) throws IOException {
		Content content = new Content(fn);
		
		FileWriter myWriter = null;
		
		try {
			myWriter= new FileWriter(Common._TestDataDir+fn+"_block.txt");
		
			for(Page page:content.pages) {
		    for(Block block:page.blocks) {
		    	myWriter.write(String.format("Block: left=%d right=%d top=%d bottom=%d ====>\n", block.left,block.right,block.top,block.bottom));
		    	for(Row row:block.rows) {
		    		for(Char ch:row.chars) {
			    		myWriter.write(String.format("%s (x=%f y=%f) width=%f height=%f fontname=%s\n", ch.str,ch.x,ch.y,ch.width,ch.height,ch.fontname));
			    	}
		    	}
		    }
			}
		} finally {
			  myWriter.close();
		}
	}
	
	public static void printBlocks(String fn) throws IOException {
		Content content = new Content(fn);
		
		FileWriter myWriter= new FileWriter(Common._TestDataDir+fn+"_block2.txt");
		
		content.print(myWriter);
		
		myWriter.close();
	}
	
	static void printContent(String fn) throws IOException {
		Content content = new Content(fn);
		
		FileWriter myWriter= new FileWriter(Common._TestDataDir+fn+"_content.txt");

		myWriter.write(content.text());
		
		myWriter.close();
	}
}
