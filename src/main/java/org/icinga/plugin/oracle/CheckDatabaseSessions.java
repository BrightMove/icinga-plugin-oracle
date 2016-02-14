package org.icinga.plugin.oracle;

import static org.icinga.plugin.oracle.NagiosStatus.UNKNOWN;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check to provide count of active sessions for the entire database
 * 
 * @author David Webb
 */
public class CheckDatabaseSessions extends CheckAdapter {

	private static final Logger logger = LoggerFactory.getLogger(CheckTablespace.class);

	/**
	 * Checks active session count for the given username.
	 * 
	 * @param connection SQL connection
	 * @param warningThreshold warning threshold
	 * @param crticalThreshold critical threshold
	 * @param debug
	 */
	public static void performCheck(Connection connection, String warningThreshold, String crticalThreshold,
			boolean debug) {

		try {

			String query = "SELECT COUNT(1) FROM v$session";

			if (debug) {
				logger.debug("Executing query " + query);
			}

			// execute query
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(query);

			int activeSessions = 0;
			int warning = Integer.valueOf(warningThreshold);
			int crtical = Integer.valueOf(crticalThreshold);
			if (rs != null && rs.next()) {
				activeSessions = rs.getInt(1);
				if (debug) {
					logger.debug(String.format("Active Sessions: ", activeSessions));
				}
			}

			rs.close();
			statement.close();

			String perfdata = "sessions=" + activeSessions + ";" + warning + ";" + crtical;
			String output = "Active sessions " + activeSessions + "|" + perfdata;

			checkLevel(activeSessions, warning, crtical, output);

		} catch (SQLException e) {
			System.err.println(e);
			System.exit(UNKNOWN.getCode());
		}

	}
}
