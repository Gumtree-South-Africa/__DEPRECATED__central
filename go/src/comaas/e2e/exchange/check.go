package exchange

import (
	"bytes"
	"fmt"
	"log"
	"math/rand"
	"net"
	"net/mail"
	"net/smtp"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/mxk/go-imap/imap"
)

type Auth struct {
	Host      string
	Cloakhost string
	Buyer     string
	Seller    string
	Password  string
}

type Message struct {
	Subject string
	From    string
	Date    string
	IsSeen  bool
	Uid     uint32
}

func Mail(auth Auth, mxhost string, lookup bool) error {
	var resolved string
	if lookup {
		mxs, err := net.LookupMX(mxhost)
		if err != nil {
			return err
		}
		if len(mxs) == 0 {
			return fmt.Errorf("lookup MX %q: no MX records found")
		}

		resolved = strings.TrimSuffix(mxs[0].Host, ".")
		log.Printf("lookup MX %q: found %s", mxhost, resolved)
	} else {
		resolved = mxhost
	}

	adid, err := send(auth, resolved)
	if err != nil {
		return err
	}

	time.Sleep(5 * time.Second)

	message, err := check(auth)
	if err != nil {
		return err
	}

	if !strings.Contains(message.Subject, adid) || message.IsSeen == true {
		return fmt.Errorf("Latest e-mail from the seller mail box does not contain an unseen e-mail with the Ad-Id")
	} else {
		fmt.Println("Successfully received e-mail with Ad-Id taken from subject:", message.Subject)
	}

	return nil
}

func send(auth Auth, mxhost string) (adid string, err error) {
	cloak := mail.Address{"", "e2e@" + auth.Cloakhost}
	buyer := mail.Address{"", auth.Buyer}
	seller := mail.Address{"", auth.Seller}

	adid = time.Now().Format("20060102030405")
	headers := map[string]string{
		"Return-Path":   cloak.String(),
		"X-Original-To": seller.String(),
		"Delivered-To":  seller.String(),

		"From":     cloak.String(),
		"Reply-To": buyer.String(),
		"To":       seller.String(),
		"Subject":  "I buy dis yes? #" + adid,

		"X-Mailer":        "COMaaS End-to-End Test Mailer",
		"X-ADID":          adid,
		"X-MESSAGE-ID":    strconv.Itoa(rand.Int()),
		"X-MESSAGE-TYPE":  "chat",
		"X-REPLY-CHANNEL": "desktop",
		"X-RTS2":          "true",
		"X-USER-MESSAGE":  "Hey I buy dis nao",
	}

	var buf bytes.Buffer
	for k, v := range headers {
		fmt.Fprintf(&buf, "%s: %s\r\n", k, v)
	}
	buf.WriteString("\r\nHey I buy dis nao")

	c, err := smtp.Dial(mxhost + ":25")
	if err != nil {
		return "", err
	}

	if err = c.Mail(cloak.Address); err != nil {
		return "", err
	}
	if err = c.Rcpt(seller.Address); err != nil {
		return "", err
	}

	w, err := c.Data()
	if err != nil {
		return "", err
	}

	if _, err := buf.WriteTo(w); err != nil {
		return "", err
	}

	if err = w.Close(); err != nil {
		return "", err
	}

	return adid, c.Quit()
}

func check(auth Auth) (message *Message, err error) {
	imap.DefaultLogger = log.New(os.Stdout, "", 0)
	imap.DefaultLogMask = imap.LogConn

	c, err := imap.DialTLS(auth.Host, nil)
	if err != nil {
		return nil, err
	}
	defer c.Logout(30 * time.Second)

	c.Data = nil

	if c.Caps["STARTTLS"] {
		c.StartTLS(nil)
	}

	if _, err := c.Login(auth.Seller, auth.Password); err != nil {
		return nil, err
	}

	cmd, err := imap.Wait(c.Select("INBOX", true))
	if err != nil {
		return nil, err
	}

	c.Data = nil

	// Retrieve only the latest message
	set, err := imap.NewSeqSet("*")
	if err != nil {
		return nil, err
	}
	// Retrieve the flags so that we can check whether the message is unread
	cmd, err = c.UIDFetch(set, "RFC822.HEADER", "FLAGS")
	if err != nil {
		return nil, err
	}

	messages := make([]*Message, 0, 1)
	for cmd.InProgress() {
		c.Recv(-1)

		for _, rsp := range cmd.Data {
			if err != nil {
				return nil, err
			}
			header := imap.AsBytes(rsp.MessageInfo().Attrs["RFC822.HEADER"])
			uid := imap.AsNumber((rsp.MessageInfo().Attrs["UID"]))
			flags := rsp.MessageInfo().Flags
			if msg, err := mail.ReadMessage(bytes.NewReader(header)); msg != nil {
				if err != nil {
					return nil, err
				}
				message := &Message{
					Subject: msg.Header.Get("Subject"),
					From:    msg.Header.Get("From"),
					Date:    msg.Header.Get("Date"),
					IsSeen:  flags["\\Seen"],
					Uid:     uid,
				}
				messages = append(messages, message)
			}
		}
		cmd.Data = nil
		c.Data = nil
	}

	if rsp, err := cmd.Result(imap.OK); err != nil {
		if err == imap.ErrAborted {
			fmt.Println("Fetch command aborted")
		} else {
			fmt.Println("Fetch error:", rsp.Info)
		}
	}

	return messages[0], nil
}
