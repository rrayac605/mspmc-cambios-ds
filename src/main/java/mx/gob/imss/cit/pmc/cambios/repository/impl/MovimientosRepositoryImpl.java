package mx.gob.imss.cit.pmc.cambios.repository.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.CountOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SetOperators.SetOperatorFactory;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import io.micrometer.core.instrument.util.StringUtils;
import mx.gob.imss.cit.mspmccommons.dto.AuditoriaDTO;
import mx.gob.imss.cit.mspmccommons.dto.CambioDTO;
import mx.gob.imss.cit.mspmccommons.dto.DetalleRegistroDTO;
import mx.gob.imss.cit.mspmccommons.dto.MovimientosOutputDTO;
import mx.gob.imss.cit.mspmccommons.enums.AccionRegistroEnum;
import mx.gob.imss.cit.mspmccommons.enums.EstadoRegistroEnum;
import mx.gob.imss.cit.mspmccommons.integration.model.CountDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.DatosModificadosDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.ResponseDTO;
import mx.gob.imss.cit.mspmccommons.utils.AggregationUtils;
import mx.gob.imss.cit.mspmccommons.utils.CustomAggregationOperation;
import mx.gob.imss.cit.mspmccommons.utils.DateUtils;
import mx.gob.imss.cit.mspmccommons.utils.ObjectUtils;
import mx.gob.imss.cit.mspmccommons.utils.PaginationUtils;
import mx.gob.imss.cit.mspmccommons.utils.Utils;
import mx.gob.imss.cit.pmc.cambios.repository.MovimientosRepository;
import mx.gob.imss.cit.pmc.cambios.repository.ParametroRepository;

@Repository("movimientosService")
public class MovimientosRepositoryImpl implements MovimientosRepository {

	private static final Logger logger = LoggerFactory.getLogger(MovimientosRepositoryImpl.class);

	private static final String RFC_IMSS = "IMS421231I45";

	@Autowired
	MongoOperations mongoOperations;
	
	@Autowired
	ParametroRepository parametroRepository;

	@Override
	public DetalleRegistroDTO getDetalleMovimiento(@Valid String objectId, @Valid String numNss, @Valid Integer position,
			@Valid String numFolioMovtoOriginal) {
		logger.debug("getDetalleMovimiento");
		// Se construye la agregacion
		MatchOperation match = Aggregation.match(Criteria.where("_id").is(new ObjectId(objectId)));
		LookupOperation lookup = Aggregation.lookup("MCT_ARCHIVO", "identificadorArchivo", "_id", "archivoDTO");
		UnwindOperation unwind = Aggregation.unwind("archivoDTO");
		TypedAggregation<DetalleRegistroDTO> aggregation = Aggregation.newAggregation(DetalleRegistroDTO.class, Arrays.asList(
				match, lookup, unwind));
		AggregationResults<DetalleRegistroDTO> aggregationResults = mongoOperations.aggregate(aggregation, DetalleRegistroDTO.class);
		DetalleRegistroDTO detalle = aggregationResults.getUniqueMappedResult();
		detalle.setFecProcesoCarga(detalle.getArchivoDTO().getFecProcesoCarga());
		detalle.setFecProcesoCarga(detalle.getArchivoDTO().getFecProcesoCarga());
		detalle.getAseguradoDTO()
				.setDesCodigoError(detalle.getAseguradoDTO().getCveCodigoError());
		detalle.setObjectIdOrigen(detalle.getObjectIdArchivoDetalle() != null ? detalle.getObjectIdArchivoDetalle().toString() : null);

		MatchOperation matchCambios = Aggregation.match(Criteria.where("objectIdOrigen").is(new ObjectId(objectId)));
		TypedAggregation<CambioDTO> aggregationCambios = Aggregation.newAggregation(CambioDTO.class,
				Collections.singletonList(matchCambios));
		AggregationResults<CambioDTO> aggregationResultsCambios = mongoOperations.aggregate(aggregationCambios,
				CambioDTO.class);
		
		if(ObjectUtils.existeValor(detalle.getAuditorias())) {
			Optional<AuditoriaDTO> ultimaAuditoria = detalle.getAuditorias()
					.stream()
					.filter(au-> !ObjectUtils.existeValor(au.getFecBaja()))
					.findFirst();
			
			if(ultimaAuditoria.isPresent()) {
				detalle.setDesSituacionRegistro(ultimaAuditoria.get().getDesSituacionRegistro());
			}
		}
		
		List<CambioDTO> listCambios = aggregationResultsCambios.getMappedResults();
		if(!listCambios.isEmpty()) {
			
			Optional<CambioDTO> cambio = listCambios.stream()
					.sorted(Comparator.comparing(CambioDTO::getFecAlta)
				    .reversed())
					.findFirst();

			if(StringUtils.isEmpty(detalle.getDesSituacionRegistro())) {
					detalle.setDesSituacionRegistro(cambio.get().getDesSituacionRegistro());
			}				

			detalle.setAuditorias(new ArrayList<>());
			List<AuditoriaDTO> auditorias = new ArrayList<>();
			auditorias = cambio.get().getAuditorias().stream().map(au->{
				AuditoriaDTO resp = new AuditoriaDTO();

				resp.setAccion(au.getAccion());
				resp.setCveIdAccionRegistro(au.getCveIdAccionRegistro());
				resp.setCveSituacionRegistro(au.getCveSituacionRegistro());
				resp.setDesAccionRegistro(au.getDesAccionRegistro());
				resp.setDesCambio(au.getDesCambio());
				resp.setDesObservacionesAprobador(au.getDesObservacionesAprobador());
				resp.setDesObservacionesSol(au.getDesObservacionesSol());
				resp.setDesSituacionRegistro(au.getDesSituacionRegistro());
				resp.setFecActualizacion(au.getFecActualizacion());
				resp.setFecAlta(au.getFecAlta());
				resp.setFecBaja(au.getFecBaja());
				resp.setNomUsuario(au.getNomUsuario());
				resp.setNumFolioMovOriginal(au.getNumFolioMovOriginal());

				return resp;
			}).collect(Collectors.toList());
			detalle.getAuditorias().addAll(auditorias);
			
		}
		return detalle;
	}

