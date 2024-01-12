package com.mobinets.nps.customer.transmission.manufacture.cambium_UBR.UBR;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.mobinets.nep.model.autoimporter.ErrorAlarm;
import com.mobinets.nep.npt.NptConstants;
import com.mobinets.nps.customer.transmission.common.CommonConfig;
import com.mobinets.nps.customer.transmission.common.FilesFilterHandler;
import com.mobinets.nps.customer.transmission.common.IpMatcher;
import com.mobinets.nps.customer.transmission.common.ManufactureFixer;
import com.mobinets.nps.customer.transmission.common.NodesMatchingParser;
import com.mobinets.nps.customer.transmission.common.SiteMatchingFounder;
import com.mobinets.nps.customer.transmission.common.TransmissionCommon;
import com.mobinets.nps.customer.transmission.externalC.airtel.common.MW_EmsNameIP_Matcher;
import com.mobinets.nps.customer.transmission.manufacture.common.ConnectorUtility;
import com.mobinets.nps.customer.transmission.manufacture.ericsson.r3.common.R3NEIDFounder;
import com.mobinets.nps.daemon.common.NodeContainer;
import com.mobinets.nps.daemon.csv.AbstractFileCsvParser;
import com.mobinets.nps.model.ipaddress.IPData;
import com.mobinets.nps.model.ipaddress.IPDataV6;
import com.mobinets.nps.model.nodeinterfaces.NetworkElement;
import com.mobinets.nps.model.nodeinterfaces.NodeBoard;
import com.mobinets.nps.model.nodeinterfaces.NodeCabinet;
import com.mobinets.nps.model.nodeinterfaces.NodeInterface;
import com.mobinets.nps.model.nodeinterfaces.NodeShelf;
import com.mobinets.nps.model.nodeinterfaces.NodeSlot;
import com.mobinets.nps.model.nodeinterfaces.VirtualInterface;
import com.mobinets.nps.model.nodeinterfaces.VirtualInterface.VirtualInterfaceName;

public class CambiumUbrNetworkElementsParser extends AbstractFileCsvParser<NetworkElement> {

	private static final Log log = LogFactory.getLog(CambiumUbrNetworkElementsParser.class);
	private static final Log logErr = LogFactory.getLog("NETWORK_ELEMENT_ERROR_LOGGER");

	private Map<String, NetworkElement> mapRes;
	private Map<String, NetworkElement> elementsByName;
	private Map<String, String> ipByElementId;
	List<String> easthubList = new ArrayList<>();
	List<String> northhubList = new ArrayList<>();
	List<String> southhubList = new ArrayList<>();
	List<String> IPdatatemporaryIpList = new ArrayList<>();
	List<String> virtualIntefacesIdList = new ArrayList<>();
	private List<String> duplicateboardMap = new ArrayList<>();
	private HashMap<String,NodeBoard> boardMap = new HashMap<>();
	private Map<String, NodeInterface> nodeInterfaceMap = new HashMap<>();
	private Map<String, NodeCabinet> cabinets = new HashMap<>();
	private Map<String, NodeSlot> slotsMap = new HashMap<>();;
	private Map<String, NodeShelf> shelves = new HashMap<>();
	List<String> westhubList = new ArrayList<>();
	List<String> neIdList = new ArrayList<>();
	private List<String> interfaceIdList = new  ArrayList<>();
	private Map<String, String> circleIdMap = new HashMap<String,String>();
	
	private CambiumUbrNodeSlotsParser cambiumUbrNodeSlotsParser;
	 
	public CambiumUbrNodeSlotsParser getCambiumUbrNodeSlotsParser() {
		return cambiumUbrNodeSlotsParser;
	}

	public void setCambiumUbrNodeSlotsParser(CambiumUbrNodeSlotsParser cambiumUbrNodeSlotsParser) {
		this.cambiumUbrNodeSlotsParser = cambiumUbrNodeSlotsParser;
	}

	public Map<String, String> getCircleIdMap() {
		return circleIdMap;
	}

	public List<String> getNeIdList() {
		return neIdList;
	}


	private Map<String, NetworkElement> elementsByObjectId;
	private List<VirtualInterface> virtualInterfaces;
	// parsers output from CDL files
	private Set<String> cdlMapRes;
	// parsers output from SOEM file
	private Set<String> soemMapRes;
	// parsersoutput from MINILINK file
	private Map<String, NetworkElement> miniLinkMapRes;
	 
