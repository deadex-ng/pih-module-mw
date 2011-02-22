package org.openmrs.module.pihmalawi.reporting.mohquarterlyart;

import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.PatientSetService.TimeModifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.pihmalawi.reporting.BeforeAfter;
import org.openmrs.module.pihmalawi.reporting.Helper;
import org.openmrs.module.pihmalawi.reporting.Event;
import org.openmrs.module.pihmalawi.reporting.extension.HibernatePihMalawiQueryDao;
import org.openmrs.module.pihmalawi.reporting.extension.InStateAtLocationCohortDefinition;
import org.openmrs.module.pihmalawi.reporting.extension.PatientStateAtLocationCohortDefinition;
import org.openmrs.module.pihmalawi.reporting.extension.StateRelativeToStateCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.DateObsCohortDefinition;
import org.openmrs.module.reporting.common.DurationUnit;
import org.openmrs.module.reporting.common.RangeComparator;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.indicator.CohortIndicator;
import org.openmrs.module.reporting.report.PeriodIndicatorReportUtil;
import org.openmrs.module.reporting.report.definition.PeriodIndicatorReportDefinition;
import org.openmrs.module.reporting.report.definition.ReportDefinition;

public class SetupArvQuarterly {
	
	protected static final Log log = LogFactory
	.getLog(HibernatePihMalawiQueryDao.class);

	private final Concept CONCEPT_APPOINTMENT_DATE;

	private final Program PROGRAM;

	Helper h = new Helper();

	private final ProgramWorkflowState STATE_DIED;

	private final ProgramWorkflowState STATE_ON_ART;

	private final ProgramWorkflowState STATE_STOPPED;

	private final ProgramWorkflowState STATE_TRANSFERRED_OUT;
	
	private final ProgramWorkflowState FOLLOWING;

	/** little hack to have a start date. maybe we could live without it */
	private static final String MIN_DATE_PARAMETER = "${startDate-5000m}";

	public SetupArvQuarterly(Helper helper) {
		h = helper;
		PROGRAM = Context.getProgramWorkflowService().getProgramByName(
				"HIV PROGRAM");
		STATE_DIED = PROGRAM.getWorkflowByName("TREATMENT STATUS")
				.getStateByName("PATIENT DIED");
		STATE_ON_ART = PROGRAM.getWorkflowByName("TREATMENT STATUS")
				.getStateByName("ON ANTIRETROVIRALS");
		STATE_STOPPED = PROGRAM.getWorkflowByName("TREATMENT STATUS")
				.getStateByName("TREATMENT STOPPED");
		STATE_TRANSFERRED_OUT = PROGRAM.getWorkflowByName("TREATMENT STATUS")
				.getStateByName("PATIENT TRANSFERRED OUT");
		FOLLOWING = PROGRAM.getWorkflowByName("TREATMENT STATUS")
		.getStateByName("FOLLOWING");
		CONCEPT_APPOINTMENT_DATE = Context.getConceptService()
				.getConceptByName("APPOINTMENT DATE");
	}

	public void setup() throws Exception {
		delete();

		PeriodIndicatorReportDefinition rd = createReportDefinition();
		createCohortDefinitions(rd);
		h.replaceReportDefinition(rd);
	}

	public void delete() {
		h.purgeDefinition(DataSetDefinition.class, "ARV Quarterly_ Data Set");
		h.purgeDefinition(ReportDefinition.class, "ARV Quarterly_");
		h.purgeAll("arvquarterly: ");
	}

	private PeriodIndicatorReportDefinition createReportDefinition() {
		// Ever On ART at location
		PatientStateAtLocationCohortDefinition pscd = new PatientStateAtLocationCohortDefinition();
		pscd.setName("arvquarterly: Ever On ART at location_");
		pscd.setState(STATE_ON_ART);
		pscd.addParameter(new Parameter("startedOnOrAfter", "startedOnOrAfter",
				Date.class));
		pscd.addParameter(new Parameter("startedOnOrBefore",
				"startedOnOrBefore", Date.class));
		pscd.addParameter(new Parameter("location", "location", Location.class));
		h.replaceCohortDefinition(pscd);

		PeriodIndicatorReportDefinition rd = new PeriodIndicatorReportDefinition();
		rd.setBaseCohortDefinition(pscd, h.parameterMap("startedOnOrAfter",
				MIN_DATE_PARAMETER, "startedOnOrBefore", "${endDate}",
				"location", "${location}"));
		rd.setName("ARV Quarterly_");
		rd.setupDataSetDefinition();
		return rd;
	}

