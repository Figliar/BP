package re.parsers.xlsx;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import re.parsers.service.ReParser;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static re.parsers.Util_functions.writeJSON;

public class AgilentXLSXParser extends ReParser {

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
        int y = 0;
        JsonObject jsonObject = new JsonObject();
        String page = "";

        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        Iterator<Sheet> iter = workbook.sheetIterator();
        Map<String, String> keys = new LinkedHashMap<String, String>();
        Map<String, String> values = new LinkedHashMap<String, String>();
        Cell mycell;
        JsonArray content = new JsonArray();

        while(iter.hasNext()){
            String[] temp;
            String full = "";
            page = "Page ".concat(String.valueOf(++y));
            Iterator<Row> iter_row = iter.next().rowIterator();
            JsonObject section = new JsonObject();
            section.addProperty("section_name", page);
            JsonObject data = new JsonObject();
            JsonObject table = new JsonObject();
            while(iter_row.hasNext()) {
                Iterator<Cell> iter_cell = iter_row.next().cellIterator();
                while (iter_cell.hasNext()) {
                    mycell = iter_cell.next();
                    if (mycell.toString().contains("Signal:")) {
                        mycell = iter_cell.next();
                        temp = mycell.getStringCellValue().split("\n");
                        full = String.join(" ", temp);
                        table.addProperty("Signal", full);
                        iter_cell = iter_row.next().cellIterator();
                        while (iter_cell.hasNext()) {
                            mycell = iter_cell.next();
                            temp = mycell.getStringCellValue().split("\n");
                            full = String.join(" ", temp);
                            keys.put(String.valueOf(mycell.getColumnIndex()), full);
                        }
                        int i = 0;
                        while (iter_row.hasNext()) {
                            boolean jump = false;
                            iter_cell = iter_row.next().cellIterator();
                            for (String key : keys.keySet()) {
                                values.put(key, "");
                            }
                            while (iter_cell.hasNext()) {
                                mycell = iter_cell.next();
                                temp = mycell.getStringCellValue().split("\n");
                                full = String.join(" ", temp);
                                values.put(String.valueOf(mycell.getColumnIndex()), full);
                                if(mycell.toString().contains("Sum")){
                                    jump = true;
                                }
                            }
                            if(jump){
                                table.addProperty("Area Sum", mycell.toString());
                                break;
                            }
                            JsonObject numbers = new JsonObject();
                            for (String key : keys.keySet()) {
                                numbers.addProperty(keys.get(key), values.get(key));
                            }
                            table.add(String.valueOf(++i), numbers);
                            section.add("table", table);
                        }
                    }
                    if(mycell.toString().replace("\n", "").contains(":")
                            || mycell.toString().replace("\n", " ").contains("Sample Description")){
                        String key = mycell.getStringCellValue().replace("\n", " ");
                        key = key.replace(":", "");
                        if(iter_cell.hasNext()) {
                            mycell = iter_cell.next();
                            String value = mycell.getStringCellValue().replace("\n", " ");
                            data.addProperty(key, value);
                        }else{
                            data.addProperty(key, "");
                        }
                        section.add("data", data);

                    }
                }
            }
            content.add(section);
        }
        jsonObject.add("content", content);
        writeJSON(metadata, jsonObject);
    }

}
