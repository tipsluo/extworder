package extworder;

import java.io.File;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.text.PDFTextStripper;


public class Extworder {
	public static void main(String args[]) throws IOException {
		//Loading an existing document
		File file = new File("Peace-Development and Peace Through.pdf");
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
		System.out.println(text);
		//Closing the document
		document.close();
	}
}