	private void createCohortDefinitions(PeriodIndicatorReportDefinition rd) {
		i6_registered(rd);

		// In state at location
		InStateAtLocationCohortDefinition islcd = new InStateAtLocationCohortDefinition();
		islcd.setName("arvquarterly: In state at location_");
		islcd.addParameter(new Parameter("state", "State",
				ProgramWorkflowState.class));
		islcd.addParameter(new Parameter("onDate", "onDate", Date.class));
		islcd.addParameter(new Parameter("location", "location", Location.class));
		h.replaceCohortDefinition(islcd);
		
		// State relative to state
		StateRelativeToStateCohortDefinition srtscd = new StateRelativeToStateCohortDefinition();
		srtscd.setName("arvquarterly: State any location relative to state at enrollment location within interval_");
		srtscd.addParameter(new Parameter("primaryStateEvent", "Primary State Event", Date.class));
		srtscd.addParameter(new Parameter("primaryState", "Primary State",
				ProgramWorkflowState.class));
		srtscd.addParameter(new Parameter("onOrAfter", "On Or After", Date.class));
		srtscd.addParameter(new Parameter("onOrBefore", "On Or Before", Date.class));
		srtscd.addParameter(new Parameter("primaryStateLocation", "Primary State Location", 
				Location.class));
		srtscd.addParameter(new Parameter("offsetAmount", "Offset Amount", Date.class));
		srtscd.addParameter(new Parameter("offsetUnit", "Offset Unit", Date.class));
		srtscd.addParameter(new Parameter("beforeAfter", "Before After", Date.class));
		srtscd.addParameter(new Parameter("relativeStateEvent", "Relative State Event", Date.class));
		srtscd.addParameter(new Parameter("relativeState", "Relative State",
				ProgramWorkflowState.class));
		
		h.replaceCohortDefinition(srtscd);

		i27_alive(rd);
		
		i28_died_1month_after_ART(rd);
		
		//i29_died_2month_after_ART(rd);
		
		//i30_died_3month_after_ART(rd);
		
		// TESTS
		//i100_stopped_ART_2month_after_started_following(rd);
		//i101_started_ART_2month_before_started_died(rd);
		//i102_stopped_ART_1month_before_started_died(rd);

		i32_died(rd);

		i33_defaulted(rd);

		i34_stopped(rd);

		i35_transferred(rd);
	}

