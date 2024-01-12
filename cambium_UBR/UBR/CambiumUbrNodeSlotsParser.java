package com.mobinets.nps.customer.transmission.manufacture.cambium_UBR.UBR;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.mobinets.nps.customer.transmission.common.CommonConfig;
import com.mobinets.nps.customer.transmission.common.FilesFilterHandler;
import com.mobinets.nps.customer.transmission.common.utilties.CsvHandler;
import com.mobinets.nps.customer.transmission.manufacture.common.ConnectorUtility;
import com.mobinets.nps.daemon.csv.AbstractFileCsvParser;
import com.mobinets.nps.model.nodeinterfaces.NodeInterface;
import com.mobinets.nps.model.nodeinterfaces.NodeSlot;

public class CambiumUbrNodeSlotsParser extends AbstractFileCsvParser<NodeSlot>implements CsvHandler {

	private static final Log log = LogFactory.getLog(CambiumUbrNodeSlotsParser.class);
	private static final Log logErr = LogFactory.getLog("NODE_SLOT_ERROR_LOGGER");

	private CommonConfig r3Config;
    private Map<String, NodeInterface> ethernetMap;
    private Map<String,NodeInterface> linktotalCpacitymap = new HashMap<>();
    public Map<String, NodeInterface> getLinktotalCpacitymap() {
		return linktotalCpacitymap;
	}

	 
	private Map<String,NodeInterface> performanceDumpMaps = new HashMap<>();
	 
    
	private boolean isParsed = false;
     
    
	public Map<String, NodeInterface> getEthernetMap() {
		return ethernetMap;
	}
	 
	Map<String, String> npuSlotMap ;	 
	private Map<String,NodeInterface> config_EthernerMap  = new HashMap<String, NodeInterface>(); 
	
	public Map<String, NodeInterface> getConfig_EthernerMap() {
		if(config_EthernerMap == null || config_EthernerMap.isEmpty()){
			parsingInterfacefromEthernetReport();
		}
		return config_EthernerMap;
	}


	private Map<String, NodeSlot> slotsMap = new HashMap<String,NodeSlot>();	 
	Map<String, String> fauSlotMap ;
 
	 
	public void setR3Config(CommonConfig r3Config) {
		this.r3Config = r3Config;
	}

	 
	public void init(){
		ethernetMap = new HashMap<>(); 
	}
	
