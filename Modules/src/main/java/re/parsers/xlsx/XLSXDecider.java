package re.parsers.xlsx;

import org.json.simple.parser.ParseException;
import re.parsers.classifier.Classifier;
import re.parsers.config.ReConfig;
import re.parsers.service.ReParser;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.sax.ToTextContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import re.parsers.Util_functions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class XLSXDecider extends AbstractParser {

    private static final MediaType MEDIA_TYPE = MediaType.application("vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final Set<MediaType> SUPPORTED_TYPES =
            Collections.singleton(MEDIA_TYPE);

    @Override
    public Set<MediaType> getSupportedTypes(ParseContext parseContext) {
        return SUPPORTED_TYPES;
    }

    @Override
    public void parse(InputStream inputStream, ContentHandler contentHandler, Metadata metadata, ParseContext parseContext) throws IOException, SAXException, TikaException {

        /*
          START
          This piece of code makes two input streams (in1, in2) out of one (inputStream)
          so it would be possible to go through sam stream twice
         */
        byte[] buffer = new byte[16000];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int len;
        while ((len = inputStream.read(buffer)) > -1) {
            outputStream.write(buffer, 0, len);
        }
        outputStream.flush();

        InputStream in1 = new ByteArrayInputStream(outputStream.toByteArray());
        InputStream in2 = new ByteArrayInputStream(outputStream.toByteArray());
        /*
          END
         */

        ContentHandler handler = new ToTextContentHandler();
        ContentHandler tee = new TeeContentHandler(handler, contentHandler);
        OOXMLParser ooxmlParser = new OOXMLParser();
        ReConfig reConfig = new ReConfig();
        Classifier classifier = new Classifier();

        ooxmlParser.parse(in1, tee, metadata, parseContext);
        Util_functions.validateRules(reConfig);
        String type = classifier.decideJSON(handler.toString(), metadata, "xlsx", reConfig.getRules());

        ServiceLoader<ReParser> loader = ServiceLoader.load(ReParser.class);
        Iterator<ReParser> iter = loader.iterator();

        while(iter.hasNext()){
            try {
                ReParser p = iter.next();
                if (p.getType().equals(type)) {
                    p.parse(in2, contentHandler, metadata);
                    Util_functions.validateOutput(metadata, reConfig);
                    break;
                }
            }
            catch (IOException | TikaException | SAXException | NoSuchElementException | ParseException e) {
                e.printStackTrace();
            }
        }
    }

}
