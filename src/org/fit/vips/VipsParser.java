/*
 * Tomas Popela, xpopel11, 2012
 * VIPS - Visual Internet Page Segmentation
 * Module - VipsParser.java
 */

package org.fit.vips;

import java.util.ArrayList;
import java.util.List;

import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.TextBox;
import org.fit.cssbox.layout.Viewport;
import org.w3c.dom.Node;

public class VipsParser {

	private VipsBlock _vipsBlocks = null;
	private VipsBlock _currentVipsBlock = null;
	private VipsBlock _tempVipsBlock = null;

	private int _sizeTresholdWidth = 0;
	private int _sizeTresholdHeight = 0;
	private Viewport _viewport = null;
	private int _visualBlocksCount = 0;

	/**
	 * Default constructor
	 * 
	 * @param viewport Rendered's page viewport
	 */
	public VipsParser(Viewport viewport) {
		this._viewport = viewport;
		this._vipsBlocks = new VipsBlock();
		this._sizeTresholdHeight = 80;
		this._sizeTresholdWidth = 80;
	}

	/**
	 * Constructor, where we can define element's size treshold
	 * @param viewport	Rendered's page viewport
	 * @param sizeTresholdWidth Element's width treshold
	 * @param sizeTresholdHeight Element's height treshold
	 */
	public VipsParser(Viewport viewport, int sizeTresholdWidth, int sizeTresholdHeight) {
		this._viewport = viewport;
		this._vipsBlocks = new VipsBlock();
		this._sizeTresholdHeight = sizeTresholdHeight;
		this._sizeTresholdWidth = sizeTresholdWidth;
	}

	/**
	 * Starts visual page segmentation on given page
	 */
	public void parse()
	{
		if (_viewport != null)
		{
			this._vipsBlocks = new VipsBlock();
			_visualBlocksCount = 0;

			constructVipsBlockTree(_viewport.getElementBoxByName("body", false), _vipsBlocks);
			divideVipsBlockTree(_vipsBlocks);

			getVisualBlocksCount(_vipsBlocks);
			System.err.println(String.valueOf(_visualBlocksCount));
		}
		else
			System.err.print("Page's ViewPort is not defined");
	}

	/**
	 * Counts number of visual blocks in visual structure
	 * @param vipsBlock Visual structure
	 */
	private void getVisualBlocksCount(VipsBlock vipsBlock)
	{
		if (vipsBlock.isVisualBlock())
			_visualBlocksCount++;

		for (VipsBlock vipsBlockChild : vipsBlock.getChildren())
		{
			if (!(vipsBlockChild.getBox() instanceof TextBox))
				getVisualBlocksCount(vipsBlockChild);
		}
	}

	private void findVisualBlocks(VipsBlock vipsBlock, List<VipsBlock> list)
	{
		if (vipsBlock.isVisualBlock())
			list.add(vipsBlock);

		for (VipsBlock vipsStructureChild : vipsBlock.getChildren())
			findVisualBlocks(vipsStructureChild, list);
	}

	public List<VipsBlock> getVisualBlocks()
	{
		List<VipsBlock> list = new ArrayList<VipsBlock>();
		findVisualBlocks(_vipsBlocks, list);

		return list;
	}

	/**
	 * Construct VIPS block tree from viewport.
	 * <p>
	 * Starts from &lt;body&gt; element.
	 * @param element Box that represents element
	 * @param node Visual structure tree node
	 */
	private void constructVipsBlockTree(Box element, VipsBlock node)
	{
		node.setBox(element);

		if (! (element instanceof TextBox))
		{
			for (Box box: ((ElementBox) element).getSubBoxList())
			{
				node.addChild(new VipsBlock());
				constructVipsBlockTree(box, node.getChildren().get(node.getChildren().size()-1));
			}
		}
	}

