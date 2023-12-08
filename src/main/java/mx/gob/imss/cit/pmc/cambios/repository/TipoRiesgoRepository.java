package mx.gob.imss.cit.pmc.cambios.repository;

import java.util.List;
import java.util.Optional;

import mx.gob.imss.cit.mspmccommons.dto.TipoRiesgoDTO;

public interface TipoRiesgoRepository {

	Optional<TipoRiesgoDTO> findOneByCve(String cveTipoRiesgo);

	Optional<List<TipoRiesgoDTO>> findAll();

}
