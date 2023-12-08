package mx.gob.imss.cit.pmc.cambios.repository;

import mx.gob.imss.cit.mspmccommons.dto.CatalogoDTO;

public interface CatalogosLocales {

	CatalogoDTO obtenerConsecuencia(String cveConsencuencia);

	CatalogoDTO obtenerLaudo(String cveLaudo);

	CatalogoDTO obtenerTipoRiesgo(String cveTipoRiesgo);

	CatalogoDTO obtenerModificacionPatronal(String cveModifPatronal);
}