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
		//String pdfName="1-2colmn-Confidence_reports_in_decision";
		//Process process=new Process("Wiley-Early life stress and HPA axis");
		//String pdfName="ALA-Past is Prologue";
		//String pdfName="APS-Evidence for CP violation in B";
		//String pdfName="Wiley-Early life stress and HPA axis";
		//String pdfName="AC-CanLoad-1column-Stem Cell Therapy in Heart Diseases";
		//String pdfName="1Column-An_ultrasensitive_photoelectro"; //subheading issue, same font height
		//String pdfName="ScientificReports-2022-Cost-efective fltering of unreliable";
		//String pdfName="Psy-2022-Racial Discrimination Distress Coping Motives";
		//String pdfName="ScientificReports-2017-Detection of American Football Head";
		//String pdfName="Oasis-2018-Drug use among youth and adults";
		//String pdfName="Elsevier-2021-Using ontologies to enhance human";
		//String pdfName="IEEE-2000-Hardware Controls for the STAR Experiment at RHIC";
		String pdfName="CellularPhysiology-2020-Preferential Killing of Tetraploid Colon";

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
