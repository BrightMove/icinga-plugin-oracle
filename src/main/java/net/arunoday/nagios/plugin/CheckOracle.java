package net.arunoday.nagios.plugin;

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
 * @author Aparna Chaudhary
 */
public class CheckOracle {

	private static final Logger logger = LoggerFactory.getLogger(CheckOracle.class);

	private static final int OK_STATUS_CODE = 0;
	private static final int WARNING_STATUS_CODE = 1;
	private static final int CRITICAL_STATUS_CODE = 2;
	private static final int UNKNOWN_STATUS_CODE = 3;

	public static void main(String args[]) {
		CheckOracle checkOracle = new CheckOracle();
		checkOracle.parseOptions(args);
	}

	@SuppressWarnings("static-access")
	private void parseOptions(String[] args) {
		try {
			Options options = new Options();
			options.addOption("h", "help", false, "Print help for this application");
			options.addOption(OptionBuilder.withDescription("Option to enable debugging [true|false]").withLongOpt("debug")
					.withType(Boolean.class).hasArg().create('d'));
			options.addOption("D", false, "Enable output of Nagios performance data");
			options.addOption(OptionBuilder.isRequired(true).withDescription("The database hostname to connect to")
					.withLongOpt("host").withType(String.class).hasArg().create('H'));
			options.addOption(OptionBuilder.isRequired(true).withDescription("The database listener port")
					.withLongOpt("port").withType(Number.class).hasArg().create("P"));
			options.addOption(OptionBuilder.isRequired(true).withDescription("The database instance name")
					.withLongOpt("instance").withType(String.class).hasArg().create("I"));
			options.addOption(OptionBuilder.isRequired(true).withDescription("The username you want to login as")
					.withLongOpt("user").withType(String.class).hasArg().create("u"));
			options.addOption(OptionBuilder.isRequired(true).withDescription("The password for the user")
					.withLongOpt("password").withType(String.class).hasArg().create("p"));
			options.addOption("W", true, "The warning threshold you want to set");
			options.addOption("C", true, "The critical threshold you want to set");

			options.addOption("t", "tablespace", true, "The tablespace to check");

			BasicParser parser = new BasicParser();
			CommandLine commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options);
			}
			boolean debug = false;
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
			System.err.println("Error: Failed to parse options.");
			System.err.println(e);
			System.exit(UNKNOWN_STATUS_CODE);
		}
	}

	private void executeCheck(CommandLine commandLine, boolean debug, String hostname, Integer port, String instanceName,
			String username, String password) {
		Connection connection = null;
		try {

			Object warning = commandLine.getOptionValue('W');
			Object crtical = commandLine.getOptionValue('C');

			if (debug) {
				logger.debug("Connection URL: " + String.format("jdbc:oracle:thin:@%s:%s:%s", hostname, port, instanceName));
			}
			connection = getConnection(hostname, port, instanceName, username, password);

			if (commandLine.hasOption('t')) {
				Object tablespace = commandLine.getOptionValue('t');
				CheckTablespace checkTablespace = new CheckTablespace(debug);
				checkTablespace.performCheck(connection, tablespace, warning, crtical);
			}
		} catch (Exception e) {
			System.err.println("Error: Failed to execute check");
			System.err.println(e);
			System.exit(UNKNOWN_STATUS_CODE);
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				System.err.println("Error: Failed to close JDBC connection");
				System.err.println(e);
				System.exit(UNKNOWN_STATUS_CODE);
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

	protected Connection getConnection(String hostname, Integer port, String instance, String username, String password)
			throws SQLException {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			System.err.println("Error: Failed to load JDBC driver.");
			System.err.println(e);
			System.exit(UNKNOWN_STATUS_CODE);
		}

		String connUrl = String.format("jdbc:oracle:thin:@%s:%s:%s", hostname, port, instance);
		Connection connection = DriverManager.getConnection(connUrl, username, password);
		return connection;
	}

}