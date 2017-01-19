package urls

import (
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"strings"
)

type Path struct {
	Tenant string
	Path   string
}

type Response struct {
	Code   int
	Search string
}

func Expect(url string, expected Response) error {
	log.Printf("Retrieving (GET) %q", url)

	resp, err := http.Get(url)
	if err != nil {
		return fmt.Errorf("lookup for %q: %s", url, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != expected.Code {
		return fmt.Errorf("lookup for %q: status %d != %d", url, resp.StatusCode, expected.Code)
	}

	data, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("lookup for %q: %s", url, err)
	}

	if !strings.Contains(string(data), expected.Search) {
		return fmt.Errorf("lookup for %q: %q not found", url, expected.Search)
	}

	log.Printf("lookup for %q passed (status = %d)", url, resp.StatusCode)

	return nil
}
