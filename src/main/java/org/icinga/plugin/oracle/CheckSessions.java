package org.icinga.plugin.oracle;

import static org.icinga.plugin.oracle.NagiosStatus.UNKNOWN;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check to provide count of active session for the given username.
 * 
 * @author Aparna Chaudhary
 */
public class CheckSessions extends AbstractCheck {

	private static final Logger logger = LoggerFactory.getLogger(CheckTablespace.class);
	private boolean debug = false;

	public CheckSessions(boolean debug) {
		this.debug = debug;
	}

	/**
	 * Checks active session count for the given username.
	 * 
	 * @param connection SQL connection
	 * @param username user for which active session count is checked
	 * @param warningThreshold warning threshold
	 * @param crticalThreshold critical threshold
	 */
	public void performCheck(Connection connection, String username, String warningThreshold, String crticalThreshold) {
		try {
			String query = "SELECT COUNT(1) FROM v$session WHERE username ='" + username + "'";

			if (debug) {
				logger.debug("Executing query " + query);
			}

			// execute query
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(query);

			int usedSessions = 0;
			int warning = Integer.valueOf(warningThreshold);
			int crtical = Integer.valueOf(crticalThreshold);
			while (rs.next()) {
				usedSessions = rs.getInt(1);
				if (debug) {
					logger.debug(String.format("Name: %20s ", username));
					logger.debug(String.format("Session used: ", usedSessions));
				}
			}

			rs.close();
			statement.close();
			connection.close();

			String perfdata = username + "_sessions=" + usedSessions + ";" + warning + ";" + crtical;
			String output = username + ": used sessions " + usedSessions + "|" + perfdata;

			checkLevel(usedSessions, warning, crtical, output);

		} catch (SQLException e) {
			System.err.println(e);
			System.exit(UNKNOWN.getCode());
		}

	}
}
