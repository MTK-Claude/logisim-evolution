/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.bfh.logisim.fpgagui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.fpgaboardeditor.BoardRectangle;
import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;
import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer.IOComponentTypes;
import com.bfh.logisim.fpgaboardeditor.PinActivity;
import com.cburch.logisim.std.io.DipSwitch;
import com.cburch.logisim.std.io.PortIO;
import com.cburch.logisim.std.wiring.Pin;

public class MappableResourcesContainer {

	final static Logger logger = LoggerFactory
			.getLogger(MappableResourcesContainer.class);

	private Map<ArrayList<String>, NetlistComponent> myMappableResources;
	public String currentBoardName;
	private BoardInformation currentUsedBoard;
	private Map<String, BoardRectangle> mappedList;
	private String toplevelName;
	private Map<String, Integer> fpgaInputsList;
	private Map<String, Integer> fpgaInOutsList;
	private Map<String, Integer> fpgaOutputsList;
	private Integer nrOfFPGAInputPins = 0;
	private Integer nrOfFPGAInOutPins = 0;
	private Integer nrOfFPGAOutputPins = 0;

	/*
	 * We differentiate two notation for each component, namely: 1) The display
	 * name: "LED: /LED1". This name can be augmented with alternates, e.g. a
	 * 7-segment display could either be: "SEVENSEGMENT: /DS1" or
	 * "SEVENSEGMENT: /DS1#Segment_A",etc.
	 * 
	 * 2) The map name: "FPGA4U:/LED1" or "FPGA4U:/DS1#Segment_A", etc.
	 * 
	 * The MappedList keeps track of the display names.
	 */
	public MappableResourcesContainer(BoardInformation CurrentBoard,
			Netlist RootNetlist) {
		ArrayList<String> Toplevel = new ArrayList<String>();
		Toplevel.add(CurrentBoard.getBoardName());
		myMappableResources = RootNetlist.GetMappableResources(Toplevel, true);
		currentBoardName = CurrentBoard.getBoardName();
		currentUsedBoard = CurrentBoard;
		toplevelName = RootNetlist.getCircuitName();
		mappedList = new HashMap<String, BoardRectangle>();
		rebuildMappedLists();
	}

	public void BuildIOMappingInformation() {
		if (fpgaInputsList == null) {
			fpgaInputsList = new HashMap<String, Integer>();
		} else {
			fpgaInputsList.clear();
		}
		if (fpgaInOutsList == null) {
			fpgaInOutsList = new HashMap<String, Integer>();
		} else {
			fpgaInOutsList.clear();
		}
		if (fpgaOutputsList == null) {
			fpgaOutputsList = new HashMap<String, Integer>();
		} else {
			fpgaOutputsList.clear();
		}
		nrOfFPGAInputPins = 0;
		nrOfFPGAInOutPins = 0;
		nrOfFPGAOutputPins = 0;
		for (ArrayList<String> key : myMappableResources.keySet()) {
			NetlistComponent comp = myMappableResources.get(key);
			for (String Map : GetMapNamesList(key, comp)) {
				FPGAIOInformationContainer BoardComp = currentUsedBoard
						.GetComponent(comp.getMap(Map));
				if (BoardComp.GetType().equals(IOComponentTypes.Pin)) {
					if (comp.getEnd(0).IsOutputEnd()) {
						fpgaInputsList.put(Map, nrOfFPGAInputPins);
						nrOfFPGAInputPins++;
					} else {
						fpgaOutputsList.put(Map, nrOfFPGAOutputPins);
						nrOfFPGAOutputPins++;
					}
				} else {
					int NrOfPins = IOComponentTypes
							.GetFPGAInputRequirement(BoardComp.GetType());
					if (NrOfPins != 0) {
						fpgaInputsList.put(Map, nrOfFPGAInputPins);
						if (BoardComp.GetType().equals(
								IOComponentTypes.DIPSwitch)) {
							nrOfFPGAInputPins += BoardComp.getNrOfPins();
						} else if (BoardComp.GetType().equals(
								IOComponentTypes.PortIO)) {
							nrOfFPGAInputPins += BoardComp.getNrOfPins();
						} else {
							nrOfFPGAInputPins += NrOfPins;
						}
					}
					NrOfPins = IOComponentTypes
							.GetFPGAOutputRequirement(BoardComp.GetType());
					if (NrOfPins != 0) {
						fpgaOutputsList.put(Map, nrOfFPGAOutputPins);
						nrOfFPGAOutputPins += NrOfPins;
					}
					NrOfPins = IOComponentTypes
							.GetFPGAInOutRequirement(BoardComp.GetType());
					if (NrOfPins != 0) {
						fpgaInOutsList.put(Map, nrOfFPGAInOutPins);
						if (BoardComp.GetType().equals(IOComponentTypes.PortIO)) {
							nrOfFPGAInOutPins += BoardComp.getNrOfPins();
						} else {
							nrOfFPGAInOutPins += NrOfPins;
						}
					}
				}
			}
		}
	}