	@Override
	public Object updateMovimiento(DatosModificadosDTO input) {
		// Se construye el query
		Query query = new Query();
		ObjectId id = new ObjectId(input.getObjectIdOrigen());
		query.addCriteria(Criteria.where("_id").is(id));
		DetalleRegistroDTO detalle = mongoOperations.findOne(query, DetalleRegistroDTO.class);
		
		if (input.getCveConsecuecniaModificado() != null && input.getCveConsecuecniaModificado() > 0) {
			detalle.getIncapacidadDTO().setCveConsecuencia(input.getCveConsecuecniaModificado() != null ?
					input.getCveConsecuecniaModificado().toString() : null);
		}

		if (StringUtils.isNotBlank(input.getDesConsecuenciaModificado())
				&& StringUtils.isNotEmpty(input.getDesConsecuenciaModificado())) {
			detalle.getIncapacidadDTO().setDesConsecuencia(input.getDesConsecuenciaModificado());
		}

		if (input.getCveTipoRiesgoModificado() != null && input.getCveTipoRiesgoModificado() > 0) {
			detalle.getIncapacidadDTO().setCveTipoRiesgo(input.getCveTipoRiesgoModificado() != null ?
					input.getCveTipoRiesgoModificado().toString() : null);
		}

		if (StringUtils.isNotBlank(input.getDesTipoRiesgoModificado())
				&& StringUtils.isNotEmpty(input.getDesTipoRiesgoModificado())) {
			detalle.getIncapacidadDTO().setDesTipoRiesgo(input.getDesTipoRiesgoModificado());
		}

		if (input.getFecFinModificado() != null) {
			detalle.getIncapacidadDTO().setFecFin(input.getFecFinModificado());
		}

		if (input.getFecInicioModificado() != null) {
			detalle.getIncapacidadDTO().setFecInicio(input.getFecInicioModificado());
		}

		if (StringUtils.isNotBlank(input.getNssModificado()) && StringUtils.isNotEmpty(input.getNssModificado())) {
			detalle.getAseguradoDTO().setNumNss(input.getNssModificado());
		}

		if (input.getNumDiasSubsidiadosModificado() != null && input.getNumDiasSubsidiadosModificado() > 0) {
			detalle.getIncapacidadDTO().setNumDiasSubsidiados(input.getNumDiasSubsidiadosModificado());
		}

		if (input.getPorcentajeIncapacidadModificado() != null) {
			detalle.getIncapacidadDTO()
					.setPorPorcentajeIncapacidad(input.getPorcentajeIncapacidadModificado());
		}

		if (StringUtils.isNotBlank(input.getRpModificado()) && StringUtils.isNotEmpty(input.getRpModificado())) {
			detalle.getPatronDTO().setRefRegistroPatronal(input.getRpModificado());

		}

		if (input.getCveIdAccionRegistro() == AccionRegistroEnum.BAJA_PENDIENTE.getClave()
				|| input.getCveIdAccionRegistro() == AccionRegistroEnum.ELIMINACION.getClave()) {
			DatosModificadosDTO datosModificadosDTO = input;
			datosModificadosDTO = calculaRN68(input);
			detalle.getAseguradoDTO().setCveEstadoRegistro(datosModificadosDTO.getCveEstadoRegistro().intValue());
			detalle.getAseguradoDTO().setDesEstadoRegistro(datosModificadosDTO.getDesEstadoRegistro());
			detalle.getAseguradoDTO().setFecBaja(new Date());
		}
		if (input.getCveIdAccionRegistro()==AccionRegistroEnum.MODIFICACION_PENDIENTE.getClave() || input.getCveIdAccionRegistro()==AccionRegistroEnum.MODIFICACION.getClave()) {
			detalle.getAseguradoDTO().setCveEstadoRegistro(EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro());
			detalle.getAseguradoDTO().setDesEstadoRegistro(EstadoRegistroEnum.CORRECTO.getDesDescripcion());
		}

		mongoOperations.save(detalle);

		return true;
	}

