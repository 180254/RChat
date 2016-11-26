package pl.nn44.rchat.client.util;

import java.util.Comparator;
import java.util.List;

public class ListUtil {

    public static <T> void sortedAdd(List<T> list, T val, Comparator<T> cmp) {

        if (list.size() == 0) {
            list.add(val);

        } else if (cmp.compare(list.get(0), val) > 0) {
            list.add(0, val);

        } else if (cmp.compare(list.get(list.size() - 1), val) < 0) {
            list.add(list.size(), val);

        } else {
            int i = 0;
            while (cmp.compare(list.get(i), val) < 0) {
                i++;
            }

            list.add(i, val);
        }
    }
}
