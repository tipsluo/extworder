package geskiw;

import java.io.IOException;
import java.util.regex.Pattern;

import extworder.Block;
import extworder.Common;
import extworder.Content;
import extworder.Page;

public class Process {

	static Content content;
	String result;
	
	public Process(String fn) throws IOException {
		content = new Content(fn, new IgnorePage(), false, true, true);
		
		clean();
	}

	void clean() {
		try {
			result=rawContent();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String rawContent() throws IOException {
	    Block abstractBlock,keywordBlock;
	    String abstractStr,keywordStr;
		
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
	
	class IgnorePage extends Common.IgnorePage {
		final String[] pstr=new String[]{
			"LENDER",
			"BORROWER",
			"SAGE Businesscases"
		};
		
		public boolean isIgnored(Page page) {
			String str=page.string();
			for(int i=0; i<pstr.length; i++)
				if (str.contains(pstr[i]))
					return true;

			return false;
		}
	}
}
