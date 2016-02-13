package net.arunoday.nagios.plugin;

import static net.arunoday.nagios.plugin.NagiosStatus.CRITICAL;
import static net.arunoday.nagios.plugin.NagiosStatus.OK;
import static net.arunoday.nagios.plugin.NagiosStatus.WARNING;

import java.util.List;

/**
 * @author Aparna Chaudhary
 */
public abstract class AbstractCheck {

	/**
	 * Checks if the input is exceeding the warning threshold or critical threshold.
	 * 
	 * @param percent_used data to verify
	 * @param warning warning threshold
	 * @param crtical critical threshold
	 * @param message nagios message
	 */
	protected void checkLevel(float percent_used, int warning, int crtical, String message) {
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

	protected void checkLevel(List<Float> levels, int warning, int critical, String output, String perfdata) {

		StringBuilder violations = new StringBuilder();
		NagiosStatus disposition = OK;

		for (Float percent_used : levels) {
			if (percent_used >= warning && percent_used < critical) {
				violations.append(String.format("(%3.2f > %d) ", percent_used, warning));
				disposition = WARNING.getCode() > disposition.getCode() ? WARNING : disposition;
			}
			if (percent_used >= critical) {
				violations.append(String.format("(%3.2f > %d) ", percent_used, critical));
				disposition = CRITICAL.getCode() > disposition.getCode() ? CRITICAL : disposition;
			}
		}

		System.out.println(String.format("%s - %s %s | %s", disposition, output, violations.toString(), perfdata));
		System.exit(disposition.getCode());

	}
}
