package re.parsers.pdf;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.sax.ToTextContentHandler;
import org.json.simple.parser.ParseException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import re.parsers.Util_functions;
import re.parsers.classifier.Classifier;
import re.parsers.config.ReConfig;
import re.parsers.service.ReParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static re.parsers.Util_functions.validateOutput;

public class PDFDecider extends AbstractParser {

    private static final MediaType MEDIA_TYPE = MediaType.application("pdf");

    private static final Set<MediaType> SUPPORTED_TYPES = Collections.singleton(MEDIA_TYPE);

    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return SUPPORTED_TYPES;
    }

    @Override
    public void parse(InputStream inputStream, ContentHandler contentHandler, Metadata metadata, ParseContext parseContext) throws IOException, SAXException, TikaException {
        ContentHandler handler = new ToTextContentHandler();
        ContentHandler tee = new TeeContentHandler(handler, contentHandler);
        PDFParser pdfParser = new PDFParser();
        ReConfig reConfig = new ReConfig();
        Classifier classifier = new Classifier();

        pdfParser.parse(inputStream, tee, metadata, parseContext);
        Util_functions.validateRules(reConfig);
        String type = classifier.decideJSON(handler.toString(), metadata, "pdf", reConfig.getRules());

        ServiceLoader<ReParser> loader = ServiceLoader.load(ReParser.class);
        Iterator<ReParser> iter = loader.iterator();

        while(iter.hasNext()){
            try {
                ReParser p = iter.next();
                if (p.getType().equals(type)) {
                    p.parse(inputStream, handler, metadata);
                    validateOutput(metadata, reConfig);
                    break;
                }
            }
            catch (IOException | TikaException | SAXException | NoSuchElementException | ParseException e) {
                e.printStackTrace();
            }
        }
    }
}
