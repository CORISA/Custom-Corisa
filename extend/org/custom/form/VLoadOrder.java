/******************************************************************************
 * Copyright (C) 2009 Low Heng Sin                                            *
 * Copyright (C) 2009 Idalica Corporation                                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.custom.form;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.VetoableChangeListener;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.compiere.apps.ADialog;
import org.compiere.apps.AppsAction;
import org.compiere.apps.ConfirmPanel;
import org.compiere.apps.StatusBar;
import org.compiere.apps.form.FormFrame;
import org.compiere.apps.form.FormPanel;
import org.compiere.common.constants.DisplayTypeConstants;
import org.compiere.grid.ed.VDate;
import org.compiere.grid.ed.VLookup;
import org.compiere.grid.ed.VNumber;
import org.compiere.minigrid.MiniTable;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MUOMConversion;
import org.compiere.plaf.CompiereColor;
import org.compiere.plaf.CompierePLAF;
import org.compiere.swing.CButton;
import org.compiere.swing.CComboBox;
import org.compiere.swing.CLabel;
import org.compiere.swing.CPanel;
import org.compiere.swing.CToggleButton;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.custom.model.MCUSTMotoristVehicle;
import org.custom.plaf.AdempiereTaskPaneUI;
import org.jdesktop.swingx.JXTaskPane;

public class VLoadOrder extends LoadOrder
	implements FormPanel, ActionListener, TableModelListener, VetoableChangeListener
{
	private CPanel 	panel = new CPanel();
	private Trx		trx = null;
	private String	trxName = new String("GLO");

	/**
	 *	Initialize Panel
	 *  @param WindowNo window
	 *  @param frame frame
	 */
	public void init (int WindowNo, FormFrame frame)
	{
		m_WindowNo = WindowNo;
		m_frame = frame;
		Env.getCtx().setContext(m_WindowNo, "IsSOTrx", "Y");   //  defaults to yes
		//	Transaction
		trx = Trx.get(trxName);
		
		try	{
			dynInit();
			jbInit();
			frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
			frame.getContentPane().add(statusBar, BorderLayout.SOUTH);
		}
		catch(Exception e)
		{
			log.log(Level.SEVERE, "", e);
		}
	}	//	init

	MLookup lookupSPP;
	
	/**	Window No			*/
	//private int         	m_WindowNo = 0;
	
	/**	FormFrame			*/
	private FormFrame 		m_frame;

	private CPanel 			mainPanel = new CPanel();
	private BorderLayout 	mainLayout = new BorderLayout();
	private CPanel 			parameterPanel = new CPanel();
	private CPanel 			loadOrderPanel = new CPanel();
	private GridBagLayout 	parameterLayout = new GridBagLayout();
	/**/
	private JLabel 			driverLabel = new JLabel();
	private CComboBox 		driverSearch = new CComboBox();
	private JLabel 			carLabel = new JLabel();
	private CComboBox 		vehicleSearch = new CComboBox();
	private JLabel 			salesRegionLabel = new JLabel();
	private VLookup 		salesRegion = null;
	private JLabel 			salesRepLabel = new JLabel();
	private VLookup 		salesRepSearch = null;
	private JLabel 			capacityLabel = new JLabel();
	private VNumber 		capacityField = null;
	private JLabel 			shipperLabel = new JLabel();
	private VLookup 		shipperPick = null;
	private JLabel 			uomVehicleLabel = new JLabel();
	private VLookup 		uomVehiclePick = null;
	private JLabel 			uomWorkLabel = new JLabel();
	private VLookup 		uomWorkPick = null;
	private JLabel 			docTypeOrderLabel = new JLabel();
	private CComboBox 		docTypeOrderSearch = new CComboBox();
	private JLabel 			warehouseLabel = new JLabel();
	private CComboBox 		warehouseSearch = new CComboBox();
	private JLabel 			locatorLabel = new JLabel();
	private CComboBox 		locatorSearch = new CComboBox();
	private JLabel 			locatorToLabel = new JLabel();
	private CComboBox 		locatorToSearch = new CComboBox();
	
	
	/**/
	private MiniTable 		orderLineTable = new MiniTable();
	private MiniTable 		orderTable = new MiniTable();
	private JSplitPane 		infoPanel = new JSplitPane();
	private CPanel 			orderPanel = new CPanel();
	private CPanel 			orderLinePanel = new CPanel();
	private JLabel 			orderLabel = new JLabel();
	private JLabel 			orderLineLabel = new JLabel();
	private BorderLayout 	orderLayout = new BorderLayout();
	private BorderLayout 	orderLineLayout = new BorderLayout();
	private JLabel 			orderInfo = new JLabel();
	private JLabel 			orderLineInfo = new JLabel();
	private JScrollPane 	orderScrollPane = new JScrollPane();
	private JScrollPane 	orderLineScrollPane = new JScrollPane();
	private GridBagLayout 	loadOrderLayout = new GridBagLayout();
	private JLabel 			differenceLabel = new JLabel();
	private VNumber 		differenceField = null;
	private JButton 		gLoadOrderButton = new JButton();

	private CPanel 			stockInfoPanel = new CPanel();
	private BorderLayout 	orderLineStockInfoLayout = new BorderLayout();
	private StatusBar 		statusBar = new StatusBar();

	private JLabel 			organizationLabel = new JLabel();
	private VLookup 		organizationPick = null;
	protected AppsAction selectAllAction =
			new AppsAction (SELECT_DESELECT_ALL, null, Msg.getMsg(Env.getCtx(), SELECT_DESELECT_ALL), true);
		
	private static final String	SELECT_DESELECT_ALL = "SelectDeselectAll";
	
	private CButton 		bSearch = new CButton();
	//	Date Document
	private CLabel 			labelDateDoc = new CLabel();
	private VDate 			fieldDateDoc = new VDate();
	
	//	Date Shippment
	private CLabel 			labelShippDate = new CLabel();
	private VDate 			fieldShipDate = new VDate();
	
	private String 			uomWorkValue = null;
	//	Stock Info
	private JScrollPane 	stockScrollPane = new JScrollPane();
	
	/**	Collapsible Panel for Parameter		*/
	private JXTaskPane parameterCollapsiblePanel = new JXTaskPane();

	/**	Collapsible Panel for Stock			*/
	private JXTaskPane stockCollapsiblePanel = new JXTaskPane();
	
	/**
	 *  Static Init
	 *  @throws Exception
	 */
	private void jbInit() throws Exception
	{
		
		CompiereColor.setBackground(panel);
		//
		mainPanel.setLayout(mainLayout);

		//
		parameterPanel.setLayout(parameterLayout);
		loadOrderPanel.setLayout(loadOrderLayout);
		driverLabel.setText(Msg.translate(Env.getCtx(), "Motorist_ID"));
		
		shipperLabel.setText(Msg.translate(Env.getCtx(), "M_Shipper_ID"));
		carLabel.setText(Msg.translate(Env.getCtx(), "Vehicle_ID"));
		salesRegionLabel.setText(Msg.translate(Env.getCtx(), "C_SalesRegion_ID"));
		salesRepLabel.setText(Msg.translate(Env.getCtx(), "SalesRep_ID"));
		capacityLabel.setText(Msg.translate(Env.getCtx(), "Capacity"));
		//	Unit Measure
		uomVehicleLabel.setText(Msg.translate(Env.getCtx(), "Vehicle_UOM_ID"));
		uomWorkLabel.setText(Msg.translate(Env.getCtx(), "Work_UOM_ID"));
		
		bSearch.setText(Msg.translate(Env.getCtx(), "Search"));
		
		//	Document Type Order
		docTypeOrderLabel.setText(Msg.translate(Env.getCtx(), "C_DocTypeOrder_ID"));
		
		//	Warehouse
		warehouseLabel.setText(Msg.translate(Env.getCtx(), "M_Warehouse_ID"));
		
		//	Locator
		locatorLabel.setText(Msg.translate(Env.getCtx(), "M_Locator_ID"));
		
		//	Locator To
		locatorToLabel.setText(Msg.translate(Env.getCtx(), "M_LocatorTo_ID"));
		
		//	Date
		labelDateDoc.setText(Msg.translate(Env.getCtx(), "DateDoc"));
		labelShippDate.setText(Msg.translate(Env.getCtx(), "DateOut"));
		
		
		orderLabel.setRequestFocusEnabled(false);
		orderLabel.setText(" " + Msg.translate(Env.getCtx(), "C_Order_ID"));
		orderLineLabel.setRequestFocusEnabled(false);
		orderLineLabel.setText(" " + Msg.translate(Env.getCtx(), "C_OrderLine_ID"));
		orderPanel.setLayout(orderLayout);
		orderLinePanel.setLayout(orderLineLayout);
		orderLineInfo.setHorizontalAlignment(SwingConstants.RIGHT);
		orderLineInfo.setHorizontalTextPosition(SwingConstants.RIGHT);
		orderInfo.setHorizontalAlignment(SwingConstants.RIGHT);
		orderInfo.setHorizontalTextPosition(SwingConstants.RIGHT);
		gLoadOrderButton.setText(Msg.getMsg(Env.getCtx(), "SGGenerateLoadOrder"));
		gLoadOrderButton.addActionListener(this);
		differenceLabel.setText(Msg.getMsg(Env.getCtx(), "SGDiffWeight"));
		differenceField = new VNumber("Difference", true, true, true, DisplayTypeConstants.Number, "Difference");
		differenceField.setValue(Env.ZERO);
		capacityField = new VNumber("Capacity", true, true, true, DisplayTypeConstants.Number, "Capacity");
		capacityField.setValue(Env.ZERO);
		//capacityField.addActionListener(this);
		
		orderLineScrollPane.setPreferredSize(new Dimension(200, 200));
		orderScrollPane.setPreferredSize(new Dimension(200, 200));
		stockScrollPane.setPreferredSize(new Dimension(200, 200));
		
		parameterCollapsiblePanel.add(parameterPanel);
		parameterCollapsiblePanel.setTitle(Msg.translate(Env.getCtx(), "SGParameters"));
		parameterCollapsiblePanel.setUI(new AdempiereTaskPaneUI());
		parameterCollapsiblePanel.getContentPane().setBackground(CompierePLAF.getFormBackground());
		parameterCollapsiblePanel.getContentPane().setForeground(CompierePLAF.getTextColor_Label());
		parameterCollapsiblePanel.setCollapsed(false);
		
		stockCollapsiblePanel.setCollapsed(true);
		stockCollapsiblePanel.setTitle(Msg.translate(Env.getCtx(), "SGWarehouseStockGroup"));
		stockCollapsiblePanel.setUI(new AdempiereTaskPaneUI());
		stockCollapsiblePanel.getContentPane().setBackground(CompierePLAF.getFormBackground());
		stockCollapsiblePanel.getContentPane().setForeground(CompierePLAF.getTextColor_Label());
		
		stockCollapsiblePanel.addVetoableChangeListener(this);
		
		mainPanel.add(parameterCollapsiblePanel, BorderLayout.NORTH);
		
		//mainPanel.add(parameterPanel, BorderLayout.NORTH);
		
		organizationLabel.setText(Msg.translate(Env.getCtx(), "AD_Org_ID"));
		parameterPanel.add(organizationLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,5,5,5), 0, 0));
		parameterPanel.add(organizationPick, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5,5,5,5), 0, 0));
		parameterPanel.add(salesRegionLabel, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		parameterPanel.add(salesRegion, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		parameterPanel.add(salesRepLabel, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		parameterPanel.add(salesRepSearch, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		parameterPanel.add(shipperLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			parameterPanel.add(shipperPick, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		parameterPanel.add(driverLabel, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
			,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		parameterPanel.add(driverSearch, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
			,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		parameterPanel.add(carLabel, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		parameterPanel.add(vehicleSearch, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		parameterPanel.add(capacityLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		parameterPanel.add(capacityField, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		//	Date
		parameterPanel.add(labelDateDoc, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		parameterPanel.add(fieldDateDoc, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		parameterPanel.add(labelShippDate, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		parameterPanel.add(fieldShipDate, new GridBagConstraints(5, 2, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		
		//	Unit Measure
		parameterPanel.add(uomVehicleLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		parameterPanel.add(uomVehiclePick, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		parameterPanel.add(uomWorkLabel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		parameterPanel.add(uomWorkPick, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		
		//	Document Type Oder
		parameterPanel.add(docTypeOrderLabel, new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		parameterPanel.add(docTypeOrderSearch, new GridBagConstraints(5, 3, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		
		//	Storage
		parameterPanel.add(warehouseLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		parameterPanel.add(warehouseSearch, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));

		parameterPanel.add(locatorLabel, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		parameterPanel.add(locatorSearch, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		
		/*parameterPanel.add(locatorToLabel, new GridBagConstraints(4, 4, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		parameterPanel.add(locatorToSearch, new GridBagConstraints(5, 4, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		*/
		
		parameterPanel.add(bSearch, new GridBagConstraints(5, 4, 1, 1, 0.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		
		
		//mainPanel.add(stockCollapsiblePanel, BorderLayout.SOUTH);
		mainPanel.add(stockInfoPanel, BorderLayout.SOUTH);
		
		
		CToggleButton selectAllButton = (CToggleButton)selectAllAction.getButton();
		selectAllButton.setMargin(ConfirmPanel.s_insets);
		selectAllButton.addActionListener(this);
		
		
		//	Botton Panel
		loadOrderPanel.add(selectAllButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
		loadOrderPanel.add(differenceLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
			,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
		loadOrderPanel.add(differenceField, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
			,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		loadOrderPanel.add(gLoadOrderButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
			,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
		
		orderPanel.add(orderLabel, BorderLayout.NORTH);
		orderPanel.add(orderInfo, BorderLayout.SOUTH);
		orderPanel.add(orderScrollPane, BorderLayout.CENTER);
		orderScrollPane.getViewport().add(orderTable, null);
		orderLinePanel.add(orderLineLabel, BorderLayout.NORTH);
		
		// Panel de Informacion y del Stock del Producto
		stockInfoPanel.setLayout(orderLineStockInfoLayout);
		
		stockInfoPanel.add(stockCollapsiblePanel, BorderLayout.NORTH);
		stockInfoPanel.add(loadOrderPanel, BorderLayout.SOUTH);
		
		orderLinePanel.add(orderLineInfo, BorderLayout.SOUTH);
		
		orderLinePanel.add(orderLineScrollPane, BorderLayout.CENTER);
		orderLineScrollPane.getViewport().add(orderLineTable, null);
		
		stockScrollPane.getViewport().add(stockTable, null);
		
		stockCollapsiblePanel.add(stockScrollPane);
		
		//
		mainPanel.add(infoPanel, BorderLayout.CENTER);
		infoPanel.setOrientation(JSplitPane.VERTICAL_SPLIT);
		infoPanel.setBorder(BorderFactory.createEtchedBorder());
		infoPanel.setTopComponent(orderPanel);
		infoPanel.setBottomComponent(orderLinePanel);
		infoPanel.add(orderPanel, JSplitPane.TOP);
		infoPanel.add(orderLinePanel, JSplitPane.BOTTOM);
		infoPanel.setContinuousLayout(true);
		infoPanel.setPreferredSize(new Dimension(800,250));
		infoPanel.setDividerLocation(150);
		orderLineInfo.setText(Msg.translate(Env.getCtx(), "total") + " = 0");
		loadMotoristVehicle();
	}   //  jbInit

	/**
	 * 	Dispose
	 */
	public void dispose()
	{
		if (m_frame != null)
			m_frame.dispose();
		m_frame = null;
	}	//	dispose

	/**
	 *  Dynamic Init (prepare dynamic fields)
	 *  @throws Exception if Lookups cannot be initialized
	 */
	public void dynInit() throws Exception
	{
			
		// Organization filter selection
		int AD_Column_ID = 849;
		MLookup lookupOrg = MLookupFactory.get(Env.getCtx(), m_WindowNo, AD_Column_ID, DisplayTypeConstants.TableDir);
		organizationPick = new VLookup("AD_Org_ID", true, false, true, lookupOrg);
		//organizationPick.setValue(Env.getAD_Org_ID(Env.getCtx()));
		organizationPick.addVetoableChangeListener(this);

		//  Shipper
		AD_Column_ID = 1003182;
		MLookup lookupSP = MLookupFactory.get(Env.getCtx(), m_WindowNo, AD_Column_ID, DisplayTypeConstants.TableDir);
		shipperPick = new VLookup("M_Shipper_ID", true, false, true, lookupSP);
		//shipperPick.setValue(Env.getAD_Org_ID(Env.getCtx()));
		shipperPick.addVetoableChangeListener(this);
		
		AD_Column_ID = 1823;		//	C_SalesRegion.C_SalesRegion_ID
		MLookup lookupWar = MLookupFactory.get(Env.getCtx(), m_WindowNo, AD_Column_ID, DisplayTypeConstants.TableDir);
		salesRegion = new VLookup("C_SalesRegion_ID", false, false, true, lookupWar);
		//salesRegion.setValue(Env.getAD_Org_ID(Env.getCtx()));
		salesRegion.addVetoableChangeListener(this);
		
		AD_Column_ID = 2186;		//	C_Order.SalesRep_ID
		MLookup lookupSal = MLookupFactory.get(Env.getCtx(), m_WindowNo, AD_Column_ID, DisplayTypeConstants.Table);
		salesRepSearch = new VLookup("SalesRep_ID", false, false, true, lookupSal);
		//salesRepSearch.setValue(Env.getAD_Org_ID(Env.getCtx()));
		salesRepSearch.addVetoableChangeListener(this);
		
		//  Capacity Unit Measure
		AD_Column_ID = 1003192;
		MLookup lookupUW = MLookupFactory.get(Env.getCtx(), m_WindowNo, AD_Column_ID, DisplayTypeConstants.Table);
		uomVehiclePick = new VLookup("Vehicle_UOM_ID", true, true, true, lookupUW);
		uomVehiclePick.addVetoableChangeListener(this);
		
		//  Working Unit Measure
		AD_Column_ID = 1003193;
		MLookup lookupUV = MLookupFactory.get(Env.getCtx(), m_WindowNo, AD_Column_ID, DisplayTypeConstants.Table);
		uomWorkPick = new VLookup("Work_UOM_ID", true, false, true, lookupUV);
		uomWorkPick.addVetoableChangeListener(this);
		
		//  Translation
		statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "SGLoadOrder"));
		statusBar.setStatusDB("");
		
		//	Conductor
		driverSearch.addActionListener(this);
		
		//	Vehiculo
		vehicleSearch.addActionListener(this);
		
		//	Document Type Order
		docTypeOrderSearch.addActionListener(this);
		
		//	Warehouse
		warehouseSearch.addActionListener(this);
		
		//	Locator
		locatorSearch.addActionListener(this);
		//	Locator To
		locatorToSearch.addActionListener(this);
		//	Search
		bSearch.addActionListener(this);
		
		//	Date
		fieldDateDoc.setMandatory(true);
		fieldDateDoc.setValue(new Timestamp(System.currentTimeMillis()));
		fieldShipDate.setMandatory(true);
		fieldShipDate.setValue(new Timestamp(System.currentTimeMillis()));
		
		//	Locator To
		locatorToLabel.setVisible(m_IsInternalLoad);
		locatorToSearch.setVisible(m_IsInternalLoad);
		
		/*orderLineTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
            	int col = ((MiniTable)me.getSource()).getSelectedColumn();
            	refresh(
            		((MiniTable)me.getSource()).getValueAt(row,2),
            		new BigDecimal(pickWarehouse.getValue().toString()).intValue(),
            		new BigDecimal(pickPriceList.getValue().toString()).intValue()
            		);
            	if(col == OL_PRODUCT){
            		stockCollapsiblePanel.setCollapsed(false);
            	}
            }
        });*/
		
		stockModel = new DefaultTableModel(null, getstockColumnNames());
		stockTable.setModel(stockModel);
		setStockColumnClass(stockTable);
		
	}   //  dynInit
	
	/**************************************************************************
	 *  Action Listener.
	 *  - MultiCurrency
	 *  - Allocate
	 *  @param e event
	 */
	public void actionPerformed(ActionEvent e)
	{
		log.config("");
		if(e.getSource().equals(vehicleSearch)){
			KeyNamePair pp = (KeyNamePair) vehicleSearch.getSelectedItem();
			int v_Vehicle_ID = (pp != null? pp.getKey(): 0);
			if(v_Vehicle_ID != 0) {
				MCUSTMotoristVehicle vehiculo = new MCUSTMotoristVehicle(Env.getCtx(), v_Vehicle_ID, null);
				capacityField.setValue(vehiculo.getCapacity());
				m_Vehicle_UOM_ID = vehiculo.getC_UOM_ID();
				uomVehiclePick.setValue(m_Vehicle_UOM_ID);
				calculate();
			} else {
				//capacityField.setReadWrite(false);
				capacityField.setValue(Env.ZERO);
			}
			clearData();
		} else if(e.getActionCommand().equals(SELECT_DESELECT_ALL)) {
			
			TableModel model = orderLineTable.getModel();
			//model.removeTableModelListener(this);
			
			// select or deselect all as required
			int rows = model.getRowCount();
			Boolean selectAll = selectAllAction.isPressed() ?
				Boolean.FALSE : Boolean.TRUE;
			for (int i = 0; i < rows; i++)
				model.setValueAt(selectAll, i, 0);
			
		} else if(e.getSource().equals(docTypeOrderSearch)){
			KeyNamePair pp = (KeyNamePair) docTypeOrderSearch.getSelectedItem();
			m_C_DocTypeOrder_ID = (pp != null? pp.getKey(): 0);
			setValueDocType(trx);
			locatorToLabel.setVisible(m_IsInternalLoad);
			locatorToSearch.setVisible(m_IsInternalLoad);
			if(m_IsInternalLoad){
				ArrayList<KeyNamePair> data = getDataLocatorTo();
				m_M_LocatorTo_ID = loadCombo(locatorToSearch, data);
			} else {
				m_M_LocatorTo_ID = 0;
				locatorToSearch.removeAllItems();
			}
			clearData();
		} else if(e.getSource().equals(warehouseSearch)){	//	Corregir para que haga las actualizaciones al momento de seleccionar
			KeyNamePair pp = (KeyNamePair) warehouseSearch.getSelectedItem();
			m_M_Warehouse_ID = (pp != null? pp.getKey(): 0);
			clearData();
			ArrayList<KeyNamePair> data = getDataLocator();
			m_M_Locator_ID = loadCombo(locatorSearch, data);
		} else if(e.getSource().equals(locatorSearch)){
			KeyNamePair pp = (KeyNamePair) locatorSearch.getSelectedItem();
			m_M_Locator_ID = (pp != null? pp.getKey(): 0);
			clearData();
		} else if(e.getSource().equals(locatorToSearch)){
			KeyNamePair pp = (KeyNamePair) locatorToSearch.getSelectedItem();
			m_M_LocatorTo_ID = (pp != null? pp.getKey(): 0);
			clearData();
		} else if(e.getSource().equals(gLoadOrderButton)){
			if(validData()){
				if (ADialog.ask(m_WindowNo, panel, "SGSaveQLoadOrder")){
					saveData();
				}
			}
		} else if (e.getSource() == bSearch)
			cmd_search();
	}   //  actionPerformed
	
	/**
	 * Busca los datos segun los parametros
	 */
	private void cmd_search(){
		getPanelValues();
		if(m_Work_UOM_ID != 0){
			if(m_Vehicle_UOM_ID != 0){
				if(m_C_DocTypeOrder_ID != 0){
					rateCapacity = MUOMConversion.getRate(Env.getCtx(), m_Vehicle_UOM_ID, m_Work_UOM_ID);
					if(rateCapacity != null){
						loadOrder();
					} else {
						ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotConversion") + " " 
								+ Msg.translate(Env.getCtx(), "of") + " "
								+ uomVehiclePick.getDisplay() + " " 
								+ Msg.translate(Env.getCtx(), "to") + " " 
								+ uomWorkPick.getDisplay()
								);
					}
				} else {
					ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotDocTypeOrder"));
					//loadOrder();
					calculate();
				}
			} else {
				ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotVehicleUOM"));
				//loadOrder();
				calculate();
			}
		} else {
			ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotWorkUOM"));
			//loadOrder();
			calculate();
		}
	}
	
	/**
	 * @author Yamel Senih 15/09/2012, 11:13
	 * Obtiene los valores de filtro y proceso del panel
	 */
	private void getPanelValues(){
		Object value = shipperPick.getValue();
		m_M_Shipper_ID = ((Integer)(value != null? value: 0)).intValue();
		value = uomVehiclePick.getValue();
		m_Vehicle_UOM_ID = ((Integer)(value != null? value: 0)).intValue();
		value = uomWorkPick.getValue();
		m_Work_UOM_ID = ((Integer)(value != null? value: 0)).intValue();
		String display = uomWorkPick.getDisplay();
		uomWorkValue = (display != null? " " + Msg.translate(Env.getCtx(), "SGIn") + " " + display: "");
		KeyNamePair pp = (KeyNamePair) driverSearch.getSelectedItem();
		m_Motorist_ID = (pp != null? pp.getKey(): 0);
		pp = (KeyNamePair) vehicleSearch.getSelectedItem();
		m_Vehicle_ID = (pp != null? pp.getKey(): 0);
		pp = (KeyNamePair) docTypeOrderSearch.getSelectedItem();
		m_C_DocTypeOrder_ID = (pp != null? pp.getKey(): 0);
		pp = (KeyNamePair) warehouseSearch.getSelectedItem();
		m_M_Warehouse_ID = (pp != null? pp.getKey(): 0);
		pp = (KeyNamePair) locatorSearch.getSelectedItem();
		m_M_Locator_ID = (pp != null? pp.getKey(): 0);
		pp = (KeyNamePair) locatorToSearch.getSelectedItem();
		m_M_LocatorTo_ID = (pp != null? pp.getKey(): 0);
		setValueDocType(trx);
	}
	
	
	/**
	 * Valida los datos antes de generar la orden de carga
	 * @author Yamel Senih 03/04/2012, 18:26:08
	 * @return boolean
	 */
	private boolean validData(){
		getPanelValues();
		if(m_AD_Org_ID != 0){
			if(m_M_Shipper_ID != 0){
				if(m_Motorist_ID != 0){
					if(m_Vehicle_ID != 0){
						if(m_Work_UOM_ID != 0){
							if(m_C_DocTypeOrder_ID != 0){
								if(m_M_Warehouse_ID != 0){
									if(m_M_Locator_ID != 0){
										if(m_M_Locator_ID != m_M_LocatorTo_ID){
											if(totalWeight.doubleValue() > 0){
												BigDecimal difference = (BigDecimal) (differenceField.getValue() != null? differenceField.getValue(): Env.ZERO);
												if(difference.compareTo(Env.ZERO) >= 0){
													if(m_IsInternalLoad){
														if(m_M_LocatorTo_ID != 0){
															return true;
														} else {
															ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotLocatorTo"));
														}
													} else {
														return true;
													}
												} else {
													ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGCarNotCapacity"));
												}	
											} else {
												ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGSumZero"));
											}
										} else {
											ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGLocatorsWheight"));
										}
									} else {
										ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotWarehouse"));
									}
								} else {
									ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotLocator"));
								}
							} else {
								ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotDocTypeOrder"));
							}
						} else {
							ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotWorkUOM"));
						}
					} else {
						ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotCar"));
					}
				} else {
					ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotBPartner"));
				}
			} else {
				ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotShipper"));
			}	
		} else {
			ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotOrg"));
		}
		return false;
	}
	
	/**
	 *  Table Model Listener.
	 *  - Recalculate Totals
	 *  @param e event
	 */
	public void tableChanged(TableModelEvent e)
	{
		boolean isUpdate = (e.getType() == TableModelEvent.UPDATE);
		int row = e.getFirstRow();
		int col = e.getColumn();
		//  Not a table update
		if (!isUpdate)
		{
			calculate();
			return;
		}
		
		boolean isOrder = (e.getSource().equals(orderTable.getModel()));
		boolean isOrderLine = (e.getSource().equals(orderLineTable.getModel()));
		if(isOrder){
			if(m_Work_UOM_ID != 0){
				if(m_Vehicle_UOM_ID != 0){
					if(m_C_DocTypeOrder_ID != 0){
						if(m_IsBulk && moreOneSelect(orderTable)){
							ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGIsBulkMaxOne"));
							orderTable.setValueAt(false, row, SELECT);
							loadOrder();
							calculate();
						} else {
							rateCapacity = MUOMConversion.getRate(Env.getCtx(), m_Vehicle_UOM_ID, m_Work_UOM_ID);
							if(rateCapacity != null){
								StringBuffer sql = getQueryLine(orderTable);
								Vector<Vector<Object>> data = getOrderLineData(orderTable, sql);
								Vector<String> columnNames = getOrderLineColumnNames();
								
								loadBuffer(orderLineTable);
								//  Remove previous listeners
								orderLineTable.getModel().removeTableModelListener(this);
								
								//  Set Model
								DefaultTableModel modelP = new DefaultTableModel(data, columnNames);
								modelP.addTableModelListener(this);
								orderLineTable.setModel(modelP);
								setOrderLineColumnClass(orderLineTable);
								setValueFromBuffer(orderLineTable);
							} else {
								ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotConversion") + " " 
										+ Msg.translate(Env.getCtx(), "of") + " "
										+ uomVehiclePick.getDisplay() + " " 
										+ Msg.translate(Env.getCtx(), "to") + " " 
										+ uomWorkPick.getDisplay()
										);
								//loadOrder();
								calculate();
							}
						}
					} else {
						ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotDocTypeOrder"));
						//loadOrder();
						calculate();
					}
				} else {
					ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotVehicleUOM"));
					//loadOrder();
					calculate();
				}
			} else {
				ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGNotWorkUOM"));
				//loadOrder();
				calculate();
			}
			
		}else if(isOrderLine){
			//int row = e.getFirstRow();
			//int col = e.getColumn();
			if(col == OL_QTY_SET){	//Qty
				BigDecimal qty = (BigDecimal) orderLineTable.getValueAt(row, OL_QTY);
				BigDecimal qtySet = (BigDecimal) orderLineTable.getValueAt(row, OL_QTY_SET);
				BigDecimal qtyOrdered = (BigDecimal) orderLineTable.getValueAt(row, OL_QTY_ORDERED);
				BigDecimal qtyDelivered = (BigDecimal) orderLineTable.getValueAt(row, OL_QTY_DELIVERED);
				BigDecimal qtyOrderLine = (BigDecimal) orderLineTable.getValueAt(row, OL_QTY_ORDER_LINE);
				//2014-01-29 Carlos Parada Add Field Returned
				BigDecimal qtyReturned = (BigDecimal) orderLineTable.getValueAt(row, OL_QTY_RETURNED);
				
				KeyNamePair pr = (KeyNamePair) orderLineTable.getValueAt(row, OL_PRODUCT);
				
				int m_M_Product_ID = pr.getKey();
				
				//BigDecimal weight = (BigDecimal) orderLineTable.getValueAt(row, OL_QTY_SET);
				
				BigDecimal rateQty = MUOMConversion.getProductRateFrom(Env.getCtx(), m_M_Product_ID, m_Work_UOM_ID);
				BigDecimal rateQtySet = MUOMConversion.getProductRateTo(Env.getCtx(), m_M_Product_ID, m_Work_UOM_ID);
				
				if(rateQty != null){
					orderLineTable.setValueAt(qtySet.multiply(rateQty), row, OL_QTY);
					
					//Round No Decimal
					if(qtySet.multiply(rateQty).setScale(0, BigDecimal.ROUND_HALF_UP).
							compareTo(qtyOrdered.
									subtract(qtyDelivered).
									subtract(qtyOrderLine).
									add(qtyReturned).setScale(0, BigDecimal.ROUND_HALF_UP)) > 0){
						
						ADialog.warn(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGQtyEx"));
						qty = qtyOrdered.subtract(qtyDelivered).subtract(qtyOrderLine).add(qtyReturned);
						orderLineTable.setValueAt(qty, row, OL_QTY);
						orderLineTable.setValueAt(qty.multiply(rateQtySet), row, OL_QTY_SET);
					} else if(qtySet.compareTo(Env.ZERO) <= 0){
						ADialog.warn(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGQtyLessZero"));
						qty = qtyOrdered.subtract(qtyDelivered).subtract(qtyOrderLine).add(qtyReturned);
						qtySet = qty.multiply(rateQty);
						orderLineTable.setValueAt(qtySet, row, OL_QTY_SET);
						orderLineTable.setValueAt(qty, row, OL_QTY);
					}
				} 
			} else if(col == SELECT){
				//	Select One
				if(m_IsBulk && moreOneSelect(orderLineTable)){
					ADialog.info(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGIsBulkMaxOneLine"));
					orderLineTable.setValueAt(false, row, SELECT);
				} else {
					boolean select = (Boolean) orderLineTable.getValueAt(row, col);
					if(select){
						m_MaxSeqNo += 10;
						orderLineTable.setValueAt(m_MaxSeqNo, row, OL_SEQNO);
					}
					//System.out.println("VLoadOrder.tableChanged() " + select);
					//	Agrupado de Productos
					loadProductsStock(orderLineTable, row, select);
				}
				
				
			} else if(col == OL_SEQNO){
				int seqNo = (Integer) orderLineTable.getValueAt(row, OL_SEQNO);
				if(!exists_seqNo(orderLineTable, row, seqNo)){
					if(seqNo > m_MaxSeqNo){
						m_MaxSeqNo = seqNo;
					}
				} else {
					ADialog.warn(m_WindowNo, panel, Msg.translate(Env.getCtx(), "SGSeqNoEx"));
					m_MaxSeqNo += 10;
					orderLineTable.setValueAt(m_MaxSeqNo, row, OL_SEQNO);
				}
			}
			//	Load Group by Product
			loadStockWarehouse(orderLineTable);
		}
		
		calculate();
	}   //  tableChanged

	/**
	 *  Vetoable Change Listener.
	 *  
	 *  @param e event
	 */
	public void vetoableChange (PropertyChangeEvent e)
	{
		String name = e.getPropertyName();
		Object value = e.getNewValue();
		log.config(name + " = " + value);
		
		if(name.equals("C_SalesRegion_ID") || 
				name.equals("SalesRep_ID") ||
				name.equals("Work_UOM_ID")) {
			
			//loadOrder();
		} else if(name.equals("AD_Org_ID")){
			m_AD_Org_ID = ((Integer)(value != null? value: 0)).intValue();
			ArrayList<KeyNamePair> data = getDataDocumentOrder();
			m_C_DocTypeOrder_ID = loadCombo(docTypeOrderSearch, data);
			if (m_C_DocTypeOrder_ID != 0) {
				setValueDocType(trx);
			} 
			data = getDataWarehouse();
			m_M_Warehouse_ID = loadCombo(warehouseSearch, data);
			if(m_M_Warehouse_ID != 0){
				data = getDataLocator();
				m_M_Locator_ID = loadCombo(locatorSearch, data);
			}
			
		} else if(name.equals("M_Shipper_ID")){
			m_M_Shipper_ID = ((Integer)(value != null? value: 0)).intValue();
			loadMotoristVehicle();
		}
		calculate();
		
	}   //  vetoableChange
	
	/**
	 * Carga los datos del Conductor y Vehiculo
	 */
	private void loadMotoristVehicle(){
		ArrayList<KeyNamePair> data = getDataDriver();
		m_Motorist_ID = loadCombo(driverSearch, data);
		//	Vehicle
		data = getDataCar();
		m_Vehicle_ID = loadCombo(vehicleSearch, data);
		//	Unit Measure
		if(m_Vehicle_ID != 0) {//System.err.println(m_Vehicle_ID);
			MCUSTMotoristVehicle vehiculo = new MCUSTMotoristVehicle(Env.getCtx(), m_Vehicle_ID, null);
			capacityField.setValue(vehiculo.getCapacity());
			uomVehiclePick.setValue(vehiculo.getC_UOM_ID());
		} else {
			//capacityField.setReadWrite(false);
			capacityField.setValue(Env.ZERO);
		}
	}
	
	public void loadOrder()
	{		//System.out.println("VLoadOrder.loadOrder()");

		String name = organizationPick.getName();
		Object value = organizationPick.getValue();
		m_AD_Org_ID = ((Integer)(value != null? value: 0)).intValue();
		log.config(name + "=" + value);
		
		name = salesRegion.getName();
		value = salesRegion.getValue();
		m_C_SalesRegion_ID = ((Integer)(value != null? value: 0)).intValue();
		log.config(name + "=" + value);
		
		name = salesRepSearch.getName();
		value = salesRepSearch.getValue();
		m_SalesRep_ID = ((Integer)(value != null? value: 0)).intValue();
		log.config(name + "=" + value);
		
		name = uomWorkPick.getName();
		value = uomWorkPick.getValue();
		String display = uomWorkPick.getDisplay();
		m_Work_UOM_ID = ((Integer)(value != null? value: 0)).intValue();
		uomWorkValue = (display != null? " " + Msg.translate(Env.getCtx(), "SGIn") + " " + display: "");
		log.config(name + "=" + value);
		
		/*name = docTypeOrderSearch.getName();
		value = docTypeOrderSearch.getValue();
		m_C_DocTypeOrder_ID = ((Integer)(value != null? value: 0)).intValue();
		log.config(name + "=" + value);
		setValueDocType(trx);*/
		
		//	Load Data
		Vector<Vector<Object>> data = getOrderData(m_AD_Org_ID, m_C_SalesRegion_ID, 
				m_SalesRep_ID, m_C_DocTypeOrder_ID, 
				orderTable);
		Vector<String> columnNames = getOrderColumnNames();
		
		//  Remove previous listeners
		orderTable.getModel().removeTableModelListener(this);
		
		
		//  Set Model
		DefaultTableModel modelP = new DefaultTableModel(data, columnNames);
		modelP.addTableModelListener(this);
		orderTable.setModel(modelP);
		setOrderColumnClass(orderTable);
		
		orderLineTable.getModel().removeTableModelListener(this);
		
		//  Set Model Line
		DefaultTableModel modelLine = new DefaultTableModel();
		orderLineTable.setModel(modelLine);
		//
	}
	
	/**
	 * Limpia los datos de las tablas
	 */
	private void clearData(){
		orderTable.getModel().removeTableModelListener(this);
		DefaultTableModel modelP = new DefaultTableModel();
		modelP.addTableModelListener(this);
		orderTable.setModel(modelP);
		
		orderLineTable.getModel().removeTableModelListener(this);
		
		//  Set Model Line
		DefaultTableModel modelLine = new DefaultTableModel();
		orderLineTable.setModel(modelLine);
		selectAllAction.setPressed(false);
	}
	
	/**
	 * 
	 * Calcula la diferencia de pesos y el peso total
	 *
	 */
	public void calculate(){
		int rows = orderLineTable.getRowCount();
		if(rows > 0){
			capacity = Env.ZERO;
			totalWeight = Env.ZERO;
			BigDecimal weight = Env.ZERO;
			BigDecimal difference = Env.ZERO;
			for (int i = 0; i < rows; i++) {
				if (((Boolean)orderLineTable.getValueAt(i, 0)).booleanValue()) {
					weight = (BigDecimal) (orderLineTable.getValueAt(i, OL_QTY_SET) != null
							? orderLineTable.getValueAt(i, OL_QTY_SET)
									: Env.ZERO);
					totalWeight = totalWeight.add(weight);
				}
			}
			if(totalWeight.compareTo(Env.ZERO) > 0){
				capacity = (BigDecimal) (capacityField.getValue() != null? capacityField.getValue(): Env.ZERO);
				if(rateCapacity != null){
					difference = capacity.multiply(rateCapacity).subtract(totalWeight);
				}
			}
			differenceLabel.setText(Msg.getMsg(Env.getCtx(), "SGDiffWeight") + uomWorkValue);
			differenceField.setValue(difference.doubleValue());
			orderLineInfo.setText(
					"(" + Msg.translate(Env.getCtx(), "SGOrdersSelected")
					+ " = " +  m_RowsSelected
					+ ") "
					+ Msg.translate(Env.getCtx(), "SGTotal") 
					+ uomWorkValue
					+ " = " + totalWeight.doubleValue());
		} else {
			differenceLabel.setText(Msg.getMsg(Env.getCtx(), "SGDiffWeight"));
			differenceField.setValue(Env.ZERO);
			orderLineInfo.setText(Msg.translate(Env.getCtx(), "SGOrderLineSum") + " = " + Env.ZERO);
		}
	}
	
	/**************************************************************************
	 *  Save Data
	 */
	public void saveData()
	{
		try	{	
			String msg = generateLoatOrder(orderLineTable, fieldDateDoc.getTimestamp(), fieldShipDate.getTimestamp(), trx);
			statusBar.setStatusLine(msg);
			trx.commit();
			ADialog.info(m_WindowNo, panel, msg);
			//shipperPick.setValue(0);
			uomVehiclePick.setValue(0);
			//uomWorkPick.setValue(0);
			driverSearch.removeAllItems();
			vehicleSearch.removeAllItems();
			//locatorToSearch.removeAllItems();
			//loadOrder();
			clearData();
			calculate();
		}
		catch (Exception e)	{
			trx.rollback();
			ADialog.error(m_WindowNo, panel, "Error", e.getLocalizedMessage());
			statusBar.setStatusLine("Error" + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
	}   //  saveData

}