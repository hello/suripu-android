package com.hello.ble;

import android.content.Context;

public class HelloBle {
    // TODO: Refactor bluetooth library to use dependency injection.

    private static Context applicationContext;

    public static void init(Context context) {
        HelloBle.applicationContext = context.getApplicationContext();
    }


    //region Getters

    public static Context getApplicationContext() {
        return applicationContext;
    }

    //endregion
}
