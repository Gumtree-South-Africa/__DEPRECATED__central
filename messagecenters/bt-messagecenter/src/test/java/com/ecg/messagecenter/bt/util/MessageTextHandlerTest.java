package com.ecg.messagecenter.bt.util;

import com.ecg.messagecenter.bt.util.MessageTextHandler;
import com.ecg.replyts.app.Mails;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class MessageTextHandlerTest {

    private static List<EmailTestCase> TEST_CASE_LIST;

    @BeforeClass
    public static void setup() {
        TEST_CASE_LIST = Lists.newArrayList(
                EmailTestCase.aTestCase("Testing template",
                        "Matt replied to your ad:\n" +
                                " So many birds, so little time\n" +
                                "\n" +
                                "Respond to Matt by replying directly to this email \n" +
                                "\n" +
                                " Testing template\n" +
                                "\n" +
                                "Matt\n" +
                                " Gumtree member since 2014\n" +
                                " Offered to pay with\n" +
                                "\n" +
                                "Agreed on a price? When you meet in person, ask your buyer to pay with the PayPal app. \n" +
                                "\n" +
                                " Already sold it?\n" +
                                " Stop getting emails about this ad. \n" +
                                " \n" +
                                " Manage this ad\n" +
                                "\n" +
                                "Regards, The Gumtree Team\n" +
                                "\n" +
                                "Please report any suspicious email. Never send or wire money without meeting in person (and finding out the identity of the person you're dealing with) and always follow our safety tips. If you have any questions, please look at our Help Pages.\n" +
                                "\n" +
                                "Copyright (C) 2014 eBay International AG."),
                EmailTestCase.aTestCase("GTAU-8769 on rts2",
                        "User replied to your ad:\n" +
                                "Red baby clothes2\n" +
                                "\n" +
                                "Respond to User by replying directly to this email\n" +
                                "\n" +
                                "GTAU-8769 on rts2\n" +
                                "\n" +
                                "User\n" +
                                "Offered to pay with\n" +
                                "\n" +
                                "Agreed on a price? When you meet in person, ask your buyer to pay with the PayPal app.\n" +
                                "\n" +
                                "Already sold it?\n" +
                                "Stop getting emails about this ad.\n" +
                                "\n" +
                                "Manage this ad\n" +
                                "\n" +
                                "Regards, The Gumtree Team\n" +
                                "\n" +
                                "Please report any suspicious email. Never send or wire money without meeting in person (and finding out the identity of the person you're dealing with) and always follow our safety tips. If you have any questions, please look at our Help Pages.\n" +
                                "\n" +
                                "Copyright (C) 2014 eBay International AG."),
                EmailTestCase.aTestCase("Name: Matt\n" +
                                "Phone: 0403333337\n" +
                                "Enquiry: Testing again",
                        "You have received a lead from Gumtree Australia regarding a VCAS TESTING, COLOR SHOULD BE ANY TRANSMISSION SHOULD BE ANY.\n" +
                                "\n" +
                                "------------------------------------\n" +
                                "URL: http://www.gumtree.dev/s-ad/australia/cars-vans-utes/vcas-testing-color-should-be-any-transmission-should-be-any/1414\n" +
                                "Stock#: ttggg\n" +
                                "Year: 2014\n" +
                                "Make: Suzuki\n" +
                                "Model: APV\n" +
                                "Price: 10020.00\n" +
                                "------------------------------------\n" +
                                "Name: Matt\n" +
                                "Email: mdarapour@gmail.com\n" +
                                "Phone: 0403333337\n" +
                                "Date: 31-07-2014\n" +
                                "Time: 15:43:56\n" +
                                "\n" +
                                "------------------------------------\n" +
                                "Enquiry: Testing again\n" +
                                "\n" +
                                "------------------------------------\n"),
                EmailTestCase.aTestCase("Name: sub\n" +
                                "Phone: 40004000\n" +
                                "Enquiry: don't log in",
                        "You have received a lead from Gumtree Australia regarding a 2000 Alfa Romeo 33 Convertible.\n" +
                                "\n" +
                                "------------------------------------\n" +
                                "URL: http://www.qa3.gumtree.com.au/s-ad/sydney-city/cars-vans-utes/2000-alfa-romeo-33-convertible/1060\n" +
                                "Stock#: N/A\n" +
                                "Year: 2000\n" +
                                "Make: Alfa Romeo\n" +
                                "Model: 33\n" +
                                "Price: 121221.00\n" +
                                "------------------------------------\n" +
                                "Name: sub\n" +
                                "Email: Cxin@ebay.com\n" +
                                "Phone: 40004000\n" +
                                "Date: 06-08-2014\n" +
                                "Time: 10:57:28\n" +
                                "\n" +
                                "------------------------------------\n" +
                                "Enquiry: don't log in\n" +
                                "\n" +
                                "------------------------------------"),
                EmailTestCase.aTestCase("Name: snow\n" +
                                "Postcode: 12000\n" +
                                "Phone: 40004000\n" +
                                "Enquiry: reply show up",
                        "You have received a lead from Gumtree Australia regarding a 2000 Alfa Romeo 33 Convertible.\n" +
                                "\n" +
                                "------------------------------------\n" +
                                "URL: http://www.qa3.gumtree.com.au/s-ad/australia/cars-vans-utes/2000-alfa-romeo-33-convertible/1060\n" +
                                "Stock#: N/A\n" +
                                "Year: 2000\n" +
                                "Make: Alfa Romeo\n" +
                                "Model: 33\n" +
                                "Price: 121221.00\n" +
                                "------------------------------------\n" +
                                "Name: snow\n" +
                                "Postcode: 12000\n" +
                                "Email: xuyin@ebay.com\n" +
                                "Phone: 40004000\n" +
                                "Date: 07-08-2014\n" +
                                "Time: 12:06:32\n" +
                                "\n" +
                                "------------------------------------\n" +
                                "Enquiry: reply show up\n" +
                                "\n" +
                                "------------------------------------"),
                EmailTestCase.aTestCase("Yeah, thanks for emailing me back!\n" +
                                "Better to have a call with you?",
                        "Yeah, thanks for emailing me back!\n" +
                                "\n" +
                                "Better to have a call with you?\n" +
                                "\n" +
                                "\n" +
                                "2014-08-25 11:06 GMT+08:00 Carol via Gumtree <\n" +
                                "Seller.1vwez0h4oxm29@users.gumtree.com.au>:\n" +
                                "\n" +
                                "> reply from mc of ljin2!\n" +
                                "> Hope you like it!\n" +
                                ">"),
                EmailTestCase.aTestCase("Oh yes!",
                        "Oh yes!\n\nOn 19/08/14 13:40, Matt via Gumtree wrote:\n> Gumtree Australia \t\t\n>\n>\n>" +
                                "   Matt replied to your ad:\n>   Old Bicycle\n>   " +
                                "<http://www.gumtree.dev/s-ad/sydney-city/toys-outdoor/old-bicycle/1285>\n>\n> " +
                                "Respond to Matt by replying directly to this email\n> Hi, the bike does not look too old, " +
                                "still available?\n> \t\n> Matt\n> " +
                                "Gumtree member since 2014\n> Offered to pay with\n>\n> " +
                                "Agreed on a price? When you meet in person, ask your buyer to pay with \n> " +
                                "the PayPal app.\n> *Already sold it?*\n> " +
                                "Stop getting emails about this ad.\n> " +
                                "Manage this ad <http://www.gumtree.dev/m-my-ad.html?adId=1285>\n>\n> " +
                                "Regards,\n> The Gumtree Team\n>\n> " +
                                "Please report any suspicious email. \n> " +
                                "<http://www.gumtree.dev/report-spam-email.html?raspd=1285_=_b:hz0hyn56_=_djcbywFZECKyrOgS68bbRw> \n> " +
                                "Never send or wire money without meeting in person (and finding out \n> " +
                                "the identity of the person you're dealing with) and always follow our \n> " +
                                "safety tips \n> <https://help.gumtree.com.au/knowledgebase.php?category=7>. " +
                                "If you \n> have any questions, please look at our Help Pages \n> " +
                                "<https://help.gumtree.com.au/knowledgebase.php>.\n> " +
                                "Copyright (C) 2014 eBay International AG.\n>\n\n"),
                EmailTestCase.aTestCase("Reply to the first email of the conversation which contains the template!",
                        "Reply to the first email of the conversation which contains the template!\n" +
                                "Gumtree Australia\n" +
                                "<https://help. gumtree. com. au/knowledgebase. php?category=7 <https://help. gumtree. com. au/knowledgebase. php"),
                EmailTestCase.aTestCase("Hello? Can you call me +61 3 4324 4324.",
                        "Hello? Can you call me +61 3 4324 4324.\n" +
                                "\n" +
                                "From: Carol Jin Li via Gumtree <Buyer.1qp4qjo91txze@users.gumtree.com.au<mailto:Buyer.1qp4qjo91txze@users.gumtree.com.au>>\n" +
                                "Date: Monday, August 25, 2014 at 11:03 AM\n" +
                                "To: \"Jin, Carol\" <ljin2@ebay.com<mailto:ljin2@ebay.com>>\n" +
                                "Subject: Carol Jin Li replied to your ad \"Internal test case, please do not reply\"\n" +
                                "\n" +
                                "[Gumtree Australia]\n" +
                                "Carol Jin Li replied to your ad:\n" +
                                "Internal test case, please do no...<http://www.gumtree.com.au/s-ad/millers-point/airconditioning-heating/internal-test-case-please-do-not-reply/1034059229>\n" +
                                "\n" +
                                "\n" +
                                "Respond to Carol Jin Li by replying directly to this email\n" +
                                "test 2 from gmail\n" +
                                "Carol's test case!!!!\n" +
                                "\n" +
                                "[http://gumtree.classistatic.com/latest/img/au/person-icon.png]\n" +
                                "Carol Jin Li\n" +
                                "\n" +
                                "\n" +
                                "Already sold it?\n" +
                                "Stop getting emails about this ad.\n" +
                                "Manage this ad<http://www.gumtree.com.au/m-my-ad.html?adId=1034059229>\n" +
                                "\n" +
                                "\n" +
                                "Regards,\n" +
                                "The Gumtree Team\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Please report any suspicious email.<http://www.gumtree.com.au/report-spam-email.html?raspd=1034059229_=_7qp:hz52g1v0_=_yFOiO4mEiofDp4Jma9z5xA> Never send or wire money without meeting in person (and finding out the identity of the person you&apos;re dealing with) and always follow our safety tips<https://help.gumtree.com.au/knowledgebase.php?category=7>. If you have any questions, please look at our Help Pages<https://help.gumtree.com.au/knowledgebase.php>.\n" +
                                "Copyright (C) 2014 eBay International AG."),
                EmailTestCase.aTestCase("Hi Dave! ☺",
                        "Hi Dave! ☺\n" +
                                "\n" +
                                "From: Dave via Gumtree [mailto:Seller.h49i6glw6fnk@users.gumtree.com.au]\n" +
                                "Sent: 2014年8月27日 8:45\n" +
                                "To: Jin, Carol\n" +
                                "Subject: Re: Carol replied to your ad \"Misc features for sale\"\n" +
                                "\n" +
                                "​Hi Carol​\n" +
                                "\n" +
                                "On 27 August 2014 10:43, Carol via Gumtree <Buyer.3rl9a9c3c4m3v@users.gumtree.com.au<mailto:Buyer.3rl9a9c3c4m3v@users.gumtree.com.au>> wrote:\n" +
                                "[Gumtree Australia]\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Carol replied to your ad:\n" +
                                "Misc features for sale<http://www.gumtree.com.au/s-ad/millers-point/miscellaneous-goods/misc-features-for-sale/1050196783>\n" +
                                "\n" +
                                "\n" +
                                "Respond to Carol by replying directly to this email\n" +
                                "\n" +
                                "Hi, reply from buyer on RTS2\n" +
                                "\n" +
                                "[http://gumtree.classistatic.com/latest/img/au/person-icon.png]\n" +
                                "\n" +
                                "Carol\n" +
                                "Gumtree member since 2012\n" +
                                "\n" +
                                "\n" +
                                "Already sold it?\n" +
                                "Stop getting emails about this ad.\n" +
                                "Manage this ad<http://www.gumtree.com.au/m-my-ad.html?adId=1050196783>\n" +
                                "\n" +
                                "\n" +
                                "Regards,\n" +
                                "The Gumtree Team\n" +
                                "\n" +
                                "\n" +
                                "Please report any suspicious email.<http://www.gumtree.com.au/report-spam-email.html?raspd=1050196783_=_7re:hz52g1v0_=_KqK0vYzlBq47ieG2ZQv6sw> Never send or wire money without meeting in person (and finding out the identity of the person you're dealing with) and always follow our safety tips<https://help.gumtree.com.au/knowledgebase.php?category=7>. If you have any questions, please look at our Help Pages<https://help.gumtree.com.au/knowledgebase.php>.\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Copyright (C) 2014 eBay International AG."),
                EmailTestCase.aTestCase("test case 2\n" +
                                "- From carol avenna's hotmail account!\n" +
                                "This is totally new!",
                        "avenna replied to your ad:\n" +
                                "Internal test case, please do no...\n" +
                                "\n" +
                                "Respond to avenna by replying directly to this email\n" +
                                "\n" +
                                "test case 2\n" +
                                "- From carol avenna's hotmail account!\n" +
                                "This is totally new!\n" +
                                "\n" +
                                "avenna\n" +
                                "\n" +
                                "Already sold it?\n" +
                                "Stop getting emails about this ad.\n" +
                                "\n" +
                                "Manage this ad\n" +
                                "\n" +
                                "Regards, The Gumtree Team\n" +
                                "\n" +
                                "Please report any suspicious email. Never send or wire money without meeting in person (and finding out the identity of the person you're dealing with) and always follow our safety tips. If you have any questions, please look at our Help Pages.\n" +
                                "\n" +
                                "Copyright (C) 2014 eBay International AG."),
                EmailTestCase.aTestCase("Nice one!",
                        "Nice one!\n" +
                                "\n" +
                                "From: avenna via Gumtree <Buyer.3o33y8awhnoij@users.gumtree.com.au<mailto:Buyer.3o33y8awhnoij@users.gumtree.com.au>>\n" +
                                "Date: Monday, August 25, 2014 at 3:31 PM\n" +
                                "To: \"Jin, Carol\" <ljin2@ebay.com<mailto:ljin2@ebay.com>>\n" +
                                "Subject: avenna replied to your ad \"Internal test case, please do not reply\"\n" +
                                "\n" +
                                "[Gumtree Australia]\n" +
                                "avenna replied to your ad:\n" +
                                "Internal test case, please do no...<http://www.gumtree.com.au/s-ad/millers-point/airconditioning-heating/internal-test-case-please-do-not-reply/1034059229>\n" +
                                "\n" +
                                "\n" +
                                "Respond to avenna by replying directly to this email\n" +
                                "test case 2\n" +
                                "- From carol avenna's hotmail account!\n" +
                                "This is totally new!\n" +
                                "\n" +
                                "[http://gumtree.classistatic.com/latest/img/au/person-icon.png]\n" +
                                "avenna\n" +
                                "\n" +
                                "\n" +
                                "Already sold it?\n" +
                                "Stop getting emails about this ad.\n" +
                                "Manage this ad<http://www.gumtree.com.au/m-my-ad.html?adId=1034059229>\n" +
                                "\n" +
                                "\n" +
                                "Regards,\n" +
                                "The Gumtree Team\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Please report any suspicious email.<http://www.gumtree.com.au/report-spam-email.html?raspd=1034059229_=_7qs:hz52g1v0_=_KuJKlFStCYfx0PvXOqwSJQ> Never send or wire money without meeting in person (and finding out the identity of the person you&apos;re dealing with) and always follow our safety tips<https://help.gumtree.com.au/knowledgebase.php?category=7>. If you have any questions, please look at our Help Pages<https://help.gumtree.com.au/knowledgebase.php>.\n" +
                                "Copyright (C) 2014 eBay International AG."),
                EmailTestCase.aTestCase("No problems.",
                        "No problems.\n" +
                                "________________________________________\n" +
                                "From: Carolyn Simpson via Gumtree [Buyer.85iatf1d05sp@users.gumtree.com.au]\n" +
                                "Sent: Wednesday, 27 August 2014 8:56 PM\n" +
                                "To: Miller, Kyra T\n" +
                                "Subject: Carolyn Simpson replied to your ad \"Baby Jogger City Select Bassine\n" +
                                "t Kit\"\n" +
                                "\n" +
                                "[Gumtree Australia]\n" +
                                "Carolyn Simpson replied to your ad:\n" +
                                "Baby Jogger City Select Bassinet...<http://www.gumtree.com.au/s-ad/ballarat-\n" +
                                "central/prams-strollers/baby-jogger-city-select-bassinet-kit/1052841061>\n" +
                                "\n" +
                                "\n" +
                                "Respond to Carolyn Simpson by replying directly to this email\n" +
                                "Thanks For replying Kyra but I've found another one that's easier to pick up\n" +
                                ". Good luck\n" +
                                "\n" +
                                "[http://gumtree.classistatic.com/latest/img/au/person-icon.png]\n" +
                                "Carolyn Simpson\n" +
                                "\n" +
                                "\n" +
                                "Already sold it?\n" +
                                "Stop getting emails about this ad.\n" +
                                "Manage this ad<http://www.gumtree.com.au/m-my-ad.html?adId=3D1052841061>\n" +
                                "\n" +
                                "\n" +
                                "Regards,\n" +
                                "The Gumtree Team\n" +
                                "\n" +
                                "\n" +
                                "Please report any suspicious email.<http://www.gumtree.com.au/report-spam-em\n" +
                                "ail.html?raspd=3D1052841061_=3D_92y:hz52g1v0_=3D_EjBojRBzsPDoZcDGlQC6MA> Nev\n" +
                                "er send or wire money without meeting in person (and finding out the identit\n" +
                                "y of the person you&apos;re dealing with) and always follow our safety tips<\n" +
                                "https://help.gumtree.com.au/knowledgebase.php?category=3D7>. If you have any\n" +
                                " questions, please look at our Help Pages<https://help.gumtree.com.au/knowle\n" +
                                "dgebase.php>.\n" +
                                "Copyright (C) 2014 eBay International AG.\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Important - This email and any attachments may be confidential. If received\n" +
                                " in error, please contact us and delete all copies. Before opening or using\n" +
                                " attachments check them for viruses and defects. Regardless of any loss, dam\n" +
                                "age or consequence, whether caused by the negligence of the sender or not, r\n" +
                                "esulting directly or indirectly from the use of any attached files our liabi\n" +
                                "lity is limited to resupplying any affected attachments. Any representations\n" +
                                " or opinions expressed are those of the individual sender, and not necessari\n" +
                                "ly those of the Department of Education and Early Childhood Development.\n"),
                EmailTestCase.aTestCase("Hi Shane\n" +
                                "Yes we still have the van and you are welcome to come and have a look.\n" +
                                "Thanks\n" +
                                "Michelle",
                        "Hi Shane\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Yes we still have the van and you are welcome to come and have a look.\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Thanks\n" +
                                "\n" +
                                "Michelle\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "From: shane via Gumtree \n" +
                                "[mailto:Buyer.1gvzyvqq7w3x8@users.gumtree.com.au]\n" +
                                "Sent: Wednesday, 27 August 2014 7:08 PM\n" +
                                "To: brimich2010@hotmail.com\n" +
                                "Subject: shane replied to your ad \"Coromal Family Series\"\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Image removed by sender. Gumtree Australia\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "shane replied to your ad:\n" +
                                "Coromal Family Series \n" +
                                "<http://www.gumtree.com.au/s-ad/wandi/caravan/coromal-family-series/10545\n" +
                                "70018> \n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Respond to shane by replying directly to this email\n" +
                                "\n" +
                                "\n" +
                                "do you still have like to have look\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Image removed by sender.\n" +
                                "\n" +
                                "shane\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Already sold it?\n" +
                                "Stop getting emails about this ad.\n" +
                                "\n" +
                                " <http://www.gumtree.com.au/m-my-ad.html?adId=3D1054570018> Manage this \n" +
                                "ad\n" +
                                "\n" +
                                "\n" +
                                "Regards,\n" +
                                "\n" +
                                "The Gumtree Team\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Please  \n" +
                                "<http://www.gumtree.com.au/report-spam-email.html?raspd=3D1054570018_=3D_\n" +
                                "1el:hz52fvx4_=3D_GrBu3FO-_pA94-FZYiJcpQ> report any suspicious email. \n" +
                                "Never send or wire money without meeting in person (and finding out the \n" +
                                "identity of the person you&apos;re dealing with) and always follow our  \n" +
                                "<https://help.gumtree.com.au/knowledgebase.php?category=3D7> safety \n" +
                                "tips. If you have any questions, please look at our  \n" +
                                "<https://help.gumtree.com.au/knowledgebase.php> Help Pages. Image \n" +
                                "removed by sender.\n" +
                                "\n" +
                                "\n" +
                                "Copyright (C) 2014 eBay International AG.\n" +
                                "\n" +
                                "Image removed by sender.\n" +
                                "\n" +
                                "  _____ \n" +
                                "\n" +
                                "No virus found in this message.\n" +
                                "Checked by AVG -  <http://www.avg.com> www.avg.com\n" +
                                "Version: 2014.0.4745 / Virus Database: 4007/8109 - Release Date: \n" +
                                "08/27/14\n" +
                                "\n" +
                                "\n" +
                                "------=_NextPart_001_0011_01CFC294.0C055C60\n" +
                                "Content-Type: text/html; charset=\"utf-8\"\n" +
                                "Content-Transfer-Encoding: quoted-printable\n" +
                                "\n" +
                                "<html xmlns:v=3D\"urn:schemas-microsoft-com:vml\" \n" +
                                "xmlns:o=3D\"urn:schemas-microsoft-com:office:office\" \n" +
                                "xmlns:w=3D\"urn:schemas-microsoft-com:office:word\" \n" +
                                "xmlns:m=3D\"http://schemas.microsoft.com/office/2004/12/omml\" \n" +
                                "xmlns=3D\"http://www.w3.org/TR/REC-html40\"><head><meta \n" +
                                "http-equiv=3DContent-Type content=3D\"text/html; charset=3Dutf-8\"><meta \n" +
                                "name=3DGenerator content=3D\"Microsoft Word 14 (filtered medium)\"><!--[if \n" +
                                "!mso]><style>v\\:* {behavior:url(#default#VML);}\n" +
                                "o\\:* {behavior:url(#default#VML);}\n" +
                                "w\\:* {behavior:url(#default#VML);}\n" +
                                ".shape {behavior:url(#default#VML);}\n" +
                                "</style><![endif]--><style><!--\n" +
                                "/* Font Definitions */\n" +
                                "@font-face\n" +
                                "\t{font-family:\"Cambria Math\";\n" +
                                "\tpanose-1:2 4 5 3 5 4 6 3 2 4;}\n" +
                                "@font-face\n" +
                                "\t{font-family:Calibri;\n" +
                                "\tpanose-1:2 15 5 2 2 2 4 3 2 4;}\n" +
                                "@font-face\n" +
                                "\t{font-family:Tahoma;\n" +
                                "\tpanose-1:2 11 6 4 3 5 4 4 2 4;}\n" +
                                "/* Style Definitions */\n" +
                                "p.MsoNormal, li.MsoNormal, div.MsoNormal\n" +
                                "\t{margin:0cm;\n" +
                                "\tmargin-bottom:.0001pt;\n" +
                                "\tfont-size:12.0pt;\n" +
                                "\tfont-family:\"Times New Roman\",\"serif\";}\n" +
                                "h1\n" +
                                "\t{mso-style-priority:9;\n" +
                                "\tmso-style-link:\"Heading 1 Char\";\n" +
                                "\tmso-margin-top-alt:auto;\n" +
                                "\tmargin-right:0cm;\n" +
                                "\tmso-margin-bottom-alt:auto;\n" +
                                "\tmargin-left:0cm;\n" +
                                "\tfont-size:24.0pt;\n" +
                                "\tfont-family:\"Times New Roman\",\"serif\";\n" +
                                "\tfont-weight:bold;}\n" +
                                "a:link, span.MsoHyperlink\n" +
                                "\t{mso-style-priority:99;\n" +
                                "\tcolor:#468100;\n" +
                                "\tfont-weight:bold;\n" +
                                "\ttext-decoration:underline;}\n" +
                                "a:visited, span.MsoHyperlinkFollowed\n" +
                                "\t{mso-style-priority:99;\n" +
                                "\tcolor:#325D00;\n" +
                                "\tfont-weight:bold;\n" +
                                "\ttext-decoration:underline;}\n" +
                                "p\n" +
                                "\t{mso-style-priority:99;\n" +
                                "\tmso-margin-top-alt:auto;\n" +
                                "\tmargin-right:0cm;\n" +
                                "\tmso-margin-bottom-alt:auto;\n" +
                                "\tmargin-left:0cm;\n" +
                                "\tfont-size:12.0pt;\n" +
                                "\tfont-family:\"Times New Roman\",\"serif\";}\n" +
                                "p.MsoAcetate, li.MsoAcetate, div.MsoAcetate\n" +
                                "\t{mso-style-priority:99;\n" +
                                "\tmso-style-link:\"Balloon Text Char\";\n" +
                                "\tmargin:0cm;\n" +
                                "\tmargin-bottom:.0001pt;\n" +
                                "\tfont-size:8.0pt;\n" +
                                "\tfont-family:\"Tahoma\",\"sans-serif\";}\n" +
                                "span.Heading1Char\n" +
                                "\t{mso-style-name:\"Heading 1 Char\";\n" +
                                "\tmso-style-priority:9;\n" +
                                "\tmso-style-link:\"Heading 1\";\n" +
                                "\tfont-family:\"Cambria\",\"serif\";\n" +
                                "\tcolor:#365F91;\n" +
                                "\tfont-weight:bold;}\n" +
                                "span.EmailStyle20\n" +
                                "\t{mso-style-type:personal-reply;\n" +
                                "\tfont-family:\"Calibri\",\"sans-serif\";\n" +
                                "\tcolor:#1F497D;}\n" +
                                "span.BalloonTextChar\n" +
                                "\t{mso-style-name:\"Balloon Text Char\";\n" +
                                "\tmso-style-priority:99;\n" +
                                "\tmso-style-link:\"Balloon Text\";\n" +
                                "\tfont-family:\"Tahoma\",\"sans-serif\";}\n" +
                                ".MsoChpDefault\n" +
                                "\t{mso-style-type:export-only;\n" +
                                "\tfont-size:10.0pt;}\n" +
                                "@page WordSection1\n" +
                                "\t{size:612.0pt 792.0pt;\n" +
                                "\tmargin:72.0pt 72.0pt 72.0pt 72.0pt;}\n" +
                                "div.WordSection1\n" +
                                "\t{page:WordSection1;}\n" +
                                "--></style><!--[if gte mso 9]><xml>\n" +
                                "<o:shapedefaults v:ext=3D\"edit\" spidmax=3D\"1026\" />\n" +
                                "</xml><![endif]--><!--[if gte mso 9]><xml>\n" +
                                "<o:shapelayout v:ext=3D\"edit\">\n" +
                                "<o:idmap v:ext=3D\"edit\" data=3D\"1\" />\n" +
                                "</o:shapelayout></xml><![endif]--></head><body lang=3DEN-AU \n" +
                                "link=3D\"#468100\" vlink=3D\"#325D00\"><div class=3DWordSection1><p \n" +
                                "class=3DMsoNormal><span \n" +
                                "style=3D'font-size:11.0pt;font-family:\"Calibri\",\"sans-serif\";color:#1F497\n" +
                                "D'>Hi Shane<o:p></o:p></span></p><p class=3DMsoNormal><span \n" +
                                "style=3D'font-size:11.0pt;font-family:\"Calibri\",\"sans-serif\";color:#1F497\n" +
                                "D'><o:p>&nbsp;</o:p></span></p><p class=3DMsoNormal><span \n" +
                                "style=3D'font-size:11.0pt;font-family:\"Calibri\",\"sans-serif\";color:#1F497\n" +
                                "D'>Yes we still have the van and you are welcome to come and have a \n" +
                                "look.<o:p></o:p></span></p><p class=3DMsoNormal><span \n" +
                                "style=3D'font-size:11.0pt;font-family:\"Calibri\",\"sans-serif\";color:#1F497\n" +
                                "D'><o:p>&nbsp;</o:p></span></p><div><p class=3DMsoNormal><span \n" +
                                "style=3D'font-size:11.0pt;font-family:\"Calibri\",\"sans-serif\";color:#1F497\n" +
                                "D'>Thanks<o:p></o:p></span></p><p class=3DMsoNormal><span \n" +
                                "style=3D'font-size:11.0pt;font-family:\"Calibri\",\"sans-serif\";color:#1F497\n" +
                                "D'>Michelle<o:p></o:p></span></p></div><p class=3DMsoNormal><span \n" +
                                "style=3D'font-size:11.0pt;font-family:\"Calibri\",\"sans-serif\";color:#1F497\n" +
                                "D'><o:p>&nbsp;</o:p></span></p><div><div \n" +
                                "style=3D'border:none;border-top:solid #B5C4DF 1.0pt;padding:3.0pt 0cm \n" +
                                "0cm 0cm'><p class=3DMsoNormal><b><span lang=3DEN-US \n" +
                                "style=3D'font-size:10.0pt;font-family:\"Tahoma\",\"sans-serif\"'>From:</span>\n" +
                                "</b><span lang=3DEN-US \n" +
                                "style=3D'font-size:10.0pt;font-family:\"Tahoma\",\"sans-serif\"'> shane via \n" +
                                "Gumtree [mailto:Buyer.1gvzyvqq7w3x8@users.gumtree.com.au] \n" +
                                "<br><b>Sent:</b> Wednesday, 27 August 2014 7:08 PM<br><b>To:</b> \n" +
                                "brimich2010@hotmail.com<br><b>Subject:</b> shane replied to your ad \n" +
                                "&quot;Coromal Family Series&quot;<o:p></o:p></span></p></div></div><p \n" +
                                "class=3DMsoNormal><o:p>&nbsp;</o:p></p><table class=3DMsoNormalTable \n" +
                                "border=3D0 cellspacing=3D0 cellpadding=3D0 width=3D\"100%\" \n" +
                                "style=3D'width:100.0%;background:white;border-collapse:collapse'><tr><td \n" +
                                "valign=3Dtop style=3D'padding:0cm 0cm 0cm 0cm'><div \n" +
                                "align=3Dcenter><table class=3DMsoNormalTable border=3D0 cellspacing=3D0 \n" +
                                "cellpadding=3D0 width=3D600 \n" +
                                "style=3D'width:450.0pt;background:white;border-collapse:collapse'><tr><td\n" +
                                " style=3D'background:#ECF2DA;padding:0cm 22.5pt 0cm 22.5pt'><table \n" +
                                "class=3DMsoNormalTable border=3D1 cellspacing=3D0 cellpadding=3D0 \n" +
                                "style=3D'background:#ECF2DA;border-collapse:collapse;border:none'><tr><td\n" +
                                " width=3D130 style=3D'width:97.5pt;border:none;border-bottom:solid \n" +
                                "#ECF2DA 1.0pt;padding:7.5pt 0cm 7.5pt 0cm'><p class=3DMsoNormal><span \n" +
                                "style=3D'font-family:\"Arial\",\"sans-serif\";border:solid windowtext \n" +
                                "1.0pt;padding:0cm'><img width=3D121 height=3D94 id=3D\"_x0000_i1025\" \n" +
                                "src=3D\"cid:image001.jpg@01CFC294.0BAF9B40\" alt=3D\"Image removed by \n" +
                                "sender. Gumtree Australia\"></span><span \n" +
                                "style=3D'font-family:\"Arial\",\"sans-serif\"'><o:p></o:p></span></p></td><td\n" +
                                " width=3D20 style=3D'width:15.0pt;border:none;border-bottom:solid \n" +
                                "#ECF2DA 1.0pt;padding:7.5pt 0cm 7.5pt 0cm'><p class=3DMsoNormal \n" +
                                "style=3D'line-height:0%'><span \n" +
                                "style=3D'font-size:1.0pt;font-family:\"Arial\",\"sans-serif\"'>&nbsp;<o:p></o\n" +
                                ":p></span></p></td><td width=3D390 \n" +
                                "style=3D'width:292.5pt;border:none;border-bottom:solid #ECF2DA \n" +
                                "1.0pt;padding:7.5pt 0cm 7.5pt 0cm'><h1><span \n" +
                                "style=3D'font-size:13.5pt;font-family:\"Arial\",\"sans-serif\";color:#468100'\n" +
                                ">shane replied to your ad:<br><a \n" +
                                "href=3D\"http://www.gumtree.com.au/s-ad/wandi/caravan/coromal-family-serie\n" +
                                "s/1054570018\" target=3D\"_blank\">Coromal Family Series</a> \n" +
                                "<o:p></o:p></span></h1></td></tr></table></td></tr><tr><td width=3D540 \n" +
                                "style=3D'width:405.0pt;padding:0cm 22.5pt 0cm 22.5pt'><table \n" +
                                "class=3DMsoNormalTable border=3D0 cellspacing=3D0 cellpadding=3D0 \n" +
                                "style=3D'background:white;border-collapse:collapse;border-spacing: \n" +
                                "0'><tr><td style=3D'padding:7.5pt 0cm 7.5pt 0cm'><p class=3DMsoNormal \n" +
                                "align=3Dcenter style=3D'text-align:center'><i><span \n" +
                                "style=3D'font-size:11.0pt;font-family:\"Arial\",\"sans-serif\";color:#444444'\n" +
                                ">Respond to shane by replying directly to this email \n" +
                                "<o:p></o:p></span></i></p></td></tr><tr><td width=3D540 \n" +
                                "style=3D'width:405.0pt;padding:0cm 0cm 0cm 0cm'><div \n" +
                                "style=3D'border:solid #468100 1.5pt;padding:8.0pt 8.0pt 8.0pt 8.0pt'><p \n" +
                                "class=3DMsoNormal><b><span \n" +
                                "style=3D'font-size:10.5pt;font-family:\"Arial\",\"sans-serif\";color:#333333'\n" +
                                ">do you still have like to have look \n" +
                                "<o:p></o:p></span></b></p></div></td></tr><tr><td width=3D540 \n" +
                                "style=3D'width:405.0pt;padding:7.5pt 0cm 0cm 0cm'><table \n" +
                                "class=3DMsoNormalTable border=3D0 cellspacing=3D0 cellpadding=3D0 \n" +
                                "width=3D\"100%\" style=3D'width:100.0%;border-collapse:collapse'><tr><td \n" +
                                "valign=3Dtop style=3D'padding:.75pt .75pt .75pt .75pt'><p \n" +
                                "class=3DMsoNormal><span style=3D'border:solid windowtext \n" +
                                "1.0pt;padding:0cm'><img border=3D0 width=3D46 height=3D54 \n" +
                                "id=3D\"_x0000_i1026\" src=3D\"cid:image002.jpg@01CFC294.0BAF9B40\" \n" +
                                "alt=3D\"Image removed by sender.\"></span><o:p></o:p></p></td><td \n" +
                                "width=3D490 style=3D'width:367.5pt;padding:.75pt .75pt .75pt \n" +
                                "7.5pt'><div><p class=3DMsoNormal><b><span \n" +
                                "style=3D'font-family:\"Arial\",\"sans-serif\";color:#468100'>shane</span></b>\n" +
                                "<span \n" +
                                "style=3D'font-family:\"Arial\",\"sans-serif\"'><o:p></o:p></span></p></div></\n" +
                                "td></tr></table></td></tr><tr><td width=3D540 \n" +
                                "style=3D'width:405.0pt;padding:0cm 0cm 7.5pt 0cm'><table \n" +
                                "class=3DMsoNormalTable border=3D0 cellspacing=3D0 cellpadding=3D0 \n" +
                                "width=3D\"100%\" style=3D'width:100.0%;border-collapse:collapse'><tr><td \n" +
                                "style=3D'padding:.75pt .75pt .75pt .75pt'><div \n" +
                                "style=3D'margin-top:7.5pt;min-height:50px;float:left'><div><p \n" +
                                "class=3DMsoNormal><strong><span \n" +
                                "style=3D'font-size:11.5pt;font-family:\"Arial\",\"sans-serif\";color:#468100'\n" +
                                ">Already sold it?</span></strong><span \n" +
                                "style=3D'font-size:10.5pt;font-family:\"Arial\",\"sans-serif\";color:#333333'\n" +
                                "><br>Stop getting emails about this ad. \n" +
                                "<o:p></o:p></span></p></div></div><div><p class=3DMsoNormal><span \n" +
                                "style=3D'font-size:10.5pt;font-family:\"Arial\",\"sans-serif\";color:#333333'\n" +
                                "><a href=3D\"http://www.gumtree.com.au/m-my-ad.html?adId=3D1054570018\" \n" +
                                "target=3D\"_blank\"><b><span \n" +
                                "style=3D'font-size:12.0pt;color:white;background:#478500;text-decoration:\n" +
                                "none'>Manage this ad</span></b></a> \n" +
                                "<o:p></o:p></span></p></div></td></tr></table></td></tr><tr><td \n" +
                                "width=3D540 style=3D'width:405.0pt;padding:7.5pt 0cm 7.5pt 0cm'><div \n" +
                                "id=3D\"_gaer\"><p class=3DMsoNormal><span \n" +
                                "style=3D'font-size:10.5pt;font-family:\"Arial\",\"sans-serif\";color:#333333'\n" +
                                ">Regards, <o:p></o:p></span></p><div><p class=3DMsoNormal><b><span \n" +
                                "style=3D'font-size:10.5pt;font-family:\"Arial\",\"sans-serif\";color:#468100'\n" +
                                ">The Gumtree \n" +
                                "Team<o:p></o:p></span></b></p></div></div></td></tr></table></td></tr><tr\n" +
                                "><td width=3D540 style=3D'width:405.0pt;background:#ECF2DA;padding:0cm \n" +
                                "22.5pt 0cm 22.5pt'><table class=3DMsoNormalTable border=3D0 \n" +
                                "cellspacing=3D0 cellpadding=3D0 \n" +
                                "style=3D'background:#ECF2DA;border-collapse:collapse;border-spacing: \n" +
                                "0'><tr><td width=3D540 style=3D'width:405.0pt;padding:7.5pt 0cm 7.5pt \n" +
                                "0cm'><p class=3DMsoNormal><span \n" +
                                "style=3D'font-size:10.0pt;font-family:\"Arial\",\"sans-serif\";color:#444444'\n" +
                                ">Please <a \n" +
                                "href=3D\"http://www.gumtree.com.au/report-spam-email.html?raspd=3D10545700\n" +
                                "18_=3D_1el:hz52fvx4_=3D_GrBu3FO-_pA94-FZYiJcpQ\" \n" +
                                "target=3D\"_blank\"><b>report any suspicious email.</b></a> Never send or \n" +
                                "wire money without meeting in person (and finding out the identity of \n" +
                                "the person you&amp;apos;re dealing with) and always follow our <a \n" +
                                "href=3D\"https://help.gumtree.com.au/knowledgebase.php?category=3D7\" \n" +
                                "target=3D\"_blank\"><b>safety tips</b></a>. If you have any questions, \n" +
                                "please look at our <a \n" +
                                "href=3D\"https://help.gumtree.com.au/knowledgebase.php\" \n" +
                                "target=3D\"_blank\"><b>Help Pages</b></a>. </span><span \n" +
                                "style=3D'font-size:10.0pt;color:#444444;border:solid windowtext \n" +
                                "1.0pt;padding:0cm'><img border=3D0 width=3D1 height=3D1 \n" +
                                "id=3D\"_x0000_i1027\" src=3D\"cid:image003.jpg@01CFC294.0BAF9B40\" \n" +
                                "alt=3D\"Image removed by sender.\"></span><span \n" +
                                "style=3D'font-size:10.0pt;color:#444444'><o:p></o:p></span></p></td></tr>\n" +
                                "<tr><td width=3D540 style=3D'width:405.0pt;padding:0cm 0cm 7.5pt 0cm'><p \n" +
                                "class=3DMsoNormal><span \n" +
                                "style=3D'font-size:10.0pt;font-family:\"Arial\",\"sans-serif\";color:#444444'\n" +
                                ">Copyright (C) 2014 eBay International AG. \n" +
                                "<o:p></o:p></span></p></td></tr></table></td></tr></table></div></td></tr\n" +
                                "></table><p class=3DMsoNormal><span style=3D'border:solid windowtext \n" +
                                "1.0pt;padding:0cm'><img border=3D0 width=3D1 height=3D1 \n" +
                                "id=3D\"_x0000_i1028\" src=3D\"cid:image003.jpg@01CFC294.0BAF9B40\" \n" +
                                "alt=3D\"Image removed by sender.\"></span><o:p></o:p></p><div \n" +
                                "class=3DMsoNormal align=3Dcenter style=3D'text-align:center'><hr \n" +
                                "size=3D1 width=3D\"100%\" noshade style=3D'color:#A0A0A0' \n" +
                                "align=3Dcenter></div><p class=3DMsoNormal \n" +
                                "style=3D'mso-margin-top-alt:auto;mso-margin-bottom-alt:auto'>No virus \n" +
                                "found in this message.<br>Checked by AVG - <a \n" +
                                "href=3D\"http://www.avg.com\"><b>www.avg.com</b></a><br>Version: \n" +
                                "2014.0.4745 / Virus Database: 4007/8109 - Release Date: \n" +
                                "08/27/14<o:p></o:p></p></div></body></html>"),
                EmailTestCase.aTestCase("Hi Laure  im looking for someone who can move in as soon as possible :)\n" +
                                "thank you for your enquiry :)",
                        "Hi Laure  im looking for someone who can move in as soon as possible :)\n" +
                                "thank you for your enquiry :)\n" +
                                "\n" +
                                "--- Original Message ---\n" +
                                "\n" +
                                "From: \"Laure via Gumtree\" <Buyer.1ytbulasjx1d0@users.gumtree.com.au>\n" +
                                "Sent: 27 August 2014 5:56 PM\n" +
                                "To: alex.rezaei@hotmail.com\n" +
                                "Subject: Laure replied to your ad \"FEMALE SHARE MATE WANTED FURNISHED ROOM \n" +
                                "FOR RENT IN BRISBANE CITY\"\n" +
                                "\n" +
                                "[Gumtree Australia]\n" +
                                "Laure replied to your ad:\n" +
                                "FEMALE SHARE MATE WANTED FURNISH...<http://www.gumtree.com.au/s-ad/brisbane\n" +
                                "-city/flatshare-houseshare/female-share-mate-wanted-furnished-room-for-rent\n" +
                                "-in-brisbane-city/1055293805>\n" +
                                "\n" +
                                "\n" +
                                "Respond to Laure by replying directly to this email\n" +
                                "Hi Alex\n" +
                                "I just saw your ad for the room in Brisbane City. I am a looking to move in\n" +
                                "to a new place around 13/09 when our current lease ends. I am living with m\n" +
                                "y partner now but he will be leaving Australia mid October for 8-10 mont\n" +
                                "hs so I am looking for a place mostly for myself (he will only be there \n" +
                                "one month from mid Sept to mid Oct). You don't mention in the ad if you wou\n" +
                                "ld accept couples for a short term so please let me know if this is some\n" +
                                "thing you would consider or not? What is the address?\n" +
                                "Cheers\n" +
                                "Laure\n" +
                                "\n" +
                                "[http://gumtree.classistatic.com/latest/img/au/person-icon.png]\n" +
                                "Laure\n" +
                                "\n" +
                                "\n" +
                                "Already sold it?\n" +
                                "Stop getting emails about this ad.\n" +
                                "Manage this ad<http://www.gumtree.com.au/m-my-ad.html?adId=3D1055293805>\n" +
                                "\n" +
                                "\n" +
                                "Regards\n" +
                                "The Gumtree Team\n" +
                                "\n" +
                                "\n" +
                                "Please report any suspicious email.<http://www.gumtree.com.au/report-spam-e\n" +
                                "mail.html?raspd=3D1055293805_=3D_7xe:hz52g1v0_=3D_651Ph7kPfaMs4jrUzLAnAg> N\n" +
                                "ever send or wire money without meeting in person (and finding out the iden\n" +
                                "tity of the person you&apos=3Bre dealing with) and always follow our safety\n" +
                                " tips<https://help.gumtree.com.au/knowledgebase.php?category=3D7>. If you h\n" +
                                "ave any questions please look at our Help Pages<https://help.gumtree.com\n" +
                                ".au/knowledgebase.php>.\n" +
                                "\n" +
                                "Copyright (C) 2014 eBay International AG."),
                EmailTestCase.aTestCase("Good Morning Lainie\n" +
                                "I have also had a query from another person on my phone, I am not sure \n" +
                                "if it is you or not but I still have all 3 lalaloopsy items so if you \n" +
                                "are interested, the first one in can have them.  But no sorry, I have no \n" +
                                "lalaloopsys themselves for sale.   Contact me via 0438764259.\n" +
                                "Many thanks\n" +
                                "Karen.",
                        "Good Morning Lainie\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "I have also had a query from another person on my phone, I am not sure \n" +
                                "if it is you or not but I still have all 3 lalaloopsy items so if you \n" +
                                "are interested, the first one in can have them.  But no sorry, I have no \n" +
                                "lalaloopsys themselves for sale.   Contact me via 0438764259.\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Many thanks\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Karen.\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "From: Lainie via Gumtree \n" +
                                "[mailto:Buyer.22dloc03kmnm2@users.gumtree.com.au]\n" +
                                "Sent: Wednesday, August 27, 2014 10:51 PM\n" +
                                "To: amitic6@bigpond.com\n" +
                                "Subject: Lainie replied to your ad \"LALALOOPSY TREEHOUSE\"\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Image removed by sender. Gumtree Australia\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Lainie replied to your ad:\n" +
                                "LALALOOPSY TREEHOUSE \n" +
                                "<http://www.gumtree.com.au/s-ad/wavell-heights/miscellaneous-goods/lalalo\n" +
                                "opsy-treehouse/1055315765> \n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Respond to Lainie by replying directly to this email\n" +
                                "\n" +
                                "\n" +
                                "Hi,\n" +
                                "I would like to buy your treehouse and doll house, if they are still \n" +
                                "available - also just wondering if you have any mini lalaloopsys you are \n" +
                                "thinking of selling?\n" +
                                "This is my first time buying from gumtree, so I'm not really sure what \n" +
                                "the process is - am I meant to do something else, or do I just wait to \n" +
                                "hear back from you?\n" +
                                "Thanks\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Image removed by sender.\n" +
                                "\n" +
                                "Lainie\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Already sold it?\n" +
                                "Stop getting emails about this ad.\n" +
                                "\n" +
                                " <http://www.gumtree.com.au/m-my-ad.html?adId=3D1055315765> Manage this \n" +
                                "ad\n" +
                                "\n" +
                                "\n" +
                                "Regards,\n" +
                                "\n" +
                                "The Gumtree Team\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Please  \n" +
                                "<http://www.gumtree.com.au/report-spam-email.html?raspd=3D1055315765_=3D_\n" +
                                "9u1:hz52g1v0_=3D_aJq6lxriRvGmDaUgQ6Og-w> report any suspicious email. \n" +
                                "Never send or wire money without meeting in person (and finding out the \n" +
                                "identity of the person you&apos;re dealing with) and always follow our  \n" +
                                "<https://help.gumtree.com.au/knowledgebase.php?category=3D7> safety \n" +
                                "tips. If you have any questions, please look at our  \n" +
                                "<https://help.gumtree.com.au/knowledgebase.php> Help Pages. Image \n" +
                                "removed by sender.\n" +
                                "\n" +
                                "\n" +
                                "Copyright (C) 2014 eBay International AG.\n" +
                                "\n" +
                                "Image removed by sender.\n"),
                EmailTestCase.aTestCase("Yeah mate still got it ?",
                        "Yeah mate still got it ?\n" +
                                "\n" +
                                "--- Original Message ---\n" +
                                "\n" +
                                "From: \"Matt via Gumtree\" <Buyer.20ons3bjkuk8p@users.gumtree.com.au>\n" +
                                "Sent: 27 August 2014 7:14 pm\n" +
                                "To: emanuel_oala1687@hotmail.com\n" +
                                "Subject: Matt replied to your ad \"1:18 scale model BMW M6\"\n" +
                                "\n" +
                                "[Gumtree Australia]\n" +
                                "Matt replied to your ad:\n" +
                                "1:18 scale model BMW M6<http://www.gumtree.com.au/s-ad/marsden/collectables\n" +
                                "/1-18-scale-model-bmw-m6/1054967518>\n" +
                                "\n" +
                                "\n" +
                                "Respond to Matt by replying directly to this email\n" +
                                "Hey you still got this car for sale I live in Brisbane so I can pick it \n" +
                                "up.\n" +
                                "\n" +
                                "Thanks Matt\n" +
                                "\n" +
                                "[http://gumtree.classistatic.com/latest/img/au/person-icon.png]\n" +
                                "Matt\n" +
                                "Gumtree member since 2014\n" +
                                "\n" +
                                "\n" +
                                "Already sold it?\n" +
                                "Stop getting emails about this ad.\n" +
                                "Manage this ad<http://www.gumtree.com.au/m-my-ad.html?adId=3D1054967518>\n" +
                                "\n" +
                                "\n" +
                                "Regards\n" +
                                "The Gumtree Team\n" +
                                "\n" +
                                "\n" +
                                "Please report any suspicious email.<http://www.gumtree.com.au/report-spam-e\n" +
                                "mail.html?raspd=3D1054967518_=3D_m8:hz52fvx4_=3D_mXoXo6_Oj_WUSaD5yPWXog> Ne\n" +
                                "ver send or wire money without meeting in person (and finding out the ident\n" +
                                "ity of the person you&apos=3Bre dealing with) and always follow our safety \n" +
                                "tips<https://help.gumtree.com.au/knowledgebase.php?category=3D7>. If you ha\n" +
                                "ve any questions please look at our Help Pages<https://help.gumtree.com.\n" +
                                "au/knowledgebase.php>.\n" +
                                "\n" +
                                "Copyright (C) 2014 eBay International AG.\n"),
                EmailTestCase.aTestCase("Thank you for your enquiry regarding the Starcraft 19.61-2 ,as we do \n" +
                                "not like to discuss prices of new vans over the internet if you could \n" +
                                "supply me your best contact details and a convenient time to call I \n" +
                                "would be more than happy to have someone call you to discuss price on \n" +
                                "this caravan  .\n" +
                                "I look forward to hearing from you\n" +
                                "Regards,\n" +
                                "Kellie Gibbs\n" +
                                "Jayco Griffith\n" +
                                "94 Willandra Ave\n" +
                                "Griffith NSW 2680\n" +
                                "Ph: 02 69642611\n" +
                                "Fax: 02 69640052",

                        "Thank you for your enquiry regarding the Starcraft 19.61-2 ,as we do \n" +
                                "not like to discuss prices of new vans over the internet if you could \n" +
                                "supply me your best contact details and a convenient time to call I \n" +
                                "would be more than happy to have someone call you to discuss price on \n" +
                                "this caravan  .\n" +
                                "\n" +
                                "I look forward to hearing from you\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Regards,\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Kellie Gibbs\n" +
                                "\n" +
                                "Jayco Griffith\n" +
                                "\n" +
                                "94 Willandra Ave\n" +
                                "\n" +
                                "Griffith NSW 2680\n" +
                                "\n" +
                                "Ph: 02 69642611\n" +
                                "\n" +
                                "Fax: 02 69640052\n" +
                                "\n" +
                                "Email:  <mailto:info@jaycogriffith.com.au> info@jaycogriffith.com.au\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "From: Les Saddler via Gumtree \n" +
                                "[mailto:Buyer.3lwomh4a04cz4@users.gumtree.com.au]\n" +
                                "Sent: Wednesday, 27 August 2014 7:39 PM\n" +
                                "To: info@jaycogriffith.com.au\n" +
                                "Subject: Les Saddler replied to your ad \"2014 JAYCO STARCRAFT 19.61-2\"\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Les Saddler replied to your ad:\n" +
                                "2014 JAYCO STARCRAFT 19.61-2 \n" +
                                "<http://www.gumtree.com.au/s-ad/griffith/caravan/2014-jayco-starcraft-19-\n" +
                                "61-2/1054886023> \n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Respond to Les Saddler by replying directly to this email\n" +
                                "\n" +
                                "\n" +
                                "Hello Can you please give me a ball park figure for this caravan?\n" +
                                "I will be in Griffith next week.\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Les Saddler\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Already sold it?\n" +
                                "Stop getting emails about this ad.\n" +
                                "\n" +
                                " <http://www.gumtree.com.au/m-my-ad.html?adId=3D1054886023> Manage this \n" +
                                "ad\n" +
                                "\n" +
                                "\n" +
                                "Regards,\n" +
                                "\n" +
                                "The Gumtree Team\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Please  \n" +
                                "<http://www.gumtree.com.au/report-spam-email.html?raspd=3D1054886023_=3D_\n" +
                                "rt:hz52fvx4_=3D_Jm8QL0lpluhdl_2_LErn4g> report any suspicious email. \n" +
                                "Never send or wire money without meeting in person (and finding out the \n" +
                                "identity of the person you&apos;re dealing with) and always follow our  \n" +
                                "<https://help.gumtree.com.au/knowledgebase.php?category=3D7> safety \n" +
                                "tips. If you have any questions, please look at our  \n" +
                                "<https://help.gumtree.com.au/knowledgebase.php> Help Pages.\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Copyright (C) 2014 eBay International AG.\n"),
                EmailTestCase.aTestCase("Hello Ralph\n" +
                                "We know it is the head gasket and not a corroded head because we had a \n" +
                                "roadworthy check on it (although not mandatory in SA) prior to putting \n" +
                                "it up on Gumtree.  We drove it to Kingston and back recently without any \n" +
                                "issues so I think you would be able to drive it to Adelaide (especially \n" +
                                "with your knowledge and skills).\n" +
                                "Attached are some photos of the bodywork. As mentioned in the ad, there \n" +
                                "are some rust spots and it could do with a little TLC!!!\n" +
                                "Feel free to call me on 0418829347 if you would like to discuss further.\n" +
                                "Cheers\n" +
                                "Ray",

                        "Hello Ralph\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "We know it is the head gasket and not a corroded head because we had a \n" +
                                "roadworthy check on it (although not mandatory in SA) prior to putting \n" +
                                "it up on Gumtree.  We drove it to Kingston and back recently without any \n" +
                                "issues so I think you would be able to drive it to Adelaide (especially \n" +
                                "with your knowledge and skills).\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Attached are some photos of the bodywork. As mentioned in the ad, there \n" +
                                "are some rust spots and it could do with a little TLC!!!\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Feel free to call me on 0418829347 if you would like to discuss further.\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Cheers\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Ray\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "From: Ralph Mcevoy via Gumtree \n" +
                                "[mailto:Buyer.3dun1xpzanih4@users.gumtree.com.au]\n" +
                                "Sent: Wednesday, 27 August 2014 5:37 PM\n" +
                                "To: rajamey1@dodo.com.au\n" +
                                "Subject: Ralph Mcevoy replied to your ad \"1988 Nissan Urvan Campervan\"\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                " Gumtree Australia \n" +
                                "<http://gumtree.classistatic.com/latest/img/au/gt_logo.png>\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Ralph Mcevoy replied to your ad:\n" +
                                "1988 Nissan Urvan Campervan \n" +
                                "<http://www.gumtree.com.au/s-ad/naracoorte/campervan/1988-nissan-urvan-ca\n" +
                                "mpervan/1054778879> \n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Respond to Ralph Mcevoy by replying directly to this email\n" +
                                "\n" +
                                "\n" +
                                "Hello, i am Ralph Mcevoy in Adelaide and a retired mechanic, we are very \n" +
                                "interested in your campervan, the head gasket is no problem for me even \n" +
                                "if it is something more serious like a corroded head, my only concern is \n" +
                                "whether it can be driven back home, is it using coolant to any great \n" +
                                "extent that would make the journey too hard, if so i would need to make \n" +
                                "other arrangements to get it back. yours Ralph\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "  <http://gumtree.classistatic.com/latest/img/au/person-icon.png>\n" +
                                "\n" +
                                "Ralph Mcevoy\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Already sold it?\n" +
                                "Stop getting emails about this ad.\n" +
                                "\n" +
                                " <http://www.gumtree.com.au/m-my-ad.html?adId=3D1054778879> Manage this \n" +
                                "ad\n" +
                                "\n" +
                                "\n" +
                                "Regards,\n" +
                                "\n" +
                                "The Gumtree Team\n" +
                                "\n" +
                                "\n" +
                                "\n" +
                                "Please  \n" +
                                "<http://www.gumtree.com.au/report-spam-email.html?raspd=3D1054778879_=3D_\n" +
                                "93:hz52fvx4_=3D_qZAxATDrRRKrOiiP67YPow> report any suspicious email. \n" +
                                "Never send or wire money without meeting in person (and finding out the \n" +
                                "identity of the person you&apos;re dealing with) and always follow our  \n" +
                                "<https://help.gumtree.com.au/knowledgebase.php?category=3D7> safety \n" +
                                "tips. If you have any questions, please look at our  \n" +
                                "<https://help.gumtree.com.au/knowledgebase.php> Help Pages.  Web Bug \n" +
                                "from \n" +
                                "http://www.google-analytics.com/__utm.gif?utmac=3DUA-24548418-1&utmt=3Dev\n" +
                                "ent&utme=3D5(REPLY*SELLER_VIEW*automotive__caravan_campervan__campervan)&\n" +
                                "utmcc=3D__utma%3D999.999.999.999.999.1%3B&utmn=3D2048464220&utmr=3D-&utmp\n" +
                                "=3D/&utmwv=3D5.4.3 <http://www.mailscanner.tv/1x1spacer.gif>\n" +
                                "\n" +
                                "\n" +
                                "Copyright (C) 2014 eBay International AG.\n" +
                                "\n" +
                                " Web Bug from \n" +
                                "https://giamx8r4.emltrk.com/giamx8r4?d=3D[dkwasY0SL01QPfrW_dnW4g] \n" +
                                "<http://www.mailscanner.tv/1x1spacer.gif>\n")
        );
    }

    private static final String AUTOGATE_LEAD = "<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?><lead schemaVersion=\\\"1.0\\\" xmlns=\\\"http://dataconnect.carsales.com.au/schema/\\\"  xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\"  xsi:schemaLocation=\\\"http://dataconnect.carsales.com.au/schema/ https://testdataconnect.carsales.com.au/lead/lead.xml\\\"><sourceID>1054</sourceID><leadType>BNCIS</leadType><requestType>Dealer</requestType><sourceDealerID>54321</sourceDealerID><dealerName>Hugo&apos;s Half Price Cars</dealerName><comments>Hi</comments><prospect><firstName>Someone</firstName><email>isader@ebay.com</email><homePhone>0403333337</homePhone></prospect><itemInfo><stockNumber>W12955</stockNumber><price>19981.00</price><colour>White</colour><description>Gumtree sales lead for &quot;New Suzuki Jimny Sierra via BU&quot; - http://www.gumtree.dev/s-ad/australia/cars-vans-utes/new-suzuki-jimny-sierra-via-bu/1174</description><itemDetails><carInfo><makeName>Ford</makeName><modelName>Falcon</modelName><releaseYear>2014</releaseYear><bodyStyle>Ute</bodyStyle><transmission>Automatic</transmission><kilometres>9938</kilometres><rego>CJE13J</rego></carInfo></itemDetails></itemInfo></lead>";

    @Test
    public void extractsTextMessage() {
        long start, end, duration;
        for (int i = 0; i < TEST_CASE_LIST.size(); i++) {
            start = System.currentTimeMillis();
            String result = MessageTextHandler.remove(TEST_CASE_LIST.get(i).actual);
            end = System.currentTimeMillis();
            duration = end - start;
            assertEquals("Index: " + i, TEST_CASE_LIST.get(i).expected, result);
            assertTrue("Long running regex, Length: " + result.length() + " Time: " + duration + " Index: " + i, duration < 30 * 10);
        }
    }

    @Test
    public void trueXml() {
        assertTrue(MessageTextHandler.isXml(AUTOGATE_LEAD));
    }

    @Test
    public void falseXml() {
        assertFalse(MessageTextHandler.isXml(null));
        assertFalse(MessageTextHandler.isXml(" "));
    }

    static class EmailTestCase {
        String expected;
        String actual;

        EmailTestCase(String expected, String actual) {
            this.expected = expected;
            this.actual = actual;
        }

        static EmailTestCase aTestCase(String expected, String actual) {
            return new EmailTestCase(expected, actual);
        }
    }

    private File[] getFiles(File folder, String extension) {
        for (int i = 0; i < 20; i++) {
            File[] mails = folder.listFiles((dir, name) -> name.endsWith(extension));
            if (mails != null) {
                return mails;
            }
        }
        throw new IllegalStateException("Could not load test files with extension " + extension + " from folder " + folder);
    }

    @Test
    public void testValidMails() throws Exception {
        File mailFolder = new File(getClass().getResource("/mailReceived").getFile());
        Map<String, String> expected = Maps.newHashMap();
        FileInputStream fin;
        long start, end, duration;
        String fileName, result;

        File[] mails = getFiles(mailFolder, "eml");
        File[] texts = getFiles(mailFolder, "txt");
        for (File text : texts) {
            expected.put(fileName(text.getName()), IOUtils.toString(new FileInputStream(text)));
        }

        for (File f : mails) {
            fin = new FileInputStream(f);
            fileName = fileName(f.getName());
            try {
                List<String> parts = Mails.readMail(ByteStreams.toByteArray(fin)).getPlaintextParts();
                start = System.currentTimeMillis();
                result = MessageTextHandler.remove(parts.get(0));
                end = System.currentTimeMillis();
                duration = end - start;
                assertTrue("Long running regex, Length: " + result.length() + " Time: " + duration + " File:[" + fileName + "]", duration < 30 * 10);
                if (expected.containsKey(fileName)) {
                    assertEqualsIgnoreLineEnding("File '" + f.getAbsolutePath() + "'", expected.get(fileName), result);
                }
            } catch (Exception e) {
                throw new RuntimeException("Mail " + f.getAbsolutePath() + " not parseable", e);
            } finally {
                fin.close();
            }
        }
    }

    private static String fileName(String fileName) {
        return fileName.replaceAll("\\..*", "");
    }

    private static void assertEqualsIgnoreLineEnding(String message, String expected, String actual) {
        assertEquals(message, expected.replaceAll("\r\n", "\n"), actual.replaceAll("\r\n", "\n"));
    }
}
