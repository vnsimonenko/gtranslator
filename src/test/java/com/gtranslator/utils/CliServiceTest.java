package com.gtranslator.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CliServiceTest {
    CliService cliService;

    @Before
    public void before() {
        cliService = new CliService().builder("D")
                .argName("property=value")
                .numberOfArgs(2)
                .desc("use value for given property")
                .hasArg(true)
                .valueSeparator('=')
                .build()
                .builder("b")
                .argName("file")
                .desc("use given build-file")
                .hasArg(true)
                .longOpt("build-file")
                .build()
                .builder("help")
                .desc("print this message")
                .required()
                .build();
    }

    @Test
    public void testCreateOptions() {
        cliService.print();
    }

    @Test
    public void testParseOptions() throws ParseException {
        String[] args = new String[]{
                "--build-file=app.java",
                "-D",
                "key=value",
                "-help"
        };

        // parse the command line arguments
        CommandLine line = cliService.parse(args);

        // validate that build-file has been set
        if (line.hasOption("build-file")) {
            // print the value of build-file
            System.out.println(line.getOptionValue("build-file"));
        }
        if (line.hasOption("D")) {
            System.out.println(line.getOptionValue("D"));
        }
        if (line.hasOption("D")) {
            System.out.println("help");
        }
        Assert.assertEquals("app.java", line.getOptionValue("build-file"));
        Assert.assertEquals("key=value", line.getOptionValue("D"));
        Assert.assertTrue(line.hasOption("help"));
    }

    @Test
    public void testHaveOptions() {
        String[] args = new String[]{
                "--build-file",
                "-D",
                "key=value",
                "-help"
        };
        try {
            cliService.parse(args);
            Assert.fail();
        } catch (ParseException e) {
            Assert.assertTrue(e.getMessage().matches(".*Missing argument for option: b"));
        }

        args = new String[]{
                "--build-file=app.java",
                "-D",
                "-help"
        };
        try {
            cliService.parse(args);
            Assert.fail();
        } catch (ParseException e) {
            //Missing argument for option: D
            Assert.assertTrue(e.getMessage().matches(".*Missing required option: help"));
        }

        args = new String[]{
                "-D",
                "--build-file=app.java",
                "-help"
        };
        try {
            cliService.parse(args);
            Assert.fail();
        } catch (ParseException e) {
            Assert.assertTrue(e.getMessage().matches(".*Missing argument for option: D"));
        }

        args = new String[]{
                "--build-file=app.java",
                "-D",
                "key=value",
                "help"
        };
        try {
            cliService.parse(args);
            Assert.fail();
        } catch (ParseException e) {
            Assert.assertTrue(e.getMessage().matches(".*Missing required option: help"));
        }
    }

    @Test
    public void testNormalText() {
        System.out.println(Utils.normalText("12werty23"));
        System.out.println(Utils.normalText("werty"));
        System.out.println(Utils.normalText("w"));
        System.out.println(Utils.normalText("1w q "));
        System.out.println(Utils.normalText(" w q "));
    }
}
