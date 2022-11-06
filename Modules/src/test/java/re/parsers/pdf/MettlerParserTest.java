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

public class MettlerParserTest {

    @After
    public void delete_file() throws IOException {
        FileUtils.forceDelete(new File("JSON_output_files/null.json"));
        FileUtils.forceDelete(new File("JSON_output_files"));
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void parse() throws IOException, TikaException, SAXException {
        String[] listOfFiles = new String[2];
        listOfFiles[0] = "src/test/resources/test-data/Mettler/KF.pdf";
        listOfFiles[1] = "src/test/resources/test-data/Mettler/107313-90-20-05-16-107313kf.pdf";

        JSONObject jsonSubject = getJsonObject(Paths.get("src/main/resources/schema_output.json"));
        PDFParser PDFParser = new PDFParser();

        int counter = 0;
        for (String s : listOfFiles) {
            if (s == null)
                break;
            File file = new File(s);
            if (file.isFile()) {
                FileInputStream inputStream = new FileInputStream(file);
                ContentHandler handler = new ToTextContentHandler();
                Metadata metadata = new Metadata();
                ParseContext parseContext = new ParseContext();
                MettlerParser _mettlerParser = new MettlerParser();

                PDFParser.parse(inputStream, handler, metadata, parseContext);
                _mettlerParser.parse(inputStream, handler, metadata);

                File file1 = new File("JSON_output_files/null.json");
                File file2 = new File("src/test/resources/test-data/json-outputs/Mettler/"
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
