package extverifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import extworder.Extworder;
import extworder.Content;
import extworder.Content.NontitleChecker;
import geskiw.Consts;
import geskiw.Process;
import geskiw.Process.NontitleBlockStringChecker;
import geskiw.Process.NontitleFirstStringChecker;

public class Extverifier {
	public final static String _TestDataDir="data/";
	
	public Extverifier() throws IOException, InterruptedException {
		
	}
	
	public static void main(String args[]) throws IOException, InterruptedException  {
		ArrayList<String> pdfs=new ArrayList<String>();
		pdfs.add("Engineer-ILL-Modeling Solute Transport in the WinSRFR S");
		pdfs.add("A model for estimating parameters of rotational landslide");
		pdfs.add("2column-Review-Deep learning for the design of photonic structures");
		pdfs.add("1Column-An_ultrasensitive_photoelectro");
		pdfs.add("1-2colmn-Confidence_reports_in_decision");
		pdfs.add("ALA-Past is Prologue");
		pdfs.add("APS-Evidence for CP violation in B");
		pdfs.add("Wiley-Early life stress and HPA axis");
		pdfs.add("AC-CanLoad-1column-Stem Cell Therapy in Heart Diseases");
		pdfs.add("1Column-An_ultrasensitive_photoelectro"); //subheading issue, same font height
		pdfs.add("ScientificReports-2022-Cost-efective fltering of unreliable");
		pdfs.add("Psy-2022-Racial Discrimination Distress Coping Motives");
		pdfs.add("ScientificReports-2017-Detection of American Football Head");
		pdfs.add("Oasis-2018-Drug use among youth and adults");
		pdfs.add("Elsevier-2021-Using ontologies to enhance human");
		pdfs.add("IEEE-2000-Hardware Controls for the STAR Experiment at RHIC");
		pdfs.add("CellularPhysiology-2020-Preferential Killing of Tetraploid Colon");
		pdfs.add("Wiley-Early life stress and HPA axis");
		pdfs.add("Elsevier-2021-Towards security automation in Software Defined");
		pdfs.add("IEEE-2000-Hardware Controls for the STAR Experiment at RHIC");  //seems like ocr
		pdfs.add("Archaeology-2020Digital Platforms and the Nature");
		pdfs.add("Spagna-1998-Dyslexia marker variables(AC2)");
		pdfs.add("ScientificReports-2019-Dental pulp cell-derived powerful inducer");
		pdfs.add("Springer-2004-New approaches to eliciting protective immunity");
		pdfs.add("Elsvier-TheGreenJournal-Mechanisms of radiation-induced endothelium damage");
		pdfs.add("Elsvier-TheGreenJournal-Whole-lung low-dose radiation therapy (LD-RT)");
		pdfs.add("Elsvier-TheGreenJournal-Tracking tumor biology with radiomics2018");
		pdfs.add("Elsvier-TheGreenJournal-Prognostic importance of radiologic extranodal");
		pdfs.add("Expert-Usability");  //format is not consistent.
		pdfs.add("NotWork- Project_muse_2021-Meritorious Heroes");
		pdfs.add("NotWork- Project_muse_2016-Chinese Glass Paintings in Bangkok Monasteries");
		pdfs.add("NotWork- Project_muse_2021-Hands Up Dont Shoot");
		pdfs.add("Project_muse_2012-A Geography of Human Rights Abuses");
		pdfs.add("Project_muse_2014-The Case for Moderate Gun Control");
		pdfs.add("Project_muse_2017-Gun Shops as Local Institutions");
		pdfs.add("Elsevier-2009-Isolation and characterization of human salivary");
		pdfs.add("Elsvier-TheGreenJournal-Tracking tumor biology with radiomics2018");
		pdfs.add("FootNote&Small#-BetweenNegativeStigmaCulturalD");
		pdfs.add("Peace-Development and Peace Through");
		pdfs.add("FootNote&Small#-BetweenNegativeStigmaCulturalD");
		pdfs.add("ILL article-Urban Myths and Rural Legends");
		pdfs.add("Jstor-2020-Escape from Rome");
		pdfs.add("Jstor-2018-Indoor Air Pollution and Infant Mortality");
		pdfs.add("NotWork- Project_muse_2016-Chinese Glass Paintings in Bangkok Monasteries");
		pdfs.add("IEEE-2020-Adaptive Fuzzy Finite-Time Tracking");   // the subtitle is not correct because there are many fomulars.
		pdfs.add("Jstor-2020-Escape from Rome");
		pdfs.add("IEEE-2000-Hardware Controls for the STAR Experiment at RHIC");
		pdfs.add("2018-original-Stem_Cell_Therapy_in_Heart_Dis");
		
		List<NontitleChecker> nontitleCheckers=new ArrayList<Content.NontitleChecker>();
		nontitleCheckers.add(new Process.NontitleFirstStringChecker());
		nontitleCheckers.add(new Process.NontitleBlockStringChecker());
		
		Extworder.main_test_pdfs(pdfs,nontitleCheckers);
		/*Process process=new Process(
				Extworder.pdfPath(pdfName),
				Consts._StopWordFile,
				Consts._AbbreviationFile,
				Consts._IrregularFile);
		String outputPDF=_TestDataDir+pdfName+"_out.pdf";
		process.writePDF(outputPDF,pdfName);
		
		System.out.println(process.extractResult);
		System.out.println("Geskiw Done.`*/
	}
}
