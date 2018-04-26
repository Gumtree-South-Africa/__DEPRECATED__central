package com.ecg.messagecenter.it.cleanup;

import com.ecg.messagecenter.it.cleanup.TextCleaner;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by jaludden on 29/01/16.
 */
public class KijijiTextCleanerIssuesTest {

    @Test public void test1() {
        String uncleanedMessage = "Ma mi stai dicendo che funziona davvero? \r\n" + "  \r\n"
                        + "Da: \"inserzionista-1bvjjoujqj94q@kijiji.annunci.it<mailto:inserzionista-1bvjjoujqj94q@kijiji.annunci.it>\" <inserzionista-1bvjjoujqj94q@kijiji.annunci.it<mailto:inserzionista-1bvjjoujqj94q@kijiji.annunci.it>>  \r\n"
                        + "Data: giovedì 28 gennaio 2016 21:38  \r\n"
                        + "A: Ivan Coppa <icoppa@ebay.com<mailto:icoppa@ebay.com>>  \r\n"
                        + "Oggetto: Re: Risposta a \"Lettore cd rotto\"  \r\n" + "  \r\n"
                        + "Il 28 gen 2016 8:54 PM, \"Ivan\" <<mail>utente-km9ymwzhdlb3@kijiji.annunci.it</a>>%20ha%20scritto:<br%20type=>  \r\n"
                        + "  \r\n" + "[Ciao, un utente  è interessato al tuo annuncio \\]  \r\n"
                        + "  \r\n" + "Ivan ha risposto al tuo annuncio \r\n" + " \r\n" + " \r\n"
                        + " \r\n" + " \r\n" + " \r\n" + " \r\n" + " \r\n" + "Ciao, \r\n" + " \r\n"
                        + "Ivan è interessato al tuo annuncio \"Lettore cd rotto\". Ecco il suo messaggio: \r\n"
                        + " \r\n" + " \r\n" + "Oh dai me lo vendi? ne ho proprio bisogno! \r\n"
                        + " \r\n" + " \r\n"
                        + "Per rispondere a questo messaggio usa il tasto Rispondi del tuo programma di posta \r\n"
                        + " \r\n" + " \r\n" + "Lo hai già venduto? \r\n"
                        + "Per non ricevere più email cancella questo annuncio dalla tua area I miei annunci<http://www.kijiji.it/miei-annunci> \r\n"
                        + " \r\n" + " \r\n"
                        + "Per tutelare ancor più la tua privacy, mascheriamo l'indirizzo email di chi si scambia messaggi in risposta agli annunci pubblicati sul sito. Lo facciamo per aumentare la tua sicurezza ed evitare che ti arrivino messaggi di spam. Maggiori informazioni<http://www.kijiji.it/aiuto/default/email>. \r\n"
                        + " \r\n" + " \r\n"
                        + "Rispondendo a questa mail contatterai l'utente direttamente. \r\n"
                        + " \r\n" + "Per vedere l'annuncio clicca sul link qui sotto: \r\n"
                        + "http://www.kijiji.it/post/91457025<http://www.kijiji.it/post/91457025?utm_source=systememail&utm_medium=core&utm_campaign=reply> \r\n"
                        + " \r\n"
                        + "Se hai problemi ad accedere, copia e incolla i link nella barra di navigazione. \r\n"
                        + " \r\n" + " \r\n" + "Il team di Kijiji \r\n" + " \r\n" + " \r\n" + " \r\n"
                        + " \r\n" + " \r\n" + " \r\n"
                        + "[Scarica l'applicazione per iPhone]<https://app.adjust.com/nfhqnm>      [Scarica l'applicazione per Android] <https://app.adjust.com/nfhqnm>    Scarica l'app \r\n"
                        + "Seguici su      [Seguici su Facebook] <http://www.facebook.com/207404152618996>         [Seguici su Twitter] <https://twitter.com/kijiji_it>    [Seguici su Youtube] <http://www.youtube.com/kijijitalia> \r\n"
                        + " \r\n" + " \r\n" + " \r\n"
                        + "Copyright © 2016 eBay International AG. Tutti i diritti riservati. I marchi registrati ed i segni distintivi sono di proprietà dei rispettivi titolari. L'uso di questo sito web implica l'accettazione delle Condizioni d'uso<http://www.kijiji.it/aiuto/default/condizioni> e delle Regole sulla privacy<http://www.kijiji.it/aiuto/default/privacy> di Marktplaats BV. \r\n"
                        + " \r\n" + " \r\n" + " \r\n"
                        + "<mail>utente-km9ymwzhdlb3@kijiji.annunci.it</a>>%20ha%20scritto:<br%20type=> \r\n";

        assertThat(new TextCleaner.KijijiTextCleaner().internalCleanupText(uncleanedMessage),
                        is("Ma mi stai dicendo che funziona davvero?"));
    }
}
