package re.parsers.config;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Class responsible for loading configurations
 */
public class ReConfig {
    String result = "";
    InputStream inputStream;
    String user_dir_path = System.getProperty("user.dir").replace("Modules","");
    String path = "/Modules/src/main/resources/re/parsers/config.properties";
    Path p;

    /**
     * @return rules file
     * @throws IOException
     */
    public JSONObject getRules() throws IOException {
        JSONObject jsonRules = null;
        InputStream isr = null;
        try {
            Properties prop = new Properties();
            InputStream is = getClass().getClassLoader().getResourceAsStream("re/parsers/config.properties");
            prop.load(is);

            isr = getClass().getClassLoader().getResourceAsStream(prop.getProperty("rules"));
            String json_scheme = null;
            if (isr != null) {
                json_scheme = IOUtils.toString( isr, StandardCharsets.UTF_8 );
                jsonRules = new JSONObject(new JSONTokener(json_scheme));
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonRules;
    }

    /**
     * @return output directory
     * @throws IOException
     */
    public Path getOut_dir() throws IOException {

        try {
            Properties prop = new Properties();
            InputStream is = getClass().getClassLoader().getResourceAsStream("re/parsers/config.properties");

            prop.load(is);

            this.p = Paths.get(prop.getProperty("out_dir"));

        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return this.p;
    }

    /**
     * @return JSONSchema to validate output
     * @throws IOException
     */
    public JSONObject getSchema_output() throws IOException {
        JSONObject jsonSchema = null;
        InputStream isr = null;
        try {
            Properties prop = new Properties();
            InputStream is = getClass().getClassLoader().getResourceAsStream("re/parsers/config.properties");
            prop.load(is);

            isr = getClass().getClassLoader().getResourceAsStream(prop.getProperty("schema_output"));
            String json_scheme = null;
            if (isr != null) {
                json_scheme = IOUtils.toString( isr, StandardCharsets.UTF_8 );
                jsonSchema = new JSONObject(new JSONTokener(json_scheme));
            }

        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonSchema;
    }

    /**
     * @return JSONSchema to validate rules file
     * @throws IOException
     */
    public JSONObject getSchema_rules() throws IOException {
        JSONObject jsonRules = null;
        InputStream isr = null;
        try {
            Properties prop = new Properties();
            InputStream is = getClass().getClassLoader().getResourceAsStream("re/parsers/config.properties");
            prop.load(is);
            isr = getClass().getClassLoader().getResourceAsStream(prop.getProperty("schema_rules"));
            String json_scheme = null;
            if (isr != null) {
                json_scheme = IOUtils.toString( isr, StandardCharsets.UTF_8 );
                jsonRules = new JSONObject(new JSONTokener(json_scheme));
            }

        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (isr != null) {
                isr.close();
            }
        }
        return jsonRules;
    }

}
