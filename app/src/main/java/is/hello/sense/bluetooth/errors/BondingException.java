package is.hello.sense.bluetooth.errors;

public class BondingException extends BluetoothException {
    public BondingException() {
        super("Could not change bonding state");
    }
}
