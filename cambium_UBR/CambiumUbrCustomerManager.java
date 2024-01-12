package com.mobinets.nps.customer.transmission.manufacture.cambium_UBR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mobinets.nps.customer.CustomerTransmissionDefaultDataProvider;
import com.mobinets.nps.customer.transmission.manufacture.cambium_UBR.UBR.CambiumUbrNetworkElementsParser;
import com.mobinets.nps.customer.transmission.manufacture.cambium_UBR.UBR.CambiumUbrNodeSlotsParser;
import com.mobinets.nps.customer.transmission.manufacture.cambium_UBR.UBR.CambiumUbrTransmissionLinkParser;
import com.mobinets.nps.model.customer.data.element.ElementMwConfiguration;
import com.mobinets.nps.model.customer.data.element.ElementTransmissionLink;
import com.mobinets.nps.model.network.ElementAdditionalInfo;
import com.mobinets.nps.model.nodeinterfaces.NetworkElement;
import com.mobinets.nps.model.nodeinterfaces.NodeBoard;
import com.mobinets.nps.model.nodeinterfaces.NodeInterface;
import com.mobinets.nps.model.nodeinterfaces.NodeSlot;
import com.mobinets.nps.model.nodeinterfaces.VirtualInterface;

public class CambiumUbrCustomerManager extends CustomerTransmissionDefaultDataProvider {

	private static final Log log = LogFactory.getLog(CambiumUbrCustomerManager.class);
	private String wataniyaPlanning;
 
	
	private CambiumUbrNetworkElementsParser cambiumUbrNetworkElementsParser;
	
	public void setCambiumUbrNetworkElementsParser(CambiumUbrNetworkElementsParser cambiumUbrNetworkElementsParser) {
		this.cambiumUbrNetworkElementsParser = cambiumUbrNetworkElementsParser;
	}
	private CambiumUbrNodeSlotsParser cambiumUbrNodeSlotsParser;
	
	public void setCambiumUbrNodeSlotsParser(CambiumUbrNodeSlotsParser cambiumUbrNodeSlotsParser) {
		this.cambiumUbrNodeSlotsParser = cambiumUbrNodeSlotsParser;
	}
	private CambiumUbrTransmissionLinkParser cambiumUbrTransmissionLinkParser;
	 

	 
	public void setCambiumUbrTransmissionLinkParser(CambiumUbrTransmissionLinkParser cambiumUbrTransmissionLinkParser) {
		this.cambiumUbrTransmissionLinkParser = cambiumUbrTransmissionLinkParser;
	}


	public void setWataniyaPlanning(String wataniyaPlanning) {
		this.wataniyaPlanning = wataniyaPlanning;
	}
 
	 
	@Override
	public List<NetworkElement> getNetworkElement() {
		log.debug("Parsing List of Network Element for Cambium UBR");
		return new ArrayList<NetworkElement>(cambiumUbrNetworkElementsParser.getMapOfNetworkElement().values());
	}
	
	public List<NodeSlot> getNodeSlot() {
		Map<String, NodeSlot> slotMap = new HashMap<>();
		if (cambiumUbrNetworkElementsParser != null && slotMap.isEmpty() && cambiumUbrNetworkElementsParser.getSlotsMaps() != null) {
			slotMap.putAll(cambiumUbrNetworkElementsParser.getSlotsMaps());
		}
		return new ArrayList<>(slotMap.values());
	}
	
//	@Override
//	public List<NodeSlot> getNodeSlot() {
//		long startTime = OSSLogging.setStartMethod(getClass(), "CambiumUBR::getNodeSlot");
//		Map<String, NodeSlot> slotMap = new HashMap<>();
//		Map<String, NodeSlot> results = new HashMap<String, NodeSlot>();
//		List<NodeInterface> ni  = new ArrayList<NodeInterface>();
//		Map<String,NodeInterface> nii  = new HashMap<String,NodeInterface>();
//
//		if (cambiumUbrNodeSlotsParser != null && slotMap.isEmpty()) {
//			slotMap.putAll(cambiumUbrNodeSlotsParser.getSlotsMaps());
//			ni.addAll(cambiumUbrNodeSlotsParser.getEthernetMap().values());
//			nii =cambiumUbrNodeSlotsParser.getLinktotalCpacitymap();
//		}
//		 
// 
//		addNodeInterfaces(slotMap, ni);
////		updateNodeIntefaceCapacity(slotMap, nii);
////		for(NodeSlot slot :slotMap.values()){
////			results.put(slot.getId(), slot);
////		}
////		
////		removeDuplicatedBoards(results);
////
//		OSSLogging.setEndMethod(getClass(), "CambiumUBR::getNodeSlot", startTime);
//		return new ArrayList<>(slotMap.values());
//		//return new ArrayList<>(slotMap.values());
//	}

