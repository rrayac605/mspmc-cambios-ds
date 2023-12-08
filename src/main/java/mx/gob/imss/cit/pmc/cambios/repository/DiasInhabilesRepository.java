package mx.gob.imss.cit.pmc.cambios.repository;

import java.util.List;

import mx.gob.imss.cit.mspmccommons.dto.FechaInhabilDTO;

public interface DiasInhabilesRepository {

	List<FechaInhabilDTO> findAll();

}
