package extworder;

import java.io.File;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;  // Import the IOException class to handle errors


public class Extworder {
	public static void main(String args[]) throws IOException  {
		
		extract("A model for estimating parameters of rotational landslide");
		extract("Peace-Development and Peace Through");
		extract("Broader perspective on ecosystem");
		extract("Taylor&Francis-Purification technology for renewable production of fuel from methan");
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

}
