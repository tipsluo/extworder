package extworder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import extworder.Block.BlockFilter;
import extworder.Block.BlockFormat;
import extworder.Block.BodyBlockFilter;
import extworder.Block.SubtitleBlockFilter;
import extworder.Common.StatGroup;

public class ContentTree {
	Content content;
	Node root;
	BodyBlockFilter bodyBlockFilter=new BodyBlockFilter();
	SubtitleBlockFilter subtitleBlockFilter=new SubtitleBlockFilter();
	Value value;
	CheckTextNode checkTextNode;
	CheckLeafNode checkLeafNode;

	public ContentTree(Content content) {
		this.content=content;
		root=new Node(content.titleBlock,null,false);
		value=new Value();
		
		List<Block> blocks=getBodyBlocks(0, (int)(content.pages.size()/2)+1);
		Map<BlockFormat,Integer> bfsg=getSubtitleFormats(blocks).sortByValue();
		//buildTree
		//getTextNodeValue();
	}
	
	private void evaluate(Map<BlockFormat,Integer> bfsg) {
		
	}
	
	private int buildTree(List<Block> blocks, BlockFormat blockformat) {
		Node node=root;
		int noprocess=0;
		
		for(Block block:blocks) {
			if(! block.format.equals(blockformat)) {
				if(block.format.charfont.height < node.block.format.charfont.height) {
					node=node.addChild(block,false);
				} else {
					Node n=node.parent;
					if(n==null)
						noprocess++;
					
					node=n.addChild(block,false);
				}
			} else {
				if(node.textNode) {
					Node n=node.parent;
					if(n==null)
						noprocess++;
					
					node=n.addChild(block,true);
				}else
					node=node.addChild(block,true);
			}
		}
		
		return noprocess;
	}
	
	private ArrayList<Block> getBodyBlocks(int startPage, int endPage) {
		ArrayList<Block> bs=content.filterBlocks(new Block.BigFontBlockFilter(),
													new Block.CenteredBlockFilter());
		bs=content.filterBlocks(new Block.AllFullWidthBlockFilter());
		
		ArrayList<Block> newbs=new ArrayList<Block>();
		for(Block block:bs)
			if(block.page.id<=endPage && block.page.id>=startPage)
				newbs.add(block);
			
		return newbs;
	}
	
	private StatGroup<BlockFormat> getSubtitleFormats(List<Block> blocks) {
		StatGroup<BlockFormat> bfsg=new StatGroup<BlockFormat>();
		
		int i;
		for(i=0;i<blocks.size();i++) {
			Block block=blocks.get(i);
			if(block.format.equals(content.bodyBlockformat) && i>0) {
				BlockFormat bf=blocks.get(i-1).format;
				bfsg.add(bf);
			}
		}
		
		return bfsg;
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
		int noprocess=0;
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
