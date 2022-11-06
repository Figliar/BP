package re.parsers.service;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public abstract class ReParser {

    /**
     * @return Name of the service provider (name of the class that implements this interface)
     */
    public abstract String getType();

    /**
     * @param inputStream stream of the document to be parsed
     * @param contentHandler text content handler of parsed document
     * @param metadata metadata of parsed document
     * @throws IOException in case of malformed input
     * @throws SAXException in case of XML related exception
     * @throws TikaException for Tika related exceptions
     */
    public abstract void parse(InputStream inputStream, ContentHandler contentHandler, Metadata metadata) throws IOException, SAXException, TikaException;

}