	private String DisplayNametoMapName(String item) {
		String[] parts = item.split(" ");
		if (parts.length != 2) {
			logger.error("Internal error");
			return "";
		}
		return currentBoardName + ":" + parts[1];
	}

	private int getBestComponent(ArrayList<Integer> list, int requiredPin) {
		int delta = 999;
		int bestMatch = -1;
		for (Integer comp : list) {
			if (comp.equals(requiredPin)) {
				return list.indexOf(comp);
			}
			if (requiredPin < comp && ((comp - requiredPin) < delta)) {
				bestMatch = comp;
			}
		}
		return list.indexOf(bestMatch);
	}

	public NetlistComponent GetComponent(ArrayList<String> hiername) {
		if (myMappableResources.containsKey(hiername)) {
			return myMappableResources.get(hiername);
		} else {
			return null;
		}
	}

	public Set<ArrayList<String>> GetComponents() {
		return myMappableResources.keySet();
	}

	public String GetDisplayName(BoardRectangle rect) {
		for (String Map : mappedList.keySet()) {
			if (mappedList.get(Map).equals(rect)) {
				return MapNametoDisplayName(Map);
			}
		}
		return "";
	}

	public int GetFPGAInOutPinId(String MapName) {
		if (fpgaInOutsList.containsKey(MapName)) {
			return fpgaInOutsList.get(MapName);
		}
		return -1;
	}

	public int GetFPGAInputPinId(String MapName) {
		if (fpgaInputsList.containsKey(MapName)) {
			return fpgaInputsList.get(MapName);
		}
		return -1;
	}

	public int GetFPGAOutputPinId(String MapName) {
		if (fpgaOutputsList.containsKey(MapName)) {
			return fpgaOutputsList.get(MapName);
		}
		return -1;
	}

	public ArrayList<String> GetFPGAPinLocs(int FPGAVendor) {
		ArrayList<String> Contents = new ArrayList<String>();
		for (String Map : fpgaInputsList.keySet()) {
			int InputId = fpgaInputsList.get(Map);
			if (!mappedList.containsKey(Map)) {
				logger.warn("No mapping found for {}", Map);
				return Contents;
			}
			BoardRectangle rect = mappedList.get(Map);
			FPGAIOInformationContainer Comp = currentUsedBoard
					.GetComponent(rect);
			Contents.addAll(Comp.GetPinlocStrings(FPGAVendor, "in", InputId));
		}
		for (String Map : fpgaInOutsList.keySet()) {
			int InOutId = fpgaInOutsList.get(Map);
			if (!mappedList.containsKey(Map)) {
				logger.warn("No mapping found for {}", Map);
				return Contents;
			}
			BoardRectangle rect = mappedList.get(Map);
			FPGAIOInformationContainer Comp = currentUsedBoard
					.GetComponent(rect);
			Contents.addAll(Comp.GetPinlocStrings(FPGAVendor, "inout", InOutId));
		}
		for (String Map : fpgaOutputsList.keySet()) {
			int OutputId = fpgaOutputsList.get(Map);
			if (!mappedList.containsKey(Map)) {
				logger.warn("No mapping found for {}", Map);
				return Contents;
			}
			BoardRectangle rect = mappedList.get(Map);
			FPGAIOInformationContainer Comp = currentUsedBoard
					.GetComponent(rect);
			Contents.addAll(Comp.GetPinlocStrings(FPGAVendor, "out", OutputId));
		}
		return Contents;
	}

	private ArrayList<String> GetHierarchyKey(String str) {
		ArrayList<String> result = new ArrayList<String>();
		String[] subtype = str.split("#");
		String[] iotype = subtype[0].split(" ");
		String[] parts = iotype[iotype.length - 1].split("/");
		result.add(currentBoardName);
		for (int i = 1; i < parts.length; i++) {
			result.add(parts[i]);
		}
		return result;
	}

