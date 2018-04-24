package com.ecg.messagecenter.kjca.cleanup;

import java.util.regex.Pattern;

public class CleanupKijijiTemplateAdvice extends AbstractCleanupAdvice {
    private static final Pattern[][] TEMPLATES = {
            new Pattern[]{
                    Pattern.compile("Hello! The following is a reply to your .+( Ad)? on Kijiji:"),
                    Pattern.compile("(From|Phone):.*")
            },
            new Pattern[]{
                    Pattern.compile("(You can respond to .+ by replying to this email\\.)|(Reply to .+)"),
                    Pattern.compile("Operated by: Marktplaats B.V Wibautstraat 224-2 Amsterdam 1097 DN Netherlands")
            },
            new Pattern[]{
                    Pattern.compile("Bonjour! Ceci est une réponse à votre (annonce|conversation au sujet de) .+( affichée)? sur Kijiji :"),
                    Pattern.compile("(Expéditeur|Téléphone) :.*")
            },
            new Pattern[]{
                    Pattern.compile("(Vous pouvez répondre à .+ à partir de ce courriel\\.)|(Répondre à .+)"),
                    Pattern.compile("Exploité par: Marktplaats B.V | Wibautstraat 224-2 | Amsterdam | 1097 DN | Netherlands")
            }};

    private static final int START = 0;
    private static final int END = 1;


    protected CleanupKijijiTemplateAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {
        boolean finding = false;
        for (Text.Line line : text.lines) {
            if (!finding && (text.getAdvice().isLineCleaned(line.originalIndex) ||
                    text.getAdvice().isLineQuoted(line.originalIndex))) {
                continue; // Ignore already cleaned lines
            }

            for (Pattern[] template : TEMPLATES) {
                finding = markQuotedFromStartQuotedLine(template, line, finding);
            }
        }
    }

    private boolean markQuotedFromStartQuotedLine(Pattern[] patterns, Text.Line line, boolean finding) {
        if(patterns[START].matcher(line.content).matches()) {
            markQuoted(line.originalIndex);
            return true;
        }
        if (patterns[END].matcher(line.content).matches()) {
            markQuoted(line.originalIndex);
            return false;
        }

        if(finding) {
            markQuoted(line.originalIndex);
        }

        return finding;
    }
}
