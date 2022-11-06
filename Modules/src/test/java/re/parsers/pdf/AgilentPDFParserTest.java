package re.parsers.pdf;

import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static re.parsers.Util_functions.getJsonObject;

public class AgilentPDFParserTest {

    @After
    public void delete_file() throws IOException {
        FileUtils.forceDelete(new File("JSON_output_files/null.json"));
        FileUtils.forceDelete(new File("JSON_output_files"));
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void parse() throws IOException, TikaException, SAXException {
        String[] listOfFiles = new String[23];
        listOfFiles[0] = "src/test/resources/test-data/Agilent/1.pdf";
        listOfFiles[1] = "src/test/resources/test-data/Agilent/2.pdf";
        listOfFiles[2] = "src/test/resources/test-data/Agilent/3.pdf";
        listOfFiles[3] = "src/test/resources/test-data/Agilent/4.pdf";
        listOfFiles[4] = "src/test/resources/test-data/Agilent/5.pdf";
        listOfFiles[5] = "src/test/resources/test-data/Agilent/107141-56-2020-05-12 107141.pdf";
        listOfFiles[6] = "src/test/resources/test-data/Agilent/107146-768-2020-05-12 107146.pdf";
        listOfFiles[7] = "src/test/resources/test-data/Agilent/107147-768-2020-05-12 107147.pdf";
        listOfFiles[8] = "src/test/resources/test-data/Agilent/107151-56-2020-05-12 107151.pdf";
        listOfFiles[9] = "src/test/resources/test-data/Agilent/107157-56-2020-05-12 107157.pdf";
        listOfFiles[10] = "src/test/resources/test-data/Agilent/107161-56-2020-05-12 107161.pdf";
        listOfFiles[11] = "src/test/resources/test-data/Agilent/107169-1631-2020-05-13 107169.pdf";
        listOfFiles[12] = "src/test/resources/test-data/Agilent/107171-56-2020-05-13 107171.pdf";
        listOfFiles[13] = "src/test/resources/test-data/Agilent/107191-56-107191.pdf";
        listOfFiles[14] = "src/test/resources/test-data/Agilent/107192-56-107192.pdf";
        listOfFiles[15] = "src/test/resources/test-data/Agilent/107193-56-107193.pdf";
        listOfFiles[16] = "src/test/resources/test-data/Agilent/107194-56-107194.pdf";
        listOfFiles[17] = "src/test/resources/test-data/Agilent/107204-56-2020-05-13 107204.pdf";
        listOfFiles[18] = "src/test/resources/test-data/Agilent/107204-56-2020-05-13 107204 grad.pdf";
        listOfFiles[19] = "src/test/resources/test-data/Agilent/107205-56-2020-05-13 107205.pdf";
        listOfFiles[20] = "src/test/resources/test-data/Agilent/107206-56-2020-05-13 107206.pdf";
        listOfFiles[21] = "src/test/resources/test-data/Agilent/107661-56-349-121.pdf";
        listOfFiles[22] = "src/test/resources/test-data/Agilent/107730-56-107730.pdf";

        JSONObject jsonSubject = getJsonObject(Paths.get("src/main/resources/schema_output.json"));

        PDFParser PDFParser = new PDFParser();
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
                AgilentPDFParser agilentPDFParser = new AgilentPDFParser();

                PDFParser.parse(inputStream, handler, metadata, parseContext);
                agilentPDFParser.parse(inputStream, handler, metadata);

                File file1 = new File("JSON_output_files/null.json");
                File file2 = new File("src/test/resources/test-data/json-outputs/Agilent/"
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