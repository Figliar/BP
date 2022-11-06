package re.parsers;

import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static re.parsers.Util_functions.getJsonObject;

public class BadJsonValidationTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void BadOutputValidationTest() throws IOException, ValidationException, SAXException {
        String test_data_path = "src/test/resources/test-data/json-outputs/BadJsonOutput/";
        File folder = new File(test_data_path);
        File[] listOfFiles = folder.listFiles();
        JSONObject jsonSubject = getJsonObject(Paths.get("src/main/resources/schema_output.json"));
        int counter = 0;
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    try {
                        Util_functions.jsonValidation(getJsonObject(Paths.get(file.getPath())), jsonSubject);
                        throw new IOException();
                    } catch (ValidationException e) {
                        // ignore
                    }
                    System.out.println(++counter + " / " + listOfFiles.length);
                }
            }
        }
    }

    @Test
    public void BadRulesValidationTest() throws IOException, ValidationException, SAXException {
        String test_data_path = "src/test/resources/test-data/json-outputs/BadJsonRules/";
        File folder = new File(test_data_path);
        File[] listOfFiles = folder.listFiles();
        JSONObject jsonSubject = getJsonObject(Paths.get("src/main/resources/schema_rules.json"));
        int counter = 0;
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    try {
                        Util_functions.jsonValidation(getJsonObject(Paths.get(file.getPath())), jsonSubject);
                        throw new IOException();
                    } catch (ValidationException e) {
                        // ignore
                    }
                    System.out.println(++counter + " / " + listOfFiles.length);
                }
            }
        }
    }
}
