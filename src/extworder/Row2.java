package extworder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Row2 extends Rectangle {
	CharFont charfont;
	public ArrayList<Char2> chars;
	Block2 block;
	Page2 page;
	private int maxInterval;
	
	public Row2(Char2 leftest, int maxInterval, Page2 page) {
		this.maxInterval=maxInterval;
		charfont=new CharFont(leftest.font.getName(),leftest.height);
		this.page=page;
		
		chars=new ArrayList<Char2>();
		build(leftest);
	}
	
	private void build(Char2 ch) {
		addChar(ch);
		
		List<Char2> rights=ch.getRightConnected(page, maxInterval,0);
		
		for(Char2 ch1:rights)
			if(ch.upper==ch1.upper && ch.lower==ch1.lower) {
				build(ch1);
				break;
			}
	}
	
	public void addChar(Char2 ch) {
		chars.add(ch);
		ch.row=this;
		updateRectangle(ch);
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
