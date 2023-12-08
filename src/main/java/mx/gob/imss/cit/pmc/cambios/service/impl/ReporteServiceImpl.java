package mx.gob.imss.cit.pmc.cambios.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.hssf.usermodel.HeaderFooter;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import mx.gob.imss.cit.mspmccommons.convert.ConvertNegResponse;
import mx.gob.imss.cit.mspmccommons.convert.ConvertResponseNeg;
import mx.gob.imss.cit.mspmccommons.dto.AuditoriaDTO;
import mx.gob.imss.cit.mspmccommons.dto.CambioDTO;
import mx.gob.imss.cit.mspmccommons.dto.DetalleRegistroDTO;
import mx.gob.imss.cit.mspmccommons.dto.ParametroDTO;
import mx.gob.imss.cit.mspmccommons.dto.ReporteCasuisticaInputDTO;
import mx.gob.imss.cit.mspmccommons.exception.BusinessException;
import mx.gob.imss.cit.mspmccommons.resp.AuditoriaResponse;
import mx.gob.imss.cit.mspmccommons.resp.DetalleRegistroResponse;
import mx.gob.imss.cit.mspmccommons.utils.DateUtils;
import mx.gob.imss.cit.pmc.cambios.repository.ParametroRepository;
import mx.gob.imss.cit.pmc.cambios.service.PmcCambiosService;
import mx.gob.imss.cit.pmc.cambios.service.ReporteService;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service("reportService")
public class ReporteServiceImpl implements ReporteService {

	private static final String SISTEMA_PMC = "Sistema PMC";
	private static final String PATTERN_DD_MM_YYYY = "dd/MM/yyyy";
	private static final String PATTERN_HH_MM = "HH:mm:ss";
	private static final String STR_VACIA = "";
	
	private static final Logger logger = LoggerFactory.getLogger(ReporteServiceImpl.class);

	@Autowired
	private PmcCambiosService pmcCambiosService;

	@Autowired
	private ParametroRepository parametroRepository;

	@Override
	public String getCasuisticaReport(ReporteCasuisticaInputDTO input)
			throws JRException, IOException, BusinessException {

		DetalleRegistroDTO detalleDTO = pmcCambiosService.getDetalleMovimiento(input.getObjectId(), input.getNumNss(),
				input.getPosition(), input.getNumFolioMovtoOriginal(), input.getIsChange());

		ConvertNegResponse convert = new ConvertNegResponse();
		DetalleRegistroResponse detalle = convert.getDetalleResp(detalleDTO);
		
		Optional<ParametroDTO> nombreInstitucion = parametroRepository.findOneByCve("nombreInstitucion");
		Optional<ParametroDTO> direccionInstitucion = parametroRepository.findOneByCve("direccionInstitucion_CC");
		Optional<ParametroDTO> unidadInstitucion = parametroRepository.findOneByCve("unidadInstitucion_CC");
		Optional<ParametroDTO> coordinacionInstituc = parametroRepository.findOneByCve("coordinacionInstitucion_CC");
		Optional<ParametroDTO> divisionInstitucion = parametroRepository.findOneByCve("divisionInstitucion_CC");
		Optional<ParametroDTO> nombreReporte = parametroRepository.findOneByCve("nombreReporte_CC");

		Map<String, Object> parameters = new HashMap<String, Object>();

		InputStream resourceAsStream = null;
		resourceAsStream = ReporteServiceImpl.class.getResourceAsStream("/reporteConsultaCasuistica.jrxml");

		JasperReport jasperReport = JasperCompileManager.compileReport(resourceAsStream);
		
		parameters.put("nombreInstitucion", nombreInstitucion.get().getDesParametro());
		parameters.put("direccionInstitucion", direccionInstitucion.get().getDesParametro());
		parameters.put("unidadInstitucion", unidadInstitucion.get().getDesParametro());
		parameters.put("coordinacionInstituc", coordinacionInstituc.get().getDesParametro());
		parameters.put("divisionInstitucion", divisionInstitucion.get().getDesParametro());
		parameters.put("nombreReporte", nombreReporte.get().getDesParametro());

		parameters.put("fromDate", null);
		parameters.put("toDate", null);

		parameters.put("delegacion", StringUtils.isEmpty(detalle.getAseguradoDTO().getDesDelegacionNss()) ? ""
				: detalle.getAseguradoDTO().getDesDelegacionNss());
		parameters.put("numDelegacion", StringUtils.isEmpty(detalle.getAseguradoDTO().getCveDelegacionNss()) ? 0
				: detalle.getAseguradoDTO().getCveDelegacionNss());
		parameters.put("subDelegacion", StringUtils.isEmpty(detalle.getAseguradoDTO().getDesSubDelNss()) ? ""
				: detalle.getAseguradoDTO().getDesSubDelNss());
		parameters.put("numSubDelegacion", StringUtils.isEmpty(detalle.getAseguradoDTO().getCveSubdelNss()) ? 0
				: detalle.getAseguradoDTO().getCveSubdelNss());

//		Asegurado
		parameters.put("nss", StringUtils.isEmpty(detalle.getAseguradoDTO().getNumNss()) ? ""
				: detalle.getAseguradoDTO().getNumNss());
		parameters.put("curp", StringUtils.isEmpty(detalle.getAseguradoDTO().getRefCurp()) ? ""
				: detalle.getAseguradoDTO().getRefCurp());
		String nombre = detalle.getAseguradoDTO().getNomAsegurado() != null
				? detalle.getAseguradoDTO().getNomAsegurado()
				: "";
		String ap = detalle.getAseguradoDTO().getRefPrimerApellido() != null
				? detalle.getAseguradoDTO().getRefPrimerApellido()
				: "";
		String am = detalle.getAseguradoDTO().getRefSegundoApellido() != null
				? detalle.getAseguradoDTO().getRefSegundoApellido()
				: "";
		parameters.put("nombreCompleto", nombre + " " + ap + " " + am);
		parameters.put("delegacionNSS", StringUtils.isEmpty(detalle.getAseguradoDTO().getDesDelegacionNss()) ? ""
				: detalle.getAseguradoDTO().getDesDelegacionNss());
		parameters.put("subDelegacionNSS", StringUtils.isEmpty(detalle.getAseguradoDTO().getDesSubDelNss()) ? ""
				: detalle.getAseguradoDTO().getDesSubDelNss());
		parameters.put("delegacionAtencionNSS",
				StringUtils.isEmpty(detalle.getAseguradoDTO().getDesDelegacionAtencion()) ? ""
						: detalle.getAseguradoDTO().getDesDelegacionAtencion());
		parameters.put("subDelegacionAtencionNSS",
				StringUtils.isEmpty(detalle.getAseguradoDTO().getDesSubDelAtencion()) ? ""
						: detalle.getAseguradoDTO().getDesSubDelAtencion());
		parameters.put("UMFAdscripcion", StringUtils.isEmpty(detalle.getAseguradoDTO().getDesUmfAdscripcion()) ? ""
				: detalle.getAseguradoDTO().getDesUmfAdscripcion());
		parameters.put("UMFExpedicion", StringUtils.isEmpty(detalle.getAseguradoDTO().getDesUmfExp()) ? ""
				: detalle.getAseguradoDTO().getDesUmfExp());
		parameters.put("UMFPagadora", StringUtils.isEmpty(detalle.getAseguradoDTO().getDesUmfPagadora()) ? ""
				: detalle.getAseguradoDTO().getDesUmfPagadora());
		parameters.put("salarioDiario", (detalle.getAseguradoDTO().getNumSalarioDiario() == null) ? new BigDecimal(0)
				: detalle.getAseguradoDTO().getNumSalarioDiario()); // BigDecimal
		parameters.put("ocupacion", StringUtils.isEmpty(detalle.getAseguradoDTO().getDesOcupacion()) ? ""
				: detalle.getAseguradoDTO().getDesOcupacion().toUpperCase());
		parameters.put("codigoError", StringUtils.isEmpty(detalle.getAseguradoDTO().getDesCodigoError()) ? ""
				: detalle.getAseguradoDTO().getDesCodigoError());
		parameters.put("anioCiclo", StringUtils.isEmpty(detalle.getAseguradoDTO().getNumCicloAnual()) ? ""
				: detalle.getAseguradoDTO().getNumCicloAnual());
		parameters.put("casoRegistro", StringUtils.isEmpty(detalle.getAseguradoDTO().getDesCasoRegistro()) ? ""
				: detalle.getAseguradoDTO().getDesCasoRegistro().toUpperCase());

//		Incapacidad
		
		parameters.put("fechaInicio",
				detalle.getIncapacidadDTO().getFecInicio() != null
						? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecInicio(), 
								PATTERN_DD_MM_YYYY) : "");
		parameters.put("fechaAtencion",
				detalle.getIncapacidadDTO().getFecAtencion() != null
						? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecAtencion(), 
								PATTERN_DD_MM_YYYY) : "");
		parameters.put("fechaAccidente",
				detalle.getIncapacidadDTO().getFecAccidente() != null
						? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecAccidente(),
								PATTERN_DD_MM_YYYY) : "");
		parameters.put("fechaIniPension",
				detalle.getIncapacidadDTO().getFecIniPension() != null
						? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecIniPension(),
								PATTERN_DD_MM_YYYY) : "");
		parameters.put("fechaAlta",
				detalle.getIncapacidadDTO().getFecAltaIncapacidad() != null
						? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecAltaIncapacidad(), 
								PATTERN_DD_MM_YYYY) : "");
		parameters.put("fechaExpedicion",
				detalle.getIncapacidadDTO().getFecExpDictamen() != null
						? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecExpDictamen(),
								PATTERN_DD_MM_YYYY) : "");
		parameters.put("fechaFin",
				detalle.getIncapacidadDTO().getFecFin() != null
						? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecFin(), 
								PATTERN_DD_MM_YYYY) : "");
		parameters.put("diasSubsidiados", String.valueOf(detalle.getIncapacidadDTO().getNumDiasSubsidiados() != null
				? detalle.getIncapacidadDTO().getNumDiasSubsidiados()
				: 0)); // int
		parameters.put("causaExterna", StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesCausaExterna()) ? ""
				: detalle.getIncapacidadDTO().getDesCausaExterna().toUpperCase());
		parameters.put("naturaleza", StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesNaturaleza()) ? ""
				: detalle.getIncapacidadDTO().getDesNaturaleza().toUpperCase());
		parameters.put("riesgoFisico", StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesRiesgoFisico()) ? ""
				: detalle.getIncapacidadDTO().getDesRiesgoFisico().toUpperCase());
		parameters.put("actoInseguro", StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesActoInseguro()) ? ""
				: detalle.getIncapacidadDTO().getDesActoInseguro().toUpperCase());
		parameters.put("tipoRiesgo", StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesTipoRiesgo()) ? ""
				: detalle.getIncapacidadDTO().getDesTipoRiesgo().toUpperCase());
		
		String descConsecuencia = StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesConsecuencia()) ? ""
				: detalle.getIncapacidadDTO().getDesConsecuencia();
		
		if(!StringUtils.isEmpty(detalle.getIncapacidadDTO().getCveConsecuencia()) &&
				!StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesConsecuencia())) {
			if(detalle.getIncapacidadDTO().getCveConsecuencia().equals("6")) {
				descConsecuencia = "Con Valuación inicial provisional posterior a la fecha de alta";
			}
		}
		
		parameters.put("consecuencia", descConsecuencia.toUpperCase());
		parameters.put("porcentajeIncapacidad",
				(detalle.getIncapacidadDTO().getPorPorcentajeIncapacidad() == null) ? new BigDecimal(0)
						: detalle.getIncapacidadDTO().getPorPorcentajeIncapacidad());
		parameters.put("laudos", StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesLaudo()) ? ""
				: detalle.getIncapacidadDTO().getDesLaudo().toUpperCase());
		parameters.put("matriculaMedTratante",
				StringUtils.isEmpty(detalle.getIncapacidadDTO().getNumMatMedTratante()) ? ""
						: detalle.getIncapacidadDTO().getNumMatMedTratante());
		parameters.put("matriculaMedAutoriza",
				StringUtils.isEmpty(detalle.getIncapacidadDTO().getNumMatMedAutCdst()) ? ""
						: detalle.getIncapacidadDTO().getNumMatMedAutCdst());
		parameters.put("codigoDiagnostico",
				StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesCodigoDiagnostico()) ? ""
						: detalle.getIncapacidadDTO().getDesCodigoDiagnostico().toUpperCase());
		parameters.put("tipoIncapacidad", StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesTipoIncapacidad()) ? ""
				: detalle.getIncapacidadDTO().getDesTipoIncapacidad().toUpperCase());

