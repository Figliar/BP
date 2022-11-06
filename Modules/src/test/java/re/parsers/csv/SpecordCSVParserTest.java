package re.parsers.csv;

import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.csv.TextAndCSVParser;
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

public class SpecordCSVParserTest {

    @After
    public void delete_file() throws IOException {
        FileUtils.forceDelete(new File("JSON_output_files/null.json"));
        FileUtils.forceDelete(new File("JSON_output_files"));

    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void parse() throws IOException, TikaException, SAXException {
        String[] listOfFiles = new String[4];
        listOfFiles[0] = "src/test/resources/test-data/SpecordCSV/vz.1.csv";
        listOfFiles[1] = "src/test/resources/test-data/SpecordCSV/vz.4.csv";
        listOfFiles[2] = "src/test/resources/test-data/SpecordCSV/st.1.csv";
        listOfFiles[3] = "src/test/resources/test-data/SpecordCSV/st.4.csv";

        JSONObject jsonSubject = getJsonObject(Paths.get("src/main/resources/schema_output.json"));

        TextAndCSVParser textAndCSVParser = new TextAndCSVParser();

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
                SpecordCSVParser specordCSVParser = new SpecordCSVParser();

                textAndCSVParser.parse(inputStream, handler, metadata, parseContext);
                specordCSVParser.parse(inputStream, handler, metadata);

                File file1 = new File("JSON_output_files/null.json");
                File file2 = new File("src/test/resources/test-data/json-outputs/SpecordCSV/"
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