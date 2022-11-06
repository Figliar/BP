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
import java.util.*;

import static re.parsers.Util_functions.*;

public class ShimadzuParser extends ReParser {

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
        Scanner scanner = new Scanner(contentHandler.toString());
        JsonObject jsonObject = new JsonObject();
        JsonObject data = new JsonObject();
        JsonObject table = new JsonObject();
        JsonObject section = new JsonObject();
        JsonArray content = new JsonArray();

        ArrayList<String> peaks = new ArrayList<>();
        ArrayList<String> peaks_start = new ArrayList<>();
        ArrayList<String> peaks_end = new ArrayList<>();
        ArrayList<String> resolutions = new ArrayList<>();
        ArrayList<String> times = new ArrayList<>();
        ArrayList<String> areas = new ArrayList<>();
        ArrayList<String> areas_p = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> heights = new ArrayList<>();
        ArrayList<String> s_n = new ArrayList<>();
        ArrayList<String> a_h = new ArrayList<>();
        ArrayList<String> units = new ArrayList<>();
        ArrayList<String> concentrations = new ArrayList<>();
        ArrayList<String> ratios = new ArrayList<>();
        Map<String, ArrayList<String>> all = new LinkedHashMap<String, ArrayList<String>>();

        String line = "";
        //TODO vzorek HYNA 2869 RM 87_2019 26.6..pdf Description hodnota cez viac riadkov
        while(scanner.hasNextLine()) {
            if (shouldNextLine(line))
                line = scanner.nextLine();
            line = checkLine(line);
            get_other_data(data, line, scanner);
            switch(line){
                case "Peak#":
                    String name = line.trim();
                    line = scanner.nextLine();
                    while(!Util_functions.customContains(line,
                            new String[] {"Ret. Time"})){
                        if(!line.isEmpty())
                            peaks.add(line.trim());
                        line = scanner.nextLine();
                        if(line.contains("Ret. Time") && line.contains("Area")) {
                            line = scanner.nextLine();
                            areas.add(line.trim());
                            break;
                        }
                    }
                    all.put(name, peaks);
                    break;
                case "Ret. Time":
                    name = line.trim();
                    line = scanner.nextLine().trim();
                    while(!Util_functions.customContains(line,
                            new String[] {"Area", "Name", "Height"})){
                        if(!line.isEmpty())
                            times.add(line.trim());
                        line = scanner.nextLine().trim();
                    }
                    all.put(name, times);
                    break;
                case "Area":
                    name = line.trim();
                    line = scanner.nextLine().trim();
                    while(!Util_functions.customContains(line,
                            new String[] {"Area%", "Area %", "Resolution(USP)", "S/N", "Area/Height", "Height"})){
                        if(!line.isEmpty())
                            areas.add(line.trim());
                        line = scanner.nextLine().trim();
                    }
                    all.put(name, areas);
                    break;
                case "Area%":
                case "Area %":
                    name = line.trim();
                    line = scanner.nextLine().trim();
                    while(!Util_functions.customContains(line,
                            new String[] {"Name", "Res.(USP)", "Resolution(USP)"})){
                        if(!line.isEmpty())
                            areas_p.add(line.trim());
                        line = scanner.nextLine().trim();
                    }
                    all.put(name, areas_p);
                    break;
                case "Name":
                    if(!names.isEmpty())
                        names.remove(names.size() - 1);
                    name = line.trim();
                    line = scanner.nextLine().trim();
                    while(!Util_functions.customContains(line,
                            new String[] {"Sample Information", ":", "Conc.", "Area"})){
                        try {
                            names.add(line.trim());
                            line = scanner.nextLine().trim();
                        }catch (NoSuchElementException e){
                            break;
                        }
                    }
                    all.put(name, names);
                    break;
                case "Conc.":
                    name = line.trim();
                    line = scanner.nextLine();
                    while(!line.equals("Unit")){
                        if(!line.isEmpty())
                            concentrations.add(line.trim());
                        line = scanner.nextLine().trim();
                    }
                    all.put(name, concentrations);
                    break;
                case "Unit":
                    name = line.trim();
                    line = scanner.nextLine();
                    while(!Util_functions.customContains(line,
                            new String[] {"Sample Information"})){
                        if(!line.isEmpty())
                            units.add(line.trim());
                        line = scanner.nextLine();
                    }
                    all.put(name, units);
                    break;
                case "Conc.(Ratio)":
//                    name = line.trim();
                    line = scanner.next().trim();
                    while(!line.isEmpty()){
                        ratios.add(line.trim());
                        line = scanner.nextLine();
                    }
                    break;
                case "Resolution(USP)":
                case "Res.(USP)":
                    name = line.trim();
                    line = scanner.next().trim();
                    while(!Util_functions.customContains(line,
                            new String[] {"Name", "S/N"})){
                        if(!line.isEmpty())
                            resolutions.add(line.trim());
                        line = scanner.nextLine();
                    }
                    all.put(name, resolutions);
                    break;
                case "S/N":
                    name = line.trim();
                    line = scanner.next().trim();
                    while(!Util_functions.customContains(line,
                            new String[] {"Name"})){
                        if(!line.isEmpty() && hasDigit(line))
                            s_n.add(line.trim());
                        line = scanner.nextLine();
                    }
                    all.put(name, s_n);
                    break;
                case "Height":
                    name = line.trim();
                    line = scanner.nextLine();
                    while(!Util_functions.customContains(line,
                            new String[] {"Area"})){
                        if(!line.isEmpty())
                            heights.add(line.trim());
                        line = scanner.nextLine();
                    }
                    heights.add("Height");
                    all.put(name, heights);
                    break;
                case "Peak Start":
                    name = line.trim();
                    line = scanner.nextLine();
                    int peak = 1;
                    while(!line.equals("Peak End")){
                        if(!line.isEmpty()) {
                            peaks.add(String.valueOf(peak++));
                            peaks_start.add(line.trim());
                        }
                        line = scanner.nextLine();
                    }
                    peaks_start.add("Peaks Start");
                    peaks.add("Total");
                    all.put(name, peaks_start);
                    break;
                case "Peak End":
                    name = line.trim();
                    line = scanner.nextLine();
                    while(!line.equals("Name")){
                        if(!line.isEmpty())
                            peaks_end.add(line.trim());
                        line = scanner.nextLine();
                    }
                    peaks_end.add("Peak End");
                    all.put(name, peaks_end);
                    break;
                case "Area/Height":
                    name = line.trim();
                    line = scanner.nextLine();
                    while(!Util_functions.customContains(line,
                            new String[] {"Res.(USP)", "Resolution(USP)"})){
                        if(!line.isEmpty())
                            a_h.add(line.trim());
                        line = scanner.nextLine();
                    }
                    all.put(name, a_h);
                    break;
                default:
                    break;
            }
        }
        section.add("data", data);

