package mx.gob.imss.cit.pmc.cambios.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import mx.gob.imss.cit.mspmccommons.dto.DetalleRegistroDTO;
import mx.gob.imss.cit.mspmccommons.exception.BusinessException;

@Component
public interface PmcValidaSusceptibles {

	Map<Integer, List<String>> validarSusceptibles(DetalleRegistroDTO detalleRegistroDTO) throws BusinessException;
	
	Map<Integer, List<String>> validarSusceptiblesSinCambios(DetalleRegistroDTO registroDTO) throws BusinessException;
	
	List<DetalleRegistroDTO> obtenerSusceptiblesSinCambios(DetalleRegistroDTO registroDTO) throws BusinessException;

	
}
