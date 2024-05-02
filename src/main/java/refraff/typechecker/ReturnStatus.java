package refraff.typechecker;


public class ReturnStatus {
    boolean maybeReturns;
    boolean definitelyReturns;

    public ReturnStatus() {
        maybeReturns = false;
        definitelyReturns = false;
    }

    public boolean getMaybeReturns() {
        return maybeReturns;
    }

    public boolean getDefinitelyReturns() {
        return definitelyReturns;
    }

    public void setMaybeReturns(boolean boolValue) {
        maybeReturns = boolValue;
    }

    public void setDefinitelyReturns(boolean boolValue) {
        definitelyReturns = boolValue;
    }

    public boolean isValidReturnStatus() {
        if (maybeReturns && !definitelyReturns) {
            return false;
        }
        return true;
    }

    public void updateReturnStatus(ReturnStatus newReturnStatus) {
        // newReturnStatus would be the status of a deeper set of statements
        // in, for example, an if statement. If if definitely or maybe returns,
        // then we update our return status to maybe
        if (newReturnStatus.getDefinitelyReturns() == true
                || newReturnStatus.getMaybeReturns() == true) {
            this.maybeReturns = true;
        }
    }

    public static ReturnStatus getMaybeReturnStatus() {
        ReturnStatus returnStatus = new ReturnStatus();
        returnStatus.setMaybeReturns(true);
        return returnStatus;
    }

    public static ReturnStatus getDefinitelyReturnsStatus() {
        ReturnStatus returnStatus = new ReturnStatus();
        returnStatus.setMaybeReturns(true);
        returnStatus.setDefinitelyReturns(true);
        return returnStatus;
    }

    public static ReturnStatus getDoesNotReturnStatus() {
        return new ReturnStatus();
    }
}
