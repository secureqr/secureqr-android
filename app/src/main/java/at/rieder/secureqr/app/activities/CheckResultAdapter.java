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

/**
 * Created by Thomas on 25.03.14.
 */
public class CheckResultAdapter extends ArrayAdapter<CheckResult> {

    private Context context;
    private CheckResult[] values;

    public CheckResultAdapter(Context context, CheckResult[] values) {
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

        headerView.setText(values[position].getCheck().getPrettyName());
        descriptionView.setText(values[position].getMessage());

        if (values[position].getSuccessful()) {
            // it was successful
            imageView.setImageResource(R.drawable.ok);
        } else {
            // it failed
            imageView.setImageResource(R.drawable.error);
        }

        return rowView;
    }
}
