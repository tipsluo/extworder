package geskiw;

import java.io.IOException;

import extworder.Extworder;

public class Geskiw {
	public final static String _TestDataDir="data/";
	
	public Geskiw(String fn) throws IOException, InterruptedException {
		
	}
	
	public static void main(String args[]) throws IOException, InterruptedException  {
		//String pdfName="Engineer-ILL-Modeling Solute Transport in the WinSRFR S";
		//String pdfName="A model for estimating parameters of rotational landslide";
		//String pdfName="2column-Review-Deep learning for the design of photonic structures";
		String pdfName="1Column-An_ultrasensitive_photoelectro";
		//Process process=new Process("Wiley-Early life stress and HPA axis");
		Process process=new Process(
				Extworder.pdfPath(pdfName),
				Consts._StopWordFile,
				Consts._AbbreviationFile,
				Consts._IrregularFile);
		
		String outputPDF=_TestDataDir+pdfName+"_out.pdf";
		process.writePDF(outputPDF);
		
		System.out.println(process.extractResult);
		System.out.println("Geskiw Done.");
	}
}
