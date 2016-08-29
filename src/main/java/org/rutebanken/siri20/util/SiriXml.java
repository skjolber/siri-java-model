package org.rutebanken.siri20.util;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.xml.sax.SAXException;
import uk.org.siri.siri20.Siri;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.net.URL;

public class SiriXml {

    private static JAXBContext jaxbContext;

    static {
        try {
            init();
        } catch (JAXBException e) {
            throw new InstantiationError();
        }
    }

    private static void init() throws JAXBException {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(Siri.class);
        }
    }

    public static Siri parseXml(String xml) throws JAXBException {
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        return (Siri) jaxbUnmarshaller.unmarshal(new StringReader(xml));
    }

    public static String toXml(Siri siri) throws JAXBException {
        return toXml(siri, null);
    }

    public static String toXml(Siri siri, NamespacePrefixMapper customNamespacePrefixMapper) throws JAXBException {

        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        if (customNamespacePrefixMapper != null) {
            jaxbMarshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", customNamespacePrefixMapper);
        }
        StringWriter sw = new StringWriter();
        jaxbMarshaller.marshal(siri, sw);

        return sw.toString();
    }

    public static void toXml(Siri siri, NamespacePrefixMapper customNamespacePrefixMapper, OutputStream out) throws JAXBException, IOException {
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        if (customNamespacePrefixMapper != null) {
            jaxbMarshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", customNamespacePrefixMapper);
        }
        jaxbMarshaller.marshal(siri, out);
        out.flush();
        out.close();
    }

    /**
     * Validates xml against xsd
     *
     * Result is printed to System.out
     *
     * @param xml
     * @param version
     * @return
     * @throws JAXBException
     * @throws SAXException
     */
    public static boolean validate(String xml, VERSION version) throws JAXBException, SAXException {
        return validate(xml, version, System.out);
    }

    /**
     * Validates xml against xsd
     *
     * Result is printed to provided PrintStream
     *
     * @param xml
     * @param version
     * @param out
     * @return
     * @throws JAXBException
     * @throws SAXException
     */
    public static boolean validate(String xml, VERSION version, PrintStream out) throws JAXBException, SAXException {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        Schema schema = sf.newSchema(getXsdRelativePath(version));

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setSchema(schema);
        SiriValidationEventHandler handler = new SiriValidationEventHandler();
        unmarshaller.setEventHandler(handler);
        Siri validSiri = (Siri) unmarshaller.unmarshal(new StringReader(xml));

        out.println("Found " + handler.events.size() + " errors");

        handler.events.forEach(event -> {
            out.println();
            out.println("EVENT");
            out.println("SEVERITY:  " + event.getSeverity());
            out.println("MESSAGE:  " + event.getMessage());
            out.println("LINKED EXCEPTION:  " + event.getLinkedException());
            out.println("LOCATOR");
            out.println("    LINE NUMBER:  " + event.getLocator().getLineNumber());
            out.println("    COLUMN NUMBER:  " + event.getLocator().getColumnNumber());
            out.println("    OFFSET:  " + event.getLocator().getOffset());
            out.println("    OBJECT:  " + event.getLocator().getObject());
            out.println("    NODE:  " + event.getLocator().getNode());
            out.println("    URL:  " + event.getLocator().getURL());
        });

        return handler.events.size() == 0;

    }

    private static URL getXsdRelativePath(VERSION version) {

        String path = "";
        switch (version) {
            case VERSION_1_0:
                path = "siri-1.0/xsd/siri.xsd";
                break;
            case VERSION_1_3:
                path = "siri-1.3/xsd/siri.xsd";
                break;
            case VERSION_1_4:
                path = "siri-1.4/xsd/siri.xsd";
                break;
            case VERSION_2_0:
            default:
            path = "siri-2.0/xsd/siri.xsd";
        }

        return new SiriXml().getClass().getClassLoader().getResource(path);
    }

    public static enum VERSION {VERSION_1_0, VERSION_1_3, VERSION_1_4, VERSION_2_0}

}

