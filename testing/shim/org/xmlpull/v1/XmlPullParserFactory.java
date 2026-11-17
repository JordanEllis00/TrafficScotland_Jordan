package org.xmlpull.v1;
/** TEST SHIM: StAX-backed stand-in for Android's built-in XmlPull implementation (non-namespace-aware, raw "prefix:name" like kxml2). */
public class XmlPullParserFactory {
    public static XmlPullParserFactory newInstance() throws XmlPullParserException { return new XmlPullParserFactory(); }
    public void setNamespaceAware(boolean b) {}
    public XmlPullParser newPullParser() throws XmlPullParserException { return new StaxPullParser(); }
}
