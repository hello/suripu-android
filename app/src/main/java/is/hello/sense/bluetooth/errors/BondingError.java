package is.hello.sense.bluetooth.errors;

public class BondingError extends BluetoothError {
    public BondingError() {
        super("Could not change bonding state");
    }
}
