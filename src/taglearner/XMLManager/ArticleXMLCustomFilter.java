package taglearner.XMLManager;

import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;

public class ArticleXMLCustomFilter implements NodeFilter {

	public short acceptNode(Node n) {
		// TODO Auto-generated method stub
		if (n.getNodeType() == Node.ELEMENT_NODE)
			return NodeFilter.FILTER_ACCEPT;
		return NodeFilter.FILTER_SKIP;
	}
}
