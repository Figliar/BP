package re.parsers;

import com.google.gson.*;
import org.apache.commons.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.simple.parser.ParseException;
import re.parsers.config.ReConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class containing all utility and help functions
 */
public class Util_functions {

    /**
     * Function makes JSON data pretty to read (adds new-lines, indents,...)
     * @param metadata metadata to add in JSON file
     * @param jsonObject JSON object to add metadata to
     */
    public static void writeJSON(Metadata metadata, JsonObject jsonObject) {

        addMetadata(metadata, jsonObject);

        String prettyJsonString = prettify(jsonObject);

        File filename = null;
        ReConfig properties = new ReConfig();
        try {
            Path path = Paths.get(properties.getOut_dir().toUri());
            if (!Files.isDirectory(path)) {
                Files.createDirectories(path);
            }
            if(!path.toString().isEmpty())
                filename = new File(path + "/" + metadata.get("resourceName") + ".json");
            else
                filename = new File(metadata.get("resourceName") + ".json");
        }
        catch (IOException e){
            e.printStackTrace();
            filename = new File(metadata.get("resourceName") + "IOException.json");
        }finally {
            if (filename != null) {
                writeToFile(prettyJsonString, filename);
            }
        }
    }

    /**
     * @param jsonObject JSON object to make pretty
     * @return pretty JSON object
     */
    @Nullable
    private static String prettify(JsonObject jsonObject) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(jsonObject.toString());
        String prettyJsonString = gson.toJson(je);
        prettyJsonString = org.apache.commons.text.StringEscapeUtils.unescapeJava(prettyJsonString);
        return prettyJsonString;
    }

    /**
     * @param metadata metadata to add to JSON object
     * @param jsonObject JSON object to add metadata to
     */
    private static void addMetadata(Metadata metadata, JsonObject jsonObject) {
        JsonObject meta = new JsonObject();
        for (String key:
             metadata.names()) {
            meta.addProperty(key, metadata.get(key).trim().replace("\n", ";")
            .replace("\\", "\\\\"));
        }
        jsonObject.add("metadata", meta);
    }

    /**
     * Function creates file named after filename value and writes prettyJsonString to it
     * @param prettyJsonString JSON string to be written
     * @param filename name of file to write to
     */
    public static void writeToFile(String prettyJsonString, File filename) {
        try{
            if(filename.createNewFile())
                System.out.println("\n>> OK <<\n" + "Created: " + filename.getName());
            else
                System.out.println("\n>> OK <<\n" + "File " + filename.getName() + " existed already. Rewritten!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileWriter myWriter = null;
        try {
            myWriter = new FileWriter(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (myWriter != null) {
                myWriter.write(prettyJsonString);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (myWriter != null) {
                myWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function to check string for digit
     * @param s string to check
     * @return true if @param s contains digit, else returns false
     */
    public static boolean hasDigit(String s) {
        boolean containsDigit = false;
        if (s != null && !s.isEmpty()) {
            for (char c : s.toCharArray()) {
                if (containsDigit = Character.isDigit(c)) {
                    break;
                }
            }
        }
        return containsDigit;
    }

    /**
     * Function parses @param temp_line for date and adds it to data @param date
     * @param data JSON data to add date to
     * @param temp_line line of text containing date
     */
    public static void addDate(JsonObject data, String[] temp_line) {
        String key = temp_line[0];
        String[] help = temp_line[3].split("(?<=\\d\\d\\s.M)");
        if(help.length != 2)
            help = temp_line[3].split("(?<=\\d\\d)");
        String value = temp_line[1] + ":" + temp_line[2] + ":" + help[0];
        data.addProperty(key.trim(), value.trim());
        if(help.length > 1) {
            key = help[1];
            value = temp_line[4];
            data.addProperty(key.trim(), value.trim());
        }
    }

    /**
     * Function checks if @param line contains any of the values in @param array
     * @param line line of text to check for matches
     * @param array array of values to look for in the @param line
     * @return true if @param line contains at least one of the values from @param array
     */
    public static boolean customContains(String line, String[] array){
        for (String s:
             array) {
            if(line.contains(s))
                return true;
        }
        return false;
    }

    /**
     * Function to validate JSON file against JSON Schema
     * @param json path to JSON file to be checked
     * @param jsonSchema path to JSON Schema file to check JSON file against
     * @throws ValidationException if JSON file is not valid
     * @throws IOException if there is problem with either of the files
     */
    public static void jsonValidation(JSONObject json, JSONObject jsonSchema) throws ValidationException, IOException {
        Schema schema = SchemaLoader.load(jsonSchema);
        schema.validate(json);
    }

    /**
     * @param reConfig configuration to load rules and schema from
     * @throws IOException if each file does not exist
     */
    public static void validateRules(ReConfig reConfig) throws IOException {
        JSONObject schema = reConfig.getSchema_rules();
        JSONObject rules = reConfig.getRules();
        try {
            jsonValidation(rules, schema);
            System.out.println("\n>> OK <<\n"
                    + "Rules file is VALID against " + schema.get("title"));
        }catch (ValidationException e){
            System.out.println("\n FAILED <<\n"
                    + "!!! " + rules + " is NOT VALID against " + schema.get("title") + "!!!\n");
        }
    }

    /**
     * Function to validate output against JSON Schema
     * @param metadata metadata containing the name of the file to check under "resourceName" key
     * @param reConfig ReConfig instance containing path to JSON Schema
     * @throws IOException if there is problem with either of the files
     */
    public static void validateOutput(Metadata metadata, ReConfig reConfig) throws IOException, ParseException {
        JSONObject schema = reConfig.getSchema_output();
        Path outdir = reConfig.getOut_dir();
        String filename = outdir.toString().isEmpty() ?
                metadata.get("resourceName") + ".json" :
                outdir + "/" + metadata.get("resourceName") + ".json";
        JSONObject file = getJsonObject(Paths.get(filename));

        try {
            jsonValidation(file, schema);
            System.out.println("\n>> OK <<\n" +
                    filename + " is VALID against " + schema.get("title"));
        }catch (ValidationException e){
            System.out.println("\n FAILED <<\n"
                    + "!!! " + filename + " is NOT VALID against " + schema + "!!!");
        }
    }

    /**
     * @param line line of text to check for character correction
     * @return corrected line
     */
    public static String checkLine(String line){
        if(line.contains("\\"))
            line = line.replace("\\", "\\\\");
        if(line.contains("\""))
            line = line.replace("\"", "\\\"");
        return line;
    }

    public static JSONObject getJsonObject(Path p) throws IOException {
        File f = new File(p.toUri());
        FileInputStream fis1 = new FileInputStream(f);
        String json_out = IOUtils.toString( fis1, StandardCharsets.UTF_8);
        return new JSONObject(new JSONTokener(json_out));
    }

    /**
     * @param file1 File to copy from
     * @param file2 File to copy to
     * @throws IOException
     */
    public static void copy_file(File file1, File file2) throws IOException {
        try (FileInputStream in = new FileInputStream(file1); FileOutputStream out = new FileOutputStream(file2)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    /**
     * @param s string to check for keywords
     * @param data_keys array of keywords to check for in @param s
     * @return returns key that finds in @param s, else empty string
     */
    public static String check_for_key(String s, String[] data_keys) {
        for (String key:
                data_keys) {
            if(s.contains(key)){
                return key;
            }
        }
        return "";
    }
}
