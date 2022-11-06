package re.parsers.pdf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import re.parsers.Util_functions;
import re.parsers.service.ReParser;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static re.parsers.Util_functions.check_for_key;

public class MettlerParser extends ReParser {

    /**
     * Function called by ServiceLoader to find needed provider
     * @return name of the class
     */
    @Override
    public String getType() {
        return this.getClass().getSimpleName();
    }

    /**
     * @param inputStream stream of the document to be parsed
     * @param contentHandler text content handler of parsed document
     * @param metadata metadata of parsed document
     * @throws IOException in case of malformed input
     * @throws SAXException in case of XML related exception
     * @throws TikaException for Tika related exceptions
     */
    @Override
    public void parse(InputStream inputStream, ContentHandler contentHandler, Metadata metadata) throws IOException, SAXException, TikaException {

        String[] data_keys = {"Method ID", "Name", "Modified on", "Release state", "Modified by",
                "Mean", "Maximum", "Total samples", "Standard deviation", "Minimum",
                "Rel. standard deviation", "Samples excluded", "Task internal ID", "Task name"};
        Scanner scanner = new Scanner(contentHandler.toString());
        JsonObject jsonObject = new JsonObject();
        JsonObject help = new JsonObject();
        JsonObject data = new JsonObject();
        JsonObject table = new JsonObject();
        JsonObject section = new JsonObject();
        JsonArray content = new JsonArray();
        int i = 1;
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = Util_functions.checkLine(line);
            extract_data(data_keys, data, line);
            if (line.contains("Summary")) {
                extract_table(scanner, help, table, i, line);
            }
            else if(Util_functions.customContains(line, new String[] {"Full name", "Login", "Time"})){
                extract_user_info(scanner, data);
            }
        }
        section.add("data", data);
        section.add("table", table);
        content.add(section);
        jsonObject.add("content", content);
        Util_functions.writeJSON(metadata, jsonObject);
    }

    /**
     * Method to extract data from footer of the document
     * @param scanner scanner to iter through document
     * @param data Json object to add data to
     */
    private void extract_user_info(Scanner scanner, JsonObject data) {
        String line;
        line = scanner.nextLine();
        String helper = "";
        Pattern pattern = Pattern.compile("(\\d{2})\\.(\\d{2})\\.(\\d{4})\\s(\\d\\d?):(\\d\\d?):(\\d\\d?)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            helper = line.replace(matcher.group(0), "");
            data.addProperty("Time", matcher.group(0));
        }
        pattern = Pattern.compile("[\\p{Lu}][\\p{Ll}]*\\s?\\p{Lu}[\\p{Ll}]*");
        matcher = pattern.matcher(line);
        if (matcher.find()) {
            helper = helper.replace(matcher.group(0), "");
            data.addProperty("Full name", matcher.group(0));
        }
        data.addProperty("Login", helper.trim());
    }

    /**
     * Method to extract table from document classified for MettlerParser
     * @param scanner scanner to iter through document
     * @param help temporary Json object to help with constructing correct structure of JSON
     * @param table Json object to add data to
     * @param i integer representing the row of the table
     * @param line line to start extracting from
     */
    private void extract_table(Scanner scanner, JsonObject help, JsonObject table, int i, String line) {
        String[] temp = null;
        while(!line.equals("Statistics")){
            line = scanner.nextLine();
            if(!line.isEmpty()){
                if(line.contains("Sample size")){
                    temp = line.split("Sample size");
                    help.addProperty("Sample ID", temp[1]);
                    help.addProperty("Result", temp[0]);
                    table.add(String.valueOf(i), help);
                }
                else if(line.contains("Content R1")){
                    Pattern pattern = Pattern.compile("(\\d+),?(\\d*)\\s?%");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String helper = "";
                        helper = line.replace("Content R1", "");
                        help.addProperty("State", helper.replace(matcher.group(0), ""));
                        help.addProperty("Content R1", matcher.group(0));
                    }
                    table.add(String.valueOf(i), help);
                    i++;
                }
            }
        }
    }

    /**
     * Method to extract key/value data from non-table parts of document classified for MettlerParser
     * @param data_keys array of keywords to look for
     * @param data Json object to add data to
     * @param line line to check for the data
     */
    private void extract_data(String[] data_keys, JsonObject data, String line) {
        String check;
        for (String data_key:
                data_keys) {
            if(line.contains(data_key)){
                String[] splitted = line.trim().split(data_key);
                if(splitted.length == 1){
                    check = check_for_key(splitted[0], data_keys);
                    if(check.isEmpty()){
                        data.addProperty(data_key, splitted[0].trim());
                    }
                    else{
                        splitted[0] = splitted[0].replace(check, "");
                        String[] values = splitted[0].split(" ");
                        data.addProperty(data_key, values[0].trim());
                        data.addProperty(check.trim(), values[1].trim());
                    }
                }
            }
        }
    }
}