	/**
	 * Tries to divide DOM elements and finds visual blocks.
	 * @param vipsBlock Visual structure
	 */
	private void divideVipsBlockTree(VipsBlock vipsBlock)
	{
		_currentVipsBlock = vipsBlock;
		ElementBox elementBox = (ElementBox) vipsBlock.getBox();
		System.err.println(elementBox.getNode().getNodeName());

		// With VIPS rules it tries to determine if element is dividable
		if (applyVipsRules(elementBox) && vipsBlock.isDividable() && !vipsBlock.isVisualBlock())
		{
			// if element is dividable, let's divide it
			_currentVipsBlock.setAlreadyDivided(true);
			for (VipsBlock vipsBlockChild : vipsBlock.getChildren())
			{
				if (!(vipsBlockChild.getBox() instanceof TextBox))
					divideVipsBlockTree(vipsBlockChild);
			}
		}
		else
		{
			// if element is not dividable
			if (vipsBlock.isDividable())
			{
				System.err.println("Element " + elementBox.getNode().getNodeName() + " is visual block");
				vipsBlock.setIsVisualBlock(true);
			}
			else
			{
				if (ruleImgLink(elementBox))
					vipsBlock.setIsVisualBlock(true);

				if (vipsBlock.isVisualBlock())
					System.err.println("Element " + elementBox.getNode().getNodeName() + " is visual block");
				else
					System.err.println("Element " + elementBox.getNode().getNodeName() + " is not dividable");
			}
		}
	}

	/**
	 * If there is image as link mark it as visual block.
	 * @param elementBox Node
	 * @return Returns true if rule was applied otherwise false.
	 */
	private boolean ruleImgLink(ElementBox elementBox)
	{
		//TODO extra rule
		Node node = elementBox.getNode();

		if (!node.getNodeName().equals("img"))
			return false;

		if (!node.getParentNode().getNodeName().equals("a"))
			return false;

		if (!node.getParentNode().getFirstChild().getNodeName().equals("img"))
			return false;

		return true;
	}

	/**
	 * Checks, if node is a valid node.
	 * <p>
	 * Node is valid, if it's visible in browser. This means, that the node's
	 * width and height are not zero.
	 * 
	 * @param node
	 *            Input node
	 * 
	 * @return True, if node is valid, otherwise false.
	 */
	private boolean isValidNode(ElementBox node)
	{
		if (node.getHeight() > 0 && node.getWidth() > 0)
			return true;

		return false;
	}

	/**
	 * Checks, if node is a text node.
	 * 
	 * @param node
	 *            Input node
	 * 
	 * @return True, if node is a text node, otherwise false.
	 */
	private boolean isTextNode(ElementBox box)
	{
		return (box.getNode().getNodeName().equals("text")) ? true : false;
	}

	/**
	 * Checks, if node is a virtual text node.
	 * <p>
	 * Inline node with only text node children is a virtual text node.
	 * 
	 * @param node
	 *            Input node
	 * 
	 * @return True, if node is virtual text node, otherwise false.
	 */
	private boolean isVirtualTextNode1(ElementBox node)
	{
		if (node.isBlock())
			return false;

		for (Box childNode : node.getSubBoxList())
		{
			if (!(childNode instanceof TextBox))
				if (!isTextNode((ElementBox) childNode))
					return false;
		}

		return true;
	}

	/**
	 * Checks, if node is virtual text node.
	 * <p>
	 * Inline node with only text node and virtual text node children is a
	 * virtual text node.
	 *
	 * @param node
	 *            Input node
	 * 
	 * @return True, if node is virtual text node, otherwise false.
	 */
	private boolean isVirtualTextNode2(ElementBox node)
	{
		if (node.isBlock())
			return false;

		for (Box childNode : node.getSubBoxList())
			if (!isTextNode((ElementBox) childNode) ||
					!isVirtualTextNode1((ElementBox) childNode))
				return false;

		return true;
	}

	/**
	 * Checks, if node is virtual text node.
	 * 
	 * @param node
	 *            Input node
	 * 
	 * @return True, if node is virtual text node, otherwise false.
	 */
	private boolean isVirtualTextNode(ElementBox node)
	{
		if (isVirtualTextNode1(node))
			return true;
		if (isVirtualTextNode2(node))
			return true;

		return false;
	}

