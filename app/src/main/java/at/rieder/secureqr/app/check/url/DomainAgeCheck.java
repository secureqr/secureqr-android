package at.rieder.secureqr.app.check.url;

import android.util.Log;

import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import at.rieder.secureqr.app.R;
import at.rieder.secureqr.app.helper.HelperUtils;
import at.rieder.secureqr.app.managers.URLResolver;
import at.rieder.secureqr.app.model.CheckResult;
import at.rieder.secureqr.app.model.Content;

/**
 * Created by Thomas on 25.03.14.
 */
public class DomainAgeCheck extends URLCheck {

    private static final String TAG = DomainAgeCheck.class.getSimpleName();

    // everything below 15 minutes is strange
    private static final Long TTL_LIMIT = 900L;
    private static final String DNS_SERVER = "208.67.222.222";
    private static final String DYNAMIC_DNS_FILE = "dynamic_dns.txt";

    private static Set<String> dyndnsDomains;

    static {
        dyndnsDomains = new HashSet<String>();
        dyndnsDomains = loadDomainsFromFile();
        Log.d(TAG, "the dynamic dns list is: " + dyndnsDomains);
    }

    public DomainAgeCheck(Content content) {
        super(content);
    }

    @Override
    public String getPrettyName() {
        return "DNS Volatility";
    }

    @Override
    public void run() {
        try {
            String host = new URL(URLResolver.resolveRedirectsOfUrl(content.getURL())).getHost();

            Resolver resolver = new SimpleResolver(DNS_SERVER);
            Record record = Record.newRecord(Name.fromString(host + "."), Type.A, 1);
            Message message = Message.newQuery(record);
            Message response;
            try {
                response = resolver.send(message);
            } catch (SocketTimeoutException e) {
                Log.w(TAG, "got a dns socket timeout. trying one more time...");
                response = resolver.send(message);
            }

            Long ttl = Long.MAX_VALUE;

            int i = 0;
            while (response.getSectionArray(i) != null && response.getSectionArray(i).length != 0) {
                Record[] records = response.getSectionArray(i);

                for (int j = 0; j < records.length; j++) {
                    Log.d(TAG, "got ttl: " + records[j].getTTL());
                    if (records[j].getTTL() > 0 && records[j].getTTL() < ttl) {
                        ttl = records[j].getTTL();
                    }
                }
                i++;
            }

            if (ttl <= TTL_LIMIT) {
                if (isDynDnsHostname(new URL(content.getURL()))) {
                    callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_dyndns_yes) + " " + ttl + "s", this));
                } else {
                    callback.notify(new CheckResult(true, HelperUtils.getContext().getString(R.string.msg_dyndns_no_short) + " " + ttl + "s", this));
                }
            } else {
                callback.notify(new CheckResult(true, HelperUtils.getContext().getString(R.string.msg_dns_ttl_ok), this));
            }

        } catch (MalformedURLException e) {
            Log.w(TAG, "error parsing hostname");
            e.printStackTrace();
            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_dyndns_error), this));
        } catch (IOException e) {
            Log.w(TAG, "error connecting to server");
            e.printStackTrace();
            callback.notify(new CheckResult(false, HelperUtils.getContext().getString(R.string.msg_dyndns_error), this));
        }
    }

    private boolean isDynDnsHostname(URL url) {
        String hostname = url.getHost();

        for (String dynhost : dyndnsDomains) {
            if (hostname.contains(dynhost)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> loadDomainsFromFile() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(HelperUtils.getContext().getAssets().open(DYNAMIC_DNS_FILE)));
            String line = reader.readLine();
            Set<String> retSet = new HashSet<String>();

            while (line != null) {
                retSet.add(line);
                line = reader.readLine();
            }
            reader.close();

            return retSet;

        } catch (IOException e) {
            Log.w(TAG, "error loading dynamic dns list");
            e.printStackTrace();
            return new HashSet<String>();
        }
    }
}
