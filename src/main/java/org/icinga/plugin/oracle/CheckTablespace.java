package org.icinga.plugin.oracle;

import static org.icinga.plugin.oracle.NagiosStatus.UNKNOWN;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.icinga.plugin.oracle.bean.TablespaceMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check to provide percent tablespace size usage.
 *
 * @author Aparna Chaudhary
 * @author David Webb
 */
public class CheckTablespace extends CheckAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(CheckTablespace.class);

	/**
	 * Checks all tablespace usage
	 *
	 * @param connection SQL connection
	 * @param warningThreshold warning threshold
	 * @param criticalThreshold critical threshold
	 */
	public static void performCheck(Connection connection, String tablespaceName, String warningThreshold,
			String criticalThreshold, boolean debug) {

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {

			StringBuilder query = new StringBuilder();
			query.append("SELECT b.tablespace_name, \n");
			query.append("  tbs_size, \n");
			query.append("  a.free_space, \n");
			query.append("  ((tbs_size - a.free_space) / tbs_size) * 100 AS pct_used \n");
			query.append("FROM \n");
			query.append("  (SELECT tablespace_name, \n");
			query.append("    ROUND(SUM(bytes)/1024/1024 ,2) AS free_space \n");
			query.append("  FROM dba_free_space \n");
			query.append("  GROUP BY tablespace_name \n");
			query.append("  ) a, \n");
			query.append("  (SELECT tablespace_name, \n");
			query.append("    SUM(bytes)/1024/1024 AS tbs_size \n");
			query.append("  FROM dba_data_files \n");
			query.append("  GROUP BY tablespace_name \n");
			query.append("  ) b \n");
			query.append("WHERE a.tablespace_name(+)=b.tablespace_name \n");
			query.append("AND upper(b.tablespace_name) = upper(?) \n");

			if (debug) {
				LOG.debug("Executing query " + query.toString());
			}

			// execute query
			pstmt = connection.prepareStatement(query.toString());
			pstmt.setString(1, tablespaceName);
			rs = pstmt.executeQuery();

			String tbspname = "";
			int warning = Integer.valueOf(warningThreshold);
			int crtical = Integer.valueOf(criticalThreshold);
			float actualSpace = 0.0F;
			float usedSpace = 0.0F;
			float freeSpace = 0.0F;
			float percent_used = 0.0F;

			List<TablespaceMetric> readings = new ArrayList<TablespaceMetric>();

			if (rs != null && rs.next()) {
				tbspname = rs.getString("tablespace_name");
				actualSpace = rs.getFloat("tbs_size");
				freeSpace = rs.getFloat("free_space");
				usedSpace = actualSpace - freeSpace;
				percent_used = rs.getFloat("pct_used");

				readings.add(
						new TablespaceMetric(tbspname, percent_used, 100.00F - percent_used, new Float(actualSpace).doubleValue(),
								new Float(usedSpace).doubleValue(), new Float(freeSpace).doubleValue()));

				if (debug) {
					LOG.debug(String.format("Name:          %s ", tbspname));
					LOG.debug(String.format("Space used:    %,10.2f MB", usedSpace));
					LOG.debug(String.format("Space total:   %,10.2f MB", actualSpace));
					LOG.debug(String.format("Space %% used: %3.2f %%\n", percent_used));
				}
			} else {
				throw new IllegalArgumentException(String.format("Tablespace [%s] does not exist", tablespaceName));
			}

			// verify level
			checkLevel(readings, warning, crtical);

		} catch (SQLException e) {
			System.err.println(e);
			System.exit(UNKNOWN.getCode());
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException muted) {
				}
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException muted) {
				}

			}

		}

	}
}
