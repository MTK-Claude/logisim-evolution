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

package com.cburch.logisim.std.hdl;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Window;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cburch.hdl.HdlModel;
import com.cburch.hdl.HdlModelListener;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.StringGetter;

import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.circuit.appear.DefaultEvolutionAppearance;
import com.cburch.logisim.circuit.appear.DefaultClassicAppearance;
import com.cburch.logisim.circuit.appear.CircuitAppearance;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.comp.EndData;
import com.cburch.draw.model.CanvasObject;

public class VhdlEntity extends InstanceFactory implements HdlModelListener {

	final static Logger logger = LoggerFactory.getLogger(VhdlEntity.class);

	static final Attribute<String> NAME_ATTR = Attributes.forString(
			"vhdlEntity", Strings.getter("vhdlEntityName"));

	static final int WIDTH = 140;
	static final int HEIGHT = 40;
	static final int PORT_GAP = 10;

	static final int X_PADDING = 5;

        private VhdlContent content;

	public VhdlEntity(VhdlContent content) {
		super("", null);
                this.content = content;
                this.content.addHdlModelListener(this);
                if (content.isValid()) 
                    this.setIconName("vhdl.gif");
                else
                    this.setIconName("vhdl-invalid.gif");
		setFacingAttribute(StdAttr.FACING);
	}

	@Override
	public String getName() {
            if (content == null)
                return "VHDL Entity";
            else
		return content.getName();
	}

	@Override
	public StringGetter getDisplayGetter() {
            if (content == null)
                    return Strings.getter("vhdlComponent");
            else
		return StringUtil.constantGetter(content.getName());
	}
        

	public VhdlContent getContent() {
		return content;
	}

	@Override
	protected void configureNewInstance(Instance instance) {
                VhdlEntityAttributes attrs = (VhdlEntityAttributes)instance.getAttributeSet();
                attrs.setInstance(instance);
                instance.addAttributeListener();

		updatePorts(instance);
	}

	@Override
	public AttributeSet createAttributeSet() {
                return new VhdlEntityAttributes(content);
	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		return content.getName().toLowerCase();
	}

