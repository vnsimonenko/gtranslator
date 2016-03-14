package com.gtranslator.utils;

import org.apache.commons.cli.*;

public final class CliService {
    Options options = new Options();
    Builder builder;

    public CliService() {
    }

    public void print() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("domain", options);
    }

    public CommandLine parse(String[] args) throws ParseException {
        // create the command line parser
        CommandLineParser parser = new DefaultParser();
        // parse the command line arguments
        return parser.parse(options, args);
    }

    public Builder builder(String opt) {
        return builder = new Builder(opt);
    }

    public class Builder {
        Option.Builder builder;

        private Builder(String opt) {
            this.builder = Option.builder(opt);
        }

        public Builder longOpt(String opt) {
            builder.longOpt(opt);
            return this;
        }

        public Builder numberOfArgs(int number) {
            builder.numberOfArgs(number);
            return this;
        }

        public Builder desc(String desc) {
            builder.desc(desc);
            return this;
        }

        public Builder hasArg(boolean b) {
            builder.hasArg(b);
            return this;
        }

        public Builder valueSeparator(char c) {
            builder.valueSeparator(c);
            return this;
        }

        public Builder required() {
            builder.required();
            return this;
        }

        public Builder argName(String argName) {
            builder.argName(argName);
            return this;
        }

        public CliService build() {
            CliService.this.options.addOption(builder.build());
            return CliService.this;
        }
    }

}