	private DatosModificadosDTO calculaRN68(DatosModificadosDTO input) {
		if (input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.DUPLICADO.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.ERRONEO.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.SUSCEPTIBLE.getCveEstadoRegistro()) {
			input.setCveEstadoRegistro(EstadoRegistroEnum.BAJA.getCveEstadoRegistro());
			input.setDesEstadoRegistro(EstadoRegistroEnum.BAJA.getDesDescripcion());

		}
		if (input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.CORRECTO_OTRAS.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.DUPLICADO_OTRAS
						.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.ERRONEO_OTRAS.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.SUSCEPTIBLE_OTRAS
						.getCveEstadoRegistro()) {
			input.setCveEstadoRegistro(EstadoRegistroEnum.BAJA_OTRAS_DELEGACIONES.getCveEstadoRegistro());
			input.setDesEstadoRegistro(EstadoRegistroEnum.BAJA_OTRAS_DELEGACIONES.getDesDescripcion());

		}
		return input;
	}

	public ResponseDTO<List<MovimientosOutputDTO>> getMovements(@Valid Integer cveDelegacion,
			@Valid Integer cveSubdelegacion, @Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia,
			@Valid Integer cveCasoRegistro, @Valid String fromMonth, @Valid String fromYear, @Valid String toMonth,
			@Valid String toYear, @Valid List<Integer> cveEstadoRegistroList,
			@Valid String numNss, @Valid String refRegistroPatronal, @Valid String cveSituacionRegistro,
			@Valid String fromDay, @Valid String toDay, @Valid Integer cveIdAccionRegistro, @Valid String rfc,
			@Valid Integer cveClase, @Valid Integer cveFraccion, @Valid Integer cveLaudo, @Valid String origenAlta,
			@Valid Long page, @Valid Long skipOffset, @Valid Long size, @Valid List<AggregationOperation> customAggregarionList,
			@Valid AggregationOperation sortOperation, @Valid Boolean isOperative, @Valid Boolean isApprover, 
			@Valid Boolean isCasuistry, @Valid Long posicionMovimiento) {

		ResponseDTO<List<DetalleRegistroDTO>> responseCambios = getMovements(cveDelegacion, cveSubdelegacion, cveTipoRiesgo,
				cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList, numNss,
				refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc, cveClase,
				cveFraccion, cveLaudo, origenAlta, page, skipOffset, Boolean.TRUE, size, customAggregarionList, sortOperation,
				isOperative, isApprover, isCasuistry, posicionMovimiento);
		List<DetalleRegistroDTO> listCambios = responseCambios.getData();
		ResponseDTO<List<MovimientosOutputDTO>> response = new ResponseDTO<>();

		if (!listCambios.isEmpty()) {
			response.setData(llenarDatos(listCambios));
		} else {
			response.setData(new ArrayList<>());
		}
		return response;
	}

