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

import java.awt.Dialog;
import java.awt.Font;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.proj.Project;
import com.cburch.hdl.HdlModel;
import com.cburch.hdl.HdlModelListener;

public class VhdlEntityAttributes extends AbstractAttributeSet {

	public static class VhdlGenericAttribute extends Attribute<Integer> {
		int start, end;
		VhdlContent.Generic g;

		private VhdlGenericAttribute(String name, StringGetter disp, int start, int end, VhdlContent.Generic g) {
			super(name, disp);
			this.start = start;
			this.end = end;
			this.g = g;
		}

		public VhdlContent.Generic getGeneric() {
			return g;
		}

		@Override
		public java.awt.Component getCellEditor(Integer value) {
			return super.getCellEditor(value != null ? value : g.getDefaultValue());
		}

		@Override
		public Integer parse(String value) {
			if (value == null)
				return null;
			value = value.trim();
			if (value.length() == 0 || value.equals("default") || value.equals("(default)") || value.equals(toDisplayString(null)))
				return null;
			long v = (long) Long.parseLong(value);
			if (v < start)
				throw new NumberFormatException("integer too small");
			if (v > end)
				throw new NumberFormatException("integer too large");
			return Integer.valueOf((int)v);
		}

		@Override
		public String toDisplayString(Integer value) {
			return value == null ? "(default) " + g.getDefaultValue() : value.toString();
		}
	}

	public static Attribute<Integer> forGeneric(VhdlContent.Generic g) {
		String name = g.getName();
		StringGetter disp = StringUtil.constantGetter(name);
		if (g.getType().equals("positive"))
			return new VhdlGenericAttribute("vhdl_" + name, disp, 1, Integer.MAX_VALUE, g);
		else if (g.getType().equals("natural"))
			return new VhdlGenericAttribute("vhdl_" + name, disp, 0, Integer.MAX_VALUE, g);
		else
			return new VhdlGenericAttribute("vhdl_" + name, disp, Integer.MIN_VALUE, Integer.MAX_VALUE, g);
	}

	private static List<Attribute<?>> static_attributes = Arrays.asList(
			(Attribute<?>)VhdlEntity.NAME_ATTR, StdAttr.LABEL, StdAttr.LABEL_FONT, StdAttr.APPEARANCE);

	static AttributeSet createBaseAttrs(VhdlContent content) {
		VhdlContent.Generic[] g = content.getGenerics();
		List<Attribute<Integer>> a = content.getGenericAttributes();
		Attribute<?>[] attrs = new Attribute<?>[5 + g.length];
		Object[] value = new Object[5 + g.length];
		attrs[0] = VhdlEntity.NAME_ATTR;
		value[0] = content.getName();
		attrs[1] = StdAttr.LABEL;
		value[1] = "";
		attrs[2] = StdAttr.LABEL_FONT;
		value[2] = StdAttr.DEFAULT_LABEL_FONT;
		attrs[3] = StdAttr.FACING;
		value[3] = Direction.EAST;
		attrs[4] = StdAttr.APPEARANCE;
		value[4] = StdAttr.APPEAR_FPGA;
		for (int i = 0; i < g.length; i++) {
			attrs[5+i] = a.get(i);
			value[5+i] = new Integer(g[i].getDefaultValue());
		}
		AttributeSet ret = AttributeSets.fixedSet(attrs, value);
		// ret.addAttributeListener(new StaticListener(content));
		return ret;
	}

	private class MyListener implements AttributeListener {
		public void attributeListChanged(AttributeEvent e) { }
		public void attributeValueChanged(AttributeEvent e) {
			if (e.getAttribute() == VhdlEntity.NAME_ATTR)
				setValue(VhdlEntity.NAME_ATTR, (String)e.getValue());
			else if (e.getAttribute() == StdAttr.APPEARANCE)
				setValue(StdAttr.APPEARANCE, (AttributeOption)e.getValue());
		}
	}

	/*
	   private static class StaticListener implements AttributeListener {
	   private VhdlContent content;
	   private StaticListener(VhdlContent vhdl) { content = vhdl; }
	   public void attributeListChanged(AttributeEvent e) { }
	   public void attributeValueChanged(AttributeEvent e) {
	   if (e.getAttribute() == VhdlEntity.NAME_ATTR) {
	   String newValue = (String)e.getValue();
	   if (!content.getName().equals(newValue)
	   && !content.setName(newValue))
	   e.getSource().setValue(VhdlEntity.NAME_ATTR, content.getName());
	   }
	   }
	   } */

	private VhdlContent content;
	private Instance vhdlInstance;
	private String label = "";
	private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
	private Direction facing = Direction.EAST;
	private HashMap<Attribute<Integer>, Integer> genericValues;
	private List<Attribute<?>> instanceAttrs;
	private VhdlEntityListener listener; // strong ref

	VhdlEntityAttributes(VhdlContent content) {
		this.content = content;
		genericValues = null;
		vhdlInstance = null;
		listener = null;
		updateGenerics();
	}

	public VhdlContent getContent() {
		return content;
	}

	public Direction getFacing() {
		return facing;
	}

