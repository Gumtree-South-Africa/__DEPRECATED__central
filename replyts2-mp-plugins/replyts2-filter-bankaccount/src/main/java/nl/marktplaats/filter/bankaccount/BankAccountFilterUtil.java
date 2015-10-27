package nl.marktplaats.filter.bankaccount;

/**
 * Created by reweber on 19/10/15
 */
public class BankAccountFilterUtil {

    public static <T> T coalesce(T... ts) {
        for (T t : ts) {
            if (t != null) return t;
        }
        return null;
    }
}
