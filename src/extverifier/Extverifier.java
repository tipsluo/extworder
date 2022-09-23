package extverifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import extworder.Extworder;

import geskiw.Process;

public class Extverifier {
/*	static final String _VeiryfDataDir="data_verify/";
	static final String _ContentNameSubfix="_content.txt";
	
	public Extverifier() throws IOException, InterruptedException {
		
	}
	
	public static void main(String args[]) throws IOException, InterruptedException  {
		List<NontitleChecker> nontitleCheckers=new ArrayList<Content.NontitleChecker>();
		nontitleCheckers.add(new Process.NontitleFirstStringChecker());
		nontitleCheckers.add(new Process.NontitleBlockStringChecker());
		
		List<String> pdfs=verifyFileList();
		Extworder.main_test_pdfs(pdfs,nontitleCheckers);
		
		for(String pdf:pdfs) {
			System.out.printf("Comparing %s ...", pdf);
			if(!Verifier.diff(pdf))
				System.out.print("Different\n");
			else System.out.print("Same\n");
		}
	}
	
	public static List<String> verifyFileList() throws IOException {
		Path path=Paths.get(_VeiryfDataDir);
		if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path must be a directory!");
        }
		
		List<String> filepaths;
        try (Stream<Path> walk = Files.walk(path)) {
        	filepaths = walk
                    .filter(p -> !Files.isDirectory(p))
                    .map(p -> p.toString().toLowerCase())
                    .filter(f -> f.endsWith(_ContentNameSubfix))
                    .collect(Collectors.toList());
        }

		List<String> pdfNames=new ArrayList<String>();
		for(String filepath:filepaths) {
			String filename;
		    int pos = filepath.lastIndexOf(File.separator);
		    if(pos > -1)
		        filename=filepath.substring(pos + 1);
		    else
		        filename=filepath;
			   
			pdfNames.add(filename.substring(0, filename.lastIndexOf(_ContentNameSubfix)));
		}
		
		return pdfNames;
	}
	
	protected String testInputFilename(String pdfName) {
		return Extworder._TestDataDir+pdfName+".pdf";
	}
	
	protected static String testContentFilename(String pdfName) {
		return Extworder._TestDataDir+pdfName+_ContentNameSubfix;
	}
	
	protected static String verifyContentFilename(String pdfName) {
		return _VeiryfDataDir+pdfName+_ContentNameSubfix;
	}*/
}
