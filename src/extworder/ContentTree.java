package extworder;

import java.util.ArrayList;
import java.util.List;

import extworder.Block.BodyBlockFilter;
import extworder.Block.SubtitleBlockFilter;

public class ContentTree {
	Node root;
	BodyBlockFilter bodyBlockFilter=new BodyBlockFilter();
	SubtitleBlockFilter subtitleBlockFilter=new SubtitleBlockFilter();
	Value value;
	CheckTextNode checkTextNode;
	CheckLeafNode checkLeafNode;

	public ContentTree(Content content) {
		root=new Node(content.titleBlock,null,false);
		value=new Value();
		
		buildTree(content);
		getTextNodeValue();
	}
	
	private void buildTree(Content content) {
		Node node=root;
		
		for(Block block: content.getMainBlocks()) {
			if(subtitleBlockFilter.filter(block)) {
				int diff=block.format.compareTo(node.block.format);
				if(diff==0) {
					node=node.parent.addChild(block,node.textNode);
				} else if(diff>0) {
					Node n;
					for(n=node; 
							block.format.compareTo(n.block.format)>=0; 
							n=n.parent) {
						if(n.parent==null)
							break;
					}
					
					if(n.parent==null)
						node=n.addChild(block,false);
					else
						node.parent.addChild(block,false);
				} else {
					node=node.addChild(block,false);
				}
			} else if(bodyBlockFilter.filter(block)) {
				if(node.textNode)
					node=node.parent.addChild(block,true);
				else
					node=node.addChild(block,true);
			}
		}
	}
	
	public float evaluate() {
		return value.textNodeCount * value.textNodeCount / value.leafNodeCount;
	}
	
	private void getTextNodeValue() {
		checkTextNode=new CheckTextNode();
		checkLeafNode=new CheckLeafNode();
		
		travel(root,checkTextNode,checkLeafNode);
		
		value.textNodeCount=checkTextNode.value;
		value.leafNodeCount=checkLeafNode.value;
	}
	
	private void travel(Node node, Checker ...checkers) {
		for(Checker checker:checkers)
			if(checker.check(node))
				checker.process(node);
		
		for(Node d: node.children)
			travel(d,checkers);
	}
	
	static class Value {
		int textNodeCount=0;
		int leafNodeCount=0;
	}
	
	class CheckTextNode extends Checker {
		@Override
		boolean check(Node node) {
			return node.textNode;
		}
	}
	
	class CheckLeafNode extends Checker {
		boolean check(Node node) {
			return node.children.size()==0;
		}
	}
	
	abstract class Checker {
		int value;
		abstract boolean check(Node node);
		void process(Node node) {
			value++;
		};
	}

	class Node {
		boolean textNode;
		Block block;
		Node parent;
		List<Node> children;
		int level;
		
		public Node(Block block,Node parent, boolean textNode) {
			children=new ArrayList<Node>();
			this.block=block;
			this.parent=parent;
			if(parent==null)
				this.level=0;
			else
				this.level=parent.level+1;
			this.textNode=textNode;
		}
		
		public Node addChild(Block block, boolean textNode) {
			return new Node(block,this,textNode);
		}
		
		public Node addChild(Block block) {
			Node node;
			if(bodyBlockFilter.filter(block))
				node=new Node(block,this,true);
			else
				node=new Node(block,this,false);
			this.children.add(node);
			return node;
		}
	}
}
