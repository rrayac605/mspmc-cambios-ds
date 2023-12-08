package mx.gob.imss.cit.pmc.cambios.service.impl;

import lombok.Getter;
import lombok.Setter;
import mx.gob.imss.cit.mspmccommons.exception.BusinessException;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.mvel.MVELRuleFactory;
import org.jeasy.rules.support.YamlRuleDefinitionReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStreamReader;

@Component
public class ValidacionDupSusFactory {

	@Setter
	@Getter
	private Rules rules;

	@Autowired
	private ResourceLoader resourceLoader;

	@PostConstruct
	public void creaReglas() throws BusinessException {
		MVELRuleFactory ruleFactory = new MVELRuleFactory(new YamlRuleDefinitionReader());
		try {
			Resource resource = resourceLoader.getResource("classpath:rules_susceptibles.yml");
			rules = ruleFactory.createRules(new InputStreamReader(resource.getInputStream()));
		} catch (Exception e) {
			throw new BusinessException(e);
		}

	}

}
