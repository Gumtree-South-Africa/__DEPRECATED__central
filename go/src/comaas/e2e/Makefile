GOVERSION = go1.7
GO        = $(HOME)/$(GOVERSION)/bin/go
GOFMT     = $(HOME)/$(GOVERSION)/bin/gofmt
GOIMPORTS = $(HOME)/$(GOVERSION)/bin/goimports

ROOT      = $(realpath $(dir $(lastword $(MAKEFILE_LIST))))
GOPATH    = $(realpath $(ROOT)/../../..)
export GOPATH

.PHONY: clean
clean:
	@rm -f main

.PHONY: build
build:
	@$(GOFMT) -w -s .
	@$(GOIMPORTS) -w .
	@$(GO) build main.go

.PHONY: help
help: 
	@echo "Usage: 'make command'"
	@echo 
	@echo "Possible commands:"
	@echo 
	@echo "  build      builds the end to end tests"
	@echo "  clean      removes the built end to end tests"
	@echo 
