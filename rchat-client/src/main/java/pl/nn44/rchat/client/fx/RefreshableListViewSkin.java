package pl.nn44.rchat.client.fx;

import com.sun.javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.ListView;

/**
 * <pre>
 *     ListViewSkin with refresh method to update JavaFx ListView without changing collection.
 *     http://stackoverflow.com/a/25962110
 * </pre>
 *
 * @param <T>
 */
public class RefreshableListViewSkin<T> extends ListViewSkin<T> {

    public RefreshableListViewSkin(ListView<T> listView) {
        super(listView);
    }

    public void refresh() {
        super.flow.recreateCells();
    }
}
