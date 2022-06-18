/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.uci.ics.crawler4j.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlMapper;
import org.apache.tika.parser.html.HtmlParser;


//import org.htmlparser.Parser;
import org.htmlparser.visitors.TagFindingVisitor;

import org.htmlparser.tags.FormTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.Node;
import org.htmlparser.tags.ScriptTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import navex.HTMLForm;
import edu.uci.ics.crawler4j.crawler.Configurable;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.exceptions.ParseException;
import edu.uci.ics.crawler4j.url.URLCanonicalizer;
import edu.uci.ics.crawler4j.url.WebURL;
import edu.uci.ics.crawler4j.util.Net;
import edu.uci.ics.crawler4j.util.Util;


import org.htmlparser.Parser.*;
import org.htmlparser.filters.TagNameFilter;

/**
 * @author Yasser Ganjisaffar
 * modified by Abeer Alhuzali and NoTamper
 */
public class Parser extends Configurable {

    protected static final Logger logger = LoggerFactory.getLogger(Parser.class);

    private final HtmlParser htmlParser;
    private final ParseContext parseContext;

    public Parser(CrawlConfig config) throws InstantiationException, IllegalAccessException {
        super(config);
        htmlParser = new HtmlParser();
        parseContext = new ParseContext();
        parseContext.set(HtmlMapper.class, AllTagMapper.class.newInstance());
    }

