package com.isecpartners.android.jdwp;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.ShellFactory;

import com.isecpartners.android.jdwp.common.QueueAgent;

public class CommandLine extends QueueAgent {

	private static Logger LOGGER = Logger
			.getLogger(CommandLine.class.getName());

	public static void main(String args[]) {
		Layout layout = new EnhancedPatternLayout(
				org.apache.log4j.EnhancedPatternLayout.TTCC_CONVERSION_PATTERN);
		
		ConsoleAppender c = new org.apache.log4j.ConsoleAppender();
		c.setWriter(new PrintWriter(System.out));
		c.setLayout(layout);

		Logger.getRootLogger().addAppender(c);
		
		try {
			FileAppender f = new org.apache.log4j.FileAppender(layout , "logs.txt");
			Logger.getRootLogger().addAppender(f);
		} catch (IOException e1) {
			LOGGER.error("could not get file appender: " + e1);
		}

		try {
			ShellFactory.createConsoleShell("cmd", "", new CommandLine())
					.commandLoop();
		} catch (IOException e) {
			LOGGER.error("main exited with exception: " + e);
		}
	}

	public CommandLine() {

	}

	@Command(name = "attach", abbrev = "a", description = "Attach to JDWP process")
	public void connect(
			@Param(name = "host", description = "target virtual machine host") String host,
			@Param(name = "port", description = "target virtual machine jdwp listening port (see 'adb jdwp' and use DDMS to discover port)") String port) {

		CommandLine.LOGGER.info("connecting - " + host + " : " + port
				+ " plugins search path: " + "plugins");
		Control ctrl = new Control(host, port, "plugins");
		ctrl.start();
	}

	@Command(name = "detach", abbrev = "d", description = "Detach from JDWP process")
	public void detach() {
		CommandLine.LOGGER.info("not implemented");
	}

	@Command(name = "quit", abbrev = "q")
	public void quit() {
		CommandLine.LOGGER.info("quit called");
		System.exit(0);
	}

	@Command(name = "test", abbrev = "t")
	public void test(
			@Param(name = "port", description = "target virtual machine jdwp listening port (see 'adb jdwp' and use DDMS to discover port)") String port) {
		this.connect("localhost", port);
	}
}
