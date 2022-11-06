package re.parsers.csv;

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

public class SpecordCSVParser extends ReParser {

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
        JsonObject json = new JsonObject();
        JsonObject data = new JsonObject();
        JsonObject table = new JsonObject();
        JsonObject section = new JsonObject();
        JsonArray content = new JsonArray();
        Scanner scanner = new Scanner(contentHandler.toString());
        String[] units = new String[0];
        while(scanner.hasNextLine()){
            String line = scanner.nextLine();
            line = checkLine(line);
            String[] splitted;
            if(line.equals("[nm];[A];")){
                units = line.split(";");
                units[0] = units[0].replace("[", "").replace("]","");
                units[1] = units[1].replace("[", "").replace("]","");
                data.addProperty("Units", line);
            }
            else if(line.contains("Sample")){
                data.addProperty("Sample#", line.split(";")[1]);
                while(scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    splitted = line.trim().split(";");
                    if (splitted.length == 2)
                        table.addProperty(splitted[0] + " " + units[0], splitted[1] + " " + units[1]);
                }
            }
            else if(line.contains("Measurement")){
                String[] keys = line.trim().split(";");
                JsonObject help = new JsonObject();
                line = scanner.nextLine();
                String[] values = line.trim().split(";");
                for (int i = 1; i < values.length; i++) {
                    help.addProperty(keys[i], values[i] + " " + (units.length == 2 ? units[1] : ""));
                }
                table.add(values[0] + " " + units[0], help);
            }
        }
        section.add("data", data);
        section.add("table", table);
        content.add(section);
        json.add("content", content);
        writeJSON(metadata, json);
    }

}
