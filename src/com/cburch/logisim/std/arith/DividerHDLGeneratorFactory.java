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
package com.cburch.logisim.std.arith;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.StdAttr;

public class DividerHDLGeneratorFactory extends AbstractHDLGeneratorFactory {
	final private static String NrOfBitsStr = "NrOfBits";
	final private static int NrOfBitsId = -1;
	final private static String CalcBitsStr = "CalcBits";
	final private static int CalcBitsId = -2;

	@Override
	public String getComponentStringIdentifier() {
		return "DIV";
	}

	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
		Inputs.put("INP_A", NrOfBitsId);
		Inputs.put("INP_B", NrOfBitsId);
		Inputs.put("Upper", NrOfBitsId);
		return Inputs;
	}

	@Override
	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		if (HDLType.equals(Settings.VHDL)) {
			if (attrs.getValue(Multiplier.MODE_ATTR).equals(Multiplier.UNSIGNED_OPTION)) {
				Contents.add("   s_extended_dividend(" + CalcBitsStr + "-1 DOWNTO " + NrOfBitsStr + ") <= Upper;");
				Contents.add("   s_extended_dividend(" + NrOfBitsStr + "-1 DOWNTO 0) <= INP_A;");
				Contents.add("   s_div_result <= std_logic_vector(unsigned(s_extended_dividend) / unsigned(INP_B));");
				Contents.add("   s_mod_result <= std_logic_vector(unsigned(s_extended_dividend) mod unsigned(INP_B));");
				Contents.add("   Quotient  <= s_div_result(" + NrOfBitsStr + "-1 DOWNTO 0);");
				Contents.add("   Remainder <= s_mod_result(" + NrOfBitsStr + "-1 DOWNTO 0);");
			} else {
				Contents.add("   s_extended_dividend(" + CalcBitsStr + "-1 DOWNTO " + NrOfBitsStr + ") <= Upper;");
				Contents.add("   s_extended_dividend(" + NrOfBitsStr + "-1 DOWNTO 0) <= INP_A;");
				Contents.add("   s_div_result <= std_logic_vector(signed(s_extended_dividend) / signed(INP_B));");
				Contents.add("   s_mod_result <= std_logic_vector(signed(s_extended_dividend) mod signed(INP_B));");
				Contents.add("   Quotient  <= s_div_result(" + NrOfBitsStr + "-1 DOWNTO 0);");
				Contents.add("   Remainder <= s_mod_result(" + NrOfBitsStr + "-1 DOWNTO 0);");
			}
		}
		return Contents;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
		Outputs.put("Quotient", NrOfBitsId);
		Outputs.put("Remainder", NrOfBitsId);
		return Outputs;
	}

	@Override
	public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
		SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
		Parameters.put(NrOfBitsId, NrOfBitsStr);
		Parameters.put(CalcBitsId, CalcBitsStr);
		return Parameters;
	}

	@Override
	public SortedMap<String, Integer> GetParameterMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter) {
		SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
		int NrOfBits = ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
                // TODO(kwalsh) - null the upper if not connected, or add a parameter
		int CalcBits = 2 * NrOfBits;
		ParameterMap.put(NrOfBitsStr, NrOfBits);
		ParameterMap.put(CalcBitsStr, CalcBits);
		return ParameterMap;
	}

	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		SortedMap<String, String> PortMap = new TreeMap<String, String>();
		PortMap.putAll(GetNetMap("INP_A", true, ComponentInfo, Divider.IN0,
				Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("INP_B", true, ComponentInfo, Divider.IN1,
				Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Upper", true, ComponentInfo, Divider.UPPER,
				Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Quotient", true, ComponentInfo,
				Divider.OUT, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Remainder", true, ComponentInfo,
				Divider.REM, Reporter, HDLType, Nets));
		return PortMap;
	}

	@Override
	public String GetSubDir() {
		return "arithmetic";
	}

	@Override
	public SortedMap<String, Integer> GetWireList(AttributeSet attrs,
			Netlist Nets) {
		SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
		Wires.put("s_div_result", CalcBitsId);
		Wires.put("s_mod_result", NrOfBitsId);
		Wires.put("s_extended_dividend", CalcBitsId);
		return Wires;
	}

	@Override
	public boolean HDLTargetSupported(String HDLType, AttributeSet attrs,
			char Vendor) {
		return HDLType.equals(Settings.VHDL);
	}

}