	public ResponseDTO<Void> getCount(@Valid Integer cveDelegacion, @Valid Integer cveSubdelegacion,
			@Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia, @Valid Integer cveCasoRegistro,
			@Valid String fromMonth, @Valid String fromYear, @Valid String toMonth, @Valid String toYear,
			@Valid List<Integer> cveEstadoRegistroList, @Valid String numNss, @Valid String refRegistroPatronal,
			@Valid String cveSituacionRegistro, @Valid String fromDay, @Valid String toDay,
			@Valid Integer cveIdAccionRegistro, @Valid String rfc, @Valid Integer cveClase, @Valid Integer cveFraccion,
			@Valid Integer cveLaudo, @Valid String origenAlta, @Valid MatchOperation customCriteria, @Valid Boolean isOperative,
			@Valid Boolean isApprover, @Valid Boolean isCasuistry) {

		Long count = getMovementCount(cveDelegacion, cveSubdelegacion, cveTipoRiesgo, cveConsecuencia, cveCasoRegistro,
				fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList, numNss, refRegistroPatronal,
				cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc, cveClase, cveFraccion, cveLaudo,
				origenAlta, customCriteria, isOperative, isApprover, isCasuistry);
		logger.info("------------------------------------Numero de registros de movimientos--------------------------------");
		logger.info(MessageFormat.format("Numero de registros totales para la consulta: {0}", count));
		logger.info("--------------------------------------------------------------------------------------------------");
		ResponseDTO<Void> response = new ResponseDTO<>();
		response.setTotalElements(count);
		return response;
	}

	public ResponseDTO<List<DetalleRegistroDTO>> getMovements(@Valid Integer cveDelegacion,
			@Valid Integer cveSubdelegacion, @Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia,
			@Valid Integer cveCasoRegistro, @Valid String fromMonth, @Valid String fromYear, @Valid String toMonth,
			@Valid String toYear, @Valid List<Integer> cveEstadoRegistroList, @Valid String numNss,
			@Valid String refRegistroPatronal, @Valid String cveSituacionRegistro, @Valid String fromDay,
			@Valid String toDay, @Valid Integer cveIdAccionRegistro, @Valid String rfc, @Valid Integer cveClase,
			@Valid Integer cveFraccion, @Valid Integer cveLaudo, @Valid String origenAlta, @Valid Long page,
			@Valid Long skipOffset, Boolean paginated, @Valid Long size, @Valid List<AggregationOperation> customAggregarionList,
			@Valid AggregationOperation sortOperation, @Valid Boolean isOperative, @Valid Boolean isApprover, 
			@Valid Boolean isCasuistry, @Valid Long posicionMovimiento) {

		ResponseDTO<List<DetalleRegistroDTO>> response = new ResponseDTO<>();
		page = PaginationUtils.validatePage(page);
		Long skipIndex = PaginationUtils.getSkipElements(page, size / 2);
		List<AggregationOperation> aggregationOperationList = buildAggregationOperationList(cveDelegacion, cveSubdelegacion,
				cveTipoRiesgo, cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList,
				numNss, refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc, cveClase,
				cveFraccion, cveLaudo, origenAlta, isOperative, isApprover, isCasuistry);
		if (customAggregarionList != null && !customAggregarionList.isEmpty()) {
			aggregationOperationList.addAll(customAggregarionList);
		}
		if (sortOperation == null) {
			sortOperation = new CustomAggregationOperation("{ $sort: { 'aseguradoDTO.numNss': 1," +
				"'incapacidadDTO.porPorcentajeIncapacidad': 1, 'incapacidadDTO.numDiasSubsidiados': 1 } }");
		}	
		aggregationOperationList.add(sortOperation);
		
		Long skipAll = skipIndex + skipOffset;
		if(ObjectUtils.existeValor(posicionMovimiento)) {
			skipAll = posicionMovimiento;
		}
		if (paginated != null && paginated && size != null) {
			if(skipAll > 0) {
				aggregationOperationList.add(Aggregation.skip(skipAll));
			}
			aggregationOperationList.add(Aggregation.limit(size));
		}
		
		TypedAggregation<DetalleRegistroDTO> aggregation = Aggregation.newAggregation(DetalleRegistroDTO.class, aggregationOperationList)
				.withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
		logger.info("--------------Query de agregacion-------------------");
		logger.info(aggregation.toString());
		logger.info("----------------------------------------------------");
		AggregationResults<DetalleRegistroDTO> aggregationResults = mongoOperations.aggregate(aggregation, DetalleRegistroDTO.class);
		response.setData(aggregationResults.getMappedResults());
		response.setSize(size);
		return response;
	}