	private void i6_registered(PeriodIndicatorReportDefinition rd) {
		CohortIndicator i = h.newCountIndicator(
				"arvquarterly: Total registered in quarter_",
				"arvquarterly: Ever On ART at location_", h.parameterMap(
						"startedOnOrAfter", "${startDate}",
						"startedOnOrBefore", "${endDate}", "location",
						"${location}"));
		PeriodIndicatorReportUtil.addColumn(rd, "6_quarter",
				"Total registered in quarter", i, null);

		i = h.newCountIndicator("arvquarterly: Total registered ever_",
				"arvquarterly: Ever On ART at location_", h.parameterMap(
						"startedOnOrAfter", MIN_DATE_PARAMETER,
						"startedOnOrBefore", "${endDate}", "location",
						"${location}"));
		PeriodIndicatorReportUtil.addColumn(rd, "6_ever",
				"Total registered ever", i, null);
	}
	
private void i28_died_1month_after_ART(PeriodIndicatorReportDefinition rd) {
		
		CohortIndicator i = h.newCountIndicator(
				"arvquarterly: In ART state any location within one month died state at enrollment location within interval_",
				"arvquarterly: State any location relative to state at enrollment location within interval_", h.parameterMap(
						"primaryStateEvent", Event.STARTED,
						"primaryState", STATE_DIED,
						"onOrAfter", "${startDate}", 
						"onOrBefore", "${endDate}", 
						"primaryStateLocation", "${location}",
						"offsetAmount", 1,
						"offsetUnit", DurationUnit.MONTHS,
						"beforeAfter", BeforeAfter.AFTER,
						"relativeStateEvent", Event.STARTED,
						"relativeState", STATE_ON_ART));
		PeriodIndicatorReportUtil.addColumn(rd, "28", "Died within the 1st month after ART initiation", i,
				null);
	}
/*
private void i29_died_2month_after_ART(PeriodIndicatorReportDefinition rd) {
	
	CohortIndicator i = h.newCountIndicator(
			"arvquarterly: In ART state any location within one month died state at enrollment location within interval_",
			"arvquarterly: State any location relative to state at enrollment location within interval_", h.parameterMap(
					"primaryStateEvent", Event.STARTED,
					"primaryState", STATE_DIED,
					"onOrAfter", "${startDate}", 
					"onOrBefore", "${endDate}", 
					"primaryStateLocation", "${location}",
					"offsetAmount", 2,
					"offsetUnit", DurationUnit.MONTHS,
					"beforeAfter", BeforeAfter.AFTER,
					"relativeStateEvent", Event.STARTED,
					"relativeState", STATE_ON_ART));
	PeriodIndicatorReportUtil.addColumn(rd, "29", "Died within the 2nd month after ART initiation", i,
			null);
}

private void i30_died_3month_after_ART(PeriodIndicatorReportDefinition rd) {
	
	CohortIndicator i = h.newCountIndicator(
			"arvquarterly: In ART state any location within one month died state at enrollment location within interval_",
			"arvquarterly: State any location relative to state at enrollment location within interval_", h.parameterMap(
					"primaryStateEvent", Event.STARTED,
					"primaryState", STATE_DIED,
					"onOrAfter", "${startDate}", 
					"onOrBefore", "${endDate}", 
					"primaryStateLocation", "${location}",
					"offsetAmount", 3,
					"offsetUnit", DurationUnit.MONTHS,
					"beforeAfter", BeforeAfter.AFTER,
					"relativeStateEvent", Event.STARTED,
					"relativeState", STATE_ON_ART));
	PeriodIndicatorReportUtil.addColumn(rd, "30", "Died within the 3rd month after ART initiation", i,
			null);
}
*/
/*  TESTS
private void i100_stopped_ART_2month_after_started_following(PeriodIndicatorReportDefinition rd) {
	
	CohortIndicator i = h.newCountIndicator(
			"arvquarterly: Stopped ART within the 2nd month after started FOLLOWING_",
			"arvquarterly: State any location relative to state at enrollment location within interval_", h.parameterMap(
					"primaryStateEvent", Event.STOPPED,
					"primaryState", STATE_ON_ART,
					"onOrAfter", "${startDate}", 
					"onOrBefore", "${endDate}",
					"primaryStateLocation", "${location}",
					"offsetAmount", 2,
					"offsetUnit", DurationUnit.MONTHS,
					"beforeAfter", BeforeAfter.AFTER,
					"relativeStateEvent", Event.STARTED,
					"relativeState", FOLLOWING));
	PeriodIndicatorReportUtil.addColumn(rd, "100", "Stopped ART within the 2nd month after started FOLLOWING", i,
			null);
}

private void i101_started_ART_2month_before_started_died(PeriodIndicatorReportDefinition rd) {
	
	CohortIndicator i = h.newCountIndicator(
			"arvquarterly: Started ART within the 2nd month before started DIED_",
			"arvquarterly: State any location relative to state at enrollment location within interval_", h.parameterMap(
					"primaryStateEvent", Event.STARTED,
					"primaryState", STATE_ON_ART,
					"onOrAfter", "${startDate}", 
					"onOrBefore", "${endDate}",
					"primaryStateLocation", "${location}",
					"offsetAmount", 2,
					"offsetUnit", DurationUnit.MONTHS,
					"beforeAfter", BeforeAfter.BEFORE,
					"relativeStateEvent", Event.STARTED,
					"relativeState", STATE_DIED));
	PeriodIndicatorReportUtil.addColumn(rd, "101", "Started ART within the 2nd month before started DIED", i,
			null);
}

private void i102_stopped_ART_1month_before_started_died(PeriodIndicatorReportDefinition rd) {
	
	CohortIndicator i = h.newCountIndicator(
			"arvquarterly: Stopped ART within 1 month before started DIED_",
			"arvquarterly: State any location relative to state at enrollment location within interval_", h.parameterMap(
					"primaryStateEvent", Event.STOPPED,
					"primaryState", STATE_ON_ART,
					"onOrAfter", "${startDate}", 
					"onOrBefore", "${endDate}",
					"primaryStateLocation", "${location}",
					"offsetAmount", 1,
					"offsetUnit", DurationUnit.MONTHS,
					"beforeAfter", BeforeAfter.BEFORE,
					"relativeStateEvent", Event.STARTED,
					"relativeState", STATE_DIED));
	PeriodIndicatorReportUtil.addColumn(rd, "102", "Stopped ART within 1 month before started DIED", i,
			null);
}
*/


