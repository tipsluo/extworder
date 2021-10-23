package extworder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;

public class Extworder {
	public static void main(String args[]) throws IOException  {
        ArrayList<String> pdfs=new ArrayList<String>();
        pdfs=Common.getAllPDFs();
        
        //pdfs.add("");
        //pdfs.add("Artforum-1995 Painting for Profit and Pleasure");
        //pdfs.add("Archaeology-2020Digital Platforms and the Nature");
        //pdfs.add("APS-Unraveling the Reaction Mechanisms Leading to Partial Fusion");
        //pdfs.add("APS-Search for millicharged particles in proton-proton collisions");
        //pdfs.add("APS-Evidence for CP violation in B");
        //pdfs.add("acs-Classics in Chemical Neuroscience");
        //pdfs.add("Wiley-Early life stress and HPA axis");
		//pdfs.add("NewsBased-A Peek Inside Vendor_Library Partnership to Establish aFirm Order");
        main_test_pdfs(pdfs);
        
		/*main_test1_gettext();
        main_test4_printRows();
		main_test4_getText();
		main_test5_block_display();
		main_test6_block_print(pdfs);
		main_test7_content_print(pdfs);*/
		/*if(pdfs.size()!=0) {
			for(String pdf:pdfs) {
				System.out.println("printContent("+pdf+")");
				printContent(pdf);
			}
			return;
		}*/
		System.out.println("Extworder Done.");
	}
	
	static public void main_test_pdfs(ArrayList<String> pdfs) throws IOException {
		main_test4_getText(pdfs);
		main_test4_printRows(pdfs);
		main_test6_block_print(pdfs);
		main_test7_content_print(pdfs);
	}

	static public void main_test1_gettext(ArrayList<String> pdfs) throws IOException {
		if(pdfs.size()!=0) {
			for(String pdf:pdfs) {
				System.out.println("extract("+pdf+")");
				extract(pdf);
			}
			return;
		}
		//extract("A model for estimating parameters of rotational landslide");
		//extract("Peace-Development and Peace Through");
		//extract("Broader perspective on ecosystem");
		//extract("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		//extract("ILL article-Impact of the KWL reading strategy");
		//extract("Medline-Beneficial Effects of Principal Polyphenols from Green Tea");
		//extract("ALA-Past is Prologue");
		//extract("AMS-PACMAN RENORMALIZATION");
	}//
	
	/*static public void main_test2_printcharinfos(ArrayList<String> pdfs) throws IOException {
		if(pdfs.size()!=0) {
			for(String pdf:pdfs) {
				System.out.println("printCharInfos("+pdf+")");
				printCharInfos(pdf);
			}
			return;
		}
		//printCharInfos("A model for estimating parameters of rotational landslide");
		//printCharInfos("Peace-Development and Peace Through");
		//printCharInfos("Broader perspective on ecosystem");
		//printCharInfos("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		//printCharInfos("ILL article-Impact of the KWL reading strategy");
	}*/
	
	/*static public void main_test3_getTitle() throws IOException {
		getTitle("A model for estimating parameters of rotational landslide");
		getTitle("Peace-Development and Peace Through");
		getTitle("Broader perspective on ecosystem");
		getTitle("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		getTitle("ILL article-Impact of the KWL reading strategy");
    }*/
	
	static public void main_test4_printRows(ArrayList<String> pdfs) throws IOException {
		if(pdfs.size()!=0) {
			for(String pdf:pdfs) {
				System.out.println("displayRows("+pdf+")");
				displayRows(pdf);
			}
			return;
		}
		//displayRows("A model for estimating parameters of rotational landslide");
		//displayRows("Peace-Development and Peace Through");
		//displayRows("Broader perspective on ecosystem");
		//displayRows("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		//displayRows("ILL article-Impact of the KWL reading strategy");
		//displayRows("Medline-Beneficial Effects of Principal Polyphenols from Green Tea");
		//displayRows("AMS-PACMAN RENORMALIZATION");
		//displayRows("ALA-Past is Prologue");
		//displayRows("AIP-Magnetic fields for modulating the nervous system");
		//displayRows("APS-Evidence for CP violation in B");
		//displayRows("Library&Archive-Review Essay-Instruction and Archives");
		//displayRows("AMS-1991-GEODESIC FLOWS, INTERVAL MAPS,");
		//displayRows("acs-A Review on Perovskite-Type LaFeO3");
		//displayRows("Wiley-Early life stress and HPA axis");
		//displayRows("Taylor&Francis-Neurologic music therapy in multidisciplinary acute stroke");
	}
	