	private Long getMovementCount(@Valid Integer cveDelegacion,
			@Valid Integer cveSubdelegacion, @Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia,
			@Valid Integer cveCasoRegistro, @Valid String fromMonth, @Valid String fromYear, @Valid String toMonth,
			@Valid String toYear, @Valid List<Integer> cveEstadoRegistroList, @Valid String numNss,
			@Valid String refRegistroPatronal, @Valid String cveSituacionRegistro, @Valid String fromDay,
			@Valid String toDay, @Valid Integer cveIdAccionRegistro, @Valid String rfc, @Valid Integer cveClase,
			@Valid Integer cveFraccion, @Valid Integer cveLaudo, @Valid String origenAlta, @Valid MatchOperation customCriteria,
			@Valid Boolean isOperative, @Valid Boolean isApprover, @Valid Boolean isCasuistry) {
		logger.info("--------------Query de agregacion de conteo-------------------");
		long totalElements;
		List<AggregationOperation> aggregationOperationList = buildAggregationOperationList(cveDelegacion, cveSubdelegacion,
				cveTipoRiesgo, cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList,
				numNss, refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc, cveClase,
				cveFraccion, cveLaudo, origenAlta, isOperative, isApprover, isCasuistry);
		if (customCriteria != null) {
			aggregationOperationList.add(0, customCriteria);
		}
		CountOperation count = Aggregation.count().as("totalElements");
		aggregationOperationList.add(count);
		TypedAggregation<CountDTO> aggregation = Aggregation.newAggregation(CountDTO.class, aggregationOperationList)
				.withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
		logger.info(aggregation.toString());
		AggregationResults<CountDTO> countAggregationResult = mongoOperations.aggregate(aggregation,
				DetalleRegistroDTO.class, CountDTO.class);
		totalElements = !countAggregationResult.getMappedResults().isEmpty()
				&& countAggregationResult.getMappedResults().get(0) != null
				? countAggregationResult.getMappedResults().get(0).getTotalElements()
				: 0;
		logger.info("----------------------------------------------------");
		return totalElements;
	}