	/*
	 * Checks if node has valid children nodes
	 */
	private boolean hasValidChildNodes(ElementBox node)
	{
		if (node.getSubBoxList().isEmpty())
			return false;

		int cnt = 0;

		for (Box childNode : node.getSubBoxList())
		{
			if (childNode instanceof TextBox)
			{
				cnt++;
				continue;
			}
			if (isValidNode((ElementBox) childNode))
				cnt++;
		}

		return (cnt > 0) ? true : false;
	}

	/*
	 * Returns the number of node's valid children
	 */
	private int numberOfValidChildNodes(ElementBox node)
	{
		int cnt = 0;

		if (node.getSubBoxList().isEmpty())
			return cnt;

		for (Box childNode : node.getSubBoxList())
		{
			if (childNode instanceof TextBox)
			{
				cnt++;
				continue;
			}
			if (isValidNode((ElementBox) childNode))
				cnt++;
		}

		return cnt;
	}

	/**
	 * On different DOM nodes it applies different sets of VIPS rules.
	 * @param node DOM node
	 * @return Returns true if element is dividable, otherwise false.
	 */
	private boolean applyVipsRules(ElementBox node)
	{
		boolean retVal = false;

		System.err.println("Applying VIPS rules on " + node.getNode().getNodeName() + " node");

		if (!node.isBlock())
		{
			retVal = applyInlineTextNodeVipsRules(node);
		}
		else if (node.getNode().getNodeName().equals("table"))
		{
			retVal = applyTableNodeVipsRules(node);
		}
		else if (node.getNode().getNodeName().equals("tr"))
		{
			retVal = applyTrNodeVipsRules(node);
		}
		else if (node.getNode().getNodeName().equals("td"))
		{
			retVal = applyTdNodeVipsRules(node);
		}
		else if (node.getNode().getNodeName().equals("p"))
		{
			retVal = applyPNodeVipsRules(node);
		}
		else
		{
			retVal = applyOtherNodeVipsRules(node);
		}

		return retVal;
	}

	/**
	 * Applies VIPS rules on block nodes other than &lt;P&gt; &lt;TD&gt;
	 * &lt;TR&gt; &lt;TABLE&gt;.
	 * @param node Node
	 * @return Returns true if one of rules success and node is dividable.
	 */
	private boolean applyOtherNodeVipsRules(ElementBox node)
	{
		// 1 2 3 4 6 8 9 11

		if (ruleOne(node))
			return true;

		if (ruleTwo(node))
			return true;

		if (ruleThree(node))
			return true;

		if (ruleFour(node))
			return true;

		if (ruleSix(node))
			return true;

		if (ruleEight(node))
			return true;

		if (ruleNine(node))
			return true;

		if (ruleEleven(node))
			return true;

		return false;
	}

	/**
	 * Applies VIPS rules on &lt;P&gt; node.
	 * @param node Node
	 * @return Returns true if one of rules success and node is dividable.
	 */
	private boolean applyPNodeVipsRules(ElementBox node)
	{
		// 1 2 3 4 5 6 8 9 11

		if (ruleOne(node))
			return true;

		if (ruleTwo(node))
			return true;

		if (ruleThree(node))
			return true;

		if (ruleFour(node))
			return true;

		if (ruleFive(node))
			return true;

		if (ruleSix(node))
			return true;

		if (ruleSeven(node))
			return true;

		if (ruleEight(node))
			return true;

		if (ruleNine(node))
			return true;

		if (ruleTen(node))
			return true;

		if (ruleEleven(node))
			return true;

		if (ruleTwelve(node))
			return true;

		return false;
	}

