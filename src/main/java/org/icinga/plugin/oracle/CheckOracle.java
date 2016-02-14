package org.icinga.plugin.oracle;

import static org.icinga.plugin.oracle.NagiosStatus.UNKNOWN;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
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

	private static final Logger logger = LoggerFactory.getLogger(CheckOracle.class);
	boolean debug = false;

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
			options.addOption(OptionBuilder.isRequired(true).withDescription("The warning threshold you want to set")
					.withType(String.class).hasArg().create("W"));
			options.addOption(OptionBuilder.isRequired(true).withDescription("The critical threshold you want to set")
					.withType(String.class).hasArg().create("C"));

			options.addOption("t", "tablespace", true, "The tablespace to check");
			options.addOption("s", "sessions", true, "The username for which session count to check");

			BasicParser parser = new BasicParser();
			CommandLine commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options);
			}
			if (commandLine.hasOption('d')) {
				debug = Boolean.valueOf(commandLine.getOptionValue('d'));
			}

			String hostname = commandLine.getOptionValue('H');
			Integer port = ((Number) commandLine.getParsedOptionValue("P")).intValue();
			String instanceName = commandLine.getOptionValue('I');
			String username = commandLine.getOptionValue('u');
			String password = commandLine.getOptionValue('p');

			executeCheck(commandLine, debug, hostname, port, instanceName, username, password);

		} catch (ParseException e) {
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
	private void executeCheck(CommandLine commandLine, boolean debug, String hostname, Integer port, String instanceName,
			String username, String password) {
		Connection conn = null;
		try {

			String warning = commandLine.getOptionValue('W');
			String crtical = commandLine.getOptionValue('C');

			conn = getConnection(hostname, port, instanceName, username, password);

			if (commandLine.hasOption('t')) {
				String tablespace = commandLine.getOptionValue('t');
				if (tablespace.equalsIgnoreCase("ALL")) {
					CheckTablespaces.performCheck(conn, warning, crtical, debug);
				} else {
					CheckTablespace.performCheck(conn, tablespace, warning, crtical, debug);
				}
			} else if (commandLine.hasOption('s')) {
				String userToCheck = commandLine.getOptionValue('s');
				CheckSessions.performCheck(conn, userToCheck, warning, crtical, debug);
			} else {
				System.err.println("Error: Invalid option");
				System.exit(UNKNOWN.getCode());
			}
		} catch (IllegalArgumentException e) {
			System.out.println(String.format("UNKNOWN - %s", e.getMessage()));
			System.exit(UNKNOWN.getCode());
		} catch (Exception e) {
			if (debug) {
				logger.error("Failed to execute check", e);
			}
			System.err.println("Error: Failed to execute check" + e);
			System.exit(UNKNOWN.getCode());
		} finally {
			try {
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				System.err.println("Error: Failed to close JDBC connection");
				System.exit(UNKNOWN.getCode());
			}
		}
	}

	/**
	 * Print help text
	 */
	public void printHelp(final Options options) {
		final String commandLineSyntax = "java -jar nagios-plugin-oracle.jar";
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
			System.err.println("Error: Failed to load JDBC driver.");
			System.err.println(e);
			System.exit(UNKNOWN.getCode());
		}

		if (debug) {
			logger.debug("Connection URL: " + String.format("jdbc:oracle:thin:@%s:%s:%s", hostname, port, instance));
		}
		String connUrl = String.format("jdbc:oracle:thin:@%s:%s:%s", hostname, port, instance);
		Connection connection = DriverManager.getConnection(connUrl, username, password);
		return connection;
	}

}