	private List<AggregationOperation> buildAggregationOperationList(@Valid Integer cveDelegacion,
			@Valid Integer cveSubdelegacion, @Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia,
			@Valid Integer cveCasoRegistro, @Valid String fromMonth, @Valid String fromYear, @Valid String toMonth,
			@Valid String toYear, @Valid List<Integer> cveEstadoRegistroList, @Valid String numNss,
			@Valid String refRegistroPatronal, @Valid String cveSituacionRegistro, @Valid String fromDay,
			@Valid String toDay, @Valid Integer cveIdAccionRegistro, @Valid String rfc, @Valid Integer cveClase,
			@Valid Integer cveFraccion, @Valid Integer cveLaudo, @Valid String origenAlta, @Valid Boolean isOperative,
			@Valid Boolean isApprover, @Valid Boolean isCasuistry) {

		// Se calculan las fechas inicio y fin para la consulta
		Date fecProcesoIni = null;
		if(StringUtils.isNotBlank(fromMonth) && StringUtils.isNotBlank(fromYear)){
			fecProcesoIni = DateUtils.calculateBeginDate(fromYear, fromMonth, fromDay);
		}
		
		Date fecProcesoFin = null; 
		if(StringUtils.isNotBlank(toMonth) && StringUtils.isNotBlank(toYear)){
			fecProcesoFin = DateUtils.calculateEndDate(toYear, toMonth, toDay);
		}
		
		Criteria cFecProcesoCarga = null;
		if (fecProcesoIni != null && fecProcesoFin != null) {
			cFecProcesoCarga = new Criteria().andOperator(Criteria.where("aseguradoDTO.fecAlta").gt(fecProcesoIni),
					Criteria.where("aseguradoDTO.fecAlta").lte(fecProcesoFin));
		}
		Criteria cDelAndSubDel = null; // aseguradoDTO.cveDelegacionNss && aseguradoDTO.cveSubdelNss
		Criteria cDel = null; // patronDTO.cveDelRegPatronal && patronDTO.cveSubDelRegPatronal
		Criteria cCveTipoRiesgo = null; // incapacidadDTO.cveTipoRiesgo
		Criteria cCveConsecuencia = null; // incapacidadDTO.cveConsecuencia
		Criteria cCveCasoRegistro = null; // aseguradoDTO.cveCasoRegistro
		Criteria cCveEstadoRegistroList = null; // aseguradoDTO.cveEstadoRegistro
		Criteria cCveIdAccionRegistro = null; // No aplica pero aun asi anexar para que cuando venga el filtro no
		// encuentre nada
		Criteria cCveLaudo = null; // incapacidadDTO.cveLaudo
		Criteria cNumNss = null; // aseguradoDTO.numNss
		Criteria cRefRegistroPatronal = null; // patronDTO.refRegistroPatronal
		Criteria cCveClase = null; // patronDTO.cveClase
		Criteria cDesRfc = null; // patronDTO.desRfc
		Criteria cCveFraccion = null; // patronDTO.cveFraccion
		Criteria cCveSituacionRegistro = null; // No existe pero anexar para que no encuentre cuando venga este filtro
		Criteria cIsOperative = null;

		if (cveDelegacion != null && cveDelegacion > 0 && cveSubdelegacion != null && cveSubdelegacion > 0) {
			Criteria delAsegurado = Criteria.where("aseguradoDTO.cveDelegacionNss").is(cveDelegacion);
			Criteria delPatron = Criteria.where("patronDTO.cveDelRegPatronal").is(cveDelegacion);

			Criteria subdelAsegurado = Criteria.where("aseguradoDTO.cveSubdelNss").is(cveSubdelegacion);
			Criteria subdelPatron = Criteria.where("patronDTO.cveSubDelRegPatronal").is(cveSubdelegacion);

			cDelAndSubDel = new Criteria().orOperator(new Criteria().andOperator(delAsegurado, subdelAsegurado),
					new Criteria().andOperator(delPatron, subdelPatron));

		} else if (cveDelegacion != null && cveDelegacion > 0) {
			Criteria delAsegurado = Criteria.where("aseguradoDTO.cveDelegacionNss").is(cveDelegacion);
			Criteria delPatron = Criteria.where("patronDTO.cveDelRegPatronal").is(cveDelegacion);
			cDel = new Criteria().orOperator(delAsegurado, delPatron);
		}
		if (cveTipoRiesgo != null && cveTipoRiesgo > 0) {
			cCveTipoRiesgo = Criteria.where("incapacidadDTO.cveTipoRiesgo").is(cveTipoRiesgo);
		}
		if (cveConsecuencia != null && cveConsecuencia >= 0) {
			cCveConsecuencia = Criteria.where("incapacidadDTO.cveConsecuencia").is(cveConsecuencia);
		}
		if (cveCasoRegistro != null && cveCasoRegistro > 0) {
			cCveCasoRegistro = Criteria.where("aseguradoDTO.cveCasoRegistro").is(cveCasoRegistro);
		}
		if (cveEstadoRegistroList != null && !cveEstadoRegistroList.isEmpty()) {
			cCveEstadoRegistroList = Criteria.where("aseguradoDTO.cveEstadoRegistro").in(cveEstadoRegistroList);
		}
		if (cveIdAccionRegistro != null && cveIdAccionRegistro > 0) {
			cCveIdAccionRegistro = Criteria.where("auditorias.cveIdAccionRegistro").is(cveIdAccionRegistro);
		}
		if (cveLaudo != null && cveLaudo > 0) {
			cCveLaudo = Criteria.where("incapacidadDTO.cveLaudo").is(cveLaudo);
		}
		if (StringUtils.isNotBlank(numNss) && StringUtils.isNotEmpty(numNss)) {
			cNumNss = Criteria.where("aseguradoDTO.numNss").is(numNss);
		}
		if (StringUtils.isNotBlank(refRegistroPatronal) && StringUtils.isNotEmpty(refRegistroPatronal)) {
			cRefRegistroPatronal = Criteria.where("patronDTO.refRegistroPatronal").is(refRegistroPatronal);
		}
		if (cveClase != null && cveClase > 0) {
			cCveClase = Criteria.where("patronDTO.cveClase").is(cveClase);
		}
		if (StringUtils.isNotBlank(origenAlta) && StringUtils.isNotEmpty(origenAlta) && origenAlta.equals("EP")) {
			cDesRfc = Criteria.where("patronDTO.desRfc").is(RFC_IMSS);
		} else if (StringUtils.isNotBlank(rfc) && StringUtils.isNotEmpty(rfc)) {
			cDesRfc = Criteria.where("patronDTO.desRfc").is(rfc);
		}
		if (cveFraccion != null && cveFraccion > 0) {
			cCveFraccion = Criteria.where("patronDTO.cveFraccion").is(cveFraccion);
		}
		if (StringUtils.isNotBlank(cveSituacionRegistro) && StringUtils.isNotEmpty(cveSituacionRegistro)
				&& !cveSituacionRegistro.equals("-1")) {
			cCveSituacionRegistro = Criteria.where("cveSituacionRegistro").is(Integer.parseInt(cveSituacionRegistro));
		} else {
			cIsOperative = new Criteria().orOperator(Criteria.where("isPending").is(null),
					Criteria.where("isPending").is(Boolean.FALSE));
		}
		// Comienza la construccion de la lista de operaciones para la agregacion
		MatchOperation fecBaja = Aggregation.match(Criteria.where("aseguradoDTO.fecBaja").is(null));
		
		if(cveEstadoRegistroList != null) {
			if(cveEstadoRegistroList.stream().filter(x-> x == 10 || x == 11).count() > 0) {
				fecBaja = null;
			}	
		}
		
		if (fecProcesoIni == null && fecProcesoFin == null) {
			fecBaja = null;
		}
		
		List<AggregationOperation> aggregationOperationList = Arrays.asList(
				fecBaja, AggregationUtils.validateMatchOp(cDelAndSubDel), AggregationUtils.validateMatchOp(cDel),
				AggregationUtils.validateMatchOp(cCveTipoRiesgo), AggregationUtils.validateMatchOp(cCveConsecuencia),
				AggregationUtils.validateMatchOp(cCveCasoRegistro), AggregationUtils.validateMatchOp(cIsOperative),
				AggregationUtils.validateMatchOp(cCveEstadoRegistroList),
				AggregationUtils.validateMatchOp(cCveIdAccionRegistro), AggregationUtils.validateMatchOp(cCveLaudo),
				AggregationUtils.validateMatchOp(cNumNss), AggregationUtils.validateMatchOp(cRefRegistroPatronal),
				AggregationUtils.validateMatchOp(cCveClase), AggregationUtils.validateMatchOp(cDesRfc),
				AggregationUtils.validateMatchOp(cCveFraccion), AggregationUtils.validateMatchOp(cCveSituacionRegistro),
				AggregationUtils.validateMatchOp(cFecProcesoCarga));
		aggregationOperationList = aggregationOperationList.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		return aggregationOperationList;
	}

