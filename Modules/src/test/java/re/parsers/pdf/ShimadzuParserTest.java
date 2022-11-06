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

public class ShimadzuParserTest {

    @After
    public void delete_file() throws IOException {
        FileUtils.forceDelete(new File("JSON_output_files/null.json"));
        FileUtils.forceDelete(new File("JSON_output_files"));
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void ParserShimadzuTest() throws IOException, TikaException, SAXException {
        String[] listOfFiles = new String[19];
        listOfFiles[0] = "src/test/resources/test-data/Shimadzu/PR.PDF";
        listOfFiles[1] = "src/test/resources/test-data/Shimadzu/PR1.pdf";
        listOfFiles[2] = "src/test/resources/test-data/Shimadzu/PR2.pdf";
        listOfFiles[3] = "src/test/resources/test-data/Shimadzu/VZ1.pdf";
        listOfFiles[4] = "src/test/resources/test-data/Shimadzu/VZ2.pdf";
        listOfFiles[5] = "src/test/resources/test-data/Letters/A.pdf";
        listOfFiles[6] = "src/test/resources/test-data/Letters/B.pdf";
        listOfFiles[7] = "src/test/resources/test-data/Letters/C.pdf";
        listOfFiles[8] = "src/test/resources/test-data/Letters/D.pdf";
        listOfFiles[9] = "src/test/resources/test-data/Letters/E.pdf";
        listOfFiles[10] = "src/test/resources/test-data/Letters/F.pdf";
        listOfFiles[11] = "src/test/resources/test-data/Shimadzu/107145-81-20-05-12-107145_bezTOL.pdf";
        listOfFiles[12] = "src/test/resources/test-data/Shimadzu/CPM_9_18_2.pdf";
        listOfFiles[13] = "src/test/resources/test-data/Shimadzu/dmad 7.pdf";
        listOfFiles[14] = "src/test/resources/test-data/Shimadzu/107144-1458-20-05-12-107144.PDF";
        listOfFiles[15] = "src/test/resources/test-data/Shimadzu/107145-81-20-05-12-107145_bezTOL_jedn.pdf";
        listOfFiles[16] = "src/test/resources/test-data/Shimadzu/107145-81-20-05-12-107145_TOL.pdf";
        listOfFiles[17] = "src/test/resources/test-data/Shimadzu/107160-81-20-05-12-107160.pdf";
        listOfFiles[18] = "src/test/resources/test-data/Shimadzu/107511-81-20-05-22-107511.PDF";

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
                ShimadzuParser shimadzuParser = new ShimadzuParser();

                PDFParser.parse(inputStream, handler, metadata, parseContext);
                shimadzuParser.parse(inputStream, handler, metadata);

                File file1 = new File("JSON_output_files/null.json");
                File file2 = new File("src/test/resources/test-data/json-outputs/Shimadzu/"
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