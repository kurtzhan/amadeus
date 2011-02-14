import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class AmadeusLib {
	private final String wsap1 = "1ASIWPOC1A";
	private final String wsap2 = "1ASIWTES1A";
	private final String readFilePath = "D:\\work\\offandaway\\test\\20110117";
	private final Hashtable<String, String> codes = new Hashtable<String, String>(){};
	private Hashtable<String, Document> responseXmlHash = new Hashtable<String, Document>();
	private List<Hotel> list = new LinkedList<Hotel>();
	
	private void InitCodesHash()
	{
		codes.put("Security_Authenticate", "VLSSLQ_06_1_1A");
		codes.put("Security_SignOut", "VLSSOQ_04_1_1A");
		codes.put("Command_Cryptic", "HSFREQ_06_2_1A");
		codes.put("Hotel_AvailabilityMultiProperties", "HMOREQ_06_1_1A");
		codes.put("Hotel_SingleAvailability", "HAVSRQ_03_1_1A");
	}
	
	private void addResponseXmlToHash(String queryType, String response) throws Exception
	{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		InputSource inputSource = new InputSource();
		inputSource.setCharacterStream(new StringReader(response));
		Document doc = dBuilder.parse(inputSource);
		responseXmlHash.put(queryType, doc);
		if(queryType.indexOf("Hotel_AvailabilityMultiProperties") != -1)
		{
		  NodeList nl = doc.getElementsByTagName("propertyName");
		  NodeList nll = doc.getElementsByTagName("propertyCode");
		  for(int i = 0; i < nl.getLength(); i++)
		  {
		    Hotel h = new Hotel();
		    h.name = nl.item(i).getTextContent();
		    h.id = nll.item(i).getTextContent();
			  list.add(h);
			}
		}
	}
	
	public List<Hotel> callAmadeus(String hotelName, String cityCode) throws Exception{
		InitCodesHash();
		String result1 = SecurityAuthenticate(wsap1, "WS1APOC", "NYCP02001", "MnJPRWJyaSZXb1V0", "12");
		addResponseXmlToHash("Security_Authenticate", result1);
		
		String sessionId1 = GetSessionId(result1);
//		System.out.println(sessionId1);
		String securityToken1 = GetSecurityToken(result1);
//		System.out.println(securityToken1);
//		System.out.println(result1);
/*
		String queryType = "Hotel_SingleAvailability";
		String result = SendQuery(wsap1, GetSessionBlock(sessionId1, securityToken1, 2), GetXml(queryType), queryType);
		System.out.println(result);
		addResponseXmlToHash("Hotel_SingleAvailability", result1);
*/	
		boolean scrolling = false;
		int numReq = 0;
		String nextItemIndex = "";
		String result = "";
		do
		{
			result = HotelAvailabilityMultiProperties(wsap1, GetSessionBlock(sessionId1, securityToken1, 2 + numReq), hotelName, cityCode, null, null, scrolling, nextItemIndex);
			addResponseXmlToHash("Hotel_AvailabilityMultiProperties" + numReq, result);
			Document doc = (Document)responseXmlHash.get("Hotel_AvailabilityMultiProperties" + numReq);
			System.out.println(doc.getElementsByTagName("displayResponse").item(0).getTextContent());
			if(doc.getElementsByTagName("displayResponse").item(0).getTextContent().indexOf("19") != -1)
			{
				nextItemIndex = doc.getElementsByTagName("nextItemReference").item(0).getTextContent();
				scrolling = true;
				numReq++;
			}
			else
				scrolling = false;
//			System.out.println(result);
			if(scrolling == false)
				break;
		}while(true);
		
		result = SecuritySignOut(wsap1, GetSessionBlock(sessionId1, securityToken1, 3 + numReq));
//		System.out.println(result);
    return list;
	}
	
	private String GetSessionBlock(String id, String securityToken, int seqNum)
	{
		return "\n\t<Session><SessionId>" + id + "</SessionId><SequenceNumber>" + seqNum + "</SequenceNumber><SecurityToken>" + securityToken + "</SecurityToken></Session>\n";
	}
	
	private String HotelAvailabilityMultiProperties(String wsap, String session, String hotelName, String cityCode, String checkIn, String checkOut, boolean scrolling, String nextItemIndex) throws Exception
	{
		String query = "<Hotel_AvailabilityMultiProperties>"
			+ "<scrollingInformation>"
			+ "		<displayRequest>" + (scrolling ? "030" : "050") + "</displayRequest>";
		if(scrolling && nextItemIndex != "")
			query += "<nextItemReference>" + nextItemIndex + "</nextItemReference>";
		query += "</scrollingInformation>";
		
		if(checkIn != null && checkOut != null)
		{
			query += "<hotelPropertySelection>"
			+ "		<checkInDate>" + checkIn + "</checkInDate>"
			+ "		<checkOutDate>" + checkOut + "</checkOutDate>"
			+ "</hotelPropertySelection>";
		}
		
		query += "<hotelLocationPrefInfo>"
			+ "		<locationSelection>"
			+ "			<cityCode>" + cityCode + "</cityCode>"
			+ "		</locationSelection>"
			+ "		<hotelNameSelection>"
			+ "			<nameSearch>" + hotelName + "</nameSearch>"
			+ "		</hotelNameSelection>"
			+ "</hotelLocationPrefInfo>"
			+ "</Hotel_AvailabilityMultiProperties>";
		
        String result = SendQuery(wsap, session, query, "Hotel_AvailabilityMultiProperties");
        return (result);
	}
	
	private String GetXml(String queryType) throws Exception
	{
		StringBuffer contents = new StringBuffer();
		File file = new File(readFilePath + "\\" + queryType + ".xml");
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String text = null;
		while((text = reader.readLine()) != null)
			contents.append(text).append(System.getProperty("line.separator"));
		return contents.toString();
	}
	
	private String SecurityAuthenticate(String wsap, String user, String office, String password, String passlen) throws Exception
	{
        String query = "<Security_Authenticate>"
            + "    <userIdentifier>"
            + "      <originIdentification>"
            + "        <sourceOffice>" + office + "</sourceOffice>"
            + "      </originIdentification>"
            + "      <originatorTypeCode>U</originatorTypeCode>"
            + "      <originator>" + user + "</originator>"
            + "    </userIdentifier>"
            + "    <dutyCode>"
            + "      <dutyCodeDetails>"
            + "        <referenceQualifier>DUT</referenceQualifier>"
            + "        <referenceIdentifier>SU</referenceIdentifier>"
            + "      </dutyCodeDetails>"
            + "    </dutyCode>"
            + "    <systemDetails>"
            + "      <organizationDetails>"
            + "        <organizationId>1A</organizationId>"
            + "      </organizationDetails>"
            + "    </systemDetails>"
            + "    <passwordInfo>"
            + "      <dataLength>" + passlen + "</dataLength>"
            + "      <dataType>E</dataType>"
            + "      <binaryData>" + password + "</binaryData>"
            + "    </passwordInfo>"
            + "  </Security_Authenticate>";

        String result = SendQuery(wsap, "", query, "Security_Authenticate");
        return (result);
	}
	
	private String SecuritySignOut(String wsap, String sessionId) throws Exception
	{
		return SendQuery(wsap, sessionId, "<Security_SignOut/>", "Security_SignOut");
	}
	
	private String SendQuery(String wsap, String session, String query, String queryType) throws Exception
	{
		URL url = new URL("https://test.webservices.amadeus.com/");
		HttpURLConnection httpConn = (HttpURLConnection)url.openConnection();
		httpConn.setRequestProperty("Content-Type","text/xml; charset=utf-8");
		httpConn.setRequestMethod("POST");
		httpConn.setRequestProperty("SOAPAction", "http://webservices.amadeus.com/" + wsap + "/" + codes.get(queryType));
		httpConn.setDoOutput(true);
		
        String final_query = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">\n";
        final_query += "<s:Header>" + session + "</s:Header>\n";
        final_query += "<s:Body xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n";
        final_query += query;
        final_query += "</s:Body>\n</s:Envelope>";
        
        OutputStream reqStream = httpConn.getOutputStream();
        reqStream.write(final_query.getBytes());
		InputStream input = httpConn.getInputStream();
		Writer writer = new StringWriter();
		Reader reader = new BufferedReader(new InputStreamReader(input));
		int n;
		char[] buffer = new char[1024];
		while((n = reader.read(buffer)) != -1)
		{
			writer.write(buffer, 0, n);
		}
		return writer.toString();
	}
	
	private String GetSessionId(String response)
	{
		/*
		Pattern pattern = Pattern.compile("<SessionId>(.*)</SessionId>");
		Matcher matcher = pattern.matcher(response);
		if(matcher.find())
			return matcher.group(1);
		else
			return "";
			*/
		Document doc = (Document)responseXmlHash.get("Security_Authenticate");
		return doc.getElementsByTagName("SessionId").item(0).getTextContent();
	}
	
	private String GetSecurityToken(String response)
	{
		/*
		Pattern pattern = Pattern.compile("<SecurityToken>(.*)</SecurityToken>");
		Matcher matcher = pattern.matcher(response);
		if(matcher.find())
			return matcher.group(1);
		else
			return "";
			*/		
		Document doc = (Document)responseXmlHash.get("Security_Authenticate");
		return doc.getElementsByTagName("SecurityToken").item(0).getTextContent();
	}
}
