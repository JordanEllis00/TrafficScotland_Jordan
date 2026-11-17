package org.xmlpull.v1;
/** TEST SHIM (JVM harness only) - mirrors the subset of Android's XmlPullParser API used by DatexXmlParser. */
public interface XmlPullParser {
    int START_DOCUMENT = 0, END_DOCUMENT = 1, START_TAG = 2, END_TAG = 3, TEXT = 4;
    void setInput(java.io.Reader r) throws XmlPullParserException;
    int getEventType() throws XmlPullParserException;
    int next() throws XmlPullParserException, java.io.IOException;
    String getName();
    int getAttributeCount();
    String getAttributeName(int i);
    String getAttributeValue(int i);
    String nextText() throws XmlPullParserException, java.io.IOException;
}
