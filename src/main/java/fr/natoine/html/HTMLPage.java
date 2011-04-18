/*
 * Copyright 2010 Antoine Seilles (Natoine)
 *   This file is part of html-parser.

    model-annotation is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    model-annotation is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with model-annotation.  If not, see <http://www.gnu.org/licenses/>.

 */
package fr.natoine.html;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.CssSelectorNodeFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.tags.HeadTag;
import org.htmlparser.tags.Html;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.StyleTag;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import fr.natoine.stringOp.StringOp;

public class HTMLPage 
{
	private String url;
	private String domain;
	private String title;
	private String css;
	private String body;
	private String scripts; //pour contenir les javascripts ou autres scripts de la page d'origine
	private String wrapperDiv;
	private String encoding ;
	private boolean valid;

	private static String DEFAULT_WRAPPER = "PortletBrowserContent";
	private static int DEFAULT_TIME_TO_CREATE = 3000 ;
	
	public HTMLPage()
	{
		valid = false ;
		url = "not a valid url";
		domain = "not a valid domain";
		title = "no title";
		css = "";
		body = "";
		wrapperDiv = DEFAULT_WRAPPER ;
		encoding = null ;
	}

	public HTMLPage(String _url)
	{
		this(_url, DEFAULT_TIME_TO_CREATE , DEFAULT_WRAPPER);//TODO tester le meilleur time pour serveur de prod. Mon localhost met de l'ordre d'1 seconde à répondre. Mon blog 2 secondes.
	}
	
	public HTMLPage(String _url , int _time_to_create , String _wrapperDiv)//throws MalformedURLException, IOException
	{
		//Sets the wrapper div
		wrapperDiv = _wrapperDiv ;
		//sets the url
		if(_url.endsWith("/")) url = _url.substring(0, _url.length()-1);
		else url = _url ;
		//processes the resources, sets the attributes
		if(_url.startsWith("http://"))
		{
			//Sets the domain
			domain = "http://" + extractDomain(_url);
			String response_content = extractFullContentPage(_url , _time_to_create);
			//System.out.println("[HTMLPage] response content : " + response_content);
			//don't try to get css, title and body if the url is not valid
			if(valid)
			{
				extractBodyTitleCss(response_content , _time_to_create);
				correctHREF("javascript:browserHREF");
				//corriger le body en fonction de l'encodage
				encodeBody();
			}
			else
			{
				//title prend l'url comme valeur par défaut. On peut vouloir annoter des ressources qui ne sont pas accessibles et qui n'ont pas de titre.
				title = url ;
			}
		}
		this.finalizeBody();
	}
	
	private void finalizeBody()
	{
		if(body != null && body.length()>0)
		{
			Pattern p = Pattern.compile("(<body>)|(<BODY>)");
	        Matcher m = p.matcher("");
	        m.reset(body);
	        body = m.replaceAll("<div id='" + wrapperDiv + "'>");
			p = Pattern.compile("(</body>)|(</BODY>)");
			m = p.matcher("");
			m.reset(body);
	        body = m.replaceAll("</div>");
		}
		else body = "";
	}
	
