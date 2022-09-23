package geskiw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.font.PDFont;


import extworder.Common;


public class CharString {
	/*ArrayList<Char> chars;
	private String string;
	HashMap<Integer,Char> charmap;
	float height;
	
	public CharString() {
		chars=new ArrayList<Char>();
		string="";
		height=0;
	}
	
	public CharString(ArrayList<Block> blocks,ArrayList<Pattern> abbrPatterns) {
		chars=new ArrayList<Char>();
		string="";
		build(blocks);
		markAbbreviation(abbrPatterns);
	}
	
	public CharString(Block block,ArrayList<Pattern> abbrPatterns) {
		chars=new ArrayList<Char>();
		string="";
		
		ArrayList<Block> blocks=new ArrayList<Block>();
		blocks.add(block);
		build(blocks);
		markAbbreviation(abbrPatterns);
	}
	
	public void addAllChars(CharString cs) {
		chars.addAll(cs.chars);
		
		if(height<cs.height)
			height=cs.height;
	}
	
	public void build(ArrayList<Block> blocks) {
		Char c=null;
		Char c0=null;
		boolean lastDash=false;
		height=0;
		
		for(Block block:blocks) {
			for(Row row:block.rows) {
				if(c!=null && !lastDash)
					addChar(c.spaceChar(c.width));	// not the first row
				
				c0=row.chars.get(0);
				for(int i=0;i<row.chars.size();i++) {
					c=row.chars.get(i);
					lastDash=false;
					
					if(i==row.chars.size()-1)
						if(c.str.charAt(c.str.length()-1)=='-') {
							lastDash=true;
							if(c.str.length()>1)
								c=new Char(c,c.str.substring(0,c.str.length()-2),c.width,c.height);
							else continue;
						}
					
					if(! c.str.equals(" ") && ! c0.str.equals(" ") &&
							c0!=c && ! Common.connectedChars(c0,c))
						addChar(c0.spaceChar(c.left-c0.right-1));
					addChar(c);
					c0=c;
				}
			}
		}
	}
	
	public void addChar(Char c) {
		chars.add(c);

		if(height<c.height)
			height=c.height;
	}

	
	public String string() {
		if(string!="")
			return string;

		charmap=new HashMap<Integer,Char>();
		int i=0;

		for(Char c:chars) {
			string+=c.str;
			for(int j=0; j<c.str.length();j++) {
				charmap.put(i+j,c);
			}
			i+=c.str.length();
		}
		return string;
	}
	
	public String reString() {
		string="";
		return string();
	}
	
	public void markAbbreviation(ArrayList<Pattern> abbrPatterns) {
		for(int i=0;i<abbrPatterns.size();i++) {
			Matcher m=abbrPatterns.get(i).matcher(string());
			while(m.find()) {
				for(int j=m.start();j<m.end();j++) {
					Char c=charmap.get(j);

					if(c.str.contains(".")) {
						c.str=c.str.replace(".",Consts._AbbrDot);
					}
				}
			}
		}
	}
	
	public CharString unmarkAbbreviation() {
		for(Char c:chars)
			if(c.str.contains(Consts._AbbrDot))
				c.str=c.str.replace(Consts._AbbrDot,".");
		return this;
	}
	
	public ArrayList<CharString> splitSentences() {
		ArrayList<CharString> ret=new ArrayList<CharString>();
		
		CharString cs=new CharString();
		boolean delimiter=false;
		boolean start=true;
		
		for(Char ch:chars) {
			if(start && 
					(ch.str.equals(" ") || 
							ch.str.equals("\n") ||
							ch.str.equals("\r"))) {
				continue;
			}
			
			start=false;
			
			if(ch.str.equals(".") || ch.str.equals("!") || ch.str.equals("?")) {
				cs.addChar(ch);
				delimiter=true;
				continue;
			}
			
			if(delimiter) {
				if(ch.str.equals("\"") || ch.str.equals(")") || ch.smallChar()) {
					cs.chars.add(ch);
					continue;
				}
				
				if(ch.str.equals(" ") || 
						ch.str.equals("\n") || 
						ch.str.equals("\r")) {
					ret.add(cs);
					cs=new CharString();
					start=true;
				} else {
					ret.add(cs);
					cs=new CharString();
					cs.addChar(ch);
					start=true;
				}
				
				delimiter=false;
				continue;
			}
			
			cs.addChar(ch);
		}
		
		ret.add(cs);
		return ret;
	}
	
	public static class VirtualCharString extends CharString {
		public VirtualCharString(String s, PDFont font, float height) {
			super();
			
			for(int i=0;i<s.length();i++) {
				VirtualChar ch=new VirtualChar(s.substring(i,i+1),height,font);
				addChar(ch);
			}
			
			VirtualRow row=new VirtualRow(chars);
			
			this.height=height;
		}
	}*/
}
