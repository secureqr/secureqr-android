package at.rieder.secureqr.app.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.model.CheckResult;
import at.rieder.secureqr.app.model.HistoryItem;

/**
 * Created by Thomas on 25.03.14.
 */
public class HistoryItemAdapter extends ArrayAdapter<HistoryItem> {

    private Context context;
    private HistoryItem[] values;

    public HistoryItemAdapter(Context context, HistoryItem[] values) {
        super(context, R.layout.checkadapter, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.checkadapter, parent, false);

        TextView headerView = (TextView) rowView.findViewById(R.id.header);
        TextView descriptionView = (TextView) rowView.findViewById(R.id.description);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

        if (values[position] != null) {
            if (values[position].getDate() != null) {
                descriptionView.setText(values[position].getDate().toString());
            }

            if(values[position].getContent() != null) {
                headerView.setText(values[position].getContent().toString());
            }
        }

        if (values[position] != null && !wasMalicious(values[position])) {
            // it was successful
            imageView.setImageResource(R.drawable.ok);
        } else {
            // it failed
            imageView.setImageResource(R.drawable.error);
        }

        return rowView;
    }

    private boolean wasMalicious(HistoryItem historyItem) {
        for (CheckResult checkResult : historyItem.getCheckResult()) {
            if (!checkResult.getSuccessful()) {
                return true;
            }
        }
        return false;
    }

}
