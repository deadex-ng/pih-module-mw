package org.openmrs.module.pihmalawi.reporting.definition;

import java.util.HashMap;
import java.util.Map;

import org.openmrs.api.context.Context;
import org.openmrs.module.pihmalawi.reporting.Helper;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.evaluation.Definition;
import org.openmrs.module.reporting.report.definition.PeriodIndicatorReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;

public class TransientHelper extends Helper {
	
	Map myDefinitions = new HashMap();
	
	public void purgeDefinition(Class clazz, String name) {
		myDefinitions.remove(clazz.getName() + name);
	}
	
	public Definition findDefinition(Class clazz, String name) {
		return (Definition) myDefinitions.get(clazz.getName() + name);
	}
	
	public CohortDefinition cohortDefinition(String name) {
		return (CohortDefinition) myDefinitions.get(CohortDefinition.class.getName() + name);
	}
	
	public void replaceCohortDefinition(CohortDefinition def) {
		myDefinitions.put(CohortDefinition.class.getName() + def.getName(), def);
	}
	
	public void replaceDefinition(Definition def) {
		myDefinitions.put(def.getClass().getName() + def.getName(), def);
	}
	
	public void replaceReportDefinition(PeriodIndicatorReportDefinition rd) {
		myDefinitions.put(rd.getClass().getName() + rd.getName(), rd);
    }

}