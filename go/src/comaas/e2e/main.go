package main

import (
	"flag"
	"fmt"
	"log"
	"net/http"
	"net/url"

	"comaas/e2e/exchange"
	"comaas/e2e/urls"
)

var paths = map[urls.Path]urls.Response{
	// core

	urls.Path{Path: "/"}:        {Code: http.StatusOK, Search: "Management Console"},
	urls.Path{Path: "/unknown"}: {Code: http.StatusNotFound, Search: ""},

	// ek messagecenter

	urls.Path{Tenant: "ek", Path: "/ebayk-msgcenter/postboxes/test@gmail.com"}: {Code: http.StatusOK, Search: "numUnread"},
}

var auth = exchange.Auth{
	Host:      "imap.gmail.com:993",
	Cloakhost: "mail.gemaaktvoorelkaar.nl",
	Buyer:     "comaas.e2e@gmail.com",
	Seller:    "comaas.e2e.seller@gmail.com",
	Password:  "comaasdabomb#e2e",
}

func main() {
	var tenant, addr, protocol, mxsuffix string
	var mxlookup bool

	flag.StringVar(&tenant, "tenant", "", "the tenant for whom to run e2e checks")
	flag.StringVar(&addr, "addr", "10.249.126.237:18081", "the host:port to check against")
	flag.StringVar(&protocol, "protocol", "http", "the protocol to use")
	flag.StringVar(&mxsuffix, "mxsuffix", ".sandbox.comaas.ecg.so", "the host string to suffix after the tenant ID for MX lookups")
	flag.BoolVar(&mxlookup, "mxlookup", true, "whether to perform an MX lookup on the 'mxsuffix' variable or treat it as an MX host")
	flag.Parse()

	var flags string
	flag.VisitAll(func(f *flag.Flag) {
		flags += fmt.Sprintf("  %s: %s\n", f.Name, f.Value.String())
	})
	log.Printf("[INFO] Running with flags:\n%s\n", flags)

	// Check regular as well as tenant-specific URLs
	for path, response := range paths {
		if path.Tenant != "" && path.Tenant != tenant {
			continue
		}

		resource := url.URL{
			Scheme: protocol,
			Host:   addr,
			Path:   path.Path,
		}

		if err := urls.Expect(resource.String(), response); err != nil {
			log.Fatal(err)
		}
	}

	// Check by sending and waiting for a buyer e-mail over the incoming SMTP channel similar to how a tenant would
	if tenant != "" {
		mxhost := mxsuffix
		if mxlookup == true {
			mxhost = tenant + mxhost
		}
		if err := exchange.Mail(auth, mxhost, mxlookup); err != nil {
			log.Fatal(err)
		}
	}
}
