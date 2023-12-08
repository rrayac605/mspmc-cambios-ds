package mx.gob.imss.cit.pmc.cambios.repository.impl;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
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
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import io.micrometer.core.instrument.util.StringUtils;
import mx.gob.imss.cit.mspmccommons.dto.AuditoriaDTO;
import mx.gob.imss.cit.mspmccommons.dto.BitacoraDictamenDTO;
import mx.gob.imss.cit.mspmccommons.dto.CambioDTO;
import mx.gob.imss.cit.mspmccommons.dto.CamposOriginalesDTO;
import mx.gob.imss.cit.mspmccommons.dto.CasoRegistroDTO;
import mx.gob.imss.cit.mspmccommons.dto.DatosModificadosDTO;
import mx.gob.imss.cit.mspmccommons.dto.DetalleRegistroDTO;
import mx.gob.imss.cit.mspmccommons.dto.MovimientoRequest;
import mx.gob.imss.cit.mspmccommons.dto.MovimientosOutputDTO;
import mx.gob.imss.cit.mspmccommons.dto.ParametroDTO;
import mx.gob.imss.cit.mspmccommons.dto.ReporteCasuisticaInputDTO;
import mx.gob.imss.cit.mspmccommons.enums.AccionRegistroEnum;
import mx.gob.imss.cit.mspmccommons.enums.EstadoRegistroEnum;
import mx.gob.imss.cit.mspmccommons.enums.IdentificadorArchivoEnum;
import mx.gob.imss.cit.mspmccommons.enums.SituacionRegistroEnum;
import mx.gob.imss.cit.mspmccommons.exception.BusinessException;
import mx.gob.imss.cit.mspmccommons.integration.model.CountDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.MccAccionRegistro;
import mx.gob.imss.cit.mspmccommons.integration.model.MccSituacionRegistro;
import mx.gob.imss.cit.mspmccommons.integration.model.ResponseDTO;
import mx.gob.imss.cit.mspmccommons.utils.AggregationUtils;
import mx.gob.imss.cit.mspmccommons.utils.CustomAggregationOperation;
import mx.gob.imss.cit.mspmccommons.utils.DateUtils;
import mx.gob.imss.cit.mspmccommons.utils.ObjectUtils;
import mx.gob.imss.cit.mspmccommons.utils.PaginationUtils;
import mx.gob.imss.cit.mspmccommons.utils.Utils;
import mx.gob.imss.cit.pmc.cambios.model.CountMovementsChanges;
import mx.gob.imss.cit.pmc.cambios.repository.CambiosRepository;
import mx.gob.imss.cit.pmc.cambios.repository.MovimientosRepository;
import mx.gob.imss.cit.pmc.cambios.repository.ParametroRepository;

@Repository
public class CambiosRepositoryImpl implements CambiosRepository {
	
	@Autowired
	private MongoOperations mongoOperations;

	@Autowired
	private ParametroRepository parametroRepository;

	@Autowired
	private MovimientosRepository movimientosRepository;

	private static final Logger logger = LoggerFactory.getLogger(CambiosRepositoryImpl.class);

	private static final String RFC_IMSS = "IMS421231I45";

	@Override
	public void save(CambioDTO cambioDTO) {

		mongoOperations.save(cambioDTO);

	}

	@Override
	public Optional<CambioDTO> findOneById(String id) {
		CambioDTO d = this.mongoOperations.findOne(new Query(Criteria.where("_id").is(id)), CambioDTO.class);

		return Optional.ofNullable(d);
	}

	private ResponseDTO<List<CambioDTO>> getChanges(@Valid Integer cveDelegacion, @Valid Integer cveSubdelegacion,
			@Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia, @Valid Integer cveCasoRegistro,
			@Valid String fromMonth, @Valid String fromYear, @Valid String toMonth, @Valid String toYear,
			@Valid List<Integer> cveEstadoRegistroList, @Valid String numNss, @Valid String refRegistroPatronal,
			@Valid String cveSituacionRegistro, @Valid String fromDay, @Valid String toDay,
			@Valid Integer cveIdAccionRegistro, @Valid String rfc, @Valid Integer cveClase, @Valid Integer cveFraccion,
			@Valid Integer cveLaudo, @Valid String origenAlta, @Valid List<AggregationOperation> customAggregarionList,
			@Valid Long page, @Valid Long skipOffset, @Valid Long size, @Valid Boolean paginated,
			@Valid Boolean isOperative, @Valid Boolean isApprover, @Valid Boolean isCasuistry,@Valid Long skipCounter) {

		ResponseDTO<List<CambioDTO>> response = new ResponseDTO<>();
		page = PaginationUtils.validatePage(page);
		Long skipIndex = PaginationUtils.getSkipElements(page, size / 2);
		Long skipAll = skipIndex + skipOffset;
		if(ObjectUtils.existeValor(skipCounter)) {
			skipAll = skipCounter;
		}
		List<AggregationOperation> aggregationOperationList = buildAggregationOperationList(cveDelegacion,
				cveSubdelegacion, cveTipoRiesgo, cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear,
				cveEstadoRegistroList, numNss, refRegistroPatronal, cveSituacionRegistro, fromDay, toDay,
				cveIdAccionRegistro, rfc, cveClase, cveFraccion, cveLaudo, origenAlta, isOperative, isApprover, isCasuistry);
		if (customAggregarionList != null && !customAggregarionList.isEmpty()) {
			aggregationOperationList.addAll(customAggregarionList);
		}
		CustomAggregationOperation sort = new CustomAggregationOperation("{ $sort: { 'numNss': 1 } }");
		aggregationOperationList.add(sort);
		if (paginated != null && paginated) {
			aggregationOperationList.add(Aggregation.skip(skipAll));
			aggregationOperationList.add(Aggregation.limit(size));
		}
		CustomAggregationOperation group = new CustomAggregationOperation(buildChangesGroup());
		aggregationOperationList.add(group);
		AggregationOptions options = AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build();
		TypedAggregation<CambioDTO> aggregation = Aggregation.newAggregation(CambioDTO.class, aggregationOperationList)
				.withOptions(options);
		logger.info("--------------Query de agregacion-------------------");
		logger.info(aggregation.toString());
		logger.info("----------------------------------------------------");
		AggregationResults<CambioDTO> aggregationResults = mongoOperations.aggregate(aggregation, CambioDTO.class);
		response.setData(aggregationResults.getMappedResults());
		response.setSize(size);
		return response;
	}

	private Long getChangeCount(@Valid Integer cveDelegacion, @Valid Integer cveSubdelegacion,
			@Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia, @Valid Integer cveCasoRegistro,
			@Valid String fromMonth, @Valid String fromYear, @Valid String toMonth, @Valid String toYear,
			@Valid List<Integer> cveEstadoRegistroList, @Valid String numNss, @Valid String refRegistroPatronal,
			@Valid String cveSituacionRegistro, @Valid String fromDay, @Valid String toDay,
			@Valid Integer cveIdAccionRegistro, @Valid String rfc, @Valid Integer cveClase, @Valid Integer cveFraccion,
			@Valid Integer cveLaudo, @Valid String origenAlta, @Valid MatchOperation customCriteria,
			@Valid Boolean isOperative, @Valid Boolean isApprover, @Valid Boolean isCasuistry) {
		logger.info("--------------Query de agregacion de conteo-------------------");
		long totalElements;
		List<AggregationOperation> aggregationOperationList = buildAggregationOperationList(cveDelegacion,
				cveSubdelegacion, cveTipoRiesgo, cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear,
				cveEstadoRegistroList, numNss, refRegistroPatronal, cveSituacionRegistro, fromDay, toDay,
				cveIdAccionRegistro, rfc, cveClase, cveFraccion, cveLaudo, origenAlta, isOperative, isApprover, isCasuistry);
		if (customCriteria != null) {
			aggregationOperationList.add(0, customCriteria);
		}
		CountOperation count = Aggregation.count().as("totalElements");
		aggregationOperationList.add(count);
		TypedAggregation<CountDTO> aggregation = Aggregation.newAggregation(CountDTO.class, aggregationOperationList)
				.withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
		logger.info(aggregation.toString());
		AggregationResults<CountDTO> countAggregationResult = mongoOperations.aggregate(aggregation, CambioDTO.class,
				CountDTO.class);
		totalElements = !countAggregationResult.getMappedResults().isEmpty()
				&& countAggregationResult.getMappedResults().get(0) != null
						? countAggregationResult.getMappedResults().get(0).getTotalElements()
						: 0;
		logger.info("Elemetos totales de la busqueda: " + totalElements);
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
			cFecProcesoCarga = new Criteria().andOperator(Criteria.where("fecProcesoCarga").gt(fecProcesoIni),
					Criteria.where("fecProcesoCarga").lte(fecProcesoFin));
		}
		
		Criteria cDelAndSubDel = null;
		Criteria cDel = null;
		Criteria cCveTipoRiesgo = null;
		Criteria cCveConsecuencia = null;
		Criteria cCveCasoRegistro = null;
		Criteria cCveEstadoRegistroList = null;
		Criteria cCveIdAccionRegistro = null;
		Criteria cCveLaudo = null;
		Criteria cNumNss = null;
		Criteria cRefRegistroPatronal = null;
		Criteria cCveClase = null;
		Criteria cDesRfc = null;
		Criteria cCveFraccion = null;
		Criteria cCveSituacionRegistro = null;
		Criteria cOrigenAlta = null;
		Criteria cIsOperative = null;
		boolean bcCveEstadoRegistro = false;
		
