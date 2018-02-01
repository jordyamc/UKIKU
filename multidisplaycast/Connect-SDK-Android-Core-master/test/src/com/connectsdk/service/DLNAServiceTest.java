package com.connectsdk.service;

import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.core.TestUtil;
import com.connectsdk.discovery.provider.ssdp.Service;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.upnp.DLNAHttpServer;

import junit.framework.Assert;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.frolov on 1/13/2015.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class DLNAServiceTest {

    private DLNAService service;

    private DLNAHttpServer dlnaServer;

    @Before
    public void setUp() {
        dlnaServer = Mockito.mock(DLNAHttpServer.class);
        service = new DLNAService(Mockito.mock(ServiceDescription.class),
                Mockito.mock(ServiceConfig.class), Robolectric.application, dlnaServer);
    }

    @Test
    public void testParseData() {
        String tag = "TrackDuration";
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "   <SOAP-ENV:Body>\n" +
                "      <m:GetPositionInfoResponse xmlns:m=\"urn:schemas-upnp-org:service:AVTransport:1\">\n" +
                "         <Track xmlns:dt=\"urn:schemas-microsoft-com:datatypes\" dt:dt=\"ui4\">1</Track>\n" +
                "         <TrackDuration xmlns:dt=\"urn:schemas-microsoft-com:datatypes\" dt:dt=\"string\">0:00:52</TrackDuration>\n" +
                "         <TrackMetaData xmlns:dt=\"urn:schemas-microsoft-com:datatypes\" dt:dt=\"string\">&lt;DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"&gt;&lt;item id=\"1000\" parentID=\"0\" restricted=\"0\"&gt;&lt;dc:title&gt;Sintel Trailer&lt;/dc:title&gt;&lt;dc:description&gt;Blender Open Movie Project&lt;/dc:description&gt;&lt;res protocolInfo=\"http-get:*:video/mp4:DLNA.ORG_OP=01\"&gt;http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/video.mp4&lt;/res&gt;&lt;upnp:albumArtURI&gt;http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/videoIcon.jpg&lt;/upnp:albumArtURI&gt;&lt;upnp:class&gt;object.item.videoItem&lt;/upnp:class&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;</TrackMetaData>\n" +
                "         <TrackURI xmlns:dt=\"urn:schemas-microsoft-com:datatypes\" dt:dt=\"string\">http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/video.mp4</TrackURI>\n" +
                "         <RelTime xmlns:dt=\"urn:schemas-microsoft-com:datatypes\" dt:dt=\"string\">0:00:00</RelTime>\n" +
                "         <AbsTime xmlns:dt=\"urn:schemas-microsoft-com:datatypes\" dt:dt=\"string\">NOT_IMPLEMENTED</AbsTime>\n" +
                "         <RelCount xmlns:dt=\"urn:schemas-microsoft-com:datatypes\" dt:dt=\"i4\">2147483647</RelCount>\n" +
                "         <AbsCount xmlns:dt=\"urn:schemas-microsoft-com:datatypes\" dt:dt=\"i4\">2147483647</AbsCount>\n" +
                "      </m:GetPositionInfoResponse>\n" +
                "   </SOAP-ENV:Body>\n" +
                "</SOAP-ENV:Envelope>";
        String value = service.parseData(response, tag);
        Assert.assertEquals("0:00:52", value);
    }

    @Test
    public void testParseDataWithError() {
        String tag = "errorCode";
        String response = "<?xml version=\"1.0\"?>\n<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><SOAP-ENV:Body><SOAP-ENV:Fault><faultcode>SOAP-ENV:Client</faultcode><faultstring>UPnPError</faultstring><detail><u:UPnPError xmlns:u=\"urn:schemas-upnp-org:control-1-0\"><u:errorCode>402</u:errorCode><u:errorDescription>Invalid Args</u:errorDescription></u:UPnPError></detail></SOAP-ENV:Fault></SOAP-ENV:Body></SOAP-ENV:Envelope>";
        String value = service.parseData(response, tag);
        Assert.assertEquals("402", value);
    }

    @Test
    public void testParseData3Symbols() {
        String tag = "errorCode";
        String response = "&lt";
        String value = null;
        try {
            value = service.parseData(response, tag);
        } catch (Exception e) {
            Assert.fail("exception thrown: " + e);
        }
        Assert.assertEquals("", value);
    }

    @Test
    public void testGetMetadata() throws Exception {
        String title = "<title>";
        String description = "description";
        String mime = "audio/mpeg";
        String mediaURL = "http://host.com/media";
        String iconURL = "http://host.com/icon";

        String expectedXML = "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:sec=\"http://www.sec.co.kr/\" " +
                "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">" +
                "<item id=\"1000\" parentID=\"0\" restricted=\"0\">" +
                "<dc:title>&lt;title&gt;</dc:title>" +
                "<dc:description>" + description + "</dc:description>" +
                "<res protocolInfo=\"http-get:*:audio/mpeg:DLNA.ORG_OP=01\">" + mediaURL + "</res>" +
                "<upnp:albumArtURI>" + iconURL + "</upnp:albumArtURI>" +
                "<upnp:class>object.item.audioItem</upnp:class><" +
                "/item></DIDL-Lite>";

        String actualXML = service.getMetadata(mediaURL, null, mime, title, description, iconURL);
        assertXMLEquals(expectedXML, actualXML);
    }

    @Test
    public void testGetMessageXml() throws Exception {
        String method = "GetPosition";
        String serviceURN = "http://serviceurn/";

        String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
        "<s:Body>" +
        "<u:" + method + " xmlns:u=\"" + serviceURN + "\">" +
        "<key>value</key>" +
        "</u:" + method + ">" +
        "</s:Body>" +
        "</s:Envelope>";

        Map<String, String> params = new HashMap<String, String>();
        params.put("key", "value");
        String actualXML = service.getMessageXml(serviceURN, method, null, params);
        assertXMLEquals(expectedXML, actualXML);
    }

    @Test
    public void testGetMessageXmlWithMetadataWithAllParametersExceptSubtitle() throws Exception {
        String method = "SetAVTransportUri";
        String serviceURN = "http://serviceurn/";

        String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body>" +
                "<u:" + method + " xmlns:u=\"" + serviceURN + "\">" +
                "<CurrentURIMetaData>" +
                "&lt;DIDL-Lite " +
                "xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
                "xmlns:sec=\"http://www.sec.co.kr/\" " +
                "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\"&gt;" +
                "&lt;item id=\"1000\" parentID=\"0\" restricted=\"0\"&gt;" +
                "&lt;dc:title&gt;&amp;amp;\"title\"&lt;/dc:title&gt;" +
                "&lt;dc:description&gt;&amp;amp;&lt;/dc:description&gt;" +
                "&lt;res protocolInfo=\"http-get:*:audio/mpeg:DLNA.ORG_OP=01\"&gt;http://url/t&amp;amp;t&lt;/res&gt;" +
                "&lt;upnp:albumArtURI&gt;http://host/image&lt;/upnp:albumArtURI&gt;" +
                "&lt;upnp:class&gt;object.item.audioItem&lt;/upnp:class&gt;" +
                "&lt;/item&gt;&lt;/DIDL-Lite&gt;" +
                "</CurrentURIMetaData>" +
                "</u:" + method + "></s:Body></s:Envelope>";

        String metadata = service.getMetadata("http://url/t&t", null, "audio/mpeg", "&\"title\"", "&", "http://host/image");
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("CurrentURIMetaData", metadata);

        String actualXML = service.getMessageXml(serviceURN, method, null, params);
        assertXMLEquals(expectedXML, actualXML);
    }

    @Test
    public void testGetMessageXmlWithMetadataWithAllParameters() throws Exception {
        String method = "SetAVTransportUri";
        String serviceURN = "http://serviceurn/";
        String subtitleType = "text/vtt";
        String subtitleSubType = "vtt";
        SubtitleInfo subtitle = new SubtitleInfo.Builder("http://subtitleurl")
                .setMimeType(subtitleType)
                .setLabel("label")
                .setLanguage("en")
                .build();
        String mediaUrl = "http://mediaurl/";
        String mediaType = "audio/mp3";
        String title = "&\"title";
        String description = "description";
        String iconUrl = "http://iconurl/";

        String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body>" +
                "<u:" + method + " xmlns:u=\"" + serviceURN + "\">" +
                "<CurrentURIMetaData>" +
                "&lt;DIDL-Lite " +
                "xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
                "xmlns:sec=\"http://www.sec.co.kr/\" " +
                "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\"&gt;" +
                "&lt;item id=\"1000\" parentID=\"0\" restricted=\"0\"&gt;" +
                "&lt;dc:title&gt;&amp;amp;\"title&lt;/dc:title&gt;" +
                "&lt;dc:description&gt;description&lt;/dc:description&gt;" +
                "&lt;res xmlns:pv=\"http://www.pv.com/pvns/\" " +
                "protocolInfo=\"http-get:*:"+mediaType+":DLNA.ORG_OP=01\" " +
                "pv:subtitleFileType=\""+subtitleSubType+"\" " +
                "pv:subtitleFileUri=\""+subtitle.getUrl()+"\"&gt;"+mediaUrl+"&lt;/res&gt;" +
                "&lt;upnp:albumArtURI&gt;"+iconUrl+"&lt;/upnp:albumArtURI&gt;" +
                "&lt;upnp:class&gt;object.item.audioItem&lt;/upnp:class&gt;" +
                "&lt;res protocolInfo=\"http-get:*:smi/caption\"&gt;"+subtitle.getUrl()+"&lt;/res&gt;" +
                "&lt;res protocolInfo=\"http-get:*:"+subtitle.getMimeType()+":\"&gt;"+subtitle.getUrl()+"&lt;/res&gt;" +
                "&lt;sec:CaptionInfoEx sec:type=\""+subtitleSubType+"\"&gt;"+subtitle.getUrl()+"&lt;/sec:CaptionInfoEx&gt;" +
                "&lt;sec:CaptionInfo sec:type=\""+subtitleSubType+"\"&gt;"+subtitle.getUrl()+"&lt;/sec:CaptionInfo&gt;" +
                "&lt;/item&gt;&lt;/DIDL-Lite&gt;" +
                "</CurrentURIMetaData>" +
                "</u:" + method + "></s:Body></s:Envelope>";

        String metadata = service.getMetadata(mediaUrl, subtitle, mediaType, title, description, iconUrl);
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("CurrentURIMetaData", metadata);

        String actualXML = service.getMessageXml(serviceURN, method, null, params);
        assertXMLEquals(expectedXML, actualXML);
    }

    @Test
    public void testGetMessageXmlWithMetadataWithSubtitleUrl() throws Exception {
        String method = "SetAVTransportUri";
        String serviceURN = "http://serviceurn/";
        String subtitleType = "text/srt";
        String subtitleSubType = "srt";
        SubtitleInfo subtitle = new SubtitleInfo.Builder("http://subtitleurl")
                .build();
        String mediaUrl = "http://mediaurl/";
        String mediaType = "audio/mp3";
        String title = "&\"title";
        String description = "description";
        String iconUrl = "http://iconurl/";

        String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body>" +
                "<u:" + method + " xmlns:u=\"" + serviceURN + "\">" +
                "<CurrentURIMetaData>" +
                "&lt;DIDL-Lite " +
                "xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
                "xmlns:sec=\"http://www.sec.co.kr/\" " +
                "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\"&gt;" +
                "&lt;item id=\"1000\" parentID=\"0\" restricted=\"0\"&gt;" +
                "&lt;dc:title&gt;&amp;amp;\"title&lt;/dc:title&gt;" +
                "&lt;dc:description&gt;description&lt;/dc:description&gt;" +
                "&lt;res xmlns:pv=\"http://www.pv.com/pvns/\" " +
                "protocolInfo=\"http-get:*:"+mediaType+":DLNA.ORG_OP=01\" " +
                "pv:subtitleFileType=\""+subtitleSubType+"\" " +
                "pv:subtitleFileUri=\""+subtitle.getUrl()+"\"&gt;"+mediaUrl+"&lt;/res&gt;" +
                "&lt;upnp:albumArtURI&gt;"+iconUrl+"&lt;/upnp:albumArtURI&gt;" +
                "&lt;upnp:class&gt;object.item.audioItem&lt;/upnp:class&gt;" +
                "&lt;res protocolInfo=\"http-get:*:smi/caption\"&gt;"+subtitle.getUrl()+"&lt;/res&gt;" +
                "&lt;res protocolInfo=\"http-get:*:"+subtitleType+":\"&gt;"+subtitle.getUrl()+"&lt;/res&gt;" +
                "&lt;sec:CaptionInfoEx sec:type=\""+subtitleSubType+"\"&gt;"+subtitle.getUrl()+"&lt;/sec:CaptionInfoEx&gt;" +
                "&lt;sec:CaptionInfo sec:type=\""+subtitleSubType+"\"&gt;"+subtitle.getUrl()+"&lt;/sec:CaptionInfo&gt;" +
                "&lt;/item&gt;&lt;/DIDL-Lite&gt;" +
                "</CurrentURIMetaData>" +
                "</u:" + method + "></s:Body></s:Envelope>";

        String metadata = service.getMetadata(mediaUrl, subtitle, mediaType, title, description, iconUrl);
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("CurrentURIMetaData", metadata);

        String actualXML = service.getMessageXml(serviceURN, method, null, params);
        assertXMLEquals(expectedXML, actualXML);
    }

    @Test
    public void testGetMessageXmlWithMetadataWithRequiredParameters() throws Exception {
        String method = "SetAVTransportUri";
        String serviceURN = "http://serviceurn/";

        String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body>" +
                "<u:" + method + " xmlns:u=\"" + serviceURN + "\">" +
                "<CurrentURIMetaData>" +
                "&lt;DIDL-Lite " +
                "xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
                "xmlns:sec=\"http://www.sec.co.kr/\" " +
                "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\"&gt;" +
                "&lt;item id=\"1000\" parentID=\"0\" restricted=\"0\"&gt;" +
                "&lt;dc:title/&gt;" +
                "&lt;dc:description/&gt;" +
                "&lt;res protocolInfo=\"http-get:*:audio/mpeg:DLNA.ORG_OP=01\"&gt;http://url/t&amp;amp;t&lt;/res&gt;" +
                "&lt;upnp:albumArtURI/&gt;" +
                "&lt;upnp:class&gt;object.item.audioItem&lt;/upnp:class&gt;" +
                "&lt;/item&gt;&lt;/DIDL-Lite&gt;" +
                "</CurrentURIMetaData>" +
                "</u:" + method + "></s:Body></s:Envelope>";

        String metadata = service.getMetadata("http://url/t&t", null, "audio/mpeg", null, null, null);
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("CurrentURIMetaData", metadata);

        String actualXML = service.getMessageXml(serviceURN, method, null, params);
        assertXMLEquals(expectedXML, actualXML);
    }

    @Test
    public void testUrlEncode() throws Exception {
        String expected = "http://192.168.1.100:8000/ph&o't'o%20with%20symbols.jpg";
        String urlStr = "http://192.168.1.100:8000/ph&o't'o with symbols.jpg";
        Assert.assertEquals(expected, service.encodeURL(urlStr));
    }

    @Test
    public void testUrlEncodeAlreadyEncoded() throws Exception {
        String expected = "http://192.168.1.100:8000/ph&o't'o%20with%20symbols.jpg";
        String urlStr = "http://192.168.1.100:8000/ph&o't'o%20with%20symbols.jpg";
        Assert.assertEquals(expected, service.encodeURL(urlStr));
    }

    @Test
    public void testNullUrlEncode() throws Exception {
        Assert.assertEquals("", service.encodeURL(null));
    }


    @Test
    public void testEmptyUrlEncode() throws Exception {
        Assert.assertEquals("", service.encodeURL(""));
    }

    @Test
    public void testServiceControlURL() {
        DLNAService dlnaService = makeServiceWithControlURL("http://192.168.1.0/", "/controlURL");
        Assert.assertEquals("http://192.168.1.0/controlURL", dlnaService.avTransportURL);
    }

    @Test
    public void testServiceControlURLWithWrongBase() {
        DLNAService dlnaService = makeServiceWithControlURL("http://192.168.1.0", "/controlURL");
        Assert.assertEquals("http://192.168.1.0/controlURL", dlnaService.avTransportURL);
    }

    @Test
    public void testServiceControlURLWithWrongControlURL() {
        DLNAService dlnaService = makeServiceWithControlURL("http://192.168.1.0/", "controlURL");
        Assert.assertEquals("http://192.168.1.0/controlURL", dlnaService.avTransportURL);
    }

    @Test
    public void testServiceControlURLWithWrongBaseAndControlURL() {
        DLNAService dlnaService = makeServiceWithControlURL("http://192.168.1.0", "controlURL");
        Assert.assertEquals("http://192.168.1.0/controlURL", dlnaService.avTransportURL);
    }

    @Test
    public void testInitialPairingType() {
        Assert.assertEquals(DeviceService.PairingType.NONE, service.getPairingType());
    }

    @Test
    public void testPairingTypeSetter() {
        service.setPairingType(DeviceService.PairingType.PIN_CODE);
        Assert.assertEquals(DeviceService.PairingType.NONE, service.getPairingType());
    }

    @Test
    public void testTimeToLongNullValue() {
        Assert.assertEquals(0L, service.convertStrTimeFormatToLong(null));
    }

    @Test
    public void testTimeToLongWrongValue() {
        Assert.assertEquals(0L, service.convertStrTimeFormatToLong("abc"));
    }

    @Test
    public void testTimeToLongZeroValue() {
        Assert.assertEquals(0L, service.convertStrTimeFormatToLong("00:00:00"));
    }

    @Test
    public void testTimeToLong() {
        Assert.assertEquals(10000L, service.convertStrTimeFormatToLong("00:00:10"));
    }

    @Test
    public void testTimeToLong12Hours() {
        Assert.assertEquals(43200000L, service.convertStrTimeFormatToLong("12:00:00"));
    }

    @Test
    public void testTimeToLong20Hours() {
        Assert.assertEquals(72000000L, service.convertStrTimeFormatToLong("20:00:00"));
    }

    @Test
    public void testTimeToLongBigValue() {
       Assert.assertEquals(432000000L, service.convertStrTimeFormatToLong("120:00:00"));
    }

    @Test
    public void testStopDLNAServerOnDisconnect() {
        service.disconnect();
        TestUtil.runUtilBackgroundTasks();
        Mockito.verify(dlnaServer).stop();
    }

    @Test
    public void testTimeToLongWithMilliseconds() {
        Assert.assertEquals(43200000L, service.convertStrTimeFormatToLong("12:00:00.777"));
    }

    @Test
    public void testTimeToLongWithInvalidArguments() {
        try {
            Assert.assertEquals(0L, service.convertStrTimeFormatToLong("01.210"));
            Assert.assertEquals(0L, service.convertStrTimeFormatToLong("00:01.210"));
            Assert.assertEquals(0L, service.convertStrTimeFormatToLong("Not a number"));
        } catch (Exception e) {
            Assert.fail("convertStrTimeFormatToLong must not throw an exception");
        }
    }

    @Test
    public void testMakeControlURL() {
        Assert.assertEquals("base/path", service.makeControlURL("base/", "path"));
    }

    @Test
    public void testMakeControlURLWithNullBase() {
        Assert.assertNull(service.makeControlURL(null, "path"));
    }

    @Test
    public void testMakeControlURLWithNullPath() {
        Assert.assertNull(service.makeControlURL("base", null));
    }

    private DLNAService makeServiceWithControlURL(String base, String controlURL) {
        List<Service> services = new ArrayList<Service>();
        Service service = new Service();
        service.baseURL = base;
        service.controlURL = controlURL;
        service.serviceType = DLNAService.AV_TRANSPORT;
        services.add(service);

        ServiceDescription description = Mockito.mock(ServiceDescription.class);
        Mockito.when(description.getServiceList()).thenReturn(services);
        return new DLNAService(description, Mockito.mock(ServiceConfig.class), Robolectric.application, null);
    }

    private void assertXMLEquals(String expectedXML, String actualXML) throws SAXException, IOException {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setNormalize(true);
        DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(expectedXML, actualXML));
        List<?> allDifferences = diff.getAllDifferences();
        Assert.assertEquals("XML differences found: " + diff.toString(), 0, allDifferences.size());
    }
}
