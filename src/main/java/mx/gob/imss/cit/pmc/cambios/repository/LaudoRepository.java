package mx.gob.imss.cit.pmc.cambios.repository;

import java.util.List;
import java.util.Optional;

import mx.gob.imss.cit.mspmccommons.dto.LaudoDTO;

public interface LaudoRepository {

	Optional<LaudoDTO> findOneByCve(String cveLaudo);

	Optional<List<LaudoDTO>> findAll();

}
