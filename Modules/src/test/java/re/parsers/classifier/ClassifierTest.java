package re.parsers.classifier;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.csv.TextAndCSVParser;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.ToTextContentHandler;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import re.parsers.Util_functions;
import re.parsers.config.ReConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static re.parsers.Util_functions.getJsonObject;

public class ClassifierTest {

    ReConfig reConfig = new ReConfig();
    JSONObject rules = getJsonObject(Paths.get("src/main/resources/rules.json"));

    public ClassifierTest() throws IOException, ParseException {
    }


    @Test(expected = ValidationException.class)
    public void InvalidOutputJson() throws IOException {
        JSONObject jsonSubject = getJsonObject(Paths.get("src/main/resources/schema_output.json"));
        Util_functions.jsonValidation(
                getJsonObject(Paths.get("src/test/resources/test-data/json-outputs/BadJsonOutput/3.json")),
                jsonSubject);
    }

    @Test
    public void ValidRulesJson() throws IOException {
        JSONObject jsonSubject = getJsonObject(Paths.get("src/main/resources/schema_rules.json"));
        Util_functions.jsonValidation(
                getJsonObject(Paths.get("src/main/resources/rules.json")),
                jsonSubject);
    }

    @Test
    public void DecisionTestAgilentXLSX() throws IOException, TikaException, SAXException {
        String test_data_path = "src/test/resources/test-data/AgilentXLSX/";

        Classifier tester = new Classifier();
        OOXMLParser ooxmlParser = new OOXMLParser();

        File folder = new File(test_data_path);
        File[] listOfFiles = folder.listFiles();

        int counter = 0;
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    FileInputStream inputStream = new FileInputStream(file);
                    ContentHandler handler = new ToTextContentHandler();
                    Metadata metadata = new Metadata();
                    ooxmlParser.parse(inputStream, handler, metadata, new ParseContext());
                    assertEquals("For the file " + file.getName() + " it should call AgilentXLSXParser",
                            "AgilentXLSXParser", tester.decideJSON(handler.toString(), metadata, "xlsx", rules));
                    assertEquals("For the file " + file.getName() + " it should call AgilentXLSXParser",
                            "AgilentXLSXParser", tester.decideXML(handler.toString(), metadata));
                    System.out.println(++counter + " / " + listOfFiles.length);
                }
            }
        }
    }

    @Test
    public void DecisionTestSpecordCSV() throws IOException, TikaException, SAXException {
        String test_data_path = "src/test/resources/test-data/SpecordCSV/";

        Classifier tester = new Classifier();
        TextAndCSVParser textAndCSVParser = new TextAndCSVParser();

        File folder = new File(test_data_path);
        File[] listOfFiles = folder.listFiles();

        int counter = 0;
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    FileInputStream inputStream = new FileInputStream(file);
                    ContentHandler handler = new ToTextContentHandler();
                    Metadata metadata = new Metadata();
                    textAndCSVParser.parse(inputStream, handler, metadata, new ParseContext());
                    assertEquals("For the file " + file.getName() + " it should call SpecordCSVParser",
                            "SpecordCSVParser", tester.decideJSON(handler.toString(), metadata, "csv", rules));
                    assertEquals("For the file " + file.getName() + " it should call SpecordCSVParser",
                            "SpecordCSVParser", tester.decideXML(handler.toString(), metadata));
                    System.out.println(++counter + " / " + listOfFiles.length);
                }
            }
        }
    }

    @Test
    public void DecisionTestShimadzu() throws IOException, TikaException, SAXException {
        String test_data_path = "src/test/resources/test-data/Shimadzu/";

        Classifier tester = new Classifier();
        PDFParser PDFParser = new PDFParser();

        File folder = new File(test_data_path);
        File[] listOfFiles = folder.listFiles();

        int counter = 0;
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    FileInputStream inputStream = new FileInputStream(file);
                    ContentHandler handler = new ToTextContentHandler();
                    Metadata metadata = new Metadata();
                    PDFParser.parse(inputStream, handler, metadata, new ParseContext());
                    assertEquals("For the file " + file.getName() + " it should call ShimadzuParser",
                            "ShimadzuParser", tester.decideJSON(handler.toString(), metadata, "pdf", rules));
                    assertEquals("For the file " + file.getName() + " it should call ShimadzuParser",
                            "ShimadzuParser", tester.decideXML(handler.toString(), metadata));
                    System.out.println(++counter + " / " + listOfFiles.length);
                }
            }
        }
    }

    @Test
    public void DecisionTestMettler() throws IOException, TikaException, SAXException {
        String test_data_path = "src/test/resources/test-data/Mettler/";

        Classifier tester = new Classifier();
        PDFParser PDFParser = new PDFParser();

        File folder = new File(test_data_path);
        File[] listOfFiles = folder.listFiles();

        int counter = 0;
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    FileInputStream inputStream = new FileInputStream(file);
                    ContentHandler handler = new ToTextContentHandler();
                    Metadata metadata = new Metadata();
                    PDFParser.parse(inputStream, handler, metadata, new ParseContext());
                    assertEquals("For the file " + file.getName() + " it should call MettlerParser",
                            "MettlerParser", tester.decideJSON(handler.toString(), metadata, "pdf", rules));
                    assertEquals("For the file " + file.getName() + " it should call MettlerParser",
                            "MettlerParser", tester.decideXML(handler.toString(), metadata));
                    System.out.println(++counter + " / " + listOfFiles.length);
                }
            }
        }
    }

    @Test
    public void DecisionTestAgilentPDF() throws IOException, TikaException, SAXException {
        String test_data_path = "src/test/resources/test-data/Agilent/";

        Classifier tester = new Classifier();
        PDFParser PDFParser = new PDFParser();

        File folder = new File(test_data_path);
        File[] listOfFiles = folder.listFiles();

        int counter = 0;
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    FileInputStream inputStream = new FileInputStream(file);
                    ContentHandler handler = new ToTextContentHandler();
                    Metadata metadata = new Metadata();
                    PDFParser.parse(inputStream, handler, metadata, new ParseContext());
                    assertEquals("For the file " + file.getName() + " it should call AgilentPDFParser",
                            "AgilentPDFParser", tester.decideJSON(handler.toString(), metadata, "pdf", rules));
                    assertEquals("For the file " + file.getName() + " it should call AgilentPDFParser",
                            "AgilentPDFParser", tester.decideXML(handler.toString(), metadata));
                    System.out.println(++counter + " / " + listOfFiles.length);
                }
            }
        }
    }

    @Test
    public void DecisionTestSpecord() throws IOException, TikaException, SAXException {
        String test_data_path = "src/test/resources/test-data/Specord/";

        Classifier tester = new Classifier();
        PDFParser PDFParser = new PDFParser();

        File folder = new File(test_data_path);
        File[] listOfFiles = folder.listFiles();

        int counter = 0;
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    FileInputStream inputStream = new FileInputStream(file);
                    ContentHandler handler = new ToTextContentHandler();
                    Metadata metadata = new Metadata();
                    PDFParser.parse(inputStream, handler, metadata, new ParseContext());
                    assertEquals("For the file " + file.getName() + " it should call SFM_SpecordParser",
                            "SpecordPDFParser", tester.decideJSON(handler.toString(), metadata, "pdf", rules));
                    assertEquals("For the file " + file.getName() + " it should call SpecordPDFParser",
                            "SpecordPDFParser", tester.decideXML(handler.toString(), metadata));
                    System.out.println(++counter + " / " + listOfFiles.length);
                }
            }
        }
    }

    @Test
    public void DecisionUnknownDataTest() throws IOException, TikaException, SAXException {

        Classifier tester = new Classifier();

        /* PDF unknown data test */
        PDFParser pdfParser = new PDFParser();
        String test_data_path = "src/test/resources/test-data/UnknownData/pdf";
        File folder = new File(test_data_path);
        File[] listOfFiles = folder.listFiles();
        unknownDataTest(tester, pdfParser, listOfFiles, "pdf");

        /* XLSX unknown data test */
        OOXMLParser ooxmlParser = new OOXMLParser();
        test_data_path = "src/test/resources/test-data/UnknownData/xlsx";
        folder = new File(test_data_path);
        listOfFiles = folder.listFiles();
        unknownDataTest(tester, ooxmlParser, listOfFiles, "xlsx");

        /* CSV unknown data test */
        TextAndCSVParser textAndCSVParser = new TextAndCSVParser();
        test_data_path = "src/test/resources/test-data/UnknownData/csv";
        folder = new File(test_data_path);
        listOfFiles = folder.listFiles();
        unknownDataTest(tester, textAndCSVParser, listOfFiles, "csv");

    }

    private void unknownDataTest(Classifier tester, Parser parser, File[] listOfFiles, String type) throws IOException, SAXException, TikaException {
        int counter = 0;
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    FileInputStream inputStream = new FileInputStream(file);
                    ContentHandler handler = new ToTextContentHandler();
                    Metadata metadata = new Metadata();
                    parser.parse(inputStream, handler, metadata, new ParseContext());
                    assertNull("For the file " + file.getName() + " it should return null",
                            tester.decideJSON(handler.toString(), metadata, type, rules));
                    assertNull("For the file " + file.getName() + " it should return null",
                            tester.decideXML(handler.toString(), metadata));
                    System.out.println(++counter + " / " + listOfFiles.length);
                }
            }
        }
    }


}