	private void updateNodeIntefaceCapacity(Map<String, NodeSlot> nodeSlotMap, Map<String,NodeInterface> nii) {
		for (NodeInterface nodeInterface : nii.values()) {
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
					Collection<NodeInterface> listNodeInterface = nodeBoard.getNodeInterfaces();
					if (listNodeInterface == null) {
						listNodeInterface = new ArrayList<NodeInterface>();
						nodeBoard.setNodeInterfaces(listNodeInterface);
					}

					if (nodeInterface != null && listNodeInterface.contains(nodeInterface)) {
						for (NodeInterface physNodeInterface : listNodeInterface) {
							if (physNodeInterface.getId().equalsIgnoreCase(nodeInterface.getId())) {
								physNodeInterface.setTotalLinkCapacity((nodeInterface.getTotalLinkCapacity()));
								// physNodeInterface.setExternalCode(nodeInterface.getCapacity());
								// physNodeInterface.setExternalCode(nodeInterface.getTotalLinkCapacity());
								break;
							}
						}
					} else {
						nodeBoard.getNodeInterfaces().add(nodeInterface);
					}
				}
			}
		}
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
					Collection<NodeInterface> listNodeInterface = nodeBoard.getNodeInterfaces();
					if (listNodeInterface == null) {
						listNodeInterface = new ArrayList<NodeInterface>();
						nodeBoard.setNodeInterfaces(listNodeInterface);
					}

					if (nodeInterface != null && listNodeInterface.contains(nodeInterface)) {
						for (NodeInterface physNodeInterface : listNodeInterface) {
							if (physNodeInterface.getId().equalsIgnoreCase(nodeInterface.getId())) {
								String desc = nodeInterface.getDescription();
								if(desc == null){
									physNodeInterface.setPortSpeed(nodeInterface.getPortSpeed());
									physNodeInterface.setPortUtilization(nodeInterface.getPortUtilization());
									physNodeInterface.setPortStatus(nodeInterface.getPortStatus());
									physNodeInterface.setMinThroughtput(nodeInterface.getMinThroughtput());
									physNodeInterface.setPeakThroughput(nodeInterface.getPeakThroughput());
									nodeBoard.getNodeInterfaces().add(physNodeInterface);
								}
								else{
									physNodeInterface.setSubInterfaces(nodeInterface.getSubInterfaces());
									physNodeInterface.setIpData(nodeInterface.getIpData());
									physNodeInterface.setVlanId(nodeInterface.getVlanId());
									physNodeInterface.setVlanSetTrunk(nodeInterface.getVlanSetTrunk());
									physNodeInterface.setExternalCode(nodeInterface.getExternalCode());
									physNodeInterface.setPortSpeed(nodeInterface.getPortSpeed());
									physNodeInterface.setPortUtilization(nodeInterface.getPortUtilization());
									physNodeInterface.setPortStatus(nodeInterface.getPortStatus());
									physNodeInterface.setMinThroughtput(nodeInterface.getMinThroughtput());
									physNodeInterface.setPeakThroughput(nodeInterface.getPeakThroughput());
									physNodeInterface.setPartnumber(nodeInterface.getPartnumber());
									// physNodeInterface.setExternalCode(nodeInterface.getCapacity());
									// physNodeInterface.setExternalCode(nodeInterface.getTotalLinkCapacity());
									break;
								}
//								physNodeInterface.setSubInterfaces(nodeInterface.getSubInterfaces());
//								physNodeInterface.setIpData(nodeInterface.getIpData());
//								physNodeInterface.setVlanId(nodeInterface.getVlanId());
//								physNodeInterface.setVlanSetTrunk(nodeInterface.getVlanSetTrunk());
//								physNodeInterface.setExternalCode(nodeInterface.getExternalCode());
//								physNodeInterface.setPortSpeed(nodeInterface.getPortSpeed());
//								physNodeInterface.setPortUtilization(nodeInterface.getPortUtilization());
//								physNodeInterface.setPortStatus(nodeInterface.getPortStatus());
//								physNodeInterface.setMinThroughtput(nodeInterface.getMinThroughtput());
//								physNodeInterface.setPeakThroughput(nodeInterface.getPeakThroughput());
//								physNodeInterface.setPartnumber(nodeInterface.getPartnumber());
//								// physNodeInterface.setExternalCode(nodeInterface.getCapacity());
//								// physNodeInterface.setExternalCode(nodeInterface.getTotalLinkCapacity());
//								break;
							}
						}
					} else {
						nodeBoard.getNodeInterfaces().add(nodeInterface);
					}
				}
			}
		}

	}
	/*@Override
	public List<NodeSlot> getNodeSlot() {
		if (wataniyaPlanning.equalsIgnoreCase("true"))
			return Collections.emptyList();
		log.debug("Parsing List of Node Slot for Aviat MW");
		try {
			 
			Map<String, NodeSlot> nodeSlots = new HashMap<String, NodeSlot>();
			if(nodeSlots.isEmpty()){
			 
			List<NodeInterface> ni = new ArrayList<NodeInterface>();
			ni.addAll(aviatmwNodeSlotsParser.getEthernetMap().values());
			 

			updateNodeIntefaceType(ni, mwConf);

			addNodeInterfaces(nodeSlots, ni);

		//	return new ArrayList<NodeSlot>(aviatmwNodeSlotsParser.getNodeSlotsElements());
			return new ArrayList<>(new HashSet<>(aviatmwNodeSlotsParser.getNodeSlotsElements()));
			}
		} catch (IOException e) {
			e.printStackTrace();
			log.error("Error : ", e);
		}

		return new ArrayList<NodeSlot>();
	}*/
	
