package at.rieder.secureqr.app.activities;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Collection;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.managers.ClipboardInterface;
import at.rieder.secureqr.app.managers.ComponentAccessor;
import at.rieder.secureqr.app.model.Content;
import at.rieder.secureqr.app.model.HistoryItem;

/**
 * Created by Thomas on 25.03.14.
 */
public class HistoryActivity extends ListActivity {

    private static final String TAG = HistoryActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Collection<HistoryItem> collection = ComponentAccessor.getInstance().getHistoryManager().getHistory();

        HistoryItem[] values = new HistoryItem[collection.size()];
        values = collection.toArray(values);

        // use your custom layout
        HistoryItemAdapter adapter = new HistoryItemAdapter(this, values);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        HistoryItem historyItem = ((HistoryItem) getListAdapter().getItem(position));
        Log.d(TAG, "the clipboard text is: " + historyItem.getContent().toString());
        ClipboardInterface.setText(historyItem.getContent().toString(), this.getApplicationContext());
        Toast.makeText(this, getString(R.string.msg_copied_item_to_clipboard), Toast.LENGTH_LONG).show();
    }
}