	private void encodeBody()
	{
		//CharsetEncoder encoder = Charset.forName("UTF-16").newEncoder();
		if(encoding != null)
		{
			if(encoding.equalsIgnoreCase("UTF-8"))
			{
				CharsetEncoder encoder = Charset.forName("ISO-8859-1").newEncoder();
				try {
					String decoded = new String(encoder.encode(CharBuffer.wrap(body.toCharArray())).array());
					//System.out.println("decoded : " + decoded);
					//CharsetDecoder decoder = Charset.forName(encoding).newDecoder();
					CharsetDecoder decoder = Charset.forName(encoding).newDecoder();
					decoded = decoder.decode(ByteBuffer.wrap(decoded.getBytes())).toString();
					//System.out.println("decoded2 : " + decoded);
					body = decoded ;
				} catch (CharacterCodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else
		{
			String[] olds = new String[16];
			String[] news = new String[16];
			olds[0] = "&acirc;"; news[0] = "â";
			olds[1] = "&agrave;"; news[1] = "à";
			olds[2] = "&eacute;"; news[2] = "é";
			olds[3] = "&ecirc;"; news[3] = "ê";
			olds[4] = "&egrave;"; news[4] = "è";
			olds[5] = "&euml;"; news[5] = "ë";
			olds[6] = "&icirc;"; news[6] = "î";
			olds[7] = "&iuml;"; news[7] = "ï";
			olds[8] = "&ocirc;"; news[8] = "ô";
			olds[9] = "&oelig;"; news[9] = "œ";
			olds[10] = "&ucirc;"; news[10] = "û";
			olds[11] = "&ugrave;"; news[11] = "ù";
			olds[12] = "&uuml;"; news[12] = "ü";
			olds[13] = "&ccedil;"; news[13] = "ç";
			olds[14] = "&lt;"; news[14] = "<";
			olds[15] = "&gt;"; news[15] = ">";
			for(int i = 0 ; i< olds.length ; i++)
			{
				body = body.replaceAll(olds[i], news[i]);
			}
		}
	}
	
	//javascript:browserHREF
	private void correctHREF(String _javascript_href_wrapper)
	{
		Parser parser = Parser.createParser(body , null);
		
		try 
		{
			NodeList nl = parser.parse(null);
			NodeList a_hrefs = nl.extractAllNodesThatMatch(new NodeClassFilter(LinkTag.class) , true);
			//System.out.println("[HTMLPage.correctHREF] nb ahref : " + a_hrefs.size());
			int nb_a = a_hrefs.size() ;
			int cpt_a ;
			char quote = '\"';
			for(cpt_a = 0 ; cpt_a < nb_a ; cpt_a ++ )
			{
				String true_href ;
				Node a = a_hrefs.elementAt(cpt_a);
				String original_href = ((TagNode) a).getAttribute("href");
				if(original_href!=null)
				{
					if(original_href.startsWith("http")) true_href = original_href;
					else if(original_href.startsWith("./"))
					{
						true_href = domain + "/" + original_href.substring(2);
					}
					//et si il y a plusieurs fois ../ ????
					else if(original_href.startsWith("../"))//remonte d'un dossier
					{
						int firstindexOfslash_href = original_href.indexOf("/");
						int lastindexOfslash = url.lastIndexOf("/");
						if(lastindexOfslash == url.length())
						{
							lastindexOfslash = url.substring(0, url.length()-1).lastIndexOf("/");
						}
						true_href = url.subSequence(0, lastindexOfslash) + "/" + original_href.substring(firstindexOfslash_href);
					}
					else if(original_href.startsWith("/")) true_href = domain + original_href;
					else true_href = url + "/" + original_href;	
					((TagNode) a).setAttribute("href" , _javascript_href_wrapper + "(\'" + true_href + "\')", quote);
				}
			}
			//System.out.println("[HTMLPage.correctHREF] nl toString : " + nl.toHtml());
			body = nl.toHtml();
		} catch (ParserException e) 
		{
			System.out.println("[HTMLPage.correctHREF] problems while Parsing");
			e.printStackTrace();
		}
	}
	
	public String extractTitle() throws ParserException
	{
		String response_content = extractFullContentPage(url , DEFAULT_TIME_TO_CREATE);
		if(valid)
		{
			title = url ;
			Parser parser = Parser.createParser(response_content , null);
			NodeList nl = parser.parse(null);
			NodeList titles = nl.extractAllNodesThatMatch(new NodeClassFilter(TitleTag.class), true);
			if(titles.size() > 0) 
			{
				if(titles.elementAt(0)instanceof Tag) title = ((Tag)titles.elementAt(0)).getFirstChild().getText();
			}
		}
		return title ;
	}
	
	public String extractDomain(String _url)
	{
		String domain = "not a valid domain";
		String[] _url_split = _url.split("/");
		if(_url_split.length > 1) return _url_split[2];
		else return domain ;
	}
	
	@SuppressWarnings("finally")
	public String extractFullContentResource(String _url , int _time_to_respond)
	{
		String response_content = null ;
		HttpClient httpclient = new DefaultHttpClient();
		//httpclient.getParams().setParameter(CoreProtocolPNames.HTTP_ELEMENT_CHARSET , "UTF-8");
		//httpclient.getParams().setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, "UTF-16");
		//httpclient.getParams().setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, "ISO-8859-1");
		//US-ASCII
		//httpclient.getParams().setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, "US-ASCII");
		httpclient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
		httpclient.getParams().setBooleanParameter(ClientPNames.HANDLE_AUTHENTICATION, true);
		httpclient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 3000);
		//HttpParams params = 
		//HttpClient httpclient = new DefaultHttpClient(params);
		HttpGet httpget = new HttpGet(_url);
		try
		{
			//Timer pour mettre un délai sur le temps d'exécution de la requête
			//Timer timer = new Timer();
			//timer.schedule(new TimerTaskTooLong(httpget , _url) , _time_to_respond);
			//HttpResponse response = httpclient.execute(httpget);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = httpclient.execute(httpget, responseHandler);
            if(responseBody != null) 
            {
            	response_content = responseBody ;
		//		valid = true ;
            }
		}
		catch (ClientProtocolException e) 
		{
		//	valid = false ;
			System.out.println("[HTMLPage.extractFullContentCssLink] url : " + _url + " doesn't support GET requests !!! ");
			e.printStackTrace();
		}
		catch (IOException e) 
		{
		//	valid = false ;
			System.out.println("[HTMLPage.extractFullContentCssLink] url : " + _url + " send no data !!! Not responding ... ");
			e.printStackTrace();
		}
		finally
		{
			httpclient.getConnectionManager().shutdown();
			return response_content ;
		}
	}
	
	private String extractFullContentPage(String _url , int _time_to_respond)
	{
		String content = extractFullContentResource(_url, _time_to_respond);
		if(content != null) valid = true;
		else valid = false ;
		//return Translate.decode(content) ;//pour régler le problème d'encodage
		//return Translate.encode(content) ;//pour régler le problème d'encodage
		return content ;
	}
	
	private void extractBodyTitleCss(String _html , int _time_to_extract_css)
	{
		try
		{
			Parser parser = Parser.createParser(_html , null);
			NodeList nl = parser.parse(null);
			//NodeList htmls = nl.extractAllNodesThatMatch (new TagNameFilter ("HTML"));
			NodeList htmls = nl.extractAllNodesThatMatch(new NodeClassFilter(Html.class));
			if(htmls.size()>0)
			{
				//NodeList heads = htmls.elementAt(0).getChildren().extractAllNodesThatMatch (new TagNameFilter ("HEAD"));
				NodeList heads = htmls.elementAt(0).getChildren().extractAllNodesThatMatch(new NodeClassFilter(HeadTag.class));
				NodeList bodys = htmls.elementAt(0).getChildren().extractAllNodesThatMatch(new NodeClassFilter(BodyTag.class));
				int nb_heads_node = heads.size() ;
				if(nb_heads_node >0 )
				{
					//NodeList titles = heads.elementAt(0).getChildren().extractAllNodesThatMatch (new TagNameFilter ("TITLE"));
					NodeList titles = heads.elementAt(0).getChildren().extractAllNodesThatMatch(new NodeClassFilter(TitleTag.class));
					if (titles.size() > 0)
					{
						if(titles.elementAt(0) instanceof Tag)
						{
							Tag tag_title = (Tag)titles.elementAt(0);
							title = tag_title.getFirstChild().getText();
						}
						else title = url ;
					}
					else
					{
						System.out.println("[HTMLPage.extractBodyTitleCss] no title tag, url for default title value");
						title = url ;
					}
					//encoding
					NodeList metas = heads.elementAt(0).getChildren().extractAllNodesThatMatch(new NodeClassFilter(MetaTag.class));
					if(metas.size() > 0)
					{
						int metas_size = metas.size();
						for(int cpt_metas = 0 ; cpt_metas < metas_size ; cpt_metas ++)
						{
							MetaTag meta = (MetaTag)metas.elementAt(cpt_metas);
							String httpEquiv = meta.getHttpEquiv();
							if(httpEquiv != null)
							{
								if(httpEquiv.equalsIgnoreCase("Content-Type"))
								{
									String content = meta.getMetaContent();
									if(content.contains("charset"))
									{
										int charset_index = content.indexOf("charset");
										int egal_index = content.indexOf("=", charset_index);
										String charset = content.substring(egal_index + 1, content.length());
										encoding = StringOp.deleteBlanks(charset); 
										break;
									}
								}
							}
							else
							{
								String charset = meta.getAttribute("charset");
								if(charset != null) 
								{
									encoding = charset ;
									break ;
								}
							}
						}
					}
					//CSS
					String wip_css = "";
					//Parcourir tous les noeuds head (bien qu'il ne devrait y en avoir qu'un seul)
					int cpt_heads_node ;
					//System.out.println("[HTMLPage.extractBodyTitleCss] nb heads : " + nb_heads_node);
					for(cpt_heads_node = 0 ; cpt_heads_node < nb_heads_node ; cpt_heads_node ++)
					{
						NodeList headChildren = heads.elementAt(cpt_heads_node).getChildren();
						/*Marche pas !!! */
						 // NodeList links = headChildren.extractAllNodesThatMatch(new NodeClassFilter(LinkTag.class));
						//Marche mais sert à rien vu que parcourt de tous les noeuds
						//NodeList styles = headChildren.extractAllNodesThatMatch(new NodeClassFilter(StyleTag.class));
						//Parcourir tous les noeuds dans head pour extraire les link de css
						int cpt_insideHead_nodes ;
						int nb_insideHead_nodes = headChildren.size() ;
						//System.out.println("[HTMLPage.extractBodyTitleCss] nb head children : " + nb_insideHead_nodes);
						for(cpt_insideHead_nodes = 0 ; cpt_insideHead_nodes < nb_insideHead_nodes ; cpt_insideHead_nodes ++)
						{
							Node currentNode = headChildren.elementAt(cpt_insideHead_nodes);
							//cas de css <style>
							if(currentNode instanceof StyleTag)
							{
								wip_css = wip_css.concat(((StyleTag)currentNode).getStyleCode());
								//System.out.println("[HTMLPage.extractBodyTitleCss] wip_css : " + wip_css);
							}
							//cas de css référencée par <link rel="stylesheet" href=""
							if(currentNode instanceof TagNode)
							{
								//if(((LinkTag)currentNode).getAttribute("rel").equalsIgnoreCase("stylesheet"))
								if(((TagNode)currentNode).getRawTagName().equalsIgnoreCase("link") && ((TagNode)currentNode).getAttribute("rel").equalsIgnoreCase("stylesheet"))
								{
									//Récupération de l'url de la css référencée
									
									//System.out.println("[HTMLPage.extractBodyTitleCss] LinkTag");
									//String href_css = ((LinkTag)currentNode).extractLink();
									String href_css = ((TagNode)currentNode).getAttribute("href");
									//System.out.println("[HTMLPage.extractBodyTitleCss] href css : " + href_css);
									//récupérer le contenu des css référencées
									String true_url_href_css = null ;
									if(href_css.startsWith("http://")) true_url_href_css = href_css;
									//TODO tester
									else if(href_css.startsWith("./"))
									{
										true_url_href_css = domain + "/" + href_css.substring(2);
									}
									//et si il y a plusieurs fois ../ ????
									else if(href_css.startsWith("../"))//remonte d'un dossier
									{
										int firstindexOfslash_href = href_css.indexOf("/");
										int lastindexOfslash = url.lastIndexOf("/");
										if(lastindexOfslash == url.length())
										{
											lastindexOfslash = url.substring(0, url.length()-1).lastIndexOf("/");
										}
										true_url_href_css = url.subSequence(0, lastindexOfslash) + "/" + href_css.substring(firstindexOfslash_href);
									}
									else if(href_css.startsWith("/")) true_url_href_css = domain + href_css;
									else true_url_href_css = url + "/" + href_css;	
									//Récupération de la css
									//System.out.println("[HTMLPage.extractBodyTitleCss] true href css : " + true_url_href_css );
									String css_content = extractFullContentResource(true_url_href_css, _time_to_extract_css);
									if(css_content != null) wip_css = wip_css.concat(css_content);
									//System.out.println("[HTMLPage.extractBodyTitleCss] wip_css : " + wip_css);
								}
							}
						}
					}
					//css = wip_css ;
					//TODO virer les commentaires dans la css les /* commentaires ... bla bla */
					css = this.deleteCommentsNewLine(wip_css , wrapperDiv);
				}
				else
				{
					System.out.println("[HTMLPage.extractBodyTitleCss] no head tag, default title value = url");
					title = url ;
				}
				if(bodys.size() > 0)
				{
					//System.out.println("[HTMLPage.extractBodyTitleCss] there is a body");
					body = bodys.elementAt(0).toHtml() ;
				}
			}
			else
			{	
				System.out.println("[HTMLPage.extractBodyTitleCss] not a valid HTML content");
				title = url ;
			}
		}
		catch(ParserException e)
		{
			title = url ;
			System.out.println("[HTMLPage.extractBodyTitleCss] error parsing HTML content");
			e.printStackTrace();
		}
	}
	//PortletBrowserContent
	//Deletes comments and new lines in css 
	public String deleteCommentsNewLine(String _wip_css, String _new_englobing_div)
	{
		if(_wip_css == null || _wip_css.length() == 0) return "";
		// Create a pattern to match comments
		//Pattern p = Pattern.compile("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", Pattern.MULTILINE);
		Pattern p = Pattern.compile("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)", Pattern.MULTILINE);
        Matcher m = p.matcher("");
        m.reset(_wip_css);
        String result = m.replaceAll("");
        // Create a pattern to match all new lines and all tabs
        p = Pattern.compile("(\n)|(\t)");
        m = p.matcher("");
        m.reset(result);
        result = m.replaceAll("");
        // Create a pattern to match all multiple " "
        p = Pattern.compile(" (?= )|(?<= ) ");
        m = p.matcher("");
        m.reset(result);
        result = m.replaceAll(" ");
        //creates a pattern to match all }
        p = Pattern.compile("}");
        m = p.matcher("");
        m.reset(result);
        result = m.replaceAll("} #" + _new_englobing_div +" ");
        int cpt_last_spaces_index = result.length() ;
        while(cpt_last_spaces_index > 0 && result.charAt(cpt_last_spaces_index -1) == ' ')
        {
        	cpt_last_spaces_index -- ;
        }
        result = result.substring(0, cpt_last_spaces_index);
        if(result.endsWith("#" + _new_englobing_div)) result = result.substring(0,result.lastIndexOf("#" + _new_englobing_div));
        result = ("#" + _new_englobing_div + " ").concat(result);
        return result ;
	}
	
	
	public String toString()
	{
		String _to_print = "Classe " + this.getClass();
		_to_print = _to_print.concat(" url : " + url);
		_to_print = _to_print.concat(" domain : " + domain);
		_to_print = _to_print.concat(" title : " + title);
		_to_print = _to_print.concat(" css : " + css);
		_to_print = _to_print.concat(" body : " + body);
		if(valid) _to_print = _to_print.concat(" valid !!!");
		else _to_print = _to_print.concat(" not valid !!!");
		return _to_print;
	}

	public String getEncoding() 
	{
		return encoding;
	}

	public void setEncoding(String encoding) 
	{
		this.encoding = encoding;
	}
	
	public String getURL() {
		return url;
	}
	public void setURL(String uRL) {
		url = uRL;
	}
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getCss() {
		return css;
	}
	public void setCss(String css) {
		this.css = css;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	
	//traitement des XPointer
	//exemples de types de xpointer acceptés pour l'instant :
	//http://www.natoine.fr#xpointer(body/1/2,16)
	//http://www.natoine.fr#xpointer(id(\"maincontent\")/1,14)
	
	public void setScripts(String scripts) {
		this.scripts = scripts;
	}

	public String getScripts() {
		return scripts;
	}

	public String[] xpointerSplit(String _xpointer)
	{
		String xpointer_tag = "#xpointer(" ;
		int begin_sub = _xpointer.indexOf(xpointer_tag) + xpointer_tag.length();
		String clean_xpointer = _xpointer.substring(begin_sub , _xpointer.length());
		clean_xpointer = clean_xpointer.substring(0, clean_xpointer.indexOf(','));
		return clean_xpointer.split("/");
	}

	//retourne l'indice textuel du xpointer
	private int getTextPositionXpointer(String _xpointer)
	{
		int coma_index = _xpointer.indexOf(',') ;
		if(coma_index > 0 && coma_index < _xpointer.length())
		{
			String position = _xpointer.substring(coma_index + 1, _xpointer.length()-1);
			return Integer.parseInt(position);
		}
		else return -1 ;
	}
	
	public boolean isChildXPointer(String _xpointer_father, String _xpointer_child) throws ParserException
	{
		//System.out.println("father : " + _xpointer_father);
		//System.out.println("child : " + _xpointer_child);
		if(_xpointer_father.contains(","))
		{
			String clean_xpointer_father = _xpointer_father.split(",")[0];
			if(_xpointer_child.startsWith(clean_xpointer_father)) return true ;
		}
		Parser parser = Parser.createParser(body , null);
		NodeList nl = parser.parse(null);
		Node father = this.getNodeXpointer(_xpointer_father, nl);
		Node child = this.getNodeXpointer(_xpointer_child, nl);
		//System.out.println("Father Node : " + father.toHtml());
		//System.out.println("Child Node : " + child.toHtml());
		return isChildNode(father , child);
	}
	
	public boolean isChildNode(Node _father , Node _child)
	{
		boolean to_return = false ;
		NodeList children = _father.getChildren();
		int children_length = 0 ;
		if(children!=null) children_length = children.size() ;
		int cpt_children = 0 ;
		while(!to_return && cpt_children < children_length)
		{
			Node to_test = children.elementAt(cpt_children);
			if(to_test.equals(_child)) return true ;
			else to_return = isChildNode(to_test , _child);
			cpt_children ++ ;
		}
		return to_return ;
	}
	
	//Renvoie le noeud relatif à un XPointer dans le body
	//!!!ATTENTION un saut de ligne entre deux divs est compté comme un textNode ... La merde htmlParser je vous jure.
	//!!!Attention, devrait ignorer les SPAN annotation déjà ajoutées ... TODO
	public Node getNodeXpointer(String _xpointer , NodeList _nl) throws ParserException
	{
		//toujours vérifier que l'on ne considére pas une balise span de classe annotation	
		Node current = null;
		String[] splited_xpointer = xpointerSplit(_xpointer);
		//créer la nodelist
		//Parser parser = Parser.createParser(body , null);
		//NodeList nl = parser.parse(null);
		int nb_selectors = splited_xpointer.length ;//nombre d'éléments dans le xpointe (séparations par des /)
		//premier élément : body ou id
		if(splited_xpointer[0].contains("body"))
		{
			//se placer sur le premier noeud
			current = _nl.elementAt(0);
			//System.out.println("Node value : " + current.toHtml());
		}
		else if(splited_xpointer[0].contains("id"))
		{
			//récupérer le véritable id
			String id = splited_xpointer[0].substring(splited_xpointer[0].indexOf("id") + 4 , splited_xpointer[0].length() - 2);
			//System.out.println("id : " + id );
			//se placer sur le noeud correspondant
			NodeList nlId = _nl.extractAllNodesThatMatch(new CssSelectorNodeFilter("#" + id), true) ;
			if(nlId.size() > 0 ) current = nlId.elementAt(0);
			//System.out.println("Node value : " + current.toHtml());
		}
		if(nb_selectors == 1) return current ; // il n'y a qu'un élément dans le xpointer donc on a fini le travail.
		else
		{
			int cpt_node_selector = 1 ;//pour compter les éléments du xpointer parcourus.
			while(cpt_node_selector < nb_selectors && current != null)
			{
				int indice_child_node = Integer.parseInt(splited_xpointer[cpt_node_selector]);
				//System.out.println("indice_child_node : " + indice_child_node);
				NodeList children = current.getChildren() ;
				//System.out.println("nb children :" + children.size());
				int nb_children = 0 ; //va compter le nombre de fils parcourus
				int true_nb_children = 0 ; // va compter le nombre de fils parcourus hors span d'annotations.
				int children_size = 0 ;
				if(children != null) children_size = children.size() ;
				while(nb_children < children_size && true_nb_children < indice_child_node)//parcours des fils du noeud courant jusqu'à trouver celui d'indice indice_child_node
				{
					Node current_child = children.elementAt(nb_children);
					//System.out.println("HTMLPage getNodeXpointer current_child nb " + nb_children + " : " + current_child.toPlainTextString() + " length : " + current_child.toPlainTextString().length());
					//tester si le noeud est une span d'annotation
					//if(current_child instanceof TagNode && ((TagNode)current_child).getTagName().equalsIgnoreCase("span") && ((TagNode)current_child).getAttribute("class").equals("annotation"))
					//if(current_child instanceof Span) System.out.println("SPAAAAAAAAAAAN !!!!!!!!!!!!!!!!!!!!!!");
					if(current_child instanceof Span && ((Span)current_child).getAttribute("class") != null && ((Span)current_child).getAttribute("class").equals("annotation"))
					{
						//System.out.println("HTMLPage getNodeXpointer considére bien que c'est un SPAN d'annotation");
						nb_children ++ ;
					}
					else if(current_child instanceof TextNode )
						//il faut ignorer les sauts de ligne dans le HTML qui ne servent qu'à structurer le code source, les navigateurs ne les prennent pas en compte eux
					{
						nb_children ++ ;
						/*String content = current_child.getText() ;
						content = content.replaceAll("\r", "").replaceAll("\n", "").replaceAll("\t", "");
						if(content.length() > 0) true_nb_children ++ ;
						else System.out.println("Noeud ignoré !!!");*/
					}
					else
					{
						nb_children ++ ;
						true_nb_children ++ ;
					}
				}
				//System.out.println("fin du while, nb_children : " + nb_children + " true_nb_children : " + true_nb_children );
				if(true_nb_children == indice_child_node && children!= null && children.size() > 0) 
				{
					if(nb_children > 0) current = children.elementAt(nb_children -1);
					else current = children.elementAt(0);
					//System.out.println("on change de current, nouveau current : " + current);
				}
				cpt_node_selector ++ ;
			}
		}
		//System.out.println("[getNodeXpointer] current : " + current.toHtml());
		//Attention au cas ou le noeud sur lequel on s'arréte est une SPAN d'annotation
		if(current instanceof Span && ((Span)current).getAttribute("class") != null && ((Span)current).getAttribute("class").equals("annotation"))
		{//dans ce cas, prendre le noeud suivant tant qu'il n'est pas une annotation. Si on a une abération, il vaut mieux renvoyer current quand même
			//System.out.println("[getNodeXpointer] parcours des nextSibling");
			Node next_sibling = current.getNextSibling();
			while(next_sibling!=null && next_sibling instanceof Span && ((Span)next_sibling).getAttribute("class")!=null && ((Span)next_sibling).getAttribute("class").equals("annotation")) 
			{
				next_sibling = next_sibling.getNextSibling();
			}
			if(next_sibling!=null) current = next_sibling ;
		}
		return current ;
	}

	//Renvoie vrai si deux xpointer référent un même noeud
	private boolean testSameNodeXpointer(String _xpointer1 , String _xpointer2) throws ParserException
	{
		//System.out.println("[testSameNodeXpointer]");
		if(! _xpointer1.substring(0, _xpointer1.indexOf("#")).equalsIgnoreCase(_xpointer2.substring(0, _xpointer2.indexOf("#")))) return false ;
		else if(_xpointer1.substring(0, _xpointer1.indexOf(',')).equalsIgnoreCase(_xpointer2.substring(0, _xpointer2.indexOf(',')))) return true ;
		else
		{
			Parser parser = Parser.createParser(body , null);
			NodeList nl = parser.parse(null);
			Node node1 = getNodeXpointer(_xpointer1 , nl);
			Node node2 = getNodeXpointer(_xpointer2 , nl);
			return node1 == node2 ;
		}
	}
	
	private void createSpanAndBefore(String _toModify , int _indice_start, int _indice_end, NodeList _newChildrenList, String _span_style , String _annotation_content, String _annotation_id , TagNode _endSpan)
	{
		String beforeSpan = _toModify.substring(0, _indice_start);
		String insideSpan = _toModify.substring(_indice_start, _indice_end);
		TextNode before_node = new TextNode(beforeSpan);
		_newChildrenList.add(before_node);
		Span span = createAnnotation(_span_style , _annotation_content , _annotation_id , insideSpan, _endSpan);
		_newChildrenList.add(span);
	}
	
	private void createSpanAndSurrounding(String _toModify , int _indice_start, int _indice_end, int _to_modify_content_length, NodeList _newChildrenList, String _span_style , String _annotation_content, String _annotation_id , TagNode _endSpan)
	{
		this.createSpanAndBefore(_toModify, _indice_start, _indice_end, _newChildrenList, _span_style, _annotation_content, _annotation_id, _endSpan);
		String afterSpan = _toModify.substring(_indice_end, _to_modify_content_length);
		TextNode after_node = new TextNode(afterSpan);
		_newChildrenList.add(after_node);
	}
	
	private Span createAnnotation(String _span_style , String _annotation_content , String _annotation_id , String _text_inside_span, TagNode _endSpan)
	{
		Span span = new Span();
		span.setAttribute("class" , "annotation" , '\"');
		span.setAttribute("style" , _span_style , '\"');
		span.setAttribute("title" , _annotation_content , '\"');
		span.setAttribute("id", "annotation_" + _annotation_id , '\'');
		NodeList newSpanChildrenList = new NodeList();
		TextNode inside_span_node = new TextNode(_text_inside_span);
		newSpanChildrenList.add(inside_span_node);
		span.setChildren(newSpanChildrenList);
		span.setEndPosition(_text_inside_span.length());
		span.setEndTag(_endSpan);
		return span ;
	}
	
	
	//Modifie le body en ajoutant les balises span nécessaires pour colorer une annotation dans la page
	public void addAnnotationSpan(String _xpointer_start, String _xpointer_end, String _span_style, String _annotation_content, String _annotation_id) throws ParserException
	{
		//System.out.println("[addAnnotationSpan] xpointerStart : " + _xpointer_start + "xpointerEnd : " + _xpointer_end);
		//Les balises span à placer sont de classe annotation
		TagNode endSpan = new TagNode();
		endSpan.setTagName("/SPAN");
		Parser parser = Parser.createParser(body , null);
		NodeList nl = parser.parse(null);
		int indice_start = getTextPositionXpointer(_xpointer_start);
		int indice_end = getTextPositionXpointer(_xpointer_end);
		int nb_span_annotation = 0 ;
		//Si les xpointers renvoient à un même noeud (l'id des balises span seront annotation_[id])
		if(testSameNodeXpointer(_xpointer_start, _xpointer_end))
		{
			//System.out.println("same node");
			Node nodeToModify = getNodeXpointer(_xpointer_start, nl);
			//System.out.println("addAnnotationSpan to modify node : " + nodeToModify.toHtml());
			if(nodeToModify != null)
			{
				if(indice_start < indice_end) addSpans(nodeToModify, indice_start, indice_end, nb_span_annotation, _span_style, _annotation_content, ""+_annotation_id, endSpan);
				else if(indice_start > indice_end) addSpans(nodeToModify, indice_end, indice_start, nb_span_annotation, _span_style, _annotation_content, ""+_annotation_id, endSpan);
				//on gére les cas ou les indices ne sont pas dans le bon ordre et on exclue les cas ou les indices sont identiques et qu'il n'y a rien à faire
			}
		}
		else
		{
			//System.out.println("not same node");
			//Si les xpointer ne renvoient pas à un même noeud (l'id des balises span seront annotation_[id]-[indice] ou indice est le nombre de spans précédentes pour cette annotation)
			boolean isChild = isChildXPointer(_xpointer_start, _xpointer_end);
			Node startNode = getNodeXpointer(_xpointer_start, nl);
			Node endNode = getNodeXpointer(_xpointer_end, nl);
			if(startNode != null && endNode != null)
			{
				int[] actual_state ;
				  //Soit la deuxième balise est un fils de la première
				if(isChild)
				{
					//System.out.println("isChild !!! xpointerStart : " + _xpointer_start + " xpointerEnd : " + _xpointer_end);
					//Colorer tout le texte allant du point de départ du xpointer jusqu'à la balise fils
					actual_state = addSpansAllNodeUntilSpecificNode(startNode, endNode, indice_start, nb_span_annotation, _span_style, _annotation_content, ""+_annotation_id, endSpan);
					indice_start = 0 ;
					nb_span_annotation = actual_state[1];
					//Colorer le texte de la balise fils/finale du début de cette balise jusqu'à son indice
					addSpans(endNode, 0, indice_end, nb_span_annotation, _span_style, _annotation_content, ""+ _annotation_id, endSpan);
				}
				else
				{
					//Soit la deuxième balise est à un autre niveau dans le DOM (pas fils)
					//System.out.println("is not Child !!! xpointerStart : " + _xpointer_start + " xpointerEnd : " + _xpointer_end);
			  		//pour noeud de départ
					actual_state = addSpansAllChildren(startNode, indice_start, 0, _span_style, _annotation_content, "" +_annotation_id, endSpan);
				  	nb_span_annotation = actual_state[1];
					//pour noeud entre départ et arrivé 
				  	//Problem, si le noeud d'arrivée n'est pas au même niveau dans le dom ?
					//Solution : Parcourir tous les noeuds à la même hauteur que le noeud de départ et tester si le noeud d'arrivée est l'un de leurs fils
				  	Node next_sibling = startNode.getNextSibling();
				  	NodeList toHighlight = new NodeList();
				  	while(next_sibling!=null && !isChildNode(next_sibling, endNode))
				  	{
				  		toHighlight.add(next_sibling);
				  		next_sibling = next_sibling.getNextSibling();
				  	}
				  	if(next_sibling != null)
				  	{
				  		//next_sibling est donc un père du noeud d'arrivée
				  		//Colorer tout ce qui est dans toHighlight
				  		for(int i =0 ; i< toHighlight.size() ; i++)
				  		{
				  			actual_state = addSpansProcessChildrenNoEndLimit(toHighlight.elementAt(i), 0, nb_span_annotation, _span_style, _annotation_content, ""+_annotation_id, endSpan);
				  			nb_span_annotation = actual_state[1];
				  		}
				  		//colorer tous les noeuds avant le endNode
				  		actual_state = addSpansAllNodeUntilSpecificNode(next_sibling, endNode, 0, nb_span_annotation, _span_style, _annotation_content,""+ _annotation_id, endSpan);
				  		nb_span_annotation = actual_state[1];
				  	}
					//pour noeud d'arrivé
					addSpans(endNode, 0, indice_end, nb_span_annotation, _span_style, _annotation_content, ""+ _annotation_id, endSpan);	
				}
			}
		}
		if(nl != null)
		{
			String new_html = nl.toHtml();
			if(new_html != null && new_html.length() > 0) body = new_html ;
		}
	}
		
	private int[] addSpansAllNodeUntilSpecificNode(Node startNode , Node endNode , int indice_start, int nb_span_annotation, String _span_style, String _annotation_content, String _annotation_id, TagNode endSpan)
	{
		NodeList startNodeChildren = startNode.getChildren();
		if(startNodeChildren != null)
		{
			NodeList newStartNodeList = new NodeList();
			int cpt_children = 0 ;
			Node current_child = startNodeChildren.elementAt(cpt_children);
			while(!current_child.equals(endNode) && !isChildNode(current_child , endNode))
			{
				//System.out.println("current_child : " + current_child.toHtml());
				if(current_child instanceof TextNode)
				{
					String toModifyContent = ((TextNode)current_child).getText();
					int to_modify_content_length = toModifyContent.length();
					//vérification que les indices sont compatibles, sinon on met 0 et length en valeurs
					if(indice_start > to_modify_content_length) indice_start = 0 ;
					if(nb_span_annotation >0 ) createSpanAndBefore(toModifyContent , indice_start, to_modify_content_length, newStartNodeList, _span_style , _annotation_content, ""+ _annotation_id +"-"+nb_span_annotation , endSpan);
					else createSpanAndBefore(toModifyContent , indice_start, to_modify_content_length, newStartNodeList, _span_style , _annotation_content, ""+ _annotation_id , endSpan);
					nb_span_annotation ++ ;
				}
				else 
				{
					int[] actual_state = addSpansNoEndLimit(current_child, indice_start, nb_span_annotation, _span_style, _annotation_content, ""+_annotation_id, endSpan);
					newStartNodeList.add(current_child);
					indice_start = 0;
					nb_span_annotation = actual_state[1];
				}
				cpt_children ++ ;
				current_child = startNodeChildren.elementAt(cpt_children);
			}
			//on ajoute à la liste les noeuds non modifiés
			while(cpt_children < startNodeChildren.size())
			{
				newStartNodeList.add(startNodeChildren.elementAt(cpt_children));
				cpt_children ++ ;
			}
			//on sette la nouvelle liste de fils
			startNode.setChildren(newStartNodeList);
		}
		int[] to_return = {indice_start , nb_span_annotation};
		return to_return ;
	}
	
	private int[] addSpans(Node _nodeToModify, int _indice_start, int _indice_end, int _nb_span_annotation, String _span_style , String _annotation_content, String _annotation_id, TagNode _endSpan)
	{
		//récupération des noeuds fils du noeud à modifier
		NodeList childrenOfNodeToModify = _nodeToModify.getChildren() ;
		if(childrenOfNodeToModify!=null)
		{
			int nb_children = childrenOfNodeToModify.size() ;
			if(nb_children > 0)//Si le noeud n'a pas au moins un fils TextNode, il n'y a rien à faire.
			{
				if(childrenOfNodeToModify.size() == 1 && childrenOfNodeToModify.elementAt(0) instanceof TextNode)//S'il n'y a que du texte dans le noeud, le traitement est simple
				{
					//System.out.println("addSpans only textNode");
					TextNode content_textnode = (TextNode)childrenOfNodeToModify.elementAt(0) ;
					String toModifyContent = content_textnode.getText() ;					
					int to_modify_content_length = toModifyContent.length();
					//vérification que les indices sont compatibles, sinon on met 0 et length en valeurs
					if(_indice_start > to_modify_content_length) _indice_start = 0 ;
					if(_indice_end > to_modify_content_length) _indice_end = to_modify_content_length ;
					NodeList newChildrenList = new NodeList();
					if(_nb_span_annotation >0 ) createSpanAndSurrounding(toModifyContent , _indice_start, _indice_end, to_modify_content_length, newChildrenList, _span_style , _annotation_content, ""+ _annotation_id +"-"+_nb_span_annotation , _endSpan);
					else createSpanAndSurrounding(toModifyContent , _indice_start, _indice_end, to_modify_content_length, newChildrenList, _span_style , _annotation_content, ""+ _annotation_id , _endSpan);
					_nodeToModify.setChildren(newChildrenList);
					_nb_span_annotation ++ ;
				}
				else
				{//il y a d'autres fils, pas qu'un noeud texte.
					//parcours de tous les fils
					int[] actual_state = addSpansProcessChildren(_nodeToModify, _indice_start, _indice_end, _nb_span_annotation, _span_style, _annotation_content, _annotation_id, _endSpan);
					_indice_start = actual_state[0];
					_indice_end = actual_state[1];
					_nb_span_annotation = actual_state[2]; 
				}
			}
		}
		else if(_nodeToModify instanceof TextNode)//en fait il n'y a que ce noeud à modifier
		{
			String toModifyContent = ((TextNode)_nodeToModify).getText();
			int to_modify_content_length = toModifyContent.length();
			//vérification que les indices sont compatibles, sinon on met 0 et length en valeurs
			if(_indice_start > to_modify_content_length) _indice_start = 0 ;
			if(_indice_end > to_modify_content_length) _indice_end = to_modify_content_length ;
			NodeList newChildrenList = new NodeList();
			if(_nb_span_annotation >0 ) createSpanAndSurrounding(toModifyContent , _indice_start, _indice_end, to_modify_content_length, newChildrenList, _span_style , _annotation_content, ""+ _annotation_id +"-"+_nb_span_annotation , _endSpan);
			else createSpanAndSurrounding(toModifyContent , _indice_start, _indice_end, to_modify_content_length, newChildrenList, _span_style , _annotation_content, ""+ _annotation_id , _endSpan);
			_nodeToModify.setChildren(newChildrenList);
			_nb_span_annotation ++ ;
		}
		int[] to_return = {_indice_start , _indice_end, _nb_span_annotation};
		return to_return;
	}
	
	private int[] addSpansNoEndLimit(Node _nodeToModify, int _indice_start, int _nb_span_annotation, String _span_style , String _annotation_content, String _annotation_id, TagNode _endSpan)
	{
		//System.out.println("[addSpansNoEndLimit]");
		//récupération des noeuds fils du noeud à modifier
		NodeList childrenOfNodeToModify = _nodeToModify.getChildren() ;
		if(childrenOfNodeToModify!=null)
		{
			//System.out.println("[addSpansNoEndLimit] has children");
			int nb_children = childrenOfNodeToModify.size() ;
			if(nb_children > 0)//Si le noeud n'a pas au moins un fils TextNode, il n'y a rien à faire.
			{
				if(childrenOfNodeToModify.size() == 1 && childrenOfNodeToModify.elementAt(0) instanceof TextNode)//S'il n'y a que du texte dans le noeud, le traitement est simple
				{
					//System.out.println("addSpansNoEndLimit : only textNode");
					TextNode content_textnode = (TextNode)childrenOfNodeToModify.elementAt(0) ;
					String toModifyContent = content_textnode.getText() ;					
					int to_modify_content_length = toModifyContent.length();
					//vérification que les indices sont compatibles, sinon on met 0 et length en valeurs
					if(_indice_start > to_modify_content_length) _indice_start = 0 ;
					NodeList newChildrenList = new NodeList();
					if(_nb_span_annotation >0 ) createSpanAndBefore(toModifyContent , _indice_start, to_modify_content_length, newChildrenList, _span_style , _annotation_content, ""+ _annotation_id +"-"+_nb_span_annotation , _endSpan);
					else createSpanAndBefore(toModifyContent , _indice_start, to_modify_content_length, newChildrenList, _span_style , _annotation_content, ""+ _annotation_id , _endSpan);
					_nodeToModify.setChildren(newChildrenList);
					//System.out.println("addSpansNoEndLimit new content : " + _nodeToModify.toHtml());
					_nb_span_annotation ++ ;
				}
				else
				{//il y a d'autres fils, pas qu'un noeud texte.
					//parcours de tous les fils
					int[] actual_state = this.addSpansProcessChildrenNoEndLimit(_nodeToModify, _indice_start, _nb_span_annotation, _span_style, _annotation_content, _annotation_id, _endSpan);
					_indice_start = actual_state[0];
					_nb_span_annotation = actual_state[1]; 
				}
			}
		}
		else if(_nodeToModify instanceof TextNode)//en fait il n'y a que ce noeud à modifier
		{
			//System.out.println("[addSpansNoEndLimit] is TextNode");
			String toModifyContent = ((TextNode)_nodeToModify).getText();
			int to_modify_content_length = toModifyContent.length();
			//vérification que les indices sont compatibles, sinon on met 0 et length en valeurs
			if(_indice_start > to_modify_content_length) _indice_start = 0 ;
			NodeList newChildrenList = new NodeList();
			if(_nb_span_annotation >0 ) createSpanAndBefore(toModifyContent , _indice_start, to_modify_content_length, newChildrenList, _span_style , _annotation_content, ""+ _annotation_id +"-"+_nb_span_annotation , _endSpan);
			else createSpanAndBefore(toModifyContent , _indice_start, to_modify_content_length, newChildrenList, _span_style , _annotation_content, ""+ _annotation_id , _endSpan);
			_nodeToModify.setChildren(newChildrenList);
			_nb_span_annotation ++ ;
		}
		int[] to_return = {_indice_start , _nb_span_annotation};
		return to_return;
	}
	
	private int[] addSpansAllChildren(Node _nodeToModify , int _start_indice, int _nb_span_annotation, String _span_style, String _annotation_content, String _annotation_id, TagNode _endSpan)
	{
		NodeList childrenOfNodeToModify = _nodeToModify.getChildren();
		if(childrenOfNodeToModify != null)
		{
			NodeList toModifyNewChildren = new NodeList();
			for(int cptchildren = 0 ; cptchildren < childrenOfNodeToModify.size() ; cptchildren ++)
			{
				Node current_child = childrenOfNodeToModify.elementAt(cptchildren);
				//SI c'est un textNode, on colore
				if(current_child instanceof TextNode)
				{
					int[] actual_state = createSpanInTextNode((TextNode)current_child, cptchildren, childrenOfNodeToModify, toModifyNewChildren, _start_indice, ((TextNode)current_child).getText().length(), _nb_span_annotation, _span_style, _annotation_content, _annotation_id, _endSpan);
					_start_indice = 0 ; //on colore l'intégralité pour les autres noeuds
					_nb_span_annotation = actual_state[2] ;
				}
				//TODO modifier ce comportement, descendre jusqu'aux Textnode et les encadrer de span annotation
				//SI c'est autre chose, rien à modifier, on ajoute le noeud tel quel, du coup quand on annote avec un lien dans l'annotation, le lien n'est pas surligné...
				else toModifyNewChildren.add(current_child);// ne pas oublier de conserver les noeuds non annotés
			}
			_nodeToModify.setChildren(toModifyNewChildren);
		}
		/*else
		{
			if(_nodeToModify instanceof TextNode)
			{
				String content = _nodeToModify.getText();
				NodeList newChildrenList = new NodeList();
				createSpanAndBefore(content, _start_indice, content.length(), newChildrenList, _span_style, _annotation_content, ""+_annotation_id, _endSpan);
				_nodeToModify.setChildren(newChildrenList);
				_nb_span_annotation ++ ;
			}
		}*/
		int[] to_return = {_start_indice , _nb_span_annotation};
		return to_return ;
	}
	
	private int[] addSpansProcessChildren( Node _nodeToModify, int _start_indice, int _end_indice, int _nb_span_annotation, String _span_style, String _annotation_content, String _annotation_id, TagNode _endSpan)
	{
		int already_ended = 0 ;
		NodeList childrenOfNodeToModify = _nodeToModify.getChildren();
		if(childrenOfNodeToModify != null)
		{
			NodeList toModifyNewChildren = new NodeList();
			for(int cptchildren = 0 ; cptchildren < childrenOfNodeToModify.size() ; cptchildren ++)
			{
				Node current_child = childrenOfNodeToModify.elementAt(cptchildren);
				//SI c'est un textNode, on colore
				if(current_child instanceof TextNode)
				{
					int[] actual_state = createSpanInTextNode((TextNode)current_child, cptchildren, childrenOfNodeToModify, toModifyNewChildren, _start_indice, _end_indice, _nb_span_annotation, _span_style, _annotation_content, _annotation_id, _endSpan);
					_start_indice = actual_state[0];
					_end_indice = actual_state[1];
					_nb_span_annotation = actual_state[2];
					already_ended = actual_state[3];
					if(already_ended == -1) cptchildren = childrenOfNodeToModify.size();
				}
				//SI c'est une SPAN annotation on va devoir faire une récursion sur son contenu
				else if(current_child instanceof Span && ((Span)current_child).getAttribute("class") != null && ((Span)current_child).getAttribute("class").equalsIgnoreCase("annotation"))
				{
					int[] actual_state = addSpansProcessChildren(current_child, _start_indice, _end_indice, _nb_span_annotation, _span_style, _annotation_content, _annotation_id, _endSpan);
					_start_indice = actual_state[0];
					_end_indice = actual_state[1];
					_nb_span_annotation = actual_state[2];
					already_ended = actual_state[3];
					toModifyNewChildren.add(current_child);
					if(already_ended == -1)
					{
						//ajouter tous les noeuds restant
						for(int i = cptchildren + 1 ; i < childrenOfNodeToModify.size() ; i ++)
						{
							toModifyNewChildren.add(childrenOfNodeToModify.elementAt(i));
						}
						//mettre fin au parcours
						cptchildren = childrenOfNodeToModify.size();
					}
				}
				//TODO modifier ce comportement, descendre jusqu'aux Textnode et les encadrer de span annotation
				//SI c'est autre chose, rien à modifier, on ajoute le noeud tel quel, du coup quand on annote avec un lien dans l'annotation, le lien n'est pas surligné...
				else toModifyNewChildren.add(current_child);// ne pas oublier de conserver les noeuds non annotés
			}
			_nodeToModify.setChildren(toModifyNewChildren);
		}
		int[] to_return = {_start_indice , _end_indice , _nb_span_annotation , already_ended};
		return to_return ;
	}
	
	private int[] addSpansProcessChildrenNoEndLimit( Node _nodeToModify, int _start_indice, int _nb_span_annotation, String _span_style, String _annotation_content, String _annotation_id, TagNode _endSpan)
	{
		//int already_ended = 0 ;
		NodeList childrenOfNodeToModify = _nodeToModify.getChildren();
		if(childrenOfNodeToModify != null)
		{
			NodeList toModifyNewChildren = new NodeList();
			for(int cptchildren = 0 ; cptchildren < childrenOfNodeToModify.size() ; cptchildren ++)
			{
				Node current_child = childrenOfNodeToModify.elementAt(cptchildren);
				//SI c'est un textNode, on colore
				if(current_child instanceof TextNode)
				{
					int[] actual_state = createSpanInTextNode((TextNode)current_child, cptchildren, childrenOfNodeToModify, toModifyNewChildren, _start_indice, ((TextNode)current_child).getText().length(), _nb_span_annotation, _span_style, _annotation_content, _annotation_id, _endSpan);
					_start_indice = actual_state[0];
					//_end_indice = actual_state[1];
					_nb_span_annotation = actual_state[2];
					//already_ended = actual_state[3];
					//if(already_ended == -1) cptchildren = childrenOfNodeToModify.size();
				}
				//SI c'est une SPAN annotation on va devoir faire une récursion sur son contenu
				/*else if(current_child instanceof Span && ((Span)current_child).getAttribute("class").equalsIgnoreCase("annotation"))
				{
					int[] actual_state = addSpansProcessChildren(current_child, _start_indice, _end_indice, _nb_span_annotation, _span_style, _annotation_content, _annotation_id, _endSpan);
					_start_indice = actual_state[0];
					_end_indice = actual_state[1];
					_nb_span_annotation = actual_state[2];
					already_ended = actual_state[3];
					toModifyNewChildren.add(current_child);
					if(already_ended == -1)
					{
						//ajouter tous les noeuds restant
						for(int i = cptchildren + 1 ; i < childrenOfNodeToModify.size() ; i ++)
						{
							toModifyNewChildren.add(childrenOfNodeToModify.elementAt(i));
						}
						//mettre fin au parcours
						cptchildren = childrenOfNodeToModify.size();
					}
				}*/
				//TODO modifier ce comportement, descendre jusqu'aux Textnode et les encadrer de span annotation
				//SI c'est autre chose, rien à modifier, on ajoute le noeud tel quel, du coup quand on annote avec un lien dans l'annotation, le lien n'est pas surligné...
				else toModifyNewChildren.add(current_child);// ne pas oublier de conserver les noeuds non annotés
			}
			_nodeToModify.setChildren(toModifyNewChildren);
		}
		//int[] to_return = {_start_indice , _end_indice , _nb_span_annotation , already_ended};
		int[] to_return = {_start_indice , _nb_span_annotation };
		return to_return ;
	}
	
	private int[] createSpanInTextNode(
			TextNode _textNodeToProcess, 
			int _cptchildrenOfNodeToModifyAlreadyProcessed, 
			NodeList _childrenOfNodeToModify, 
			NodeList _newChildrenOfNodeToModify, 
			int _start_indice, int _end_indice, int _nb_span_annotation, 
			String _span_style, String _annotation_content, String _annotation_id, TagNode _endSpan)
	{
		int end = 0 ;
		String text_content = _textNodeToProcess.getText() ;
		int length_text_content = text_content.length() ;
		if(length_text_content < _start_indice) //on est avant l'annotation
		{
			_start_indice = _start_indice - length_text_content ;
			_end_indice = _end_indice - length_text_content ;
			_newChildrenOfNodeToModify.add(_textNodeToProcess);
		}
		else if(length_text_content >= _start_indice && length_text_content > _end_indice)//toute l'annotation est dans ce noeud texte
		{
			//créer l'annotation
			if(_nb_span_annotation > 0) 
			{
				this.createSpanAndSurrounding(text_content, _start_indice, _end_indice, text_content.length(), _newChildrenOfNodeToModify, _span_style, _annotation_content, _annotation_id + "-" + _nb_span_annotation, _endSpan);
			}
			else this.createSpanAndSurrounding(text_content, _start_indice, _end_indice, text_content.length(), _newChildrenOfNodeToModify, _span_style, _annotation_content, ""+ _annotation_id, _endSpan);
			//mettre fin au parcours de fils puisque l'annotation a été créée, mais ne pas oublier d'ajouter tout le reste des noeuds non parcourus
			for(int children_unchecked = _cptchildrenOfNodeToModifyAlreadyProcessed + 1 ; children_unchecked < _childrenOfNodeToModify.size() ; children_unchecked ++)
			{
				_newChildrenOfNodeToModify.add(_childrenOfNodeToModify.elementAt(children_unchecked));
			}
			_cptchildrenOfNodeToModifyAlreadyProcessed = _childrenOfNodeToModify.size() ;
			end = -1 ;
		}
		else if(length_text_content >= _start_indice)//le début de l'annotation est dans ce noeud texte mais la fin est dans un autre noeud
		{
			//il va falloir créer plusieurs annotations
			createSpanAndBefore(text_content, _start_indice, text_content.length(), _newChildrenOfNodeToModify, _span_style, _annotation_content, ""+_annotation_id + "-" + _nb_span_annotation, _endSpan);
			_nb_span_annotation ++ ;
			_start_indice = 0 ;
			_end_indice = _end_indice - length_text_content ;
		}
		int[] to_return = {_start_indice ,  _end_indice, _nb_span_annotation, end};
		return to_return ;
	}
}