//		Patron
		parameters.put("registroPatronal", StringUtils.isEmpty(detalle.getPatronDTO().getRefRegistroPatronal()) ? ""
				: detalle.getPatronDTO().getRefRegistroPatronal());
		parameters.put("RFCPatron",
				StringUtils.isEmpty(detalle.getPatronDTO().getDesRfc()) ? "" : detalle.getPatronDTO().getDesRfc());
		parameters.put("razonSocial", StringUtils.isEmpty(detalle.getPatronDTO().getDesRazonSocial()) ? ""
				: detalle.getPatronDTO().getDesRazonSocial());
		parameters.put("delegacionRP", StringUtils.isEmpty(detalle.getPatronDTO().getDesDelRegPatronal()) ? ""
				: detalle.getPatronDTO().getDesDelRegPatronal());
		parameters.put("subDelegacionRP", StringUtils.isEmpty(detalle.getPatronDTO().getDesSubDelRegPatronal()) ? ""
				: detalle.getPatronDTO().getDesSubDelRegPatronal());
		parameters.put("clase",
				StringUtils.isEmpty(detalle.getPatronDTO().getDesClase()) ? "" : detalle.getPatronDTO().getDesClase());
		parameters.put("fraccion", StringUtils.isEmpty(detalle.getPatronDTO().getDesFraccion()) ? ""
				: detalle.getPatronDTO().getDesFraccion());
		parameters.put("prima", StringUtils.isEmpty(detalle.getPatronDTO().getNumPrima()) ? new BigDecimal(0)
				: detalle.getPatronDTO().getNumPrima());

