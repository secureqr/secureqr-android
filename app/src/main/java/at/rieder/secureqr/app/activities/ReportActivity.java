package at.rieder.secureqr.app.activities;

import android.app.ListActivity;
import android.os.Bundle;

import java.util.Collection;

import at.rieder.secureqr.app.managers.ComponentAccessor;
import at.rieder.secureqr.app.model.CheckResult;

/**
 * Created by Thomas on 25.03.14.
 */
public class ReportActivity extends ListActivity {

    private static final String TAG = ReportActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Collection<CheckResult> collection = ComponentAccessor.getInstance().getHistoryManager().getMostRecentCheckResults();
        CheckResult[] values = new CheckResult[collection.size()];
        values = collection.toArray(values);

        // use your custom layout
        CheckResultAdapter adapter = new CheckResultAdapter(this, values);
        setListAdapter(adapter);
    }
}