	private CommonConfig r3Config;
	private R3NEIDFounder r3NeIdFounder;
	private NodesMatchingParser nodesMatcher;
	private SiteMatchingFounder siteMatchFounder;
	private ManufactureFixer siteNameFixer;
	private MW_EmsNameIP_Matcher mW_EmsNameIP_Matcher;
	private IpMatcher ipMatcher;
	
	
	public void setSiteMatchFounder(SiteMatchingFounder siteMatchFounder) {
		this.siteMatchFounder = siteMatchFounder;
	}
	
	public ManufactureFixer getSiteNameFixer() {
		return siteNameFixer;
	}

	public void setNodesMatcher(NodesMatchingParser nodesMatcher) {
		this.nodesMatcher = nodesMatcher;
	}

	public void setR3Config(CommonConfig r3Config) {
		this.r3Config = r3Config;
	}

	public void setR3NeIdFounder(R3NEIDFounder r3NeIdFounder) {
		this.r3NeIdFounder = r3NeIdFounder;
	}

	public void setSiteNameFixer(ManufactureFixer siteNameFixer) {
		this.siteNameFixer = siteNameFixer;
	}

	
	public void setmW_EmsNameIP_Matcher(MW_EmsNameIP_Matcher mW_EmsNameIP_Matcher) {
		this.mW_EmsNameIP_Matcher = mW_EmsNameIP_Matcher;
	}
	
	public void setIpMatcher(IpMatcher ipMatcher) {
		this.ipMatcher = ipMatcher;
	}