    public void parse(Page page, String contextURL)
        throws NotAllowedContentException, ParseException {
        if (Util.hasBinaryContent(page.getContentType())) { // BINARY
            BinaryParseData parseData = new BinaryParseData();
            if (config.isIncludeBinaryContentInCrawling()) {
                if (config.isProcessBinaryContentInCrawling()) {
                    parseData.setBinaryContent(page.getContentData());
                } else {
                    parseData.setHtml("<html></html>");
                }
                page.setParseData(parseData);
                if (parseData.getHtml() == null) {
                    throw new ParseException();
                }
                parseData.setOutgoingUrls(Net.extractUrls(parseData.getHtml()));
            } else {
                throw new NotAllowedContentException();
            }
        } else if (Util.hasPlainTextContent(page.getContentType())) { // plain Text
            try {
                TextParseData parseData = new TextParseData();
                if (page.getContentCharset() == null) {
                    parseData.setTextContent(new String(page.getContentData()));
                } else {
                    parseData.setTextContent(
                        new String(page.getContentData(), page.getContentCharset()));
                }
                parseData.setOutgoingUrls(Net.extractUrls(parseData.getTextContent()));
                page.setParseData(parseData);
            } catch (Exception e) {
                logger.error("{}, while parsing: {}", e.getMessage(), page.getWebURL().getURL());
                throw new ParseException();
            }
        } else { // isHTML
            Metadata metadata = new Metadata();
            HtmlContentHandler contentHandler = new HtmlContentHandler();
            try (InputStream inputStream = new ByteArrayInputStream(page.getContentData())) {
                htmlParser.parse(inputStream, contentHandler, metadata, parseContext);
            } catch (Exception e) {
                logger.error("{}, while parsing: {}", e.getMessage(), page.getWebURL().getURL());
                throw new ParseException();
            }

            if (page.getContentCharset() == null) {
                page.setContentCharset(metadata.get("Content-Encoding"));
            }

            HtmlParseData parseData = new HtmlParseData();
            parseData.setText(contentHandler.getBodyText().trim());
        
            parseData.setTitle(metadata.get(DublinCore.TITLE));
            parseData.setMetaTags(contentHandler.getMetaTags());
            // Please note that identifying language takes less than 10 milliseconds
            LanguageIdentifier languageIdentifier = new LanguageIdentifier(parseData.getText());
            page.setLanguage(languageIdentifier.getLanguage());

            Set<WebURL> outgoingUrls = new HashSet<>();

            String baseURL = contentHandler.getBaseUrl();
            if (baseURL != null) {
                contextURL = baseURL;
            }
          
            

            int urlCount = 0;
            for (ExtractedUrlAnchorPair urlAnchorPair : contentHandler.getOutgoingUrls()) {

                String href = urlAnchorPair.getHref();
                if ((href == null) || href.trim().isEmpty()) {
                    continue;
                }

                String hrefLoweredCase = href.trim().toLowerCase();
                if (!hrefLoweredCase.contains("javascript:") &&
                    !hrefLoweredCase.contains("mailto:") && !hrefLoweredCase.contains("@")) {
                    String url = URLCanonicalizer.getCanonicalURL(href, contextURL);
                    System.out.println("THE URL IS "+url);
                    if (url != null) {
                        WebURL webURL = new WebURL();
                        webURL.setURL(url);
                        webURL.setTag(urlAnchorPair.getTag());
                        webURL.setAnchor(urlAnchorPair.getAnchor());
                        outgoingUrls.add(webURL);
                        urlCount++;
                        if (urlCount > config.getMaxOutgoingLinksToFollow()) {
                            break;
                        }
                    }
                }
            }
            parseData.setOutgoingUrls(outgoingUrls);

            try {
                if (page.getContentCharset() == null) {
                    parseData.setHtml(new String(page.getContentData()));
                } else {
                    parseData.setHtml(new String(page.getContentData(), page.getContentCharset()));
                }

              
            } catch (UnsupportedEncodingException e) {
                logger.error("error parsing the html: " + page.getWebURL().getURL(), e);
                throw new ParseException();
            }
            
            //navex
            Set<HTMLForm> forms =null;
 		try {
 			forms = parseForm(contextURL , parseData.getHtml());
 			 if (forms != null)
 	        	   parseData.setForms(forms);
 	            //end 
 		} catch (Exception e1) {
 			// TODO Auto-generated catch block
 			e1.printStackTrace();
 		}
           
 		      page.setParseData(parseData);   
        }
    }
   // we will invoke org.htmlparser to parse the page again to get the fields 
    //not elegant 
	private Set<HTMLForm> parseForm(String url, String pageContent)  throws Exception {
		Set<HTMLForm> forms = new HashSet<HTMLForm>();
		 Node[] formNodes ;
		try {
			
			
			System.out.println(" fetching the page - " + url);
			org.htmlparser.Parser parser = new org.htmlparser.Parser(pageContent);
		    String[] formTag = { "form", "script"};
			TagFindingVisitor tfv = new TagFindingVisitor(formTag);
			 parser.visitAllNodesWith(tfv);
			formNodes = tfv.getTags(0);
			Node[] scripts = tfv.getTags(1);
			
			// extract JavaScript common to all the forms
			// this also includes inlining of external scripts
			// by fetching them using the src attribute
			String commonJS = getCommonJS(scripts);
			
			for (Node form : formNodes){
				FormTag f = (FormTag)form;
				//forms where the action has .com , we ignore that form 
				if (f.getFormLocation().contains(".com"))
					continue;
				HTMLForm hf = new HTMLForm (f, url, commonJS);
				
				String action = f.getFormLocation();
				if (action.isEmpty() || action == null){
					f.setFormLocation(hf.getUrl());
				}
				hf.setForm(f);
				//create formulas from the form and save it...
				
				//extraHf: in some forms there are more than one submit bottons
				//in that case, we have to create a copy of that form with one submit 
				//botton value
				hf.simulateJSValidation();
				HashSet<HTMLForm> extraHF  = hf.processInputsForZ3();
				
				System.out.println("FORM is "+hf.toString());
				forms.add(hf);
				if (extraHF != null ){
					System.out.println("......Added Extra Forms : "+ extraHF.size());
					forms.addAll(extraHF);
				}
			}
		} catch (Exception e) {
			System.out.println("Exception while fetching page - "
					+ e.getMessage());
			e.printStackTrace();
		}
		
		return forms;
	}
	
	public String getCommonJS(Node[] scripts) throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("\n // Script Tag Contents\n");

		for (Node n : scripts) {
			ScriptTag script = (ScriptTag) n;
			String type = script.getType();

			if (type != null
					&& !script.getType().equalsIgnoreCase("text/javascript"))
				continue;

			// get the source code if it is an external script
			sb.append(getScriptBody(script)).append("\n");
		}

		logger.debug( " Common JS for file - "
						+ " is - " + sb);
		return sb.toString();
	}
	
	private String getScriptBody(ScriptTag script) throws Exception {
		String body = "";

		String url = script.getAttribute("src");
		if (url != null) {
			// external script : fetch
			
			// the narcissus engine is failing to parse this 
			// it is not relevant to form validation, which is 
			// processed normally...
			if(url.indexOf("js/wz_dragdrop.js") >= 0){
				return "";
			}

			
		} else {
			body = script.getScriptCode().replace("<!--", "// ") + "\n";
		}

		return body;
	}

}
