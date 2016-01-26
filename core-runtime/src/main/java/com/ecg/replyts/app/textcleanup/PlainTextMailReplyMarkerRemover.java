package com.ecg.replyts.app.textcleanup;

/**
 * Remove reply marker used by mail programs to mark reply.
 */
class PlainTextMailReplyMarkerRemover {

    public String remove(final String plainTextMail) {
        // this version of remover was inspired by DiffMathPatch.class from google lib.
        int lineStart = 0;
        int lineEnd = -1;
        String line;
        StringBuilder chars = new StringBuilder();
        // Walk the text, pulling out a substring for each line.
        // text.split('\n') would would temporarily double our memory footprint.
        // Modifying text would create many large strings to garbage collect.
        while (lineEnd < plainTextMail.length() - 1) {
            lineEnd = plainTextMail.indexOf('\n', lineStart);
            if (lineEnd == -1) {
                lineEnd = plainTextMail.length() - 1;
            }
            line = plainTextMail.substring(lineStart, lineEnd + 1);
            lineStart = lineEnd + 1;

            chars.append(processLine(line));
        }
        return chars.toString();
    }

    private String processLine(final String line) {
        if (line.startsWith(">"))
            return processLine(line.substring(1));
        else
            return line;
    }
}
