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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.Icon;

import com.cburch.logisim.proj.Project;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.toolbar.AbstractToolbarModel;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.draw.toolbar.ToolbarSeparator;
import com.cburch.draw.tools.AbstractTool;
import com.cburch.draw.tools.CurveTool;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.draw.tools.LineTool;
import com.cburch.draw.tools.OvalTool;
import com.cburch.draw.tools.PolyTool;
import com.cburch.draw.tools.RectangleTool;
import com.cburch.draw.tools.RoundRectangleTool;
import com.cburch.draw.tools.TextTool;
import com.cburch.draw.tools.ToolbarToolItem;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;
import com.cburch.hdl.HdlModelListener;
import com.cburch.hdl.HdlModel;


class HdlToolbarModel extends AbstractToolbarModel implements HdlModelListener {
    private Project proj;
    private HdlContentView editor;
    private List<ToolbarItem> items;

    HdlToolbarItem hdlImport, hdlExport, hdlValidate;

    public static final String HDL_IMPORT = "hdlImport";
    public static final String HDL_EXPORT = "hdlExport";
    public static final String HDL_VALIDATE = "hdlValidate";

    public HdlToolbarModel(Project proj, HdlContentView editor) {
        this.proj = proj;
        this.editor = editor;

        ArrayList<ToolbarItem> rawItems = new ArrayList<ToolbarItem>();
        hdlImport = new HdlToolbarItem("hdlimport.gif", HDL_IMPORT, Strings.getter("hdlOpenButton"));
        hdlExport = new HdlToolbarItem("hdlexport.gif", HDL_EXPORT, Strings.getter("hdlSaveButton"));
        hdlValidate = new HdlToolbarItem("hdlvalidate.gif", HDL_VALIDATE, Strings.getter("validateButton"));
        rawItems.add(hdlImport);
        rawItems.add(hdlExport);
        rawItems.add(hdlValidate);
        items = Collections.unmodifiableList(rawItems);
    }

    @Override
    public List<ToolbarItem> getItems() {
        return items;
    }

    @Override
    public boolean isSelected(ToolbarItem item) {
        return false;
    }

    @Override
    public void itemSelected(ToolbarItem item) {
        doAction(((HdlToolbarItem)item).action);
    }

    boolean validateEnabled = false;

    void doAction(String action) {
        if (action == HDL_IMPORT)
            editor.doImport();
        else if (action == HDL_EXPORT)
            editor.doExport();
        else if (action == HDL_VALIDATE)
            editor.doValidate();
    }

    boolean isEnabled(String action) {
        if (action == HDL_VALIDATE)
            return validateEnabled;
        else
            return true;
    }

    void setDirty(boolean dirty) {
        if (validateEnabled == dirty)
            return;
        validateEnabled = dirty;
        fireToolbarContentsChanged();
    }

    @Override
    public void contentSet(HdlModel source) {
        if (validateEnabled) {
            validateEnabled = false;
            fireToolbarContentsChanged();
        }
    }

    @Override
    public void aboutToSave(HdlModel source) { }

    @Override
    public void displayChanged(HdlModel source) { }

    @Override
    public void appearanceChanged(HdlModel source) { }

    private class HdlToolbarItem implements ToolbarItem {
        Icon icon;
        String action;
        StringGetter toolTip;

        public HdlToolbarItem(String iconName, String action, StringGetter toolTip) {
            this.icon = Icons.getIcon(iconName);
            this.action = action;
            this.toolTip = toolTip;
        }

        public void doAction() {
            if (isEnabled(action))
                HdlToolbarModel.this.doAction(action);
        }

        public Dimension getDimension(Object orientation) {
            if (icon == null)
                return new Dimension(16, 16);
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            return new Dimension(w, h + 2);
        }

        public String getToolTip() {
            return (toolTip == null ? null : toolTip.toString());
        }

        public boolean isSelectable() {
            return isEnabled(action);
        }

        public void paintIcon(Component destination, Graphics g) {
            if (!isSelectable() && g instanceof Graphics2D) {
                Composite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                        0.3f);
                ((Graphics2D) g).setComposite(c);
            }

            if (icon == null) {
                g.setColor(new Color(255, 128, 128));
                g.fillRect(4, 4, 8, 8);
                g.setColor(Color.BLACK);
                g.drawLine(4, 4, 12, 12);
                g.drawLine(4, 12, 12, 4);
                g.drawRect(4, 4, 8, 8);
            } else {
                icon.paintIcon(destination, g, 0, 1);
            }
        }
    }

}
