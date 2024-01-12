package com.mobinets.nps.customer.transmission.manufacture.cambium_UBR.UBR;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.mobinets.nep.model.autoimporter.MWConfiguration.AtpcModeEnum;
import com.mobinets.nps.customer.transmission.common.CommonConfig;
import com.mobinets.nps.customer.transmission.common.FilesFilterHandler;
import com.mobinets.nps.customer.transmission.common.IpMatcher;
import com.mobinets.nps.customer.transmission.common.TransmissionCommon;
import com.mobinets.nps.customer.transmission.common.TypeMatchingParser;
import com.mobinets.nps.customer.transmission.manufacture.common.AdditionalInfoUtilities;
import com.mobinets.nps.customer.transmission.manufacture.common.ConnectorUtility;
import com.mobinets.nps.customer.transmission.manufacture.ericsson.r3.node.R3NetworkElementsParser;
import com.mobinets.nps.customer.transmission.manufacture.ericsson.r3.nodeSlot.R3NodeSlotsParser;
import com.mobinets.nps.daemon.csv.AbstractFileCsvParser;
import com.mobinets.nps.model.customer.data.element.ElementMwConfiguration;
import com.mobinets.nps.model.customer.data.element.ElementTransmissionLink;
import com.mobinets.nps.model.network.ElementAdditionalInfo;
import com.mobinets.nps.model.network.ElementAdditionalInfo.ElementType;
import com.mobinets.nps.model.network.UbrConfiguration;
import com.mobinets.nps.model.nodeinterfaces.NetworkElement;
import com.mobinets.nps.model.nodeinterfaces.NodeInterface;
import com.mobinets.nps.model.nodeinterfaces.NodeSlot;

public class CambiumUbrTransmissionLinkParser extends AbstractFileCsvParser<ElementTransmissionLink> {

	private static final Log log = LogFactory.getLog(CambiumUbrTransmissionLinkParser.class);
	private static final Log logErr = LogFactory.getLog("TRLINK_ERROR_LOGGER");

	private CommonConfig r3Config;
	private TypeMatchingParser typeMatching;
	private R3NetworkElementsParser r3NetworkElementsParser;
	private R3NodeSlotsParser r3NodeSlotsParser;
	List<LinkPart> linkParts = new ArrayList<>();
	private Map<String, NodeSlot> nodeSlotMap;
	private Set<String> exterCodeSet;
	private Map<String, Integer> sameIdMap;
	private Map<String, String> farEndTerminalMapError;
	private Map<String, NetworkElement> networkEleMap;
	private Map<String, String> circleIdmap;
	private Map<String, NetworkElement> elementByName;
	private Map<String, NodeInterface> nodeInterfacesMap;
	private Map<String,List<String>>txFrquencyListMap;
	private List<String>tempList;
	private List<String> nodeIdList;
	private Map<String, ElementTransmissionLink> linksMap; 
	private Map<String, String> tcapacitymap = new HashMap<>();
	
	
	
	public List<ElementMwConfiguration> getLinkDataforMwConfig() throws IOException {
		List<ElementMwConfiguration> mwConfig = new ArrayList<>();
		if (elementMwConfigurationMap == null)
			parserLinks();
		mwConfig.addAll(elementMwConfigurationMap.values());
		return mwConfig;
	}
	

	private Map<String, ElementMwConfiguration> elementMwConfigurationMap;
	private List<ElementAdditionalInfo> additionalInfos =  new ArrayList<>();
	private List<String> AggregationList;
	private Map<String, NetworkElement>linkconfigurationMap;
	private Map<String, List<LinkPart>> linkPartByCircleMap = new ConcurrentHashMap<String, List<LinkPart>>();
	Map<String, Node> nodeByCirleMap = new HashMap<String, Node>();
	Map<String, List<String>> node2AdditionalInfoByInstanceRF1ByCircleMap = new HashMap<>();
	private Matcher numMatcher;
	private CambiumUbrNetworkElementsParser cambiumUbrNetworkElementsParser;

	 
	public void setCambiumUbrNetworkElementsParser(CambiumUbrNetworkElementsParser cambiumUbrNetworkElementsParser) {
		this.cambiumUbrNetworkElementsParser = cambiumUbrNetworkElementsParser;
	}

	 
	private Map<String, String> channelSpacingmap = new HashMap<>(); 
	private Map<String, String> bandMap = new HashMap<>();
	Map<String, Node> nodeMap = new HashMap<String, Node>();
	private IpMatcher ipMatcher;
	Set<String> usedInterfaces = new HashSet<>();


