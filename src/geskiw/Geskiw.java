package geskiw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import extworder.Extworder;
import extworder.Content.NontitleChecker;
import geskiw.Process.NontitleBlockStringChecker;
import geskiw.Process.NontitleFirstStringChecker;

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
		//String pdfName="CellularPhysiology-2020-Preferential Killing of Tetraploid Colon";
		//String pdfName="Wiley-Early life stress and HPA axis";
		//String pdfName="Elsevier-2021-Towards security automation in Software Defined";
		//String pdfName="IEEE-2000-Hardware Controls for the STAR Experiment at RHIC";  //seems like ocr
		//String pdfName="Archaeology-2020Digital Platforms and the Nature";
		//String pdfName="Spagna-1998-Dyslexia marker variables(AC2)";
		//String pdfName="ScientificReports-2019-Dental pulp cell-derived powerful inducer";
		//String pdfName="Springer-2004-New approaches to eliciting protective immunity";
		//String pdfName="Elsvier-TheGreenJournal-Mechanisms of radiation-induced endothelium damage";
		//String pdfName="Elsvier-TheGreenJournal-Whole-lung low-dose radiation therapy (LD-RT)";
		//String pdfName="Elsvier-TheGreenJournal-Tracking tumor biology with radiomics2018";
		//String pdfName="Elsvier-TheGreenJournal-Prognostic importance of radiologic extranodal";
		//String pdfName="Expert-Usability";  //format is not consistent.
		//String pdfName="NotWork- Project_muse_2021-Meritorious Heroes";
		//String pdfName="NotWork- Project_muse_2016-Chinese Glass Paintings in Bangkok Monasteries";
		//String pdfName="NotWork- Project_muse_2021-Hands Up Dont Shoot";
		//String pdfName="Project_muse_2012-A Geography of Human Rights Abuses";
		//String pdfName="Project_muse_2014-The Case for Moderate Gun Control";
		//String pdfName="Project_muse_2017-Gun Shops as Local Institutions";
		//String pdfName="Elsevier-2009-Isolation and characterization of human salivary";
		//String pdfName="Elsvier-TheGreenJournal-Tracking tumor biology with radiomics2018";
		//String pdfName="FootNote&Small#-BetweenNegativeStigmaCulturalD";
		//String pdfName="Peace-Development and Peace Through";
		//String pdfName="FootNote&Small#-BetweenNegativeStigmaCulturalD";
		//String pdfName="ILL article-Urban Myths and Rural Legends";
		//String pdfName="Jstor-2020-Escape from Rome";
		String pdfName="Jstor-2018-Indoor Air Pollution and Infant Mortality";

		ArrayList<String> pdfs=new ArrayList<String>();
		pdfs.add(pdfName);
		
		List<NontitleChecker> nontitleCheckers=new ArrayList<NontitleChecker>();
		nontitleCheckers.add(new NontitleFirstStringChecker());
		nontitleCheckers.add(new NontitleBlockStringChecker());
		
		Extworder.main_test_pdfs(pdfs,nontitleCheckers);
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
