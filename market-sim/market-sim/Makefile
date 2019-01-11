# Make file for building and doing most things of use with the market simulator
# usage: see `make help`
SHELL := /usr/bin/env bash -euf -o pipefail

# Detect os for certain commands
UNAME := $(shell uname -s)
ifeq ($(UNAME),Linux)
	BROWSER := sensible-browser
else
ifeq ($(UNAME),Darwin)
	PREFIX = g
	BROWSER := open
else
	# The error variable is never used, it just enforces that the error gets
	# thrown immediately
	ERROR := $(error Not using Mac or Linux)
endif
endif


# Color the output of maven with long but simple sed script
COLOR = cat

# Current directory
PWD := $(shell pwd)

# Set error for egta target of `def` is not defined
ifndef def
	DEFERROR = $(error `def` is undefined. Make sure you build egta like \
		`make egta def=<default.json>`. See `make help`)
endif

# Key Class files for auto-generating docs
KEYS := $(wildcard target/site/apidocs/edu/umich/srg/marketsim/Keys.*.html)

## Targets
help:
	@echo "usage: make <target>"
	@echo "where <target> is one of:"
	@echo "  jar    : Compile java into an executable jar that can be called with"
	@echo "           market-sim.sh or run-egta.sh"
	@echo "  test   : Run all java unit tests"
	@echo "  egta def=<defaults.json> : Compile an egta zip file, where defaults.json"
	@echo "           is the location of the desired defaults.json file, and the name"
	@echo "           of the file is the simulator name"
	@echo "  report : Generate and display a number of reports about the java code"
	@echo "  docs   : Compile docmentation any"
	@echo "  clean  : Remove all java byproducts"

jar:
	@echo mvn compile assembly:single
	@mvn compile assembly:single | $(COLOR)

egta: jar
	$(DEFERROR)
	$(eval $@_TMP := $(shell mktemp -d))
	$(eval $@_NAME := $(notdir $(basename $(def))))
	mkdir -p "$($@_TMP)/$($@_NAME)/script/target"
	mkdir -p "$($@_TMP)/$($@_NAME)/script/jq"
	ln -s "$(PWD)/$(def)" "$($@_TMP)/$($@_NAME)/defaults.json"
	ln -s "$(PWD)/target/marketsim-4.0.0-jar-with-dependencies.jar" "$($@_TMP)/$($@_NAME)/script/target/"
	ln -s "$(PWD)/jq/reduce_strats.jq" "$($@_TMP)/$($@_NAME)/script/jq"
	ln -s "$(PWD)/run-egta.sh" "$($@_TMP)/$($@_NAME)/script/batch"
	cd $($@_TMP) && zip -r "$(PWD)/$($@_NAME).zip" "$($@_NAME)"
	rm -rf "$($@_TMP)"

test:
	@echo mvn test
	@mvn test | $(COLOR)

create-report:
	@echo mvn site
	@mvn site | $(COLOR)

report: create-report
	$(BROWSER) target/site/project-reports.html 

docs: ../docs/keys.md

../docs/keys.md: scripts/keys_docs.js $(KEYS)
	mvn javadoc:javadoc | $(COLOR)
	node $^ > $@

clean:
	@echo mvn clean
	@mvn clean | $(COLOR)

.PHONY: docs
