package at.rieder.secureqr.app.check;

/**
 * Created by Thomas on 18.03.14.
 */
public interface Check extends Runnable {

    public boolean doesVerify();

    public void addCallback(CheckCallback callback);

    public String getPrettyName();
}
