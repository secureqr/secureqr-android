package at.rieder.secureqr.app.model;

import java.io.Serializable;

import at.rieder.secureqr.app.check.Check;

/**
 * Created by Thomas on 18.03.14.
 */
public class CheckResult implements Comparable<CheckResult>, Serializable {

    private boolean successful;
    private String message;
    private transient Check check;

    public CheckResult(boolean successful, String message, Check check) {
        this.successful = successful;
        this.message = message;
        this.check = check;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CheckResult that = (CheckResult) o;

        if (successful != that.successful) return false;
        if (check != null ? !check.equals(that.check) : that.check != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (successful ? 1 : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (check != null ? check.hashCode() : 0);
        return result;
    }

    public boolean getSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Check getCheck() {
        return check;
    }

    public void setCheck(Check check) {
        this.check = check;
    }

    @Override
    public int compareTo(CheckResult checkResult) {
        if (check != null && checkResult.check != null) {
            return this.getCheck().getPrettyName().compareTo(checkResult.getCheck().getPrettyName());
        } else {
            return this.message.compareTo(checkResult.message);
        }

    }
}
