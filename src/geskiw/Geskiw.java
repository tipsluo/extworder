package geskiw;

import java.io.IOException;

import extworder.Extworder;

public class Geskiw {
	
	public Geskiw(String fn) throws IOException, InterruptedException {
		
	}
	
	public static void main(String args[]) throws IOException, InterruptedException  {
		//String pdfName="Engineer-ILL-Modeling Solute Transport in the WinSRFR S";
		//String pdfName="A model for estimating parameters of rotational landslide";
		String pdfName="2column-Review-Deep learning for the design of photonic structures";
		//Process process=new Process("Wiley-Early life stress and HPA axis");
		Process process=new Process(Extworder.pdfPath(pdfName),Consts._StopWordFile,Consts._AbbreviationFile);
		
		System.out.println(process.extractResult);
		System.out.println("Geskiw Done.");
	}
}
