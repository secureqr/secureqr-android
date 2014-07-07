package at.rieder.secureqr.app.check;

import java.util.Set;
import java.util.TreeSet;

import at.rieder.secureqr.app.model.CheckResult;

/**
 * Created by Thomas on 18.03.14.
 */
public class CheckCallback {

    private Set<CheckResult> checkResults;
    private final CheckUIUpdater checkUIUpdater;

    private Integer completedCount;
    private Integer errorCount;

    public CheckCallback(CheckUIUpdater checkUIUpdater) {
        this.checkUIUpdater = checkUIUpdater;
        this.checkResults = new TreeSet<CheckResult>();
        this.completedCount = 0;
        this.errorCount = 0;
    }

    public synchronized void notify(CheckResult result) {
        this.checkResults.add(result);
        this.completedCount++;

        if (!result.getSuccessful()) {
            errorCount++;
        }

        this.updateUI();
    }

    private synchronized void updateUI() {
        this.checkUIUpdater.setProgress(this.completedCount, this.errorCount);
    }

    public Set<CheckResult> getCheckResults() {
        return checkResults;
    }
}