	public void setIpMatcher(IpMatcher ipMatcher) {
		this.ipMatcher = ipMatcher;
	}


	public void setTypeMatching(TypeMatchingParser typeMatching) {
		this.typeMatching = typeMatching;
	}

	public void setR3Config(CommonConfig r3Config) {
		this.r3Config = r3Config;
	}

	public void setR3NetworkElementsParser(R3NetworkElementsParser r3NetworkElementsParser) {
		this.r3NetworkElementsParser = r3NetworkElementsParser;
	}

	public void setR3NodeSlotsParser(R3NodeSlotsParser r3NodeSlotsParser) {
		this.r3NodeSlotsParser = r3NodeSlotsParser;
	}
    
/*	UbrConfiguration ubr = new UbrConfiguration();*/
	private Pattern numPattern = Pattern.compile("\\d*");

	private void init() {
		linksMap = new HashMap<>();
		elementMwConfigurationMap = new HashMap<>();
		networkEleMap = cambiumUbrNetworkElementsParser.getMapOfNetworkElement();
		circleIdmap = cambiumUbrNetworkElementsParser.getCircleIdMap();
		nodeIdList = cambiumUbrNetworkElementsParser.getNeIdList();
		nodeInterfacesMap = new HashMap<String, NodeInterface>();
		tempList = new ArrayList<>();
		linkconfigurationMap = new HashMap<>();
		AggregationList = new ArrayList<>();
		txFrquencyListMap = new HashMap<>();
		 
		

		nodeSlotMap = new HashMap<String, NodeSlot>();
		try {
			for (NodeSlot slot : r3NodeSlotsParser.getNodeSlotsElements(networkEleMap)) {
				nodeSlotMap.put(slot.getId(), slot);
			}
		} catch (Exception e) {
		}

	}
 
	public List<ElementTransmissionLink> getElementTrsLinks() throws IOException {
		List<ElementTransmissionLink> trLinkRes = new ArrayList<>();
		if (linksMap == null)
			parserLinks();
		trLinkRes.addAll(linksMap.values());

	 

		return trLinkRes;
	}
 
 

	public List<ElementAdditionalInfo> getAdditionalInfos() {
		if (additionalInfos.isEmpty())
			parserLinks();
		return additionalInfos;
	}
 

	public void clearElements() {
		nodeSlotMap = null;
		exterCodeSet = null;
		sameIdMap = null;
		farEndTerminalMapError = null;
		networkEleMap = null;
		elementByName = null;
		nodeInterfacesMap = null;
		linksMap = null;
	}

