package jibiki.fr.shishito.Util;

import android.util.Log;
import android.util.Xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import jibiki.fr.shishito.Dictionary;
import jibiki.fr.shishito.ListEntry;

import org.apache.commons.io.IOUtils;

/**
 * Created by tibo on 17/09/15.
 */
public final class XMLUtils {

    private static final String ns = null;
    private static final String TAG = XMLUtils.class.getSimpleName();

    protected static HashMap<String, String> newoldTagMap = new java.util.HashMap<>();
    protected static HashMap<String, String> oldnewTagMap = new java.util.HashMap<>();


    private XMLUtils() {
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    public static ArrayList<ListEntry> parseEntryList(InputStream stream, Dictionary dict) throws IOException, XmlPullParserException, XPathExpressionException, SAXException, ParserConfigurationException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer);
        String string = writer.toString();
        string = replaceTags(string, newoldTagMap, oldnewTagMap);
        //Log.d(TAG, source);

        NamespaceContext context = new NamespaceContextMap(
                "d", "http://www-clips.imag.fr/geta/services/dml",
                "xslt", "http://bar",
                "def", "http://def");

        org.xml.sax.InputSource source = new org.xml.sax.InputSource(new java.io.StringReader(string));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(source);

        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        xPath.setNamespaceContext(context);
        ArrayList<ListEntry> entries = new ArrayList<ListEntry>();
        String xpath = ((String) dict.getElements().get("cdm-entry"));
        xpath = replaceXpathstring(xpath, oldnewTagMap);
        NodeList shows = (NodeList) xPath.evaluate(xpath, document, XPathConstants.NODESET);
        for (int i = 0; i < shows.getLength(); i++) {
            Element show = (Element) shows.item(i);
            ListEntry entry = new ListEntry();
            xpath = ((String) dict.getElements().get("cdm-headword"));
            xpath = replaceXpathstring(xpath, oldnewTagMap);
            entry.setKanji(xPath.evaluate(xpath, show));
            xpath =  ((String) dict.getElements().get("cdm-writing"));
            xpath = replaceXpathstring(xpath, oldnewTagMap);
            entry.setHiragana(xPath.evaluate(xpath, show));
            xpath = ((String) dict.getElements().get("cdm-reading"));
            xpath = replaceXpathstring(xpath, oldnewTagMap);
            entry.setRomanji(xPath.evaluate(xpath, show));
            entries.add(entry);
        }

        return entries;
    }

    public static Dictionary createDictionary(InputStream stream) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(stream, "UTF-8");
        Dictionary dict = new Dictionary();

        //Skip the start of document
        parser.next();
        parser.require(XmlPullParser.START_TAG, ns, "volume-metadata-files");
        parser.next();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            } else {
                String name = parser.getName();
                Log.d(TAG, parser.getName());
                // Starts by looking for the entry tag
                if (name.equals("authors")) {
                    dict.setAuthors(parser.getText());
                    skip(parser);
                } else if (name.equals(("cdm-elements"))) {
                    while (parser.next() != XmlPullParser.END_TAG) {
                        String xPath = "";
                        if ((xPath = parser.getAttributeValue(null, "xpath")) != null) {
                            dict.getElements().put(parser.getName(), xPath);
                            Log.d(TAG, (String) dict.getElements().get(parser.getName()));
                            parser.next();
                        }
                    }
                } else {
                    Log.d(TAG, "HEYHEYEHHH");
                    skip(parser);
                }
            }
        }
        return dict;
    }

    protected static String replaceTags(String oldXml,  java.util.HashMap<String, String> newTagMap,  java.util.HashMap<String, String> oldTagMap) {
        String newXml = oldXml;

        java.util.regex.Pattern regex = 	java.util.regex.Pattern.compile("<([^\\/\\s\\\\?>:]+:)?([^\\/\\s\\?:>]+)", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = regex.matcher(newXml);

        int numtags = 1;
        while(m.find()) {
            String oldTagname = m.group(2);
            String prefix = m.group(1);
            if(prefix == null) {
                prefix = "";
            }
            String newTagname = "a" + numtags++;
            newoldTagMap.put(newTagname,oldTagname);
            oldTagMap.put(oldTagname,newTagname);
            //System.out.println("old:" + oldTagname + " new:"  +newTagname + " pref:" + prefix);
            newXml = newXml.replaceAll("<" + prefix + oldTagname + "\\s", "<" + prefix + newTagname + " ");
            newXml = newXml.replaceAll("<" + prefix + oldTagname + ">", "<" + prefix + newTagname + ">");
            newXml = newXml.replaceAll("</" + prefix + oldTagname + ">", "</" + prefix + newTagname+ ">");
        }

        return newXml;
    }

    protected static String replaceXpathstring(String xpathString, java.util.HashMap<String, String> theTagMap) {
        java.util.regex.Pattern regexXpath = 	java.util.regex.Pattern.compile("/([^\\/\\s\\[:]+:)?([^\\[\\/\\s:]+)", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher mXpath = regexXpath.matcher(xpathString);
        String newXpathString = xpathString;

        if(newXpathString.contains("/volume")){
            newXpathString = newXpathString.replace("/volume", " /d:entry-list/d:entry/volume/d:contribution/d:data");
        }

        while(mXpath.find()) {
            String oldTagname = mXpath.group(2);
            String prefix = mXpath.group(1);
            if(prefix == null) {
                prefix = "";
            }
            String newTagname = theTagMap.get(oldTagname);
            //System.out.println("old:" + oldTagname + " new:"  +newTagname + " pref:" + prefix);
            newXpathString = newXpathString.replaceAll("/" + prefix + oldTagname + "$", "/" + prefix + newTagname);
            newXpathString = newXpathString.replaceAll("/" + prefix + oldTagname + "/", "/" + prefix + newTagname + "/");
            newXpathString = newXpathString.replaceAll("/" + prefix + oldTagname + "\\[", "/" + prefix + newTagname + "\\[");
        }

        return newXpathString;
    }

}
