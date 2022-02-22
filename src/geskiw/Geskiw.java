package geskiw;

import java.io.IOException;
import java.util.ArrayList;

import extworder.Extworder;

public class Geskiw {
	public final static String _TestDataDir="data/";
	
	public Geskiw(String fn) throws IOException, InterruptedException {
		
	}
	
	public static void main(String args[]) throws IOException, InterruptedException  {
		//String pdfName="Engineer-ILL-Modeling Solute Transport in the WinSRFR S";
		//String pdfName="A model for estimating parameters of rotational landslide";
		//String pdfName="2column-Review-Deep learning for the design of photonic structures";
		//String pdfName="1Column-An_ultrasensitive_photoelectro";
		String pdfName="1-2colmn-Confidence_reports_in_decision";
		//Process process=new Process("Wiley-Early life stress and HPA axis");
		
		ArrayList<String> pdfs=new ArrayList<String>();
		pdfs.add(pdfName);
		
		Extworder.main_test_pdfs(pdfs);
		Process process=new Process(
				Extworder.pdfPath(pdfName),
				Consts._StopWordFile,
				Consts._AbbreviationFile,
				Consts._IrregularFile);
		
		String outputPDF=_TestDataDir+pdfName+"_out.pdf";
		process.writePDF(outputPDF,pdfName);
		
		System.out.println(process.extractResult);
		System.out.println("Geskiw Done.");
	}
}
