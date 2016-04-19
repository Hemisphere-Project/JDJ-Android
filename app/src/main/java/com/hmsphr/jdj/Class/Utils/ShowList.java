package com.hmsphr.jdj.Class.Utils;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by mgr on 15/01/16.
 */
public class ShowList {
    private List<Show> showlist = new ArrayList<Show>();

    public void addShow(Show _show) {
        showlist.add(_show);
        Collections.sort(showlist);
    }

    public void addShow(int _id, String _date, String _place) {
        addShow(new Show(_id, _date, _place));
    }

    public String export() {
        Gson gson = new Gson();
        return gson.toJson(this.showlist);
    }

    public void inflate(String json) {
        Gson gson = new Gson();
        Type collectionType = new TypeToken<List<Show>>(){}.getType();
        showlist = gson.fromJson(json, collectionType);
    }

    public String[] itemsList() {
        List<String> list = new ArrayList<>();
        int size = 0;
        if (showlist != null) {
            for (int i = 0; i < showlist.size(); i++) {
                list.add(i, showlist.get(i).label());
            }
            size = list.size();
        }

        return list.toArray(new String[size]);
    }

    public Show find(String item) {
        if (showlist != null)
            for (int i = 0; i < showlist.size(); i++) {
                if (showlist.get(i).label().equals(item))
                {
                    Log.d("ShowList", "show found");
                    return showlist.get(i);
                }
            }
        return null;
    }
}

