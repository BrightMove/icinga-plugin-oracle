package net.arunoday.nagios.plugin;

import static net.arunoday.nagios.plugin.NagiosStatus.CRITICAL;
import static net.arunoday.nagios.plugin.NagiosStatus.OK;
import static net.arunoday.nagios.plugin.NagiosStatus.WARNING;

/**
 * @author Aparna Chaudhary
 */
public abstract class AbstractCheck {

	/**
	 * Checks if the input is exceeding the warning threshold or critical threshold.
	 * 
	 * @param data data to verify
	 * @param warning warning threshold
	 * @param crtical critical threshold
	 * @param message nagios message
	 */
	protected void checkLevel(int data, int warning, int crtical, String message) {
		if (data < warning) {
			System.out.println("OK - " + message);
			System.exit(OK.getCode());
		}
		if (data >= warning && data < crtical) {
			System.out.println("WARNING - " + message);
			System.exit(WARNING.getCode());
		}
		if (data >= crtical) {
			System.out.println("CRITICAL - " + message);
			System.exit(CRITICAL.getCode());
		}
	}

}
