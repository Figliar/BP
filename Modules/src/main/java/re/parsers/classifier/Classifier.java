package re.parsers.classifier;


import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.csv.TextAndCSVParser;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.ToTextContentHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import re.parsers.Util_functions;
import re.parsers.config.ReConfig;
import re.parsers.service.ReParser;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.MissingFormatArgumentException;
import java.util.ServiceLoader;

/**
 * Class containing methods for document classification
 */
public class Classifier {

    /**
     * @param handler text content of the document to classify
     * @param metadata metadata of the document to classify
     * @param type MIME type of the document (precisely indicator for rules.json file)
     * @param rules path to rules.json file
     * @return parser name when successful and null if not
     */
    public String decideJSON(String handler, Metadata metadata, String type, org.json.JSONObject rules){
        JSONParser parser = new JSONParser();
        try {
            // A JSON object. Key value pairs are unordered. JSONObject supports java.util.Map interface.
            org.json.simple.JSONObject tree = (JSONObject) parser.parse(rules.get(type).toString());
//            org.json.simple.JSONObject tree = (org.json.simple.JSONObject) rules.get(type);
            for (Object o : tree.keySet()) {
                JSONObject text = (JSONObject) tree.get(o);
                if (checkTextMetadata(text, handler, metadata)){
                    return o.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param jo JSON object from rules.json file
     * @param handler text content of the document to classify
     * @param metadata metadata of the document to classify
     * @return true if handler and metadata contain values in jo, else false
     */
    private boolean checkTextMetadata(JSONObject jo, String handler, Metadata metadata){
        int i = 0;
        JSONArray textList = (JSONArray) jo.get("text");
        if(textList != null) {
            for (Object element :
                    textList) {
                if (!handler.contains(element.toString().trim()))
                    break;
                else
                    i++;
                if (textList.size() == i) {
                    JSONObject metadataList = (JSONObject) jo.get("metadata");
                    return checkMetadata(metadataList, metadata);
                }
            }
        }
        return false;
    }


    /**
     * @param metadataList JSON object containing metadata keys and values
     * @param metadata metadata of the document to be parsed
     * @return true if metadata contains values from metadataList, else false
     */
    private boolean checkMetadata(JSONObject metadataList, Metadata metadata) {
        if(metadataList != null) {
            int j = 0;
            for (Object meta :
                    metadataList.keySet()) {
                if (Arrays.toString(metadata.names()).contains(meta.toString())){
                    if(metadataList.get(meta.toString()).getClass().equals(JSONArray.class)){
                        JSONArray temp = (JSONArray) metadataList.get(meta.toString());
                        boolean truthy = false;
                        for (Object o : temp) {
                            if (metadata.get(meta.toString()).equals(o.toString())) {
                                truthy = true;
                            }
                        }
                        if(truthy)
                            j++;
                        else
                            return false;
                    }else if(metadataList.get(meta).getClass().equals(String.class)){
                        if (!metadata.get(meta.toString()).equals(metadataList.get(meta).toString().trim())) {
                            return false;
                        } else
                            j++;
                    }
                }
                else
                    return false;
                if(metadataList.size() == j){
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * Deprecated
     * @param handler text content of the document to classify
     * @param metadata metadata of the document to classify
     * @return parser name when successful and null if not
     * @throws IOException
     */
    public String decideXML(String handler, Metadata metadata) throws IOException {
        File xml = new File("/home/rene/Desktop/Bakalarka/ReFormator" +
                "/extension/Modules/src/main/resources/rules.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        String p = "";
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xml);
            doc.getDocumentElement().normalize();

            NodeList parserList = doc.getElementsByTagName("parser");
            for (int temp = 0; temp < parserList.getLength(); temp++) {

                Node parser = parserList.item(temp);
                Element pElement = (Element) parser;
                boolean text = false;
                boolean meta = false;
                if (parser.getNodeType() == Node.ELEMENT_NODE) {
                    NodeList tList = pElement.getChildNodes();
                    for (int i = 0; i < tList.getLength(); i++) {
                        Node tNode = tList.item(i);
                        if (tNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element tElement = (Element) tNode;
                            if (tElement.getTagName().equals("text")) {
                                NodeList elementList = tNode.getChildNodes();
                                for (int j = 0; j < elementList.getLength(); j++) {
                                    Node element = elementList.item(j);
                                    if (element.getNodeType() == Node.ELEMENT_NODE) {
                                        Element eElement = (Element) element;
                                        if(!handler.contains(eElement.getAttribute("value").trim())) {
                                            break;
                                        }
                                        if(j == (elementList.getLength() - 2)){
                                            p = pElement.getAttribute("id");
                                            text = true;
                                        }
                                    }

                                }
                            }
                            else if (tElement.getTagName().equals("metadata") && text) {
                                NodeList elementList = tElement.getChildNodes();
                                if(elementList.getLength() == 1)
                                    meta = true;
                                else {
                                    for (int j = 0; j < elementList.getLength(); j++) {
                                        Node element = elementList.item(j);
                                        if (element.getNodeType() == Node.ELEMENT_NODE) {
                                            Element eElement = (Element) element;
                                            if (Arrays.toString(metadata.names()).contains(eElement.getAttribute("key"))) {
                                                NodeList metaElements = eElement.getChildNodes();
                                                if(metaElements.getLength() > 1) {
                                                    for (int x = 0; x < metaElements.getLength(); x++) {
                                                        Node metaElement = metaElements.item(x);
                                                        if (metaElement.getNodeType() == Node.ELEMENT_NODE) {
                                                            Element mElement = (Element) metaElement;
                                                            meta = metadata.get(eElement.getAttribute("key")).equals(mElement.getAttribute("value"));
                                                            if (meta)
                                                                break;
                                                        }
                                                    }
                                                }
                                                else{
                                                    meta = metadata.get(eElement.getAttribute("key")).equals(eElement.getAttribute("value"));
                                                }
                                            }
                                        }
                                    }
                                }
                                if(meta)
                                    return p;
                            }
                        }
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void speed_test() throws IOException, SAXException, TikaException, MissingFormatArgumentException {
        File file = new File("/home/rene/Desktop/Bakalarka/ReFormator/extension/Modules/src/test/resources/test-data/Agilent/1.pdf");
        FileInputStream inputStream = new FileInputStream(file);
        PDFParser pdfParser = new PDFParser();
        Metadata metadata = new Metadata();
        ContentHandler handler = new ToTextContentHandler();
        pdfParser.parse(inputStream, handler, metadata, new ParseContext());
        String type = "";
        int ITER = 100000;

        Classifier classifier = new Classifier();

        long xml = 0;
        long startTime = System.nanoTime();
        for (int count = 0; count < ITER; count++) {
            type = classifier.decideXML(handler.toString(), metadata);
        }
        System.out.println(type);
        long endTime = System.nanoTime();
        xml = endTime - startTime;

        System.out.println("XML pre " + ITER + " rozhodnutí");
        System.out.println(xml/1000000 + " ms");
        System.out.println((double)xml/ 1_000_000_000 + " s");

        long json = 0;
        startTime = System.nanoTime();
        for (int count = 0; count < ITER; count++){
            type = classifier.decideJSON(handler.toString(), metadata, "pdf", new ReConfig().getRules());
        }
        System.out.println("\n" + type);
        endTime = System.nanoTime();
        json = endTime - startTime;

        System.out.println("JSON pre " + ITER + " rozhodnutí");
        System.out.println(json/1000000 + " ms");
        System.out.println((double)json/ 1_000_000_000 + " s");

        System.out.printf("XML je %.1f x pomalsie ako JSON\n", (double)xml/ 1_000_000_000 / ((double)json/ 1_000_000_000));
        System.out.printf("\n\nXML priemerne na jedno rozhodnutie: %.4f ms\n",
                ((double)xml/ 1_000_000 /ITER));
        System.out.printf("JSON priemerne na jedno rozhodnutie: %.4f ms\n",
                ((double)json/(1_000_000)/ITER));
    }

    public static void test(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context, String type){
        ServiceLoader<ReParser> loader = ServiceLoader.load(ReParser.class);

        int ITER = 100_000;
        long startTime = System.nanoTime();
        for (int i = 0; i < ITER; i++) {
            loader.forEach(l -> {
                try {
                    if (l.getType().equals(type)) {
                        l.parse(stream, handler, metadata);
                    }
                } catch (IOException | TikaException | SAXException e) {
                    e.printStackTrace();
                }
            });
        }
        long endTime = System.nanoTime();
        long diff = endTime - startTime;
        startTime = System.nanoTime();
        for (int i = 0; i < ITER; i++) {
            for(ReParser sp : loader){
                try {
                    if (sp.getType().equals(type)) {
                        sp.parse(stream, handler, metadata);
                        break;
                    }
                }
                catch (IOException | TikaException | SAXException e) {
                    e.printStackTrace();
                }
            }
        }
        endTime = System.nanoTime();
        long diff2 = endTime - startTime;
        startTime = System.nanoTime();
        Iterator<ReParser> iter = loader.iterator();
        for (int i = 0; i < ITER; i++) {
            while(iter.hasNext()){
                try {
                    ReParser p = iter.next();
                    if (p.getType().equals(type)) {
                        p.parse(stream, handler, metadata);
                        break;
                    }
                }
                catch (IOException | TikaException | SAXException e) {
                    e.printStackTrace();
                }
            }
        }
        endTime = System.nanoTime();
        long diff3 = endTime - startTime;
        startTime = System.nanoTime();
        for (int i = 0; i < ITER; i++) {
            loader.stream().filter(l -> l.toString().equals(type)).forEach(l -> {
                try {
                    l.get().parse(stream, handler, metadata);
                } catch (IOException | TikaException | SAXException e) {
                    e.printStackTrace();
                }
            });
        }
        endTime = System.nanoTime();
        long diff4 = endTime - startTime;


        System.out.println("Pre " + ITER + " iterácií:");
        System.out.printf("%.4f sek loader.forEach()\n", ((double)diff/1000000000));
        System.out.printf("%.4f sek for(ServiceProvider sp : loader)\n", ((double)diff2/1000000000));
        System.out.printf("%.4f sek iter = loader.iterator => while(iter.hasNext())\n", ((double)diff3/1000000000));
        System.out.printf("%.4f sek loader.stream().filter(l -> l.toString().equals(type)).forEach(l -> {\n", ((double)diff4/1000000000));
    }

    public static void main(String[] args) throws IOException, TikaException, SAXException, ParseException {

        File file = new File("Modules/src/test/resources" +
                "/test-data/Agilent/107171-56-2020-05-13 107171.pdf");

        FileInputStream inputStream = new FileInputStream(file);

        PDFParser pdfParser = new PDFParser();
        TextAndCSVParser textAndCSVParser = new TextAndCSVParser();
        OOXMLParser ooxmlParser = new OOXMLParser();

        ContentHandler handler = new ToTextContentHandler();
        Metadata metadata = new Metadata();
        ParseContext ps = new ParseContext();

        pdfParser.parse(inputStream, handler, metadata, ps);
//        textAndCSVParser.parse(inputStream, handler, metadata, ps);
//        byte[] buffer = new byte[16000];
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        int len;
//        while ((len = inputStream.read(buffer)) > - 1) {
//            outputStream.write(buffer, 0, len);
//        }
//        outputStream.flush();
//        InputStream in1 = new ByteArrayInputStream(outputStream.toByteArray());
//        InputStream in2 = new ByteArrayInputStream(outputStream.toByteArray());

//        ooxmlParser.parse(in1, handler, metadata, ps);

        ReConfig reConfig = new ReConfig();
        Util_functions.validateRules(reConfig);
        Classifier classifier = new Classifier();
        String type = classifier.decideJSON(handler.toString(), metadata, "pdf", new ReConfig().getRules());
        ServiceLoader<ReParser> loader = ServiceLoader.load(ReParser.class);
        Iterator<ReParser> iter = loader.iterator();

        while(iter.hasNext()){
            try {
                ReParser p = iter.next();
                if (p.getType().equals(type)) {
                    p.parse(inputStream, handler, metadata);
//                    p.parse(in2, handler, metadata);
                    Util_functions.validateOutput(metadata, reConfig);
                    break;
                }
            }
            catch (IOException | TikaException | SAXException | NullPointerException e) {
                e.printStackTrace();
            }
        }


//        speed_test();
//        test(inputStream, handler,metadata,ps,"ShimadzuParser");

    }
}