	/**
	 * 
	 */
	private void parsingInterfacefromEthernetReport() {
	 init();
         parsePerformamcedumps();

		
	 
		log.debug("Start Paring Cambium Ethernet Report");
		init();

		String path = r3Config.getProperty("cambium.ubr.dumps");

		if (null == path) {
			log.error("Missing path (cambium.ubr.dumps) in context file.");
			return;
		}
		File folder = new File(path);
		if (!folder.exists()) {
			log.error("Folder (" + path + ") not found");
			return;
		}
		
		String cabinetIndex = "1",shelfIndex = "1" , indexOnSlot ="0", slotIndex = "0", portIndex ="2";
		String interfaceId="",nodeBoardId ="";
		String site1 ="", site2="";
		String externalCode ="",capacity = "",linkType ="",linkModulation="",interfaceid="";
		String ethernetCapcity="";
	    String circle="", id = "";
		
		 
		if (!folder.exists()) {
			logErr.error("Folder (" + path + ") not found");
			log.error("Folder (" + path + ") not found");
			return;
		}
		
		List<File> aviatFiles = new ArrayList<>();
		ConnectorUtility.listofFiles(path, aviatFiles, new FilesFilterHandler.CsvFiles());
		
		
		clearHeaders();
		addHeaderToParse("IPv6 Address");
		addHeaderToParse("IP Address");
		addHeaderToParse("Device Name");
		addHeaderToParse("Network");
		addHeaderToParse("LAN Status");
		addHeaderToParse("LAN Speed Status (Mbps)");
		addHeaderToParse("MAC");
		addHeaderToParse("LAN Mode Status");
		
		
	 

		for (int i =0;i<aviatFiles.size();i++) {
			File file = aviatFiles.get(i);
		
			if (!file.getName().contains("cnMaestro-device-ePMP-ap-sm-system-daily"))
				continue;
			
			 
			try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
 
			CsvListReader csvReader = new CsvListReader(bufferedReader, CsvPreference.EXCEL_PREFERENCE);
			
			try {
				 
				final String[] header = csvReader.getCSVHeader(true);
				boolean isOK = fillHeaderIndex(header);

				if (!isOK) {
					logErr.error("Error data in header (Interface Name, Mpbs) .. for file " + file.getPath());
					continue;
				}
			
				List<String> row = new ArrayList<String>();
				while ((row = csvReader.read()) != null) {
					try{
					String deviceName = row.get(headerIndexOf("Device Name"));
					String network = row.get(headerIndexOf("Network"));  
					String ipV6Address = row.get(headerIndexOf("IPv6 Address"));  
					String ipAddress = row.get(headerIndexOf("IP Address"));  
					String portSpeed = row.get(headerIndexOf("LAN Speed Status (Mbps)"));  
					String portStatus = row.get(headerIndexOf("LAN Status"));  
					String macAddress = row.get(headerIndexOf("MAC"));  
					String configredVlan = "N/A";  
					String autoNegotitaions = "N/A";
					String duplexMode = row.get(headerIndexOf("LAN Mode Status")); 
					
					
					if(network.contains("default") || network.contains("NESA") || network.contains(" ")){
						 
						circle = deviceName.substring(0, Math.min(deviceName.length(), 2));
					}
					else{
						
						circle = deviceName.substring(0, Math.min(deviceName.length(), 2));
						
                       if(circle.contains("BH")){
            	       circle = "BR";
                       }
                       else if(circle.contains("JH")){
                       circle = "BR";
            	       }
                       else if(circle.contains("CH")){
            	       circle = "CN";
                      }
					}
             
                   if(!ipV6Address.contains("N/A")){
                	    id = ipV6Address+"_"+circle;   
                   }
                   else{
                	    id = ipAddress+"_"+circle;
                   }

            			 
				interfaceId  = 	id+"_"+cabinetIndex+"_"+shelfIndex+"_"+slotIndex+"_"+indexOnSlot+"_"+portIndex;
				nodeBoardId = id+"_"+cabinetIndex+"_"+shelfIndex+"_"+slotIndex+"_"+indexOnSlot;
					 
					 NodeInterface ethernetInterfaceName = new NodeInterface();
					 ethernetInterfaceName.setId(interfaceId);
					 ethernetInterfaceName.setPortSpeed(portSpeed);
					 ethernetInterfaceName.setPortStatus(portStatus);
					 ethernetInterfaceName.setMacAddress(macAddress);
					 ethernetInterfaceName.setAutoNegotiation(autoNegotitaions);
					 ethernetInterfaceName.setDuplexMode(duplexMode);
					// ethernetInterfaceName.setEthernetCapacity(ethernetCapcity);
					 if(performanceDumpMaps.containsKey(interfaceId)){
						 
						 NodeInterface infaces = performanceDumpMaps.get(interfaceId);
						 String ulminThroughtput = infaces.getMinThroughtput();
						 String ulpeakThroughOut = infaces.getPeakThroughput();
						 String ulportUtilization = infaces.getPortUtilization();
						 
						 String dlminThroughtput = infaces.getDlMinThroughput();
						 String dlpeakThroughOut = infaces.getDlPeakThroughput();
						 String dlportUtilization = infaces.getDlPortUtilization();
						 
						 ethernetInterfaceName.setDlMinThroughput(dlminThroughtput);
						 ethernetInterfaceName.setDlPeakThroughput(dlpeakThroughOut);
						 ethernetInterfaceName.setDlPortUtilization(dlportUtilization);
						 
						 ethernetInterfaceName.setMinThroughtput(ulminThroughtput);
						 ethernetInterfaceName.setPeakThroughput(ulpeakThroughOut);
						 ethernetInterfaceName.setPortUtilization(ulportUtilization); 
					 }
					 ethernetInterfaceName.setNodeBoardId(nodeBoardId);
					 
					 config_EthernerMap.put(interfaceId, ethernetInterfaceName);
					 
					}catch (Exception e) {
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
    log.debug("End of parsing Module Type name on the basis of IP from Configuration file...");
			}      
	} 
 
  

	 
	private void parsePerformamcedumps() {
	 			
		 
			log.debug("Start Paring Cambium Ethernet Report Performnance dumps");
		 

			String path = r3Config.getProperty("cambium.ubr.dumps");

			if (null == path) {
				log.error("Missing path (cambium.ubr.dumps) in context file.");
				return;
			}
			File folder = new File(path);
			if (!folder.exists()) {
				log.error("Folder (" + path + ") not found");
				return;
			}
			
			String cabinetIndex = "1",shelfIndex = "1" , indexOnSlot ="0", slotIndex = "0", portIndex ="2";
			String interfaceId="";
			String site1 ="", site2="";
			String externalCode ="",capacity = "",linkType ="",linkModulation="",interfaceid="";
			String ethernetCapcity="";
		    String circle="", id = "";
			
			 
			if (!folder.exists()) {
				logErr.error("Folder (" + path + ") not found");
				log.error("Folder (" + path + ") not found");
				return;
			}
			
			List<File> aviatFiles = new ArrayList<>();
			ConnectorUtility.listofFiles(path, aviatFiles, new FilesFilterHandler.CsvFiles());
			
			
			clearHeaders();
			addHeaderToParse("Max  Uplink ThroughPut (Kbps)");
			addHeaderToParse("Min Uplink ThroughPut (Kbps)");
			addHeaderToParse("Max Downlink ThroughPut (Kbps)");
			addHeaderToParse("Min Downlink ThroughPut (Kbps)");
			addHeaderToParse("Device Name");
			addHeaderToParse("Network");
			addHeaderToParse("IPv6 Address");
			addHeaderToParse("IP Address");
	
			for (int i =0;i<aviatFiles.size();i++) {
				File file = aviatFiles.get(i);
			
				if (!file.getName().contains("cnMaestro-performance-5Minutes-ePMP-ap-sm-system-daily"))
					continue;
				
				 
				try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
 
				CsvListReader csvReader = new CsvListReader(bufferedReader, CsvPreference.EXCEL_PREFERENCE);
				
				try {
					 
					final String[] header = csvReader.getCSVHeader(true);
					boolean isOK = fillHeaderIndex(header);

					if (!isOK) {
						logErr.error("Error data in header (Interface Name, Mpbs) .. for file " + file.getPath());
						continue;
					}
				
					List<String> row = new ArrayList<String>();
					while ((row = csvReader.read()) != null) {
						try{
						String ulPort_Utilization_max = row.get(headerIndexOf("Max  Uplink ThroughPut (Kbps)"));
						String dlPort_Utilization_max = row.get(headerIndexOf("Max  Uplink ThroughPut (Kbps)"));  
						String ulPeakThrougout = row.get(headerIndexOf("Max  Uplink ThroughPut (Kbps)"));  
						String dlPeakThrougout = row.get(headerIndexOf("Max Downlink ThroughPut (Kbps)"));  
						String ulMinThrougout = row.get(headerIndexOf("Min Uplink ThroughPut (Kbps)"));  
						String dlMinThrougout = row.get(headerIndexOf("Min Downlink ThroughPut (Kbps)"));  
						String deviceName = row.get(headerIndexOf("Device Name"));
						String network = row.get(headerIndexOf("Network"));  
						String ipV6Address = row.get(headerIndexOf("IPv6 Address"));  
						String ipAddress = row.get(headerIndexOf("IP Address"));   
						
				 
						if(network.contains("default") || network.contains("NESA") || network.contains(" ")){
							 
							circle = deviceName.substring(0, Math.min(deviceName.length(), 2));
						}
						else{
							
							circle = deviceName.substring(0, Math.min(deviceName.length(), 2));
							
	                       if(circle.contains("BH")){
	            	       circle = "BR";
	                       }
	                       else if(circle.contains("JH")){
	                       circle = "BR";
	            	       }
	                       else if(circle.contains("CH")){
	            	       circle = "CN";
	                      }
						}
	             
	                   if(!ipV6Address.contains("N/A")){
	                	    id = ipV6Address+"_"+circle;   
	                   }
	                   else{
	                	    id = ipAddress+"_"+circle;
	                   }

	            			 
					interfaceId  = 	id+"_"+cabinetIndex+"_"+shelfIndex+"_"+slotIndex+"_"+indexOnSlot+"_"+portIndex;
						 
						 NodeInterface ethernetInterfaceName = new NodeInterface();

						 ethernetInterfaceName.setId(interfaceId);
						 ethernetInterfaceName.setDlMinThroughput(dlPort_Utilization_max);
						 ethernetInterfaceName.setDlPeakThroughput(dlPeakThrougout);
						 ethernetInterfaceName.setDlPortUtilization(ulPeakThrougout);
						 
						 
						 ethernetInterfaceName.setPortUtilization(ulPort_Utilization_max);
						 ethernetInterfaceName.setPeakThroughput(ulPeakThrougout);
						 ethernetInterfaceName.setMinThroughtput(ulMinThrougout);
						 performanceDumpMaps.put(interfaceId, ethernetInterfaceName);
						 
 
						}catch (Exception e) {
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
	    log.debug("End of parsing Module Type name on the basis of IP from Configuration file...");
				}      
	
}
 	
	public Map<String, NodeSlot> getSlotsMaps() {
		if (!isParsed  || slotsMap.isEmpty())
			parsingInterfacefromEthernetReport();
		
		
		//removeNodeInterfaceDuplication();
		return slotsMap;
	}

	/**
	 * @return
	 */
}