//		Datos registro
		parameters.put("estadoRegistro", StringUtils.isEmpty(detalle.getAseguradoDTO().getDesEstadoRegistro()) ? ""
				: detalle.getAseguradoDTO().getDesEstadoRegistro().toUpperCase());
		String accion = "";
		String fechaCambio = "";
		String horaCambio = "";
		String cuentaUsuario = "";

		if (CollectionUtils.isNotEmpty(detalle.getAuditorias())) {
			Optional<AuditoriaResponse> aud = detalle.getAuditorias().stream().filter(a->a.getFecBaja() == null).findFirst();
			if(aud.isPresent()) {
				accion = aud.get().getDesAccionRegistro();
				
				fechaCambio = aud.get().getFecAlta() != null
						? DateUtils.parserDatetoString(aud.get().getFecAlta() ,PATTERN_DD_MM_YYYY) : "";
				
				horaCambio = aud.get().getFecAlta() != null
						? DateUtils.parserDatetoString(aud.get().getFecAlta(),PATTERN_HH_MM): "";
				
				cuentaUsuario = StringUtils.isEmpty(aud.get().getNomUsuario()) ? "" : aud.get().getNomUsuario();
				
			}
		}
		
		parameters.put("accionRegistro", !StringUtils.isEmpty(accion) ? accion.toUpperCase() : "");
		parameters.put("situacionRegistro", StringUtils.isEmpty(detalle.getDesSituacionRegistro()) ? ""
				: detalle.getDesSituacionRegistro().toUpperCase());

		if(cuentaUsuario.isEmpty()) {
			cuentaUsuario = StringUtils.isEmpty(detalle.getAseguradoDTO().getUsuarioModificador()) ? "" 
					: detalle.getAseguradoDTO().getUsuarioModificador();
		}
		
		if(fechaCambio.isEmpty()) {
			fechaCambio = detalle.getAseguradoDTO().getFecActualizacion() != null ?
					DateUtils.parserDatetoString(detalle.getAseguradoDTO().getFecActualizacion(), PATTERN_DD_MM_YYYY) : "";
		}
		
		if(horaCambio.isEmpty()) {
			horaCambio = detalle.getAseguradoDTO().getFecActualizacion() != null ?
					DateUtils.parserDatetoString(detalle.getAseguradoDTO().getFecActualizacion(), PATTERN_HH_MM) : "";
		}
		
		parameters.put("fechaCambio", fechaCambio);
		parameters.put("horaCambio", horaCambio);
		parameters.put("cuentaUsuario", cuentaUsuario);

		JasperPrint print = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
		return Base64.getEncoder().encodeToString(JasperExportManager.exportReportToPdf(print));
	}

	@Override
	public String getGeneralCasuisticaReportPdf(ReporteCasuisticaInputDTO input)
			throws JRException, IOException, BusinessException {

		List<CambioDTO> cambios = pmcCambiosService.findMovimientosReporte(input);

		Optional<ParametroDTO> nombreInstitucion = parametroRepository.findOneByCve("nombreInstitucion");
		Optional<ParametroDTO> direccionInstitucion = parametroRepository.findOneByCve("direccionInstitucion_CC");
		Optional<ParametroDTO> unidadInstitucion = parametroRepository.findOneByCve("unidadInstitucion_CC");
		Optional<ParametroDTO> coordinacionInstituc = parametroRepository.findOneByCve("coordinacionInstitucion_CC");
		Optional<ParametroDTO> divisionInstitucion = parametroRepository.findOneByCve("divisionInstitucion_CC");
		Optional<ParametroDTO> nombreReporte = parametroRepository.findOneByCve("nombreReporte_CC");
		String desSubDelegacion = input.getDesSubdelegacion();
		Integer cveDelegacion = input.getCveDelegacion();
		String desDelegacion = input.getDesDelegacion();
		Integer cveSubDelegacion = input.getCveSubdelegacion();

		Map<String, Object> parameters = new HashMap<String, Object>();

		InputStream resourceAsStream = ReporteServiceImpl.class
				.getResourceAsStream("/reporteConsultaCasuisticaGeneral.jrxml");

		JasperReport jasperReport = JasperCompileManager.compileReport(resourceAsStream);

		parameters.put("nombreInstitucion", nombreInstitucion.get().getDesParametro());
		parameters.put("direccionInstitucion", direccionInstitucion.get().getDesParametro());
		parameters.put("unidadInstitucion", unidadInstitucion.get().getDesParametro());
		parameters.put("coordinacionInstituc", coordinacionInstituc.get().getDesParametro());
		parameters.put("divisionInstitucion", divisionInstitucion.get().getDesParametro());
		parameters.put("nombreReporte", nombreReporte.get().getDesParametro());
		parameters.put("subDelegacion", desSubDelegacion);
		parameters.put("delegacion", desDelegacion);
		parameters.put("numDelegacion", cveDelegacion != null ? cveDelegacion : 0);
		parameters.put("numSubDelegacion", cveSubDelegacion != null ? cveSubDelegacion : 0);

		parameters.put("fromDate", null);
		parameters.put("toDate", null);

		JRBeanCollectionDataSource studentDataSource = new JRBeanCollectionDataSource(cambios, false);

		JasperPrint print = JasperFillManager.fillReport(jasperReport, parameters, studentDataSource);

		logger.info("Se lleno el reporte y se retorna el b64");

		return Base64.getEncoder().encodeToString(JasperExportManager.exportReportToPdf(print));
	}

	@Override
	public Workbook getCasuisticaReportXls(ReporteCasuisticaInputDTO input)
			throws JRException, IOException, BusinessException {
		DetalleRegistroDTO detalleDTO = pmcCambiosService.getDetalleMovimiento(input.getObjectId(), input.getNumNss(),
				input.getPosition(), input.getNumFolioMovtoOriginal(), input.getIsChange());

		ConvertNegResponse convert = new ConvertNegResponse();
		DetalleRegistroResponse detalle = convert.getDetalleResp(detalleDTO);
		
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFFont font = workbook.createFont();
		font.setFontName("Montserrat");
		font.setFontHeightInPoints((short) 8);
		font.setBold(true);

		XSSFFont fontPeriodo = workbook.createFont();
		fontPeriodo.setFontName("Montserrat");
		fontPeriodo.setFontHeightInPoints((short) 8);
		fontPeriodo.setColor(HSSFColor.WHITE.index);
		fontPeriodo.setBold(true);

		// Estilos de celda
		CellStyle rowColorStyleLeft = createStyle(font, HorizontalAlignment.LEFT, VerticalAlignment.CENTER,
				HSSFColor.GREY_25_PERCENT.index, false, HSSFColor.WHITE.index, workbook, true);
		CellStyle rowWhiteStyle = createStyle(font, HorizontalAlignment.LEFT, VerticalAlignment.CENTER,
				HSSFColor.WHITE.index, false, HSSFColor.WHITE.index, workbook, true);

		if (input != null) {
			Sheet sheetCasuistica = workbook.createSheet("Registro 01 ");
			Header header = sheetCasuistica.getHeader();
			header.setRight("Hoja " + HeaderFooter.page() + " de " + HeaderFooter.numPages());

			sheetCasuistica.setColumnWidth(0, 512);
			sheetCasuistica.setMargin(Sheet.LeftMargin, 0.5 /* inches */ );
			sheetCasuistica.setMargin(Sheet.RightMargin, 0.5 /* inches */ );

			for (int i = 1; i < 55; i++) {
				sheetCasuistica.setColumnWidth(i, 5120);
			}

			fillDatosPaciente(detalle, sheetCasuistica, 0, rowColorStyleLeft, rowWhiteStyle);
		}
		return workbook;
	}

	@Override
	public Workbook getGeneralCasuisticaReportXls(ReporteCasuisticaInputDTO input)
			throws JRException, IOException, BusinessException {
		List<CambioDTO> cambios = pmcCambiosService.findMovimientosReporte(input);
		ConvertResponseNeg convert = new ConvertResponseNeg();
		
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFFont font = workbook.createFont();
		font.setFontName("Montserrat");
		font.setFontHeightInPoints((short) 8);
		font.setBold(true);

		XSSFFont fontPeriodo = workbook.createFont();
		fontPeriodo.setFontName("Montserrat");
		fontPeriodo.setFontHeightInPoints((short) 8);
		fontPeriodo.setColor(HSSFColor.WHITE.index);
		fontPeriodo.setBold(true);

		Sheet sheetCasuistica = workbook.createSheet("Registro 01 ");

		// Estilos de celda
		CellStyle rowColorStyleLeft = createStyle(font, HorizontalAlignment.LEFT, VerticalAlignment.CENTER,
				HSSFColor.GREY_25_PERCENT.index, false, HSSFColor.WHITE.index, workbook, true);
		CellStyle rowWhiteStyle = createStyle(font, HorizontalAlignment.LEFT, VerticalAlignment.CENTER,
				HSSFColor.WHITE.index, false, HSSFColor.WHITE.index, workbook, true);

		int b = 0;
		for (CambioDTO cambio : cambios) {
			Header header = sheetCasuistica.getHeader();
			header.setRight("Hoja " + HeaderFooter.page() + " de " + HeaderFooter.numPages());

			sheetCasuistica.setColumnWidth(0, 512);
			sheetCasuistica.setMargin(Sheet.LeftMargin, 0.5 /* inches */ );
			sheetCasuistica.setMargin(Sheet.RightMargin, 0.5 /* inches */ );

			for (int i = 1; i < 50; i++) {
				sheetCasuistica.setColumnWidth(i, 5120);
			}
			
			fillDatosPaciente(cambio, sheetCasuistica, b, rowColorStyleLeft,
					rowWhiteStyle);
			b++;
		}
		return workbook;
	}

	private void fillDatosPaciente(DetalleRegistroResponse detalle, Sheet sheetCasuistica, int row, CellStyle rowColorStyle,
			CellStyle rowWhiteStyle) {

		if (row == 0) {
			Row r1 = sheetCasuistica.createRow(0);

			Cell cNSS = r1.createCell(1);
			cNSS.setCellValue("NSS");
			cNSS.setCellStyle(rowColorStyle);

			Cell cCURP = r1.createCell(2);
			cCURP.setCellValue("CURP");
			cCURP.setCellStyle(rowColorStyle);

			Cell cNombre = r1.createCell(3);
			cNombre.setCellValue("Nombre(s) y apellidos");
			cNombre.setCellStyle(rowColorStyle);

			Cell del = r1.createCell(4);
			del.setCellValue("Delegación de NSS");
			del.setCellStyle(rowColorStyle);

			Cell subDel = r1.createCell(5);
			subDel.setCellValue("Subdelegación de NSS");
			subDel.setCellStyle(rowColorStyle);

			Cell delAtencion = r1.createCell(6);
			delAtencion.setCellValue("Delegación atención del NSS");
			delAtencion.setCellStyle(rowColorStyle);

			Cell subDelAtencion = r1.createCell(7);
			subDelAtencion.setCellValue("Subdelegación atención del NSS");
			subDelAtencion.setCellStyle(rowColorStyle);

			Cell umfAdscripcion = r1.createCell(8);
			umfAdscripcion.setCellValue("UMF de adscripción");
			umfAdscripcion.setCellStyle(rowColorStyle);

			Cell umfExpedicion = r1.createCell(9);
			umfExpedicion.setCellValue("UMF de expedición");
			umfExpedicion.setCellStyle(rowColorStyle);

			Cell umfPagadora = r1.createCell(10);
			umfPagadora.setCellValue("UMF pagadora");
			umfPagadora.setCellStyle(rowColorStyle);

			Cell salario = r1.createCell(11);
			salario.setCellValue("Salario diario");
			salario.setCellStyle(rowColorStyle);

			Cell ocupacion = r1.createCell(12);
			ocupacion.setCellValue("Ocupación");
			ocupacion.setCellStyle(rowColorStyle);

			Cell codigoError = r1.createCell(13);
			codigoError.setCellValue("Código de error");
			codigoError.setCellStyle(rowColorStyle);

			Cell anioCiclo = r1.createCell(14);
			anioCiclo.setCellValue("Año ciclo");
			anioCiclo.setCellStyle(rowColorStyle);

			Cell casoRegistro = r1.createCell(15);
			casoRegistro.setCellValue("Caso del registro");
			casoRegistro.setCellStyle(rowColorStyle);

			Cell fecInicio = r1.createCell(16);
			fecInicio.setCellValue("Fecha de inicio");
			fecInicio.setCellStyle(rowColorStyle);

			Cell fecAtencion = r1.createCell(17);
			fecAtencion.setCellValue("Fecha de atención");
			fecAtencion.setCellStyle(rowColorStyle);

			Cell fecAccidente = r1.createCell(18);
			fecAccidente.setCellValue("Fecha de accidente");
			fecAccidente.setCellStyle(rowColorStyle);

			Cell fecInicioP = r1.createCell(19);
			fecInicioP.setCellValue("Fecha inicio de pensión");
			fecInicioP.setCellStyle(rowColorStyle);

			Cell fecAlta = r1.createCell(20);
			fecAlta.setCellValue("Fecha de alta");
			fecAlta.setCellStyle(rowColorStyle);

			Cell fecExpedicion = r1.createCell(21);
			fecExpedicion.setCellValue("Fecha de expedición del dictamen");
			fecExpedicion.setCellStyle(rowColorStyle);

			Cell fecFin = r1.createCell(22);
			fecFin.setCellValue("Fecha fin");
			fecFin.setCellStyle(rowColorStyle);

			Cell diasSubsidiados = r1.createCell(23);
			diasSubsidiados.setCellValue("Días subsidiados");
			diasSubsidiados.setCellStyle(rowColorStyle);

			Cell causaExterna = r1.createCell(24);
			causaExterna.setCellValue("Causa externa");
			causaExterna.setCellStyle(rowColorStyle);

			Cell naturaleza = r1.createCell(25);
			naturaleza.setCellValue("Naturaleza");
			naturaleza.setCellStyle(rowColorStyle);

			Cell riesgoFisico = r1.createCell(26);
			riesgoFisico.setCellValue("Riesgo físico");
			riesgoFisico.setCellStyle(rowColorStyle);

			Cell actoInseguro = r1.createCell(27);
			actoInseguro.setCellValue("Acto inseguro");
			actoInseguro.setCellStyle(rowColorStyle);

			Cell tipoRiesgo = r1.createCell(28);
			tipoRiesgo.setCellValue("Tipo de riesgo");
			tipoRiesgo.setCellStyle(rowColorStyle);

			Cell consecuencia = r1.createCell(29);
			consecuencia.setCellValue("Consecuencia");
			consecuencia.setCellStyle(rowColorStyle);

			Cell porcentajeIncapacidad = r1.createCell(30);
			porcentajeIncapacidad.setCellValue("Porcentaje de incapacidad");
			porcentajeIncapacidad.setCellStyle(rowColorStyle);

			Cell tipoIncapacidad = r1.createCell(31);
			tipoIncapacidad.setCellValue("Tipo de incapacidad");
			tipoIncapacidad.setCellStyle(rowColorStyle);

			Cell laudos = r1.createCell(32);
			laudos.setCellValue("Laudos");
			laudos.setCellStyle(rowColorStyle);

			Cell codigoDiagnostico = r1.createCell(33);
			codigoDiagnostico.setCellValue("Código del diagnóstico");
			codigoDiagnostico.setCellStyle(rowColorStyle);

			Cell matriculaMDTratante = r1.createCell(34);
			matriculaMDTratante.setCellValue("Matrícula del médico tratante");
			matriculaMDTratante.setCellStyle(rowColorStyle);

			Cell matriculaMDAutoriza = r1.createCell(35);
			matriculaMDAutoriza.setCellValue("Matrícula del médico que autoriza CDST");
			matriculaMDAutoriza.setCellStyle(rowColorStyle);

			Cell regPatronal = r1.createCell(36);
			regPatronal.setCellValue("Registro patronal");
			regPatronal.setCellStyle(rowColorStyle);

			Cell rfcPatron = r1.createCell(37);
			rfcPatron.setCellValue("RFC patrón");
			rfcPatron.setCellStyle(rowColorStyle);

			Cell razonSocial = r1.createCell(38);
			razonSocial.setCellValue("Razón social");
			razonSocial.setCellStyle(rowColorStyle);

			Cell delRegistroPatronal = r1.createCell(39);
			delRegistroPatronal.setCellValue("Delegación del registro patronal");
			delRegistroPatronal.setCellStyle(rowColorStyle);

			Cell subDelRegistroPatronal = r1.createCell(40);
			subDelRegistroPatronal.setCellValue("Subdelegación del registro patronal");
			subDelRegistroPatronal.setCellStyle(rowColorStyle);

			Cell clase = r1.createCell(41);
			clase.setCellValue("Clase");
			clase.setCellStyle(rowColorStyle);

			Cell fraccion = r1.createCell(42);
			fraccion.setCellValue("Fracción");
			fraccion.setCellStyle(rowColorStyle);

			Cell prima = r1.createCell(43);
			prima.setCellValue("Prima");
			prima.setCellStyle(rowColorStyle);

			Cell estadoRegistro = r1.createCell(44);
			estadoRegistro.setCellValue("Estado del registro");
			estadoRegistro.setCellStyle(rowColorStyle);

			Cell accionRegistro = r1.createCell(45);
			accionRegistro.setCellValue("Acción del registro");
			accionRegistro.setCellStyle(rowColorStyle);

			Cell situacionRegistro = r1.createCell(46);
			situacionRegistro.setCellValue("Situación del registro");
			situacionRegistro.setCellStyle(rowColorStyle);

			Cell fechaCambio = r1.createCell(47);
			fechaCambio.setCellValue("Fecha del cambio");
			fechaCambio.setCellStyle(rowColorStyle);

			Cell horaCambio = r1.createCell(48);
			horaCambio.setCellValue("Hora del cambio");
			horaCambio.setCellStyle(rowColorStyle);

			Cell cuentaUsuario = r1.createCell(49);
			cuentaUsuario.setCellValue("Cuenta de Usuario");
			cuentaUsuario.setCellStyle(rowColorStyle);
		}
		Row r2 = sheetCasuistica.createRow(row + 1);

		Cell cdNSS = r2.createCell(1);
		cdNSS.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getNumNss()) ? ""
				: detalle.getAseguradoDTO().getNumNss());
		cdNSS.setCellStyle(rowWhiteStyle);
		Cell cdCURP = r2.createCell(2);
		cdCURP.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getRefCurp()) ? ""
				: detalle.getAseguradoDTO().getRefCurp());
		cdCURP.setCellStyle(rowWhiteStyle);

		String nombre = detalle.getAseguradoDTO().getNomAsegurado() != null
				? detalle.getAseguradoDTO().getNomAsegurado()
				: "";
		String ap = detalle.getAseguradoDTO().getRefPrimerApellido() != null
				? detalle.getAseguradoDTO().getRefPrimerApellido()
				: "";
		String am = detalle.getAseguradoDTO().getRefSegundoApellido() != null
				? detalle.getAseguradoDTO().getRefSegundoApellido()
				: "";

		Cell cdNombre = r2.createCell(3);
		cdNombre.setCellValue(nombre + " " + ap + " " + am);
		cdNombre.setCellStyle(rowWhiteStyle);

		Cell dDel = r2.createCell(4);
		dDel.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getDesDelegacionNss()) ? ""
				: detalle.getAseguradoDTO().getDesDelegacionNss());
		dDel.setCellStyle(rowWhiteStyle);

		Cell dsubDel = r2.createCell(5);
		dsubDel.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getDesSubDelNss()) ? ""
				: detalle.getAseguradoDTO().getDesSubDelNss());
		dsubDel.setCellStyle(rowWhiteStyle);

		Cell ddelAtencion = r2.createCell(6);
		ddelAtencion.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getDesDelegacionAtencion()) ? ""
				: detalle.getAseguradoDTO().getDesDelegacionAtencion());
		ddelAtencion.setCellStyle(rowWhiteStyle);

		Cell dsubDelAtencion = r2.createCell(7);
		dsubDelAtencion.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getDesSubDelAtencion()) ? ""
				: detalle.getAseguradoDTO().getDesSubDelAtencion());
		dsubDelAtencion.setCellStyle(rowWhiteStyle);

		Cell dumfAdscripcion = r2.createCell(8);
		dumfAdscripcion.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getDesUmfAdscripcion()) ? ""
				: detalle.getAseguradoDTO().getDesUmfAdscripcion());
		dumfAdscripcion.setCellStyle(rowWhiteStyle);

		Cell dumfExpedicion = r2.createCell(9);
		dumfExpedicion.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getDesUmfExp()) ? ""
				: detalle.getAseguradoDTO().getDesUmfExp());
		dumfExpedicion.setCellStyle(rowWhiteStyle);

		Cell dumfPagadora = r2.createCell(10);
		dumfPagadora.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getDesUmfPagadora()) ? ""
				: detalle.getAseguradoDTO().getDesUmfPagadora());
		dumfPagadora.setCellStyle(rowWhiteStyle);

		Cell dsalario = r2.createCell(11);
		dsalario.setCellValue(
				(detalle.getAseguradoDTO().getNumSalarioDiario() == null) ? new BigDecimal(0).doubleValue()
						: detalle.getAseguradoDTO().getNumSalarioDiario().doubleValue());
		dsalario.setCellStyle(rowWhiteStyle);

		Cell docupacion = r2.createCell(12);
		docupacion.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getDesOcupacion()) ? ""
				: detalle.getAseguradoDTO().getDesOcupacion());
		docupacion.setCellStyle(rowWhiteStyle);

		Cell dcodigoError = r2.createCell(13);
		dcodigoError.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getDesCodigoError()) ? ""
				: detalle.getAseguradoDTO().getDesCodigoError());
		dcodigoError.setCellStyle(rowWhiteStyle);

		Cell danioCiclo = r2.createCell(14);
		danioCiclo.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getNumCicloAnual()) ? ""
				: detalle.getAseguradoDTO().getNumCicloAnual());
		danioCiclo.setCellStyle(rowWhiteStyle);

		Cell dcasoRegistro = r2.createCell(15);
		dcasoRegistro.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getDesCasoRegistro()) ? ""
				: detalle.getAseguradoDTO().getDesCasoRegistro());
		dcasoRegistro.setCellStyle(rowWhiteStyle);
		
		Cell dfecInicio = r2.createCell(16);
		dfecInicio.setCellValue(detalle.getIncapacidadDTO().getFecInicio() != null
				? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecInicio(), PATTERN_DD_MM_YYYY)
				: "");
		dfecInicio.setCellStyle(rowWhiteStyle);

		Cell dfecAtencion = r2.createCell(17);
		dfecAtencion.setCellValue(detalle.getIncapacidadDTO().getFecAtencion() != null
				? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecAtencion(), PATTERN_DD_MM_YYYY)
				: "");
		dfecAtencion.setCellStyle(rowWhiteStyle);

		Cell dfecAccidente = r2.createCell(18);
		dfecAccidente.setCellValue(detalle.getIncapacidadDTO().getFecAccidente() != null
				? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecAccidente(), PATTERN_DD_MM_YYYY)
				: "");
		dfecAccidente.setCellStyle(rowWhiteStyle);

		Cell dfecInicioP = r2.createCell(19);
		dfecInicioP.setCellValue(detalle.getIncapacidadDTO().getFecIniPension() != null
				? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecIniPension(), PATTERN_DD_MM_YYYY)
				: "");
		dfecInicioP.setCellStyle(rowWhiteStyle);

		Cell dfecAlta = r2.createCell(20);
		dfecAlta.setCellValue(detalle.getIncapacidadDTO().getFecAltaIncapacidad() != null
				? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecAltaIncapacidad(), PATTERN_DD_MM_YYYY)
				: "");
		dfecAlta.setCellStyle(rowWhiteStyle);

		Cell dfecExpedicion = r2.createCell(21);
		dfecExpedicion.setCellValue(detalle.getIncapacidadDTO().getFecExpDictamen() != null
				? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecExpDictamen(), PATTERN_DD_MM_YYYY)
				: "");
		dfecExpedicion.setCellStyle(rowWhiteStyle);
		
		Cell dfecFin = r2.createCell(22);
		dfecFin.setCellValue(detalle.getIncapacidadDTO().getFecFin() != null
				? DateUtils.parserDatetoStringUTC(detalle.getIncapacidadDTO().getFecFin(), PATTERN_DD_MM_YYYY)
				: "");
		dfecFin.setCellStyle(rowWhiteStyle);

		Cell ddiasSubsidiados = r2.createCell(23);
		ddiasSubsidiados.setCellValue(String.valueOf(detalle.getIncapacidadDTO().getNumDiasSubsidiados() != null
				? detalle.getIncapacidadDTO().getNumDiasSubsidiados()
				: 0));
		ddiasSubsidiados.setCellStyle(rowWhiteStyle);

		Cell dcausaExterna = r2.createCell(24);
		dcausaExterna.setCellValue(StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesCausaExterna()) ? ""
				: detalle.getIncapacidadDTO().getDesCausaExterna());
		dcausaExterna.setCellStyle(rowWhiteStyle);

		Cell dnaturaleza = r2.createCell(25);
		dnaturaleza.setCellValue(StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesNaturaleza()) ? ""
				: detalle.getIncapacidadDTO().getDesNaturaleza());
		dnaturaleza.setCellStyle(rowWhiteStyle);

		Cell driesgoFisico = r2.createCell(26);
		driesgoFisico.setCellValue(StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesRiesgoFisico()) ? ""
				: detalle.getIncapacidadDTO().getDesRiesgoFisico());
		driesgoFisico.setCellStyle(rowWhiteStyle);

		Cell dactoInseguro = r2.createCell(27);
		dactoInseguro.setCellValue(StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesActoInseguro()) ? ""
				: detalle.getIncapacidadDTO().getDesActoInseguro());
		dactoInseguro.setCellStyle(rowWhiteStyle);

		Cell dtipoRiesgo = r2.createCell(28);
		dtipoRiesgo.setCellValue(StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesTipoRiesgo()) ? ""
				: detalle.getIncapacidadDTO().getDesTipoRiesgo());
		dtipoRiesgo.setCellStyle(rowWhiteStyle);

		String descConsecuencia = StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesConsecuencia()) ? ""
				: detalle.getIncapacidadDTO().getDesConsecuencia();
		if(!StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesConsecuencia()) &&
				!StringUtils.isEmpty(detalle.getIncapacidadDTO().getCveConsecuencia())) {
			if(detalle.getIncapacidadDTO().getCveConsecuencia().equals("6")) {
				descConsecuencia = "Con Valuación inicial provisional posterior a la fecha de alta";
			}
		}
		
		Cell dconsecuencia = r2.createCell(29);
		dconsecuencia.setCellValue(descConsecuencia);
		dconsecuencia.setCellStyle(rowWhiteStyle);

		Cell dporcentajeIncapacidad = r2.createCell(30);
		dporcentajeIncapacidad.setCellValue(
				(detalle.getIncapacidadDTO().getPorPorcentajeIncapacidad() == null) ? new BigDecimal(0).doubleValue()
						: detalle.getIncapacidadDTO().getPorPorcentajeIncapacidad().doubleValue());
		dporcentajeIncapacidad.setCellStyle(rowWhiteStyle);

		Cell dtipoIncapacidad = r2.createCell(31);
		dtipoIncapacidad.setCellValue(StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesTipoIncapacidad()) ? ""
				: detalle.getIncapacidadDTO().getDesTipoIncapacidad());
		dtipoIncapacidad.setCellStyle(rowWhiteStyle);

		Cell dlaudos = r2.createCell(32);
		dlaudos.setCellValue(StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesLaudo()) ? ""
				: detalle.getIncapacidadDTO().getDesLaudo());
		dlaudos.setCellStyle(rowWhiteStyle);

		Cell dcodigoDiagnostico = r2.createCell(33);
		dcodigoDiagnostico.setCellValue(StringUtils.isEmpty(detalle.getIncapacidadDTO().getDesCodigoDiagnostico()) ? ""
				: detalle.getIncapacidadDTO().getDesCodigoDiagnostico());
		dcodigoDiagnostico.setCellStyle(rowWhiteStyle);

		Cell dmatriculaMDTratante = r2.createCell(34);
		dmatriculaMDTratante.setCellValue(StringUtils.isEmpty(detalle.getIncapacidadDTO().getNumMatMedTratante()) ? ""
				: detalle.getIncapacidadDTO().getNumMatMedTratante());
		dmatriculaMDTratante.setCellStyle(rowWhiteStyle);

		Cell dmatriculaMDAutoriza = r2.createCell(35);
		dmatriculaMDAutoriza.setCellValue(StringUtils.isEmpty(detalle.getIncapacidadDTO().getNumMatMedAutCdst()) ? ""
				: detalle.getIncapacidadDTO().getNumMatMedAutCdst());
		dmatriculaMDAutoriza.setCellStyle(rowWhiteStyle);

		// Datos del patron

		Cell dregPatronal = r2.createCell(36);
		dregPatronal.setCellValue(StringUtils.isEmpty(detalle.getPatronDTO().getRefRegistroPatronal()) ? ""
				: detalle.getPatronDTO().getRefRegistroPatronal());
		dregPatronal.setCellStyle(rowWhiteStyle);

		Cell drfcPatron = r2.createCell(37);
		drfcPatron.setCellValue(
				StringUtils.isEmpty(detalle.getPatronDTO().getDesRfc()) ? "" : detalle.getPatronDTO().getDesRfc());
		drfcPatron.setCellStyle(rowWhiteStyle);

		Cell drazonSocial = r2.createCell(38);
		drazonSocial.setCellValue(StringUtils.isEmpty(detalle.getPatronDTO().getDesRazonSocial()) ? ""
				: detalle.getPatronDTO().getDesRazonSocial());
		drazonSocial.setCellStyle(rowWhiteStyle);

		Cell ddelRegistroPatronal = r2.createCell(39);
		ddelRegistroPatronal.setCellValue(StringUtils.isEmpty(detalle.getPatronDTO().getDesDelRegPatronal()) ? ""
				: detalle.getPatronDTO().getDesDelRegPatronal());
		ddelRegistroPatronal.setCellStyle(rowWhiteStyle);

		Cell dsubDelRegistroPatronal = r2.createCell(40);
		dsubDelRegistroPatronal.setCellValue(StringUtils.isEmpty(detalle.getPatronDTO().getDesSubDelRegPatronal()) ? ""
				: detalle.getPatronDTO().getDesSubDelRegPatronal());
		dsubDelRegistroPatronal.setCellStyle(rowWhiteStyle);

		Cell dclase = r2.createCell(41);
		dclase.setCellValue(
				StringUtils.isEmpty(detalle.getPatronDTO().getDesClase()) ? "" : detalle.getPatronDTO().getDesClase());
		dclase.setCellStyle(rowWhiteStyle);

		Cell dfraccion = r2.createCell(42);
		dfraccion.setCellValue(StringUtils.isEmpty(detalle.getPatronDTO().getDesFraccion()) ? ""
				: detalle.getPatronDTO().getDesFraccion());
		dfraccion.setCellStyle(rowWhiteStyle);

		Cell dprima = r2.createCell(43);
		dprima.setCellValue(
				StringUtils.isEmpty(detalle.getPatronDTO().getDesPrima()) ? 
						obtenerPrima(detalle.getPatronDTO().getNumPrima()) : detalle.getPatronDTO().getDesPrima());
		dprima.setCellStyle(rowWhiteStyle);

		// Datos del registro

		Cell destadoRegistro = r2.createCell(44);
		destadoRegistro.setCellValue(StringUtils.isEmpty(detalle.getAseguradoDTO().getDesEstadoRegistro()) ? ""
				: detalle.getAseguradoDTO().getDesEstadoRegistro());
		destadoRegistro.setCellStyle(rowWhiteStyle);

		String accion = "";
		String fechaCambio = "";
		String horaCambio = "";
		String cuentaUsuario = "";

		if (CollectionUtils.isNotEmpty(detalle.getAuditorias())) {
			Optional<AuditoriaResponse> aud = detalle.getAuditorias().stream().filter(a->a.getFecBaja() == null).findFirst();
			if(aud.isPresent()) {
				accion = aud.get().getDesAccionRegistro();

				fechaCambio = aud.get().getFecAlta() != null ? 
						DateUtils.parserDatetoString(aud.get().getFecAlta(), PATTERN_DD_MM_YYYY): "";
				
				horaCambio = aud.get().getFecAlta() != null
						? DateUtils.parserDatetoString(aud.get().getFecAlta(), PATTERN_HH_MM) : "";
				
				cuentaUsuario = StringUtils.isEmpty(aud.get().getNomUsuario()) ? "" : aud.get().getNomUsuario();
				
			}
		}
		
		if(cuentaUsuario.isEmpty()) {
			cuentaUsuario = StringUtils.isEmpty(detalle.getAseguradoDTO().getUsuarioModificador()) ? "" 
					: detalle.getAseguradoDTO().getUsuarioModificador();
		}
		
		if(fechaCambio.isEmpty()) {
			fechaCambio = detalle.getAseguradoDTO().getFecActualizacion() != null ?
					DateUtils.parserDatetoString(detalle.getAseguradoDTO().getFecActualizacion(), PATTERN_DD_MM_YYYY) : "";
		}
		
		if(horaCambio.isEmpty()) {
			horaCambio = detalle.getAseguradoDTO().getFecActualizacion() != null ?
					DateUtils.parserDatetoString(detalle.getAseguradoDTO().getFecActualizacion(), PATTERN_HH_MM) : "";
		}

		Cell daccionRegistro = r2.createCell(45);
		daccionRegistro.setCellValue(accion);
		daccionRegistro.setCellStyle(rowWhiteStyle);

		Cell dsituacionRegistro = r2.createCell(46);
		dsituacionRegistro.setCellValue(
				StringUtils.isEmpty(detalle.getDesSituacionRegistro()) ? "" : detalle.getDesSituacionRegistro());
		dsituacionRegistro.setCellStyle(rowWhiteStyle);

		Cell dfechaCambio = r2.createCell(47);
		dfechaCambio.setCellValue(fechaCambio);
		dfechaCambio.setCellStyle(rowWhiteStyle);

		Cell dhoraCambio = r2.createCell(48);
		dhoraCambio.setCellValue(horaCambio);
		dhoraCambio.setCellStyle(rowWhiteStyle);

		Cell dcuentaUsuario = r2.createCell(49);
		dcuentaUsuario.setCellValue(cuentaUsuario);
		dcuentaUsuario.setCellStyle(rowWhiteStyle);

	}

	private CellStyle createStyle(XSSFFont font, HorizontalAlignment hAlign, VerticalAlignment vAlign, short cellColor,
			boolean cellBorder, short cellBorderColor, Workbook workbook, boolean wrap) {

		CellStyle style = workbook.createCellStyle();
		style.setFont(font);
		style.setFillForegroundColor(cellColor);
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setAlignment(hAlign);
		style.setVerticalAlignment(vAlign);
		style.setWrapText(wrap);

		if (cellBorder) {
			style.setBorderTop(BorderStyle.THIN);
			style.setBorderLeft(BorderStyle.THIN);
			style.setBorderRight(BorderStyle.THIN);
			style.setBorderBottom(BorderStyle.THIN);

			style.setTopBorderColor(cellBorderColor);
			style.setLeftBorderColor(cellBorderColor);
			style.setRightBorderColor(cellBorderColor);
			style.setBottomBorderColor(cellBorderColor);
		}

		return style;
	}
	
	private String obtenerPrima(BigDecimal numPrima) {
		return numPrima != null ? String.valueOf(numPrima) : STR_VACIA;
	}

	
	private void fillDatosPaciente(CambioDTO cambioDTO, Sheet sheetCasuistica, int row, CellStyle rowColorStyle,
			CellStyle rowWhiteStyle) {

		if (row == 0) {
			Row r1 = sheetCasuistica.createRow(0);

			Cell cNSS = r1.createCell(1);
			cNSS.setCellValue("NSS");
			cNSS.setCellStyle(rowColorStyle);

			Cell cCURP = r1.createCell(2);
			cCURP.setCellValue("CURP");
			cCURP.setCellStyle(rowColorStyle);

			Cell cNombre = r1.createCell(3);
			cNombre.setCellValue("Nombre(s) y apellidos");
			cNombre.setCellStyle(rowColorStyle);

			Cell del = r1.createCell(4);
			del.setCellValue("Delegación de NSS");
			del.setCellStyle(rowColorStyle);

			Cell subDel = r1.createCell(5);
			subDel.setCellValue("Subdelegación de NSS");
			subDel.setCellStyle(rowColorStyle);

			Cell delAtencion = r1.createCell(6);
			delAtencion.setCellValue("Delegación atención del NSS");
			delAtencion.setCellStyle(rowColorStyle);

			Cell subDelAtencion = r1.createCell(7);
			subDelAtencion.setCellValue("Subdelegación atención del NSS");
			subDelAtencion.setCellStyle(rowColorStyle);

			Cell umfAdscripcion = r1.createCell(8);
			umfAdscripcion.setCellValue("UMF de adscripción");
			umfAdscripcion.setCellStyle(rowColorStyle);

			Cell umfExpedicion = r1.createCell(9);
			umfExpedicion.setCellValue("UMF de expedición");
			umfExpedicion.setCellStyle(rowColorStyle);

			Cell umfPagadora = r1.createCell(10);
			umfPagadora.setCellValue("UMF pagadora");
			umfPagadora.setCellStyle(rowColorStyle);

			Cell salario = r1.createCell(11);
			salario.setCellValue("Salario diario");
			salario.setCellStyle(rowColorStyle);

			Cell ocupacion = r1.createCell(12);
			ocupacion.setCellValue("Ocupación");
			ocupacion.setCellStyle(rowColorStyle);

			Cell codigoError = r1.createCell(13);
			codigoError.setCellValue("Código de error");
			codigoError.setCellStyle(rowColorStyle);

			Cell anioCiclo = r1.createCell(14);
			anioCiclo.setCellValue("Año ciclo");
			anioCiclo.setCellStyle(rowColorStyle);

			Cell casoRegistro = r1.createCell(15);
			casoRegistro.setCellValue("Caso del registro");
			casoRegistro.setCellStyle(rowColorStyle);

			Cell fecInicio = r1.createCell(16);
			fecInicio.setCellValue("Fecha de inicio");
			fecInicio.setCellStyle(rowColorStyle);

			Cell fecAtencion = r1.createCell(17);
			fecAtencion.setCellValue("Fecha de atención");
			fecAtencion.setCellStyle(rowColorStyle);

			Cell fecAccidente = r1.createCell(18);
			fecAccidente.setCellValue("Fecha de accidente");
			fecAccidente.setCellStyle(rowColorStyle);

			Cell fecInicioP = r1.createCell(19);
			fecInicioP.setCellValue("Fecha inicio de pensión");
			fecInicioP.setCellStyle(rowColorStyle);

			Cell fecAlta = r1.createCell(20);
			fecAlta.setCellValue("Fecha de alta");
			fecAlta.setCellStyle(rowColorStyle);

			Cell fecExpedicion = r1.createCell(21);
			fecExpedicion.setCellValue("Fecha de expedición del dictamen");
			fecExpedicion.setCellStyle(rowColorStyle);

			Cell fecFin = r1.createCell(22);
			fecFin.setCellValue("Fecha fin");
			fecFin.setCellStyle(rowColorStyle);

			Cell diasSubsidiados = r1.createCell(23);
			diasSubsidiados.setCellValue("Días subsidiados");
			diasSubsidiados.setCellStyle(rowColorStyle);

			Cell causaExterna = r1.createCell(24);
			causaExterna.setCellValue("Causa externa");
			causaExterna.setCellStyle(rowColorStyle);

			Cell naturaleza = r1.createCell(25);
			naturaleza.setCellValue("Naturaleza");
			naturaleza.setCellStyle(rowColorStyle);

			Cell riesgoFisico = r1.createCell(26);
			riesgoFisico.setCellValue("Riesgo físico");
			riesgoFisico.setCellStyle(rowColorStyle);

			Cell actoInseguro = r1.createCell(27);
			actoInseguro.setCellValue("Acto inseguro");
			actoInseguro.setCellStyle(rowColorStyle);

			Cell tipoRiesgo = r1.createCell(28);
			tipoRiesgo.setCellValue("Tipo de riesgo");
			tipoRiesgo.setCellStyle(rowColorStyle);

			Cell consecuencia = r1.createCell(29);
			consecuencia.setCellValue("Consecuencia");
			consecuencia.setCellStyle(rowColorStyle);

			Cell porcentajeIncapacidad = r1.createCell(30);
			porcentajeIncapacidad.setCellValue("Porcentaje de incapacidad");
			porcentajeIncapacidad.setCellStyle(rowColorStyle);

			Cell tipoIncapacidad = r1.createCell(31);
			tipoIncapacidad.setCellValue("Tipo de incapacidad");
			tipoIncapacidad.setCellStyle(rowColorStyle);

			Cell laudos = r1.createCell(32);
			laudos.setCellValue("Laudos");
			laudos.setCellStyle(rowColorStyle);

			Cell codigoDiagnostico = r1.createCell(33);
			codigoDiagnostico.setCellValue("Código del diagnóstico");
			codigoDiagnostico.setCellStyle(rowColorStyle);

			Cell matriculaMDTratante = r1.createCell(34);
			matriculaMDTratante.setCellValue("Matrícula del médico tratante");
			matriculaMDTratante.setCellStyle(rowColorStyle);

			Cell matriculaMDAutoriza = r1.createCell(35);
			matriculaMDAutoriza.setCellValue("Matrícula del médico que autoriza CDST");
			matriculaMDAutoriza.setCellStyle(rowColorStyle);

			Cell regPatronal = r1.createCell(36);
			regPatronal.setCellValue("Registro patronal");
			regPatronal.setCellStyle(rowColorStyle);

			Cell rfcPatron = r1.createCell(37);
			rfcPatron.setCellValue("RFC patrón");
			rfcPatron.setCellStyle(rowColorStyle);

			Cell razonSocial = r1.createCell(38);
			razonSocial.setCellValue("Razón social");
			razonSocial.setCellStyle(rowColorStyle);

			Cell delRegistroPatronal = r1.createCell(39);
			delRegistroPatronal.setCellValue("Delegación del registro patronal");
			delRegistroPatronal.setCellStyle(rowColorStyle);

			Cell subDelRegistroPatronal = r1.createCell(40);
			subDelRegistroPatronal.setCellValue("Subdelegación del registro patronal");
			subDelRegistroPatronal.setCellStyle(rowColorStyle);

			Cell clase = r1.createCell(41);
			clase.setCellValue("Clase");
			clase.setCellStyle(rowColorStyle);

			Cell fraccion = r1.createCell(42);
			fraccion.setCellValue("Fracción");
			fraccion.setCellStyle(rowColorStyle);

			Cell prima = r1.createCell(43);
			prima.setCellValue("Prima");
			prima.setCellStyle(rowColorStyle);

			Cell estadoRegistro = r1.createCell(44);
			estadoRegistro.setCellValue("Estado del registro");
			estadoRegistro.setCellStyle(rowColorStyle);

			Cell accionRegistro = r1.createCell(45);
			accionRegistro.setCellValue("Acción del registro");
			accionRegistro.setCellStyle(rowColorStyle);

			Cell situacionRegistro = r1.createCell(46);
			situacionRegistro.setCellValue("Situación del registro");
			situacionRegistro.setCellStyle(rowColorStyle);

			Cell fechaCambio = r1.createCell(47);
			fechaCambio.setCellValue("Fecha del cambio");
			fechaCambio.setCellStyle(rowColorStyle);

			Cell horaCambio = r1.createCell(48);
			horaCambio.setCellValue("Hora del cambio");
			horaCambio.setCellStyle(rowColorStyle);

			Cell cuentaUsuario = r1.createCell(49);
			cuentaUsuario.setCellValue("Cuenta de Usuario");
			cuentaUsuario.setCellStyle(rowColorStyle);
		}
		Row r2 = sheetCasuistica.createRow(row + 1);

		Cell cdNSS = r2.createCell(1);
		cdNSS.setCellValue(StringUtils.isEmpty(cambioDTO.getNumNss()) ? ""
				: cambioDTO.getNumNss());
		cdNSS.setCellStyle(rowWhiteStyle);
		Cell cdCURP = r2.createCell(2);
		cdCURP.setCellValue(StringUtils.isEmpty(cambioDTO.getRefCurp()) ? ""
				: cambioDTO.getRefCurp());
		cdCURP.setCellStyle(rowWhiteStyle);

		String nombre = cambioDTO.getNomAsegurado() != null
				? cambioDTO.getNomAsegurado()
				: "";
		String ap = cambioDTO.getRefPrimerApellido() != null
				? cambioDTO.getRefPrimerApellido()
				: "";
		String am = cambioDTO.getRefSegundoApellido() != null
				? cambioDTO.getRefSegundoApellido()
				: "";

		Cell cdNombre = r2.createCell(3);
		cdNombre.setCellValue(nombre + " " + ap + " " + am);
		cdNombre.setCellStyle(rowWhiteStyle);

		Cell dDel = r2.createCell(4);
		dDel.setCellValue(StringUtils.isEmpty(cambioDTO.getDesDelegacionNss()) ? ""
				: cambioDTO.getDesDelegacionNss());
		dDel.setCellStyle(rowWhiteStyle);

		Cell dsubDel = r2.createCell(5);
		dsubDel.setCellValue(StringUtils.isEmpty(cambioDTO.getDesSubDelNss()) ? ""
				: cambioDTO.getDesSubDelNss());
		dsubDel.setCellStyle(rowWhiteStyle);

		Cell ddelAtencion = r2.createCell(6);
		ddelAtencion.setCellValue(StringUtils.isEmpty(cambioDTO.getDesDelegacionAtencion()) ? ""
				: cambioDTO.getDesDelegacionAtencion());
		ddelAtencion.setCellStyle(rowWhiteStyle);

		Cell dsubDelAtencion = r2.createCell(7);
		dsubDelAtencion.setCellValue(StringUtils.isEmpty(cambioDTO.getDesSubDelAtencion()) ? ""
				: cambioDTO.getDesSubDelAtencion());
		dsubDelAtencion.setCellStyle(rowWhiteStyle);

		Cell dumfAdscripcion = r2.createCell(8);
		dumfAdscripcion.setCellValue(StringUtils.isEmpty(cambioDTO.getDesUmfAdscripcion()) ? ""
				: cambioDTO.getDesUmfAdscripcion());
		dumfAdscripcion.setCellStyle(rowWhiteStyle);

		Cell dumfExpedicion = r2.createCell(9);
		dumfExpedicion.setCellValue(StringUtils.isEmpty(cambioDTO.getDesUmfExp()) ? ""
				: cambioDTO.getDesUmfExp());
		dumfExpedicion.setCellStyle(rowWhiteStyle);

		Cell dumfPagadora = r2.createCell(10);
		dumfPagadora.setCellValue(StringUtils.isEmpty(cambioDTO.getDesUmfPagadora()) ? ""
				: cambioDTO.getDesUmfPagadora());
		dumfPagadora.setCellStyle(rowWhiteStyle);

		Cell dsalario = r2.createCell(11);
		dsalario.setCellValue(
				(cambioDTO.getNumSalarioDiario() == null) ? new BigDecimal(0).doubleValue()
						: cambioDTO.getNumSalarioDiario().doubleValue());
		dsalario.setCellStyle(rowWhiteStyle);

		Cell docupacion = r2.createCell(12);
		docupacion.setCellValue(StringUtils.isEmpty(cambioDTO.getDesOcupacion()) ? ""
				: cambioDTO.getDesOcupacion());
		docupacion.setCellStyle(rowWhiteStyle);

		Cell dcodigoError = r2.createCell(13);
		dcodigoError.setCellValue(StringUtils.isEmpty(cambioDTO.getDesCodigoError()) ? ""
				: cambioDTO.getDesCodigoError());
		dcodigoError.setCellStyle(rowWhiteStyle);

		Cell danioCiclo = r2.createCell(14);
		danioCiclo.setCellValue(StringUtils.isEmpty(cambioDTO.getNumCicloAnual()) ? ""
				: cambioDTO.getNumCicloAnual());
		danioCiclo.setCellStyle(rowWhiteStyle);

		Cell dcasoRegistro = r2.createCell(15);
		dcasoRegistro.setCellValue(StringUtils.isEmpty(cambioDTO.getDesCasoRegistro()) ? ""
				: cambioDTO.getDesCasoRegistro());
		dcasoRegistro.setCellStyle(rowWhiteStyle);
		
		Cell dfecInicio = r2.createCell(16);
		dfecInicio.setCellValue(cambioDTO.getFecInicio() != null
				? DateUtils.parserDatetoStringUTC(cambioDTO.getFecInicio(), PATTERN_DD_MM_YYYY)
				: "");
		dfecInicio.setCellStyle(rowWhiteStyle);

		Cell dfecAtencion = r2.createCell(17);
		dfecAtencion.setCellValue(cambioDTO.getFecAtencion() != null
				? DateUtils.parserDatetoStringUTC(cambioDTO.getFecAtencion(), PATTERN_DD_MM_YYYY)
				: "");
		dfecAtencion.setCellStyle(rowWhiteStyle);

		Cell dfecAccidente = r2.createCell(18);
		dfecAccidente.setCellValue(cambioDTO.getFecAccidente() != null
				? DateUtils.parserDatetoStringUTC(cambioDTO.getFecAccidente(), PATTERN_DD_MM_YYYY)
				: "");
		dfecAccidente.setCellStyle(rowWhiteStyle);

		Cell dfecInicioP = r2.createCell(19);
		dfecInicioP.setCellValue(cambioDTO.getFecIniPension() != null
				? DateUtils.parserDatetoStringUTC(cambioDTO.getFecIniPension(), PATTERN_DD_MM_YYYY)
				: "");
		dfecInicioP.setCellStyle(rowWhiteStyle);

		Cell dfecAlta = r2.createCell(20);
		dfecAlta.setCellValue(cambioDTO.getFecAltaIncapacidad() != null
				? DateUtils.parserDatetoStringUTC(cambioDTO.getFecAltaIncapacidad(), PATTERN_DD_MM_YYYY)
				: "");
		dfecAlta.setCellStyle(rowWhiteStyle);

		Cell dfecExpedicion = r2.createCell(21);
		dfecExpedicion.setCellValue(cambioDTO.getFecExpedicionDictamen() != null
				? DateUtils.parserDatetoStringUTC(cambioDTO.getFecExpedicionDictamen(), PATTERN_DD_MM_YYYY)
				: "");
		dfecExpedicion.setCellStyle(rowWhiteStyle);
		
		Cell dfecFin = r2.createCell(22);
		dfecFin.setCellValue(cambioDTO.getFecFin() != null
				? DateUtils.parserDatetoStringUTC(cambioDTO.getFecFin(), PATTERN_DD_MM_YYYY)
				: "");
		dfecFin.setCellStyle(rowWhiteStyle);

		Cell ddiasSubsidiados = r2.createCell(23);
		ddiasSubsidiados.setCellValue(String.valueOf(cambioDTO.getNumDiasSubsidiados() != null
				? cambioDTO.getNumDiasSubsidiados()
				: 0));
		ddiasSubsidiados.setCellStyle(rowWhiteStyle);

		Cell dcausaExterna = r2.createCell(24);
		dcausaExterna.setCellValue(StringUtils.isEmpty(cambioDTO.getDesCausaExterna()) ? ""
				: cambioDTO.getDesCausaExterna());
		dcausaExterna.setCellStyle(rowWhiteStyle);

		Cell dnaturaleza = r2.createCell(25);
		dnaturaleza.setCellValue(StringUtils.isEmpty(cambioDTO.getDesNaturaleza()) ? ""
				: cambioDTO.getDesNaturaleza());
		dnaturaleza.setCellStyle(rowWhiteStyle);

		Cell driesgoFisico = r2.createCell(26);
		driesgoFisico.setCellValue(StringUtils.isEmpty(cambioDTO.getDesRiesgoFisico()) ? ""
				: cambioDTO.getDesRiesgoFisico());
		driesgoFisico.setCellStyle(rowWhiteStyle);

		Cell dactoInseguro = r2.createCell(27);
		dactoInseguro.setCellValue(StringUtils.isEmpty(cambioDTO.getDesActoInseguro()) ? ""
				: cambioDTO.getDesActoInseguro());
		dactoInseguro.setCellStyle(rowWhiteStyle);

		Cell dtipoRiesgo = r2.createCell(28);
		dtipoRiesgo.setCellValue(StringUtils.isEmpty(cambioDTO.getDesTipoRiesgo()) ? ""
				: cambioDTO.getDesTipoRiesgo());
		dtipoRiesgo.setCellStyle(rowWhiteStyle);

		String descConsecuencia = StringUtils.isEmpty(cambioDTO.getDesConsecuencia()) ? ""
				: cambioDTO.getDesConsecuencia();
		if(!StringUtils.isEmpty(cambioDTO.getDesConsecuencia()) &&
				!StringUtils.isEmpty(cambioDTO.getCveConsecuencia())) {
			if(String.valueOf(cambioDTO.getCveConsecuencia()).equals("6")) {
				descConsecuencia = "Con Valuación inicial provisional posterior a la fecha de alta";
			}
		}
		
		Cell dconsecuencia = r2.createCell(29);
		dconsecuencia.setCellValue(descConsecuencia);
		dconsecuencia.setCellStyle(rowWhiteStyle);

		Cell dporcentajeIncapacidad = r2.createCell(30);
		dporcentajeIncapacidad.setCellValue(
				(cambioDTO.getPorcentajeIncapacidad() == null) ? new BigDecimal(0).doubleValue()
						: cambioDTO.getPorcentajeIncapacidad().doubleValue());
		dporcentajeIncapacidad.setCellStyle(rowWhiteStyle);

		Cell dtipoIncapacidad = r2.createCell(31);
		dtipoIncapacidad.setCellValue(StringUtils.isEmpty(cambioDTO.getDesTipoIncapacidad()) ? ""
				: cambioDTO.getDesTipoIncapacidad());
		dtipoIncapacidad.setCellStyle(rowWhiteStyle);

		Cell dlaudos = r2.createCell(32);
		dlaudos.setCellValue(StringUtils.isEmpty(cambioDTO.getDesLaudo()) ? ""
				: cambioDTO.getDesLaudo());
		dlaudos.setCellStyle(rowWhiteStyle);

		Cell dcodigoDiagnostico = r2.createCell(33);
		dcodigoDiagnostico.setCellValue(StringUtils.isEmpty(cambioDTO.getDesCodigoDiagnostico()) ? ""
				: cambioDTO.getDesCodigoDiagnostico());
		dcodigoDiagnostico.setCellStyle(rowWhiteStyle);

		Cell dmatriculaMDTratante = r2.createCell(34);
		dmatriculaMDTratante.setCellValue(StringUtils.isEmpty(cambioDTO.getNumMatMedTratante()) ? ""
				: cambioDTO.getNumMatMedTratante());
		dmatriculaMDTratante.setCellStyle(rowWhiteStyle);

		Cell dmatriculaMDAutoriza = r2.createCell(35);
		dmatriculaMDAutoriza.setCellValue(StringUtils.isEmpty(cambioDTO.getNumMatMedAutCdst()) ? ""
				: cambioDTO.getNumMatMedAutCdst());
		dmatriculaMDAutoriza.setCellStyle(rowWhiteStyle);

		// Datos del patron

		Cell dregPatronal = r2.createCell(36);
		dregPatronal.setCellValue(StringUtils.isEmpty(cambioDTO.getRefRegistroPatronal()) ? ""
				: cambioDTO.getRefRegistroPatronal());
		dregPatronal.setCellStyle(rowWhiteStyle);

		Cell drfcPatron = r2.createCell(37);
		drfcPatron.setCellValue(
				StringUtils.isEmpty(cambioDTO.getDesRfc()) ? "" : cambioDTO.getDesRfc());
		drfcPatron.setCellStyle(rowWhiteStyle);

		Cell drazonSocial = r2.createCell(38);
		drazonSocial.setCellValue(StringUtils.isEmpty(cambioDTO.getDesRazonSocial()) ? ""
				: cambioDTO.getDesRazonSocial());
		drazonSocial.setCellStyle(rowWhiteStyle);

		Cell ddelRegistroPatronal = r2.createCell(39);
		ddelRegistroPatronal.setCellValue(StringUtils.isEmpty(cambioDTO.getDesDelRegPatronal()) ? ""
				: cambioDTO.getDesDelRegPatronal());
		ddelRegistroPatronal.setCellStyle(rowWhiteStyle);

		Cell dsubDelRegistroPatronal = r2.createCell(40);
		dsubDelRegistroPatronal.setCellValue(StringUtils.isEmpty(cambioDTO.getDesSubDelRegPatronal()) ? ""
				: cambioDTO.getDesSubDelRegPatronal());
		dsubDelRegistroPatronal.setCellStyle(rowWhiteStyle);

		Cell dclase = r2.createCell(41);
		dclase.setCellValue(
				StringUtils.isEmpty(cambioDTO.getDesClase()) ? "" : cambioDTO.getDesClase());
		dclase.setCellStyle(rowWhiteStyle);

		Cell dfraccion = r2.createCell(42);
		dfraccion.setCellValue(StringUtils.isEmpty(cambioDTO.getDesFraccion()) ? ""
				: cambioDTO.getDesFraccion());
		dfraccion.setCellStyle(rowWhiteStyle);

		Cell dprima = r2.createCell(43);
		dprima.setCellValue(
				StringUtils.isEmpty(cambioDTO.getDesPrima()) ? 
						obtenerPrima(cambioDTO.getNumPrima()) : cambioDTO.getDesPrima());
		dprima.setCellStyle(rowWhiteStyle);

		// Datos del registro

		Cell destadoRegistro = r2.createCell(44);
		destadoRegistro.setCellValue(StringUtils.isEmpty(cambioDTO.getDesEstadoRegistro()) ? ""
				: cambioDTO.getDesEstadoRegistro());
		destadoRegistro.setCellStyle(rowWhiteStyle);

		String accion = "";
		String fechaCambio = "";
		String horaCambio = "";
		String cuentaUsuario = "";

		if (CollectionUtils.isNotEmpty(cambioDTO.getAuditorias())) {
			Optional<AuditoriaDTO> aud = cambioDTO.getAuditorias().stream().filter(a->a.getFecBaja() == null).findFirst();
			if(aud.isPresent()) {
				accion = aud.get().getDesAccionRegistro();

				fechaCambio = aud.get().getFecAlta() != null ? 
						DateUtils.parserDatetoString(aud.get().getFecAlta(), PATTERN_DD_MM_YYYY): "";
				
				horaCambio = aud.get().getFecAlta() != null
						? DateUtils.parserDatetoString(aud.get().getFecAlta(), PATTERN_HH_MM) : "";
				
				cuentaUsuario = StringUtils.isEmpty(aud.get().getNomUsuario()) ? "" : aud.get().getNomUsuario();
				
			}
		}
		
		if(cuentaUsuario.isEmpty()) {
			cuentaUsuario = StringUtils.isEmpty(cambioDTO.getUsuarioModificador()) ? "" 
					: cambioDTO.getUsuarioModificador();
		}
		
		if(fechaCambio.isEmpty()) {
			fechaCambio = cambioDTO.getFecActualizacion() != null ?
					DateUtils.parserDatetoString(cambioDTO.getFecActualizacion(), PATTERN_DD_MM_YYYY) : "";
		}
		
		if(horaCambio.isEmpty()) {
			horaCambio = cambioDTO.getFecActualizacion() != null ?
					DateUtils.parserDatetoString(cambioDTO.getFecActualizacion(), PATTERN_HH_MM) : "";
		}

		Cell daccionRegistro = r2.createCell(45);
		daccionRegistro.setCellValue(accion);
		daccionRegistro.setCellStyle(rowWhiteStyle);

		Cell dsituacionRegistro = r2.createCell(46);
		dsituacionRegistro.setCellValue(
				StringUtils.isEmpty(cambioDTO) ? "" : cambioDTO.getDesSituacionRegistro());
		dsituacionRegistro.setCellStyle(rowWhiteStyle);

		Cell dfechaCambio = r2.createCell(47);
		dfechaCambio.setCellValue(fechaCambio);
		dfechaCambio.setCellStyle(rowWhiteStyle);

		Cell dhoraCambio = r2.createCell(48);
		dhoraCambio.setCellValue(horaCambio);
		dhoraCambio.setCellStyle(rowWhiteStyle);

		Cell dcuentaUsuario = r2.createCell(49);
		dcuentaUsuario.setCellValue(cuentaUsuario);
		dcuentaUsuario.setCellStyle(rowWhiteStyle);

	}
	
	
}
