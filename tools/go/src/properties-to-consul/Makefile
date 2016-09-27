GO := $(shell which go)

GOPATH := $(shell pwd)/../..

export PATH := $(GOPATH)/bin:$(PATH)

build: properties-to-consul

properties-to-consul:
	@$(GO) build

clean:
	@$(GO) clean