	/**
	 * @return
	 */
	private void parseAviatMwDumps() {
		log.debug("Begin of Cambium Network Elements parsing from UBR Files");
		try {

			if (null == mapRes)
				mapRes = new HashMap<String, NetworkElement>();
			if (null == elementsByName)
				elementsByName = new HashMap<String, NetworkElement>();
			if (null == elementsByObjectId)
				elementsByObjectId = new HashMap<String, NetworkElement>();

			if (null == virtualInterfaces)
				virtualInterfaces = new ArrayList<VirtualInterface>();

			if (soemMapRes == null)
				soemMapRes = new HashSet<String>();
	
			if(ipByElementId==null)
				ipByElementId = new HashMap<String, String>();
			
			String path = r3Config.getProperty("cambium.ubr.dumps");

			if (null == path) {
				log.error("Missing attribute (cambium.ubr.dumps) in manufacture-config.xml.");
				return;
			}

			File folder = new File(path);

			if (!folder.exists()) {
				logErr.error("Folder (" + path + ") not found");
				log.error("Folder (" + path + ") not found");
				return;
			}
			
			List<File> aviatFiles = new ArrayList<>();
			ConnectorUtility.listofFiles(path, aviatFiles, new FilesFilterHandler.CsvFiles());
			
			
			addHeaderToParse("Device Name");
			addHeaderToParse("Device Mode");
			addHeaderToParse("Product Name");
			addHeaderToParse("Last Updated Time");
			addHeaderToParse("Status");
			addHeaderToParse("Status Time");
			addHeaderToParse("IPv6 Address");
			addHeaderToParse("IP Address"); 
			addHeaderToParse("Network");
			 
			
			for (int i =0;i<aviatFiles.size();i++) {
				File file = aviatFiles.get(i);
			
				if (!file.getName().contains("cnMaestro-device-ePMP-ap-sm-system"))
					continue;
			
				try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {

		            // Skip the first two lines
//		            for (int i1 = 0; i1 < 2; i1++) {
//		                bufferedReader.readLine();
//		            }
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
						String deviceName = row.get(headerIndexOf("Device Name"));
						String model = row.get(headerIndexOf("Product Name")).trim();
						String deviceMode = row.get(headerIndexOf("Device Mode"));
						String lastUpdatedTime = row.get(headerIndexOf("Last Updated Time")).trim();
						String status = row.get(headerIndexOf("Status")).trim();
						String statusTime = row.get(headerIndexOf("Status Time")).trim();
						String ipV6Address = row.get(headerIndexOf("IPv6 Address")).trim();
						String ipAddress = row.get(headerIndexOf("IP Address")).trim();
						String network = row.get(headerIndexOf("Network")).trim();
						
						if(deviceName.contains("DLUBR15041"))
							System.out.println();
						String circle ="";   
						String id ="",siteId="";
					    String cabinetIndex="1";
					    String shelfIndex="1";
						String indexonSlot="0";
						String slotIndex= "0"; 
						String type ="UBR";
						String manufacturerName ="Cambium";

  
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

                       if(deviceName.contains("_")){
                    	   
                    	   siteId = StringUtils.substringBeforeLast(deviceName, "_");
                    	   siteId = StringUtils.substring(siteId, 5);
                       }
                       else if(deviceName.contains("-")){
                    	   
                    	   siteId = StringUtils.substring(deviceName, 5);
                       }
                       else{
                    	   siteId = StringUtils.substring(deviceName, 5);}
                       if(model.contains("ePMP")){
                    	   model = "ePMP Force 300-25";
                    	 }
                       else if(model.contains("ePMP Force 300-25")){
                    	   model = "ePMP Force 300-25";   
                       }
                       else if(model.contains("ePMP Force 300-25L")){
                    	   model = "ePMP Force 300-25";}
                       else if(model.contains("PTP 550")){
                    	   model ="PTP 550";
                       }
                       
						
						createNode(siteId, deviceMode, circle, deviceName, id, ipV6Address, lastUpdatedTime,status,statusTime,ipAddress,model,
								cabinetIndex,shelfIndex,indexonSlot,slotIndex,type,manufacturerName);
						
 					
			 }
						 catch (Exception e) {
							log.error("Error : ", e);
						}
					}			
					csvReader.close();
				}	catch (Exception e) {
					log.error("Error : ", e);
				}	
			}catch(Exception e){
				e.printStackTrace();
				log.error("Error: ",e);
			}
	
		} 
			}catch (Exception e) {
			log.error("Error : ", e);
		}
	}			

	private void createNode(String siteId, String deviceMode, String circle, String deviceName, String id, String ipV6Address, String lastUpdatedTime,
			String status,String statusTime,String ipAddress,String model,String cabinetIdnex,String shelfIndex, String indexOnSlot, String slotIndex,String type,
			String manufactureName) {
		
		 
		
		NetworkElement ele = new NetworkElement();
		ele.setId(id);
		ele.setNeId(id);
		ele.setName(deviceName);
		ele.setExternalCode(id);
		ele.setObjectId(id);
		ele.setNeModel(model);
		ele.setManufacturerName("Cambium");
		ele.setXpicAbility(true);
		ele.setNeType("UBR");
		ele.setVersion(deviceMode);
		ele.setSite(siteId);
		ele.setStatus(status);
		
		String subnetMask =  "128";

		if (siteNameFixer.isAlfa3xImport())
			soemMapRes.add(ele.getNeId());

		if (!mapRes.containsKey(id)) {
			if (elementsByName.get(ele.getName().toLowerCase()) != null && id.equals(ipByElementId.get(elementsByName.get(ele.getName().toLowerCase()).getId()))) {
				ErrorAlarm error = new ErrorAlarm();
				error.setMessage("Duplicated Name: " + ele.getName() + " with different external code: " + ele.getNeId() + " and " + elementsByName.get(ele.getName().toLowerCase()).getNeId());
			//	error.setAdditionalInfo("File Path : " + file.getPath());
				error.setNeType(NptConstants.IDU);
				error.setType(ErrorAlarm.NETWORK_ERROR);
				NodeContainer.addErrorToList(error);
				ele.setName(ele.getName() + "_" + ele.getNeId());
				ele.setObjectId(ele.getNeId());
			}
			
			mapRes.put(ele.getNeId(), ele);
		 
			
			String cabinetIds = id + "_" + cabinetIdnex;
			String shelfIds = cabinetIds + "_" + shelfIndex;
			String slotId = shelfIds + "_" + indexOnSlot;int portIdx =1;
			
			NodeCabinet nodeCabinet = createNodeCabinet(cabinetIds, type,model,manufactureName,ele);
			NodeShelf nodeShelf = createNodeShelf(shelfIds,shelfIndex, nodeCabinet,type,manufactureName,model);
			
			NodeSlot nodeSlot = createNodeSlot(id, slotId, indexOnSlot, nodeShelf);

			String boardIds = slotId + "_" + indexOnSlot;
			
	 
			NodeBoard nodeBoard = boardMap.get(boardIds);
			 
			String strIndexOnSlot = String.valueOf(indexOnSlot);
			nodeBoard = createNodeBoard(id, slotId, indexOnSlot, boardIds,
					TransmissionCommon.convertStringToInteger(strIndexOnSlot), type,
					model,nodeSlot);
			boardMap.put(boardIds, nodeBoard);
			duplicateboardMap.add(boardIds);
			
			String interfaceid = id+"_"+cabinetIdnex+"_"+shelfIndex+"_"+slotIndex+"_"+indexOnSlot+"_"+portIdx;
 
		NodeInterface nodeInterface = createNodeInterface(interfaceid,boardIds,portIdx,nodeBoard,nodeSlot,id);
		nodeBoard.getNodeInterfaces().add(nodeInterface);
		  nodeSlot.getNodeBoards().add(nodeBoard);
		   slotsMap.put(nodeSlot.getId(), nodeSlot);
		   interfaceIdList.add(interfaceid);
		   
		 

			
			
			
			// dont touch//////
			if (ipV6Address != null && !ipV6Address.isEmpty()) {
				String virtualInterfaceId ="";
				if(ipV6Address.contains(".")){
					subnetMask = "255.255.255.255";
				 virtualInterfaceId = ipV6Address+"-"+ipAddress+"-"+"255.255.255.255";
				}
				else{
				 virtualInterfaceId = ipV6Address+"-"+ipAddress+"-"+"128";
				}
				if(!virtualIntefacesIdList.contains(virtualInterfaceId)){
				 
				
				VirtualInterface vi = new VirtualInterface();
				vi.setInterfaceName(VirtualInterfaceName.MANAGEMENT1.toString());
				vi.setId(virtualInterfaceId);
				vi.setNodeId(id);
				if(!id.contains(":")){
					if(!IPdatatemporaryIpList.contains(ipAddress)){
				vi.setIpData(NodeContainer.getIpDataForName(id + "_" + ipAddress, ipAddress, subnetMask));
					}
				}
				if(!id.contains(".")){
				IPDataV6 ipDatav6 = NodeContainer.getIpDataV6ForNameNokia(id, virtualInterfaceId,
					"123", VirtualInterfaceName.MANAGEMENT1.toString());
				vi.setIpDataV6(ipDatav6);
				}
				
			//	vi.setIpDataV6(ipDatav6);
				virtualInterfaces.add(vi);
				virtualIntefacesIdList.add(virtualInterfaceId);
				IPdatatemporaryIpList.add(ipAddress);
				}
				
			}
			
			elementsByName.put(ele.getName().toLowerCase(), ele);
			elementsByObjectId.put(ele.getObjectId(), ele);
		}
	}
 
	private NodeInterface createNodeInterface(String interfaceId, String boardIds, int portIdx, NodeBoard nodeBoard,
			NodeSlot nodeSlot,String id) {
		 
	NodeInterface interfaces =  new NodeInterface();{
			
			 
			
				interfaces.setId(interfaceId);
				String portvalue1 = StringUtils.substringAfterLast(interfaceId, "_");
				int portindex1 = Integer.valueOf(portvalue1);
				interfaces.setInterfaceIndex(portindex1);
				interfaces.setPhysicalAddress(id);
			    interfaces.setNodeBoardId(boardIds);
				nodeInterfaceMap.put(interfaceId, interfaces);
				interfaceIdList.add(interfaceId);
			
		}
	return interfaces;
	}

	private NodeBoard createNodeBoard(String id, String slotId, String indexOnSlot, String boardIds,
			Integer convertStringToInteger, String type, String model, NodeSlot nodeSlot) {

		 
		String indexOnslot =  String.valueOf(indexOnSlot); 
		NodeBoard board=new NodeBoard();
		
//		String boardId=TransmissionCommon.concatenateStrings("_", slotId, indexOnslot);
	 	board.setId(boardIds);
	 	board.setExternalCode(boardIds);
		board.setBoardIndexOnSlot(Integer.parseInt(indexOnslot));
		board.setSlotId(slotId);
		board.setBoardTypeCode(NodeContainer.getBoardDictionnaryForName(model));
		board.setDescription(model);
		board.setModel(model);
		return board;
	}

	private NodeSlot createNodeSlot(String id, String slotId, String indexOnSlot, NodeShelf nodeShelf) {
		   
		Integer intSlot = Integer.valueOf(indexOnSlot);
			NodeSlot nodeSlot = slotsMap.get(slotId);
			if (nodeSlot == null) {
				nodeSlot = new NodeSlot();
				nodeSlot.setId(slotId);
				nodeSlot.setNodeId(id);
				nodeSlot.setSlotIndex(intSlot);
				nodeSlot.setShelf(nodeShelf);

				slotsMap.put(slotId, nodeSlot);
			}

			return nodeSlot;
		}
	

	private NodeShelf createNodeShelf(String shelfIds, String shelfIndex, NodeCabinet nodeCabinet, String type,
			String manufactureName, String model) {

 
		NodeShelf nodeShelf = new NodeShelf();
			
		    nodeShelf.setId(shelfIds);
			nodeShelf.setModel(type);
			nodeShelf.setStartRu(0);
			nodeShelf.setShelfIndex(1);
			nodeShelf.setModel(model);
			nodeShelf.setShelfType(NodeContainer.getNodeShelfTypeForName(model));
			nodeShelf.setCabinet(nodeCabinet);
			nodeShelf.setManufacturerName(manufactureName);
			NodeContainer.getNodeShelfForName(shelfIds, nodeShelf);
//			NodeContainer.getNodeShelfForName(nodeShelf.getId(), nodeShelf.getShelfIndex(), nodeShelf.getModel());
			shelves.put(shelfIds, nodeShelf);
		
		return nodeShelf;
	}

	private NodeCabinet createNodeCabinet(String cabinetIds, String type, String model, String manufactureName,
			NetworkElement ele) {
       
		    NodeCabinet nodeCabinet = new NodeCabinet();
			nodeCabinet.setId(cabinetIds);
			nodeCabinet.setModel(model);
			nodeCabinet.setNodeId(cabinetIds);
			nodeCabinet.setNodeType(type);
			nodeCabinet.setCabinetType(NodeContainer.getNodeCabinetTypeForName(model));
			nodeCabinet.setCabinetIndex(1);
			nodeCabinet.setManufacturerName(manufactureName);
			nodeCabinet = NodeContainer.getNodeCabinetForName(cabinetIds, nodeCabinet);
			//NodeContainer.getNodeCabinetForName(nodeCabinet.getId(), nodeCabinet.getCabinetIndex(),
				//	nodeCabinet.getModel());
			cabinets.put(cabinetIds, nodeCabinet);
		  
		return nodeCabinet;
	}

	/**
	 * @return
	 */

	public Map<String, NetworkElement> getMapOfNetworkElement() {
		if(mapRes==null)
			this.parseAviatMwDumps();
		return mapRes;
	}
	
	public Map<String, NodeSlot> getSlotsMaps() {
		if(slotsMap.isEmpty())
			this.parseAviatMwDumps();
		removeNodeInterfaceDuplication();
		List<NodeInterface> ni  = new ArrayList<NodeInterface>();

		if (cambiumUbrNodeSlotsParser != null && !slotsMap.isEmpty()) {
			ni.addAll(cambiumUbrNodeSlotsParser.getConfig_EthernerMap().values());

		}
		addNodeInterfaces(slotsMap, ni);
		
		return slotsMap;
	}
	
	
	private void addNodeInterfaces(Map<String, NodeSlot> nodeSlotMap, List<NodeInterface> nodeInterfaces) {

		for (NodeInterface nodeInterface : nodeInterfaces) {
			String boardId = nodeInterface.getNodeBoardId();

			if (boardId == null)
				continue;

			Integer lastIndexOf_ = boardId.lastIndexOf("_");

			String slotId = boardId.substring(0, lastIndexOf_);
			NodeSlot nodeSlot = nodeSlotMap.get(slotId);

			if (nodeSlot == null)
				continue;

			for (NodeBoard nodeBoard : nodeSlot.getNodeBoards()) {
				if (nodeBoard.getId().equalsIgnoreCase(boardId)) {
						nodeBoard.getNodeInterfaces().add(nodeInterface);
					}}}}
	
	 private void removeNodeInterfaceDuplication() {
			for(NodeSlot nodeSlot : slotsMap.values()) {
				
				try{
					Map<String,NodeBoard> map = new HashMap<>();
					
					for(NodeBoard board : nodeSlot.getNodeBoards()) {
						map.put(board.getId(), board);
					}
					
					nodeSlot.getNodeBoards().clear();
					nodeSlot.setNodeBoards(map.values());
				}
				catch(Exception e){
					
				}
				
				for(NodeBoard board : nodeSlot.getNodeBoards()) {
								
					try{
					Map<String,NodeInterface> map = new HashMap<>();
					for(NodeInterface nodeInterface : board.getNodeInterfaces()) {
						map.put(nodeInterface.getId(), nodeInterface);
					}
					board.getNodeInterfaces().clear();
					board.setNodeInterfaces(map.values());
					}
					catch (Exception e) {
						// TODO: handle exception
					}
				}
			}
			
		}

	/**
	 * @return
	 */
	public List<VirtualInterface> getVirtualInterfaces() {
		if (virtualInterfaces == null || virtualInterfaces.size() == 0)
			this.getMapOfNetworkElement();
		return virtualInterfaces;
	}
	
	public void clearVirtualInterfaces(){
		virtualInterfaces = null;
	}
	
	public void clearElements(){	
		mapRes = null;
		elementsByName = null;
		elementsByObjectId = null;
		virtualInterfaces = null;
		cdlMapRes = null;
		soemMapRes = null;
		miniLinkMapRes = null;
       
	}


}