	/**
	 * Applies VIPS rules on &lt;TD&gt; node.
	 * @param node Node
	 * @return Returns true if one of rules success and node is dividable.
	 */
	private boolean applyTdNodeVipsRules(ElementBox node)
	{
		// 1 2 3 4 8 9 10 12

		if (ruleOne(node))
			return true;

		if (ruleTwo(node))
			return true;

		if (ruleThree(node))
			return true;

		if (ruleFour(node))
			return true;

		if (ruleEight(node))
			return true;

		if (ruleNine(node))
			return true;

		if (ruleTen(node))
			return true;

		if (ruleTwelve(node))
			return true;

		return false;
	}

	/**
	 * Applies VIPS rules on &TR;&gt; node.
	 * @param node Node
	 * @return Returns true if one of rules success and node is dividable.
	 */
	private boolean applyTrNodeVipsRules(ElementBox node)
	{
		// 1 2 3 7 9 12

		if (ruleOne(node))
			return true;

		if (ruleTwo(node))
			return true;

		if (ruleThree(node))
			return true;

		if (ruleSeven(node))
			return true;

		if (ruleNine(node))
			return true;

		if (ruleTwelve(node))
			return true;

		return false;
	}

	/**
	 * Applies VIPS rules on &lt;TABLE&gt; node.
	 * @param node Node
	 * @return Returns true if one of rules success and node is dividable.
	 */
	private boolean applyTableNodeVipsRules(ElementBox node)
	{
		// 1 2 3 7 9 12

		if (ruleOne(node))
			return true;

		if (ruleTwo(node))
			return true;

		if (ruleThree(node))
			return true;

		if (ruleSeven(node))
			return true;

		if (ruleNine(node))
			return true;

		if (ruleTwelve(node))
			return true;

		return false;
	}

	/**
	 * Applies VIPS rules on inline nodes.
	 * @param node Node
	 * @return Returns true if one of rules success and node is dividable.
	 */
	private boolean applyInlineTextNodeVipsRules(ElementBox node)
	{
		// 1 2 3 4 5 6 8 9 11

		if (ruleOne(node))
			return true;

		if (ruleTwo(node))
			return true;

		if (ruleThree(node))
			return true;

		if (ruleFour(node))
			return true;

		if (ruleFive(node))
			return true;

		if (ruleSix(node))
			return true;

		if (ruleEight(node))
			return true;

		if (ruleNine(node))
			return true;

		if (ruleTwelve(node))
			return true;

		return false;
	}

	/**
	 * VIPS Rule One
	 * <p>
	 * If the DOM node is not a text node and it has no valid children, then
	 * this node cannot be divided and will be cut.
	 * 
	 * @param node
	 *            Input node
	 * 
	 * @return True, if rule is applied, otherwise false.
	 */
	private boolean ruleOne(ElementBox node)
	{
		System.err.println("Applying rule One on " + node.getNode().getNodeName() + " node");

		if (!isTextNode(node))
		{
			if (!hasValidChildNodes(node))
			{
				_currentVipsBlock.setIsDividable(false);
				return true;
			}
		}

		return false;
	}

	/**
	 * VIPS Rule Two
	 * <p>
	 * If the DOM node has only one valid child and the child is not a text
	 * node, then divide this node
	 * 
	 * @param node
	 *            Input node
	 * 
	 * @return True, if rule is applied, otherwise false.
	 */
	private boolean ruleTwo(ElementBox node)
	{
		System.err.println("Applying rule Two on " + node.getNode().getNodeName() + " node");

		if (numberOfValidChildNodes(node) == 1)
		{
			if (node.getSubBox(0) instanceof TextBox)
				return false;
			if (!isTextNode((ElementBox) node.getSubBox(0)))
				return true;
		}

		return false;
	}

	/**
	 * VIPS Rule Three
	 * <p>
	 * If the DOM node is the root node of the sub-DOM tree (corresponding to
	 * the block), and there is only one sub DOM tree corresponding to this
	 * block, divide this node.
	 * 
	 * @param node
	 *            Input node
	 * 
	 * @return True, if rule is applied, otherwise false.
	 */
	private boolean ruleThree(ElementBox node)
	{
		System.err.println("Applying rule Three on " + node.getNode().getNodeName() + " node");

		// TODO check if node is root element of the block
		if (!node.isRootElement())
			return false;

		boolean result = true;
		int cnt = 0;

		for (VipsBlock vipsBlock : _vipsBlocks.getChildren())
		{
			if (vipsBlock.getBox().getNode().getNodeName().equals(node.getNode().getNodeName()))
			{
				result = true;
				isOnlyOneDomSubTree(node.getNode(), vipsBlock.getBox().getNode(), result);

				if (result)
					cnt++;
			}
		}

		return (cnt == 1) ? true : false;
	}