	public BoardRectangle GetMap(String id) {
		ArrayList<String> key = GetHierarchyKey(id);
		NetlistComponent MapComp = myMappableResources.get(key);
		if (MapComp == null) {
			logger.error("Internal error!");
			return null;
		}
		return MapComp.getMap(DisplayNametoMapName(id));
	}

	public ArrayList<String> GetMapNamesList(ArrayList<String> HierName) {
		if (myMappableResources.containsKey(HierName)) {
			NetlistComponent comp = myMappableResources.get(HierName);
			return GetMapNamesList(HierName, comp);
		}
		return null;
	}

	private ArrayList<String> GetMapNamesList(ArrayList<String> hiername,
			NetlistComponent comp) {
		ArrayList<String> result = new ArrayList<String>();
		StringBuffer name = new StringBuffer();
		/* we strip off the board name and add the component type */
		name.append(currentBoardName + ":");
		for (int i = 1; i < hiername.size(); i++) {
			name.append("/");
			name.append(hiername.get(i));
		}
		if (comp.AlternateMappingEnabled(hiername)) {
			for (int i = 0; i < comp.GetIOInformationContainer()
					.GetNrOfInports(); i++) {
				result.add(name.toString() + "#"
						+ comp.GetIOInformationContainer().GetInportLabel(i));
			}
			for (int i = 0; i < comp.GetIOInformationContainer()
					.GetNrOfInOutports(); i++) {
				result.add(name.toString() + "#"
						+ comp.GetIOInformationContainer().GetInOutportLabel(i));
			}
			for (int i = 0; i < comp.GetIOInformationContainer()
					.GetNrOfOutports(); i++) {
				result.add(name.toString() + "#"
						+ comp.GetIOInformationContainer().GetOutportLabel(i));
			}
		} else {
			result.add(name.toString());
		}
		return result;
	}

	public Collection<BoardRectangle> GetMappedRectangles() {
		return mappedList.values();
	}

	public int GetNrOfPins(String MapName) {
		if (mappedList.containsKey(MapName)) {
			FPGAIOInformationContainer BoardComp = currentUsedBoard
					.GetComponent(mappedList.get(MapName));
			if (BoardComp.GetType().equals(IOComponentTypes.DIPSwitch)) {
				return BoardComp.getNrOfPins();
			} else if (BoardComp.GetType().equals(IOComponentTypes.PortIO)) {
				return BoardComp.getNrOfPins();
			} else {
				return IOComponentTypes.GetNrOfFPGAPins(BoardComp.GetType());
			}
		}
		return 0;
	}

	public int GetNrOfToplevelInOutPins() {
		return nrOfFPGAInOutPins;
	}

	public int GetNrOfToplevelInputPins() {
		return nrOfFPGAInputPins;
	}

	public int GetNrOfToplevelOutputPins() {
		return nrOfFPGAOutputPins;
	}

	public ArrayList<BoardRectangle> GetSelectableItemsList(String DisplayName,
			BoardInformation BoardInfo) {
		ArrayList<BoardRectangle> List;
		ArrayList<String> key = GetHierarchyKey(DisplayName);
		NetlistComponent comp = myMappableResources.get(key);
		int pinNeeded = comp.GetIOInformationContainer().GetNrOfInOutports()
				+ comp.GetIOInformationContainer().GetNrOfInports()
				+ comp.GetIOInformationContainer().GetNrOfOutports();
		/* first check main map type */
		if (!comp.AlternateMappingEnabled(key)) {
			List = BoardInfo.GetIoComponentsOfType(comp
					.GetIOInformationContainer().GetMainMapType(), pinNeeded);
			if (!List.isEmpty()) {
				return RemoveUsedItems(List, 0);
			}
		}
		List = new ArrayList<BoardRectangle>();
		int MapId = 0;
		IOComponentTypes MapType;
		do {
			MapType = comp.GetIOInformationContainer().GetAlternateMapType(
					MapId);
			List.addAll(BoardInfo.GetIoComponentsOfType(MapType, 0));
			MapId++;
		} while (MapType != IOComponentTypes.Unknown);
		return RemoveUsedItems(List, pinNeeded);
	}

	public String GetToplevelName() {
		return toplevelName;
	}

	public boolean hasMappedComponents() {
		return !mappedList.isEmpty();
	}