		if (cveDelegacion != null && cveDelegacion > 0 && cveSubdelegacion != null && cveSubdelegacion > 0) {
			Criteria delAsegurado = Criteria.where("cveDelegacionNss").is(cveDelegacion);
			Criteria delPatron = Criteria.where("cveDelRegPatronal").is(cveDelegacion);

			Criteria subdelAsegurado = Criteria.where("cveSubdelNss").is(cveSubdelegacion);
			Criteria subdelPatron = Criteria.where("cveSubDelRegPatronal").is(cveSubdelegacion);

			cDelAndSubDel = new Criteria().orOperator(new Criteria().andOperator(delAsegurado, subdelAsegurado),
					new Criteria().andOperator(delPatron, subdelPatron));

		} else {
			if (cveDelegacion != null && cveDelegacion > 0) {
				Criteria delAsegurado = Criteria.where("cveDelegacionNss").is(cveDelegacion);
				Criteria delPatron = Criteria.where("cveDelRegPatronal").is(cveDelegacion);
				cDel = new Criteria().orOperator(delAsegurado, delPatron);
			}
		}
		if (cveTipoRiesgo != null && cveTipoRiesgo > 0) {
			cCveTipoRiesgo = Criteria.where("cveTipoRiesgo").is(cveTipoRiesgo);
		}
		if (cveConsecuencia != null && cveConsecuencia >= 0) {
			cCveConsecuencia = Criteria.where("cveConsecuencia").is(cveConsecuencia);
		}
		if (cveCasoRegistro != null && cveCasoRegistro > 0) {
			cCveCasoRegistro = Criteria.where("cveCasoRegistro").is(cveCasoRegistro);
		}
		if (cveEstadoRegistroList != null && !cveEstadoRegistroList.isEmpty()) {
			cCveEstadoRegistroList = Criteria.where("cveEstadoRegistro").in(cveEstadoRegistroList);
			bcCveEstadoRegistro = true;
		}
		if (cveIdAccionRegistro != null && cveIdAccionRegistro > 0) {
			cCveIdAccionRegistro = Criteria.where("auditorias.cveIdAccionRegistro").is(cveIdAccionRegistro);
		}
		if (cveLaudo != null && cveLaudo > 0) {
			cCveLaudo = Criteria.where("cveLaudo").is(cveLaudo);
		}
		if (StringUtils.isNotBlank(numNss) && StringUtils.isNotEmpty(numNss)) {
			cNumNss = Criteria.where("numNss").is(numNss);
		}
		if (StringUtils.isNotBlank(refRegistroPatronal) && StringUtils.isNotEmpty(refRegistroPatronal)) {
			cRefRegistroPatronal = Criteria.where("refRegistroPatronal").is(refRegistroPatronal);
		}
		if (cveClase != null && cveClase > 0) {
			cCveClase = Criteria.where("cveClase").is(cveClase);
		}
		if (StringUtils.isNotBlank(rfc) && StringUtils.isNotEmpty(rfc)) {
			if (!(StringUtils.isNotBlank(origenAlta) && StringUtils.isNotEmpty(origenAlta)
					&& origenAlta.equals("EP"))) {
				cDesRfc = Criteria.where("desRfc").is(rfc);
			}
		}
		if (cveFraccion != null && cveFraccion > 0) {
			cCveFraccion = Criteria.where("cveFraccion").is(cveFraccion);
		}
		if (StringUtils.isNotBlank(cveSituacionRegistro) && StringUtils.isNotEmpty(cveSituacionRegistro)
				&& !cveSituacionRegistro.equals("-1")) {
			if (!cveSituacionRegistro.equals("2")) {
				cIsOperative = new Criteria().orOperator(Criteria.where("isPending").is(Boolean.FALSE));
				cCveSituacionRegistro = Criteria.where("cveSituacionRegistro")
						.is(Integer.parseInt(cveSituacionRegistro));
			} else {
				cIsOperative = Criteria.where("cveSituacionRegistro").is(Integer.parseInt(cveSituacionRegistro));
			}
		} else if ((isCasuistry == null || !isCasuistry) && isOperative != null && isOperative) {
			// se aniade filtro para que no retorne ningun registro en caso de que se haga
			// una busqueda sin filtro de situacion
			cIsOperative = new Criteria().andOperator(Criteria.where("isPending").is(Boolean.FALSE),
					Criteria.where("cveSituacionRegistro").is(1));
		} else if ((isCasuistry == null || !isCasuistry) && isApprover != null && isApprover) {
			cIsOperative = new Criteria().orOperator(Criteria.where("isPending").is(Boolean.TRUE),
					Criteria.where("cveSituacionRegistro").is(2));
		} else if (isCasuistry != null && isCasuistry) {
			cIsOperative = new Criteria().orOperator(Criteria.where("isPending").is(Boolean.FALSE),
					Criteria.where("isPending").is(null));
		}

		if (StringUtils.isNotBlank(origenAlta) && StringUtils.isNotEmpty(origenAlta)) {
			if (origenAlta.equals("EP")) {
				cOrigenAlta = Criteria.where("desRfc").is(RFC_IMSS);
			}
		}

		// Se inicia la construccion de las operaciones para la agregacion
		UnwindOperation unwind = Aggregation.unwind("auditorias");
		Criteria cFecBaja = null; 
		Criteria fecBaja = null;
		cFecBaja = Criteria.where("auditorias.fecBaja").is(null);
		fecBaja = Criteria.where("fecBaja").is(null);
		//Parametros de busqueda para filtro de baja
		Criteria cObjectOrigen = null; 
		Criteria cAltaOrigen = null;
		
        //Validacion de filtro de baja
		if (cveEstadoRegistroList!=null){	
			if(cveEstadoRegistroList.contains(10)||cveEstadoRegistroList.contains(11)) {
				fecBaja = null;
				cObjectOrigen = Criteria.where("objectIdOrigen").is(null);
				cAltaOrigen = Criteria.where("idOrigenAlta").is(null);
			}else {
				cAltaOrigen = Criteria.where("idOrigenAlta").is(null);
			}
		}
		
		if (fecProcesoIni == null && fecProcesoFin == null) {
			fecBaja = null;
			cObjectOrigen = Criteria.where("objectIdOrigen").is(null);
			cAltaOrigen = Criteria.where("idOrigenAlta").is(null);
		}
		
		String accionJson = "{ $addFields: { desAccionRegistro: '$auditorias.desAccionRegistro' } }";
		CustomAggregationOperation addFieldsAccion = new CustomAggregationOperation(accionJson);

