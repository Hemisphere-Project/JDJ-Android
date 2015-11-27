package com.hmsphr.jdj.Class;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Collections;

class SortedArrayList<T> extends ArrayList<T> {
    @SuppressWarnings("unchecked")
    public void insertSorted(T value) {
        add(value);
        Comparable<T> cmp = (Comparable<T>) value;
        for (int i = size()-1; i > 0 && cmp.compareTo(get(i-1)) < 0; i--)
            Collections.swap(this, i, i - 1);
    }
}

