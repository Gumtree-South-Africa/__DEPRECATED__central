package main

import (
	"flag"
	"log"
	"net/http"
	"net/url"
	"os"
	"path"
	"strings"

	"github.com/rickar/props"
)

func readProperties(filename string) (*props.Properties, error) {
	log.Printf("Reading properties from file %s\n", filename)
	reader, err := os.Open(filename)
	if err != nil {
		return nil, err
	}
	return props.Read(reader)
}

func main() {
	consulAddress := flag.String("consul", "http://localhost:8500/", "consul host to connect to")
	prefix := flag.String("prefix", "", "prefix or directory in consul, properties will be saved to consuladdress/v1/kv/prefix/...")
	filename := flag.String("file", "replyts.properties", "the properties file to read")
	flag.Parse()

	*consulAddress += "/v1/kv/"

	properties, err := readProperties(*filename)
	if err != nil {
		log.Fatalf("Encountered an error while reading input file %s: %s", *filename, err)
	}
	names := properties.Names()

	client := &http.Client{}
	u, err := url.Parse(*consulAddress + *prefix)
	if err != nil {
		log.Fatalf("Encountered an error parsing Consul URL %s: %s", *consulAddress, err)
	}
	p := path.Clean(u.RequestURI())

	log.Printf("Importing %d properties into Consul at %s", len(names), u.String())

	for _, name := range names {
		u.Path = p + "/" + name
		urlStr := u.String()

		req, err := http.NewRequest("PUT", urlStr, strings.NewReader(properties.Get(name)))
		if err != nil {
			log.Fatalf("Encountered an error creating http request for %q: %s", urlStr, err)
		}

		resp, err := client.Do(req)
		if err != nil {
			log.Fatalf("Encountered an error reading %q: %s", urlStr, err)
		}

		if resp.StatusCode != http.StatusOK {
			log.Printf("[ERROR] PUT returned status code != 200: %d for %q\n%q", resp.StatusCode, urlStr, resp)
		}
	}
}