//	@Override
//	public List<ElementTransmissionLink> getTrsLinkList() {
//		if (wataniyaPlanning.equalsIgnoreCase("true"))
//			return Collections.emptyList();
//		log.debug("Parsing List of Transmission Link for Ericsson R3");
//		try {
//			return aviatTransmissionLinkParser.getElementTrsLinks();
//		} catch (IOException e) {
//			log.error("Error : ", e);
//		}
//		return Collections.emptyList();
//	}
	
	@Override
	public List<ElementTransmissionLink> getTrsLinkList() {
		if (wataniyaPlanning.equalsIgnoreCase("true"))
			return Collections.emptyList();
		List<ElementTransmissionLink> result = new ArrayList<>();
		if(cambiumUbrTransmissionLinkParser != null)
			try {
				result.addAll(cambiumUbrTransmissionLinkParser.getElementTrsLinks());
				wataniyaPlanning ="true";
			} catch (IOException e) {
				e.printStackTrace();
			}
		return result;
	}
	
	@Override
	public List<ElementMwConfiguration> getElementMwConfigurationList() {
		List<ElementMwConfiguration> result = new ArrayList<>();
		if(cambiumUbrTransmissionLinkParser != null)
			try {
				result.addAll(cambiumUbrTransmissionLinkParser.getLinkDataforMwConfig());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return result;
	}
	
	@Override
	public List<VirtualInterface> getVirtualInterfaces() {
		log.debug("Parsing List of Virtual Interfaces for Ericsson");
		List<VirtualInterface> list = cambiumUbrNetworkElementsParser.getVirtualInterfaces();

		return list;
	}
	
	@Override
	public List<ElementAdditionalInfo> getAddInfoList() {
		List<ElementAdditionalInfo> addInfos = new ArrayList<>();
		log.debug("Start Getting AdditionalInfo from TransmissionLinkParser");
		try {
			addInfos.addAll(cambiumUbrTransmissionLinkParser.getAdditionalInfos());
	 
		} catch (Exception e) {
			e.printStackTrace();
		}
 		return addInfos;
	}
	private void removeDuplicatedBoards(Map<String, NodeSlot> nodeSlotMap) {
		for(NodeSlot slot :nodeSlotMap.values()){
			try{
			Map<String, NodeBoard> bordsMap = new HashMap<>();
			for(NodeBoard board :slot.getNodeBoards()){
				bordsMap.put(board.getId(), board);
			}
			slot.getNodeBoards().clear();
			slot.getNodeBoards().addAll(bordsMap.values());
		}
		
		catch (Exception e) {
		}
		}
	}

}
 
 
//	private void addNodeInterfaces(Map<String, NodeSlot> nodeSlotMap, List<NodeInterface> nodeInterfaces) {
//
//		for (NodeInterface nodeInterface : nodeInterfaces) {
//			String boardId = nodeInterface.getNodeBoardId();
//
//			if (boardId == null)
//				continue;
//
//			Integer lastIndexOf_ = boardId.lastIndexOf("-");
//
//			String slotId = boardId.substring(0, lastIndexOf_);
//			NodeSlot nodeSlot = nodeSlotMap.get(slotId);
//
//			if (nodeSlot == null)
//				continue;
//
//			for (NodeBoard nodeBoard : nodeSlot.getNodeBoards()) {
//				if (nodeBoard.getId().equalsIgnoreCase(boardId)) {
//					Collection<NodeInterface> listNodeInterface = nodeBoard.getNodeInterfaces();
//					if (listNodeInterface == null) {
//						listNodeInterface = new ArrayList<NodeInterface>();
//						nodeBoard.setNodeInterfaces(listNodeInterface);
//					}
//
//					if (nodeInterface != null && listNodeInterface.contains(nodeInterface)) {
//						for (NodeInterface physNodeInterface : listNodeInterface) {
//							if (physNodeInterface.getId().equalsIgnoreCase(nodeInterface.getId())) {
//								physNodeInterface.setSubInterfaces(nodeInterface.getSubInterfaces());
//								physNodeInterface.setIpData(nodeInterface.getIpData());
//								physNodeInterface.setInterfaceName(nodeInterface.getInterfaceName());
//								physNodeInterface.setVlanId(nodeInterface.getVlanId());
//								physNodeInterface.setVlanSetTrunk(nodeInterface.getVlanSetTrunk());
//								physNodeInterface.setExternalCode(nodeInterface.getExternalCode());
//								physNodeInterface.setPortSpeed(nodeInterface.getPortSpeed());
//								physNodeInterface.setPortUtilization(nodeInterface.getPortUtilization());
//								physNodeInterface.setPortStatus(nodeInterface.getPortStatus());
//								physNodeInterface.setMinThroughtput(nodeInterface.getMinThroughtput());
//								physNodeInterface.setPeakThroughput(nodeInterface.getPeakThroughput());
//								// physNodeInterface.setExternalCode(nodeInterface.getCapacity());
//								// physNodeInterface.setExternalCode(nodeInterface.getTotalLinkCapacity());
//								break;
//							}
//						}
//					} else {
//						nodeBoard.getNodeInterfaces().add(nodeInterface);
//					}
//				}
//			}
//		}
//
//	}
//
 