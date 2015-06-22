/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package is.hello.sense.ui.widget;

import android.animation.TypeEvaluator;
import android.graphics.Rect;

public class RectEvaluatorCompat implements TypeEvaluator<Rect> {
    private Rect rect;

    public RectEvaluatorCompat() {
        this(new Rect());
    }

    public RectEvaluatorCompat(Rect reuseRect) {
        rect = reuseRect;
    }

    @Override
    public Rect evaluate(float fraction, Rect startValue, Rect endValue) {
        rect.left = startValue.left + (int) ((endValue.left - startValue.left) * fraction);
        rect.top = startValue.top + (int) ((endValue.top - startValue.top) * fraction);
        rect.right = startValue.right + (int) ((endValue.right - startValue.right) * fraction);
        rect.bottom = startValue.bottom + (int) ((endValue.bottom - startValue.bottom) * fraction);

        return rect;
    }
}
