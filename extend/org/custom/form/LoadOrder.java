/**
 * 
 */
package org.custom.form;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.table.DefaultTableModel;

import org.compiere.minigrid.MiniTable;
import org.compiere.model.MDocType;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MUOMConversion;
import org.compiere.swing.CComboBox;
import org.compiere.util.CLogger;
import org.compiere.util.CompiereSystemException;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.custom.model.BufferTableSelect;
import org.custom.model.MCUSTLoadOrder;
import org.custom.model.MCUSTLoadOrderLine;

/**
 * @author Yamel Senih 24/06/2011, 12:57
 *
 */
public class LoadOrder {

	/**	Logger			*/
	public static CLogger log = CLogger.getCLogger(LoadOrder.class);
	
	public final int OL_SO_LOC = 6;
	public final int SELECT = 0;
	public final int ORDER = 2;
	public final int ORDER_LINE = 2;
	public final int OL_PRODUCT = 3;
	public final int OL_UOM_CONVERSION = 4;
	public final int OL_QTY_ENTERED = 5;
	public final int OL_UOM = 6;
	public final int OL_QTY_ONDHAND = 7;
	public final int OL_QTY_ORDERED = 8;
	public final int OL_QTY_RESERVERD = 9;
	public final int OL_QTY_INVOICED = 10;
	public final int OL_QTY_DELIVERED = 11;
	//2014-01-29 Carlos Parada Add Return Column And Change Order 
	public final int OL_QTY_RETURNED = 12;
	public final int OL_QTY_ORDER_LINE = 13;
	public final int OL_QTY = 14;
	public final int OL_QTY_SET = 15;
	public final int OL_SEQNO = 16;
	
	//	
	public final int SW_PRODUCT = 0;
	public final int SW_QTYONHAND = 1;
	public final int SW_QTYSET = 2;
	public final int SW_QTYAVAILABLE = 3;
	
	/**	Buffer				*/
	public Vector<BufferTableSelect> m_BufferSelect = null;
	
	public StringBuffer m_Symmary = new StringBuffer();
	public StringBuffer m_QueryAdd = new StringBuffer();
	
	/**	Organization		*/
	protected int 		m_AD_Org_ID = 0;
	/**	Warehouse			*/
	protected int 		m_C_SalesRegion_ID = 0;
	/**	Sales Rep			*/
	protected int 		m_SalesRep_ID = 0;;
	/**	Max Sequence		*/
	protected int		m_MaxSeqNo = 0;
	/**	Shipper				*/
	protected int 		m_M_Shipper_ID = 0;
	/**	Motorist			*/
	protected int 		m_Motorist_ID = 0;
	/**	Vehicle				*/
	protected int 		m_Vehicle_ID = 0;
	/**	Vehicle Unit Measure*/
	protected int 		m_Vehicle_UOM_ID = 0;
	/**	Work Unit Measure	*/
	protected int 		m_Work_UOM_ID = 0;
	/**	Document Type Order	*/
	protected int 		m_C_DocTypeOrder_ID = 0;
	/**	Warehouse			*/
	protected int 		m_M_Warehouse_ID = 0;
	/**	Locator				*/
	protected int		m_M_Locator_ID = 0;
	/**	Locator To			*/
	protected int		m_M_LocatorTo_ID = 0;
	/**	Rows Selected		*/
	protected int		m_RowsSelected = 0;
	/**	Is Internal Load	*/
	protected boolean	m_IsInternalLoad = false;
	/**	Is Bulk Product		*/
	protected boolean	m_IsBulk = false;
	/**	Converions			*/
	protected BigDecimal rateCapacity = null;
	
	/**	Total Weight		*/
	protected BigDecimal	totalWeight = Env.ZERO;
	/**	Capacity			*/
	protected BigDecimal	capacity = Env.ZERO;
	
	protected MiniTable			stockTable = new MiniTable();
	protected DefaultTableModel 	stockModel = null;
	
	protected int 					m_WindowNo = 0;
	