	public void clearAdditionalInfos() {
		additionalInfos = null;
	}

 

 	
 	private void parserLinks() {
 		 
		log.debug("Start creating Cambium UBR Transmission links");
		init();

		String path = r3Config.getProperty("cambium.ubr.dumps");

		if (null == path) {
			log.error("Missing path (cambium.ubr.dumps.mw.dumps) in context file.");
			return;
		}
		File folder = new File(path);
		if (!folder.exists()) {
			log.error("Folder (" + path + ") not found");
			return;
		}
		
		String cabinetIndex = "1",shelfIndex = "1" , indexOnSlot ="0", slotIndex = "0", portIndex ="1";
		String interfaceId1="", interfaceId2 ="";
		String site1 ="", site2="";
		String externalCode ="",capacity = "",linkType ="",linkModulation="",typeofBand="";
		String nodeID_1 ="", nodeID_2="",ethernet_Link_capacity = "";	
		String aggregation="";
		 
		if (!folder.exists()) {
			logErr.error("Folder (" + path + ") not found");
			log.error("Folder (" + path + ") not found");
			return;
		}
		
		List<File> aviatFiles = new ArrayList<>();
		ConnectorUtility.listofFiles(path, aviatFiles, new FilesFilterHandler.CsvFiles());
		
		
		clearHeaders();
		addHeaderToParse("Device Name");
		addHeaderToParse("AP Device Name");
		addHeaderToParse("IPv6 Address");
		addHeaderToParse("IP Address"); 
		addHeaderToParse("AP Ipv6 Address");
		addHeaderToParse("AP Ip Address"); 
		addHeaderToParse("Product Name");
      
		//for link configuration and UBR Configuration
		addHeaderToParse("RF Frequency (MHz)"); 
		addHeaderToParse("RF Frequency2 (MHz)"); 
		addHeaderToParse("Channel Width (MHz)"); 
		addHeaderToParse("Channel Width2 (MHz)"); 
		addHeaderToParse("Antenna Gain (dBi)"); 
		addHeaderToParse("Antenna Gain (dBi)"); 
		addHeaderToParse("Radio TX Power (dBm)"); 
		addHeaderToParse("Radio TX Power2 (dBm)"); 
		addHeaderToParse("Uplink RSSI (dBm)");
		addHeaderToParse("Downlink RSSI (dBm)");
		addHeaderToParse("Downlink RSSI2 (dBm)");
		addHeaderToParse("Uplink RSSI (dBm)");
		addHeaderToParse("Uplink MCS2");
		addHeaderToParse("Uplink MCS"); 
		addHeaderToParse("Downlink MCS"); 
		addHeaderToParse("Downlink MCS2");
		  
		
		
		 
		for (int i =0;i<aviatFiles.size();i++) {
			File file = aviatFiles.get(i);
		
			if (!file.getName().contains("cnMaestro-device-ePMP-ap-sm-system"))
				continue;
		
			try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
 
			CsvListReader csvReader = new CsvListReader(bufferedReader, CsvPreference.EXCEL_PREFERENCE);
			
			try {
				 
				final String[] header = csvReader.getCSVHeader(true);
				boolean isOK = fillHeaderIndex(header);

				if (!isOK) {
					logErr.error("Error data in header (Device Name, Device Type, Status, Product Name) .. for file " + file.getPath());
					continue;
				}
			
				List<String> row = new ArrayList<String>();
				while ((row = csvReader.read()) != null) {
					try{
						
					String node1 = row.get(headerIndexOf("Device Name"));
					int sizeOfRow = row.size();
					if(sizeOfRow == 88){
						System.out.println(sizeOfRow);
						continue;}
					
					System.out.println(sizeOfRow);
					String node2 = row.get(headerIndexOf("AP Device Name")).trim();
				 	String ipV6Address = row.get(headerIndexOf("IPv6 Address")).trim();
					String ipAddress = row.get(headerIndexOf("IP Address")).trim();
					String A_ipV6Address = row.get(headerIndexOf("AP Ipv6 Address")).trim();
					String A_ipAddress = row.get(headerIndexOf("AP Ip Address")).trim();
					String model = row.get(headerIndexOf("Product Name")).trim();
					String channel_spacing = row.get(headerIndexOf("Channel Width (MHz)")).trim();
					String antenna_gain = row.get(headerIndexOf("Antenna Gain (dBi)")).trim();
					String dlrss1_dbm = row.get(headerIndexOf("Downlink RSSI (dBm)")).trim();
					String dlrss2_dbm = row.get(headerIndexOf("Downlink RSSI2 (dBm)")).trim();
					
					String Ulrss1_dbm = row.get(headerIndexOf("Uplink RSSI (dBm)")).trim();
					String Ulrss2_dbm =  "N/A";
					
					String  TXPower_dBm = row.get(headerIndexOf("Radio TX Power (dBm)")).trim();
					String  TXPower_dBm2 = row.get(headerIndexOf("Radio TX Power2 (dBm)")).trim();
					
					String downLink_1 = row.get(headerIndexOf("Downlink MCS")).trim(); 
					String upLink_1 = row.get(headerIndexOf("Uplink MCS")).trim(); 
					
					String downLink_2 = row.get(headerIndexOf("Downlink MCS2")).trim(); 
					String upLink_2 = row.get(headerIndexOf("Uplink MCS2")).trim(); 
					
					
					
					String Frequency1_MHz = row.get(headerIndexOf("RF Frequency (MHz)")).trim(); 
					String Frequency2_MHz = row.get(headerIndexOf("RF Frequency2 (MHz)")).trim();
					
					String channel_spacing1 = row.get(headerIndexOf("Channel Width (MHz)")).trim();
					String channel_spacing2 = row.get(headerIndexOf("Channel Width2 (MHz)")).trim();
					
					String freqBand = "5G";
					
					
					 
					 
					 if(node2.isEmpty() || A_ipV6Address.isEmpty() || A_ipAddress.isEmpty()){
						 continue;}
		 
					 String s1 = StringUtils.substring(site1, 4) + "_" + StringUtils.substring(site1, 0, 2);
					 String s2 = StringUtils.substring(site2, 4) + "_" + StringUtils.substring(site2, 0, 2);
					 
					 String circle1 = node1.substring(0, Math.min(node1.length(), 2));
					 String circle2 = node2.substring(0, Math.min(node2.length(), 2));
					 
                       
					 site1 = StringUtils.substring(node1, 5);
					 site2 = StringUtils.substring(node2, 5);
					 
					  
					 aggregation = site1+"-"+site2+"_"+"Cambium_UBR"+"_"+freqBand;
   
					 if(!ipV6Address.isEmpty()){
					  nodeID_1 = ipV6Address;
					 }
					 else{
					  nodeID_1 = ipAddress;
					 }
					 
					 if(!A_ipV6Address.isEmpty()){
						  nodeID_2 = A_ipV6Address;
						 }
					else{
						  nodeID_2 = A_ipAddress;
						 }
					 
					 
						String external1 = nodeID_1 + "_"+circle1+"-"+cabinetIndex+"-"+shelfIndex+"-"+slotIndex + "-" + indexOnSlot + "-"
								+ portIndex;
						String external2 = nodeID_2 + "_"+circle2+"-"+cabinetIndex+"-"+shelfIndex+"-"+slotIndex +"-" + indexOnSlot + "-"
								+ portIndex;
						
				
						 externalCode = TransmissionCommon.getKey(external1, external2, "_");
						
					 if (!externalCode.contains("null")) {
							if (!usedInterfaces.contains(external1) && !usedInterfaces.contains(external2)) {
								ElementTransmissionLink trLink = new ElementTransmissionLink();
								trLink.setManufacturer(TransmissionCommon.ERICSSON);
								trLink.setId(nodeID_1+"_"+nodeID_2);
								trLink.setName(externalCode);
								trLink.setTrLinkCapacity(" ");
								trLink.setSlotIndex1(slotIndex);
								trLink.setSlotIndex2(slotIndex);
								trLink.setInterfaceIndex1(portIndex);
								trLink.setInterfaceIndex2(portIndex);
								trLink.setBoardIndex1(indexOnSlot);
								trLink.setBoardIndex2(indexOnSlot);
								trLink.setSite1(s1);
								trLink.setSite2(s2);
								trLink.setNode1(node1+"_"+circle1);
								trLink.setNode2(node2+"_"+circle2);
								trLink.setType("Cambium_Link");
								trLink.setExternalCode(externalCode);
								linksMap.put(externalCode, trLink);
								
//								channel_spacing = row.get(headerIndexOf("Channel Width (MHz)")).trim();
//								String antenna_gain = row.get(headerIndexOf("Antenna Gain (dBi)")).trim();
//								String dlrss1_dbm = row.get(headerIndexOf("Downlink RSSI (dBm)")).trim();
//								String dlrss2_dbm = row.get(headerIndexOf("Downlink RSSI2 (dBm)")).trim();
//								
//								String Ulrss1_dbm = row.get(headerIndexOf("Uplink RSSI (dBm)")).trim();
//								String Ulrss2_dbm =  "N/A";
//								
//								String  TXPower_dBm = row.get(headerIndexOf("Radio TX Power (dBm)")).trim();
//								String  TXPower_dBm2 = row.get(headerIndexOf("Radio TX Power2 (dBm)")).trim();
//								
//								String downLink_1 = row.get(headerIndexOf("Downlink MCS")).trim(); 
//								String upLink_1 = row.get(headerIndexOf("Uplink MCS")).trim(); 
//								
//								String downLink_2 = row.get(headerIndexOf("Downlink MCS2")).trim(); 
//								String upLink_2 = row.get(headerIndexOf("Uplink MCS2")).trim(); 
//								
//								
//								
//								String Frequency1_MHz = row.get(headerIndexOf("RF Frequency (MHz)")).trim(); 
//								String Frequency2_MHz = row.get(headerIndexOf("RF Frequency2 (MHz)")).trim();
//								
//								String channel_spacing1 = row.get(headerIndexOf("Channel Width (MHz)")).trim();
//								String channel_spacing2 = row.get(headerIndexOf("Channel Width2 (MHz)")).trim();
//								
//								String freqBand = "5G";
//								
						
								createUbrConfig(externalCode,aggregation,channel_spacing,antenna_gain,dlrss1_dbm,
										dlrss2_dbm,Ulrss1_dbm,Ulrss2_dbm,TXPower_dBm,TXPower_dBm2,
										downLink_1,upLink_1,downLink_2,upLink_2,Frequency1_MHz,Frequency2_MHz,channel_spacing2,freqBand);
								
							}
					 }
							 
							
				
					}catch (Exception e) {
						e.printStackTrace();
				log.error("Error : ", e);
			}	
				}
		}catch(Exception e){
			e.printStackTrace();
			log.error("Error: ",e);
		}

	} 
	catch(Exception e){
	e.printStackTrace();
	log.error("Error: ",e);
	}
       log.debug("End of Ericsson Node Slots parsing From Soem Inventory File ...");
			}
      
	} 

	 
	private void createUbrConfig(String externalCode, String aggregation, String channel_spacing, String antenna_gain,
			String dlrss1_dbm, String dlrss2_dbm, String ulrss1_dbm, String ulrss2_dbm, String tXPower_dBm,
			String tXPower_dBm2, String downLink_1, String upLink_1, String downLink_2, String upLink_2,
			String frequency1_MHz, String frequency2_MHz, String channel_spacing2, String freqBand) {
		 
		UbrConfiguration ubrconfig = new UbrConfiguration();
		ubrconfig.setAntennaGain(antenna_gain);
	//	ubrconfig.setCableLoss(cableLoss);
		ubrconfig.setUbrConfigId(externalCode);
		ubrconfig.setChannelSpacing2(channel_spacing);
//		ubrconfig.setDlModulation(dlModulation);
	//	ubrconfig.setDlModulation2(dlModulation2);
		ubrconfig.setDlRss(dlrss1_dbm);
		ubrconfig.setDlRss2(dlrss2_dbm);
		ubrconfig.setFrequency(frequency1_MHz);
		ubrconfig.setFrequency2(frequency2_MHz);
		ubrconfig.setFreqBand(freqBand);
		ubrconfig.setUlRss(upLink_1);
		ubrconfig.setUlRss2(upLink_2);
		ubrconfig.setTxRadioPower(tXPower_dBm);
		ubrconfig.setTxRadioPower2(tXPower_dBm2);
		
	}


