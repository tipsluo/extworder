package extworder;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import extworder.Common.Stretch;
import extworder.Page.Border;

public class PageLayout {
	private Page page;

	public PageLayout(Page page) {
		this.page=page;
		
		getAllCharVBorders();
	}
	
	private List<Border2> getAllCharVBorders() {
		List<Border2> charVBorders;
		
		charVBorders=new ArrayList<Border2>();
		
		for(Char c:page.chars) {
			charVBorders.add(new Border2(c.left, new Stretch(c.upper,c.lower), Common._LEFTORIENTED));
			charVBorders.add(new Border2(c.left, new Stretch(c.upper,c.lower), Common._RIGHTORIENTED));
		}
		
		Collections.sort(charVBorders);
		
		return charVBorders;
	}
	
	private List<List<Border2>> getAllVBorderGroups(List<Border2> borders) {
		List<List<Border2>> borderGroups=new ArrayList<List<Border2>>();
		
		List<Border2> borderGroup=new ArrayList<Border2>();
		Border2 b0=borders.get(0);
		borderGroup.add(b0);
		
		for(int i=1;i<borders.size();i++) {
			Border2 b=borders.get(i);
			if(b.x==b0.x && b.orient==b0.orient) {
				borderGroup.add(b);
			} else {
				borderGroups.add(borderGroup);
				borderGroup=new ArrayList<Border2>();
				borderGroup.add(b);
			}
			b0=b;
		}
		
		if(borderGroup.size()!=0)
			borderGroups.add(borderGroup);
		
		return borderGroups;
	}
	
	/*private List<Connection> buildBorderGroup(List<Border2> borders) {
		List<Connection> connects=new ArrayList<Connection>();
		
		for(int i=0; i<borders.size(); i++) {
			Border2 b1=borders.get(i);
		}
	}*/
	
	private List<Connection<Border2,Border2>> buildTargetBorders(List<Border2> allSourceBorders, List<Connection<Border2,Border2>> upStreamConnections, int index) {
		List<Connection<Border2,Border2>> connections=new ArrayList<Connection<Border2,Border2>>();
		
		Border2 current=allSourceBorders.get(index);
		
		for(Connection<Border2,Border2> upStreamConnection:upStreamConnections) {
			Border2 b=new Border2(upStreamConnection.target);
			b.extend(current);
			Connection<Border2,Border2> c=new Connection<Border2,Border2>(current,b,1);
			connections.add(c);
			
			Connection.copyConnectionSources(upStreamConnections,b);
		}
		
		borders.add(current);
		buildTargetBorders(allSourceBorders,borders,index+1);
		
	}
	
	private class Border2 extends Cell implements Comparable<Border2> {
		final int x;
		final Stretch stretch;
		final int orient;
		
		public Border2(Border2 border) {
			x=border.x;
			stretch=new Stretch(border.stretch);
			orient=border.orient;
		}
		
		public Border2(int x, Stretch stretch, int orient) {
			this.x=x;
			this.stretch=stretch;
			this.orient=orient;
		}
		
		public void extend(Border2 border) {
			stretch.extend(border.stretch);
		}

		@Override
		public int compareTo(Border2 b1) {
			if(x!=b1.x)
				return x-b1.x;
			
			if(orient!=b1.orient)
				return orient-b1.orient;
			
			return stretch.compareTo(b1.stretch);
		}

		@Override
		public float value() {
			return stretch.length();
		}
	}
	
	abstract class Cell {
		abstract float value();
	}
	
	static private class Connection<S extends Cell, T extends Cell> {
		S source;
		T target;
		float weight;
		
		public Connection(S source, T target) {
			this.source=source;
			this.target=target;
		}
		
		public Connection(S source, T target,float weight) {
			this.source=source;
			this.target=target;
			this.weight=weight;
		}
		
		public void setSource(S source) {
			this.source=source;
		}
		
		public void setTarget(T target) {
			this.target=target;
		}
		
		public void setWeight(float weight) {
			this.weight=weight;
		}
		
		public static List<Connection<S, T>> copyConnectionSources(List<Connection<S,T>> connections, T t) {
			List<Connection<S, T>> newConnections=new ArrayList<Connection<S, T>>();
			
			for(Connection<S,T> connection: connections) {
				Connection<S,T> newConnection=new Connection(connection.source,t,connection.weight);
				newConnections.add(newConnection);
			}
			return newConnections;
		}
	}
}