	static public void main_test4_getText(ArrayList<String> pdfs) throws IOException {
		if(pdfs.size()!=0) {
			for(String pdf:pdfs) {
				System.out.println("displayChars("+pdf+")");
				displayChars(pdf);
			}
			return;
		}
		//displayChars("A model for estimating parameters of rotational landslide");
		//getText("Peace-Development and Peace Through");
		//getText("Broader perspective on ecosystem");
		//getText("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		//getText("ILL article-Impact of the KWL reading strategy");
		//displayChars("Medline-Beneficial Effects of Principal Polyphenols from Green Tea");
		//displayChars("APS-Evidence for CP violation in B");
		//displayChars("ALA-Past is Prologue");
		//displayChars("AIP-Magnetic fields for modulating the nervous system");
		//displayChars("Library&Archive-Review Essay-Instruction and Archives");
		//displayChars("AMS-PACMAN RENORMALIZATION");
    }
	
	public static void main_test5_block_display() throws IOException {
		//displayBlocks("A model for estimating parameters of rotational landslide");
		//displayBlocks("Peace-Development and Peace Through");
		//displayBlocks("Broader perspective on ecosystem");
		//displayBlocks("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		//displayBlocks("ILL article-Impact of the KWL reading strategy");
		//displayBlocks("NewsBased-A Peek Inside Vendor_Library Partnership to Establish aFirm Order");
		//displayBlocks("APS-Search for millicharged particles in proton-proton collisions");
		//displayBlocks("Medline-Beneficial Effects of Principal Polyphenols from Green Tea");
	}
	
	static public void main_test6_block_print(ArrayList<String> pdfs) throws IOException {
		if(pdfs.size()!=0) {
			for(String pdf:pdfs) {
				System.out.println("printBlocks("+pdf+")");
				printBlocks(pdf);
			}
			return;
		}
		
		//printBlocks("A model for estimating parameters of rotational landslide");
		//printBlocks("Peace-Development and Peace Through");
		//printBlocks("Broader perspective on ecosystem");
		//printBlocks("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		//printBlocks("ILL article-Impact of the KWL reading strategy");
		//printBlocks("Nursing-NewFormat-narrative inquiry approach to understanding");
		//printBlocks("Amer-Trends affecting entry level");
		//printBlocks("NewsBased-A Peek Inside Vendor_Library Partnership to Establish aFirm Order");
		//printBlocks("AMS-PACMAN RENORMALIZATION");
		//printBlocks("APS-Search for millicharged particles in proton-proton collisions");
		//printBlocks("Medline-Beneficial Effects of Principal Polyphenols from Green Tea");
		//printBlocks("acs-A Review on Perovskite-Type LaFeO3");
		//printBlocks("APS-Evidence for CP violation in B"); 
		//printBlocks("APS-Unraveling the Reaction Mechanisms Leading to Partial Fusion"); 
		//printBlocks("ALA-Past is Prologue");
		//printBlocks("No Headings- Moisture assisted perovskite film"); 
		//printBlocks("AIP-Magnetic fields for modulating the nervous system");
		//printBlocks("APS-Evidence for CP violation in B");
		//printBlocks("Library&Archive-Review Essay-Instruction and Archives");
		//printBlocks("Artforum-1995 Painting for Profit and Pleasure");
		//printBlocks("Ad in front-Library-driven approach for fast implementation");
		//printBlocks("Nursing-NewFormat-narrative inquiry approach to understanding");
		//printBlocks("AMS-PACMAN RENORMALIZATION");
		//printBlocks("AMS-1991-GEODESIC FLOWS, INTERVAL MAPS,");
		//printBlocks("APS-Unraveling the Reaction Mechanisms Leading to Partial Fusion");
		//printBlocks("Archaeology-2020Digital Platforms and the Nature");
		//printBlocks("Wiley-Early life stress and HPA axis");
		//printBlocks("Taylor&Francis-Neurologic music therapy in multidisciplinary acute stroke");
		//printBlocks("Engineer-ILL-Modeling Solute Transport in the WinSRFR S");
		//printBlocks("FootNote&Small#-BetweenNegativeStigmaCulturalD");
	}
	