	public boolean IsMappable(Map<String, ArrayList<Integer>> BoardComponents,
			FPGAReport MyReporter) {
		for (ArrayList<String> key : myMappableResources.keySet()) {
			NetlistComponent comp = myMappableResources.get(key);
			/*
			 * we have a special case: a pinbus of the toplevel, this one has
			 * never a mainmaptype, so we should skip the test
			 */
			if (!((comp.GetComponent().getFactory() instanceof Pin) && (comp
					.GetComponent().getEnd(0).getWidth().getWidth() > 1))) {
				/* for each component we first check the main map type */
				String MainMapType = comp.GetIOInformationContainer()
						.GetMainMapType().toString();
				if (BoardComponents.containsKey(MainMapType)) {
					/* okay it exists lets see if we have enough of those */
					if (BoardComponents.get(MainMapType).size() > 0) {
						if (comp.GetComponent().getFactory() instanceof PortIO
								|| comp.GetComponent().getFactory() instanceof DipSwitch) {
							/* Care of Port and Dip as their size may vary */
							int NrOfBCRequired = comp
									.GetIOInformationContainer()
									.GetNrOfInports()
									+ comp.GetIOInformationContainer()
											.GetNrOfOutports()
									+ comp.GetIOInformationContainer()
											.GetNrOfInOutports();
							int bestComponentIdx = getBestComponent(
									BoardComponents.get(MainMapType),
									NrOfBCRequired);
							if (bestComponentIdx > -1) {
								BoardComponents.get(MainMapType).remove(
										bestComponentIdx);
								continue;
							}
						} else {
							/*
							 * no Problem, we have enough of those , we allocate
							 * and decrease
							 */
							BoardComponents.get(MainMapType)
									.remove(BoardComponents.get(MainMapType)
											.size() - 1);
							continue;
						}
					}
				} else {
					/*
					 * The board does not have the main type, hence we have
					 * anyways to use alternate mapping
					 */
					comp.ToggleAlternateMapping(key);
					comp.LockAlternateMapping(key);
				}
			}
			/* Here we check if the component can be mapped to an alternate map */
			int AltMapId = 0;
			String AltMapType;
			boolean found = false;
			do {
				AltMapType = comp.GetIOInformationContainer()
						.GetAlternateMapType(AltMapId).toString();
				if (!AltMapType.equals(IOComponentTypes.Unknown.toString())) {
					if (BoardComponents.containsKey(AltMapType)) {
						int NrOfBCRequired = comp.GetIOInformationContainer()
								.GetNrOfInports()
								+ comp.GetIOInformationContainer()
										.GetNrOfOutports()
								+ comp.GetIOInformationContainer()
										.GetNrOfInOutports();
						if (NrOfBCRequired <= BoardComponents.get(AltMapType)
								.size()) {
							// BoardComponents.put(AltMapType,
							// BoardComponents.get(AltMapType) -
							// NrOfBCRequired);
							for (int i = 0; i < NrOfBCRequired; i++) {
								BoardComponents.get(AltMapType)
										.remove(BoardComponents.get(AltMapType)
												.size() - 1);
							}
							found = true;
							break;
						}
					}
				}
				AltMapId++;
			} while (!AltMapType.equals(IOComponentTypes.Unknown.toString()));
			if (!found) {
				if (comp.AlternateMappingEnabled(key)) {
					comp.UnlockAlternateMapping(key);
					comp.ToggleAlternateMapping(key);
				}
				MyReporter
						.AddError("The Target board "
								+ currentBoardName
								+ " does not have enough IO resources to map the design!");
				MyReporter.AddError("The component \""
						+ MapNametoDisplayName(GetMapNamesList(key, comp)
								.get(0)) + "\" cannot be placed!");
				return false;
			}
		}
		return true;
	}

	public void Map(String comp, BoardRectangle item, String Maptype) {
		ArrayList<String> key = GetHierarchyKey(comp);
		NetlistComponent MapComp = myMappableResources.get(key);
		if (MapComp == null) {
			logger.error("Internal error! comp: {}, key: {}", comp, key);
			return;
		}
		MapComp.addMap(DisplayNametoMapName(comp), item, Maptype);
		rebuildMappedLists();
	}

	private String MapNametoDisplayName(String item) {
		String[] parts = item.split(":");
		if (parts.length != 2) {
			logger.error("Internal error!");
			return "";
		}
		ArrayList<String> key = GetHierarchyKey(parts[1]);
		if (key != null) {
			return myMappableResources.get(key).GetIOInformationContainer()
					.GetMainMapType().toString().toUpperCase()
					+ ": " + parts[1];
		}
		return "";
	}