	private List<MovimientosOutputDTO> llenarDatos(List<DetalleRegistroDTO> listDetalleArhivos) {
		List<MovimientosOutputDTO> listMovimientos = null;
		if (listDetalleArhivos != null && !listDetalleArhivos.isEmpty()) {
			listMovimientos = new ArrayList<MovimientosOutputDTO>();
			for (DetalleRegistroDTO detalleArchivoDTO : listDetalleArhivos) {
				MovimientosOutputDTO movimientosOutputDTO = new MovimientosOutputDTO();
				movimientosOutputDTO.setPosition(detalleArchivoDTO.getAseguradoDTO().getNumIndice());
				movimientosOutputDTO.setDesConsecuencia(detalleArchivoDTO.getIncapacidadDTO().getDesConsecuencia());
				movimientosOutputDTO
						.setCveConsecuencia(detalleArchivoDTO.getIncapacidadDTO().getCveConsecuencia() != null
								? detalleArchivoDTO.getIncapacidadDTO().getCveConsecuencia()
								: null);
				movimientosOutputDTO
						.setDesEstadoRegistro(validaCadena(detalleArchivoDTO.getAseguradoDTO().getDesEstadoRegistro()));
				movimientosOutputDTO.setCveEstadoRegistro(detalleArchivoDTO.getAseguradoDTO().getCveEstadoRegistro());
				movimientosOutputDTO.setDesTipoRiesgo(detalleArchivoDTO.getIncapacidadDTO().getDesTipoRiesgo());
				movimientosOutputDTO.setCveTipoRiesgo(detalleArchivoDTO.getIncapacidadDTO().getCveTipoRiesgo() != null
						? detalleArchivoDTO.getIncapacidadDTO().getCveTipoRiesgo()
						: null);
				movimientosOutputDTO.setFecFin(detalleArchivoDTO.getIncapacidadDTO().getFecFin() != null
						? detalleArchivoDTO.getIncapacidadDTO().getFecFin().toString()
						: "");
				movimientosOutputDTO.setFecInicio(detalleArchivoDTO.getIncapacidadDTO().getFecInicio() != null
						? detalleArchivoDTO.getIncapacidadDTO().getFecInicio().toString()
						: "");
				movimientosOutputDTO.setFecActualizacion(detalleArchivoDTO.getAseguradoDTO().getFecActualizacion());
				StringBuffer nomCompleto = new StringBuffer();
				nomCompleto.append(validaCadena(detalleArchivoDTO.getAseguradoDTO().getRefPrimerApellido()));
				nomCompleto.append(" ");
				nomCompleto.append(validaCadena(detalleArchivoDTO.getAseguradoDTO().getRefSegundoApellido()));
				nomCompleto.append(" ");
				nomCompleto.append(validaCadena(detalleArchivoDTO.getAseguradoDTO().getNomAsegurado()));

				movimientosOutputDTO.setNomCompletoAsegurado(nomCompleto.toString());

				movimientosOutputDTO
						.setNumDiasSubsidiados(detalleArchivoDTO.getIncapacidadDTO().getNumDiasSubsidiados());
				movimientosOutputDTO.setNumFolioMovtoOriginal(
						validaCadena(detalleArchivoDTO.getAseguradoDTO().getRefFolioOriginal()));
				movimientosOutputDTO.setNumNss(validaCadena(detalleArchivoDTO.getAseguradoDTO().getNumNss()));
				movimientosOutputDTO.setPorcentajeIncapacidad(
						validaCadena(Utils.enteroACadena(detalleArchivoDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad())));
				movimientosOutputDTO.setRefRegistroPatronal(
						validaCadena(detalleArchivoDTO.getPatronDTO().getRefRegistroPatronal()));
				movimientosOutputDTO.setObjectId(detalleArchivoDTO.getObjectIdArchivoDetalle().toString());
				movimientosOutputDTO.setObjectIdArchivo(detalleArchivoDTO.getIdentificadorArchivo() != null
						? detalleArchivoDTO.getIdentificadorArchivo().toString()
						: null);
				if (detalleArchivoDTO.getIncapacidadDTO() != null
						&& detalleArchivoDTO.getIncapacidadDTO().getCveConsecuencia() != null) {
					
					if(detalleArchivoDTO.getIncapacidadDTO().getCveConsecuencia().equals("0")) {
						movimientosOutputDTO.setDesConsecuencia("Sin consecuencias");	
					}
					
					if(detalleArchivoDTO.getIncapacidadDTO().getCveConsecuencia().equals("6")) {
						movimientosOutputDTO.setDesConsecuencia("Con Valuación inicial provisional posterior a la fecha de alta");	
					}
				}
				movimientosOutputDTO.setIsChange(Boolean.FALSE);
				listMovimientos.add(movimientosOutputDTO);

			}
			logger.info("Numero de archivos iterados: " + listDetalleArhivos.size());
		}

		return listMovimientos;
	}

