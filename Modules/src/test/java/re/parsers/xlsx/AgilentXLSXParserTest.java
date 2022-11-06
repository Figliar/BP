package re.parsers.xlsx;

import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.sax.ToTextContentHandler;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import re.parsers.Util_functions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static re.parsers.Util_functions.getJsonObject;

public class AgilentXLSXParserTest {

    @After
    public void delete_file() throws IOException {
        FileUtils.forceDelete(new File("JSON_output_files/null.json"));
        FileUtils.forceDelete(new File("JSON_output_files"));
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void parse() throws IOException, TikaException, SAXException {
        String[] listOfFiles = new String[5];
        listOfFiles[0] = "src/test/resources/test-data/AgilentXLSX/area.xlsx";
        listOfFiles[1] = "src/test/resources/test-data/AgilentXLSX/assay.xlsx";
        listOfFiles[2] = "src/test/resources/test-data/AgilentXLSX/BSA.xlsx";
        listOfFiles[3] = "src/test/resources/test-data/AgilentXLSX/Cl-BSA+toulen.xlsx";
        listOfFiles[4] = "src/test/resources/test-data/AgilentXLSX/rs.xlsx";

        JSONObject jsonSubject = getJsonObject(Paths.get("src/main/resources/schema_output.json"));
        OOXMLParser ooxmlParser = new OOXMLParser();
        AgilentXLSXParser agilentXLSXParser = new AgilentXLSXParser();
        int counter = 0;
        for (String s : listOfFiles) {
            if(s == null)
                break;
            File file = new File(s);
            if (file.isFile()) {
                FileInputStream inputStream = new FileInputStream(file);
                ContentHandler handler = new ToTextContentHandler();
                Metadata metadata = new Metadata();
                ParseContext parseContext = new ParseContext();

                byte[] buffer = new byte[16000];
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                int len;
                while ((len = inputStream.read(buffer)) > - 1) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.flush();
                InputStream in1 = new ByteArrayInputStream(outputStream.toByteArray());
                InputStream in2 = new ByteArrayInputStream(outputStream.toByteArray());

                ooxmlParser.parse(in1, handler, metadata, parseContext);
                agilentXLSXParser.parse(in2, handler, metadata);

                File file1 = new File("JSON_output_files/null.json");
                File file2 = new File("src/test/resources/test-data/json-outputs/AgilentXLSX/"
                        + s.split("/")[s.split("/").length - 1] + ".json");

                byte[] f1 = Files.readAllBytes(file1.toPath());
                byte[] f2 = Files.readAllBytes(file2.toPath());

                Assert.assertArrayEquals("Files " + file1.getAbsolutePath() + " and \n" + file2.getAbsolutePath() + " should be same", f1, f2);
                Util_functions.jsonValidation(getJsonObject(Paths.get(file2.getPath())), jsonSubject);
                System.out.println(++counter + " / " + listOfFiles.length);
            }
        }
    }
}