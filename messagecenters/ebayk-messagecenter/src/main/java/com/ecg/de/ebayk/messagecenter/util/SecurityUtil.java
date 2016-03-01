package com.ecg.de.ebayk.messagecenter.util;

import com.google.common.base.Strings;

import java.util.HashMap;
import java.util.Map;

public final class SecurityUtil {

    //this is a map of characters that need to for example avoid an XSS attack
    private static final Map<Character, String> XSS_CHARACTERS = new HashMap<Character, String>();

    static {
        //add more as needed
        XSS_CHARACTERS.put('<', "&lt;");
        XSS_CHARACTERS.put('>', "&gt;");
        XSS_CHARACTERS.put('"', "&quot;");
        XSS_CHARACTERS.put('\'', "&#x27;");
        XSS_CHARACTERS.put('/', "&#x2F;");
    }

    private SecurityUtil() {
    }

    /**
     * Escaping string to protect against XSS attacks,
     * If input is null it returns empty string, this is more NPE robust against further string-operations.
     * Exception: It does not escape '&' chars.
     *
     * @see <a href="http://www.owasp.org/index.php/XSS_%28Cross_Site_Scripting%29_Prevention_Cheat_Sheet#RULE_.231_-_HTML_Escape_Before_Inserting_Untrusted_Data_into_HTML_Element_Content"}
     */
    public static String xssEscape(String input) {
        if (Strings.isNullOrEmpty(input)) {
            return input;
        }
        //new impl to avoid costly String.replace
        String output = input;
        boolean needsMasking = false;
        if (input != null) {
            char[] chars = input.toCharArray();
            for (int i = 0; i < chars.length && !needsMasking; i++) {
                needsMasking = XSS_CHARACTERS.containsKey(chars[i]);
            }
        }
        if (needsMasking) {
            char[] chars = input.toCharArray();
            StringBuilder stringBuilder = new StringBuilder();
            for (char c : chars) {
                if (XSS_CHARACTERS.containsKey(c)) {
                    stringBuilder.append(XSS_CHARACTERS.get(c));
                } else {
                    stringBuilder.append(c);
                }
            }
            output = stringBuilder.toString();
        }
        return output == null ? "" : output;
    }


}