	/**
	 * Checks if node's subtree is unique in DOM tree.
	 * @param pattern Node for comparing
	 * @param node Node from DOM tree
	 * @param result True if element is unique otherwise false
	 */
	private void isOnlyOneDomSubTree(Node pattern, Node node, boolean result)
	{
		if (!pattern.getNodeName().equals(node.getNodeName()))
			result = false;

		if (pattern.getChildNodes().getLength() != node.getChildNodes().getLength())
			result = false;

		if (!result)
			return;

		for (int i = 0; i < pattern.getChildNodes().getLength(); i++)
		{
			isOnlyOneDomSubTree(pattern.getChildNodes().item(i), node.getChildNodes().item(i), result);
		}
	}

	/**
	 * VIPS Rule Four
	 * <p>
	 * If all of the child nodes of the DOM node are text nodes or virtual text
	 * nodes, do not divide the node. <br>
	 * If the font size and font weight of all these child nodes are same, set
	 * the DoC of the extracted block to 10.
	 * Otherwise, set the DoC of this extracted block to 9.
	 * 
	 * @param node
	 *            Input node
	 * 
	 * @return True, if rule is applied, otherwise false.
	 */
	private boolean ruleFour(ElementBox node)
	{
		System.err.println("Applying rule Four on " + node.getNode().getNodeName() + " node");

		if (node.getSubBoxList().isEmpty())
			return false;

		for (Box box : node.getSubBoxList())
		{
			if (box instanceof TextBox)
				continue;
			if (!isTextNode((ElementBox) box) ||
					!isVirtualTextNode((ElementBox) box))
				return false;
		}

		// if there is only one child and it has empty text inside it
		if (node.getSubBoxList().size() == 1 && node.getSubBox(0).getText().equals(" "))
		{
			// it's not visual block
			_currentVipsBlock.setIsVisualBlock(false);
			_currentVipsBlock.setIsDividable(false);
			return true;
		}

		_currentVipsBlock.setIsVisualBlock(true);
		_currentVipsBlock.setIsDividable(false);

		String fontWeight = "";
		int fontSize = 0;

		for (Box childNode : node.getSubBoxList())
		{
			int childFontSize = childNode.getVisualContext().getFont().getSize();

			if (childNode instanceof TextBox)
			{
				if (fontSize > 0)
				{
					if (fontSize != childFontSize)
					{
						_currentVipsBlock.setDoC(9);
						break;
					}
					else
						_currentVipsBlock.setDoC(10);
				}
				else
					fontSize = childFontSize;
				continue;
			}

			ElementBox child = (ElementBox) childNode;

			if (child.getStylePropertyValue("font-weight") == null)
				return false;

			if (fontSize > 0)
			{
				if (child.getStylePropertyValue("font-weight").toString().equals(fontWeight) &&
						childFontSize == fontSize)
				{
					_currentVipsBlock.setDoC(10);
				}
				else
				{
					_currentVipsBlock.setDoC(9);
					break;
				}
			}
			else
			{
				fontWeight = child.getStylePropertyValue("font-weight").toString();
				fontSize = childFontSize;
			}
		}

		return true;
	}

	/**
	 * VIPS Rule Five
	 * <p>
	 * If one of the child nodes of the DOM node is line-break node, then
	 * divide this DOM node.
	 * 
	 * @param node
	 *            Input node
	 * 
	 * @return True, if rule is applied, otherwise false.
	 */
	private boolean ruleFive(ElementBox node)
	{
		System.err.println("Applying rule Five on " + node.getNode().getNodeName() + " node");

		if (node.getSubBoxList().isEmpty())
			return false;

		for (Box childNode : node.getSubBoxList())
			if (childNode.isBlock())
				return true;

		return false;
	}