        // Tabel_1 to JSON
        for(int i = 0; i < peaks.size(); i++){
            JsonObject help2 = new JsonObject();
            for (String key:
                    all.keySet()) {
                if(all.get(key).size() > i) {
                    help2.addProperty(key, all.get(key).get(i));
                }
                else{
                    help2.addProperty(key, "");
                }
                table.add(peaks.get(i), help2);
            }
            section.add("table", table);
        }

        //Table_2 to JSON
        JsonObject table_2 = new JsonObject();
        int counter = 1;
        for (String ratio:
                ratios) {
            JsonObject temp = new JsonObject();
            if(!ratio.isEmpty()){
                temp.addProperty("Conc.(Ratio)", ratio);
                table_2.add(String.valueOf(counter), temp);
                section.add("table_2", table_2);
                counter++;
            }
        }

        content.add(section);
        jsonObject.add("content", content);
        writeJSON(metadata, jsonObject);
    }

    /**
     * Method to check for important keywords. Called to check if parse() should iter to next line
     * @param line line of text to check for important keywords
     * @return true if does not contain, else false
     */
    private boolean shouldNextLine(String line) {
        return !line.equals("Ret. Time") && !line.equals("Area") && !Util_functions.customContains(line, new String[]{"Area%", "Area %"})
                && !line.equals("Name") && !line.equals("Conc.") && !line.equals("Unit") && !line.equals("Resolution(USP)")
                && !line.equals("Res.(USP)") && !line.equals("S/N") && !line.equals("Height") && !line.equals("Peak End")
                && !line.equals("Area/Height");
    }

    /**
     * Method called to collect key/value type data from non-table parts of document
     * @param data Json object to write data to
     * @param line line to check for data
     * @param scanner to iter to next line, if data continues on next line
     */
    private void get_other_data(JsonObject data, String line, Scanner scanner) {
        String[] temp_line;
        if(line.contains(":")) {
            String second_key = "";
            temp_line = line.trim().split(":");
            if (!hasDigit(temp_line[0])) {
                if (temp_line.length == 2) {
                    if(temp_line[0].trim().equals("Comment")){
                        line = scanner.nextLine();
                        while(scanner.hasNextLine()){
                            if(line.trim().equals("Chromatogram") || line.trim().equals("<<Column>>"))
                                break;
                            temp_line[1] += line.trim();
                            temp_line[1] += " ";
                            line = scanner.nextLine();
                        }
                    }
                    data.addProperty(temp_line[0].trim(), temp_line[1].trim());
                } else if (temp_line.length > 2) {
                    switch (temp_line[0].trim()) {
                        case "Vial #":
                            data.addProperty(temp_line[0].trim(), temp_line[1].trim().split(" ")[0].trim());
                            second_key = temp_line[1].replace(temp_line[1].trim().split(" ")[0], "");
                            data.addProperty(second_key.trim(), temp_line[2].trim());
                            break;
                        case "Date Acquired":
                        case "Date Processed":
                        case "Data Processed":
                        case "Data Acquired":
                            String[] help;
                            if (line.contains("PM") || line.contains("AM")) {
                                String[] date;
                                help = line.split("(?<=[(P|A)]M)");
                                date = help[0].split(":");
                                String[] second = help[1].split(":");
                                data.addProperty(date[0].trim(), help[0].replace(date[0], "").replace(": ", "").trim());
                                data.addProperty(second[0].trim(), second[1].trim());
                                break;
                            }
                            else{
                                help = line.split("(?<=[:])");
                                data.addProperty(temp_line[0].trim(), line.replace(help[0], "").trim());
                                break;
                            }
                        case "Description":
                            data.addProperty(temp_line[0].trim(), temp_line[1].concat(temp_line[2]).trim()); // klasika riesenie
                            break;
                        default:
                            data.addProperty(temp_line[0].trim(), temp_line[1].trim().split(" ")[0].trim());
                            break;
                    }
                }else {
                    data.addProperty(temp_line[0].trim(), "");
                }
            }
        }
    }
}
