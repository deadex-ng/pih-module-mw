/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.pihmalawi.reporting.reports;

import org.junit.Ignore;
import org.openmrs.module.pihmalawi.metadata.HivMetadata;
import org.openmrs.module.reporting.common.DateUtil;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.report.manager.ReportManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the methods in the PatientDataFactory
 */
@Ignore
public class HivVisitsReportTest extends ReportManagerTest {

	@Autowired
	HivMetadata hivMetadata;

	@Autowired
	HivVisitsReport hivVisitsReport;

	@Override
	public ReportManager getReportManager() {
		return hivVisitsReport;
	}

	@Override
	public EvaluationContext getEvaluationContext() {
		EvaluationContext context = new EvaluationContext();
		context.addParameterValue("startDate", DateUtil.getDateTime(2014,6,3));
		context.addParameterValue("endDate", DateUtil.getDateTime(2014,6,4));
		return context;
	}

	@Override
	protected boolean isEnabled() {
		return false;
	}

	@Override
	public boolean enableReportOutput() {
		return false;
	}
}