	public Set<String> MappedList() {
		SortedSet<String> result = new TreeSet<String>(new NaturalOrderComparator());
		for (String MapName : mappedList.keySet()) {
			result.add(MapNametoDisplayName(MapName));
		}
		return result;
	}

	public void rebuildMappedLists() {
		mappedList.clear();
		for (ArrayList<String> key : myMappableResources.keySet()) {
			if (key.get(0).equals(currentBoardName)) {
				NetlistComponent comp = myMappableResources.get(key);
				/*
				 * we can have two different situations: 1) A multipin component
				 * is mapped to a multipin resource. 2) A multipin component is
				 * mapped to multiple singlepin resources.
				 */
				/* first we handle the single pin version */
				boolean hasmap = false;
				for (String MapName : GetMapNamesList(key, comp)) {
					if (comp.getMap(MapName) != null) {
						hasmap = true;
						mappedList.put(MapName, comp.getMap(MapName));
					}
				}
				if (!hasmap) {
					comp.ToggleAlternateMapping(key);
					for (String MapName : GetMapNamesList(key, comp)) {
						if (comp.getMap(MapName) != null) {
							hasmap = true;
							mappedList.put(MapName, comp.getMap(MapName));
						}
					}
					if (!hasmap) {
						comp.ToggleAlternateMapping(key);
					}
				}
			}
		}
	}

	/**
	 * *************************************************************************
	 * **************************************
	 */
	/**
	 * Here all private handles are defined *
	 */
	/**
	 * *************************************************************************
	 * **************************************
	 */
	private ArrayList<BoardRectangle> RemoveUsedItems(
			ArrayList<BoardRectangle> List, int pinNeeded) {
		Iterator<BoardRectangle> ListIterator = List.iterator();
		while (ListIterator.hasNext()) {
			BoardRectangle current = ListIterator.next();
			if (mappedList.containsValue(current)) {
				ListIterator.remove();
			}
		}
		/* We dont want to list Pin if they are not enough */
		// if (List.size() < pinNeeded) {
		// 	List.clear();
		// }
		return List;
	}

	public boolean RequiresToplevelInversion(
			ArrayList<String> ComponentIdentifier, String MapName) {
		if (!mappedList.containsKey(MapName)) {
			return false;
		}
		if (!myMappableResources.containsKey(ComponentIdentifier)) {
			return false;
		}
		FPGAIOInformationContainer BoardComp = currentUsedBoard
				.GetComponent(mappedList.get(MapName));
		NetlistComponent Comp = myMappableResources.get(ComponentIdentifier);
		boolean BoardActiveHigh = (BoardComp.GetActivityLevel() == PinActivity.ActiveHigh);
		boolean CompActiveHigh = Comp.GetComponent().getFactory()
				.ActiveOnHigh(Comp.GetComponent().getAttributeSet());
		boolean Invert = BoardActiveHigh ^ CompActiveHigh;
		return Invert;
	}

	public void ToggleAlternateMapping(String item) {
		ArrayList<String> key = GetHierarchyKey(item);
		NetlistComponent comp = myMappableResources.get(key);
		if (comp != null) {
			if (comp.AlternateMappingEnabled(key)) {
				for (String MapName : GetMapNamesList(key, comp)) {
					if (mappedList.containsKey(MapName)) {
						return;
					}
				}
			}
			comp.ToggleAlternateMapping(key);
		}
	}

	public void TryMap(String DisplayName, BoardRectangle rect, String Maptype) {
		ArrayList<String> key = GetHierarchyKey(DisplayName);
		if (!myMappableResources.containsKey(key)) {
			return;
		}
		if (UnmappedList().contains(DisplayName)) {
			Map(DisplayName, rect, Maptype);
			return;
		}
		myMappableResources.get(key).ToggleAlternateMapping(key);
		if (UnmappedList().contains(DisplayName)) {
			Map(DisplayName, rect, Maptype);
			return;
		}
		myMappableResources.get(key).ToggleAlternateMapping(key);
	}

	public void UnMap(String comp) {
		ArrayList<String> key = GetHierarchyKey(comp);
		NetlistComponent MapComp = myMappableResources.get(key);
		if (MapComp == null) {
			logger.error("Internal error!");
			return;
		}
		MapComp.removeMap(DisplayNametoMapName(comp));
		rebuildMappedLists();
	}

