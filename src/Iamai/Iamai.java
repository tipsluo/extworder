package Iamai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import geskiw.Geskiw;
import geskiw.Process;

public class Iamai {
/*	static final String _DBDir="db/";
	static final String _DocStatFilename=_DBDir+"pattern.dat";
	static Pattern pattern;*/
	
	/*public static void main(String args[]) throws IOException, InterruptedException  {
		List<NontitleChecker> nontitleCheckers=new ArrayList<Content.NontitleChecker>();
		nontitleCheckers.add(new Process.NontitleFirstStringChecker());
		nontitleCheckers.add(new Process.NontitleBlockStringChecker());
		
		pattern=new Pattern() ;
		
		List<String> pdfs=Extverifier.verifyFileList();
		for(String pdf:pdfs) {
			Process process=Geskiw.processPDF(pdf);
			Validation validation=new Validation(process.content,_DocStatFilename);
			
			pattern.addDocStat(validation.docStat);
		}
		
		pattern.updateDocStat();
		pattern.savePattern();
		
		System.out.println("Pattern saved.");
	}*/
}