	/**
	 * VIPS Rule Six
	 * <p>
	 * If one of the child nodes of the DOM node has HTML tag &lt;hr&gt;, then
	 * divide this DOM node
	 * 
	 * @param node
	 *            Input node
	 * 
	 * @return True, if rule is applied, otherwise false.
	 */
	private boolean ruleSix(ElementBox node)
	{
		System.err.println("Applying rule Six on " + node.getNode().getNodeName() + " node");
		if (node.getSubBoxList().isEmpty())
			return false;

		for (Box childNode : node.getSubBoxList())
			if (childNode.getNode().getNodeName().equals("hr"))
				return true;

		return false;
	}

	/**
	 * VIPS Rule Seven
	 * <p>
	 * If the background color of this node is different from one of its
	 * children’s, divide this node and at the same time, the child node with
	 * different background color will not be divided in this round.
	 * Set the DoC value (6-8) for the child node based on the &lt;html&gt;
	 * tag of the child node and the size of the child node.
	 * 
	 * @param node
	 *            Input node
	 * 
	 * @return True, if rule is applied, otherwise false.
	 */
	private boolean ruleSeven(ElementBox node)
	{
		System.err.println("Applying rule Seven on " + node.getNode().getNodeName() + " node");
		if (node.getSubBoxList().isEmpty())
			return false;

		if (isTextNode(node))
			return false;

		//String nodeBgColor = node.getStylePropertyValue("background-color");
		String nodeBgColor = _currentVipsBlock.getBgColor();

		for (VipsBlock vipsStructureChild : _currentVipsBlock.getChildren())
		{
			if (!(vipsStructureChild.getBgColor().equals(nodeBgColor)))
			{
				vipsStructureChild.setIsDividable(false);
				vipsStructureChild.setIsVisualBlock(true);
				// TODO DoC values
				vipsStructureChild.setDoC(7);
				return true;
			}
		}

		return false;
	}

	/**
	 * VIPS Rule Eight
	 * <p>
	 * If the node has at least one text node child or at least one virtual
	 * text node child, and the node's relative size is smaller than
	 * a threshold, then the node cannot be divided.
	 * Set the DoC value (from 5-8) based on the html tag of the node.
	 * @param node
	 *            Input node
	 * 
	 * @return True, if rule is applied, otherwise false.
	 */
	private boolean ruleEight(ElementBox node)
	{
		System.err.println("Applying rule Eight on " + node.getNode().getNodeName() + " node");
		if (node.getSubBoxList().isEmpty())
			return false;

		int cnt = 0;

		for (Box childNode : node.getSubBoxList())
		{
			if (childNode instanceof TextBox)
			{
				cnt++;
				continue;
			}
			ElementBox child = (ElementBox) childNode;
			if (isTextNode(child)|| isVirtualTextNode(child))
				cnt++;
		}

		if (cnt == 0)
			return false;

		if (node.getWidth() * node.getHeight() > _sizeTresholdHeight * _sizeTresholdWidth)
			return false;

		_currentVipsBlock.setIsVisualBlock(true);
		_currentVipsBlock.setIsDividable(false);

		_currentVipsBlock.setDoC(222);
		//TODO DoC Part
		return true;
	}

	/**
	 * VIPS Rule Nine
	 * <p>
	 * If the child of the node with maximum size are small than
	 * a threshold (relative size), do not divide this node. <br>
	 * Set the DoC based on the html tag and size of this node.
	 * @param node
	 *            Input node
	 * 
	 * @return True, if rule is applied, otherwise false.
	 */
	private boolean ruleNine(ElementBox node)
	{
		System.err.println("Applying rule Nine on " + node.getNode().getNodeName() + " node");
		if (node.getSubBoxList().isEmpty())
			return false;

		int maxSize = 0;

		for (Box childNode : node.getSubBoxList())
		{
			System.out.println(childNode.getWidth() + " " + childNode.getHeight());
			int childSize = childNode.getWidth() * childNode.getHeight();

			if (maxSize < childSize)
				maxSize = childSize;
		}

		if (maxSize > _sizeTresholdWidth * _sizeTresholdHeight)
			return true;

		//TODO set DOC
		_currentVipsBlock.setIsVisualBlock(true);
		_currentVipsBlock.setIsDividable(false);
		_currentVipsBlock.setDoC(111);
		return false;
	}