	void setInstance(Instance value) {
		vhdlInstance = value;
		if (vhdlInstance != null && listener != null) {
			listener = new VhdlEntityListener(this);
			content.addHdlModelListener(listener);
		}
	}

	void updateGenerics() {
		List<Attribute<Integer>> genericAttrs = content.getGenericAttributes();
		instanceAttrs = new ArrayList<Attribute<?>>(5 + genericAttrs.size());
		instanceAttrs.add(VhdlEntity.NAME_ATTR);
		instanceAttrs.add(StdAttr.LABEL);
		instanceAttrs.add(StdAttr.LABEL_FONT);
		instanceAttrs.add(StdAttr.FACING);
		instanceAttrs.add(StdAttr.APPEARANCE);
		for (Attribute<Integer> a : genericAttrs) {
			instanceAttrs.add(a);
		}
		if (genericValues == null)
			genericValues = new HashMap<Attribute<Integer>, Integer>();
		ArrayList<Attribute<Integer>> toRemove = new ArrayList<Attribute<Integer>>();
		for (Attribute<Integer> a : genericValues.keySet()) {
			if (!genericAttrs.contains(a))
				toRemove.add(a);
		}
		for (Attribute<Integer> a : toRemove) {
			genericValues.remove(a);
		}
		fireAttributeListChanged();
	}

	@Override
	protected void copyInto(AbstractAttributeSet dest) {
		VhdlEntityAttributes attr = (VhdlEntityAttributes) dest;
		attr.content = content; // .clone();
		// attr.label = unchanged;
		attr.labelFont = labelFont;
		attr.facing = facing;
		attr.instanceAttrs = instanceAttrs;
		attr.genericValues = new HashMap<Attribute<Integer>, Integer>();
		for (Attribute<Integer> a : genericValues.keySet())
			attr.genericValues.put(a, genericValues.get(a));
		attr.listener = null;
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return instanceAttrs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> V getValue(Attribute<V> attr) {
		if (attr == VhdlEntity.NAME_ATTR) {
			return (V) content.getName();
		}
		if (attr == StdAttr.LABEL) {
			return (V) label;
		}
		if (attr == StdAttr.LABEL_FONT) {
			return (V) labelFont;
		}
		if (attr == StdAttr.APPEARANCE) {
			return (V) content.getAppearance();
		}
		if (attr == StdAttr.FACING) {
			return (V) facing;
		}
		if (genericValues.containsKey((Attribute<Integer>)attr)) {
			V v = (V) genericValues.get((Attribute<Integer>)attr);
			return v;
		}
		// V v = content.getStaticAttributes().getValue(attr);
		// return v;
		return null;
	}

	@Override
	public boolean isToSave(Attribute<?> attr) {
		return true;
	}

	@Override
	public <V> void setValue(Attribute<V> attr, V value) {
		if (attr == VhdlEntity.NAME_ATTR) {
			String newValue = (String)value;
			if (content.getName().equals(newValue))
				return;
			if (!content.setName(newValue))
				return;
			fireAttributeValueChanged(attr, value);
			return;
		}
		if (attr == StdAttr.LABEL && value instanceof String) {
			String newLabel = (String) value;
			if (label.equals(newLabel))
				return;
			label = newLabel;
			fireAttributeValueChanged(attr, value);
			return;
		}
		if (attr == StdAttr.LABEL_FONT && value instanceof Font) {
			Font newFont = (Font) value;
			if (labelFont.equals(newFont))
				return;
			labelFont = newFont;
			fireAttributeValueChanged(attr, value);
			return;
		}
		if (attr == StdAttr.FACING) {
			Direction val = (Direction) value;
			if (facing.equals(val))
				return;
			facing = val;
			fireAttributeValueChanged(attr, value);
			return;
		}
		if (attr == StdAttr.APPEARANCE &&
				(value == StdAttr.APPEAR_FPGA || value == StdAttr.APPEAR_CLASSIC)) {
			AttributeOption a = (AttributeOption) value;
			if (content.getAppearance().equals(a))
				return;
			content.setAppearance(a);
			fireAttributeValueChanged(attr, value);
			return;
				}
		if (genericValues != null) {
			genericValues.put((Attribute<Integer>)attr, (Integer)value);
			fireAttributeValueChanged(attr, value);
		}
	}

	static class VhdlEntityListener implements HdlModelListener {
		VhdlEntityAttributes attrs;
		VhdlEntityListener(VhdlEntityAttributes attrs) {
			this.attrs = attrs;
		}
		@Override
		public void contentSet(HdlModel source) {
			attrs.updateGenerics();
			attrs.vhdlInstance.fireInvalidated();
			attrs.vhdlInstance.recomputeBounds();
			attrs.fireAttributeValueChanged(VhdlEntity.NAME_ATTR, ((VhdlContent)source).getName());
		}
		@Override
		public void aboutToSave(HdlModel source) { }
		@Override
		public void displayChanged(HdlModel source) { }
		@Override
		public void appearanceChanged(HdlModel source) {
			attrs.vhdlInstance.recomputeBounds();
			attrs.fireAttributeValueChanged(StdAttr.APPEARANCE, ((VhdlContent)source).getAppearance());
		}
	}

}
