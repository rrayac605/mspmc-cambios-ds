package mx.gob.imss.cit.pmc.cambios.service;

import mx.gob.imss.cit.mspmccommons.dto.ReporteCasuisticaInputDTO;
import mx.gob.imss.cit.mspmccommons.exception.BusinessException;
import net.sf.jasperreports.engine.JRException;
import org.apache.poi.ss.usermodel.Workbook;
import java.io.IOException;

public interface ReporteService {

	String getCasuisticaReport(ReporteCasuisticaInputDTO input)
			throws JRException, IOException, BusinessException;

	Workbook getCasuisticaReportXls(ReporteCasuisticaInputDTO input) throws JRException, IOException, BusinessException;

	Workbook getGeneralCasuisticaReportXls(ReporteCasuisticaInputDTO input) throws JRException, IOException, BusinessException;

	String getGeneralCasuisticaReportPdf(ReporteCasuisticaInputDTO input) throws JRException, IOException, BusinessException;

}
