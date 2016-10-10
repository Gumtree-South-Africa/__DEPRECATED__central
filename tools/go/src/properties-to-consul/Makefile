GO := $(shell which go)

GOPATH := $(shell pwd)/../..

export PATH := $(GOPATH)/bin:$(PATH)

build: properties-to-consul

properties-to-consul:
	@$(GO) build

build-linux: properties-to-consul-linux

properties-to-consul-linux:
	@GOOS=linux $(GO) build -o properties-to-consul-linux

clean:
	@$(GO) clean
	rm -f properties-to-consul*