	/**
	 * VIPS Rule Ten
	 * <p>
	 * If previous sibling node has not been divided, do not divide this node
	 * @param node
	 *            Input node
	 * 
	 * @return True, if rule is applied, otherwise false.
	 */
	private boolean ruleTen(ElementBox node)
	{
		System.err.println("Applying rule Ten on " + node.getNode().getNodeName() + " node");

		//VipsBlock previousSiblingVipsBlock = null;
		//findPreviousSiblingNodeVipsBlock(node.getNode().getPreviousSibling(), _vipsBlocks, previousSiblingVipsBlock);

		_tempVipsBlock = null;
		findPreviousSiblingNodeVipsBlock(node.getNode().getPreviousSibling(), _vipsBlocks);

		if (_tempVipsBlock == null)
			return false;

		if (_tempVipsBlock.isAlreadyDivided())
			return true;

		return false;
	}

	/**
	 * VIPS Rule Eleven
	 * <p>
	 * Divide this node.
	 * @param node
	 *            Input node
	 * 
	 * @return True, if rule is applied, otherwise false.
	 */
	private boolean ruleEleven(ElementBox node)
	{
		System.err.println("Applying rule Eleven on " + node.getNode().getNodeName() + " node");

		return (isTextNode(node)) ? false : true;
	}

	/**
	 * VIPS Rule Twelve
	 * <p>
	 * Do not divide this node <br>
	 * Set the DoC value based on the html tag and size of this node.
	 * @param node
	 *            Input node
	 * 
	 * @return True, if rule is applied, otherwise false.
	 */
	private boolean ruleTwelve(ElementBox node)
	{
		System.err.println("Applying rule Twelve on " + node.getNode().getNodeName() + " node");

		_currentVipsBlock.setIsDividable(false);
		_currentVipsBlock.setIsVisualBlock(true);

		_currentVipsBlock.setDoC(333);
		//TODO DoC Part
		return true;
	}

	/**
	 * @return the _sizeTresholdWidth
	 */
	public int getSizeTresholdWidth()
	{
		return _sizeTresholdWidth;
	}

	/**
	 * @param sizeTresholdWidth the _sizeTresholdWidth to set
	 */
	public void setSizeTresholdWidth(int sizeTresholdWidth)
	{
		this._sizeTresholdWidth = sizeTresholdWidth;
	}

	/**
	 * @return the _sizeTresholdHeight
	 */
	public int getSizeTresholdHeight()
	{
		return _sizeTresholdHeight;
	}

	/**
	 * @param sizeTresholdHeight the _sizeTresholdHeight to set
	 */
	public void setSizeTresholdHeight(int sizeTresholdHeight)
	{
		this._sizeTresholdHeight = sizeTresholdHeight;
	}

	public VipsBlock getVipsBlocks()
	{
		return _vipsBlocks;
	}

	/**
	 * Finds previous sibling node's VIPS block.
	 * @param node Node
	 * @param vipsBlock Actual VIPS block
	 * @param foundBlock VIPS block for given node
	 */
	private void findPreviousSiblingNodeVipsBlock(Node node, VipsBlock vipsBlock)
	{
		if (vipsBlock.getBox().getNode().equals(node))
		{
			_tempVipsBlock = vipsBlock;
			return;
		}
		else
			for (VipsBlock vipsBlockChild : vipsBlock.getChildren())
				findPreviousSiblingNodeVipsBlock(node, vipsBlockChild);
	}
}
