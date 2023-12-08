package mx.gob.imss.cit.pmc.cambios.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.validation.Valid;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import mx.gob.imss.cit.mspmccommons.dto.ErrorResponse;
import mx.gob.imss.cit.mspmccommons.dto.MovimientoRequest;
import mx.gob.imss.cit.mspmccommons.dto.ReporteCasuisticaInputDTO;
import mx.gob.imss.cit.mspmccommons.enums.EnumHttpStatus;
import mx.gob.imss.cit.mspmccommons.exception.BusinessException;
import mx.gob.imss.cit.mspmccommons.model.ModelVersion;
import mx.gob.imss.cit.mspmccommons.resp.DetalleRegistroResponse;
import mx.gob.imss.cit.pmc.cambios.service.PmcCambiosService;
import mx.gob.imss.cit.pmc.cambios.service.ReporteService;
import net.sf.jasperreports.engine.JRException;

@RestController
@Api(value = "Guardado de registro con cambio PMC", tags = { "Guardado de registro con cambio PMC Rest" })
@RequestMapping("/mscambios/v1")
public class MsPmcCambiosController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String GENERAL_ERROR = "Error de aplicación";

	@Autowired
	private PmcCambiosService pmcCambiosService;

	@Autowired
	private ReporteService reporteService;
	
	private final static String VERSION_SERVICE = "mspmc-cambios-1.0.11";
	
	private final static String FOLIO_SERVICE = "INC112455";
	
	private final static String NOTA_SERVICE = "Optimizar reporte v1.1";

	@RequestMapping("/health/ready")
	@ResponseStatus(HttpStatus.OK)
	public void ready() {
		// Service to validate if the server is ready
	}

	@RequestMapping("/health/live")
	@ResponseStatus(HttpStatus.OK)
	public void live() {
		// Service to validate if the server is alive
	}
	
	@ApiOperation(value = "Guardado de registro con cambio", nickname = "validarLocal", notes = "Guardado de registro con cambio", response = Object.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = Object.class, responseContainer = "List"),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(value = "/insertar", produces = MediaType.APPLICATION_JSON_VALUE)
	public Object validarLocal(@RequestBody DetalleRegistroResponse input) {
		Object resultado = null;
		try {
			logger.info("MspmcCambiosController:insertar:try {}", input);
			input = pmcCambiosService.guardarCambio(input);

			input.setObjectIdOrigen(input.getObjectIdArchivoDetalle().toString());
			resultado = new ResponseEntity<Object>(input, HttpStatus.OK);

			logger.info("MspmcCambiosController:insertar:returnOk");
		} catch (BusinessException be) {
			resultado = buildBusinessException(be, "MspmcCambiosController:insertar:catch",
					"MspmcCambiosController:insertar:numberHTTPDesired: {}");
		}
		logger.info("MspmcCambiosController:insertar:FinalReturn");
		return resultado;
	}

	@ApiOperation(value = "Eliminar registro ", nickname = "Eliminar", notes = "Eliminado de movimiento/casuistica", response = Object.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = Object.class, responseContainer = "List"),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(value = "/eliminar", produces = MediaType.TEXT_PLAIN_VALUE)
	public Object eliminar(@RequestBody DetalleRegistroResponse input) {
		Object resultado = null;
		try {
			logger.info("MspmcCambiosController:eliminarMovimiento:try {}", input);
			pmcCambiosService.eliminarMovimiento(input);
			String responseBody = "Movimiento eliminado.";
			resultado = ResponseEntity.ok().body(responseBody);

		} catch (BusinessException be) {
			resultado = buildBusinessException(be, "MspmcCambiosController:eliminarMovimiento:catch",
					"MspmcCambiosController:eliminarMovimiento:numberHTTPDesired: {}");
		}

		logger.info("MspmcCambiosController:eliminarMovimiento:EliminarReturn");
		return resultado;
	}

	@ApiOperation(value = "Guardado de registro con cambio", nickname = "validarLocal", notes = "Guardado de registro con cambio", response = Object.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = Object.class, responseContainer = "List"),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(value = "/insertarNuevo", produces = MediaType.APPLICATION_JSON_VALUE)
	public Object insertarNuevo(@RequestBody DetalleRegistroResponse input) {
		Object resultado = null;
		try {
			logger.info("MspmcCambiosController:inserarNuevo:try {}", input);
			pmcCambiosService.guardarCambioNuevo(input);
			input.setObjectIdOrigen(input.getObjectIdArchivoDetalle().toString());
			resultado = new ResponseEntity<Object>(input, HttpStatus.OK);
			logger.info("MspmcCambiosController:inserarNuevo:returnOk");
		} catch (BusinessException be) {
			resultado = buildBusinessException(be, "MspmcCambiosController:inserarNuevo:catch",
					"MspmcCambiosController:inserarNuevo:numberHTTPDesired: {}");
		}

		logger.info("MspmcCambiosController:inserarNuevo:FinalReturn");
		return resultado;
	}

	@ApiOperation(value = "Guardado de registro suseptible a cambio ", nickname = "validarLocal", notes = "Guardado de registro suspectible a cambiar ", response = Object.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = Object.class, responseContainer = "List"),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(value = "/insertarNuevoSus", produces = MediaType.APPLICATION_JSON_VALUE)
	public Object insertarNuevoSus(@RequestBody DetalleRegistroResponse input) {
		Object resultado = null;
		try {
			
			input.getAseguradoDTO().setCveOcupacion(regresarVacio(input.getAseguradoDTO().getCveOcupacion()));
			input.getAseguradoDTO().setDesOcupacion(regresarVacio(input.getAseguradoDTO().getDesOcupacion()));
			
			input.getIncapacidadDTO().setDesActoInseguro(regresarVacio(input.getIncapacidadDTO().getDesActoInseguro()));
			input.getIncapacidadDTO().setNumRiesgoFisico(regresarVacio(input.getIncapacidadDTO().getNumRiesgoFisico()));
			input.getIncapacidadDTO().setNumActoInseguro(regresarVacio(input.getIncapacidadDTO().getNumActoInseguro()));
			input.getIncapacidadDTO().setNumMatMedAutCdst(regresarVacio(input.getIncapacidadDTO().getNumMatMedAutCdst()));
			input.getIncapacidadDTO().setNumMatMedTratante(regresarVacio(input.getIncapacidadDTO().getNumMatMedTratante()));
			
			logger.info("MspmcCambiosController:insertarNuevoSus:try {}", input);
			logger.info("_____________  insertarNuevoSus");
			pmcCambiosService.guardarCambioNuevoSus(input,input.getOrigenPantalla());
			input.setObjectIdOrigen(input.getObjectIdArchivoDetalle().toString());
			resultado = new ResponseEntity<Object>(input, HttpStatus.OK);
			logger.info("MspmcCambiosController:insertarNuevoSus:returnOk");
		} catch (BusinessException be) {
			resultado = buildBusinessException(be, "MspmcCambiosController:insertarNuevoSus:catch",
					"MspmcCambiosController:insertarNuevoSus:numberHTTPDesired: {}");
		}

		logger.info("MspmcCambiosController:buscarEstadoArchivo:FinalReturn");
		return resultado;
	}
	
	
	
	String regresarVacio(String combo) {
		String valorRetorno=combo;
		if(combo != null ) {
		if(combo.equals("-1") || combo.equals("")) {
			valorRetorno=null;
		}
		}else {
			valorRetorno=null;
		}
		return valorRetorno;
		
	}

	@ApiOperation(value = "Guardado de registro suseptible a cambio ", nickname = "validarLocal", notes = "Guardado de registro suspectible a cambiar ", response = Object.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = Object.class, responseContainer = "List"),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(value = "/modificarCambiosSus", produces = MediaType.APPLICATION_JSON_VALUE)
	public Object modificarCambiosSus(@RequestBody DetalleRegistroResponse input) {
		Object resultado = null;
		try {
			logger.info("MspmcCambiosController:modificarCambiosSus:try [{}]", input.toString());
			logger.info("_____________  modificarCambiosSus");
			String pantalla = "";
			pmcCambiosService.guardarCambioNuevoSus(input,pantalla);
			input.setObjectIdOrigen(input.getObjectIdArchivoDetalle().toString());
			resultado = new ResponseEntity<Object>(input, HttpStatus.OK);
			logger.info("MspmcCambiosController:modificarCambiosSus:returnOk");
		} catch (BusinessException be) {
			logger.info("MspmcCambiosController:modificarCambiosSus:catch");
			ErrorResponse errorResponse = new ErrorResponse(EnumHttpStatus.SERVER_ERROR_INTERNAL, be.getMessage(),
					GENERAL_ERROR);
			int numberHTTPDesired = Integer.parseInt(errorResponse.getCode());

			resultado = new ResponseEntity<>(errorResponse, HttpStatus.valueOf(numberHTTPDesired));
			logger.info("MspmcCambiosController:modificarCambiosSus:numberHTTPDesired");

		}

		logger.info("MspmcCambiosController:modificarCambiosSus:FinalReturn");
		return resultado;
	}
	
	@ApiOperation(value = "Busqueda de cambios y movimientos", nickname = "buscarMovimientosYCambios", notes = "Busqueda de movimientos y cambios", response = Object.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = Object.class, responseContainer = "List"),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping(value = "/cambiosMovimientos", produces = MediaType.APPLICATION_JSON_VALUE)
	public Object buscarCambiosMovimientos(
			@ApiParam(value = "Clave de la Delegacion") @Valid @RequestParam(value = "cveDelegacion", required = false) Integer cveDelegacion,
			@ApiParam(value = "Clave de la subdelegacion") @Valid @RequestParam(value = "cveSubdelegacion", required = false) Integer cveSubdelegacion,
			@ApiParam(value = "Clave Tipo Riesgo") @Valid @RequestParam(value = "cveTipoRiesgo", required = false) Integer cveTipoRiesgo,
			@ApiParam(value = "Clave consecuencia") @Valid @RequestParam(value = "cveConsecuencia", required = false) Integer cveConsecuencia,
			@ApiParam(value = "Clave caso del regisgtro") @Valid @RequestParam(value = "cveCasoRegistro", required = false) Integer cveCasoRegistro,
			@ApiParam(value = "Dia inicio consulta") @Valid @RequestParam(value = "fromDay", required = false) String fromDay,
			@ApiParam(value = "Mes inicio consulta") @Valid @RequestParam(value = "fromMonth", required = false) String fromMonth,
			@ApiParam(value = "Anio inicio consulta") @Valid @RequestParam(value = "fromYear", required = false) String fromYear,
			@ApiParam(value = "Dia fin consulta") @Valid @RequestParam(value = "toDay", required = false) String toDay,
			@ApiParam(value = "Mes fin consulta") @Valid @RequestParam(value = "toMonth", required = false) String toMonth,
			@ApiParam(value = "Anio fin consulta") @Valid @RequestParam(value = "toYear", required = false) String toYear,
			@ApiParam(value = "Lista de claves del estado del registro") @Valid @RequestParam(value = "cveEstadoRegistroList", required = false) List<Integer> cveEstadoRegistroList,
			@ApiParam(value = "NSS") @Valid @RequestParam(value = "numNss", required = false) String numNss,
			@ApiParam(value = "Registro Patronal") @Valid @RequestParam(value = "refRegistroPatronal", required = false) String refRegistroPatronal,
			@ApiParam(value = "Situacion del registro") @Valid @RequestParam(value = "cveSituacionRegistro", required = false) String cveSituacionRegistro,
			@ApiParam(value = "Accion del registro") @Valid @RequestParam(value = "cveIdAccionRegistro", required = false) Integer cveIdAccionRegistro,
			@ApiParam(value = "RFC Patron") @Valid @RequestParam(value = "rfc", required = false) String rfc,
			@ApiParam(value = "Clase patron") @Valid @RequestParam(value = "cveClase", required = false) Integer cveClase,
			@ApiParam(value = "Fraccion patron") @Valid @RequestParam(value = "cveFraccion", required = false) Integer cveFraccion,
			@ApiParam(value = "Laudo incapacidad") @Valid @RequestParam(value = "cveLaudo", required = false) Integer cveLaudo, 
			@ApiParam(value = "Pagina que se desea consultar") 
			@Valid @RequestParam(value = "page", required = false) Long page,
			@ApiParam(value = "Elementos totales de la busqueda, usado para saber si ejecutar el conteo o no") 
			@Valid @RequestParam(value = "totalElements", required = false) Long totalElements,
			@ApiParam(value = "Origen del alta dependiendo del perfil") @Valid @RequestParam(value = "origenAlta", required = false) String origenAlta,
			@ApiParam(value = "Numero total de elementos de movimientos")
			@Valid @RequestParam(value = "totalElementsMovement", required = false) Long totalElementsMovement,
			@ApiParam(value = "Numero de registros que tienen nss menor que el menor de movimientos")
			@Valid @RequestParam(value = "changesMinorThanMovements", required = false) Long changesMinorThanMovements,
			@ApiParam(value = "Bandera que indica que tipo de usuario es el que realiza la busqueda")
			@Valid @RequestParam(value = "isOperative", required = false) Boolean isOperative,
			@ApiParam(value = "Bandera que indica si el usuario que realiza la busqueda es aprovador")
			@Valid @RequestParam(value = "isOperative", required = false) Boolean isApprover,
			@ApiParam(value = "Bandera que indica si la consulta se lanzo desde la busqueda de casuistica")
			@Valid @RequestParam(value = "isCasuistry", required = false) Boolean isCasuistry) {
		Object resultado = null;
		try {
			Object findMovimientos = pmcCambiosService.findCambiosMovimientos(cveDelegacion, cveSubdelegacion, cveTipoRiesgo,
					cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList, numNss,
					refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc, cveClase, cveFraccion,
					cveLaudo, page, totalElements, origenAlta, totalElementsMovement, changesMinorThanMovements, isOperative,
					isApprover, isCasuistry);
			if (findMovimientos != null) {
				return new ResponseEntity<>(findMovimientos, HttpStatus.PARTIAL_CONTENT);
			} else {
				return new ResponseEntity<>(findMovimientos, HttpStatus.NO_CONTENT);
			}
		} catch (BusinessException be) {
			logger.info("MspmcCambiosController:cambiosMovimientos:catch");
			ErrorResponse errorResponse = new ErrorResponse(EnumHttpStatus.SERVER_ERROR_INTERNAL, be.getMessage(),
					GENERAL_ERROR);

			int numberHTTPDesired = Integer.parseInt(errorResponse.getCode());

			resultado = new ResponseEntity<>(errorResponse, HttpStatus.valueOf(numberHTTPDesired));
			logger.info("MspmcCambiosController:cambiosMovimientos:numberHTTPDesired");

		}

		logger.info("MspmcCambiosController:cambiosMovimientos:FinalReturn");
		return resultado;
	}
	
	@ApiOperation(value = "reporteGeneralPdf", nickname = "reporteGeneralPdf", notes = "reporteGeneralPdf", response = Object.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = ResponseEntity.class, responseContainer = "String"),
			@ApiResponse(code = 204, message = "Sin resultados", response = ResponseEntity.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(value = "/reporteGeneralPdf", produces = MediaType.APPLICATION_JSON_VALUE)
	public Object reporteGeneralPdf(@RequestBody ReporteCasuisticaInputDTO input) {
		Object respuesta = null;
		try {
			respuesta = reporteService.getGeneralCasuisticaReportPdf(input);
		} catch (JRException | IOException | BusinessException e) {

			logger.info("MspmcCambiosController:reporteGeneralPdf:catch");
			ErrorResponse errorResponse = new ErrorResponse(EnumHttpStatus.SERVER_ERROR_INTERNAL, e.getMessage(),
					GENERAL_ERROR);

			int numberHTTPDesired = Integer.parseInt(errorResponse.getCode());

			respuesta = new ResponseEntity<>(errorResponse, HttpStatus.valueOf(numberHTTPDesired));
			logger.info("MspmcCambiosController:reporteGeneralPdf:numberHTTPDesired");

		}
		return respuesta;
	}
	
	@ApiOperation(value = "reporteGeneralExcel", nickname = "reporteGeneralExcel", notes = "reporteGeneralExcel", response = Object.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = String.class, responseContainer = "String"),
			@ApiResponse(code = 204, message = "Sin resultados", response = ResponseEntity.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(value = "/reporteGeneralExcel", produces = MediaType.APPLICATION_JSON_VALUE)
	public Object reporteGeneralExcel(@RequestBody ReporteCasuisticaInputDTO input) {
		Object respuesta = null;
		logger.debug("getDetallesReportXls service ready to return");

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Disposition", "attachment; filename=consultaCasuistica.xlsx");
			headers.add("Content-Type", "application/vnd.ms-excel;charset=UTF-8");
			Workbook wb = reporteService.getGeneralCasuisticaReportXls(input);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			bos.close();
			wb.close();

			respuesta = new ResponseEntity<Object>(bos.toByteArray(), HttpStatus.OK);
			return respuesta;

		} catch (IOException | BusinessException | JRException e) {

			logger.info("MspmcCambiosController:reporteGeneralExcel:catch");
			ErrorResponse errorResponse = new ErrorResponse(EnumHttpStatus.SERVER_ERROR_INTERNAL, e.getMessage(),
					GENERAL_ERROR);

			int numberHTTPDesired = Integer.parseInt(errorResponse.getCode());

			respuesta = new ResponseEntity<>(errorResponse, HttpStatus.valueOf(numberHTTPDesired));
			logger.info("MspmcCambiosController:reporteGeneralExcel:numberHTTPDesired");

		}

		return respuesta;
	}

	@ApiOperation(value = "Consulta detalle movimientos", nickname = "detallemovimientossGet", notes = "Consulta movimientos", response = ResponseEntity.class, responseContainer = "List",tags={  })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Respuesta exitosa", response = ResponseEntity.class, responseContainer = "List"),
        @ApiResponse(code = 204, message = "Sin resultados", response = ResponseEntity.class),
        @ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders="*")
	@GetMapping(value = "/detallemovimientos", produces = { "application/json" })
	public Object movimientoDetalle(
			@ApiParam(value = "ObjectId")
			@Valid @RequestParam(value = "objectId", required = true) String objectId,
			@Valid @RequestParam(value = "numNss", required = true) String numNss,
			@ApiParam(value = "Posicion detalle registro")
			@Valid @RequestParam(value = "position", required = false) Integer position,
			@ApiParam(value = "Num Folio Movimiento Original")
			@Valid @RequestParam(value = "numFolioMovtoOriginal", required = false) String numFolioMovtoOriginal ) {

		Object responseEntity;

		try {

				Object findMovimientos = pmcCambiosService.getDetalleMovimiento(objectId, numNss, position, numFolioMovtoOriginal);
				if (findMovimientos != null) {
					return new ResponseEntity<>(findMovimientos, HttpStatus.OK);
				}else {
					return new ResponseEntity<>(HttpStatus.NO_CONTENT);
				}
		} catch (BusinessException be) {
			responseEntity = buildBusinessException(be, "MspmcCambiosController:detallemovimientos:catch",
					"MspmcCambiosController:detallemovimientos:numberHTTPDesired: {}");
		}
		return responseEntity;
     }


	@ApiOperation(value = "Actualizar Registro", nickname = "actualizaRegistro", notes = "Actualiza Registro", response = ResponseEntity.class, responseContainer = "List",tags={  })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Respuesta exitosa", response = ResponseEntity.class, responseContainer = "List"),
        @ApiResponse(code = 204, message = "Sin resultados", response = ResponseEntity.class),
        @ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders="*")
	@PostMapping(value = "/cambios", produces = { "application/json" })
	public Object actualizaMovimiento(@RequestBody MovimientoRequest input) {
		Object responseEntity;
		try {
			
				Object findMovimientos = pmcCambiosService.updateCambio(input);
				if (findMovimientos!=null) {
					return new ResponseEntity<>("Registro actualizado correctamente", HttpStatus.OK);
				}else {
					return new ResponseEntity<>("El Registro se encuentra en un estatus que no puede ser aprobado ni rechazado ", HttpStatus.CONFLICT);
				}
		} catch (BusinessException be) {
			responseEntity = buildBusinessException(be, "MspmcCambiosController:cambios:catch",
					"MspmcCambiosController:cambios:numberHTTPDesired: {}");
		}
		return responseEntity;

	}

	@ApiOperation(value = "Guardar sin cambios", nickname = "guardarSinCambios", notes = "Guardar sin cambios", response = ResponseEntity.class, responseContainer = "String", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = ResponseEntity.class, responseContainer = "String"),
			@ApiResponse(code = 204, message = "Sin resultados", response = ResponseEntity.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(value = "/guardarSinCambios", produces = { "application/json" })
	public ResponseEntity<String> guardarSinCambios(@RequestBody MovimientoRequest input) {
		try {
			if (pmcCambiosService.guardarSinCambios(input)) {
				return ResponseEntity.ok("{\"resultado\": \"Registro actualizado correctamente\"}");
			} else {
				return ResponseEntity.noContent().build();
			}
		} catch (BusinessException be) {
			ResponseEntity.notFound().build();
		}
		return ResponseEntity.notFound().build();

	}
	
	@PostMapping(value = "/aprobarSinCambios", produces = { "application/json" })
	public Object aprobarSinCambios(@RequestBody MovimientoRequest input) {
		Object responseEntity;
		try {
				Object findMovimientos = pmcCambiosService.aprobarSinCambios(input);
				if (findMovimientos!=null) {
					return new ResponseEntity<>("{\"resultado\": \"Registro actualizado correctamente\"}", HttpStatus.OK);
				}else {
					return new ResponseEntity<>("{\"resultado\": \"El Registro se encuentra en un estatus que no puede ser aprobado ni rechazado\"} ", HttpStatus.CONFLICT);
				}
		} catch (BusinessException be) {
			responseEntity = buildBusinessException(be, "MspmcCambiosController:cambios:catch",
					"MspmcCambiosController:cambios:numberHTTPDesired: {}");
		}
		return responseEntity;

	}
	
	@ApiOperation(value = "Reporte PDF consulta casuistica", nickname = "Reporte PDF consulta casuistica", notes = "Reporte PDF consulta casuistica", response = ResponseEntity.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = ResponseEntity.class, responseContainer = "String"),
			@ApiResponse(code = 204, message = "Sin resultados", response = ResponseEntity.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping("/reporteDetallepdf")
	public Object getDetallesReportPdf(@RequestBody ReporteCasuisticaInputDTO input) {
		String respuesta = null;
		try {
			respuesta = reporteService.getCasuisticaReport(input);
		} catch (JRException | IOException | BusinessException e) {
			logger.error(e.getMessage());
		}
		return respuesta;
	}

	@ApiOperation(value = "Reporte XLS consulta casuistica", nickname = "Reporte XLS consulta casuistica", notes = "Reporte XLS consulta casuistica", response = ResponseEntity.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = ResponseEntity.class, responseContainer = "Object"),
			@ApiResponse(code = 204, message = "Sin resultados", response = ResponseEntity.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping("/reporteDetallexls")
	public Object getDetallesReportXls(@RequestBody ReporteCasuisticaInputDTO input) {
		Object respuesta = null;
		logger.debug("getDetallesReportXls service ready to return");

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Disposition", "attachment; filename=consultaCasuistica.xlsx");
			headers.add("Content-Type", "application/vnd.ms-excel;charset=UTF-8");
			Workbook wb = reporteService.getCasuisticaReportXls(input);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			bos.close();
	        wb.close();

			respuesta = new ResponseEntity<Object>(bos.toByteArray(), HttpStatus.OK);
			return respuesta;

		} catch (IOException | BusinessException | JRException e) {
			logger.error(e.getMessage());
		}

		return respuesta;
	}

	private Object buildBusinessException(BusinessException be, String message1, String message2) {
		logger.info(message1);
		ErrorResponse errorResponse = new ErrorResponse(EnumHttpStatus.SERVER_ERROR_INTERNAL, be.getMessage(), GENERAL_ERROR);
		int numberHTTPDesired = Integer.parseInt(errorResponse.getCode());
		logger.info(message2, numberHTTPDesired);
		return new ResponseEntity<>(errorResponse, HttpStatus.valueOf(numberHTTPDesired));
	}
	
	@ApiOperation(value = "version", nickname = "version", notes = "version", response = Object.class, responseContainer = "binary", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = ResponseEntity.class, responseContainer = "byte"),
			@ApiResponse(code = 204, message = "Sin resultados", response = ResponseEntity.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping(value = "/version")
	public ModelVersion version() throws Exception {
		return new ModelVersion(VERSION_SERVICE, FOLIO_SERVICE, NOTA_SERVICE);
	}
	
	@ApiOperation(value = "versionCommons", nickname = "versionCommons", notes = "versionCommons", response = Object.class, responseContainer = "binary", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = ResponseEntity.class, responseContainer = "byte"),
			@ApiResponse(code = 204, message = "Sin resultados", response = ResponseEntity.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping(value = "/versionCommons")
	public ModelVersion versionCommons() throws Exception {
		return new ModelVersion();
	}

}
