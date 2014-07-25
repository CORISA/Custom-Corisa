/**
 * 
 */
package org.custom.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.model.MBPartner;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MUOM;
import org.compiere.model.MUOMConversion;
import org.compiere.model.X_C_Order;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.CompiereSystemException;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.custom.model.MCUSTLoadOrder;
import org.custom.model.MCUSTLoadOrderLine;
import org.custom.model.MCUSTRecordWeight;

/**
 * @author info-analista2
 *
 */
public class GenerateInvoiceFromLoadOrder extends SvrProcess {
	/**	Manual Selection		*/
	private boolean 		p_Selection = false;
	/**	Load Order ID		*/
	//private int 			p_CUST_LoadOrder_ID = 0;
	/**	Sales Order			*/
	private int			m_C_Order_ID = 0;
	/**	Load Order			*/
	private MCUSTLoadOrder 	m_LoadOrder = null;
	/**	Created				*/
	private int 			m_created = 0;
	/** Received			*/
	private MInvoice 		invoice = null;
	/**	Weight Register		*/
	private MCUSTRecordWeight		m_WeightRegister = null;
	
	/**	Date Inveoice		*/
	private Timestamp		p_DateInvoiced;
	/**	Business Partner	*/
	private int			p_C_BPartner_ID = 0;
	/**	Business Partner Location	*/
	private int			p_C_BPartner_Location_ID = 0;
	/**	Document Action		*/
	@SuppressWarnings("unused")
	private	 String			p_DocAction = "CO";
	/**	Generate Lines		*/
	private int 			m_GenerateLines = 0;
	
