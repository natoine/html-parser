package fr.natoine.html.tests;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;

import fr.natoine.html.HTMLPage;
import junit.framework.TestCase;

public class HTMLPageTest extends TestCase
{
	public HTMLPageTest(String name) 
	{		    
		super(name);
	}
	
	public void testInstance()
	{
		HTMLPage testPage = new HTMLPage("http://www.natoine.fr/");
		System.out.println("[TestInstance] title : " + testPage.getTitle());
		//TEST annotation avec un élément entre le début et la fin de l'annotation
		//String xpointer10 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/5,4)" ;
		//String xpointer20 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/5,70)" ;
		
		String xpointer10 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/3,4)" ;
		String xpointer20 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/3,70)" ;
		
		
		try {
			testPage.addAnnotationSpan(xpointer10, xpointer20, "background-color:yellow;", "contient un lien entre le début et la fin", ""+1);
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//TEST deux annotations identiques
		String xpointer16 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/1,29)" ;
		String xpointer26 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/1,32)" ;
		
		String xpointer17 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/1,29)" ;
		String xpointer27 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/1,32)" ;
		
		try {
			testPage.addAnnotationSpan(xpointer16, xpointer26, "background-color:yellow;", "deux annotations identiques 8", ""+8);
			testPage.addAnnotationSpan(xpointer17, xpointer27, "background-color:green;", "deux annotations identiques 9", ""+9);
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	//TEST annotation dans annotation
		String xpointer1 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/1,12)" ;
		String xpointer2 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/1,27)" ;
		
		String xpointer11 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/1,14)" ;
		String xpointer21 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/1,25)" ;
		
		String xpointer14 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/1,16)" ;
		String xpointer24 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/1,23)" ;
		
		String xpointer15 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/1,18)" ;
		String xpointer25 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/1,21)" ;
		
		//String xpointer = "http://www.natoine.fr#xpointer(body/1/2,16)" ;
		//HTMLPage testPage = new HTMLPage();
		//HTMLPage testPage = new HTMLPage("http://www.natoine.fr/");
		try {
			testPage.addAnnotationSpan(xpointer1, xpointer2, "background-color:yellow;", "annotation large 2", ""+2);
			testPage.addAnnotationSpan(xpointer11, xpointer21, "background-color:blue;", "annotation 3 dans annotation 2", ""+3);
			testPage.addAnnotationSpan(xpointer14, xpointer24, "background-color:green;", "annotation 6 dans annotation 3", ""+6);
			testPage.addAnnotationSpan(xpointer15, xpointer25, "background-color:red;", "annotation 7 dans annotation 6",""+ 7);
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//TEST annotation à cheval dans une annotation et hors de l'annotation
		/*String xpointer12 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/3,14)" ;
		String xpointer22 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/3,25)" ;
		
		String xpointer13 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/3,18)" ;
		String xpointer23 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/3,28)" ;*/
		
		String xpointer12 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/2,14)" ;
		String xpointer22 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/2,25)" ;
		
		String xpointer13 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/2,18)" ;
		String xpointer23 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/2,28)" ;
		
		try {
			testPage.addAnnotationSpan(xpointer12, xpointer22, "background-color:yellow;", "annotation 4", ""+4);
			testPage.addAnnotationSpan(xpointer13, xpointer23, "background-color:green;", "annotation 5 à cheval sur annotation 4", ""+5);
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//TESTs annotation sur plusieurs éléments
			//TEST annotation commence dans un noeud, finit dans l'un de ces fils :
		//String xpointer100 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/5,4)" ;
		//String xpointer200 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/5/1,2)" ;
		String xpointer100 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/3,4)" ;
		String xpointer200 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/3/1,2)" ;
		
		try {
			testPage.addAnnotationSpan(xpointer100, xpointer200, "background-color:grey;", "commence dans un noeud, finit dans un fils",""+ 10);
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
			//TEST annotation commence dans un noeud, finit dans un noeud ailleurs dans le dom
		String xpointer101 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/1,4)" ;
		//String xpointer201 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/5/1,4)" ;
		String xpointer201 = "http://www.natoine.fr/#xpointer(id(\"maincontent\")/3/1,5)" ;
			
		try {
			testPage.addAnnotationSpan(xpointer101, xpointer201, "background-color:purple;", "dans deux noeuds différents", ""+11);
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		System.out.println("body après xpointer add : " + testPage.getBody());
		/*System.out.println("xpointer : " + xpointer);
		String[] _xpointer_split = testPage.xpointerSplit(xpointer);
		for(int i = 0 ; i < _xpointer_split.length ; i ++)
		{
			System.out.println(_xpointer_split[i]);
		}
		//System.out.println("text id : " + testPage.getTextPositionXpointer(xpointer));
		try {
			Parser parser = Parser.createParser(testPage.getBody() , null);
			NodeList nl = parser.parse(null);
			Node testnode = testPage.getNodeXpointer(xpointer , nl);
			System.out.println(testnode.toHtml());
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		//HTMLPage testPage = new HTMLPage("http://www.jeu2debat.org/");
		//HTMLPage testPage = new HTMLPage("http://www.natoine.fr/pipo");
		//HTMLPage testPage = new HTMLPage("http://www.natoine.fr/");
		//HTMLPage testPage = new HTMLPage("http://www.natoine.fr:8180/xwiki");
		//HTMLPage testPage = new HTMLPage("http://www.natoine.fr");
		//HTMLPage testPage = new HTMLPage("http://www.natoine.fr/wordpress");
		//HTMLPage testPage = new HTMLPage("http://www.google.fr");
		//HTMLPage testPage = new HTMLPage("http://localhost:8080/portal");
		//HTMLPage testPage = new HTMLPage("http://localhost:8080/PortletAnnotation-1.0.0/ServletViewResources?id=9666560");
		//HTMLPage testPage = new HTMLPage("http://www.wikipedia.fr");
		//System.out.println("Test : " + testPage.toString());
	}
	
	public void testIsChild()
	{
		HTMLPage testPage = new HTMLPage("http://www.natoine.fr/");
		
		//TEST annotation avec un élément entre le début et la fin de l'annotation
		String xpointer10 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/5,4)" ;
		String xpointer20 = "http://www.natoine.fr#xpointer(id(\"maincontent\")/5/2,70)" ;
		try {
			boolean isChild = testPage.isChildXPointer(xpointer10, xpointer20);
			System.out.println("is child : " + isChild);
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testExtractTitle()
	{
		HTMLPage testPage = new HTMLPage();
		testPage.setURL("http://www.natoine.fr");
		try {
			testPage.extractTitle();
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("[TestExtractTitle] title : " + testPage.getTitle());
	}
	
	public void testEncodage()
	{
		HTMLPage testPage = new HTMLPage("http://www.natoine.fr/expe_vero/platon.html");
		//System.out.println("[testEncodage] encoding : " + testPage.getEncoding());
		//System.out.println("[testEncodage] body : " + testPage.getBody());
		
		testPage = new HTMLPage("http://www.google.fr");
		//System.out.println("[testEncodage] encoding : " + testPage.getEncoding());
		//System.out.println("[testEncodage] body : " + testPage.getBody());
		
		
		testPage = new HTMLPage("http://www.google.fr/intl/fr/ads/");
		//System.out.println("[testEncodage] encoding : " + testPage.getEncoding());
		//System.out.println("[testEncodage] body : " + testPage.getBody());
		
		testPage = new HTMLPage("http://www.lirmm.fr");
		//System.out.println("[testEncodage] encoding : " + testPage.getEncoding());
		//System.out.println("[testEncodage] body : " + testPage.getBody());
		
		testPage = new HTMLPage("http://www.natoine.fr");
		System.out.println("[testEncodage] encoding : " + testPage.getEncoding());
		System.out.println("[testEncodage] body : " + testPage.getBody());
	}
}
