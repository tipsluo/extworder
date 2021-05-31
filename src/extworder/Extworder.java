package extworder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
//import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;  // Import the IOException class to handle errors


public class Extworder {
	public static void main(String args[]) throws IOException  {
		main_test1_gettext();
		//main_test2_printcharinfos();
	}

	static public void main_test1_gettext() throws IOException {
		extract("A model for estimating parameters of rotational landslide");
		extract("Peace-Development and Peace Through");
		extract("Broader perspective on ecosystem");
		extract("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		extract("ILL article-Impact of the KWL reading strategy");
	}
	
	static public void main_test2_printcharinfos() throws IOException {
		printCharInfos("A model for estimating parameters of rotational landslide");
		printCharInfos("Peace-Development and Peace Through");
		printCharInfos("Broader perspective on ecosystem");
		printCharInfos("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		printCharInfos("ILL article-Impact of the KWL reading strategy");
	}
	
	static public void printCharInfos(String fn) throws IOException {
		File file = new File(fn+".pdf");
		PDDocument document = PDDocument.load(file);
		
		Content content = new Content(fn);
		content.setSortByPosition( true );
		content.setStartPage( 0 );
		content.setEndPage( document.getNumberOfPages() );
		 
		Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
		try {
			content.writeText(document, dummy);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if( document != null ) {
                document.close();
            }
        }
		
        Float maxHeight = content.charHeights.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
        //System.out.printf("Highest: %f, Max Height Number: %f\nTitle: %s\n",content.charHeights.lastKey(), maxHeight,content.getTitle());
        System.out.printf("PDF: %s\nTitle: %s\n",fn,content.getTitle());
	}
	
	public static void extract(String fn) throws IOException {
	//Loading an existing document
			File file = new File(fn+".pdf");
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

			//Instantiate PDFTextStripper class
			PDFTextStripper pdfStripper = new PDFTextStripper();
			
			//Retrieving text from PDF document
			String text = pdfStripper.getText(document);
			//System.out.println(text);

			
			try {
			      FileWriter myWriter = new FileWriter(fn+".txt");
			      
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
			
			//Closing the document
			document.close();
	}
	
	/*public static void test(PDDocument document, PDFTextStripper pdfStripper) {
		//List<PDPage> allPages = document.getDocumentCatalog().getPages();
        //for (int i = 0; i < allPages.size(); i++) {
		
		//int num=document.getNumberOfPages();
		
		for (PDPage page:document.getPages()) {
            //PDPage page = (PDPage) allPages.get(i);
            //System.out.println("Processing page: " + i);
            InputStream contents = page.getContents();
            if (contents != null) {
            	pdfStripper.processStream(page, page.findResources(), page.getContents().getStream());
            }
        }
	}*/
}
