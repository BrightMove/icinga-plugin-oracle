package org.icinga.plugin.oracle;

import static org.icinga.plugin.oracle.NagiosStatus.CRITICAL;
import static org.icinga.plugin.oracle.NagiosStatus.OK;
import static org.icinga.plugin.oracle.NagiosStatus.WARNING;

import java.util.List;

import org.icinga.plugin.oracle.bean.TablespaceMetric;

/**
 * Helpers to finalize a check by evaluating the readings against the thresholds, then generating output, perfdata and
 * exit codes.
 * 
 * @author Aparna Chaudhary
 * @author David Webb
 */
public class CheckAdapter {

	/**
	 * Checks if the input is exceeding the warning threshold or critical threshold.
	 * 
	 * @param percent_used data to verify
	 * @param warning warning threshold
	 * @param crtical critical threshold
	 * @param message nagios message
	 */
	protected static void checkLevel(float percent_used, int warning, int crtical, String message) {
		if (percent_used < warning) {
			System.out.println("OK - " + message);
			System.exit(OK.getCode());
		}
		if (percent_used >= warning && percent_used < crtical) {
			System.out.println("WARNING - " + message);
			System.exit(WARNING.getCode());
		}
		if (percent_used >= crtical) {
			System.out.println("CRITICAL - " + message);
			System.exit(CRITICAL.getCode());
		}
	}

	protected static void checkLevel(List<TablespaceMetric> readings, int warning, int critical) {

		StringBuilder violations = new StringBuilder();
		NagiosStatus disposition = OK;

		for (TablespaceMetric metric : readings) {
			if (metric.getUsedCapacityPct() >= warning && metric.getUsedCapacityPct() < critical) {
				violations
						.append(String.format("%s (%3.2f>%d) ", metric.getTablespaceName(), metric.getUsedCapacityPct(), warning));
				disposition = WARNING.getCode() > disposition.getCode() ? WARNING : disposition;
			}
			if (metric.getUsedCapacityPct() >= critical) {
				violations
						.append(String.format("%s (%3.2f>%d) ", metric.getTablespaceName(), metric.getUsedCapacityPct(), warning));
				disposition = CRITICAL.getCode() > disposition.getCode() ? CRITICAL : disposition;
			}
		}

		System.out.println(String.format("%s - %s| %s", disposition,
				violations.toString().length() == 0 ? "All Tablespaces are Healthy" : violations.toString(),
				generatePerfData(readings, warning, critical)));
		System.exit(disposition.getCode());

	}

	private static Object generatePerfData(List<TablespaceMetric> readings, int warning, int critical) {

		StringBuilder perfdata = new StringBuilder();

		for (TablespaceMetric metric : readings) {
			perfdata.append(String.format("%s=%3.2fMB;%.2f;%.2f;0;%.2f ", metric.getTablespaceName(),
					metric.getUsedCapacityMb(), metric.getTotalCapacityMb() * ((double) warning / 100),
					metric.getTotalCapacityMb() * ((double) critical / 100), metric.getTotalCapacityMb()));
		}

		return perfdata.toString();
	}
}
