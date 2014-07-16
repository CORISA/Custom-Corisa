/**
 * 
 */
package org.custom.process;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.X_M_InOut;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.CompiereSystemException;
import org.compiere.util.Trx;
import org.custom.model.MCUSTRecordWeight;

/**
 * @author info-analista2
 *
 */
public class GenerateInOutFromRecordWeight extends SvrProcess {
	/**	Record Weight ID		*/
	private int 			p_CUST_RecordWeight_ID = 0;
	/**	Sales Order			*/
	private int			m_C_Order_ID = 0;
	/**	Created				*/
	private int 			m_created = 0;
	/** Received			*/
	private MInOut 			inOut = null;
	/**	Weight Register		*/
	private MCUSTRecordWeight		m_RecordWeight = null;
	
	/**	Date Inveoice		*/
	private Timestamp		p_ShipDate;
	
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
			else if (name.equals("CUST_RecordWeight_ID"))
				p_CUST_RecordWeight_ID = para.getParameterAsInt();
			else if (name.equals("ShipDate"))
				p_ShipDate = (Timestamp)para.getParameter();
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
		
		try	{
			if(p_CUST_RecordWeight_ID == 0)
				throw new CompiereSystemException("@CUST_RecordWeight_ID@");
			out = generateReturn();
			trx.commit();
		}catch (Exception e) {
			trx.rollback();
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
	private String generateReturn() throws Exception{
		m_RecordWeight = new MCUSTRecordWeight(getCtx(), p_CUST_RecordWeight_ID, trx);
		m_C_Order_ID = m_RecordWeight.getM_RMA_ID();
		MOrder order = new MOrder(getCtx(), m_C_Order_ID, trx);
		inOut = new MInOut(order, 0, p_ShipDate);
		if(!inOut.save(trx))
			throw new CompiereSystemException("@SaveError@ @M_InOut_ID@");
		
		MOrderLine [] oLines = order.getLines();
		
		for(MOrderLine oLine : oLines){
			MInOutLine ioLine = new MInOutLine(inOut);
			ioLine.setOrderLine(oLine, 0, oLine.getQtyEntered());
			ioLine.setQty(oLine.getQtyEntered());	//	Correct UOM for QtyEntered
			
			if (oLine.getQtyEntered().compareTo(oLine.getQtyOrdered()) != 0)
				ioLine.setQtyEntered(oLine.getQtyEntered()
						.multiply(oLine.getQtyEntered())
						.divide(oLine.getQtyOrdered(), 12, BigDecimal.ROUND_HALF_UP));
			if(!ioLine.save(trx))
				throw new CompiereSystemException("@SaveError@ @M_InOut_ID@");
			
			//2014-01-29 Carlos Parada Set Qty Invoiced equals to Qty Ordered (Don't Pending for Invoice) 
			oLine.setQtyInvoiced(oLine.getQtyOrdered());
			if(!oLine.save(trx))
				throw new CompiereSystemException("@SaveError@ @C_OrderLine_ID@");
			
			//2014-30-01 Carlos Parada Add Reserved to Original Sales Order
			if (oLine.getOrig_OrderLine_ID()!=0){
				MOrderLine origLine = new MOrderLine(getCtx(), oLine.getOrig_OrderLine_ID(), trx);
				origLine.setQtyReserved(origLine.getQtyReserved().add(oLine.getQtyOrdered()));
				origLine.save(trx);
			}
				
			//End Carlos Parada
		}
		
		completeReturn();
		return "@Created@ = " + m_created;
	}
	
	private void completeReturn() throws CompiereSystemException{
		/*
		 * Completa la Entrega de Material
		 */
		inOut.setDocAction(X_M_InOut.DOCACTION_Complete);
		if (inOut != null){
			//entrega.completeIt();
			inOut.processIt(X_M_InOut.DOCACTION_Complete);
			//entrega.setDocStatus(X_M_InOut.DOCSTATUS_Completed);
			
			if(!inOut.save(trx))
				throw new CompiereSystemException("@SaveError@ @M_InOut_ID@");
			//
			addLog(inOut.getM_InOut_ID(), inOut.getDateAcct(), null, inOut.getDocumentNo());
			m_created++;
		}
	}	
}
