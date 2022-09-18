package extworder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import extworder.Common.StatGroup;
import extworder.Row.CharFont;

public class Row2 extends Rectangle {
	CharFont charfont;
	ArrayList<Char2> chars;
	Block2 block;
	ArrayList<Block2> blocks;
	Page2 page;
	int interval;
	private String str="";
	
	static Comparator<Row2> compareRows = (Row2 r1, Row2 r2) ->
		r1.upper != r2.upper ? Common.compareValue(r1.upper,r2.upper) : Common.compareValue(r1.left,r2.left);
	static Comparator<Row2> compareRowHeights = (Row2 r1, Row2 r2) ->
		(int)(r1.height-r2.height) ;
	static Comparator<Row2> compareRowLefts = (Row2 r1, Row2 r2) ->
		r1.left != r2.left ? Common.compareValue(r1.left,r2.left) : Common.compareValue(r1.upper,r2.upper);
	
	public Row2() {
		super();

		chars=new ArrayList<Char2>();
	}
	
	public Row2(Char2 ch, Page2 page, int interval) {
		chars=new ArrayList<Char2>();
		blocks=new ArrayList<Block2>();
		this.page=page;
		this.interval=interval;
		
		registerChar(ch);

		searchLeftestChar(ch,interval);
		searchRightestChar(ch,interval);
		
		page.rows.add(this);
	}
	
	void registerChar(Char2 ch) {
		chars.add(ch);
		ch.rows.add(this);
		updateRectangle(ch);
		charfont=getCharFont();
	}
	
	public String render() {
		Collections.sort(chars,Char2.compareChars);
		str="";
		string();
		charfont=getCharFont();
		return str;
	}
	
	void setActive() {
		for(Char2 ch:chars) {
			ch.row=this;
		}
	}
	
	public String string() {
		if(str!=null && str!="")
			return str;
		
		Char2 ch0=chars.get(0);
	
		for(Char2 ch:chars) {
			if(interval < ch.left-ch0.right) {
				str+=" ";
			}
			str+=ch.str;
			ch0=ch;
		}
		
		return str;
	}
	
	private void searchLeftestChar(Char2 ch, int interval) {
		List<Char2> ls=ch.getLeftConnected(page, interval, 0);
		
		Char2 ch1;
		if(ls.size()==1) {
			ch1=ls.get(0);
			if(ch1.upper==ch.upper && ch1.lower==ch.lower) {
				chars.add(0,ch1);
				ch1.rows.add(this);
				searchLeftestChar(ch1,interval);
			}
		}
	}
	
	private void searchRightestChar(Char2 ch, int interval) {
		List<Char2> ls=ch.getLeftConnected(page, interval, 0);
		
		Char2 ch1;
		if(ls.size()==1) {
			ch1=ls.get(0);
			if(ch1.upper==ch.upper && ch1.lower==ch.lower) {
				chars.add(ch1);
				ch1.rows.add(this);
				searchRightestChar(ch1,interval);
			}
		}
	}
	
	private CharFont getCharFont() {
		if(chars.size()==0)
			return null;
		
		StatGroup<CharFont> charfonts=new StatGroup<CharFont>();
		
		for (Char2 ch: chars) {
			CharFont cf=new CharFont(ch.font.getName(),ch.height);
			
			charfonts.add(cf);
		}
			
		return charfonts.maxByValue();
	}
	
	static public class CharFont implements Comparable<CharFont>{
		String name;
		public float height;
		int bold;
		
		public CharFont(String name,float height) {
			this.name=name;
			this.height=height;
					
			bold=CheckBold.check(name);
		}
		
		public CharFont(CharFont cf) {
			this.name=cf.name;
			this.height=cf.height;
			bold=cf.bold;
		}
		
		public boolean similar(CharFont cf) {
			return Math.abs(value()-cf.value())<=Common._MaxSameCharFontHeightDiff;
		}
		
		public float value() {
			float f=height;
			
			if(bold>0)
				f=f + bold * Common._BoldCharFontValue;
					
	        return f;
		}
		
	    @Override
	    public int hashCode() {
	    	return (int)(value()*4096) + (name.hashCode()>>20);
	    }
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
	            return true;
	        if (obj == null)
	            return false;
	        if (getClass() != obj.getClass())
	            return false;
	        
	        CharFont other = (CharFont) obj;
	        
	        return value()==other.value();
		}
		
		public boolean allEquals(CharFont charfont) {
			return name==charfont.name && height==charfont.height;
		}
		
		@Override
	    public int compareTo(CharFont charfont) {
	        return (int)(value()-charfont.value());
	    }

		final static class CheckBold {
			final static int _BOLD=2;
			final static int _SEMIBOLD=1;
			final static int _NOBOLD=0;
			
			static final Pattern LastPart;
			static final Pattern Bold;
			static final Pattern Semibold;
			
			static {
				LastPart=Pattern.compile("[\\.-](.*)$");
				Bold=Pattern.compile("Bold");
				Semibold=Pattern.compile("Semibold");
			}
			
			public static int check(String s) {
				Matcher m=LastPart.matcher(s);
				
				if(m.find()) {
					String lastPart=m.group(1);
					
					if(lastPart.equals("B"))
						return _BOLD;
					else {
						m=Bold.matcher(lastPart);
						if(m.find())
							return _BOLD;
						else {
							m=Semibold.matcher(lastPart);
							if(m.find())
								return _SEMIBOLD;
							else
								return _NOBOLD;
						}
					}
				}
				
				return _NOBOLD;
			}
		}
	}
	
	static public class VirtualRow extends Row2 {
		public VirtualRow(ArrayList<Char2> vcs) {
			for(Char2 vc:vcs) {
				vc.row=this;
				vc.updateRectangle(vc);
			}
		}
	}
}