	private void i35_transferred(PeriodIndicatorReportDefinition rd) {
		CohortIndicator i = h.newCountIndicator(
				"arvquarterly: Transferred out_",
				"arvquarterly: In state at location_", h.parameterMap("onDate",
						"${endDate}", "state", STATE_TRANSFERRED_OUT,
						"location", "${location}"));
		PeriodIndicatorReportUtil.addColumn(rd, "35", "Transferred out", i,
				null);
	}

	private void i34_stopped(PeriodIndicatorReportDefinition rd) {
		CohortIndicator i = h.newCountIndicator(
				"arvquarterly: Stopped taking ARVs_",
				"arvquarterly: In state at location_", h.parameterMap("onDate",
						"${endDate}", "state", STATE_STOPPED, "location",
						"${location}"));
		PeriodIndicatorReportUtil.addColumn(rd, "34", "Stopped taking ARVs", i,
				null);
	}

	private void i32_died(PeriodIndicatorReportDefinition rd) {
		CohortIndicator i = h.newCountIndicator("arvquarterly: Died total_",
				"arvquarterly: In state at location_", h.parameterMap("onDate",
						"${endDate}", "state", STATE_DIED, "location",
						"${location}"));
		PeriodIndicatorReportUtil.addColumn(rd, "32", "Died total", i, null);
	}

	private void i27_alive(PeriodIndicatorReportDefinition rd) {
		// todo: excluding defaulters
		CohortIndicator i = h.newCountIndicator(
				"arvquarterly: Total alive and On ART_",
				"arvquarterly: In state at location_", h.parameterMap("onDate",
						"${endDate}", "state", STATE_ON_ART, "location",
						"${location}"));
		PeriodIndicatorReportUtil.addColumn(rd, "27", "Total alive and On ART",
				i, null);
	}

	private void i33_defaulted(PeriodIndicatorReportDefinition rd) {
		// todo: only take appointment dates from art clinic. not sure how to
		// get them though
		// todo: defaulted calculated from next app date, but should be taken
		// from last visit and expected date running out of arvs. for now we
		// assume this is the same
		DateObsCohortDefinition dod = new DateObsCohortDefinition();
		dod.setName("arvquarterly: Missed Appointment_");
		dod.setTimeModifier(TimeModifier.MAX);
		dod.setQuestion(CONCEPT_APPOINTMENT_DATE);
		dod.setOperator1(RangeComparator.LESS_THAN);
		dod.setOperator2(RangeComparator.GREATER_EQUAL);
		dod.addParameter(new Parameter("onOrBefore", "endDate", Date.class));
		dod.addParameter(new Parameter("value1", "to", Date.class));
		dod.addParameter(new Parameter("value2", "from", Date.class));
		h.replaceCohortDefinition(dod);

		CohortIndicator i = h.newCountIndicator("arvquarterly: Defaulted_",
				"arvquarterly: Missed Appointment_", h.parameterMap("value1",
						"${endDate-8w}", "onOrBefore", "${endDate}"));
		PeriodIndicatorReportUtil.addColumn(rd, "33", "Defaulted", i, null);
	}
}