	private String validaCadena(String cadena) {
		return StringUtils.isNotBlank(cadena) && StringUtils.isNotEmpty(cadena) ? cadena.toString() : "";
	}
	
	@Override
	public 	DetalleRegistroDTO getDetalleMovimiento(@Valid String objectId) {
		logger.debug("getDetalleMovimiento");
		// Se construye la agregacion
		MatchOperation match = Aggregation.match(Criteria.where("_id").is(new ObjectId(objectId)));
		LookupOperation lookup = Aggregation.lookup("MCT_ARCHIVO", "identificadorArchivo", "_id", "archivoDTO");
		UnwindOperation unwind = Aggregation.unwind("archivoDTO");
		TypedAggregation<DetalleRegistroDTO> aggregation = Aggregation.newAggregation(DetalleRegistroDTO.class, Arrays.asList(
				match, lookup, unwind));
		AggregationResults<DetalleRegistroDTO> aggregationResults = mongoOperations.aggregate(aggregation, DetalleRegistroDTO.class);
		DetalleRegistroDTO detalle = aggregationResults.getUniqueMappedResult();
		detalle.setFecProcesoCarga(detalle.getArchivoDTO().getFecProcesoCarga());
		detalle.setFecProcesoCarga(detalle.getArchivoDTO().getFecProcesoCarga());
		detalle.getAseguradoDTO()
				.setDesCodigoError(detalle.getAseguradoDTO().getCveCodigoError());
		detalle.setObjectIdOrigen(detalle.getObjectIdArchivoDetalle() != null ? detalle.getObjectIdArchivoDetalle().toString() : null);

		MatchOperation matchCambios = Aggregation.match(Criteria.where("objectIdOrigen").is(new ObjectId(objectId)));
		TypedAggregation<CambioDTO> aggregationCambios = Aggregation.newAggregation(CambioDTO.class,
				Collections.singletonList(matchCambios));
		AggregationResults<CambioDTO> aggregationResultsCambios = mongoOperations.aggregate(aggregationCambios,
				CambioDTO.class);
		
		List<CambioDTO> listCambios = aggregationResultsCambios.getMappedResults();
		if(!listCambios.isEmpty()) {
			int totalCambios = listCambios.size() - 1;
			if(StringUtils.isEmpty( detalle.getDesSituacionRegistro())) {
				detalle.setDesSituacionRegistro(listCambios.get(totalCambios).getDesSituacionRegistro());
			}				
			
			if(listCambios.get(totalCambios).getAuditorias() != null) {
				int ultimaAuditoria = listCambios.get(totalCambios).getAuditorias().size() - 1;
				String estadoRegistro = listCambios.get(totalCambios).getAuditorias().get(ultimaAuditoria).getDesAccionRegistro();
				
				AuditoriaDTO auditoria = new AuditoriaDTO();
				auditoria.setDesAccionRegistro(estadoRegistro);
				
				if(detalle.getAuditorias() == null) {
					detalle.setAuditorias(new ArrayList<>());
				}
				
				detalle.getAuditorias().add(auditoria);
				
			}
		}
		return detalle;
		
	}

}
