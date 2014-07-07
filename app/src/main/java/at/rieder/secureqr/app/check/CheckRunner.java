package at.rieder.secureqr.app.check;

import android.util.Log;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Thomas on 18.03.14.
 */
public class CheckRunner extends Thread {

    private static final int TIMEOUT_DURATION = 10;
    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    private static final String TAG = CheckRunner.class.getSimpleName();


    private Collection<Check> checks;

    public CheckRunner(Collection<Check> checks) {
        this.checks = checks;
    }

    public void runChecks() {
        ExecutorService executor = Executors.newCachedThreadPool();

        for (Check check : checks) {
            executor.execute(check);
        }

        executor.shutdown();

        try {
            executor.awaitTermination(TIMEOUT_DURATION, TIMEOUT_UNIT);
        } catch (InterruptedException e) {
            Log.i(TAG, "CheckRunner got interrupted");
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        this.runChecks();
    }
}
