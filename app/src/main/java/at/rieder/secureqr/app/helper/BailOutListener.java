package at.rieder.secureqr.app.helper;

import android.app.Activity;
import android.content.DialogInterface;

/**
 * Created by Thomas on 18.03.14.
 */
public class BailOutListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

    private Activity activity;

    public BailOutListener(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        this.activity.finish();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        this.activity.finish();
    }
}