	private String 			trxName = null;
	private Trx 			trx = null;
	/* (non-Javadoc)
	 * @see org.compiere.process.SvrProcess#prepare()
	 */
	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if (para.getParameter() == null)
				;
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para.getParameterAsInt();
			else if (name.equals("C_BPartner_Location_ID"))
				p_C_BPartner_Location_ID = para.getParameterAsInt();
			else if (name.equals("Selection"))
				p_Selection = "Y".equals(para.getParameter());
			else if (name.equals("DateInvoiced"))
				p_DateInvoiced = (Timestamp)para.getParameter();
			else if (name.equals("DocAction"))
				p_DocAction = (String)para.getParameter();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		
		trxName = new String("GM");
		trx = Trx.get(trxName);
	}
	
	/* (non-Javadoc)
	 * @see org.compiere.process.SvrProcess#doIt()
	 */
	@Override
	protected String doIt(){
		String out = null;
		
		StringBuffer sql = new StringBuffer("SELECT ord.C_Order_ID,lo.CUST_LoadOrder_ID  " + 
				"       FROM CUST_LoadOrder lo" + 
				"		INNER JOIN CUST_LoadOrderLine lol ON(lol.CUST_LoadOrder_ID = lo.CUST_LoadOrder_ID)" +
				"		INNER JOIN C_OrderLine lov ON(lov.C_OrderLine_ID = lol.C_OrderLine_ID)" + 
				"       INNER JOIN C_Order ord ON(ord.C_Order_ID = lov.C_Order_ID)" +
				"		LEFT JOIN C_Invoice inc ON(inc.C_Invoice_ID = lol.C_Invoice_ID)" + 
				"		WHERE lo.DocStatus IN('CO', 'CL') AND lo.IsInternalLoad = 'N' " +
				"		AND (inc.C_Invoice_ID IS NULL OR inc.DocStatus IN('VO', 'RE'))" + 
				"		AND lo.IsSelected = ? ");
		
		if(p_C_BPartner_ID != 0)
			sql.append("AND ord.C_BPartner_ID = " + p_C_BPartner_ID + " ");
		
		sql.append("GROUP BY ord.C_Order_ID,lo.CUST_LoadOrder_ID");
		
		PreparedStatement pstmt = null;
		try	{
			pstmt = DB.prepareStatement (sql.toString(), trx);
			int index = 1;
			pstmt.setString(index++, p_Selection? "Y": "N");
			
			out = generateInvoices(pstmt);
			trx.commit();
		}catch (Exception e) {
			trx.rollback();
			log.log(Level.SEVERE, sql.toString(), e);
			out = e.getMessage();
		}
		
		return out;
	}
	
	/**
	 * @author Yamel Senih, 10/05/2011 11:14
	 * Genera las Entregas de materiales
	 * @param pstmt
	 * @return
	 * @throws SQLException 
	 */
	private String generateInvoices(PreparedStatement pstmt) throws Exception{
		int m_CUST_LoadOrder_ID=0;
		ResultSet rs;
		rs = pstmt.executeQuery ();
		//findWeightRegister();
		while (rs.next ()){
			m_C_Order_ID = rs.getInt("C_Order_ID");
			m_CUST_LoadOrder_ID = rs.getInt("CUST_LoadOrder_ID");
			MOrder orden = new MOrder (getCtx(), m_C_Order_ID, trx);
			if(X_C_Order.DOCSTATUS_Completed.equals(orden.getDocStatus())){
				procOrden(orden,m_CUST_LoadOrder_ID);
			}
		}

		if (pstmt != null)
				pstmt.close ();
			pstmt = null;
			
		completeInvoice();
			
		return "@Created@ = " + m_created;
	}
	
	/**
	 * Crea las Entregas una a una a partir de una Orden de Venta
	 * @return
	 */
	private void procOrden(MOrder orden,int m_CUST_LoadOrder_ID) throws Exception {
		
		/*
		 * Crea el encabezado de la Entrega a partir de la orden de Venta
		 */
		if(invoice == null){
			invoice = new MInvoice(orden, 0, p_DateInvoiced);
			
			
			
			MBPartner m_bpartner = MBPartner.get(getCtx(), orden.getC_BPartner_ID());
			
			invoice.setAD_Org_ID(orden.getAD_Org_ID());
			//invoice.setM_Warehouse_ID(orden.getM_Warehouse_ID());
			invoice.setC_Activity_ID(orden.getC_Activity_ID());
			invoice.setDateInvoiced(p_DateInvoiced);
			invoice.setBPartner(m_bpartner);
			invoice.setSalesRep_ID(orden.getSalesRep_ID());
			invoice.setM_PriceList_ID(orden.getM_PriceList_ID());		
			invoice.setDateAcct(p_DateInvoiced);
			//invoice.set_Value("CUSTLoadOrder_ID", value)
			if(p_C_BPartner_Location_ID != 0)
				invoice.setC_BPartner_Location_ID(p_C_BPartner_Location_ID);
			else
			invoice.setC_BPartner_Location_ID(orden.getBill_Location_ID());
		}
		
		if(invoice.save()){
			StringBuffer sql = new StringBuffer("SELECT loc.* FROM CUST_LoadOrder oc " +
					"INNER JOIN CUST_LoadOrderLine loc ON(loc.CUST_LoadOrder_ID = oc.CUST_LoadOrder_ID) " +
					"INNER JOIN C_OrderLine lov ON(lov.C_OrderLine_ID = loc.C_OrderLine_ID) " +
					"WHERE oc.Processed = 'Y' " +
					"AND lov.C_Order_ID = ? " +
					"And oc.CUST_LOADORDER_ID=? ");
					
			PreparedStatement pstmt = null;
			pstmt = DB.prepareStatement (sql.toString(), trx);
			pstmt.setInt(1, orden.getC_Order_ID());
			pstmt.setInt(2, m_CUST_LoadOrder_ID);
			ResultSet rs;
			rs = pstmt.executeQuery ();
			while (rs.next ()){
				
				MCUSTLoadOrderLine loCarga = new MCUSTLoadOrderLine(getCtx(), rs, trx);
				
				m_LoadOrder = new MCUSTLoadOrder(getCtx(), loCarga.getCUST_LoadOrder_ID(), trx);
				
				int m_C_OrderLine_ID = loCarga.getC_OrderLine_ID();
				
				BigDecimal toInvoice = loCarga.getQty();
				
				if(m_C_OrderLine_ID != 0){
					String sqlReturn = new String("SELECT del.MovementQty - SUM(COALESCE(rel.QtyOrdered, 0)) QtyAvailable " +
							"FROM M_InOut de " +
							"JOIN M_InOutLine del ON del.M_InOut_ID = de.M_InOut_ID " +
							"JOIN C_OrderLine ol ON ol.C_OrderLine_ID = del.C_OrderLine_ID " +
							"LEFT JOIN C_OrderLine rel ON rel.Orig_InOutLine_ID = del.M_InOutLine_ID " +
							"WHERE de.DocStatus IN('CO', 'CL') " +
							"AND de.MovementType = 'C-' " +
							"AND del.C_OrderLine_ID = ? " + 
							"AND EXISTS (SELECT 1 FROM CUST_LOADORDERLINE WHERE del.M_InOut_ID=CUST_LOADORDERLINE.M_InOut_ID " +//Changed by Jorge Colmenarez And Carlos Parada 2014-04-28
							"AND CUST_LOADORDERLINE.CUST_LOADORDERLINE_ID = "+loCarga.getCUST_LoadOrderLine_ID()+")" + //Add Exists for that Query return only the line affected.
							"GROUP BY del.M_InOutLine_ID, del.MovementQty");
					
					toInvoice = DB.getSQLValueBD(trx, sqlReturn, m_C_OrderLine_ID);
				}
				
				System.out.println("m_C_OrderLine_ID " + m_C_OrderLine_ID + " toInvoice " + toInvoice);
				
				if(toInvoice != null && !toInvoice.equals(Env.ZERO)){
					MOrderLine loVenta = new MOrderLine(getCtx(), loCarga.getC_OrderLine_ID(), trx);
					MInvoiceLine invoiceLine = new MInvoiceLine(invoice);
					invoiceLine.setOrderLine(loVenta);
					invoiceLine.setC_Activity_ID(loVenta.getC_Activity_ID());
					
					//BigDecimal toInvoice = loCarga.getQty();
					MProduct pr = new MProduct(getCtx(),loCarga.getM_Product_ID(),trx);
					if(m_WeightRegister != null && m_LoadOrder.isBulk()){
						BigDecimal netWeight = (BigDecimal) m_WeightRegister.getNetWeight();
						int m_C_UOM_ID = m_WeightRegister.getC_UOM_ID();
						
						int m_C_UOM_To_ID = pr.getC_UOM_ID();
						//	Correcciones de Error en la Conversion Usando el Cache
						BigDecimal rate = MUOMConversion.getRate(m_C_UOM_ID, m_C_UOM_To_ID);//Rate(getCtx(), m_C_UOM_ID, m_C_UOM_To_ID);
						BigDecimal qtyWeightRegister = Env.ZERO;
						if(rate != null){
							qtyWeightRegister = netWeight.multiply(rate);
							if(toInvoice.compareTo(qtyWeightRegister) >= 0){
								toInvoice = qtyWeightRegister;
							} else {
								throw new CompiereSystemException(Msg.translate(getCtx(), "SGQtyWRegisterHi") + " " 
										+ Msg.translate(getCtx(), "QtyOrdered") + " " 
										+ toInvoice.doubleValue() + " " 
										+ Msg.translate(getCtx(), "SGQtyWeightRegister") + " " 
										+ qtyWeightRegister.doubleValue()
										);
							}
						} else {
							MUOM m_uom = new MUOM(getCtx(), m_C_UOM_ID, trx);
							MUOM m_uom_to = new MUOM(getCtx(), m_C_UOM_To_ID, trx);
							throw new CompiereSystemException(Msg.translate(getCtx(), "SGNotConversion") + " " 
									+ Msg.translate(getCtx(), "of") + " "
									+ m_uom.getName() + " " 
									+ Msg.translate(getCtx(), "to") + " " 
									+ m_uom_to.getName()
									);
						}
					} else if(m_WeightRegister == null && m_LoadOrder.isBulk()){
						throw new CompiereSystemException(Msg.translate(getCtx(), "SGNotRecordWeight"));
					}
					
					BigDecimal rate = MUOMConversion.getProductRateTo(Env.getCtx(), pr.getM_Product_ID(), m_LoadOrder.getWork_UOM_ID());
					
					invoiceLine.setQty(toInvoice.multiply(rate));	//	Correct UOM
					invoiceLine.setPriceActual(invoiceLine.getPriceActual());
					invoiceLine.setPriceEntered(invoiceLine.getPriceEntered().divide(rate));
					invoiceLine.setC_UOM_ID(m_LoadOrder.getWork_UOM_ID());
					
					//invoiceLine.setOrderLine(loVenta);
					
					invoiceLine.setQtyEntered(toInvoice.multiply(rate));
					
					invoiceLine.setQtyInvoiced(toInvoice);
					/*BigDecimal qtyEntered = toInvoice;
					
					invoiceLine.setQty(toInvoice);	//	Correct UOM
					
					if (loVenta.getQtyEntered().compareTo(loVenta.getQtyOrdered()) != 0)
						qtyEntered = toInvoice
								.multiply(loVenta.getQtyEntered())
								.divide(loVenta.getQtyOrdered(), 12, BigDecimal.ROUND_HALF_UP);
					
					invoiceLine.setOrderLine(loVenta);
					invoiceLine.setQtyEntered(qtyEntered);
					
					invoiceLine.setQtyInvoiced(toInvoice);*/

					if(!invoiceLine.save())
						throw new CompiereSystemException("@SaveError@ ");// + invoiceLine.);
					
					m_GenerateLines ++;
					
				}
				
				if(m_GenerateLines == 0)
					throw new CompiereSystemException("@SaveError@ @ReturnEOrdered@");
				
				//	Reference C_Invoice
				loCarga.setC_Invoice_ID(invoice.getC_Invoice_ID());
				
				if(!loCarga.save())
					throw new CompiereSystemException("@SaveError@ @CUST_LoadOrderLine_ID@");
			}	
			if (pstmt != null)
				pstmt.close ();
			pstmt = null;
			m_LoadOrder.setIsDelivered(true);
			if(!m_LoadOrder.save()){
				throw new CompiereSystemException("@SaveError@ @CUST_LoadOrder_ID@");
			} else {
				//trx.rollback();
				//throw new CompiereSystemException("@SaveError@ @CUST_LoadOrder_ID@");
			}
			
		} else {
			throw new CompiereSystemException("@SaveError@ @C_Invoice_ID@");
		}
	}
	
	private void completeInvoice() throws CompiereSystemException{
		/*
		 * Completa la Entrega de Material
		 */
		//invoice.setDocAction(DocActionConstants.ACTION_Complete);
		if (invoice != null){
			//entrega.completeIt();
			/*Carlos Parada (Factura no se completa por diferencia en monto)*/
			//invoice.processIt(p_DocAction);
			
			if(!invoice.save(trx))
				throw new CompiereSystemException("@SaveError@ @C_Invoice_ID@");
			//
			addLog(invoice.getC_Invoice_ID(), invoice.getDateAcct(), null, invoice.getDocumentNo());
			m_created++;
		}
	}

	/**
	 * Verifica que exista una Pesada completada relacionada a la Orden de Carga
	 * @author Yamel Senih 19/03/2012, 03:51:27
	 * @return void
	 */
	/*private void findWeightRegister() throws Exception{
		String sql = new String("SELECT pp.* " +
				"FROM CUST_RecordWeight pp " +
				"WHERE pp.CUST_LoadOrder_ID = ? " +
				"AND pp.DocStatus IN('CO', 'CL')");
		PreparedStatement pstmt = null;
		//try	{
			pstmt = DB.prepareStatement (sql, trx);
			int index = 1;
			if (p_CUST_LoadOrder_ID != 0)
				pstmt.setInt(index++, p_CUST_LoadOrder_ID);
			ResultSet rs = pstmt.executeQuery();
			if(rs.next()){
				m_WeightRegister = new MCUSTRecordWeight(getCtx(), rs, trx);
			}
			if (pstmt != null)
				pstmt.close ();
			pstmt = null;
		/*}catch (Exception e) {
			pstmt = null;
			log.log(Level.SEVERE, sql.toString(), e);
		}	*/	
	//}	
}