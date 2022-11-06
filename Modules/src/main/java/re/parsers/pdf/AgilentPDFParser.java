package re.parsers.pdf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import re.parsers.service.ReParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static re.parsers.Util_functions.*;

public class AgilentPDFParser extends ReParser {

    /**
     * Function called by ServiceLoader to find needed provider
     * @return name of the class
     */
    @Override
    public String getType() {
        return this.getClass().getSimpleName();
    }

    //TODO 5.pdf krátka tabuľka pre tri grafy
    //TODO 107141-56-2020-05-12 107141.pdf LIMS ID OA: => LIMS ID: OA chyba
    //TODO 107161-56-2020-05-12 107161.pdf Sample Description: hodnota na viac riadkov

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
        if(metadata.get("dc:subject").trim().equals("Sequence Summary Report"))
            sequence_summary_report(contentHandler, metadata);
        else
            signal_injection_report(contentHandler, metadata);
    }

    /**
     * Method for single file 4.pdf which has metadata value "Sequence Summary Report" for "dc:subject" key
     * @param handler text contents of parsed file
     * @param metadata metadata of parsed file
     * @throws IOException if there is something wrong with the file
     */
    private void sequence_summary_report(ContentHandler handler, Metadata metadata) throws IOException {
        Scanner scanner = new Scanner(handler.toString());
        JsonObject jsonObject = new JsonObject();
        JsonObject data = new JsonObject();
        JsonObject section = new JsonObject();
        JsonArray content = new JsonArray();
        ArrayList<String> signals = new ArrayList<>();
        String[] temp_line = null;
        int counter = 1;
        ArrayList<String> compound = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();
        String sample_name = "";
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            System.out.println(line);
            if(line.contains("\\"))
                line = line.replace("\\", "\\\\");
            if(!line.isEmpty()) {
                if(line.contains("Compound:")){
                    compound.add(line.split(":")[1].trim());
                    while(scanner.hasNextLine()) {
                        line = scanner.nextLine();
                        if(!line.isEmpty()){
                            if(line.contains("Sample") && line.contains("Area"))
                                keys.add(line.trim());
                            if(line.contains("MBS"))
                                values.add(line.trim());
                            if(line.contains("RSD NaN"))
                                break;
                        }
                    }

                }
                else if(!values.isEmpty()){
                    if(line.contains(values.get(0).split(" ")[0])) {
                        sample_name = line.trim();
                    }
                }
                else {
                    get_other_data(data, line, scanner);
                    section.add("data", data);
                }
            }
        }
        for (int i = 0; i < compound.size(); i++) {
            JsonObject table = new JsonObject();
            table.addProperty("Compound", compound.get(i));
            String[] full_keys = keys.get(i).split(" ");
            String[] full_values = values.get(i).split(" ");
            section.add("table_" + (i + 1), addToJSON2(full_keys, full_values, sample_name, table));
        }
        content.add(section);
        jsonObject.add("content", content);
        writeJSON(metadata, jsonObject);
        writeJSON(metadata, jsonObject);
    }

    /**
     * Method for parsing documents classified for AgilentPDFParser
     * @param handler text contents of parsed file
     * @param metadata metadata of parsed file
     */
    private void signal_injection_report(ContentHandler handler, Metadata metadata) {

        Scanner scanner = new Scanner(handler.toString());
        int page_counter = 0;
        String line = "";
        JsonObject jsonObject = new JsonObject();
        JsonArray content = new JsonArray();

        while(scanner.hasNextLine()){
            if(!line.contains("Project Name"))
                line = scanner.nextLine();
            if(line.contains("Project Name")){
                JsonObject section = new JsonObject();
                section.addProperty("section_name", "Report " + ++page_counter);
                JsonObject data = new JsonObject();
                String summary = "";
                ArrayList<String> signals = new ArrayList<>();
                String[] temp_line = null;

                int counter = 0;
                while(scanner.hasNextLine()) {
                    line = checkLine(line);
                    if(!line.isEmpty()) {
                        if(line.contains("Signal:") && check_signal(signals, line)){
                            ArrayList<String> keys = new ArrayList<>();
                            ArrayList<String> values = new ArrayList<>();
                            JsonObject table = new JsonObject();
                            int row = 0;
                            signals.add(line.trim().split(":")[1].trim());
                            counter++;
                            while(scanner.hasNextLine()){
                                line = scanner.nextLine();
                                if(line.isEmpty()) continue;
                                if(line.contains(".")){
                                    break;
                                }
                                keys.add(line);

                            }
                            String full = String.join(" ", keys);
                            String[] full_keys = full.split(" ");
                            while(scanner.hasNextLine()){
                                if(line.contains("Sum")){
                                    temp_line = line.split(" ");
                                    summary = temp_line.length == 2 ? temp_line[1] : "";
                                    JsonObject sum = new JsonObject();
                                    sum.addProperty("Area", summary);
                                    table.add("Sum", sum);
                                    break;
                                }
                                if(!line.isEmpty() && check(full_keys, line)) {
                                    values.add(line.trim());
                                }
                                line = scanner.nextLine();
                            }
                            table.addProperty("Signal", signals.get(signals.size() - 1));
                            String[] full_values = null;
                            for (String value:
                                    values) {
                                full_values = value.split(" ");
                                section.add("table_" + counter, addToJSON(full_keys, full_values, ++row, table));
                            }
                            values.clear();
                            keys.clear();
                        }
                        else {
                            get_other_data(data, line, scanner);
                            section.add("data", data);
                        }
                    }
                    line = scanner.nextLine();
                    if(line.contains("Project Name"))
                        break;
                }
                if(section.get("table_" + counter) == null){
                    section.add("table_" + ++counter, new JsonObject());
                }
                content.add(section);
            }
        }
        jsonObject.add("content", content);
        writeJSON(metadata, jsonObject);
    }

    /**
     * Method called to collect key/value type data from non-table part sof document
     * @param data Json object to add data to
     * @param line line from text content of document to check for data
     * @param scanner to iter to next line, if data continues on next line
     */
    private void get_other_data(JsonObject data, String line, Scanner scanner) {
        String[] temp_line;
        String[] data_keys = new String[]{"Sample Amount (mg)", "Project Name",
                "Sequence Name", "Particle size", "Sample Description", "Operator",
                "Location", "LIMS ID", "Serial #", "Sample Amount", "Instrument", "Acq. method",
                "Acq. operator", "Sample name", "Diameter", "Length", "Processing method",
                "Injection date", "Processed by", "Signal", "Column name", "Diameter", "Inj. volume"};
//        ArrayList<String> data_keys = new ArrayList<>(Arrays.asList("Sample Amount (mg)", "Project Name",
//                "Sequence Name", "Particle size", "Sample Description", "Operator",
//                "Location", "LIMS ID", "Serial #", "Sample Amount", "Instrument", "Acq. method",
//                "Acq. operator", "Sample name", "Diameter", "Length", "Processing method",
//                "Injection date", "Processed by", "Signal", "Column name", "Diameter", "Inj. volume"
//                ));
        if(line.contains(":")) {
            temp_line = line.trim().split(":");
            if(temp_line[0].contains("Sample Description") && temp_line[0].contains("Location")) {
                String temp = temp_line[0].replace("Sample Description", "");
                data.addProperty("Sample Description", temp.replace("Location", "").trim());
                data.addProperty("Location", temp_line[1].trim());
            }
            else if (!hasDigit(temp_line[0])) {
                if (temp_line.length == 2) {
                    if(temp_line[1].contains("Project Name")){
                        String[] temp = temp_line[1].split("Project Name");
                        data.addProperty(temp_line[0].trim(), temp[0].trim());
                        data.addProperty("Project Name", temp.length == 1 ? "" : temp[1].trim());
                    }else if(temp_line[1].contains("Sequence Name")){
                        String[] temp = temp_line[1].split("Sequence Name");
                        data.addProperty(temp_line[0].trim(), temp[0].trim());
                        data.addProperty("Sequence Name", temp.length == 1 ? "" : temp[1].trim());
                    }else if(temp_line[1].contains("Particle size")){
                        String[] temp = temp_line[1].split("Particle size");
                        data.addProperty(temp_line[0].trim(), temp[0].trim());
                        data.addProperty("Particle size", temp.length == 1 ? "" : temp[1].trim());
                    }else if(temp_line[1].contains("Sample Description") && temp_line[0].contains("Location")) {
                        String[] temp = temp_line[1].split("Sample Description");
                        data.addProperty("Location", temp[0].trim());
                        data.addProperty("Sample Description", temp.length == 1 ? "" : temp[1].trim());
                    }else if(temp_line[1].contains("Serial #") && temp_line[0].contains("LIMS ID")) {
                        String[] temp = temp_line[1].split("Serial #");
                        data.addProperty("LIMS ID", temp[0].trim());
                        data.addProperty("Serial #", temp.length == 1 ? "" : temp[1].trim());
                    }
                    else if(temp_line[0].contains("Serial #") && temp_line[0].contains("LIMS ID")) {
                        String[] temp = temp_line[0].split("Serial #");
                        data.addProperty("Serial #", temp_line[1].trim());
                        data.addProperty("LIMS ID", temp.length == 1 ? "" : temp[1].trim());
                    }
                    else {
                        StringBuilder end = new StringBuilder();
                        if(line.contains("Data file") && !line.contains(".dx")){
                            line = scanner.nextLine();
                            while(!line.contains(".dx")){
                                line = scanner.nextLine();
                                end.append(line.trim());
                            }
                        }
                        data.addProperty(temp_line[0].trim(), temp_line[1].trim() + end.toString());
                    }
                } else if (temp_line.length == 3) {
                    String key = "";
                    String key2 = "";
                    StringBuilder value = new StringBuilder();
                    switch (temp_line[0].trim()) {
                        case "Project Name":
                        case "Sequence Name":
                        case "Sample name":
                        case "Processed by":
                        case "Operator":
                        case "Acq. method":
                        case "LIMS ID":
                        case "LIMS ID OA":
                        case "Data file":
                            three_keys(data, temp_line, data_keys);
                            break;
                        case "Serial #":
                        case "Sample Amount":
                            key = temp_line[0];
                            value = new StringBuilder();
                            for (int i = 0; i < temp_line[1].length(); i++){
                                value.append(temp_line[1].charAt(i));
                                if(i > 0) {
                                    if (Character.isDigit(temp_line[1].charAt(i)) && Character.isAlphabetic(temp_line[1].charAt(i + 1)))
                                        break;
                                }
                            }
                            data.addProperty(key.trim(), value.toString().trim());
                            key = temp_line[1].replace(value.toString(), "");
                            data.addProperty(key.trim(), temp_line[2].trim());
                            break;
                        case "Instrument":
                            key = temp_line[0];
                            if(temp_line[1].contains("Name"))
                                value.append(temp_line[1].replace("Project Name", ""));
                            data.addProperty(key.trim(), value.toString().trim());
                            data.addProperty("Project Name", temp_line[2].trim());
                            break;
                        case "Acq. operator":
                            if(temp_line[1].contains("Diameter")){
                                String temp = temp_line[1].trim().split("Diameter")[0];
                                data.addProperty(temp_line[0].trim(), temp);
                                data.addProperty(temp_line[1].replace(temp, "").trim(), temp_line[2].trim());
                            }
                            break;
                        default:
                            data.addProperty(temp_line[0].trim(), temp_line[1].trim().split(" ")[0]);
                            break;
                    }
                }
                else if (temp_line[0].trim().equals("Injection date")){
                    addDate(data, temp_line);
                }
                else if(temp_line.length == 1){
                    data.addProperty(temp_line[0], "");
                }
            }else{
                if (temp_line.length == 1){
                    if(temp_line[0].contains("Serial #") && temp_line[0].contains("LIMS ID")) {
                        String[] temp = temp_line[0].split("Serial #");
                        data.addProperty("LIMS ID", temp[0].replace("LIMS ID", "").trim());
                        data.addProperty("Serial #", temp.length == 1 ? "" : temp[1].trim());
                    }
                }
                else if(temp_line.length == 2){
                    if(temp_line[0].contains("Serial #") && temp_line[0].contains("LIMS ID")) {
                        String[] temp = temp_line[0].split("Serial #");
                        data.addProperty("LIMS ID", temp[0].replace("LIMS ID", "").trim());
                        data.addProperty("Serial #", temp_line[1].trim());
                    }
                }
            }
        }else if(line.contains("LIMS ID") && !line.contains("Sample Name")){
            String[] splitted = line.split(" ");
            data.addProperty(splitted[0] + " " + splitted[1], splitted[2].trim());
        }else if(line.contains("Particle size")){
            String[] splitted = line.split(" ");
            data.addProperty(splitted[0] + " " + splitted[1], splitted[2].trim());
        }
    }

    /**
     * Method to deal with lines where it looks like there are three keywords
     * @param data Json object to add data to
     * @param temp_line array of chunks from splitted line
     * @param data_keys keywords to look for in the temp_line chunks
     */
    private void three_keys(JsonObject data, String[] temp_line, String[] data_keys) {
        String key2;
        StringBuilder value;
        String key;
        key = temp_line[0];
        key2 = check_for_key(temp_line[1], data_keys);
        if(!key2.isEmpty()) {
            value = new StringBuilder(temp_line[1].replace(key2, ""));
            data.addProperty(key.trim(), value.toString().trim());
            value = new StringBuilder(temp_line[2]);
            data.addProperty(key2.trim(), value.toString().trim());
        }
    }

    /**
     * Method to add data extracted from table of parsed document called in signal_injection_report()
     * @param full_keys string array of all keys
     * @param full_values string array of all values
     * @param row integer representing row number of the table
     * @param table Json object to add data to
     * @return updated param @table
     */
    private JsonElement addToJSON(String[] full_keys, String[] full_values, int row, JsonObject table) {
        JsonObject json_element = new JsonObject();
        int value = 0;
        for (int key = 0; key < full_keys.length; key++) {
            switch(full_keys[key]){
                case "RT":
                case "Width":
                    json_element.addProperty(full_keys[key++] + " " + full_keys[key], full_values[value++]);
                    table.add(String.valueOf(row), json_element);
                    break;
                case "Type":
                    if(Character.isDigit(full_values[value + 1].charAt(0)))
                        json_element.addProperty(full_keys[key], full_values[value++]);
                    else
                        json_element.addProperty(full_keys[key], full_values[value++] + " " + full_values[value++]);
                    table.add(String.valueOf(row), json_element);
                    break;
                case "Area":
                case "Height":
                case "Area%":
                    if(value < full_values.length) {
                        json_element.addProperty(full_keys[key], full_values[value++]);
                    }
                    else
                        json_element.addProperty(full_keys[key], "");
                    table.add(String.valueOf(row), json_element);
                    break;
                case "Name":
                    if(value < full_values.length) {
                        if (!isNumeric(full_values[value])) {
                            if (value + 1 < full_values.length) {
                                if (full_values[value + 1].matches("[\\d]{3}nm")) {
                                    json_element.addProperty(full_keys[key],
                                            full_values[value++] + " " + full_values[value++]);
                                } else {
                                    json_element.addProperty(full_keys[key], full_values[value++] +
                                    ((full_keys.length == key + 1 && value + 1 <= full_values.length)
                                            ? " " + full_values[value++] : ""));
                                }
                            } else
                                json_element.addProperty(full_keys[key], full_values[value++] +
                                        ((full_keys.length == key + 1 && value + 1 <= full_values.length)
                                                ? " " + full_values[value++] : ""));
                        } else
                            json_element.addProperty(full_keys[key], "");
                    } else
                        json_element.addProperty(full_keys[key], "");
                    table.add(String.valueOf(row), json_element);
                    break;
                case "Peak":
                    if(full_keys[key + 1].equals("Theoretical") && full_keys[key + 2].equals("Plates") &&
                            full_keys[key + 3].equals("USP")) {
                        json_element.addProperty("Peak Theoretical Plates USP", full_values[value++]);
                        key += 3;
                    }
                    else if(full_keys[key + 1].trim().equals("Resolution") && full_keys[key + 2].trim().equals("USP")){
                        if(full_values.length - 1 == value) {
                            json_element.addProperty("Peak Resolution USP", "");
                        }
                        else {
                            json_element.addProperty("Peak Resolution USP", full_values[value++]);
                        }
                        key += 2;
                    }
                    else if(full_keys[key + 1].equals("Tail") && full_keys[key + 2].equals("Factor")) {
                        json_element.addProperty("Peak Tail Factor", full_values[value++]);
                        key += 2;
                    }
                    else if(full_keys[key + 1].equals("Plates") && full_keys[key + 2].equals("Per") &&
                            full_keys[key + 3].equals("Meter") && full_keys[key + 4].equals("USP")) {
                        json_element.addProperty("Peak Plates Per Meter USP", full_values[value++]);
                        key += 4;
                    }
                    table.add(String.valueOf(row), json_element);
                    break;
                default:
                    break;
            }
        }
        return table;
    }

    /**
     * Method to add data extracted from table of parsed document called in sequence_summary_report()
     * @param full_keys string array of all keys
     * @param full_values string array of all values
     * @param sample_name name of section in table
     * @param table updated param @table
     * @return updated param @table
     */
    private JsonObject addToJSON2(String[] full_keys, String[] full_values, String sample_name, JsonObject table) {
        int value = 0;
        System.out.println("ADDTOJSON2 <<<<<<<<<<<<<<");
        for (int key = 0; key < full_keys.length; key++) {
            System.out.println(value + "  " + full_values[value]);
            switch(full_keys[key]){
                case "Sample":
                    table.addProperty(full_keys[key] + " " + full_keys[++key ], sample_name);
                    String eq = full_values[value++];
                    while(!sample_name.equals(eq)){
                        eq += " " + (full_values[value++]);
                    }
                    break;
                case "LIMS":
                case "N":
                    table.addProperty(full_keys[key] + " " + full_keys[++key], full_values[value++]);
                    break;
                case "Vial":
                    table.addProperty(full_keys[key], full_values[value++] + " " + full_values[value++]);
                    break;
                case "RT":
                case "T":
                    table.addProperty(full_keys[key], full_values[value++]);
                    break;
                case "Area":
                    if(full_keys[key + 1].equals("Area")){
                        table.addProperty(full_keys[key], full_values[value++]);
                        break;
                    }
                    else if(full_keys[key + 1].equals("%")){
                        table.addProperty(full_keys[key] + " " + full_keys[++key], full_values[value]);
                        break;
                    }
                case "R":
                    if(full_values.length - 1 - value == 4){
                        table.addProperty(full_keys[key] + " " + full_keys[++key], full_values[value++]);
                    }else{
                        table.addProperty(full_keys[key] + " " + full_keys[++key],"");
                    }
                    break;
                case "k":
                    table.addProperty(full_keys[key],"");
                default:
                    break;
            }
        }
        return table;
    }

    /**
     * Method to check if signal in line was already used or it is new signal for new table
     * @param signals array of used signals
     * @param line line containing signal to check
     * @return true if new signal, else false
     */
    private boolean check_signal(ArrayList<String> signals, String line) {
        for (String signal:
                signals) {
            if(line.contains(signal)) return false;
        }
        return true;
    }

    /**
     * Method to check for some patterns that indicate if the line should be stored
     * @param keys array of keywords to check for
     * @param line line to check for patterns
     * @return true if should store line, else false
     */
    // TODO pozor na 107157-56-2020-05-12 107157.pdf v Name hodnoty RT3, RT5
    private boolean check(String[] keys, String line) {
        String pattern = "RT\\d,\\dmin";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(line);
        if (m.find( ))
            return true;
        if(line.trim().contains("RT,3min"))
            return false;
        for (String key:
                keys) {
            if(line.contains(key)) return false;
        }
        return !line.contains("Report");
    }

    /**
     * Method for checking strings for digits
     * @param strNum string to check for digits
     * @return true if contains digit, else false
     */
    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

}
