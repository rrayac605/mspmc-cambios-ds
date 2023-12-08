package mx.gob.imss.cit.pmc.cambios.model;

import lombok.Getter;
import lombok.Setter;

public class CountMovementsChanges {

	@Getter
	@Setter
	private Long movementsOffSet;

	@Getter
	@Setter
	private Long changesOffSet;

	@Getter
	@Setter
	private String numNssChanges;
	
	@Getter
	@Setter
	private Long posicionChange;
	
	@Getter
	@Setter
	private Long posicionMov;
}