	protected Vector<Vector<Object>> getOrderData(int p_AD_Org_ID, int p_M_Warehouse_ID, 
			int p_SalesRep_ID, int p_C_DocTypeOrder_ID, 
			MiniTable orderTable){
		/**
		 * Carga los datos de las ordenes de Venta 
		 * 
		 * 
		 */
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		StringBuffer sql = new StringBuffer("SELECT " +
				"wr.Name Warehouse, ord.C_Order_ID, ord.DocumentNo, " +	//	1..3
				"ord.DateOrdered, ord.DatePromised, SUM(lord.QtyOrdered) Weight, sr.Name SalesRep, " +	//	4..7
				"cp.Name Partner, bploc.Name, " +	//	8..9
				"reg.Name, cit.Name, loc.Address1, loc.Address2, loc.Address3, loc.Address4, ord.C_BPartner_Location_ID " +	//	10..14
				"FROM C_Order ord " +
				"INNER JOIN C_OrderLine lord ON(lord.C_Order_ID = ord.C_Order_ID) " +
				"INNER JOIN C_BPartner cp ON(cp.C_BPartner_ID = ord.C_BPartner_ID) " +
				"INNER JOIN AD_User sr ON(sr.AD_User_ID = ord.SalesRep_ID) " +
				"INNER JOIN M_Warehouse wr ON(wr.M_Warehouse_ID = ord.M_Warehouse_ID) " +
				"INNER JOIN C_BPartner_Location bploc ON(bploc.C_BPartner_Location_ID = ord.C_BPartner_Location_ID) " +
				"INNER JOIN C_Location loc ON(loc.C_Location_ID = bploc.C_Location_ID) " +
				"INNER JOIN C_Region reg ON(reg.C_Region_ID = loc.C_Region_ID) " +
				"LEFT JOIN C_City cit ON(cit.C_City_ID = loc.C_City_ID) " +
				"LEFT JOIN (SELECT lord.C_Order_ID, " +
				"	(COALESCE(lord.QtyOrdered, 0) - SUM(CASE WHEN c.DocStatus = 'CO' THEN lc.Qty ELSE 0 END))+Coalesce(return.QtyReturned,0) QtyAvailable " +
				"	FROM C_OrderLine lord " +
				"	LEFT JOIN CUST_LoadOrderLine lc ON(lc.C_OrderLine_ID = lord.C_OrderLine_ID) " +
				"	LEFT JOIN CUST_LoadOrder c ON(c.CUST_LoadOrder_ID = lc.CUST_LoadOrder_ID) " +
				//2014-01-29 Carlos Parada Add Returns to Load Order Windows
					"LEFT JOIN (Select colorig.C_OrderLine_ID,Sum(miol.QtyEntered) As QtyReturned From " + 
							"CUST_RecordWeight crw " +
							"Inner Join C_DocType cdt On crw.C_DocTypeTarget_ID = cdt.C_DocType_ID " +
							"Inner Join C_Order co On co.C_Order_ID = crw.M_RMA_ID " +
							"Inner Join C_OrderLine col On co.C_Order_ID = col.C_Order_ID " +
							"Inner Join C_OrderLine colorig On col.Orig_OrderLine_ID=colorig.C_OrderLine_ID " +
							"Inner Join M_InOutLine miol On miol.C_OrderLine_ID = col.C_OrderLine_ID " +
							"Inner Join M_InOut mio On mio.M_InOut_ID = miol.M_InOut_ID " +
							"Where cdt.DocBaseType = 'SRW' And " + 
							"cdt.IsReturnTrx='Y' And " + 
							"mio.DOCSTATUS In ('CO','CL') "+
							"Group By  colorig.C_OrderLine_ID) return ON return.C_OrderLine_ID= lord.C_OrderLine_ID " +
 				//End Carlos Parada
				"	WHERE lord.M_Product_ID IS NOT NULL " +
				"	GROUP BY lord.C_Order_ID, lord.C_OrderLine_ID, lord.QtyOrdered, return.QtyReturned " +
				//"	HAVING (COALESCE(lord.QtyOrdered, 0) - COALESCE(lord.QtyDelivered, 0) - SUM(CASE WHEN c.DocStatus = 'CO' THEN lc.Qty ELSE 0 END)) > 0 " +
				"	ORDER BY lord.C_OrderLine_ID ASC) QAFL " +
				"	ON(QAFL.C_Order_ID = ord.C_Order_ID) " +
				//2014-01-29 Carlos Parada Add Returns to Load Order Windows
				"LEFT JOIN (Select colorig.C_OrderLine_ID,Sum(miol.QtyEntered) As QtyReturned From " + 
						"CUST_RecordWeight crw " +
						"Inner Join C_DocType cdt On crw.C_DocTypeTarget_ID = cdt.C_DocType_ID " +
						"Inner Join C_Order co On co.C_Order_ID = crw.M_RMA_ID " +
						"Inner Join C_OrderLine col On co.C_Order_ID = col.C_Order_ID " +
						"Inner Join C_OrderLine colorig On col.Orig_OrderLine_ID=colorig.C_OrderLine_ID " +
						"Inner Join M_InOutLine miol On miol.C_OrderLine_ID = col.C_OrderLine_ID " +
						"Inner Join M_InOut mio On mio.M_InOut_ID = miol.M_InOut_ID " +
						"Where cdt.DocBaseType = 'SRW' And " + 
						"cdt.IsReturnTrx='Y' And " + 
						"mio.DOCSTATUS In ('CO','CL') "+
						"Group By  colorig.C_OrderLine_ID) return ON return.C_OrderLine_ID= lord.C_OrderLine_ID " +
				//End Carlos Parada
				
				"WHERE ord.IsSOTrx = 'Y' " +
				"AND wr.IsActive = 'Y' " +
				"AND ord.DocStatus = 'CO' " +
				"AND (QAFL.QtyAvailable > 0 OR QAFL.QtyAvailable IS NULL) " +
				"AND ord.AD_Client_ID=? ");
		if (p_AD_Org_ID != 0)
			sql.append("AND ord.AD_Org_ID=? ");
		if (p_M_Warehouse_ID != 0 )
			sql.append("AND bploc.C_SalesRegion_ID=? ");
		if (p_SalesRep_ID != 0 )
			sql.append("AND ord.SalesRep_ID=? ");
		if (p_C_DocTypeOrder_ID != 0 )
			sql.append("AND ord.C_DocType_ID=? ");
		
		//	Group By
		sql.append("GROUP BY wr.Name, ord.C_Order_ID, ord.DocumentNo, ord.DateOrdered, " +
				"ord.DatePromised, sr.Name, cp.Name, bploc.Name, " +
				"reg.Name, cit.Name, loc.Address1, loc.Address2, loc.Address3, loc.Address4, ord.C_BPartner_Location_ID,return.QtyReturned ");
	
		//	Having
		sql.append("HAVING (SUM(COALESCE(lord.QtyOrdered, 0)) - SUM(COALESCE(lord.QtyDelivered, 0))) + Coalesce(return.QtyReturned,0) > 0 ");
		
		
		//	Order By
		sql.append("ORDER BY ord.C_Order_ID ASC");
		
		// role security
		
		log.fine("LoadOrderSQL=" + sql.toString());
		//System.out.println(sql);
		try
		{
			int param = 1;
			int column = 1;
			
			PreparedStatement pstmt = DB.prepareStatement(sql.toString(), null);
			
			pstmt.setInt(param++, Env.getCtx().getAD_Client_ID());
			
			if (p_AD_Org_ID != 0)
				pstmt.setInt(param++, p_AD_Org_ID);
			if (p_M_Warehouse_ID != 0 )
				pstmt.setInt(param++, p_M_Warehouse_ID);
			if (p_SalesRep_ID != 0 )
				pstmt.setInt(param++, p_SalesRep_ID);
			if (p_C_DocTypeOrder_ID != 0 )
				pstmt.setInt(param++, p_C_DocTypeOrder_ID);
			
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{
				column = 1;
				Vector<Object> line = new Vector<Object>();
				line.add(new Boolean(false));       		//  0-Selection
				line.add(rs.getString(column++));       	//  1-Warehouse
				KeyNamePair pp = new KeyNamePair(rs.getInt(column++), rs.getString(column++));
				line.add(pp);				       			//  2-DocumentNo
				line.add(rs.getTimestamp(column++));      	//  3-DateOrdered
				line.add(rs.getTimestamp(column++));      	//  4-DatePromised
				line.add(rs.getBigDecimal(column++));		//	5-Weight
				line.add(rs.getString(column++));			//	6-Sales Representative
				line.add(rs.getString(column++));			//	7-Business Partner
				line.add(rs.getString(column++));			//	8-Location
				line.add(rs.getString(column++));			//	9-Region
				line.add(rs.getString(column++));			//	10-City
				line.add(rs.getString(column++));			//	11-Address 1
				line.add(rs.getString(column++));			//	11-Address 2
				line.add(rs.getString(column++));			//	12-Address 3
				line.add(rs.getString(column++));			//	13-Address 4
				//	
				data.add(line);
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		
		return data;
	}
	
	/**
	 * Obtiene los nombres de las columnas de las ordenes de carga
	 * @return
	 */
	protected Vector<String> getOrderColumnNames(){	
		//  Header Info
		Vector<String> columnNames = new Vector<String>();
		columnNames.add(Msg.getMsg(Env.getCtx(), "Select"));
		columnNames.add(Msg.translate(Env.getCtx(), "M_Warehouse_ID"));
		columnNames.add(Msg.translate(Env.getCtx(), "DocumentNo"));
		columnNames.add(Msg.translate(Env.getCtx(), "DateOrdered"));
		columnNames.add(Msg.translate(Env.getCtx(), "DatePromised"));
		columnNames.add(Msg.translate(Env.getCtx(), "Weight"));
		columnNames.add(Msg.translate(Env.getCtx(), "SalesRep_ID"));
		columnNames.add(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		columnNames.add(Msg.translate(Env.getCtx(), "C_Location_ID"));
		columnNames.add(Msg.translate(Env.getCtx(), "C_Region_ID"));
		columnNames.add(Msg.translate(Env.getCtx(), "C_City_ID"));
		columnNames.add(Msg.translate(Env.getCtx(), "Address1"));
		columnNames.add(Msg.translate(Env.getCtx(), "Address2"));
		columnNames.add(Msg.getElement(Env.getCtx(), "Address3"));
		columnNames.add(Msg.getElement(Env.getCtx(), "Address4"));
		//
		return columnNames;
	}
	
	/**
	 * Establece las clases de las columnas de las ordenes
	 * @param orderTable
	 */
	protected void setOrderColumnClass(MiniTable orderTable){
		int i = 0;
		orderTable.setColumnClass(i++, Boolean.class, false);			//  0-Selection
		orderTable.setColumnClass(i++, String.class, true);			//  1-Warehouse
		orderTable.setColumnClass(i++, String.class, true);			//  2-DocumentNo
		orderTable.setColumnClass(i++, Timestamp.class, true);			//  3-DateOrdered
		orderTable.setColumnClass(i++, Timestamp.class, true);			//  4-DatePromiset
		orderTable.setColumnClass(i++, BigDecimal.class, true);		//  5-Weight
		orderTable.setColumnClass(i++, String.class, true);			//  6-Sales Representative
		orderTable.setColumnClass(i++, String.class, true);			//  7-Business Partner
		orderTable.setColumnClass(i++, String.class, true);			//  8-Location
		orderTable.setColumnClass(i++, String.class, true);			//  9-Region
		orderTable.setColumnClass(i++, String.class, true);			//  10-City
		orderTable.setColumnClass(i++, String.class, true);			//  11-Address 1
		orderTable.setColumnClass(i++, String.class, true);			//  11-Address 2
		orderTable.setColumnClass(i++, String.class, true);			//  12-Address 3
		orderTable.setColumnClass(i++, String.class, true);			//  13-Address 4
		//  Table UI
		orderTable.autoSize();
	}
	
	/**
	 * Obtiene los datos de las lineas de la orden de carga
	 * @param orderLineTable
	 * @param sqlPrep
	 * @return
	 */
	protected Vector<Vector<Object>> getOrderLineData(MiniTable orderLineTable, StringBuffer sqlPrep){
		/**
		 * Carga los datos de las ordenes de Venta 
		 * 
		 * 
		 */
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		
		// role security
		//sqlPrep = new StringBuffer( MRole.getDefault(Env.getCtx(), false).addAccessSQL( sqlPrep.toString(), "ord", MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO ) );
		
		log.fine("LoadOrderLineSQL=" + sqlPrep.toString());
		try
		{
			
			PreparedStatement pstmt = DB.prepareStatement(sqlPrep.toString(), null);
			ResultSet rs = pstmt.executeQuery();
			//int seqNo = 0;
			int column = 1;
			BigDecimal rate = Env.ZERO;
			BigDecimal qty = Env.ZERO;
			while (rs.next())
			{
				column = 1;
				Vector<Object> line = new Vector<Object>();
				line.add(new Boolean(false));       	//  0-Selection
				KeyNamePair wr = new KeyNamePair(rs.getInt(column++), rs.getString(column++));
				line.add(wr);       					//  1-Warehouse
				KeyNamePair lo = new KeyNamePair(rs.getInt(column++), rs.getString(column++));
				line.add(lo);				       		//  2-DocumentNo
				KeyNamePair pr = new KeyNamePair(rs.getInt(column++), rs.getString(column++));
				line.add(pr);				      		//  3-Product
				KeyNamePair uo = new KeyNamePair(rs.getInt(column++), rs.getString(column++));
				line.add(uo);				      		//  4-Unit Conversion
				line.add(rs.getBigDecimal(column++));  	//  5-QtyEntered
				KeyNamePair uop = new KeyNamePair(rs.getInt(column++), rs.getString(column++));
				line.add(uop);				      		//  6-Unit Product
				line.add(rs.getBigDecimal(column++));  	//  7-QtyOnHand
				line.add(rs.getBigDecimal(column++));  	//  8-QtyOrdered
				line.add(rs.getBigDecimal(column++)); 	//  9-QtyReserved
				line.add(rs.getBigDecimal(column++));  	//  10-QtyInvoiced
				line.add(rs.getBigDecimal(column++));	//  11-QtyDelivered
				//2014-01-29 Carlos Parada Add QtyReturned
				line.add(rs.getBigDecimal(column++));	//  12-QtyDelivered
				line.add(rs.getBigDecimal(column++));	//  13-QtyLoc
				
				qty = rs.getBigDecimal(column++);
				
				line.add(qty);							//  14-Qty
				rate = MUOMConversion.getProductRateTo(Env.getCtx(), pr.getKey(), m_Work_UOM_ID);
				if(rate != null){
					line.add(qty.multiply(rate));		//  15-Qty Set
					data.add(line);
				}
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sqlPrep.toString(), e);
		}
		
		return data;
	}
	
	/**
	 * Obtiene los nombres de las columnas de las lineas de la orden de carga
	 * @return
	 */
	protected Vector<String> getOrderLineColumnNames(){	
		//  Header Info
		Vector<String> columnNames = new Vector<String>();
		columnNames.add(Msg.getMsg(Env.getCtx(), "Select"));
		columnNames.add(Msg.translate(Env.getCtx(), "M_Warehouse_ID"));
		columnNames.add(Msg.translate(Env.getCtx(), "DocumentNo"));
		columnNames.add(Msg.translate(Env.getCtx(), "M_Product_ID"));
		columnNames.add(Msg.translate(Env.getCtx(), "C_UOM_To_ID"));
		columnNames.add(Msg.translate(Env.getCtx(), "QtyEntered"));
		columnNames.add(Msg.translate(Env.getCtx(), "C_UOM_ID"));
		columnNames.add(Msg.translate(Env.getCtx(), "QtyOnHand"));
		columnNames.add(Msg.translate(Env.getCtx(), "SGQtyOrdered"));
		columnNames.add(Msg.translate(Env.getCtx(), "SGQtyReserved"));
		columnNames.add(Msg.translate(Env.getCtx(), "SGQtyInvoiced"));
		columnNames.add(Msg.translate(Env.getCtx(), "SGQtyDelivered"));
		//2014-01-29 Carlos Parada Add QtyReturned
		columnNames.add(Msg.translate(Env.getCtx(), "QtyReturned"));
		//End Carlos Parada
		columnNames.add(Msg.translate(Env.getCtx(), "SGQtyLoc"));
		//columnNames.add(Msg.translate(Env.getCtx(), "Weight"));
		columnNames.add(Msg.translate(Env.getCtx(), "Qty"));
		columnNames.add(Msg.translate(Env.getCtx(), "SGQtySet"));
		columnNames.add(Msg.translate(Env.getCtx(), "SeqNo"));
		
		return columnNames;
	}
	
	/**
	 * Columnas de la tabla Stock
	 * @author Yamel Senih 07/06/2012, 16:25:36
	 * @return
	 * @return Vector<String>
	 */
	protected Vector<String> getstockColumnNames(){	
		//  Header Info
		Vector<String> columnNames = new Vector<String>();
		columnNames.add(Msg.translate(Env.getCtx(), "M_Product_ID"));
		columnNames.add(Msg.translate(Env.getCtx(), "QtyOnHand"));
		columnNames.add(Msg.translate(Env.getCtx(), "SGQtySet"));
		columnNames.add(Msg.translate(Env.getCtx(), "QtyAvailable"));
		return columnNames;
	}

	/**
	 * Establece la clase de las columnas
	 * @author Yamel Senih 07/06/2012, 16:30:59
	 * @param stockTable
	 * @return void
	 */
	protected void setStockColumnClass(MiniTable stockTable){
		int i = 0;
		stockTable.setColumnClass(i++, String.class, true);			//  1-Product
		stockTable.setColumnClass(i++, BigDecimal.class, true);		//  2-Quantity On Hand
		stockTable.setColumnClass(i++, BigDecimal.class, true);		//  3-Quantity Set
		stockTable.setColumnClass(i++, BigDecimal.class, true);		//  4-Quantity Available
		//  Table UI
		stockTable.autoSize();
	}
	
	
	/**
	 * Clases de las columnas de las Lineas de la orden
	 * @param orderLineTable
	 */
	protected void setOrderLineColumnClass(MiniTable orderLineTable){
		int i = 0;
		orderLineTable.setColumnClass(i++, Boolean.class, false);		//  0-Selection
		orderLineTable.setColumnClass(i++, String.class, true);			//  1-Warehouse
		orderLineTable.setColumnClass(i++, String.class, true);			//  2-DocumentNo
		orderLineTable.setColumnClass(i++, String.class, true);			//  3-Product
		orderLineTable.setColumnClass(i++, String.class, true);			//  4-Unit Measure Conversion
		orderLineTable.setColumnClass(i++, BigDecimal.class, true);		//  5-QtyEntered
		orderLineTable.setColumnClass(i++, String.class, true);			//  6-Unit Measure Product
		orderLineTable.setColumnClass(i++, BigDecimal.class, true);		//  7-QtyOnHand
		orderLineTable.setColumnClass(i++, BigDecimal.class, true);		//  8-QtyOrdered
		orderLineTable.setColumnClass(i++, BigDecimal.class, true);		//  9-QtyReserved
		orderLineTable.setColumnClass(i++, BigDecimal.class, true);		//  10-QtyInvoiced
		orderLineTable.setColumnClass(i++, BigDecimal.class, true);		//  11-QtyDelivered
		//2014-01-29 Carlos Parada Change Sort Columns For Add Qty Returned
		/*
		orderLineTable.setColumnClass(i++, BigDecimal.class, true);		//	12-QtyLoc
		orderLineTable.setColumnClass(i++, BigDecimal.class, true);		//  13-Qty
		orderLineTable.setColumnClass(i++, BigDecimal.class, false);		//  14-Qty Set
		orderLineTable.setColumnClass(i++, Integer.class, false);			//  15-Sequence No*/
		orderLineTable.setColumnClass(i++, BigDecimal.class, true);		//	12-QtyReturned
		orderLineTable.setColumnClass(i++, BigDecimal.class, true);		//	13-QtyLoc
		orderLineTable.setColumnClass(i++, BigDecimal.class, true);		//  14-Qty
		orderLineTable.setColumnClass(i++, BigDecimal.class, false);		//  15-Qty Set
		orderLineTable.setColumnClass(i++, Integer.class, false);			//  16-Sequence No
		// End Carlos Parada
		//	

		//  Table UI
		orderLineTable.autoSize();
	}
		
	/**
	 * Obtiene el Query para cargar las lineas de las ordenes seleccionadas
	 * @param orderTable
	 * @return
	 */
	protected StringBuffer getQueryLine(MiniTable orderTable)
	{
		log.config("");

		//  Order
		
		int rows = orderTable.getRowCount();
		StringBuffer sqlWhere = new StringBuffer("SELECT lord.M_Warehouse_ID, alm.Name Warehouse, lord.C_OrderLine_ID, " +
				"ord.DocumentNo, lord.M_Product_ID, pro.Name Product, lord.C_UOM_ID, uom.UOMSymbol, lord.QtyEntered, " +
				"pro.C_UOM_ID, uomp.UOMSymbol, " +
				"COALESCE((SELECT SUM(st.QtyOnHand) FROM M_Storage st WHERE st.M_Product_ID = lord.M_Product_ID AND st.M_Locator_ID = " + m_M_Locator_ID + "), 0) QtyOnHand, " +
				"lord.QtyOrdered, lord.QtyReserved, lord.QtyInvoiced, lord.QtyDelivered" +
				//2014-01-29 Carlos Parada Add field Returns to Load Order Windows
				",COALESCE(return.QtyReturned,0) AS QtyReturned, "+
				//End Carlos Parada
				"SUM(CASE WHEN c.DocStatus = 'CO' AND (lc.M_InOut_ID IS NULL OR de.DocStatus IN('RE', 'VO')) THEN lc.Qty ELSE 0 END) QtyLoc, " +
				"((COALESCE(lord.QtyOrdered, 0) - SUM(CASE WHEN c.DocStatus = 'CO' AND (lc.M_InOut_ID IS NULL OR de.DocStatus IN('RE', 'VO')) THEN lc.Qty ELSE 0 END))) Qty, " +
				"(COALESCE(lord.QtyOrdered, 0) - SUM(CASE WHEN c.DocStatus = 'CO' AND (lc.M_InOut_ID IS NULL OR de.DocStatus IN('RE', 'VO')) THEN lc.Qty ELSE 0 END)) QtySet " +
				
				"FROM C_Order ord " +
				"INNER JOIN C_OrderLine lord ON(lord.C_Order_ID = ord.C_Order_ID) " +
				"INNER JOIN M_Warehouse alm ON(alm.M_Warehouse_ID = lord.M_Warehouse_ID) " +
				"INNER JOIN M_Product pro ON(pro.M_Product_ID = lord.M_Product_ID) " +
				"INNER JOIN C_UOM uom ON(uom.C_UOM_ID = lord.C_UOM_ID) " +
				"INNER JOIN C_UOM uomp ON(uomp.C_UOM_ID = pro.C_UOM_ID) " +
				//"LEFT JOIN M_Storage st ON(st.M_Product_ID = lord.M_Product_ID) " +
				"LEFT JOIN CUST_LoadOrderLine lc ON(lc.C_OrderLine_ID = lord.C_OrderLine_ID) " +
				"LEFT JOIN CUST_LoadOrder c ON(c.CUST_LoadOrder_ID = lc.CUST_LoadOrder_ID) " +
				"LEFT JOIN M_InOut de ON(de.M_InOut_ID = lc.M_InOut_ID) " +
				//2014-01-29 Carlos Parada Add Returns to Load Order Windows
				"LEFT JOIN (Select colorig.C_OrderLine_ID,Sum(miol.QtyEntered) As QtyReturned From " + 
						"CUST_RecordWeight crw " +
						"Inner Join C_DocType cdt On crw.C_DocTypeTarget_ID = cdt.C_DocType_ID " +
						"Inner Join C_Order co On co.C_Order_ID = crw.M_RMA_ID " +
						"Inner Join C_OrderLine col On co.C_Order_ID = col.C_Order_ID " +
						"Inner Join C_OrderLine colorig On col.Orig_OrderLine_ID=colorig.C_OrderLine_ID " +
						"Inner Join M_InOutLine miol On miol.C_OrderLine_ID = col.C_OrderLine_ID " +
						"Inner Join M_InOut mio On mio.M_InOut_ID = miol.M_InOut_ID " +
						"Where cdt.DocBaseType = 'SRW' And " + 
						"cdt.IsReturnTrx='Y' And " + 
						"mio.DOCSTATUS In ('CO','CL') "+
						"Group By  colorig.C_OrderLine_ID) return ON return.C_OrderLine_ID= lord.C_OrderLine_ID " +
 				//End Carlos Parada
				"WHERE lord.M_Product_ID IS NOT NULL " +
				//"AND st.M_Locator_ID = " + m_M_Locator_ID + " " +
				"AND ord.C_Order_ID IN(0");
		m_RowsSelected = 0;
		for (int i = 0; i < rows; i++) {
			if (((Boolean)orderTable.getValueAt(i, 0)).booleanValue()) {
				int ID = ((KeyNamePair)orderTable.getValueAt(i, ORDER)).getKey();
				sqlWhere.append(",");
				sqlWhere.append(ID);
				m_RowsSelected ++;
			}
		}
		sqlWhere.append(") GROUP BY lord.M_Warehouse_ID, lord.C_Order_ID, lord.C_OrderLine_ID, " +
				"alm.Name, ord.DocumentNo, lord.M_Product_ID, pro.Name, lord.C_UOM_ID, uom.UOMSymbol, lord.QtyEntered, " +
				"pro.C_UOM_ID, uomp.UOMSymbol, lord.QtyOrdered, lord.QtyReserved, lord.QtyDelivered, " +
				"lord.QtyInvoiced,return.QtyReturned " +
				"HAVING (COALESCE(lord.QtyOrdered, 0) - SUM(CASE WHEN c.DocStatus = 'CO' THEN lc.Qty ELSE 0 END)) > 0 " +
				"ORDER BY lord.C_Order_ID ASC");
		log.config("SQL Line Order=" + sqlWhere.toString());
		//System.out.println("LoadOrder.getQueryLine() " + sqlWhere);
		return sqlWhere;
	}
	
	/**
	 * Genera La orden de Carga con sus lineas.
	 * @author Yamel Senih 18/03/2012, 00:22:14
	 * @param orderLineTable
	 * @param dateDoc
	 * @param shipDate
	 * @param trx
	 * @return
	 * @return String
	 * @throws Exception 
	 */
	protected String generateLoatOrder(MiniTable orderLineTable, Timestamp dateDoc, Timestamp shipDate, Trx trx) throws Exception{
		if(viewResultPeriod()){
			int m_gen = 0;
			int rows = orderLineTable.getRowCount();
			MCUSTLoadOrder loadOrder = new MCUSTLoadOrder(Env.getCtx(), 0, trx);
			MCUSTLoadOrderLine lorder = null;
			//	Org Info
			MOrgInfo orgInfo = null;
			
			String DocumentNo = "0";
			
			orgInfo = MOrgInfo.get(Env.getCtx(), m_AD_Org_ID, trx);
			
			BigDecimal totalWeight = Env.ZERO;
			loadOrder.setAD_Org_ID(m_AD_Org_ID);
			loadOrder.setM_Shipper_ID(m_M_Shipper_ID);
			loadOrder.setMotorist_ID(m_Motorist_ID);
			loadOrder.setVehicle_ID(m_Vehicle_ID);
			loadOrder.setDateDoc(dateDoc);
			//loadOrder.setShipDate(shipDate);
			loadOrder.setC_DocTypeOrder_ID(m_C_DocTypeOrder_ID);
			loadOrder.setIsInternalLoad(m_IsInternalLoad);
			loadOrder.setIsBulk(m_IsBulk);
			loadOrder.setIsRecordWeight(orgInfo.get_ValueAsBoolean("IsRecordWeight"));
			loadOrder.setCapacity(capacity);
			loadOrder.setVehicle_UOM_ID(m_Vehicle_UOM_ID);
			loadOrder.setWork_UOM_ID(m_Work_UOM_ID);
			loadOrder.setM_Warehouse_ID(m_M_Warehouse_ID);
			loadOrder.setM_Locator_ID(m_M_Locator_ID);
			loadOrder.setDocStatus(MCUSTLoadOrder.DOCSTATUS_Drafted);
			loadOrder.setDocAction(MCUSTLoadOrder.DOCACTION_Complete);
			
			if(loadOrder.save()){
				int m_CUST_LoadOrder_ID = loadOrder.getCUST_LoadOrder_ID();
				//System.err.println("EPALE " + m_CUST_LoadOrder_ID);
				DocumentNo = loadOrder.getDocumentNo();
				for (int i = 0; i < rows; i++) {
					if (((Boolean)orderLineTable.getValueAt(i, 0)).booleanValue()) {
						int m_C_OrderLine_ID = ((KeyNamePair)orderLineTable.getValueAt(i, ORDER_LINE)).getKey();
						int m_M_Product_ID = ((KeyNamePair)orderLineTable.getValueAt(i, OL_PRODUCT)).getKey();
						int m_C_UOM_ID = ((KeyNamePair)orderLineTable.getValueAt(i, OL_UOM_CONVERSION)).getKey();
						BigDecimal qty = (BigDecimal) orderLineTable.getValueAt(i, OL_QTY);
						BigDecimal seqNo = new BigDecimal((Integer) orderLineTable.getValueAt(i, OL_SEQNO));
						BigDecimal qtySet = (BigDecimal) orderLineTable.getValueAt(i, OL_QTY_SET);
						lorder = new MCUSTLoadOrderLine(Env.getCtx(), 0, trx);
						lorder.setAD_Org_ID(m_AD_Org_ID);
						lorder.setCUST_LoadOrder_ID(m_CUST_LoadOrder_ID);
						lorder.setC_OrderLine_ID(m_C_OrderLine_ID);
						lorder.setM_Product_ID(m_M_Product_ID);
						lorder.setC_UOM_ID(m_C_UOM_ID);
						lorder.setQty(qty);
						lorder.setSeqNo(seqNo);
						lorder.setWeight(qtySet);
						totalWeight = totalWeight.add(qtySet);
						
						addQuery(m_C_OrderLine_ID, qty);
						
						if(lorder.save(trx)){
							m_gen ++;
						} else {
							throw new CompiereSystemException("@CUST_LoadOrderLine_ID@"); 
						}
					}
				}
				
				loadOrder.setTotalWeight(totalWeight);
				
				String resultQuery = viewResult();
				if(resultQuery != null && resultQuery.length() > 0){
					throw new CompiereSystemException(Msg.translate(Env.getCtx(), "SGErrorsProcess") 
							+ "\n" + resultQuery);
				}
				
				loadOrder.processIt(MCUSTLoadOrder.DOCACTION_Complete);
				
				if(!loadOrder.save(trx)){
					throw new CompiereSystemException("@CUST_LoadOrder_ID@");
				}
			} else {
				throw new CompiereSystemException("@CUST_LoadOrder_ID@"); 
			}
			return Msg.translate(Env.getCtx(), "SGLoadOrderGenerate") + " = [" + DocumentNo + "] || " +
			Msg.translate(Env.getCtx(), "SGLoadOrderLineGenerate") + " = [" + m_gen + "]";
		} else {
			throw new CompiereSystemException("@C_Period_ID@" 
					+ " " +Msg.translate(Env.getCtx(), "SGClosed"));
		}
	}
	
	/**
	 * Evalua el resultado de los querys generados para validar concurrencia
	 * @return
	 * @throws SQLException 
	 */
	private String viewResult() throws SQLException{
		//System.err.println(m_QueryAdd);
		String m_Result = null;
		StringBuffer m_SB_Add = new StringBuffer();
		PreparedStatement pstmt = DB.prepareStatement(m_QueryAdd.toString(), null);
		ResultSet rs = pstmt.executeQuery();
		while (rs.next()){
			m_SB_Add.append(Msg.translate(Env.getCtx(), "C_Order_ID") + " = " + rs.getString("OrderName") + " ");
			m_SB_Add.append(Msg.translate(Env.getCtx(), "M_Product_ID") + " = " + rs.getString("ProductName") + " ");
			m_SB_Add.append(Msg.translate(Env.getCtx(), "QtyAvailable") + " = " + rs.getBigDecimal("QtyAvailable") + " ");
			m_SB_Add.append(Msg.translate(Env.getCtx(), "QtyLoc") + " = " + rs.getBigDecimal("QtyLoc") + "\n");
		}
		rs.close();
		pstmt.close();
		m_Result = m_SB_Add.toString();
		m_QueryAdd = new StringBuffer();
		return m_Result;
	}
	
	/**
	 * Agrega un Query a una UNION de Querys para consultar
	 * @param m_C_OrderLine_ID
	 * @param qty
	 */
	private void addQuery(int m_C_OrderLine_ID, BigDecimal qty){
		String queryTemp = new String("SELECT ord.DocumentNo OrderName, pr.Name ProductName, " +
				"COALESCE(lord.QtyOrdered, 0) - " +
				//"COALESCE(lord.QtyDelivered, 0) - " +
				"SUM(CASE WHEN c.DocStatus = 'CO' THEN lc.Qty ELSE 0 END)+Coalesce(return.QtyReturned,0) QtyAvailable, " +
				qty.doubleValue() + " QtyLoc " +
				"FROM C_Order ord " +
				"INNER JOIN C_OrderLine lord ON(lord.C_Order_ID = ord.C_Order_ID) " +
				"INNER JOIN M_Product pr ON(pr.M_Product_ID = lord.M_Product_ID) " +
				"LEFT JOIN CUST_LoadOrderLine lc ON(lord.C_OrderLine_ID = lc.C_OrderLine_ID) " +
				"LEFT JOIN CUST_LoadOrder c ON(c.CUST_LoadOrder_ID = lc.CUST_LoadOrder_ID) " +
				//2014-01-29 Carlos Parada Add Qty Returned
				"LEFT JOIN (Select colorig.C_OrderLine_ID,Sum(miol.QtyEntered) As QtyReturned From " + 
				"CUST_RecordWeight crw " +
				"Inner Join C_DocType cdt On crw.C_DocTypeTarget_ID = cdt.C_DocType_ID " +
				"Inner Join C_Order co On co.C_Order_ID = crw.M_RMA_ID " +
				"Inner Join C_OrderLine col On co.C_Order_ID = col.C_Order_ID " +
				"Inner Join C_OrderLine colorig On col.Orig_OrderLine_ID=colorig.C_OrderLine_ID " +
				"Inner Join M_InOutLine miol On miol.C_OrderLine_ID = col.C_OrderLine_ID " +
				"Inner Join M_InOut mio On mio.M_InOut_ID = miol.M_InOut_ID " +
				"Where cdt.DocBaseType = 'SRW' And " + 
				"cdt.IsReturnTrx='Y' And " + 
				"mio.DOCSTATUS In ('CO','CL') "+
				"Group By  colorig.C_OrderLine_ID) return ON return.C_OrderLine_ID= lord.C_OrderLine_ID " +
				//End Carlos Parada
				"WHERE lord.M_Product_ID IS NOT NULL " +
				"AND lord.C_OrderLine_ID = " + 
				m_C_OrderLine_ID + 
				" " +
				"GROUP BY ord.DocumentNo, pr.Name, lord.C_OrderLine_ID, lord.QtyOrdered ,return.QtyReturned " +
				"HAVING (COALESCE(lord.QtyOrdered, 0) - " +
				"SUM(CASE WHEN c.DocStatus = 'CO' THEN lc.Qty ELSE 0 END)) + Coalesce(return.QtyReturned,0) < " + 
				qty.doubleValue() + 
				" ");
		if(m_QueryAdd.length() > 0){
			m_QueryAdd.append(" UNION ALL ");
		}
		m_QueryAdd.append(queryTemp);
		
	}
	
	/**
	 * Carga los datos del Conductor del Vehículo 
	 * que pertenezca al Transportista seleccionado
	 * @return
	 */
	protected ArrayList<KeyNamePair> getDataDriver(){
		
		String sql = "SELECT c.CUST_Motorist_Vehicle_ID, c.Value || ' - ' || c.Name " +
				"FROM CUST_Motorist_Vehicle c " +
				"WHERE c.IsActive = 'Y' " +
				"AND c.RecordType = 'M' " +
				"AND c.M_Shipper_ID = ? " +
				"AND c.CUST_Motorist_Vehicle_ID NOT IN" +
				"(SELECT Motorist_ID " +
				"FROM CUST_LoadOrder " +
				"WHERE DocStatus = 'CO' AND IsMotoristReleased = 'N') " +
				"ORDER BY c.Value, c.Name";		
		return getData(m_M_Shipper_ID, sql, true);
	}
	
	/**
	 * Obtiene los datos de los Documentos de Orden de Venta
	 * @author Yamel Senih 18/03/2012, 01:24:48
	 * @return
	 * @return ArrayList<KeyNamePair>
	 */
	protected ArrayList<KeyNamePair> getDataDocumentOrder(){
		
		String sql = "SELECT doc.C_DocType_ID, TRIM(doc.Name) " +
				"FROM C_DocType doc " +
				"WHERE doc.DocBaseType IN('SOO') " +
				"AND doc.IsSOTrx='Y' " +
				"AND doc.IsActive='Y' " +
				"AND doc.AD_Client_ID = ? " +
				"AND COALESCE(doc.DocSubTypeSO,' ')<>'RM' " +
				"ORDER BY doc.Name";		
		return getData(Env.getCtx().getAD_Client_ID(), sql, true);
	}
	
	/**
	 * Carga los datos del Vehiculo
	 * @return
	 */
	protected ArrayList<KeyNamePair> getDataCar(){
		
		String sql = "SELECT v.CUST_Motorist_Vehicle_ID, v.Value || ' - ' || v.Name " +
				"FROM CUST_Motorist_Vehicle v " +
				"WHERE v.IsActive = 'Y' " +
				"AND v.RecordType = 'V' " +
				"AND v.M_Shipper_ID = ? " +
				"AND v.CUST_Motorist_Vehicle_ID NOT IN " +
				"(SELECT Vehicle_ID " +
				"FROM CUST_LoadOrder " +
				"WHERE DocStatus = 'CO' AND IsVehicleReleased = 'N') " +
				"ORDER BY v.Value, v.Name";
		return getData(m_M_Shipper_ID, sql, true);
	}
	
	
	/**
	 * Carga los datos del Almacen
	 * @author Yamel Senih 30/03/2012, 10:34:38
	 * @return
	 * @return ArrayList<KeyNamePair>
	 */
	protected ArrayList<KeyNamePair> getDataWarehouse(){
		
		String sql = "SELECT w.M_Warehouse_ID, w.Name " + //	 || ' - ' || v.Nombre 
				"FROM M_Warehouse w " +
				"WHERE w.IsActive = 'Y' " +
				"AND w.AD_Org_ID = ? " +
				"ORDER BY w.Name";
		return getData(m_AD_Org_ID, sql, true);
	}
	
	/**
	 * Carga los datos del localizador
	 * @author Yamel Senih 30/03/2012, 10:37:27
	 * @return
	 * @return ArrayList<KeyNamePair>
	 */
	protected ArrayList<KeyNamePair> getDataLocator(){
		
		String sql = "SELECT l.M_Locator_ID, l.Value " + //	 || ' - ' || v.Nombre 
				"FROM M_Locator l " +
				"WHERE l.IsActive = 'Y' " +
				"AND l.M_Warehouse_ID = ? " +
				"ORDER BY l.Value";
		return getData(m_M_Warehouse_ID, sql, true);
	}
	
	/**
	 * Carga los datos del Localizador destino
	 * @author Yamel Senih 03/04/2012, 17:11:18
	 * @return
	 * @return ArrayList<KeyNamePair>
	 */
	protected ArrayList<KeyNamePair> getDataLocatorTo(){
		
		String sql = "SELECT l.M_Locator_ID, l.Value " + //	 || ' - ' || v.Nombre 
				"FROM M_Locator l " +
				"WHERE l.IsActive = 'Y' " +
				"AND (l.M_Warehouse_ID = ? " +
				"OR 1 = 1) " +
				"ORDER BY l.Value";
		return getData(m_M_Warehouse_ID, sql, true);
	}
	
	/**
	 * Carga un Combo Box a partir de un arreglo de datos
	 * @author Yamel Senih 30/03/2012, 11:00:38
	 * @param comboSearch
	 * @param data
	 * @param m_ID
	 * @return int
	 */
	protected int loadCombo(CComboBox comboSearch, ArrayList<KeyNamePair> data) {
		comboSearch.removeAllItems();
		int m_ID = 0;
		for(KeyNamePair pp : data) {
			comboSearch.addItem(pp);
		}
		
		if (comboSearch.getItemCount() != 0) {
			comboSearch.setSelectedIndex(0);
			KeyNamePair pp = (KeyNamePair) comboSearch.getSelectedItem();
			m_ID = (pp != null? pp.getKey(): 0);
		}
		return m_ID;
	}
	
	/**
	 * Carga datos de un sql para Conductores y Vehiculos
	 * @param m_M_Shipper_ID
	 * @param sql
	 * @param mandatory
	 * @return
	 */
	private ArrayList<KeyNamePair> getData(int m_M_Shipper_ID, String sql, boolean mandatory){
		ArrayList<KeyNamePair> data = new ArrayList<KeyNamePair>();
		
		log.config("getData");
		
		try	{
			PreparedStatement pstmt = DB.prepareStatement(sql, null);
			if(mandatory)
				pstmt.setInt(1, m_M_Shipper_ID);
			ResultSet rs = pstmt.executeQuery();
			//
			while (rs.next()) {
				KeyNamePair pp = new KeyNamePair(rs.getInt(1), rs.getString(2));
				data.add(pp);
			}
			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			log.log(Level.SEVERE, sql, e);
		}
		return data;
	}
	
	/**
	 * Verifica que un numero de secuencia no exista en la tabla
	 * @param orderLineTable
	 * @param seqNo
	 * @return
	 */
	public boolean exists_seqNo(MiniTable orderLineTable, int row, int seqNo){
		log.info("exists_seqNo");
		int rows = orderLineTable.getRowCount();
		int seqNoTable = 0;
		for (int i = 0; i < rows; i++) {
			if (((Boolean)orderLineTable.getValueAt(i, SELECT)).booleanValue() 
					&& i != row) {
				seqNoTable = (Integer) orderLineTable.getValueAt(i, OL_SEQNO);
				if(seqNo == seqNoTable){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Carga los valores seleccionados de la tabla en un Buffer
	 * @param orderLineTable
	 */
	public void loadBuffer(MiniTable orderLineTable){
		log.info("Load Buffer");
		int rows = orderLineTable.getRowCount();
		int m_C_OrderLine_ID = 0;
		BigDecimal qty = Env.ZERO;
		Integer seqNo = 0;
		m_BufferSelect = new Vector<BufferTableSelect>();
		
		//stockModel = new DefaultTableModel(null, getstockColumnNames());
		
		for (int i = 0; i < rows; i++) {
			if (((Boolean)orderLineTable.getValueAt(i, SELECT)).booleanValue()) {
				m_C_OrderLine_ID = ((KeyNamePair)orderLineTable.getValueAt(i, ORDER_LINE)).getKey();
				qty = (BigDecimal)orderLineTable.getValueAt(i, OL_QTY_SET);
				seqNo = (Integer)orderLineTable.getValueAt(i, OL_SEQNO);
				m_BufferSelect.addElement(
						new BufferTableSelect(m_C_OrderLine_ID, qty, seqNo));
				//loadProductsStock(orderLineTable, i, true);
			}
		}
		//stockTable.setModel(stockModel);
		//stockTable.autoSize();
		//setStockColumnClass(stockTable);
	}
	
	/**
	 * Recarga los datos de la tabla de valores acumulados
	 * @author Yamel Senih 08/06/2012, 11:39:44
	 * @param orderLineTable
	 * @return void
	 */
	public void loadStockWarehouse(MiniTable orderLineTable){
		
		log.info("Load StockWarehouse");
		int rows = orderLineTable.getRowCount();
		stockModel = new DefaultTableModel(null, getstockColumnNames());
		
		for (int i = 0; i < rows; i++) {
			if (((Boolean)orderLineTable.getValueAt(i, SELECT)).booleanValue()) {
				loadProductsStock(orderLineTable, i, true);
			}
		}
		stockTable.setModel(stockModel);
		stockTable.autoSize();
		setStockColumnClass(stockTable);
	}
	
	/**
	 * Verifica si existe el producto en una tabla
	 * @author Yamel Senih 08/06/2012, 10:08:57
	 * @param stockTable
	 * @param Product_ID
	 * @return
	 * @return int
	 */
	private int existProductStock(int Product_ID){
		for(int i = 0; i < stockModel.getRowCount(); i++){
			if(((KeyNamePair) stockModel.getValueAt(i, SW_PRODUCT)).getKey() == Product_ID){
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Carga los productos en la tabla de Stock y acumulado
	 * @author Yamel Senih 08/06/2012, 10:56:29
	 * @param orderLineTable
	 * @param row
	 * @param isSelected
	 * @return void
	 */
	protected void loadProductsStock(MiniTable orderLineTable, int row, boolean isSelected){
		KeyNamePair product = (KeyNamePair) orderLineTable.getValueAt(row, OL_PRODUCT);
		BigDecimal qtyOnHand = (BigDecimal) orderLineTable.getValueAt(row, OL_QTY_ONDHAND);
		BigDecimal qtySet = (BigDecimal) orderLineTable.getValueAt(row, OL_QTY_SET);
		
		/*System.out.println("LoadOrder.loadProductsStock() product " + product.getName() + " qtySet " + 
				qtySet);*/
		
		int pos = existProductStock(product.getKey());
		
		if(pos > -1){
			BigDecimal qtySetOld = (BigDecimal) stockModel.getValueAt(pos, SW_QTYSET);
			
			//System.out.println(" qtySetOld " + qtySetOld);
			//	Negate
			if(!isSelected)
				qtySet = qtySet.negate();
			
			qtySet = qtySet.add(qtySetOld);
			
			stockModel.setValueAt(qtyOnHand, pos, SW_QTYONHAND);
			stockModel.setValueAt(qtySet, pos, SW_QTYSET);
			stockModel.setValueAt(qtyOnHand.subtract(qtySet), pos, SW_QTYAVAILABLE);
		} else if(isSelected){
			Vector<Object> line = new Vector<Object>();
			line.add(product);
			line.add(qtyOnHand);
			line.add(qtySet);
			line.add(qtyOnHand.subtract(qtySet));
			stockModel.addRow(line);
		}
	}
	
	/**
	 * Verifica si existe un ID en el Vector
	 * @param m_Record_ID
	 * @return
	 */
	private BufferTableSelect isSelect(int m_Record_ID){
		log.info("Is Select " + m_Record_ID);
		if(m_BufferSelect != null){
			for(int i = 0; i < m_BufferSelect.size(); i++){
				if(m_BufferSelect.get(i).getRecord_ID() == m_Record_ID){
					return m_BufferSelect.get(i);
				}
			}	
		}
		return null;
	}
	
	/**
	 * Establece los valores cargados en el buffer en la tabla
	 * @param orderLineTable
	 */
	protected void setValueFromBuffer(MiniTable orderLineTable){
		log.info("Set Value From Buffer");
		if(m_BufferSelect != null){
			int rows = orderLineTable.getRowCount();
			int m_C_OrderLine_ID = 0;
			BufferTableSelect bts = null;
			for (int i = 0; i < rows; i++) {
				m_C_OrderLine_ID = ((KeyNamePair)orderLineTable.getValueAt(i, ORDER_LINE)).getKey();
				bts = isSelect(m_C_OrderLine_ID);
				if(bts != null){
					orderLineTable.setValueAt(true, i, SELECT);
					orderLineTable.setValueAt(bts.getQty(), i, OL_QTY_SET);
					orderLineTable.setValueAt(bts.getSeqNo(), i, OL_SEQNO);
				}
			}	
		}
	}
	
	/** 
	 * Verifica si el periodo esta abierto
	 * @return
	 */
	private boolean viewResultPeriod(){
		String sql = new String("SELECT p.* " +
				"FROM C_Period p " +
				"INNER JOIN C_PeriodControl pc ON(pc.C_Period_ID = p.C_Period_ID) " +
				"WHERE pc.DocBaseType = 'SOO' " +
				"AND pc.PeriodStatus = 'O' " +
				"AND pc.AD_Client_ID = ? " +
				"AND p.StartDate <= ? AND p.EndDate >= ?");
		
		log.fine("viewResultPeriod SQL = " + sql);
		try {
			
			Timestamp date = new Timestamp(Env.getCtx().getContextAsTime("#Date"));
			
			PreparedStatement pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, Env.getCtx().getAD_Client_ID());
			pstmt.setTimestamp(2, date);
			pstmt.setTimestamp(3, date);
			ResultSet rs = pstmt.executeQuery();
			if(rs.next()){
				return true;
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, null, e);
		} 
		return false;
	}
	
	/**
	 * Establece los valores de los campos dependientes del Tipo de Documento
	 * @author Yamel Senih 18/03/2012, 00:36:32
	 * @param trx
	 * @return void
	 */
	protected void setValueDocType(Trx trx) {
		if(m_C_DocTypeOrder_ID != 0){
			MDocType docType = new MDocType(Env.getCtx(), m_C_DocTypeOrder_ID, trx);
			m_IsInternalLoad = docType.get_ValueAsBoolean("IsInternalLoad");
			m_IsBulk = docType.get_ValueAsBoolean("IsBulk");
		} else {
			m_IsInternalLoad = false;
			m_IsBulk = false;
		}
	}
	
	/**
	 * Verifica que ya esté seleccionado una fila de la table
	 * @author Yamel Senih 18/03/2012, 12:56:59
	 * @param table
	 * @return
	 * @return boolean
	 */
	protected boolean moreOneSelect(MiniTable table) {
		int rows = table.getRowCount();
		int cont = 0;
		for (int i = 0; i < rows; i++) {
			if (((Boolean)table.getValueAt(i, SELECT)).booleanValue()) {
				cont++;
				if(cont > 1){
					return true;
				}
			}
		}
		return false;
	}
	
}