	@Override
	public String getHDLTopName(AttributeSet attrs) {

		String label = "";

		if (attrs.getValue(StdAttr.LABEL) != null && attrs.getValue(StdAttr.LABEL).length() != 0)
			label = "_" + attrs.getValue(StdAttr.LABEL).toLowerCase();

		return getHDLName(attrs) + label;
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		// int nbInputs = content.getInputsNumber();
		// int nbOutputs = content.getOutputsNumber();

		// return Bounds.create(0, 0, WIDTH, Math.max(nbInputs, nbOutputs)
		//		* PORT_GAP + HEIGHT);
                if (appearance == null)
                    return Bounds.create(0, 0, 100, 100);
		Direction facing = attrs.getValue(StdAttr.FACING);
                return appearance.getOffsetBounds().rotate(Direction.EAST, facing, 0, 0);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs, char Vendor) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new VhdlHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs, Vendor);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.FACING) {
			updatePorts(instance);
		} else if (attr == StdAttr.APPEARANCE) {
			updatePorts(instance);
		}
	}

	private static class VhdlAppearance extends CircuitAppearance {
		String style;
		VhdlAppearance(List <CanvasObject> shapes) {
			super(null);
			setObjectsForce(shapes);
		}
		static VhdlAppearance create(List<Instance> pins, String name, AttributeOption style) {
			if (style == StdAttr.APPEAR_CLASSIC) {
				VhdlAppearance a = new VhdlAppearance(DefaultClassicAppearance.build(pins));
				a.style = "classic";
				return a;
			} else {
				VhdlAppearance a = new VhdlAppearance(DefaultEvolutionAppearance.build(pins, name));
				a.style = "evolution";
				return a;
			}
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		VhdlEntityAttributes attrs = (VhdlEntityAttributes) painter.getAttributeSet();
		Direction facing = attrs.getFacing();
		Graphics g = painter.getGraphics();

		Location loc = painter.getLocation();
		g.translate(loc.getX(), loc.getY());
		appearance.paintSubcircuit(painter, g, facing);
		g.translate(-loc.getX(), -loc.getY());

		String label = painter.getAttributeValue(StdAttr.LABEL);
		if (label != null) {
			Bounds bds = painter.getBounds();
			Font oldFont = g.getFont();
			g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
			GraphicsUtil.drawCenteredText(g, label, bds.getX() + bds.getWidth() / 2, bds.getY() - g.getFont().getSize());
			g.setFont(oldFont);
		}

		painter.drawPorts();
	}

	@Override
	/**
	 * Propagate signals through the VHDL component.
	 * Logisim doesn't have a VHDL simulation tool. So we need to use an external tool.
	 * We send signals to Questasim/Modelsim through a socket and a tcl binder. Then,
	 * a simulation step is done and the tcl server sends the output signals back to
	 * Logisim. Then we can set the VHDL component output properly.
	 *
	 * This can be done only if Logisim could connect to the tcl server (socket). This is
	 * done in Simulation.java.
	 */
	public void propagate(InstanceState state) {

		if (state.getProject().getVhdlSimulator().isEnabled()
				&& state.getProject().getVhdlSimulator().isRunning()) {

			VhdlSimulator vhdlSimulator = state.getProject().getVhdlSimulator();

			for (Port p : state.getInstance().getPorts()) {
				int index = state.getPortIndex(p);
				Value val = state.getPortValue(index);

				String vhdlEntityName = getHDLTopName(state.getAttributeSet());

				String message = p.getType() + ":" + vhdlEntityName + "_"
						+ p.getToolTip() + ":" + val.toBinaryString() + ":"
						+ index;

				vhdlSimulator.send(message);
			}

			vhdlSimulator.send("sync");

			/* Get response from tcl server */
			String server_response;
			while ((server_response = vhdlSimulator.receive()) != null
					&& server_response.length() > 0
					&& !server_response.equals("sync")) {

				String[] parameters = server_response.split("\\:");

				String busValue = parameters[1];

				Value vector_values[] = new Value[busValue.length()];

				int k = busValue.length() - 1;
				for (char bit : busValue.toCharArray()) {

					try {
						switch (Character.getNumericValue(bit)) {
						case 0:
							vector_values[k] = Value.FALSE;
							break;
						case 1:
							vector_values[k] = Value.TRUE;
							break;
						default:
							vector_values[k] = Value.UNKNOWN;
							break;
						}
					} catch (NumberFormatException e) {
						vector_values[k] = Value.UNKNOWN;
					}
					k--;
				}

				state.setPort(Integer.parseInt(parameters[2]),
						Value.create(vector_values), 1);
			}

			/* VhdlSimulation stopped/disabled */
		} else {

			for (Port p : state.getInstance().getPorts()) {
				int index = state.getPortIndex(p);

				/* If it is an output */
				if (p.getType() == 2) {
					Value vector_values[] = new Value[p.getFixedBitWidth()
							.getWidth()];
					for (int k = 0; k < p.getFixedBitWidth().getWidth(); k++) {
						vector_values[k] = Value.UNKNOWN;
					}

					state.setPort(index, Value.create(vector_values), 1);
				}
			}

			new UnsupportedOperationException(
					"VHDL component simulation is not supported. This could be because there is no Questasim/Modelsim simulation server running.");
		}
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}

	/**
	 * Save the VHDL entity in a file. The file is used for VHDL components
	 * simulation by QUestasim/Modelsim
	 */
	public void saveFile(AttributeSet attrs) {

		PrintWriter writer;
		try {
			writer = new PrintWriter(VhdlSimulator.SIM_SRC_PATH
					+ getHDLTopName(attrs) + ".vhdl", "UTF-8");

			String content = this.content.getContent();

			content = content.replaceAll("(?i)" + getHDLName(attrs),
					getHDLTopName(attrs));

			writer.print(content);
			writer.close();
		} catch (FileNotFoundException e) {
			logger.error("Could not create vhdl file: {}", e.getMessage());
			e.printStackTrace();
			return;
		} catch (UnsupportedEncodingException e) {
			logger.error("Could not create vhdl file: {}", e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	private VhdlAppearance appearance;

	void updatePorts(Instance instance) {
		ArrayList<Instance> pins = new ArrayList<Instance>();
		int y = 0;
		for (VhdlParser.PortDescription p: content.getPorts()) {
			AttributeSet a = Pin.FACTORY.createAttributeSet();
			a.setValue(StdAttr.LABEL, p.getName());
			a.setValue(Pin.ATTR_TYPE, p.getType() != Port.INPUT);
			a.setValue(StdAttr.FACING, p.getType() != Port.INPUT ? Direction.WEST : Direction.EAST);
			a.setValue(StdAttr.WIDTH, p.getWidth());
			InstanceComponent ic = (InstanceComponent)Pin.FACTORY.createComponent(Location.create(100, y), a);
			pins.add(ic.getInstance());
			y += 10;
		}
		AttributeOption style = instance.getAttributeValue(StdAttr.APPEARANCE);
		appearance = VhdlAppearance.create(pins, getName(), style);

		Direction facing = instance.getAttributeValue(StdAttr.FACING);
		Map<Location, Instance> portLocs = appearance.getPortOffsets(facing);

		Port[] ports = new Port[portLocs.size()];
		int i = -1;
		for (Map.Entry<Location, Instance> portLoc : portLocs.entrySet()) {
			i++;
			Location loc = portLoc.getKey();
			Instance pin = portLoc.getValue();
			String type = Pin.FACTORY.isInputPin(pin) ? Port.INPUT
				: Port.OUTPUT;
			BitWidth width = pin.getAttributeValue(StdAttr.WIDTH);
			ports[i] = new Port(loc.getX(), loc.getY(), type, width);

			String label = pin.getAttributeValue(StdAttr.LABEL);
			if (label != null && label.length() > 0) {
				ports[i].setToolTip(StringUtil.constantGetter(label));
			}
		}
		instance.setPorts(ports);
		instance.recomputeBounds();
	}

	@Override
	public void contentSet(HdlModel source) {
		if (content.isValid()) 
			this.setIconName("vhdl.gif");
		else
			this.setIconName("vhdl-invalid.gif");
	}

	@Override
	public void aboutToSave(HdlModel source) { }

	@Override
	public void displayChanged(HdlModel source) { }

	@Override
	public void appearanceChanged(HdlModel source) { }

	private WeakHashMap<Component, Circuit> circuitsUsingThis = new WeakHashMap<>();
	public Collection<Circuit> getCircuitsUsingThis() {
		return circuitsUsingThis.values();
	}
	public void addCircuitUsing(Component comp, Circuit circ) {
		circuitsUsingThis.put(comp, circ);
	}
	public void removeCircuitUsing(Component comp) {
		circuitsUsingThis.remove(comp);
	}

}
