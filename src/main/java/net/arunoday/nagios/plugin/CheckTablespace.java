package net.arunoday.nagios.plugin;

import static net.arunoday.nagios.plugin.NagiosStatus.UNKNOWN;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check to provide percent tablespace size usage.
 * 
 * @author Aparna Chaudhary
 */
public class CheckTablespace extends AbstractCheck {

	private static final Logger logger = LoggerFactory.getLogger(CheckTablespace.class);
	private boolean debug = false;

	public CheckTablespace(boolean debug) {
		this.debug = debug;
	}

	/**
	 * Checks tablespace usage
	 * 
	 * @param connection SQL connection
	 * @param tablespace name of tablespace for which percent usage is checked
	 * @param warningThreshold warning threshold
	 * @param crticalThreshold critical threshold
	 */
	public void performCheck(Connection connection, String tablespace, String warningThreshold, String crticalThreshold) {
		try {
			String query = "SELECT tablespace_name, round(sum(bytes)/1024/1024) AS USED_MB, round(sum(maxbytes)/1024/1024) AS ACTUAL_MB "
					+ "FROM dba_data_files " + "WHERE tablespace_name = ? " + "group by tablespace_name";

			if (debug) {
				logger.debug("Executing query " + query);
			}

			// execute query
			PreparedStatement statement = connection.prepareStatement(query);
			statement.setObject(1, tablespace);
			ResultSet rs = statement.executeQuery();

			String tbspname = "";
			int warning = Integer.valueOf(warningThreshold);
			int crtical = Integer.valueOf(crticalThreshold);
			int actualSpace = 0;
			int usedSpace = 0;
			int percent_used = 0;
			if (rs != null && rs.next()) {
				tbspname = rs.getString(1);
				usedSpace = rs.getInt(2);
				actualSpace = rs.getInt(3);
				percent_used = usedSpace / actualSpace;

				if (debug) {
					logger.debug(String.format("Name: %20s ", tbspname));
					logger.debug(String.format("Space used: %10d MB", usedSpace));
					logger.debug(String.format("Space total: %10d MB", actualSpace));
					logger.debug(String.format("Space %% used: %3d %%\n", percent_used));
				}
			}

			rs.close();
			statement.close();
			connection.close();

			String perfdata = tbspname + "_usage=" + percent_used + "%;" + warning + ";" + crtical;
			String output = tbspname + ": used " + usedSpace + "MB  of " + actualSpace + "MB" + "|" + perfdata;

			// verify level
			checkLevel(percent_used, warning, crtical, output);

		} catch (SQLException e) {
			System.err.println(e);
			System.exit(UNKNOWN.getCode());
		}

	}
}
