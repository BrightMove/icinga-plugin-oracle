package org.icinga.plugin.oracle;

import static org.icinga.plugin.oracle.NagiosStatus.CRITICAL;
import static org.icinga.plugin.oracle.NagiosStatus.OK;
import static org.icinga.plugin.oracle.NagiosStatus.UNKNOWN;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for Nagios checks.
 * 
 * @author Aparna Chaudhary
 * @author David Webb
 */
public class CheckOracle {

	private static final Logger LOG = LoggerFactory.getLogger(CheckOracle.class);
	private boolean debug = false;

	public static void main(String args[]) {
		CheckOracle checkOracle = new CheckOracle();
		checkOracle.parseOptions(args);
	}

	@SuppressWarnings("static-access")
	private void parseOptions(String[] args) {
		Options options = null;
		try {
			options = new Options();
			options.addOption("h", "help", false, "Print help for this application");
			options.addOption(OptionBuilder.withDescription("Option to enable debugging [true|false]").withLongOpt("debug")
					.withType(Boolean.class).hasArg().create('d'));
			options.addOption("D", false, "Enable output of Nagios performance data");
			options.addOption(OptionBuilder.isRequired(true).withDescription("The database hostname to connect to")
					.withLongOpt("host").withType(String.class).hasArg().create('H'));
			options.addOption(OptionBuilder.isRequired(true).withDescription("The database listener port").withLongOpt("port")
					.withType(Number.class).hasArg().create("P"));
			options.addOption(OptionBuilder.isRequired(true).withDescription("The database instance name")
					.withLongOpt("instance").withType(String.class).hasArg().create("I"));
			options.addOption(OptionBuilder.isRequired(true).withDescription("The username you want to login as")
					.withLongOpt("user").withType(String.class).hasArg().create("u"));
			options.addOption(OptionBuilder.isRequired(true).withDescription("The password for the user")
					.withLongOpt("password").withType(String.class).hasArg().create("p"));
			options.addOption(OptionBuilder.isRequired(false).withDescription("The warning threshold you want to set")
					.withType(String.class).hasArg().create("W"));
			options.addOption(OptionBuilder.isRequired(false).withDescription("The critical threshold you want to set")
					.withType(String.class).hasArg().create("C"));

			OptionGroup checkOptionGroup = new OptionGroup();
			checkOptionGroup.addOption(
					OptionBuilder.isRequired(false).withDescription("The tablespace to check, pass ALL for all tablespaces")
							.withLongOpt("tablespace").withType(String.class).hasArg().create("t"));
			checkOptionGroup.addOption(OptionBuilder.isRequired(false)
					.withDescription("The username for which session count to check, pass ALL to count all sessions")
					.withLongOpt("sessions").withType(String.class).hasArg().create("s"));
			checkOptionGroup.addOption(
					OptionBuilder.isRequired(false).withDescription("Check that a connection can be made to the database.")
							.withLongOpt("tns-listener-check").create("tns"));
			options.addOptionGroup(checkOptionGroup);

			BasicParser parser = new BasicParser();
			CommandLine commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options);
			}
			if (commandLine.hasOption('d')) {
				debug = true;
			}

			String hostname = commandLine.getOptionValue('H');
			Integer port = ((Number) commandLine.getParsedOptionValue("P")).intValue();
			String instanceName = commandLine.getOptionValue('I');
			String username = commandLine.getOptionValue('u');
			String password = commandLine.getOptionValue('p');

			executeCheck(commandLine, hostname, port, instanceName, username, password);

		} catch (ParseException e) {
			LOG.error("ParseException", e);
			printHelp(options);
			System.exit(UNKNOWN.getCode());
		}
	}

	/**
	 * Executes the check based on provided options
	 * 
	 * @param commandLine
	 * @param debug flag to enable debug logging
	 * @param hostname oracle server host
	 * @param port listener port
	 * @param instanceName instance name
	 * @param username DBA user name
	 * @param password password
	 */
	private void executeCheck(CommandLine commandLine, String hostname, Integer port, String instanceName,
			String username, String password) {
		Connection conn = null;
		try {

			String warning = commandLine.getOptionValue('W');
			String crtical = commandLine.getOptionValue('C');

			if (commandLine.hasOption("tns")) {
				try {
					conn = getConnection(hostname, port, instanceName, username, password);

					System.out.println(String.format("OK - Connected to %s (%s)", conn.getMetaData().getDatabaseProductName(),
							conn.getMetaData().getDatabaseProductVersion()));
					System.exit(OK.getCode());
				} catch (SQLException e) {
					LOG.error("TNS Check SQLException", e);
					System.out.println("Error: Unable to connect to database - " + e.getMessage());
					System.exit(CRITICAL.getCode());
				}
			} else {
				conn = getConnection(hostname, port, instanceName, username, password);
			}

			if (commandLine.hasOption('t')) {
				String tablespace = commandLine.getOptionValue('t');
				if (tablespace.equalsIgnoreCase("ALL")) {
					CheckTablespaces.performCheck(conn, warning, crtical, debug);
				} else {
					CheckTablespace.performCheck(conn, tablespace, warning, crtical, debug);
				}
			} else if (commandLine.hasOption('s')) {
				String userToCheck = commandLine.getOptionValue('s');
				if (userToCheck.equalsIgnoreCase("ALL")) {
					CheckDatabaseSessions.performCheck(conn, warning, crtical, debug);
				} else {
					CheckUserSessions.performCheck(conn, userToCheck, warning, crtical, debug);
				}
			} else {
				LOG.error("Error: Invalid option");
				System.out.println("Error: Invalid option");
				System.exit(UNKNOWN.getCode());
			}
		} catch (IllegalArgumentException e) {
			LOG.error(String.format("UNKNOWN - %s", e.getMessage()));
			System.out.println(String.format("UNKNOWN - %s", e.getMessage()));
			System.exit(UNKNOWN.getCode());
		} catch (Exception e) {
			LOG.error("Failed to execute check", e);
			System.out.println("Error: Failed to execute check" + e);
			System.exit(UNKNOWN.getCode());
		} finally {
			try {
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				LOG.error("SQLException closing connection", e);
				System.out.println("Error: Failed to close JDBC connection");
				System.exit(UNKNOWN.getCode());
			}
		}
	}

	/**
	 * Print help text
	 */
	public void printHelp(final Options options) {
		final String commandLineSyntax = "check_oracle";
		final HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.printHelp(commandLineSyntax, options, true);
	}

	/**
	 * Gets a SQL connection based on input parameters
	 * 
	 * @param hostname oracle server host
	 * @param port listener port
	 * @param instance instance name
	 * @param username DBA user name
	 * @param password password
	 * @return SQL connection
	 * @throws SQLException thrown when SQL connection cannot be established
	 */
	protected Connection getConnection(String hostname, Integer port, String instance, String username, String password)
			throws SQLException {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			System.out.println("Error: Failed to load JDBC driver.");
			System.out.println(e);
			System.exit(UNKNOWN.getCode());
		}

		if (debug) {
			LOG.debug("Connection URL: " + String.format("jdbc:oracle:thin:@%s:%s:%s", hostname, port, instance));
		}
		String connUrl = String.format("jdbc:oracle:thin:@%s:%s:%s", hostname, port, instance);
		Connection connection = DriverManager.getConnection(connUrl, username, password);
		return connection;
	}

}