		String fechaAccionJson = "{ $addFields: { fechaAccion: '$auditorias.fecAlta' } }";
		CustomAggregationOperation addFieldsFechaAccion = new CustomAggregationOperation(fechaAccionJson);
		String usuarioAccionccionJson = "{ $addFields: { usuarioAccion: '$auditorias.nomUsuario' } }";
		CustomAggregationOperation addFieldsUsuarioAccion = new CustomAggregationOperation(usuarioAccionccionJson);
        List<AggregationOperation> aggregationOperationList = null;
		        aggregationOperationList = Arrays.asList(unwind,AggregationUtils.validateMatchOp(fecBaja),
				AggregationUtils.validateMatchOp(cFecBaja), addFieldsAccion, addFieldsFechaAccion,
				addFieldsUsuarioAccion, AggregationUtils.validateMatchOp(cFecProcesoCarga),
				AggregationUtils.validateMatchOp(cDelAndSubDel), AggregationUtils.validateMatchOp(cDel),
				AggregationUtils.validateMatchOp(cCveTipoRiesgo), AggregationUtils.validateMatchOp(cCveConsecuencia),
				AggregationUtils.validateMatchOp(cCveCasoRegistro), 
				AggregationUtils.validateMatchOp(cCveEstadoRegistroList),
				AggregationUtils.validateMatchOp(cIsOperative), AggregationUtils.validateMatchOp(cCveIdAccionRegistro),
				AggregationUtils.validateMatchOp(cCveLaudo), AggregationUtils.validateMatchOp(cNumNss),
				AggregationUtils.validateMatchOp(cRefRegistroPatronal), AggregationUtils.validateMatchOp(cCveClase),
				AggregationUtils.validateMatchOp(cDesRfc), AggregationUtils.validateMatchOp(cCveFraccion),
				AggregationUtils.validateMatchOp(cCveSituacionRegistro), AggregationUtils.validateMatchOp(cOrigenAlta),
				AggregationUtils.validateMatchOp(cObjectOrigen),AggregationUtils.validateMatchOp(cAltaOrigen));
		   aggregationOperationList = aggregationOperationList.stream().filter(Objects::nonNull)
				.collect(Collectors.toList());
		return aggregationOperationList;
	}

	private ResponseDTO<List<MovimientosOutputDTO>> getChangesConsequence(@Valid Integer cveDelegacion,
			@Valid Integer cveSubdelegacion, @Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia,
			@Valid Integer cveCasoRegistro, @Valid String fromMonth, @Valid String fromYear, @Valid String toMonth,
			@Valid String toYear, @Valid List<Integer> cveEstadoRegistroList, @Valid String numNss,
			@Valid String refRegistroPatronal, @Valid String cveSituacionRegistro, @Valid String fromDay,
			@Valid String toDay, @Valid Integer cveIdAccionRegistro, @Valid String rfc, @Valid Integer cveClase,
			@Valid Integer cveFraccion, @Valid Integer cveLaudo, @Valid String origenAlta, @Valid Long page,
			@Valid Long skipOffset, @Valid Long size, @Valid List<AggregationOperation> customAggregarionList,
			@Valid Boolean isOperative, @Valid Boolean isApprover, @Valid Boolean isCasuistry, @Valid Long skipCounter) {

		ResponseDTO<List<CambioDTO>> responseCambios = getChanges(cveDelegacion, cveSubdelegacion, cveTipoRiesgo,
				cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList, numNss,
				refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc, cveClase,
				cveFraccion, cveLaudo, origenAlta, customAggregarionList, page, skipOffset, size, Boolean.TRUE,
				isOperative, isApprover, isCasuistry, skipCounter);
		List<CambioDTO> listCambios = responseCambios.getData();
		ResponseDTO<List<MovimientosOutputDTO>> response = new ResponseDTO<>();

		if (!listCambios.isEmpty()) {
			response.setData(llenarDatosMovimientos(listCambios));
		} else {
			response.setData(new ArrayList<>());
		}
		return response;
	}

	private ResponseDTO<Void> getCount(@Valid Integer cveDelegacion, @Valid Integer cveSubdelegacion,
			@Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia, @Valid Integer cveCasoRegistro,
			@Valid String fromMonth, @Valid String fromYear, @Valid String toMonth, @Valid String toYear,
			@Valid List<Integer> cveEstadoRegistroList, @Valid String numNss, @Valid String refRegistroPatronal,
			@Valid String cveSituacionRegistro, @Valid String fromDay, @Valid String toDay,
			@Valid Integer cveIdAccionRegistro, @Valid String rfc, @Valid Integer cveClase, @Valid Integer cveFraccion,
			@Valid Integer cveLaudo, @Valid String origenAlta, @Valid MatchOperation customCriteria,
			@Valid Boolean isOperative, @Valid Boolean isApprover, @Valid Boolean isCasuistry) {
		Long count = getChangeCount(cveDelegacion, cveSubdelegacion, cveTipoRiesgo, cveConsecuencia, cveCasoRegistro,
				fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList, numNss, refRegistroPatronal,
				cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc, cveClase, cveFraccion, cveLaudo,
				origenAlta, customCriteria, isOperative, isApprover, isCasuistry);
		logger.info(
				"------------------------------------Numero de registros de cambios--------------------------------");
		logger.info(MessageFormat.format("Numero de registros totales para la consulta: {0}", count));
		logger.info(
				"--------------------------------------------------------------------------------------------------");
		ResponseDTO<Void> response = new ResponseDTO<>();
		response.setTotalElements(count);
		return response;
	}

	
	@Override
	public ResponseDTO<List<MovimientosOutputDTO>> findCambiosMovimientos(@Valid Integer cveDelegacion,
			@Valid Integer cveSubdelegacion, @Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia,
			@Valid Integer cveCasoRegistro, @Valid String fromMonth, @Valid String fromYear, @Valid String toMonth,
			@Valid String toYear, @Valid List<Integer> cveEstadoRegistroList, @Valid String numNss,
			@Valid String refRegistroPatronal, @Valid String cveSituacionRegistro, @Valid String fromDay,
			@Valid String toDay, @Valid Integer cveIdAccionRegistro, @Valid String rfc, @Valid Integer cveClase,
			@Valid Integer cveFraccion, @Valid Integer cveLaudo, @Valid Long page, @Valid Long totalElements,
			@Valid String origenAlta, @Valid Long totalElementsMovement, @Valid Long changesMinorThanMovements,
			@Valid Boolean isOperative, @Valid Boolean isApprover, @Valid Boolean isCasuistry) throws BusinessException {

		ResponseDTO<List<MovimientosOutputDTO>> response = new ResponseDTO<>();
		ResponseDTO<Void> changesCount = null;
		ResponseDTO<Void> movementsCount = null;
		ResponseDTO<List<MovimientosOutputDTO>> movementResponse = new ResponseDTO<>();
		ResponseDTO<List<MovimientosOutputDTO>> changesResponse = new ResponseDTO<>();
		boolean onlyChanges = Boolean.FALSE;
		// Conteo de registros de cambios
		if (totalElements == null) {
			changesCount = getCount(cveDelegacion, cveSubdelegacion, cveTipoRiesgo, cveConsecuencia, cveCasoRegistro,
					fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList, numNss, refRegistroPatronal,
					cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc, cveClase, cveFraccion, cveLaudo,
					origenAlta, null, isOperative, isApprover, isCasuistry);
			// Conteo de registros de movimientos
			movementsCount = movimientosRepository.getCount(cveDelegacion, cveSubdelegacion, cveTipoRiesgo,
					cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList,
					numNss, refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc,
					cveClase, cveFraccion, cveLaudo, origenAlta, null, isOperative, isApprover, isCasuistry);
		}
		response.setTotalElementsMovement(
				movementsCount != null ? movementsCount.getTotalElements() : totalElementsMovement);
		// Se obtiene el tamaño de la respuesta y se valida la pagina
		ParametroDTO parametro = parametroRepository.findOneByCve("elementsPaginator").orElse(new ParametroDTO());
		Long size = parametro.getDesParametro() != null ? Long.parseLong(parametro.getDesParametro()) : 1L;
		page = PaginationUtils.validatePage(page);
		// Se agregan los conteos a la respuesta asi como la pagina y el tamaño de
		// pagina
		response.setTotalElements((changesCount != null && movementsCount != null)
				? (changesCount.getTotalElements() + movementsCount.getTotalElements())
				: totalElements);
		response.setPage(page);
		response.setSize(size);
		response.setTotalRows(PaginationUtils.getTotalRows(response.getTotalElements(), size));

		// Se validan los offsets que se manejaran y si hay cambios con nss menor que el
		// menor de movimientos
		response.setChangesMinorThanMovements(changesMinorThanMovements == null ? 0 : changesMinorThanMovements);
		long movementsOffset = 0;
		CountMovementsChanges countAll = new CountMovementsChanges();
		if (page > 1) {
			
			countAll = adjustOffset2(response, cveDelegacion, cveSubdelegacion, cveTipoRiesgo, cveConsecuencia,
					cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList, numNss,
					refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc, cveClase,
					cveFraccion, cveLaudo, page, origenAlta, size, isOperative, isApprover, isCasuistry);
			
			movementsOffset = countAll.getMovementsOffSet();

		}
		long changesOffset = 0;
		long normaQueryFirstPage = (response.getChangesMinorThanMovements() / size) + 1L;
		long normalQuerySkipElements = response.getChangesMinorThanMovements() % size;
		
		// Se ejecuta la busqueda a la coleccion de cambios
		long pagesOfChanges = PaginationUtils.calculatePages(response.getChangesMinorThanMovements(), size);
		if (response.getChangesMinorThanMovements() > 0 && page <= pagesOfChanges) {
			changesResponse = getChangesConsequence(cveDelegacion, cveSubdelegacion, cveTipoRiesgo, cveConsecuencia,
					cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList, numNss,
					refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc, cveClase,
					cveFraccion, cveLaudo, origenAlta, page, 0L, size * 2, null,
					isOperative, isApprover, isCasuistry, 
					ObjectUtils.existeValor(countAll.getPosicionChange()) ? countAll.getPosicionChange() : null);
			changesResponse.setData(PaginationUtils.sortList(changesResponse.getData()));
			long changeSize = page == pagesOfChanges ? normalQuerySkipElements : size;
			PaginationUtils.completePage(response, changesResponse, changeSize);
		}
		if (page >= normaQueryFirstPage) {
			long normalQueryPage = PaginationUtils.calculateSubConsultPage(page, normaQueryFirstPage);
			long normalQuerySkipOffset = PaginationUtils.calculateSkipOffset(page, normaQueryFirstPage, size,
					normalQuerySkipElements);
			// Se ejecuta la busqueda a la coleccion de movimientos
			movementResponse = movimientosRepository.getMovements(cveDelegacion, cveSubdelegacion, cveTipoRiesgo,
					cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList,
					numNss, refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc,
					cveClase, cveFraccion, cveLaudo, origenAlta, normalQueryPage,
					movementsOffset + normalQuerySkipOffset, size * 2, null, null,
					isOperative, isApprover, isCasuistry, 
					ObjectUtils.existeValor(countAll.getPosicionMov()) ? countAll.getPosicionMov() : null );
			if (movementResponse.getData() == null || movementResponse.getData().isEmpty()) {
				// Se trata de obtener el ultimo nss que hubo de movimientos
				CustomAggregationOperation sort = new CustomAggregationOperation("{ $sort: { 'aseguradoDTO.numNss': -1,"
						+ "'incapacidadDTO.porPorcentajeIncapacidad': 1, 'incapacidadDTO.numDiasSubsidiados': 1 } }");
				movementResponse = movimientosRepository.getMovements(cveDelegacion, cveSubdelegacion, cveTipoRiesgo,
						cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList,
						numNss, refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc,
						cveClase, cveFraccion, cveLaudo, origenAlta, 1L, 0L, 1L, null,
						sort, isOperative, isApprover, isCasuistry, null);
				if (movementResponse.getData() == null || movementResponse.getData().isEmpty()) {
					// Si nunca hubo movimientos entonces la busqueda de cambios se ejecuta con
					// paginado normal
					changesResponse = getChangesConsequence(cveDelegacion, cveSubdelegacion, cveTipoRiesgo,
							cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear,
							cveEstadoRegistroList, numNss, refRegistroPatronal, cveSituacionRegistro, fromDay, toDay,
							cveIdAccionRegistro, rfc, cveClase, cveFraccion, cveLaudo, origenAlta, page, 0L, size * 2,
							null, isOperative, isApprover, isCasuistry,
							ObjectUtils.existeValor(countAll.getPosicionChange()) ? countAll.getPosicionChange() : null);
					changesResponse.setData(PaginationUtils.sortList(changesResponse.getData()));
					PaginationUtils.completePage(response, changesResponse, size);
					return response;
				} else {
					// Si hubo movimientos pero ya se acabaron, entonces se calcula la pagina actual
					// con el skip offset correspondiente
					changesOffset = -response.getTotalElementsMovement();
					onlyChanges = Boolean.TRUE;
				}
			}
			// Se ejecuta la busqueda a la coleccion de cambios

			MatchOperation nssMatch = null;
			if(ObjectUtils.existeValor(countAll.getNumNssChanges())) {
				nssMatch = Aggregation.match(Criteria.where("numNss").gte(countAll.getNumNssChanges()));
			}else {
				nssMatch = Aggregation
						.match(Criteria.where("numNss").gte(movementResponse.getData().get(0).getNumNss()));				
			}
			
			changesResponse = getChangesConsequence(cveDelegacion, cveSubdelegacion, cveTipoRiesgo, cveConsecuencia,
					cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList, numNss,
					refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc, cveClase,
					cveFraccion, cveLaudo, origenAlta, page,
					onlyChanges ? changesOffset : -PaginationUtils.getSkipElements(page, size), size * page,
					(page > 1 && !onlyChanges) ? Collections.singletonList(nssMatch) : null, isOperative, isApprover, isCasuistry,
					ObjectUtils.existeValor(countAll.getPosicionChange()) ? countAll.getPosicionChange() : null);
			if (onlyChanges) {
				movementResponse.setData(null);
			}
		}

		// Se unifican ambas listas de registros
		ResponseDTO<List<MovimientosOutputDTO>> unifiedResponse = new ResponseDTO<>();
		PaginationUtils.safeAddList(unifiedResponse, movementResponse.getData());
		PaginationUtils.safeAddList(unifiedResponse, changesResponse.getData());
		unifiedResponse.setData(PaginationUtils.sortList(unifiedResponse.getData()));
		PaginationUtils.completePage(response, unifiedResponse, size);
		return response;
	}
	
	private CountMovementsChanges adjustOffset2(ResponseDTO<List<MovimientosOutputDTO>> response, Integer cveDelegacion,
			Integer cveSubdelegacion, Integer cveTipoRiesgo, Integer cveConsecuencia, Integer cveCasoRegistro,
			String fromMonth, String fromYear, String toMonth, String toYear, List<Integer> cveEstadoRegistroList,
			String numNss, String refRegistroPatronal, String cveSituacionRegistro, String fromDay, String toDay,
			Integer cveIdAccionRegistro, String rfc, Integer cveClase, Integer cveFraccion, Integer cveLaudo, Long page,
			String origenAlta, Long size, Boolean isOperative, Boolean isApprover, Boolean isCasuistry) {
		Long evenOffset = 0L;
		Long oddOffset = 0L;
		int counter = 1;
		int times = 0;
		Long changesOffset = 0L;
		long movementOffset = 0L;
		long ternaria;
		int countMovements = 0;
		long minRegistros = (page - 1) * size;
		long maxRegistros = page * size;
		long pageSkipElements = ((page - 1) * size);
		String registrosComplete = "";
		long totalMovement = response.getTotalElementsMovement();
		if(pageSkipElements > totalMovement) {
			pageSkipElements = totalMovement;
		}
		CountMovementsChanges resp = new CountMovementsChanges();
		
		do {
			if (counter >= 50) {
				resp.setMovementsOffSet(0L);
				return resp;
			}
 
			long floorDivChanges = Math.floorDiv(response.getChangesMinorThanMovements(), size) * size;
			long pageSkipElementsChange = response.getChangesMinorThanMovements() > 0
					? floorDivChanges + (response.getChangesMinorThanMovements() - floorDivChanges)
					: 0;
			
			ternaria = pageSkipElements < response.getChangesMinorThanMovements() ? pageSkipElements
					: pageSkipElementsChange;
			movementOffset = pageSkipElements - changesOffset - ternaria;
			countMovements = countMovements + 1;
			if(movementOffset <= 0) {
				movementOffset = pageSkipElements - countMovements;
			}

			// Se hace una primer consulta para saber el nss consecutivo que se trabajara
			ResponseDTO<List<MovimientosOutputDTO>> movementResponse = movimientosRepository.getMovements(cveDelegacion,
					cveSubdelegacion, cveTipoRiesgo, cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth,
					toYear, cveEstadoRegistroList, numNss, refRegistroPatronal, cveSituacionRegistro, fromDay, toDay,
					cveIdAccionRegistro, rfc, cveClase, cveFraccion, cveLaudo, origenAlta, page, movementOffset, 1L,
					null, null, isOperative, isApprover, isCasuistry, null);
			// Se realiza la consulta de conteo para todos los registros que tengan un nss
			// menor al actual de la consulta
			if (movementResponse.getData() == null || movementResponse.getData().isEmpty()) {
				CustomAggregationOperation sort = new CustomAggregationOperation("{ $sort: { 'aseguradoDTO.numNss': -1,"
						+ "'incapacidadDTO.porPorcentajeIncapacidad': 1, 'incapacidadDTO.numDiasSubsidiados': 1 } }");
				movementResponse = movimientosRepository.getMovements(cveDelegacion, cveSubdelegacion, cveTipoRiesgo,
						cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList,
						numNss, refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc,
						cveClase, cveFraccion, cveLaudo, origenAlta, 1L, 0L, 1L, null, sort, isOperative,
						isApprover, isCasuistry, null);
				// Solo sobran cambios, no es necesario calcular un offset
				if (page.equals(1L)) {
					response.setChangesMinorThanMovements(0L);
				}
				if (movementResponse.getData() == null || movementResponse.getData().isEmpty()) {
					// Nunca hubo movimientos, entonces se retorna un offset de 0
					resp.setMovementsOffSet(0L);
					return resp;
				}
			}
			String nss = movementResponse.getData().get(0).getNumNss();
			MatchOperation nssMatch = Aggregation.match(Criteria.where("numNss").lte(nss));
			ResponseDTO<Void> changesCount = getCount(cveDelegacion, cveSubdelegacion, cveTipoRiesgo, cveConsecuencia,
					cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList, numNss,
					refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc, cveClase,
					cveFraccion, cveLaudo, origenAlta, nssMatch, isOperative, isApprover, isCasuistry);
			if (page.equals(1L)) {
				MatchOperation eqNssMatch = Aggregation.match(Criteria.where("numNss").is(nss));
				ResponseDTO<Void> changesCountEqNss = getCount(cveDelegacion, cveSubdelegacion, cveTipoRiesgo,
						cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList,
						numNss, refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc,
						cveClase, cveFraccion, cveLaudo, origenAlta, eqNssMatch, isOperative, isApprover, isCasuistry);
				response.setChangesMinorThanMovements(
						changesCount.getTotalElements() - changesCountEqNss.getTotalElements());
				if (changesCount.getTotalElements() > 0L) {
					resp.setMovementsOffSet(0L);
					resp.setNumNssChanges(nss);
					return resp;
				}
			}
			
			boolean even = counter % 2 == 0;
			if (even) {
				evenOffset = changesCount.getTotalElements() - response.getChangesMinorThanMovements();
			} else {
				oddOffset = changesCount.getTotalElements() - response.getChangesMinorThanMovements();
			}
			counter++;
			if (counter >= 3 && (evenOffset.equals(changesCount.getTotalElements())
					|| oddOffset.equals(changesCount.getTotalElements()))) {
				times++;
			} else {
				times = 0;
			}

			if (times >= totalMovement) {
				if (oddOffset.compareTo(evenOffset) > 0) {
					response.setRecordsToDiscard(oddOffset - evenOffset);
					resp.setMovementsOffSet(-oddOffset);
					resp.setNumNssChanges(nss);
					return resp;
				} else {
					response.setRecordsToDiscard(evenOffset - oddOffset);
					resp.setMovementsOffSet(-evenOffset);
					resp.setNumNssChanges(nss);
					return resp;
				}
			} else if (!changesOffset
					.equals(changesCount.getTotalElements() - response.getChangesMinorThanMovements())) {
				changesOffset = changesCount.getTotalElements() - response.getChangesMinorThanMovements();
			} else {
				if(changesOffset <= 9) {
					resp.setMovementsOffSet(-changesOffset);
					resp.setNumNssChanges(nss);
					return resp;
				}
			}
			
			long numRegistrosAll = changesCount.getTotalElements() + movementOffset;
			if(numRegistrosAll >= minRegistros && numRegistrosAll <= maxRegistros) {
				registrosComplete = "min";
			}
			
			if(registrosComplete.equals("min") && numRegistrosAll < minRegistros) {
				
				ResponseDTO<Void> changesCountGte = getCount(cveDelegacion, cveSubdelegacion, cveTipoRiesgo, cveConsecuencia,
						cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistroList, numNss,
						refRegistroPatronal, cveSituacionRegistro, fromDay, toDay, cveIdAccionRegistro, rfc, cveClase,
						cveFraccion, cveLaudo, origenAlta, nssMatch, isOperative, isApprover, isCasuistry);				
				
				movementOffset = movementOffset + 1;
				Long registrosNssMatch = minRegistros - (changesCountGte.getTotalElements() + movementOffset);
				
				resp.setMovementsOffSet(-movementOffset);
				resp.setNumNssChanges(nss);
				resp.setPosicionMov(movementOffset);
				resp.setPosicionChange(registrosNssMatch);
				return resp;
			}
			
		} while (true);
	}

	private String buildChangesGroup() {
		return "{ $group: { _id: '$_id', auditorias: { $push: '$auditorias' }, cveCasoRegistro: { $first: '$cveCasoRegistro' }, "
				+ "cveCodigoError: { $first: '$cveCodigoError' }, cveConsecuencia: { $first: '$cveConsecuencia' }, "
				+ "cveDelegacionAtencion: { $first: '$cveDelegacionAtencion' }, cveDelegacionNss: { $first: '$cveDelegacionNss' }, "
				+ "cveEstadoRegistro: { $first: '$cveEstadoRegistro' }, cveLaudo: { $first: '$cveLaudo' }, "
				+ "cveOrigenArchivo: { $first: '$cveOrigenArchivo' }, cveSituacionRegistro: { $first: '$cveSituacionRegistro' }, "
				+ "cveSubDelAtencion: { $first: '$cveSubDelAtencion' }, cveSubdelNss: { $first: '$cveSubdelNss' }, "
				+ "cveTipoIncapacidad: { $first: '$cveTipoIncapacidad' }, cveTipoRiesgo: { $first: '$cveTipoRiesgo' }, "
				+ "cveUmfAdscripcion: { $first: '$cveUmfAdscripcion' }, cveUmfExp: { $first: '$cveUmfExp' }, "
				+ "cveUmfPagadora: { $first: '$cveUmfPagadora' }, desCasoRegistro: { $first: '$desCasoRegistro' }, "
				+ "desCodigoError: { $first: '$desCodigoError' }, desConsecuencia: { $first: '$desConsecuencia' }, "
				+ "desDelegacionNss: { $first: '$desDelegacionNss' },desEstadoRegistro: { $first: '$desEstadoRegistro' }, "
				+ "desLaudo: { $first: '$desLaudo' },desOcupacion: { $first: '$desOcupacion' }, "
				+ "desSituacionRegistro: { $first: '$desSituacionRegistro' },desSubDelNss: { $first: '$desSubDelNss' }, "
				+ "desTipoIncapacidad: { $first: '$desTipoIncapacidad' },desTipoRiesgo: { $first: '$desTipoRiesgo' }, "
				+ "desUmfAdscripcion: { $first: '$desUmfAdscripcion' },fecAltaIncapacidad: { $first: '$fecAltaIncapacidad' }, "
				+ "fecFin: { $first: '$fecFin' }, fecInicio: { $first: '$fecInicio' },fecIniPension: { $first: '$fecIniPension' }, "
				+ "fecAtencion: { $first: '$fecAtencion' }, fecAccidente: { $first: '$fecAccidente' }, fecExpedicionDictamen: { $first: '$fecExpedicionDictamen' }, "
				+ "fecProcesoCarga: { $first: '$fecProcesoCarga' },nomAsegurado: { $first: '$nomAsegurado' }, "
				+ "numCicloAnual: { $first: '$numCicloAnual' },numDiasSubsidiados: { $first: '$numDiasSubsidiados' }, "
				+ "numIndice: { $first: '$numIndice' },numMatMedAutCdst: { $first: '$numMatMedAutCdst' }, "
				+ "numMatMedTratante: { $first: '$numMatMedTratante' },numNss: { $first: '$numNss' }, "
				+ "numSalarioDiario: { $first: '$numSalarioDiario' },objectIdOrigen: { $first: '$objectIdOrigen' }, "
				+ "porcentajeIncapacidad: { $first: '$porcentajeIncapacidad' },refCurp: { $first: '$refCurp' }, "
				+ "refFolioOriginal: { $first: '$refFolioOriginal' },refPrimerApellido: { $first: '$refPrimerApellido' }, "
				+ "refSegundoApellido: { $first: '$refSegundoApellido' },origenAlta: { $first: '$origenAlta' }, "
				+ "nssExisteBDTU: { $first: '$nssExisteBDTU' },refRegistroPatronal: { $first: '$refRegistroPatronal' }, "
				+ "desRazonSocial: { $first: '$desRazonSocial' },desRfc: { $first: '$desRfc' },"
				+ "cveClase: { $first: '$cveClase' },cveDelRegPatronal: { $first: '$cveDelRegPatronal' },"
				+ "cveFraccion: { $first: '$cveFraccion' },numPrima: { $first: '$numPrima' },"
				+ "cveSubDelRegPatronal: { $first: '$cveSubDelRegPatronal' },desClase: { $first: '$desClase' },"
				+ "desDelRegPatronal: { $first: '$desDelRegPatronal' },desFraccion: { $first: '$desFraccion' },"
				+ "desPrima: { $first: '$desPrima' },desSubDelRegPatronal: { $first: '$desSubDelRegPatronal' },"
				+ "fecAlta: { $first: '$fecAlta' },_class: { $first: '$_class' },desAccionRegistro: { $first: '$desAccionRegistro' },"
				+ "fechaAccion: { $first: '$fechaAccion' },usuarioAccion: { $first: '$usuarioAccion' },    } }";
	}

	@Override
	public List<CambioDTO> findMovimientosReporte(ReporteCasuisticaInputDTO input) throws BusinessException {

		logger.debug("Valores de entrada: {}", input.toString());
		logger.info("Delegacion: " + input.getCveDelegacion());
		logger.info("Sub Delegacion: " + input.getCveSubdelegacion());

		ResponseDTO<List<CambioDTO>> changesResponse = getChanges(input.getCveDelegacion(), input.getCveSubdelegacion(),
				input.getCveTipoRiesgo(), input.getCveConsecuencia(), input.getCveCasoRegistro(), input.getFromMonth(),
				input.getFromYear(), input.getToMonth(), input.getToYear(), input.getCveEstadoRegistroList(),
				input.getNumNss(), input.getRefRegistroPatronal(), input.getCveSituacionRegistro(), input.getFromDay(),
				input.getToDay(), input.getCveIdAccionRegistro(), input.getRfc(), input.getCveClase(),
				input.getCveFraccion(), input.getCveLaudo(), input.getOrigenAlta(), null, null, 0L, 1L, Boolean.FALSE,
				input.getIsOperative(), input.getIsApprover(), input.getIsCasuistry(), null);
		logger.info("Numero de registros de cambios: " + changesResponse.getData().size());

		ResponseDTO<List<DetalleRegistroDTO>> movementsList = movimientosRepository.getMovements(
				input.getCveDelegacion(), input.getCveSubdelegacion(), input.getCveTipoRiesgo(),
				input.getCveConsecuencia(), input.getCveCasoRegistro(), input.getFromMonth(), input.getFromYear(),
				input.getToMonth(), input.getToYear(), input.getCveEstadoRegistroList(), input.getNumNss(),
				input.getRefRegistroPatronal(), input.getCveSituacionRegistro(), input.getFromDay(), input.getToDay(),
				input.getCveIdAccionRegistro(), input.getRfc(), input.getCveClase(), input.getCveFraccion(),
				input.getCveLaudo(), input.getOrigenAlta(), 1L, 0L, Boolean.FALSE, 1L, null, null,
				input.getIsOperative(), input.getIsApprover(), input.getIsCasuistry(), null);
		logger.info("Numero de registros de movimientos: " + movementsList.getData().size());
		
		List<CambioDTO> movementToChangeList = new ArrayList<>();
		List<DetalleRegistroDTO> movementTo = new ArrayList<>();
		if (movementsList.getData() != null && movementsList.getData().size() > 0) {
			movementTo = getDatosCambios(movementsList.getData());
			movementToChangeList = movementTo.stream().map(Utils::detalleRegistroDTOtoCambioDTO)
					.collect(Collectors.toList());
			
		}
		
		List<CambioDTO> responseList = new ArrayList<>();
		responseList.addAll(changesResponse.getData());
		responseList.addAll(movementToChangeList);

		return responseList;
	}

	private List<DetalleRegistroDTO> getDatosCambios(List<DetalleRegistroDTO> list){
		for (DetalleRegistroDTO item : list) {
			item = getDatoCambio(item);
		}
		return list;
	}
	
	private DetalleRegistroDTO getDatoCambio(DetalleRegistroDTO detalle) {
		
		List<AggregationOperation> aggregationOperationList = 
				Arrays.asList(Aggregation.match(Criteria.where("objectIdOrigen").is(detalle.getObjectIdArchivoDetalle())));
				aggregationOperationList = aggregationOperationList.stream()
						.filter(Objects::nonNull)
						.collect(Collectors.toList());
				
		CustomAggregationOperation sort = new CustomAggregationOperation("{ $sort: { 'fecBaja':-1 } }");		
		aggregationOperationList.add(sort);
		aggregationOperationList.add(Aggregation.limit(1));
		
		TypedAggregation<CambioDTO> aggregation = Aggregation.newAggregation(CambioDTO.class, aggregationOperationList)
				.withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
		
		AggregationResults<CambioDTO> aggregationResultsCambios = mongoOperations.aggregate(aggregation,
				CambioDTO.class);
		
		List<CambioDTO> listCambios = aggregationResultsCambios.getMappedResults();


		if(ObjectUtils.existeValor(detalle.getAuditorias())) {
			Optional<AuditoriaDTO> ultimaAuditoria = detalle.getAuditorias()
					.stream()
					.filter(au-> !ObjectUtils.existeValor(au.getFecBaja()))
					.findFirst();
			
			if(ultimaAuditoria.isPresent()) {
				detalle.setDesSituacionRegistro(ultimaAuditoria.get().getDesSituacionRegistro());
			}
		}
		
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
	
	private String validaCadena(String cadena) {
		String result = StringUtils.isNotBlank(cadena) && StringUtils.isNotEmpty(cadena) ? cadena.toString() : "";
		return result;
	}

	private List<MovimientosOutputDTO> llenarDatosMovimientos(List<CambioDTO> listCambios) {
		List<MovimientosOutputDTO> listMovimientos = null;
		if (listCambios != null && !listCambios.isEmpty()) {
			listMovimientos = new ArrayList<MovimientosOutputDTO>();
			for (CambioDTO cambioDTO : listCambios) {
				MovimientosOutputDTO movimientosOutputDTO = new MovimientosOutputDTO();
				movimientosOutputDTO.setDesConsecuencia(cambioDTO.getDesConsecuencia());
				movimientosOutputDTO.setCveConsecuencia(String.valueOf(cambioDTO.getCveConsecuencia()));
				movimientosOutputDTO.setDesEstadoRegistro(validaCadena(cambioDTO.getDesEstadoRegistro()));
				movimientosOutputDTO.setCveEstadoRegistro(cambioDTO.getCveEstadoRegistro());
				movimientosOutputDTO.setDesTipoRiesgo(cambioDTO.getDesTipoRiesgo());
				movimientosOutputDTO.setCveTipoRiesgo(String.valueOf(cambioDTO.getCveTipoRiesgo()));
				movimientosOutputDTO.setFecFin(cambioDTO.getFecFin() != null ? cambioDTO.getFecFin().toString() : "");
				movimientosOutputDTO
						.setFecInicio(cambioDTO.getFecInicio() != null ? cambioDTO.getFecInicio().toString() : "");
				Optional<AuditoriaDTO> auditoria = cambioDTO.getAuditorias().stream()
						.filter(audit -> audit.getFecBaja() == null).findFirst();
				movimientosOutputDTO.setFecActualizacion(auditoria.isPresent() ? auditoria.get().getFecAlta() : null);
				StringBuffer nomCompleto = new StringBuffer();
				nomCompleto.append(validaCadena(cambioDTO.getRefPrimerApellido()));
				nomCompleto.append(" ");
				nomCompleto.append(validaCadena(cambioDTO.getRefSegundoApellido()));
				nomCompleto.append(" ");
				nomCompleto.append(validaCadena(cambioDTO.getNomAsegurado()));

				movimientosOutputDTO.setNomCompletoAsegurado(nomCompleto.toString());

				movimientosOutputDTO.setNumDiasSubsidiados(cambioDTO.getNumDiasSubsidiados());
				movimientosOutputDTO.setNumFolioMovtoOriginal(validaCadena(cambioDTO.getRefFolioOriginal()));
				movimientosOutputDTO.setNumNss(validaCadena(cambioDTO.getNumNss()));
				movimientosOutputDTO.setPorcentajeIncapacidad(
						cambioDTO.getPorcentajeIncapacidad() != null ? cambioDTO.getPorcentajeIncapacidad().toString()
								: null);
				movimientosOutputDTO.setRefRegistroPatronal(validaCadena(cambioDTO.getRefRegistroPatronal()));
				movimientosOutputDTO.setObjectId(cambioDTO.getObjectIdCambio());
				movimientosOutputDTO.setPosition(cambioDTO.getNumIndice());
				movimientosOutputDTO.setObjectIdOrigen(
						cambioDTO.getObjectIdOrigen() != null ? cambioDTO.getObjectIdOrigen().toString() : null);
				movimientosOutputDTO.setIsChange(Boolean.TRUE);
				movimientosOutputDTO.setDesSituacionRegistro(cambioDTO.getDesSituacionRegistro());
				movimientosOutputDTO.setCveSituacionRegistro(cambioDTO.getCveSituacionRegistro());
				if (cambioDTO.getCveConsecuencia() == 0) {
					movimientosOutputDTO.setDesConsecuencia("Sin consecuencias");
				}
				if (cambioDTO.getCveConsecuencia() == 6) {
					movimientosOutputDTO.setDesConsecuencia("Con Valuación inicial provisional posterior a la fecha de alta");
				}
				movimientosOutputDTO.setIdOrigenAlta(cambioDTO.getIdOrigenAlta());
				listMovimientos.add(movimientosOutputDTO);
			}

		}

		return listMovimientos;
	}

	@Override
	public DetalleRegistroDTO getDetalleMovimiento(@Valid String objectId, @Valid String numNss,
			@Valid Integer position, @Valid String numFolioMovtoOriginal) {
		Query query = new Query();
		ObjectId id = new ObjectId(objectId);
		query.addCriteria(Criteria.where("_id").is(id));
		query.addCriteria(Criteria.where("numNss").is(numNss));
		query.addCriteria(Criteria.where("numIndice").is(position));
		query.addCriteria(Criteria.where("refFolioOriginal").is(numFolioMovtoOriginal));
		List<CambioDTO> listArhivos = mongoOperations.find(query, CambioDTO.class);
		CambioDTO cambioDTO = listArhivos.get(0);
		if (cambioDTO != null && cambioDTO.getNumPrima() != null) {
			cambioDTO.setDesPrima(cambioDTO.getNumPrima().toString());
		}
		return Utils.cambioDTOtoDetalleRegistroDTO(cambioDTO != null ? cambioDTO : new CambioDTO(), position);
	}
	
	@Override
	public CambioDTO updateCambio(MovimientoRequest input) throws BusinessException {
		CambioDTO cambioDTO = getCambio(input);
		DateFormat df = new SimpleDateFormat("yyyy");
		if (cambioDTO.getCveOrigenArchivo() != null && !cambioDTO.getCveOrigenArchivo().equals("MN")) {
			cambioDTO.setFecBaja(DateUtils.getSysDateMongoISO());
		} else if (cambioDTO.getCveOrigenArchivo() != null && cambioDTO.getCveOrigenArchivo().equals("MN")
				&& cambioDTO.getIdOrigenAlta() != null) {
			cambioDTO.setFecBaja(DateUtils.getSysDateMongoISO());
		} else if (cambioDTO.getCveOrigenArchivo() != null && cambioDTO.getCveOrigenArchivo().equals("MN")) {
			cambioDTO.setIsPending(Boolean.FALSE);
		}
		// Pendiente de aprobar
		if (cambioDTO.getCveSituacionRegistro().intValue() == 2) {
			Query querySituacion = new Query();
			querySituacion.addCriteria(Criteria.where("cveIdSituacionRegistro").is(input.getCveSituacionRegistro()));
			MccSituacionRegistro situacionRegistro = mongoOperations.findOne(querySituacion,
					MccSituacionRegistro.class);
			Query queryAccion = new Query();
			queryAccion.addCriteria(Criteria.where("cveIdAccionRegistro").is(input.getCveIdAccionRegistro()));
			MccAccionRegistro accionRegistro = mongoOperations.findOne(queryAccion, MccAccionRegistro.class);

			if (cambioDTO != null) {
				cambioDTO.setCveSituacionRegistro(Integer.valueOf(situacionRegistro.getCveIdSituacionRegistro()));
				cambioDTO.setDesSituacionRegistro(situacionRegistro.getDesSituacionRegistro());

				if (accionRegistro.getCveIdAccionRegistro().intValue() == AccionRegistroEnum.BAJA_PENDIENTE.getClave()
						|| accionRegistro.getCveIdAccionRegistro().intValue() == AccionRegistroEnum.ELIMINACION
								.getClave()) {
					calculaRn68(cambioDTO);
				}

			}
			if (cambioDTO.getCveOrigenArchivo() != null
					&& cambioDTO.getCveOrigenArchivo().equals(IdentificadorArchivoEnum.MANUAL.getIdentificador())) {
				cambioDTO.setFecFin(cambioDTO.getFecAltaIncapacidad());
				cambioDTO.setCveCodigoError("0");
			}

			if(!cambioDTO.getAuditorias().isEmpty()) {
			
				List<AuditoriaDTO> auditorias =	cambioDTO.getAuditorias().stream()
					.peek(audit->{
						if(audit.getFecBaja() == null) {
							audit.setFecBaja(DateUtils.getSysDateMongoISO());
						}
					}).collect(Collectors.toList());
				
				cambioDTO.setAuditorias(auditorias);
				
			}
			
			for (int i = 0; i < cambioDTO.getAuditorias().size();) {
				AuditoriaDTO auditoriaDTO = cambioDTO.getAuditorias().get(i);
				AuditoriaDTO auditoriaDTOAux = new AuditoriaDTO();
				auditoriaDTOAux.setCamposOriginalesDTO(auditoriaDTO.getCamposOriginalesDTO());
				if (cambioDTO.getCveOrigenArchivo() != null
						&& cambioDTO.getCveOrigenArchivo().equals(IdentificadorArchivoEnum.MANUAL.getIdentificador())) {
					if (input.getCveSituacionRegistro() == SituacionRegistroEnum.RECHAZADO.getClave()) {
						auditoriaDTOAux.setCveIdAccionRegistro(AccionRegistroEnum.ALTA_RECHAZADO.getClave());
						auditoriaDTOAux.setDesAccionRegistro(AccionRegistroEnum.ALTA_RECHAZADO.getDescripcion());
						if(accionRegistro.getCveIdAccionRegistro().intValue() == AccionRegistroEnum.BAJA_RECHAZADO.getClave()) {
							auditoriaDTOAux.setCveIdAccionRegistro(AccionRegistroEnum.BAJA_RECHAZADO.getClave());
							auditoriaDTOAux.setDesAccionRegistro(AccionRegistroEnum.BAJA_RECHAZADO.getDescripcion());
						}		
						auditoriaDTOAux.setCveSituacionRegistro(SituacionRegistroEnum.RECHAZADO.getClave());
						auditoriaDTOAux.setDesSituacionRegistro(SituacionRegistroEnum.RECHAZADO.getDescripcion());
					} else {
						
						auditoriaDTOAux.setCveIdAccionRegistro(AccionRegistroEnum.ALTA.getClave());
						auditoriaDTOAux.setDesAccionRegistro(AccionRegistroEnum.ALTA.getDescripcion());
						if(accionRegistro.getCveIdAccionRegistro().intValue() == AccionRegistroEnum.ELIMINACION.getClave()) {
							auditoriaDTOAux.setCveIdAccionRegistro(AccionRegistroEnum.ELIMINACION.getClave());
							auditoriaDTOAux.setDesAccionRegistro(AccionRegistroEnum.ELIMINACION.getDescripcion());
						}						
						auditoriaDTOAux.setCveSituacionRegistro(SituacionRegistroEnum.APROBADO.getClave());
						auditoriaDTOAux.setDesSituacionRegistro(SituacionRegistroEnum.APROBADO.getDescripcion());
					}
				} else {
					auditoriaDTOAux.setCveIdAccionRegistro(accionRegistro.getCveIdAccionRegistro());
					auditoriaDTOAux.setDesAccionRegistro(accionRegistro.getDesAccionRegistro());
					auditoriaDTOAux.setCveSituacionRegistro(Integer.parseInt(situacionRegistro.getCveIdSituacionRegistro()));
					auditoriaDTOAux.setDesSituacionRegistro(situacionRegistro.getDesSituacionRegistro());
					
					if (accionRegistro.getCveIdAccionRegistro().intValue() == AccionRegistroEnum.CONFIRMAR_PENDIENTE.getClave()) {
						if (input.getCveSituacionRegistro() == SituacionRegistroEnum.APROBADO.getClave()) {
							auditoriaDTOAux.setCveIdAccionRegistro(AccionRegistroEnum.CONFIRMAR.getClave());
							auditoriaDTOAux.setDesAccionRegistro(AccionRegistroEnum.CONFIRMAR.getDescripcion());
						}
						if (input.getCveSituacionRegistro() == SituacionRegistroEnum.RECHAZADO.getClave()) {
							auditoriaDTOAux.setCveIdAccionRegistro(AccionRegistroEnum.CONFIRMAR_RECHAZADO.getClave());
							auditoriaDTOAux.setDesAccionRegistro(AccionRegistroEnum.CONFIRMAR_RECHAZADO.getDescripcion());
						}
					}
					
				}
				auditoriaDTOAux.setDesCambio(auditoriaDTO.getDesCambio());
				auditoriaDTOAux.setDesObservacionesAprobador(input.getDesObservaciones());
				auditoriaDTOAux.setDesObservacionesSol(auditoriaDTO.getDesObservacionesSol());
				auditoriaDTOAux.setFecActualizacion(null);
				auditoriaDTOAux.setFecAlta(DateUtils.getSysDateMongoISO());
				auditoriaDTOAux.setFecBaja(null);
				auditoriaDTOAux.setNomUsuario(input.getCveCurp());
				cambioDTO.getAuditorias().add(auditoriaDTOAux);
				break;

			}
			if (cambioDTO.getOrigenAlta() != null && cambioDTO.getOrigenAlta().equals("ST")) {				
				cambioDTO.setCveSituacionRegistro(SituacionRegistroEnum.APROBADO.getClave());
				cambioDTO.setDesSituacionRegistro(SituacionRegistroEnum.APROBADO.getDescripcion());
			}
			if (cambioDTO.getCveEstadoRegistro().equals(EstadoRegistroEnum.ERRONEO.getCveEstadoRegistro())) {
				cambioDTO.setCveEstadoRegistro(EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro());
				cambioDTO.setDesEstadoRegistro(EstadoRegistroEnum.CORRECTO.getDesDescripcion());
			} else if (cambioDTO.getCveEstadoRegistro()
					.equals(EstadoRegistroEnum.ERRONEO_OTRAS.getCveEstadoRegistro())) {
				cambioDTO.setCveEstadoRegistro(EstadoRegistroEnum.CORRECTO_OTRAS.getCveEstadoRegistro());
				cambioDTO.setDesEstadoRegistro(EstadoRegistroEnum.CORRECTO_OTRAS.getDesDescripcion());
			}
			if(cambioDTO.getCveDelegacionNss() != cambioDTO.getCveDelRegPatronal()) {
				cambioDTO.setCveEstadoRegistro(EstadoRegistroEnum.CORRECTO_OTRAS.getCveEstadoRegistro());
				cambioDTO.setDesEstadoRegistro(EstadoRegistroEnum.CORRECTO_OTRAS.getDesDescripcion());				
			} 
			
			cambioDTO.setNumCicloAnual(cambioDTO.getFecFin() != null ? df.format(cambioDTO.getFecFin()) : "");
			if(cambioDTO.getNumCicloAnual().equals("")) {
				cambioDTO.setNumCicloAnual(cambioDTO.getFecIniPension() != null ? df.format(cambioDTO.getFecIniPension()) : "");
			}			
			
			if (accionRegistro.getCveIdAccionRegistro().intValue() != AccionRegistroEnum.BAJA_PENDIENTE.getClave()
					&& accionRegistro.getCveIdAccionRegistro().intValue() != AccionRegistroEnum.ELIMINACION.getClave()) {
				
				CasoRegistroDTO caso = DateUtils.obtenerCasoRegistro(cambioDTO.getFecFin());
				if(caso.getIdCaso() > 0) {
					cambioDTO.setCveCasoRegistro(caso.getIdCaso());
					cambioDTO.setDesCasoRegistro(caso.getDescripcion());	
				}
				
			}
			
			mongoOperations.save(cambioDTO);
			return cambioDTO;
		} else {
			return null;
		}
	}

	@Override
	public CambioDTO updateSinCambio(MovimientoRequest input) throws BusinessException {
		CambioDTO cambioDTO = getCambio(input);
		DateFormat df = new SimpleDateFormat("yyyy");
		cambioDTO.setIsPending(Boolean.FALSE);
		// Pendiente de aprobar
		Query querySituacion = new Query();
		querySituacion.addCriteria(Criteria.where("cveIdSituacionRegistro").is(input.getCveSituacionRegistro()));
		MccSituacionRegistro situacionRegistro = mongoOperations.findOne(querySituacion, MccSituacionRegistro.class);
		Query queryAccion = new Query();
		queryAccion.addCriteria(Criteria.where("cveIdAccionRegistro").is(input.getCveIdAccionRegistro()));
		MccAccionRegistro accionRegistro = mongoOperations.findOne(queryAccion, MccAccionRegistro.class);
		if(!cambioDTO.getCveOrigenArchivo().equals("MN")) {
			cambioDTO.setFecBaja(DateUtils.getSysDateMongoISO());
		}
		if (cambioDTO != null) {
			cambioDTO.setCveSituacionRegistro(Integer.valueOf(situacionRegistro.getCveIdSituacionRegistro()));
			cambioDTO.setDesSituacionRegistro(situacionRegistro.getDesSituacionRegistro());

			if (accionRegistro.getCveIdAccionRegistro().intValue() == AccionRegistroEnum.BAJA_PENDIENTE.getClave()
					|| accionRegistro.getCveIdAccionRegistro().intValue() == AccionRegistroEnum.ELIMINACION
							.getClave()) {
				calculaRn68(cambioDTO);
			}

		}

		if (cambioDTO.getAuditorias() != null) {
			for (AuditoriaDTO auditoriaDTO : cambioDTO.getAuditorias()) {
				auditoriaDTO.setFecActualizacion(DateUtils.obtenerFechaActualDate());
				auditoriaDTO.setFecBaja(DateUtils.obtenerFechaActualDate());
			}
			AuditoriaDTO auditoriaDTO = new AuditoriaDTO();
			auditoriaDTO.setCveSituacionRegistro(SituacionRegistroEnum.APROBADO.getClave());
			auditoriaDTO.setDesSituacionRegistro(SituacionRegistroEnum.APROBADO.getDescripcion());
			auditoriaDTO.setFecAlta(new Date());
			auditoriaDTO.setCveIdAccionRegistro(AccionRegistroEnum.CONFIRMAR.getClave());
			auditoriaDTO.setDesAccionRegistro(AccionRegistroEnum.CONFIRMAR.getDescripcion());
			auditoriaDTO.setNomUsuario(input.getCveCurp());
			cambioDTO.getAuditorias().add(auditoriaDTO);
		}

		cambioDTO.setCveSituacionRegistro(SituacionRegistroEnum.APROBADO.getClave());
		cambioDTO.setDesSituacionRegistro(SituacionRegistroEnum.APROBADO.getDescripcion());

		cambioDTO.setCveEstadoRegistro(EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro());
		cambioDTO.setDesEstadoRegistro(EstadoRegistroEnum.CORRECTO.getDesDescripcion());

		cambioDTO.setNumCicloAnual(cambioDTO.getFecFin() != null ? df.format(cambioDTO.getFecFin()) : "");
		CasoRegistroDTO caso = DateUtils.obtenerCasoRegistro(cambioDTO.getFecFin());
		cambioDTO.setCveCasoRegistro(caso.getIdCaso() > 0 ? caso.getIdCaso() : null);
		cambioDTO.setDesCasoRegistro(caso.getDescripcion());
		mongoOperations.save(cambioDTO);
				
		return cambioDTO;

	}
	
	public void updateCambio(CambioDTO cambioDTO) {
		mongoOperations.save(cambioDTO);
	}

	private void calculaRn68(CambioDTO cambioDTO) {
		if (cambioDTO.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro()
				|| cambioDTO.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.DUPLICADO.getCveEstadoRegistro()
				|| cambioDTO.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.ERRONEO.getCveEstadoRegistro()
				|| cambioDTO.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.SUSCEPTIBLE.getCveEstadoRegistro()
				|| cambioDTO.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.BAJA.getCveEstadoRegistro()) {
			cambioDTO.setCveEstadoRegistro(EstadoRegistroEnum.BAJA.getCveEstadoRegistro());
			cambioDTO.setDesEstadoRegistro(EstadoRegistroEnum.BAJA.getDesDescripcion());
		}
		if (cambioDTO.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.CORRECTO_OTRAS.getCveEstadoRegistro()
				|| cambioDTO.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.DUPLICADO_OTRAS
						.getCveEstadoRegistro()
				|| cambioDTO.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.ERRONEO_OTRAS
						.getCveEstadoRegistro()
				|| cambioDTO.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.SUSCEPTIBLE_OTRAS
						.getCveEstadoRegistro()
				|| cambioDTO.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.BAJA_OTRAS_DELEGACIONES
						.getCveEstadoRegistro()) {
			cambioDTO.setCveEstadoRegistro(EstadoRegistroEnum.BAJA_OTRAS_DELEGACIONES.getCveEstadoRegistro());
			cambioDTO.setDesEstadoRegistro(EstadoRegistroEnum.BAJA_OTRAS_DELEGACIONES.getDesDescripcion());
		}
	}

	public CambioDTO getCambio(MovimientoRequest input) {
		Query query = new Query();
		ObjectId id = new ObjectId(input.getObjectId());
		query.addCriteria(Criteria.where("_id").is(id));
		query.addCriteria(Criteria.where("numNss").is(input.getNumNss()));

		if (StringUtils.isNotBlank(input.getNumFolioMovtoOriginal())
				&& StringUtils.isNotEmpty(input.getNumFolioMovtoOriginal())) {
			query.addCriteria(Criteria.where("refFolioOriginal").is(input.getNumFolioMovtoOriginal()));
		}

		CambioDTO cambioDTO = mongoOperations.findOne(query, CambioDTO.class);
		return cambioDTO;
	}

	@Override
	public void markAsPending(ObjectId objectId, Boolean isPending) {
		Query query = new Query(Criteria.where("_id").is(objectId));
		CambioDTO cambioDTO = mongoOperations.findOne(query, CambioDTO.class);
		if (cambioDTO != null) {
			cambioDTO.setIsPending(isPending);
			mongoOperations.save(cambioDTO);
		}
	}

	@Override
	public void updateAlta(DatosModificadosDTO datosModificadosDTO, MovimientoRequest movimientoRequest) {
		Query query = new Query(Criteria.where("_id").is(new ObjectId(datosModificadosDTO.getObjectIdOrigen())));
		DateFormat df = new SimpleDateFormat("yyyy");
		CambioDTO cambioDTO = mongoOperations.findOne(query, CambioDTO.class);
		// se aniade la fecha de actualizacion al registro asi como el usuario que hizo
		// la actualizacion
		assert cambioDTO != null;
		cambioDTO.setIsPending(Boolean.FALSE);
		if (datosModificadosDTO.getCveConsecuecniaModificado() != null
				&& StringUtils.isNotBlank(datosModificadosDTO.getCveConsecuecniaModificado())
				&& Integer.parseInt(datosModificadosDTO.getCveConsecuecniaModificado()) >= 0) {
			cambioDTO.setCveConsecuencia(Integer.parseInt(datosModificadosDTO.getCveConsecuecniaModificado()));
		}
		if (StringUtils.isNotBlank(datosModificadosDTO.getDesConsecuenciaModificado())
				&& StringUtils.isNotEmpty(datosModificadosDTO.getDesConsecuenciaModificado())) {
			cambioDTO.setDesConsecuencia(datosModificadosDTO.getDesConsecuenciaModificado());
		}
		if (datosModificadosDTO.getCveTipoRiesgoModificado() != null
				&& datosModificadosDTO.getCveTipoRiesgoModificado() > 0) {
			cambioDTO.setCveTipoRiesgo(datosModificadosDTO.getCveTipoRiesgoModificado());
		}
		if (StringUtils.isNotBlank(datosModificadosDTO.getDesTipoRiesgoModificado())
				&& StringUtils.isNotEmpty(datosModificadosDTO.getDesTipoRiesgoModificado())) {
			cambioDTO.setDesTipoRiesgo(datosModificadosDTO.getDesTipoRiesgoModificado());
		}
		if (datosModificadosDTO.getFecFinModificado() != null) {
			cambioDTO.setFecFin(datosModificadosDTO.getFecFinModificado());
		}
		if (datosModificadosDTO.getFecInicioModificado() != null) {
			cambioDTO.setFecInicio(datosModificadosDTO.getFecInicioModificado());
		}

		if (StringUtils.isNotBlank(datosModificadosDTO.getNssModificado())
				&& StringUtils.isNotEmpty(datosModificadosDTO.getNssModificado())) {
			cambioDTO.setNumNss(datosModificadosDTO.getNssModificado());
		}

		if (datosModificadosDTO.getNumDiasSubsidiadosModificado() != null
				&& datosModificadosDTO.getNumDiasSubsidiadosModificado() >= 0) {
			cambioDTO.setNumDiasSubsidiados(datosModificadosDTO.getNumDiasSubsidiadosModificado());
		}

		if (datosModificadosDTO.getPorcentajeIncapacidadModificado() != null) {
			cambioDTO.setPorcentajeIncapacidad(datosModificadosDTO.getPorcentajeIncapacidadModificado());
		}

		if (StringUtils.isNotBlank(datosModificadosDTO.getRpModificado())
				&& StringUtils.isNotEmpty(datosModificadosDTO.getRpModificado())) {
			cambioDTO.setRefRegistroPatronal(datosModificadosDTO.getRpModificado());

		}

		if (datosModificadosDTO.getCveIdAccionRegistro() == AccionRegistroEnum.BAJA_PENDIENTE.getClave()
				|| datosModificadosDTO.getCveIdAccionRegistro() == AccionRegistroEnum.ELIMINACION.getClave()) {
			calculaRN68(datosModificadosDTO);
			cambioDTO.setCveEstadoRegistro(datosModificadosDTO.getCveEstadoRegistro());
			cambioDTO.setDesEstadoRegistro(datosModificadosDTO.getDesEstadoRegistro());
			cambioDTO.setFecBaja(new Date());
		}
		if (datosModificadosDTO.getCveIdAccionRegistro() == AccionRegistroEnum.MODIFICACION_PENDIENTE.getClave()
				|| datosModificadosDTO.getCveIdAccionRegistro() == AccionRegistroEnum.MODIFICACION.getClave()) {
			if (datosModificadosDTO.getCveEstadoRegistro()
					.equals(EstadoRegistroEnum.SUSCEPTIBLE.getCveEstadoRegistro())) {
				cambioDTO.setCveEstadoRegistro(EstadoRegistroEnum.SUSCEPTIBLE.getCveEstadoRegistro());
				cambioDTO.setDesEstadoRegistro(EstadoRegistroEnum.SUSCEPTIBLE.getDesDescripcion());
			} else if (datosModificadosDTO.getCveEstadoRegistro()
					.equals(EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro())) {
				cambioDTO.setCveEstadoRegistro(EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro());
				cambioDTO.setDesEstadoRegistro(EstadoRegistroEnum.CORRECTO.getDesDescripcion());
			}
		}

		cambioDTO.setAuditorias(cambioDTO.getAuditorias().stream()
				.peek(audit -> audit.setFecBaja(DateUtils.getSysDateMongoISO())).collect(Collectors.toList()));
		AuditoriaDTO auditoriaDTO = fillAuditoria(datosModificadosDTO, movimientoRequest);
		auditoriaDTO.setCamposOriginalesDTO(fillCamposOriginales(cambioDTO));
		auditoriaDTO.setCveSituacionRegistro(cambioDTO.getCveSituacionRegistro());
		auditoriaDTO.setDesSituacionRegistro(cambioDTO.getDesSituacionRegistro());
		cambioDTO.getAuditorias().add(auditoriaDTO);
		cambioDTO.setNumCicloAnual(cambioDTO.getFecFin() != null ? df.format(cambioDTO.getFecFin()) : "");
		CasoRegistroDTO caso = DateUtils.obtenerCasoRegistro(cambioDTO.getFecFin());
		cambioDTO.setCveCasoRegistro(caso.getIdCaso() > 0 ? caso.getIdCaso() : null);
		cambioDTO.setDesCasoRegistro(caso.getDescripcion());
		if (movimientoRequest.getCveIdAccionRegistro() == AccionRegistroEnum.CONFIRMAR_PENDIENTE.getClave()
				|| movimientoRequest.getCveIdAccionRegistro() == AccionRegistroEnum.CONFIRMAR.getClave()) {
			cambioDTO.setConfirmarSinCambios(true);
			cambioDTO.setCveEstadoRegistro(datosModificadosDTO.getCveEstadoRegistro());
			cambioDTO.setDesEstadoRegistro(datosModificadosDTO.getDesEstadoRegistro());
		}
		
		if(ObjectUtils.existeValor(datosModificadosDTO.getBitacoraDictamen())) {
			List<BitacoraDictamenDTO> bitacora = datosModificadosDTO.
					getBitacoraDictamen().stream().filter(dic -> dic.isActivo())
				.collect(Collectors.toList());
			cambioDTO.setBitacoraDictamen(bitacora);
		}else {
			if(ObjectUtils.existeValor(cambioDTO.getBitacoraDictamen())) {
				if(!cambioDTO.getBitacoraDictamen().isEmpty()) {
					List<BitacoraDictamenDTO> bitacora = cambioDTO.
							getBitacoraDictamen().stream().filter(dic -> dic.isActivo())
						.collect(Collectors.toList());
					
					if(bitacora.isEmpty()) {
						List<BitacoraDictamenDTO> bitacoraFalse = cambioDTO.getBitacoraDictamen();
						bitacora.stream()
							.peek(dic -> dic.setActivo(false))
							.collect(Collectors.toList());
						cambioDTO.setBitacoraDictamen(bitacoraFalse);
					}else {
						cambioDTO.setBitacoraDictamen(bitacora);
					}
				}
			}
		}
		
		
		mongoOperations.save(cambioDTO);		
	}

	private CamposOriginalesDTO fillCamposOriginales(CambioDTO cambioDTO) {
		CamposOriginalesDTO camposOriginalesDTO = new CamposOriginalesDTO();
		camposOriginalesDTO.setCveEstadoRegistro(cambioDTO.getCveEstadoRegistro());
		camposOriginalesDTO.setDesEstadoRegistro(cambioDTO.getDesEstadoRegistro());
		camposOriginalesDTO.setConsecuencia(cambioDTO.getDesConsecuencia());
		camposOriginalesDTO.setDiasSubsidiados(
				cambioDTO.getNumDiasSubsidiados() != null ? cambioDTO.getNumDiasSubsidiados().toString() : null);
		camposOriginalesDTO.setFechaFin(cambioDTO.getFecFin());
		camposOriginalesDTO.setFechaInicio(cambioDTO.getFecInicio());
		camposOriginalesDTO.setNss(cambioDTO.getNumNss());
		camposOriginalesDTO.setProcentaje(
				cambioDTO.getPorcentajeIncapacidad() != null ? cambioDTO.getPorcentajeIncapacidad().toString() : null);
		camposOriginalesDTO.setRp(cambioDTO.getRefRegistroPatronal());
		camposOriginalesDTO.setTipoRiesgo(cambioDTO.getDesTipoRiesgo());
		return camposOriginalesDTO;
	}

	private AuditoriaDTO fillAuditoria(DatosModificadosDTO datosModificadosDTO, MovimientoRequest movimientoRequest) {
		AuditoriaDTO auditoriaDTO = new AuditoriaDTO();
		Optional<AccionRegistroEnum> accionRegistroEnum = AccionRegistroEnum
				.findByCve(datosModificadosDTO.getCveIdAccionRegistro());
		auditoriaDTO.setDesAccionRegistro(accionRegistroEnum.map(AccionRegistroEnum::getDescripcion).orElse(null));
		auditoriaDTO.setCveIdAccionRegistro(accionRegistroEnum.map(AccionRegistroEnum::getClave).orElse(null));

		auditoriaDTO.setFecAlta(DateUtils.getSysDateMongoISO());
		auditoriaDTO.setDesCambio(movimientoRequest.getDesObservaciones());
		auditoriaDTO.setAccion(accionRegistroEnum.map(AccionRegistroEnum::getDescripcion).orElse(null));
		auditoriaDTO.setNomUsuario(movimientoRequest.getCveCurp());
		auditoriaDTO.setNumFolioMovOriginal(movimientoRequest.getNumFolioMovtoOriginal());
		return auditoriaDTO;
	}

	private void calculaRN68(DatosModificadosDTO input) {
		if (input.getCveEstadoRegistro() == EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro() == EstadoRegistroEnum.DUPLICADO.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro() == EstadoRegistroEnum.ERRONEO.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro() == EstadoRegistroEnum.SUSCEPTIBLE.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro() == EstadoRegistroEnum.BAJA.getCveEstadoRegistro()) {
			input.setCveEstadoRegistro(EstadoRegistroEnum.BAJA.getCveEstadoRegistro());
			input.setDesEstadoRegistro(EstadoRegistroEnum.BAJA.getDesDescripcion());
		} else if (input.getCveEstadoRegistro() == EstadoRegistroEnum.CORRECTO_OTRAS.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro() == EstadoRegistroEnum.DUPLICADO_OTRAS
						.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro() == EstadoRegistroEnum.ERRONEO_OTRAS.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro() == EstadoRegistroEnum.SUSCEPTIBLE_OTRAS.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro() == EstadoRegistroEnum.BAJA_OTRAS_DELEGACIONES.getCveEstadoRegistro()) {
			input.setCveEstadoRegistro(EstadoRegistroEnum.BAJA_OTRAS_DELEGACIONES.getCveEstadoRegistro());
			input.setDesEstadoRegistro(EstadoRegistroEnum.BAJA_OTRAS_DELEGACIONES.getDesDescripcion());

		}
	}
	
	public boolean isPendienteAprobar(ObjectId idObject) {
		Query query = new Query();
		query.addCriteria(Criteria.where("objectIdOrigen").is(idObject));
		query.addCriteria(Criteria.where("cveSituacionRegistro").is(2));
				
		List<CambioDTO> cambioDTO = mongoOperations.find(query, CambioDTO.class);
		
		if(!cambioDTO.isEmpty()) {
			return true;
		}
		
		return false;
	}

	public boolean isPendienteAprobarMN(ObjectId idObject) {
		Query query = new Query();
		query.addCriteria(Criteria.where("idOrigenAlta").is(idObject));
		query.addCriteria(Criteria.where("cveSituacionRegistro").is(2));
				
		List<CambioDTO> cambioDTO = mongoOperations.find(query, CambioDTO.class);
		
		if(!cambioDTO.isEmpty()) {
			return true;
		}
		
		return false;
	}
}
