package is.hello.sense.presenters.outputs;

public interface FlowOutput {

    void finishActivity();

    void finishFlowWithResult(int resultCode);

    void finishFlow();

    void cancelFlow();
}
