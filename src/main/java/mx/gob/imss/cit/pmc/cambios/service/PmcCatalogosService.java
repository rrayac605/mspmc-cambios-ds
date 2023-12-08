package mx.gob.imss.cit.pmc.cambios.service;

import mx.gob.imss.cit.mspmccommons.dto.ConsecuenciaDTO;
import mx.gob.imss.cit.mspmccommons.dto.LaudoDTO;
import mx.gob.imss.cit.mspmccommons.dto.ModificacionPatronalDTO;
import mx.gob.imss.cit.mspmccommons.dto.TipoRiesgoDTO;

public interface PmcCatalogosService {

	TipoRiesgoDTO obtenerTipoRiesgo(String cveTipoRiesgo);

	ConsecuenciaDTO obtenerConsecuencia(String cveConsecuencia);

	LaudoDTO obtenerLaudo(String cveLaudo);		
	
	ModificacionPatronalDTO obtenerModificacionPatronal(String cveModificacionPatronal);

}
