/**
 * @author Carlos Parada
 * @Date 23/03/2012
 * Clase Para Generar la Planilla Anual ARCV IANCA
 */
package org.custom.report;

import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

/**
 * @author info-analista1
 *
 */
public class XX_ARCV extends SvrProcess{

	/* 
	 * @author Carlos Parada
	 * @Date 23/03/2012 
	 * @Return void 
	 * Carga las variables del reporte dependiendo de lo que viene por parametro
	 */
	@Override
	protected void prepare() {
		// TODO Auto-generated method stub
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if (para.getParameter() == null)
				;
			else if (name.toLowerCase().equals("ad_org_id"))
				m_AD_Org_ID = para.getParameterAsInt();
			else if (name.toLowerCase().equals("c_bpartner_id"))
				m_C_BPartner = para.getParameterAsInt();
			else if (name.toLowerCase().equals("dateinvoiced"))
			{
				m_DateInvoiced = (para.getParameter()==null?null:(Timestamp)para.getParameter());
				m_DateInvoiced_To = (para.getParameter()==null?null:(Timestamp)para.getParameter_To());
			}
			else if (name.toLowerCase().equals("dateacct"))
			{
				m_DateAcct = (para.getParameter()==null?null:(Timestamp)para.getParameter());
				m_DateAcct_To = (para.getParameter()==null?null:(Timestamp)para.getParameter_To());
			}
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		m_arcv = new MXX_ARCV(getAD_PInstance_ID(), getAD_Client_ID(), m_AD_Org_ID, m_C_BPartner, m_DateInvoiced, m_DateInvoiced_To, m_DateAcct, m_DateAcct_To);
		Env.getCtx().setContext("#AD_PInstance_ID", getAD_PInstance_ID());
	}

	/* 
	 * @author Carlos Parada
	 * @Date 23/03/2012 
	 * @Return void 
	 * Busca los Datos del Arcv con los parametros enviados, si no devuelve ningun dato envia un msg al log
	 */
	@Override
	protected String doIt() throws Exception {
		// TODO Auto-generated method stub
		if (!m_arcv.LoadDataReport())
			log.log(Level.SEVERE, "No hay datos con los parametros indicados");
		return null;
	}
	
	
	private Integer m_AD_Org_ID;
	private Integer m_C_BPartner;
	private Timestamp m_DateInvoiced;
	private Timestamp m_DateInvoiced_To;
	private Timestamp m_DateAcct;
	private Timestamp m_DateAcct_To;
	private MXX_ARCV m_arcv; 
	
	
	
}