//	private void createUbrConfig(String external1, String external2, String externalCode, String node1, String node2,
//		String site1, String site2, String trLinkCapacity,
//		String totalLinkCapacity, String linkModulation, String atpc_mode,String txfrequency,String rxfrequency,String s1, String s2) {
//		
// 
//		
//		double txfreq = Double.valueOf(txfrequency);
//		double rxfreq = Double.valueOf(rxfrequency);
//		
//	   double freqBand = txfreq-rxfreq;
//	  
//	   String fqBand = String.valueOf(freqBand);
//	   
//	   if(fqBand.contains("10000")){
//		   fqBand="80";
//	   }
//	   
//	   
//	   double fband = Double.valueOf(fqBand);
//	  
//	   String aggregationiId = s1+"-"+s2+"_"+"Aviat"+"_"+fqBand;
//	   String masterLinkid = "false";
//	   String linkConfiguration ="";
//	   
//	   ElementMwConfiguration mwConfig = new ElementMwConfiguration();
//	   
//	   UbrConfiguration ubr = new UbrConfiguration();
//	 //  ubr.setChangedby(changedby);
//	//   ubr.setAntennaGain(antennaGain);
//	   
//	   mwConfig.setAggrgationLinkId(aggregationiId);
//	   
//	   if(atpc_mode.contains("true")){
//	   mwConfig.setAtpcMode(AtpcModeEnum.ON);}
//	   if(atpc_mode.contains("false")){
//	   mwConfig.setAtpcMode(AtpcModeEnum.OFF);}
//	   mwConfig.setCapacity(totalLinkCapacity);
//	   mwConfig.setMasterLink(masterLinkid);
//	   mwConfig.setModulation(linkModulation);
//	   mwConfig.setFreqBand(fband);
//	   mwConfig.setFreqChannelRx(rxfreq);
//	   mwConfig.setFreqChannelTx(txfreq);
//	   mwConfig.setTrLinkId(externalCode);
//	   if(linkConfiguration.isEmpty()){
//		   
//		    String typeofLink_node1 = bandMap.get(node1);
//			String typeofLink_node2 = bandMap.get(node2);
//				
//				if(bandMap.containsKey(node1) && typeofLink_node1.contains("L1LA1")){
//				 
//					linkConfiguration = "1+0 (SDB-Eband)";	}
//				else if(bandMap.containsKey(node2) && typeofLink_node2.contains("L1LA1")){
//					 
//					linkConfiguration = "1+0 (SDB-Eband)";	}
//					else{
//						linkConfiguration = "1+0 (SA-Eband)";
//					}
//		   
//	   }
//	   mwConfig.setLinkConfiguration(linkConfiguration);
//	   elementMwConfigurationMap.put(externalCode,mwConfig);
//	   
//	  	  
//}


	/*private void createMwConfig(String external1, String external2, String externalCode, String node1, String node2,
			String site1, String site2, String external12, String external22, String externalCode2,
			String trLinkCapacity, String totalLinkCapacity, String linkModulation, String atpc_mode) {
		
		log.debug("Start creating Aviat MW Transmission links");
		init();

		String path = r3Config.getProperty("aviat.mw.dumps");

		if (null == path) {
			log.error("Missing path (aviat.mw.dumps) in context file.");
			return;
		}
		File folder = new File(path);
		if (!folder.exists()) {
			log.error("Folder (" + path + ") not found");
			return;
		}
		
		String cabinetIndex = "1",shelfIndex = "1" , indexOnSlot ="0", slotIndex = "0", portIndex ="1";
		String interfaceId1="", interfaceId2 ="";
		String site1 ="", site2="";
		String externalCode ="",capacity = "",linkType ="",linkModulation="";
		
		 
		if (!folder.exists()) {
			logErr.error("Folder (" + path + ") not found");
			log.error("Folder (" + path + ") not found");
			return;
		}
		
		List<File> aviatFiles = new ArrayList<>();
		ConnectorUtility.listofFiles(path, aviatFiles, new FilesFilterHandler.CsvFiles());
		
		
		clearHeaders();
		addHeaderToParse("Site A IP");
		addHeaderToParse("Site Z IP");
		addHeaderToParse("Site A Name");
		addHeaderToParse("Site Z Name");
		addHeaderToParse("Site A Maximum Configured Capacity");
		addHeaderToParse("Site Z Maximum Configured Capacity");

		for (int i =0;i<aviatFiles.size();i++) {
			File file = aviatFiles.get(i);
		
			if (!file.getName().contains("LINK_REPORT"))
				continue;
			
			 
			try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {

	            // Skip the first two lines
	            for (int i1 = 0; i1 < 2; i1++) {
	                bufferedReader.readLine();
	            }
			CsvListReader csvReader = new CsvListReader(bufferedReader, CsvPreference.EXCEL_PREFERENCE);
			
			try {
				 
				final String[] header = csvReader.getCSVHeader(true);
				boolean isOK = fillHeaderIndex(header);

				if (!isOK) {
					logErr.error("Error data in header (ID, Type, Address, NEName) .. for file " + file.getPath());
					continue;
				}
			
				List<String> row = new ArrayList<String>();
				while ((row = csvReader.read()) != null) {
					try{
					String node1 = row.get(headerIndexOf("Site A IP"));
					String node2 = row.get(headerIndexOf("Site Z IP"));
					String siteA_MaxCapacity = row.get(headerIndexOf("Site A Maximum Configured Capacity"));
					String siteB_MaxCapacity = row.get(headerIndexOf("Site Z Maximum Configured Capacity"));
					String siteA_Max_ConfigCap = row.get(headerIndexOf("Site A Maximum Configured Capacity"));   
					String siteB_Max_ConfigCap = row.get(headerIndexOf("Site A Maximum Configured Capacity"));
					String siteA_CurrentModulation = row.get(headerIndexOf("Site A Current Modulation")); 
					String siteZ_CurrentModulation = row.get(headerIndexOf("Site Z Current Modulation")); 
					String atpc_mode = row.get(headerIndexOf("ATPC Status"));  
					}catch (Exception e) {
						e.printStackTrace();
						log.error(e);
				}
				}
		try {

			double freqRx, freqTx, freq, bandWidth;
			Double power;
			boolean isOneOfDataFilled = false;
			try {
				freqRx = Double.valueOf(frequencyChannelRx).doubleValue();
				isOneOfDataFilled = true;
			} catch (Exception e) {
				freqRx = -1;
			}

			try {
				freqTx = Double.valueOf(frequencyChannelTx);
				isOneOfDataFilled = true;
			} catch (Exception e) {
				freqTx = -1;
			}

			try {
				freq = Double.valueOf(freqBandRa).doubleValue() * 1000;
				isOneOfDataFilled = true;
			} catch (Exception e) {
				freq = -1;
			}

			try {
				power = Double.valueOf(selectedPower);
				isOneOfDataFilled = true;
			} catch (Exception e) {
				power = null;
			}

			try {
				bandWidth = Double.valueOf(bandwidthStr).doubleValue();
				isOneOfDataFilled = true;
			} catch (Exception e) {
				bandWidth = -1;
			}

			if (!isOneOfDataFilled && capacity.isEmpty() && atpcMode.isEmpty() && protectionMode.isEmpty())
				return; // ignore it because all data are empty

			protectionMode = protectionMode.replaceAll("(?i)hot", "").trim();

			if (!elementMwConfigurationMap.containsKey(externalCode)) {
				ElementMwConfiguration mwconfig = new ElementMwConfiguration();
				mwconfig.setTrLinkId(externalCode);

				mwconfig.setCapacity(capacity);
				mwconfig.setFreqChannelRx(freqRx);
				mwconfig.setFreqChannelTx(freqTx);
				mwconfig.setFreqBand(freq);
				mwconfig.setPower(power);
				mwconfig.setProtectionMode(PathMWConfiguration.getMWConfiguration(protectionMode));
				mwconfig.setBandwidth(bandWidth);
				mwconfig.setAtpcMode(AtpcModeEnum.valueOfAtpcMode(atpcMode));
				mwconfig.setParity(ParityEnum.NONE);
				mwconfig.setImporterConnector(ImporterConnector.Ericsson_R3);
				mwconfig.setDumpType(DumpType.MLE);
				elementMwConfigurationMap.put(externalCode, mwconfig);
			}

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}
			}catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}

	}*/
	
		 
	public static class LinkPart {
		public String selectedPower;
		public String freqBandRa;
		public String link_type;
		public String modulation;
		public String max_Modulation;
		public String packetLinkCapacity;
		public String neId;
		public String terminaId;
		public String farEndId;
		public String instanceRf1;
		public String instanceRf2;
		public String protectionModeAdminStatus;
		public String type;
		public String farEndNEIP;
		public String farEndNESlot;
		public String farEndNESlot2;
		public String txFreq;
		public String rxFreq;
		public String capacity;
		public String e1Number;
		public String channel_Spacing;
		public String circle;
		public String ossIp;
	}

	public static class Node {
		public String id;
		public String name;
		public String address;
	}

}
