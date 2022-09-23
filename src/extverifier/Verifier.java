package extverifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Verifier {
	/*static MessageDigest _MD5Digest;
	
	static {
		try {
			_MD5Digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Verifier()  throws IOException, NoSuchAlgorithmException {
		
	}
	
	public static boolean diff(String pdfName) throws IOException {
		return diff(Extverifier.testContentFilename(pdfName),Extverifier.verifyContentFilename(pdfName));
	}
	
	private static boolean diff(String fp1, String fp2) throws IOException {
		String checksum1 = checksum(new File(fp1));
		String checksum2 = checksum(new File(fp2));

		return checksum1.equals(checksum2);
	}
	
	private static String checksum(File file) throws IOException {
	  FileInputStream fis = new FileInputStream(file);
	   
	  byte[] byteArray = new byte[1024];
	  int bytesCount = 0; 
	    
	  while ((bytesCount = fis.read(byteArray)) != -1) {
		  _MD5Digest.update(byteArray, 0, bytesCount);
	  };
	   
	  fis.close();
	   
	  byte[] bytes = _MD5Digest.digest();
	   
	  StringBuilder sb = new StringBuilder();
	  for(int i=0; i< bytes.length ;i++) {
	    sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
	  }
	   
	   return sb.toString();
	}*/
}
