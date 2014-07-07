package at.rieder.secureqr.app.check;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import at.rieder.secureqr.app.activities.ScanQRActivity;
import at.rieder.secureqr.app.activities.SettingsActivity;
import at.rieder.secureqr.app.check.generic.ErrorRateCheck;
import at.rieder.secureqr.app.check.url.DomainAgeCheck;
import at.rieder.secureqr.app.check.url.PhishtankCheck;
import at.rieder.secureqr.app.check.url.RedirectCountCheck;
import at.rieder.secureqr.app.check.url.SafeBrowsingCheck;
import at.rieder.secureqr.app.check.url.SearchEngineCheck;
import at.rieder.secureqr.app.check.url.SignatureCheck;
import at.rieder.secureqr.app.check.url.WebsiteAgeCheck;
import at.rieder.secureqr.app.model.Content;

/**
 * Created by Thomas on 18.03.14.
 */
public class CheckRunnerFactory {


    private CheckRunnerFactory() {

    }

    public static CheckRunner buildCheckRunner(ScanQRActivity scanQRActivity, Content content) {

        Set<Check> checkSet = new HashSet<Check>();
        Set<Check> allChecks = new HashSet<Check>();
        CheckUIUpdater checkUIUpdater;
        CheckCallback callback;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(scanQRActivity);

        if (prefs.getBoolean(SettingsActivity.CHECK_SAFEBROWSING, false)) {
            allChecks.add(new SafeBrowsingCheck(content));
        }

        if (prefs.getBoolean(SettingsActivity.CHECK_PHISHTANK, false)) {
            allChecks.add(new PhishtankCheck(content));
        }

        if (prefs.getBoolean(SettingsActivity.CHECK_ERRORRATE, false)) {
            allChecks.add(new ErrorRateCheck(content));
        }

        if (prefs.getBoolean(SettingsActivity.CHECK_METRICS_DNS_AGE, false)) {
            allChecks.add(new DomainAgeCheck(content));
        }

        if (prefs.getBoolean(SettingsActivity.CHECK_METRICS_REDIRECT_COUNT, false)) {
            allChecks.add(new RedirectCountCheck(content));
        }

        if (prefs.getBoolean(SettingsActivity.CHECK_METRICS_IF_PUBLIC, false)) {
            allChecks.add(new SearchEngineCheck(content));
        }

        if (prefs.getBoolean(SettingsActivity.CHECK_METRICS_WEBSITE_AGE, false)) {
            allChecks.add(new WebsiteAgeCheck(content));
        }

        if (prefs.getBoolean(SettingsActivity.CHECK_SIGNATURE, false)) {
            allChecks.add(new SignatureCheck(content));
        }

        // TODO expand with additional checks

        for (Check check : allChecks) {
            if (check.doesVerify()) {
                checkSet.add(check);
            }
        }

        checkUIUpdater = new CheckUIUpdater(scanQRActivity, checkSet.size(), content);
        checkUIUpdater.setProgress(0, 0);

        callback = new CheckCallback(checkUIUpdater);
        checkUIUpdater.setCallback(callback);

        for (Check check : checkSet) {
            check.addCallback(callback);
        }

        return new CheckRunner(checkSet);
    }
}
