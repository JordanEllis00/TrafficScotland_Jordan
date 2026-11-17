package org.xmlpull.v1;
import javax.xml.stream.*;
import java.io.Reader;
class StaxPullParser implements XmlPullParser {
    private XMLStreamReader r; private int ev = START_DOCUMENT;
    public void setInput(Reader in) throws XmlPullParserException {
        try {
            XMLInputFactory f = XMLInputFactory.newInstance();
            f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
            f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
            f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            r = f.createXMLStreamReader(in);
        } catch (XMLStreamException e) { throw new XmlPullParserException("setInput", e); }
    }
    public int getEventType() { return ev; }
    public int next() throws XmlPullParserException {
        try {
            while (true) {
                if (!r.hasNext()) { ev = END_DOCUMENT; return ev; }
                switch (r.next()) {
                    case XMLStreamConstants.START_ELEMENT: return ev = START_TAG;
                    case XMLStreamConstants.END_ELEMENT: return ev = END_TAG;
                    case XMLStreamConstants.CHARACTERS: case XMLStreamConstants.CDATA: case XMLStreamConstants.SPACE: return ev = TEXT;
                    case XMLStreamConstants.END_DOCUMENT: return ev = END_DOCUMENT;
                    default: continue;
                }
            }
        } catch (XMLStreamException ex) { throw new XmlPullParserException("next: " + ex.getMessage(), ex); }
    }
    /** Raw qualified name as kxml2 reports it when not namespace aware, e.g. "d2:situation". */
    public String getName() { return (ev==START_TAG||ev==END_TAG) ? r.getLocalName() : null; }
    public int getAttributeCount() { return r.getAttributeCount(); }
    public String getAttributeName(int i) {
        String p = r.getAttributePrefix(i), l = r.getAttributeLocalName(i);
        return (p == null || p.isEmpty()) ? l : p + ":" + l;
    }
    public String getAttributeValue(int i) { return r.getAttributeValue(i); }
    public String nextText() throws XmlPullParserException {
        if (ev != START_TAG) throw new XmlPullParserException("parser must be on START_TAG to read next text");
        try {
            StringBuilder sb = new StringBuilder();
            int e = next();
            while (e == TEXT) { sb.append(r.getText()); e = next(); }
            if (e != END_TAG) throw new XmlPullParserException("END_TAG expected");
            return sb.toString();
        } catch (XmlPullParserException ex) { throw ex; }
    }
}
