package com.ecg.comaas.ebayk.postprocessor.linkproxy;

import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor.ProcessedMail;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LinkProxyPostProcessorIntegrationTest {
    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
        Properties properties = new Properties();

        properties.put("replyts.linkescaper.proxyurl", "http://foo.com?url=%s");
        properties.put("replyts.linkescaper.whitelist", "host.com");

        return properties;
    }).get());

    @Test
    public void proxiesUnknownDomains() {
        List<String> plaintextParts = rule.deliver(MailBuilder.aNewMail().from("foo@asdf.com").to("sdf.sf@asdf.com").adId("213").htmlBody("hello http://www.facebook.com")).getOutboundMail().getPlaintextParts();
        for (String plaintextPart : plaintextParts) {
            assertTrue(plaintextPart.contains("http://foo.com?url=http%3A%2F%2Fwww.facebook.com"));
        }
    }

    @Test
    public void doesNotProxyMailsFromDomainsInTees() {
        ProcessedMail deliver = rule.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com").adId("12323").plainBody("hello <http://www.ebay.de> blo"));
        String escaped = deliver.getOutboundMail().getPlaintextParts().get(0);

        assertEquals("hello <http://foo.com?url=http%3A%2F%2Fwww.ebay.de> blo", escaped);
    }

    @Test
    public void doesNotProxyHttpDomainFromWhitelist() {
            ProcessedMail deliver = rule.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com").adId("12323").plainBody("hello <http://www.host.com> blo"));
            String escaped = deliver.getOutboundMail().getPlaintextParts().get(0);

            assertEquals("hello <http://www.host.com> blo", escaped);
    }

    @Test
    public void doesNotProxyHttpsDomainFromWhitelist() {
            ProcessedMail deliver = rule.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com").adId("12323").plainBody("hello <https://www.host.com> blo"));
            String escaped = deliver.getOutboundMail().getPlaintextParts().get(0);

            assertEquals("hello <https://www.host.com> blo", escaped);
    }

    @Test
    public void proxiesMailsFromForm() {

        String body = "Return-Path: <interessent-1k78irrey15cf@mail.ebay-kleinanzeigen.de>\n" +
                "Received: from kreplyts44-2.mobile.rz (cmsserver44-1.220.mobile.rz [10.44.220.9])\n" +
                "\tby webmail44-1.localdomain (Postfix) with ESMTP id A4E621654D3\n" +
                "\tfor <malte.1369249442905-3@adfeeder.de>; Tue, 28 May 2013 16:40:57 +0200 (CEST)\n" +
                "Received: from kreplyts44-2.mobile.rz (localhost [127.0.0.1])\n" +
                "\tby kreplyts44-2.mobile.rz (Postfix) with ESMTP id 91EBB2240C\n" +
                "\tfor <malte.1369249442905-3@adfeeder.de>; Tue, 28 May 2013 16:40:57 +0200 (CEST)\n" +
                "Date: Tue, 28 May 2013 16:40:55 +0200 (CEST)\n" +
                "Subject: Nutzer-Anfrage zu Ihrer Kleinanzeige \"Badewannengestell \"\n" +
                "MIME-Version: 1.0\n" +
                "Content-Type: text/html;charset=UTF-8\n" +
                "Content-Transfer-Encoding: quoted-printable\n" +
                "Message-ID: <1hre8phhunjulvz05snnfpkgngs@mail.ebay-kleinanzeigen.de>\n" +
                "To: malte.1369249442905-3@adfeeder.de\n" +
                "From: interessent-1k78irrey15cf@mail.ebay-kleinanzeigen.de\n" +
                "X-Originating-IP: 10.44.220.9\n" +
                "\n" +
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4=2E01 Transitional//EN\" \"http://kl=\n" +
                "einanzeigen=2Eebay=2Ede/anzeigen/externer-link-weiterleitung=2Ehtml?to=3Dht=\n" +
                "tp%3A%2F%2Fwww=2Ew3=2Eorg%2FTR%2Fhtml4%2Floose=2Edtd\">\n" +
                "<html>\n" +
                "    <head>\n" +
                "        <meta content=3D\"text/html; charset=3Dutf-8\" http-equiv=3D\"Content=\n" +
                "-Type\" >\n" +
                "        <title>eBay Kleinanzeigen | Kostenlos=2E Einfach=2E Lokal=2E Anzei=\n" +
                "gen gratis inserieren mit eBay Kleinanzeigen</title>\n" +
                "\n" +
                "</head>\n" +
                "    <body vlink=3D\"#001e9b\" alink=3D\"#001e9b\" link=3D\"#001e9b\" bgcolor=3D\"=\n" +
                "#ffffff\" style=3D\"margin: 10px; background-color: #ffffff; font-family: Ari=\n" +
                "al,Helvetica, sans-serif;\">\n" +
                "        <table cellspacing=3D\"0\" border=3D\"0\" style=3D\"width: 590px; backg=\n" +
                "round-color: #ffffff;\" width=3D\"590\" cellpadding=3D\"0\">\n" +
                "            <tbody>\n" +
                "                <!-- **************************************  Header ******=\n" +
                "***************************************************************************=\n" +
                "******************* -->\n" +
                "                <tr>\n" +
                "                    <td valign=3D\"top\" style=3D\"font-size: 12px; font-fami=\n" +
                "ly: Arial,Helvetica, sans-serif; border-bottom-style: solid; border-bottom-=\n" +
                "width: 5px; border-bottom-color: #9aca29; vertical-align: top; width: 590px=\n" +
                ";\" width=3D\"590\">\n" +
                "                        <table cellpadding=3D\"0\" cellspacing=3D\"0\" width=\n" +
                "=3D\"100%\" border=3D\"0\">\n" +
                "                            <tr>\n" +
                "                                <td valign=3D\"middle\" style=3D\"color:#5353=\n" +
                "53; padding-right: 5px; font-size: 12px; font-family: Arial,Helvetica, sans=\n" +
                "-serif; vertical-align: middle; padding-bottom: 10px;\">\n" +
                "                                    <h1 style=3D\"font-size: 18px; margin: =\n" +
                "0; font-family: Arial,Helvetica, sans-serif; color:#535353;\">\n" +
                "                                        <font face=3D\"Arial,Helvetica, san=\n" +
                "s-serif\" color=3D\"#535353\">\n" +
                "                                                                          =\n" +
                "                  Anfrage zu Ihrer Kleinanzeige\n" +
                "                                        </font>\n" +
                "                                    </h1>\n" +
                "                                </td>\n" +
                "                                <td align=3D\"right\" valign=3D\"middle\" styl=\n" +
                "e=3D\"font-size: 12px; font-family: Arial,Helvetica, sans-serif; vertical-al=\n" +
                "ign: middle; padding-bottom: 10px; width: 180px;\" width=3D\"180\">\n" +
                "                                    <a href=3D\"http://kleinanzeigen=2Eebay=\n" +
                "=2Ede/anzeigen/?utm_source=3Demail&utm_medium=3Dsystem_email&utm_campaign=\n" +
                "=3Demail-ContactPoster&utm_content=3DLogo\" ><img src=3D\"http://kleinanzeige=\n" +
                "n=2Eebay=2Ede/static/img/mail/logo=2Egif\" width=3D\"201\" height=3D\"40\" alt=\n" +
                "=3D\"eBay Kleinanzeigen\" border=3D\"0\"  ></a>\n" +
                "                                </td>\n" +
                "                            </tr>\n" +
                "                        </table>\n" +
                "                    </td>\n" +
                "                </tr>\n" +
                "\n" +
                "                <!-- **************************************  Body ********=\n" +
                "***************************************************************************=\n" +
                "***************** -->\n" +
                "                <tr>\n" +
                "                    <td valign=3D\"top\" style=3D\"font-size: 12px; font-fami=\n" +
                "ly: Arial,Helvetica, sans-serif; border-bottom-width: 5px; border-bottom-st=\n" +
                "yle: solid; padding: 5px 0; border-bottom-color: #9aca29; vertical-align: t=\n" +
                "op; width: 590px;\" width=3D\"590\">\n" +
                "                        <table cellpadding=3D\"0\" cellspacing=3D\"0\" width=\n" +
                "=3D\"590\"  border=3D\"0\">\n" +
                "<tr>\n" +
                "    <td valign=3D\"top\" style=3D\"font-size: 12px; font-family: Arial,Helvet=\n" +
                "ica, sans-serif; padding: 10px; vertical-align: top;\">\n" +
                "        <p style=3D\"font-size: 12px; font-family: Arial,Helvetica, sans-se=\n" +
                "rif; margin: 0; padding: 0;\">\n" +
                "            <font face=3D\"Arial,Helvetica, sans-serif\">\n" +
                "                Lieber Nutzer!\n" +
                "                <br><br>\n" +
                "                Ein Interessent hat eine Anfrage zu Ihrer Kleinanzeige ges=\n" +
                "endet:\n" +
                "                <br>\n" +
                "                <a href=3D\"http://kleinanzeigen=2Eebay=2Ede/anzeigen/s-anz=\n" +
                "eige/badewannengestell-/46549409-91-3467?utm_source=3Demail&utm_medium=3Dsy=\n" +
                "stem_email&utm_campaign=3Demail-ContactPoster&utm_content=3DViewAd\" style=\n" +
                "=3D\"text-decoration: underline; color: #001e9b;\" target=3D\"_blank\">'Badewan=\n" +
                "nengestell '</a>\n" +
                "                <br>\n" +
                "                Anzeigennummer: 46549409              =20\n" +
                "            </font>\n" +
                "        </p>\n" +
                "    </td>\n" +
                "</tr>\n" +
                "\n" +
                "<tr>                               =20\n" +
                "    <td valign=3D\"top\" bgcolor=3D\"#f0f0f0\" style=3D\"font-size: 12px; borde=\n" +
                "r-top-color: #ccc; background-color: #f0f0f0; font-family: Arial,Helvetica,=\n" +
                " sans-serif; padding: 10px; border-bottom-style: solid; border-bottom-width=\n" +
                ": 1px; border-bottom-color: #ccc; border-top-style: solid; border-top-width=\n" +
                ": 1px; vertical-align: top;\">\n" +
                "        <p style=3D\"font-size: 12px; font-family: Arial,Helvetica, sans-se=\n" +
                "rif; margin: 0; padding: 0;\">\n" +
                "            <font face=3D\"Arial,Helvetica, sans-serif\">\n" +
                "                    <b>Nachricht von:</b> asdfasfasfasdfasdf\n" +
                "                <br><br>\n" +
                "                asfasdf\n" +
                "<br />asdf\n" +
                "<br />asdfadfas\n" +
                "<br />fasdf\n" +
                "<br />http:&#x2F;&#x2F;www=2Efacebook=2Ecom\n" +
                "            </font>\n" +
                "        </p>\n" +
                "    </td>\n" +
                "</tr>\n" +
                "\n" +
                "<tr>\n" +
                "    <td valign=3D\"top\" style=3D\"font-size: 12px; font-family: Arial,Helvet=\n" +
                "ica, sans-serif; padding: 10px; vertical-align: top;\">\n" +
                "        <p style=3D\"font-size: 12px; font-family: Arial,Helvetica, sans-se=\n" +
                "rif; margin: 0; padding: 0;\">\n" +
                "            <font face=3D\"Arial,Helvetica, sans-serif\">\n" +
                "                    Beantworten Sie diese Nachricht einfach mit der 'Antwo=\n" +
                "rten'-Funktion Ihres E-Mail-Programms=2E\n" +
                "\n" +
                "            </font>\n" +
                "        </p>\n" +
                "    </td>\n" +
                "</tr>\n" +
                "\n" +
                "<!-- Hint Box -->\n" +
                "<tr>\n" +
                "    <td valign=3D\"top\" style=3D\"font-size: 12px; font-family: Arial,Helvet=\n" +
                "ica, sans-serif; padding: 10px; vertical-align: top;\">              =20\n" +
                "        Sch=C3=BCtzen Sie sich vor Betrug: <a target=3D\"_blank\"  style=3D\"=\n" +
                "text-decoration: underline; color: #001e9b;\" href=3D\"http://kleinanzeigen=\n" +
                "=2Eebay=2Ede/anzeigen/sicherheitshinweise=2Ehtml?utm_source=3Demail&utm_med=\n" +
                "ium=3Dsystem_email&utm_campaign=3Demail-ContactPoster&utm_content=3DTipps\">=\n" +
                "Tipps f=C3=BCr Ihre Sicherheit</a>\n" +
                "    </td>\n" +
                "</tr>\n" +
                "\n" +
                "<tr>\n" +
                "    <td valign=3D\"top\" style=3D\"font-size: 12px; font-family: Arial,Helvet=\n" +
                "ica, sans-serif; padding: 10px; vertical-align: top;\">\n" +
                "        <p style=3D\"font-size: 12px; font-family: Arial,Helvetica, sans-se=\n" +
                "rif; margin: 0; padding: 0;\">\n" +
                "            <font face=3D\"Arial,Helvetica, sans-serif\">\n" +
                "                    Falls Sie uns diese E-Mail als Spam oder Betrug melden=\n" +
                " wollen, klicken Sie bitte <a style=3D\"text-decoration: underline; color: #=\n" +
                "001e9b;\" href=3D\"http://kleinanzeigen=2Eebay=2Ede/anzeigen/spam-email-melde=\n" +
                "n=2Ehtml?raspd=3D715baa9f8a9f54eb12b69bd893555569ffa01352243b94f8dfc9eebc2a=\n" +
                "794bae71c633693187d326ed65a2cb3eacdf5dcf25d5b52cbac88178bc9a94b1a690a526fa5=\n" +
                "9c288487e77c2d1ed7acdcb40bdec010acbb89e6d3c46248de420cbdf54fa72da02b94b9dc3=\n" +
                "&utm_source=3Demail&utm_medium=3Dsystem_email&utm_campaign=3Demail-ContactP=\n" +
                "oster&utm_content=3DOther\" target=3D\"_blank\">hier</a>=2E\n" +
                "\n" +
                "                    <br><br>\n" +
                "                    Zum Schutz unserer Nutzer filtern wir Spam und andere =\n" +
                "verd=C3=A4chtige Nachrichten=2E Wir behalten uns vor, bei konkretem Verdach=\n" +
                "t auf betr=C3=BCgerische Aktivit=C3=A4ten oder Verst=C3=B6=C3=9Fen gegen un=\n" +
                "sere Nutzungsbedingungen die =C3=9Cbermittlung von Nachrichten zu verz=C3=\n" +
                "=B6gern oder zu verweigern=2E\n" +
                "            </font>\n" +
                "        </p>\n" +
                "    </td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "    <td valign=3D\"top\" style=3D\"font-size: 12px; font-family: Arial,Helvet=\n" +
                "ica, sans-serif; padding: 10px; vertical-align: top;\">\n" +
                "        <p style=3D\"font-size: 12px; font-family: Arial,Helvetica, sans-se=\n" +
                "rif; margin: 0; padding: 0;\">\n" +
                "            <font face=3D\"Arial,Helvetica, sans-serif\">\n" +
                "                Ihr eBay Kleinanzeigen-Team\n" +
                "            </font>\n" +
                "        </p>\n" +
                "    </td>\n" +
                "</tr>\n" +
                "\n" +
                "\n" +
                "                            <tr>\n" +
                "                                <td valign=3D\"top\" style=3D\"font-size: 12p=\n" +
                "x; font-family: Arial,Helvetica, sans-serif; padding: 10px; vertical-align:=\n" +
                " top;\">\n" +
                "                                    <p style=3D\"font-size: 12px; font-fami=\n" +
                "ly: Arial,Helvetica, sans-serif; margin: 0; padding: 0;\">\n" +
                "                                        <font face=3D\"Arial,Helvetica, san=\n" +
                "s-serif\">                                          =20\n" +
                "                                            Wenn Sie Fragen haben, schauen=\n" +
                " Sie bitte auf unsere <a href=3D\"http://kleinanzeigen=2Eebay=2Ede/anzeigen/=\n" +
                "hilfe=2Ehtml?utm_source=3Demail&utm_medium=3Dsystem_email&utm_campaign=3Dem=\n" +
                "ail-ContactPoster&utm_content=3DOther\" title=3D\"Hilfeseiten\" target=3D\"_bla=\n" +
                "nk\" style=3D\"text-decoration: underline; color: #001e9b;\">Hilfeseiten</a> o=\n" +
                "der<br>\n" +
                "                                            kontaktieren Sie unseren <a hr=\n" +
                "ef=3D\"http://kleinanzeigen=2Eebay=2Ede/anzeigen/kontakt=2Ehtml?utm_source=\n" +
                "=3Demail&utm_medium=3Dsystem_email&utm_campaign=3Demail-ContactPoster&utm_c=\n" +
                "ontent=3DOther\" title=3D\"Kontakt\" target=3D\"_blank\" style=3D\"text-decoratio=\n" +
                "n: underline; color: #001e9b;\">Kundenservice=2E</a>\n" +
                "                                        </font>\n" +
                "                                    </p>\n" +
                "                                </td>\n" +
                "                            </tr>\n" +
                "                        </table>\n" +
                "                    </td>\n" +
                "                </tr>\n" +
                "\n" +
                "                <!-- **************************************  Footer ******=\n" +
                "***************************************************************************=\n" +
                "******************* -->\n" +
                "                <tr>\n" +
                "                    <td valign=3D\"top\" style=3D\"font-size: 12px; font-fami=\n" +
                "ly: Arial,Helvetica, sans-serif; color: #888; padding: 10px; vertical-align=\n" +
                ": top; width: 590px;\" width=3D\"590\">\n" +
                "                        <font face=3D\"Arial,Helvetica, sans-serif\" color=\n" +
                "=3D\"#888\">\n" +
                "                            Copyright &copy; 2005-2013 eBay International =\n" +
                "AG=2E Alle Rechte vorbehalten=2E<br>\n" +
                "                            Ausgewiesene Marken geh=C3=B6ren ihren jeweili=\n" +
                "gen Eigent=C3=BCmern=2E\n" +
                "                        </font>\n" +
                "\n" +
                "                    </td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                    <td valign=3D\"top\" style=3D\"font-size: 12px; border-to=\n" +
                "p-color: #ccc; font-family: Arial,Helvetica, sans-serif; color: #001e9b; pa=\n" +
                "dding: 10px; border-top-width: 1px; border-top-style: solid; vertical-align=\n" +
                ": top; width: 590px;\" width=3D\"590\">\n" +
                "                        <font face=3D\"Arial,Helvetica, sans-serif\" color=\n" +
                "=3D\"#001e9b\">\n" +
                "                            <a href=3D\"http://kleinanzeigen=2Eebay=2Ede/an=\n" +
                "zeigen/hilfe=2Ehtml?utm_source=3Demail&utm_medium=3Dsystem_email&utm_campai=\n" +
                "gn=3Demail-ContactPoster&utm_content=3DOther\" title=3D\"Hilfe\" target=3D\"_bl=\n" +
                "ank\" style=3D\"text-decoration: underline; color: #001e9b;\">Hilfe</a>&nbsp;|=\n" +
                "&nbsp;<a href=3D\"http://kleinanzeigen=2Eebay=2Ede/anzeigen/datenschutzerkla=\n" +
                "erung=2Ehtml?utm_source=3Demail&utm_medium=3Dsystem_email&utm_campaign=3Dem=\n" +
                "ail-ContactPoster&utm_content=3DOther\" title=3D\"Datenschutzerkl=C3=A4rung\" =\n" +
                "target=3D\"_blank\" style=3D\"text-decoration: underline; color: #001e9b;\">Dat=\n" +
                "enschutzerkl=C3=A4rung</a>&nbsp;|&nbsp;<a href=3D\"http://kleinanzeigen=2Eeb=\n" +
                "ay=2Ede/anzeigen/nutzungsbedingungen=2Ehtml?utm_source=3Demail&utm_medium=\n" +
                "=3Dsystem_email&utm_campaign=3Demail-ContactPoster&utm_content=3DOther\" tit=\n" +
                "le=3D\"Nutzungsbedingungen\" target=3D\"_blank\" style=3D\"text-decoration: unde=\n" +
                "rline; color: #001e9b;\">Nutzungsbedingungen</a>&nbsp;|&nbsp; <a href=3D\"htt=\n" +
                "p://kleinanzeigen=2Eebay=2Ede/anzeigen/impressum=2Ehtml?utm_source=3Demail&=\n" +
                "utm_medium=3Dsystem_email&utm_campaign=3Demail-ContactPoster&utm_content=3D=\n" +
                "Other\" title=3D\"Impressum\" target=3D\"_blank\" style=3D\"text-decoration: unde=\n" +
                "rline; color: #001e9b;\">Impressum</a>&nbsp;|&nbsp; <a href=3D\"http://presse=\n" +
                "=2Eebay=2Ede\" title=3D\"Presse\" target=3D\"_blank\" style=3D\"text-decoration: =\n" +
                "underline; color: #001e9b;\">Presse</a>&nbsp;|&nbsp; <a href=3D\"http://klein=\n" +
                "anzeigen=2Eebay=2Ede/anzeigen/kontakt=2Ehtml?utm_source=3Demail&utm_medium=\n" +
                "=3Dsystem_email&utm_campaign=3Demail-ContactPoster&utm_content=3DOther\" tit=\n" +
                "le=3D\"Kontakt\" target=3D\"_blank\" style=3D\"text-decoration: underline; color=\n" +
                ": #001e9b;\">Kontakt</a>\n" +
                "                        </font>\n" +
                "\n" +
                "                    </td>\n" +
                "                </tr>\n" +
                "\n" +
                "            </tbody>\n" +
                "        </table>\n" +
                "\n" +
                "    </body>\n" +
                "</html>\n";

        List<String> plaintextParts = rule.deliver(MailBuilder.aNewMail().from("foo@asdf.com").to("sdf.sf@asdf.com").adId("213").htmlBody("hello http://www.facebook.com")).getOutboundMail().getPlaintextParts();
        for (String plaintextPart : plaintextParts) {
            assertTrue(plaintextPart.contains("http://foo.com?url=http%3A%2F%2Fwww.facebook.com"));
        }
    }


}
