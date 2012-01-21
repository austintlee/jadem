package taglearner.XMLManager;

//import java.util.*;
import java.io.IOException;
import java.io.File;
import org.w3c.dom.*;
import org.w3c.dom.traversal.NodeFilter;
import org.xml.sax.*;

import javax.xml.parsers.*;

//org.apache.xerces
import org.apache.xerces.dom.*;
import org.apache.xerces.impl.*;

// Parses an XML file and returns a corresponding DOM object
public class InputParser {

	private DocumentBuilder builder;
	File xml;
	
	public InputParser(File f) {
		this.xml = f;
	}
	
	public NodeIteratorImpl parse(NodeFilter filter) {
		Document doc = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {

		}
		
		try {
			doc = builder.parse(this.xml);
			//System.out.println("Parsing done");
		} catch (SAXException e) {
			
		} catch (IOException e) {
			
		}
		
		NodeIteratorImpl iter = 
			(NodeIteratorImpl) ((DocumentImpl) doc).createNodeIterator((Node) doc,
					NodeFilter.SHOW_ELEMENT, filter, true); 
		
		return iter;
	}


/*
	public static void main(String[] argv) {
	
		//System.out.println("Hello World");   // sanity check
		
		if (argv.length < 2) {
			System.out.println("Usage:");
			System.out.println("arg1 = articles_xxxx.xml, arg2 = <path_to_xml>");
		}
		
		String articleXml = "/Users/austin/classes/6901/SampleData/articles_1894110101.xml";
		InputParser ip = new InputParser(articleXml);
		
		Document article = null;
		
		try {
			article = ip.parse();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		NodeList nodes;
		if (article != null)
			nodes = article.getDocumentElement().getChildNodes();
		else {
			System.out.println("Null doc");
			return;
		}
		
		//int dmdSecCnt=0;
		//System.out.println(Integer.toString(nodes.getLength()));
		Node mdWrap, xmlData, mods;
		for (int i=0 ; i < nodes.getLength() ; i++) {
			Node node = nodes.item(i);
			//System.out.println(node.toString());
			if (node.getNodeType() == Node.ELEMENT_NODE 
					&& ((Element) node).getTagName().equals("dmdSec")) {
				if (((Element) node).getAttribute("ID").startsWith("artModsBib")) {
					//NodeList dmdChildNodes = node.getChildNodes();
					if (node.hasChildNodes()) {
						
						mdWrap = node.getFirstChild();
						if (mdWrap.getNodeType() == Node.ELEMENT_NODE) {
							if (((Element) mdWrap).getTagName().equals("mdWrap"))
								System.out.println("Found mdWrap");
							xmlData = mdWrap.getFirstChild();
							if (xmlData != null)
								System.out.println(((Element) xmlData).getTagName());
							else
								System.out.println("xmlData no child nodes");
							//Node mods = xmlData.getFirstChild();
							//System.out.println(((Element) mods).getTagName());
						}
					}
					else
						System.out.println("mdWrap no child nodes");
				}
				//dmdSecCnt++;
			}
		}
		//System.out.println("dmdSec found: "+Integer.toString(dmdSecCnt));
		//System.out.println("");
	}
	*/
}