	public void UnmapAll() {
		for (ArrayList<String> key : myMappableResources.keySet()) {
			if (key.get(0).equals(currentBoardName)) {
				NetlistComponent comp = myMappableResources.get(key);
				for (String MapName : GetMapNamesList(key, comp)) {
					comp.removeMap(MapName);
				}
			}
		}
	}

	public Set<String> UnmappedList() {
		SortedSet<String> result = new TreeSet<String>(new NaturalOrderComparator());

		for (ArrayList<String> key : myMappableResources.keySet()) {
			for (String MapName : GetMapNamesList(key,
					myMappableResources.get(key))) {
				if (!mappedList.containsKey(MapName)) {
					result.add(MapNametoDisplayName(MapName));
				}
			}
		}
		return result;
	}


	/*
	   The code below for NaturalOrderComparator comes from:
       https://github.com/paour/natorder/blob/master/NaturalOrderComparator.java
	   It has been altered for use in Logisim. The original file header is as follows:

	   NaturalOrderComparator.java -- Perform 'natural order' comparisons of strings in Java.
	   Copyright (C) 2003 by Pierre-Luc Paour <natorder@paour.com>

	   Based on the C version by Martin Pool, of which this is more or less a straight conversion.
	   Copyright (C) 2000 by Martin Pool <mbp@humbug.org.au>

	   This software is provided 'as-is', without any express or implied
	   warranty.  In no event will the authors be held liable for any damages
	   arising from the use of this software.

	   Permission is granted to anyone to use this software for any purpose,
	   including commercial applications, and to alter it and redistribute it
	   freely, subject to the following restrictions:

	   1. The origin of this software must not be misrepresented; you must not
	   claim that you wrote the original software. If you use this software
	   in a product, an acknowledgment in the product documentation would be
	   appreciated but is not required.
	   2. Altered source versions must be plainly marked as such, and must not be
	   misrepresented as being the original software.
	   3. This notice may not be removed or altered from any source distribution.
	*/

	private static class NaturalOrderComparator implements Comparator<String> {
		public int compare(String a, String b) {
			int na = a.length(), nb = b.length();
			int ia = 0, ib = 0;
			for (;; ia++, ib++) {
				char ca = charAt(a, ia, na);
				char cb = charAt(b, ib, nb);

				// skip spaces
				while (Character.isSpaceChar(ca))
					ca = charAt(a, ++ia, na);
				while (Character.isSpaceChar(cb))
					cb = charAt(b, ++ib, nb);

				// copmare numerical sequences
				if (Character.isDigit(ca) && Character.isDigit(cb))
				{
					int bias = 0;
					for (;; ia++, ib++)
					{
						ca = charAt(a, ia, na);
						cb = charAt(b, ib, nb);
						if (!Character.isDigit(ca) && !Character.isDigit(cb))
							break;
						else if (!Character.isDigit(ca))
							return -1; // a is less
						else if (!Character.isDigit(cb))
							return +1; // a is greater
						else if (bias == 0 && ca < cb)
							bias = -1; // a is less, if equal length
						else if (bias == 0 && ca > cb)
							bias = +1; // a is greater, if equal length
					}
					if (bias != 0)
						return bias;
				}

				// compare ascii
				if (ca < cb)
					return -1; // a is less
				else if (ca > cb)
					return +1; // a is greater
				else if (ca == 0 && cb == 0)
					return a.compareTo(b);
			}
		}

		static char charAt(String s, int i, int n) {
			return (i >= n ? 0 : s.charAt(i));
		}

		/*
		public static void main(String[] args) {
			String[] strings = new String[] {
				"1-2", "1-02", "1-20", "10-20",
					"fred", "jane",
					"pic2", "pic3", "pic4", "pic 4 else", "pic 5", "pic 5", "pic 5 something", "pic 6", "pic   7",
					"pic01", "pic02", "pic02a", "pic05",
					"pic100", "pic100a", "pic120", "pic121", "pic02000",
					"tom",
					"x2-g8", "x2-y7", "x2-y08", "x8-y8" };
			java.util.List list = java.util.Arrays.asList(strings);

			String orig = list.toString();
			System.out.println("Original: " + orig);

			Collections.shuffle(list);
			// System.out.println("Scrambled: " + list);
			Collections.sort(list, new NaturalOrderComparator());
			System.out.println("Sorted  : " + list);

			System.out.println("Correct? " + (orig.equals(list.toString())));
		}
		*/
	}

}
