package at.rieder.secureqr.app.managers;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import at.rieder.secureqr.app.helper.HelperUtils;
import at.rieder.secureqr.app.model.CheckResult;
import at.rieder.secureqr.app.model.Content;
import at.rieder.secureqr.app.model.HistoryItem;

/**
 * Created by Thomas on 25.03.14.
 */
public class HistoryManager {

    private static final String TAG = HistoryManager.class.getSimpleName();

    private Set<HistoryItem> historySet;
    private HistoryStorage historyStorage;

    public HistoryManager(Context context) {
        this.historyStorage = new HistoryStorage(context);
        this.historySet = new TreeSet<HistoryItem>();
        loadFromDatabase();
    }

    private void loadFromDatabase() {
        Collection<HistoryItem> items = this.historyStorage.getAllHistoryItems();
        this.historySet.addAll(items);
    }

    public void close() {
        this.historyStorage.close();
    }

    public synchronized void addScanResult(Content content, Collection<CheckResult> checkResults) {
        Log.d(TAG, "got the content: " + content + " and the checkresults: " + checkResults);

        if (checkResults == null) {
            checkResults = new ArrayList<CheckResult>();
        }

        if (content != null) {
            HistoryItem historyItem = new HistoryItem();
            historyItem.setContent(content);
            historyItem.setDate(new Date());
            historyItem.setCheckResult(checkResults);

            historySet.add(historyItem);
            historyStorage.addScanResultToDatabase(historyItem);
        }
    }

    public synchronized Collection<CheckResult> getMostRecentCheckResults() {
        HistoryItem lastItem = getMostRecentHistoryItem();

        if (lastItem != null) {
            return lastItem.getCheckResult();
        } else {
            return new ArrayList<CheckResult>();
        }
    }

    public synchronized Collection<HistoryItem> getHistory() {
        return historySet;
    }

    public boolean isEmpty() {
        return this.historySet.isEmpty();
    }

    public synchronized void clearHistory() {
        this.historySet.clear();
        this.historyStorage.clear();
    }

    public synchronized Content getMostRecentContent() {
        HistoryItem lastItem = getMostRecentHistoryItem();

        if (lastItem != null) {
            return lastItem.getContent();
        } else {
            return new Content();
        }
    }

    private HistoryItem getMostRecentHistoryItem() {
        HistoryItem lastItem = null;

        for (HistoryItem historyItem : historySet) {
            if (lastItem == null) {
                lastItem = historyItem;
            } else {
                if (lastItem.getDate().before(historyItem.getDate())) {
                    lastItem = historyItem;
                }
            }
        }

        return lastItem;
    }
}
