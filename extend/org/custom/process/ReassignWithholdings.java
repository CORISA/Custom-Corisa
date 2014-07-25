package org.custom.process;

import org.compiere.model.MInvoice;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import java.sql.Timestamp;

/**
 *  @author Jorge Colmenarez
 * 	This method allows you to assign the voucher number and date 
 * 	of a document canceled a complete document.
 */

public class ReassignWithholdings extends SvrProcess {
	
	private String		p_TypeRetention;
	private int			p_Invoice_From;
	private int			p_Invoice_To;
	private Timestamp	p_DateDoc;

	@Override
	protected void prepare() {
		// TODO Auto-generated method stub
		for(ProcessInfoParameter parameter :getParameter()){
			String name = parameter.getParameterName();
			if(parameter.getParameter() == null){
				;
			}else if(name.equals("XX_TipoRetencion")){
				p_TypeRetention = (String) parameter.getParameter();
			}
			else if(name.equals("C_Invoice_From_ID"))
				p_Invoice_From = parameter.getParameterAsInt();	
			else if(name.equals("C_Invoice_To_ID"))
				p_Invoice_To = parameter.getParameterAsInt();	
			else if(name.equals("DateDoc"))
				p_DateDoc = (Timestamp) parameter.getParameter();		
		}
	}

	@Override
	protected String doIt() throws Exception {
		// TODO Auto-generated method stub		
		MInvoice ifrom = MInvoice.get(getCtx(), p_Invoice_From);
		MInvoice ito = MInvoice.get(getCtx(), p_Invoice_To);
		if(p_TypeRetention.equals("IV")){
			ito.set_Value("XX_Nro_Comprobante",ifrom.get_Value("XX_Nro_Comprobante"));
			if(p_DateDoc==null)
				ito.set_Value("XX_Fecha_Comprobante",ifrom.get_Value("XX_Fecha_Comprobante"));
			else
				ito.set_Value("XX_Fecha_Comprobante",p_DateDoc);
			ito.save();
			ifrom.set_Value("XX_Nro_Comprobante",null);
			ifrom.set_Value("XX_Fecha_Comprobante",null);
			ifrom.save();
		}else if(p_TypeRetention.equals("IS")){
			ito.set_Value("XX_Nro_ComprobanteISLR",ifrom.get_Value("XX_Nro_ComprobanteISLR"));
			if(p_DateDoc==null)
				ito.set_Value("XX_Fecha_ComprobanteISLR",ifrom.get_Value("XX_Fecha_ComprobanteISLR"));
			else
				ito.set_Value("XX_Fecha_ComprobanteISLR",p_DateDoc);
			ito.save();
			ifrom.set_Value("XX_Nro_ComprobanteISLR",null);
			ifrom.set_Value("XX_Fecha_ComprobanteISLR",null);
			ifrom.save();
		}
		
		addLog("@CompleteReassignment@");
		
		return null;
	}

}