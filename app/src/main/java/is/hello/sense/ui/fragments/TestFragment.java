package is.hello.sense.ui.fragments;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.util.AnimatorSetHandler;

//TODO delete this fragment after merging functionality to proper classes
public class TestFragment extends android.app.Fragment{

    View blueBox;
    private AnimatorSetHandler animatorSetHandler;
    private AnimatorSet set;

    public TestFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pill_ota_view, container, false);

        this.blueBox = view.findViewById(R.id.blue_box_view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeAnimation();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseAnimation();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        set = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.bluetooth_sleep_pill_ota_animator);
        set.setTarget(blueBox);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.animatorSetHandler = null;
        this.blueBox.clearAnimation();
        this.blueBox = null;
        this.set = null;
    }

    private void pauseAnimation() {
        this.animatorSetHandler.removeCallbacks();
    }

    private void resumeAnimation(){
        if(animatorSetHandler == null) {
            this.animatorSetHandler = new AnimatorSetHandler(800, set);
        }
        animatorSetHandler.start();
    }
}