	static public void main_test7_content_print(ArrayList<String> pdfs) throws IOException {
		if(pdfs.size()!=0) {
			for(String pdf:pdfs) {
				System.out.println("printContent("+pdf+")");
				printContent(pdf);
			}
			return;
		}
		
		
		//printContent("A model for estimating parameters of rotational landslide");
		//printContent("Peace-Development and Peace Through");
		//printContent("Broader perspective on ecosystem");
		//printContent("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		//printContent("ILL article-Impact of the KWL reading strategy");
		// pages messed up: printContent("NewsBased-A Peek Inside Vendor_Library Partnership to Establish aFirm Order");
		//printContent("AMS-PACMAN RENORMALIZATION");
		//printContent("AMS-1991-GEODESIC FLOWS, INTERVAL MAPS,");
		//printContent("APS-Search for millicharged particles in proton-proton collisions");
		//printContent("Medline-Beneficial Effects of Principal Polyphenols from Green Tea");
		//printContent("ALA-Past is Prologue");
		//printContent("APS-Evidence for CP violation in B");
		//printContent("Wiley-Early life stress and HPA axis");
		//printContent("Taylor&Francis-Neurologic music therapy in multidisciplinary acute stroke");
		//printContent("No Headings- Moisture assisted perovskite film");  //no obvious abstract
		//printContent("ALA-Past is Prologue");  //big letter
		//printContent("AIP-Magnetic fields for modulating the nervous system"); // big letter, not aligned column
		//printContent("AIP-Magnetic fields for modulating the nervous system");
		//printContent("APS-Evidence for CP violation in B");
		//printContent("Artforum-1995 Painting for Profit and Pleasure");
		//printContent("Ad in front-Library-driven approach for fast implementation");
		//printContent("AMS-PACMAN RENORMALIZATION");
		//printContent("Nursing-NewFormat-narrative inquiry approach to understanding");
		//printContent("APS-Unraveling the Reaction Mechanisms Leading to Partial Fusion"); 
		//printContent("Archaeology-2020Digital Platforms and the Nature");
		//printContent("Engineer-ILL-Modeling Solute Transport in the WinSRFR S");
		//printContent("acs-A Review on Perovskite-Type LaFeO3");
		//printContent("FootNote&Small#-BetweenNegativeStigmaCulturalD");
		//printContent("NewsBased-A Peek Inside Vendor_Library Partnership to Establish aFirm Order");
		//to fix:
		
		//backlog:
		//printContent("AMS-1991-GEODESIC FLOWS, INTERVAL MAPS,");
		//printContent("Archaeology-1896 CRETAN EXPEDITION"); // not resolved , arabic chars
		//printContent("Library&Archive-Review Essay-Instruction and Archives"); //strange. some chars are not be able to be marked.
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
	
	public static void displayChars(String fn) throws IOException {
		Content content = new Content(fn, new IgnorePage(), false, true, true);
		
		FileWriter myWriter = null;
		
		try {
			myWriter= new FileWriter(Common._TestDataDir+fn+"_char2.txt");
		
			for(Page page:content.pages) {
				myWriter.write(String.format("Page: %d left:%d upper:%d\n",page.id,page.left,page.upper));
			    		for(Char ch:page.chars) {
				    		myWriter.write(String.format("%s (x=%d y=%d) width=%d height=%d fontname=%s\n", 
				    				ch.str,ch.left,ch.upper,ch.width,ch.height,ch.fontname));
				    	}
			    	
			}
		} finally {
			  myWriter.close();
		}
	}
	
	public static void displayRows(String fn) throws IOException {
		Content content = new Content(fn, new IgnorePage(), false, true, true);
		
		FileWriter myWriter = null;
		
		try {
			myWriter= new FileWriter(Common._TestDataDir+fn+"_row.txt");
		
			for(Page page:content.pages) {
				myWriter.write(String.format("Page: %d left:%d upper:%d\n",page.id,page.left,page.upper));
				for(Block block:page.blocks)
					for(Row row:block.rows) {
						myWriter.write(String.format("%s\npage=%d block=%d (x=%d y=%d)"+
								" width=%d height=%d fontname=%s wordint.min=%d wordint.max=%d\n", 
								row.string(),row.page.id,row.block.hashCode(),row.left,row.upper,
								row.width,row.height,row.charfont.name,row.charReach, row.wordReach));
					}
			    	
			}
		} finally {
			  myWriter.close();
		}
	}
	
	public static void displayBlocks(String fn) throws IOException {
		Content content = new Content(fn, new IgnorePage(), false, true,true);
		
		FileWriter myWriter = null;
		
		try {
			myWriter= new FileWriter(Common._TestDataDir+fn+"_block.txt");
		
			for(Page page:content.pages) {
				myWriter.write(String.format("Page: %d\n",page.id));
				for(Block block:page.blocks) {
			    	myWriter.write(String.format("Block: left=%d right=%d top=%d bottom=%d ====>\n", block.left,block.right,block.upper,block.lower));
			    	for(Row row:block.rows) {
			    		for(Char ch:row.chars) {
				    		myWriter.write(String.format("%s (x=%d y=%d) width=%f height=%f fontname=%s\n", ch.str,ch.left,ch.upper,ch.width,ch.height,ch.fontname));
				    	}
			    	}
			    }
			}
		} finally {
			  myWriter.close();
		}
	}
	
	public static void printBlocks(String fn) throws IOException {
		Content content = new Content(fn, new IgnorePage(),false,true,true);
		
		FileWriter myWriter= new FileWriter(Common._TestDataDir+fn+"_block2.txt");
		
		content.print(myWriter);
		
		myWriter.close();
	}
	
	static void printContent(String fn) throws IOException {
		Content content = new Content(fn, new IgnorePage(),true,true,true);
		
		FileWriter myWriter= new FileWriter(Common._TestDataDir+fn+"_content.txt");

		myWriter.write(String.format("Title:\n%s\nAbstract:\n%s\n----------------------\n",
				content.title(),content.abstractStr));
		//myWriter.write(String.format("Subtitles:\n"));
		//myWriter.write(content.subtitles());
		myWriter.write(String.format("\n\nText:\n----------------------\n"));
		myWriter.write(content.body());
		
		//java.io.PrintStream p = new java.io.PrintStream(Common._TestDataDir+fn+"_content.txt","UTF-8");
		//p.println(content.text());
		
		//File fileTemp = new File(Common._TestDataDir+fn + "_page1.jpg");
		//ImageIO.write(content.pages.get(1).pageImg.img,"JPEG",fileTemp);
		
		myWriter.close();
	}
	
	/*static class IgnorePage extends Common.IgnorePage {
		public boolean isIgnored(Page page) {
			return false;
		}
	}*/
	
	static class IgnorePage extends Common.IgnorePage {
		final String[] pstr=new String[]{
			"LENDER",
			"BORROWER",
			"SAGE Businesscases",
			"JSTOR is a not-for-profit service that helps scholars"
		};
		
		public boolean isIgnored(Page page) {
			String str=page.string();
			for(int i=0; i<pstr.length; i++)
				if (str.contains(pstr[i]))
					return true;

			return false;
		}
	}
}
