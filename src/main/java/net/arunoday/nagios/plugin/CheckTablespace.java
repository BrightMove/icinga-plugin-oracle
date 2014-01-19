package net.arunoday.nagios.plugin;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Aparna Chaudhary
 */
public class CheckTablespace {

	private static final Logger logger = LoggerFactory.getLogger(CheckTablespace.class);
	private boolean debug = false;

	public CheckTablespace(boolean debug) {
		this.debug = debug;
	}

	public void performCheck(Connection connection, Object tablespace, Object warning, Object crtical) {
	}

}
