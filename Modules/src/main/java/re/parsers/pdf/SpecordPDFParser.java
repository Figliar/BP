package re.parsers.pdf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import re.parsers.service.ReParser;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static re.parsers.Util_functions.checkLine;
import static re.parsers.Util_functions.writeJSON;

public class SpecordPDFParser extends ReParser {

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
        JsonObject table = new JsonObject();
        JsonObject section = new JsonObject();
        JsonArray content = new JsonArray();
        String choice = "";
        JsonObject data = new JsonObject();
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = checkLine(line);
            if(line.contains("Meas. mode")){
                String[] head = line.split("Meas\\. mode");
                choice = head[1].trim();
                data.addProperty("Meas. mode", head[1].trim());
                section.add("data", data);
                while(!line.isEmpty()) {
                    line = scanner.nextLine();
                    head = line.split("(?<=])");
                    if(head.length == 2) {
                        data.addProperty(head[0].trim(), head[1].trim());
                    }
                }
                while(!line.contains("Parameters")) {
                    line = scanner.nextLine();
                    head = line.split(" ");
                    if(head[0].equals("Number")) {
                        data.addProperty(head[0].trim(), head[1].trim());
                    }
                    else if(head[0].equals("Cycle")){
                        data.addProperty(head[0] + " " + head[1], head[2]);
                    }
                }
            }
            else if(choice.equals("Spectral Scan")){
                while(scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    if(line.contains(":") && line.contains("A")) {
                        String[] splitted = line.split("A");
                        for (String splitt:
                                splitted) {
                            table.addProperty(splitt.split(":")[0].trim(), splitt.split(":")[1].concat("A").trim());
                            section.add("table", table);
                        }
                    }
                }
            }
            else if(choice.trim().equals("Wavelengths")){
                String key = "", value = "";
                while(scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    if(line.trim().contains("Measurement")){
                        key = line.trim();
                    }
                    if(line.contains(":") && line.contains("A")) {
                        value = line.trim();
                    }
                    if(!key.isEmpty()) {
                        table.addProperty(key, value);
                        section.add("table", table);
                    }
                }
            }
            else{
                String[] splitted = line.split(" ");
                if(splitted.length > 1) {
                    if (splitted.length == 2){
                        data.addProperty(splitted[0].trim(), splitted[1].trim());
                    }
                    else {
                        String key = "";
                        String value = "";
                        switch (splitted[0]) {
                            case "File":
                                key = splitted[0] + " " + splitted[1];
                                value = String.join(" ", splitted).replace(key, "");
                                break;
                            case "Designation":
                            case "Title":
                                key = splitted[0];
                                value = String.join(" ", splitted).replace(key, "");
                                break;
                            case "Date/Time":
                            case "Operator":
                                key = splitted[0];
                                value = String.join(" ", splitted).replace(splitted[0], "");
                                break;
                            case "Lamp":
                                key = splitted[0] + " " + splitted[1] + " " + splitted[2];
                                value = String.join(" ", splitted).replace(key, "");
                                break;
                        }
                        data.addProperty(key.trim(), value.trim());
                    }
                }
            }
        }
        section.add("data", data);
        content.add(section);
        jsonObject.add("content", content);
        writeJSON(metadata, jsonObject);
    }
}
