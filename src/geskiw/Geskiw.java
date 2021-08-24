package geskiw;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import extworder.Block;
import extworder.Common;
import extworder.Content;
import extworder.Extworder;

public class Geskiw {
	Content content;
	String result;
	
	public Geskiw(String fn) {
		clean(fn);
	}

	public static void main(String args[]) throws IOException  {
		//test1("A model for estimating parameters of rotational landslide");
		//test1("Peace-Development and Peace Through");
		//test1("Broader perspective on ecosystem");
		//test1("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		//test1("ILL article-Impact of the KWL reading strategy");
		Geskiw geskiw=new Geskiw("Broader perspective on ecosystem");
		System.out.println(geskiw.result);
		System.out.println("Geskiw Done.");
	}
	
	/*static void test1(String fn) throws IOException {
		Content content = new Content(fn);
		
		System.out.println(String.format("Title:\n%s\nAbstract:\n%s\n----------------------\n",
				content.title(),content.abstractStr));
	}*/
	
	void clean(String fn) {
		try {
			result=rawContent(fn);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String rawContent(String fn) throws IOException {
	    Block abstractBlock,keywordBlock;
	    String abstractStr,keywordStr;
	    
		content = new Content(fn);
		
		String str;
		str=content.title()+"\n\n\n";
		
		abstractStr=getAbstractBlock();
		abstractBlock=content.activeBlock;
		
		keywordStr=getKeywordBlock();
		keywordBlock=content.activeBlock;
	
		if(keywordBlock.priorTo(abstractBlock))
			str+=keywordStr+"\n\n\n";
		
		str+=abstractStr+"\n\n\n";
		
		ignoreCatNSubBlock();

		str+=content.text();
		
		return str;
	}
	
	private String getAbstractBlock() {
		return content.getKeyBlockStr(
				Pattern.compile("^\\s*[Aa][Bb][Ss][Tt][Rr][Aa][Cc][Tt]\\s*[\\s:\n]?"));
	}
	
	private String getKeywordBlock() {
		return content.getKeyBlockStr(
				Pattern.compile("^\\s*[Kk][Ee][Yy][Ww][Oo][Rr][Dd]\\s*[\\s:\n]?"));
	}
	
	private void ignoreCatNSubBlock() {
		content.getKeyBlockStr(
				Pattern.compile("^\\s*Categories\s+and\s+Subject\s+Descriptors\\s*[\\s:\n]?"));
		content.activeBlock.setIgnored(Consts._CatNSubBlock);
	}
}
