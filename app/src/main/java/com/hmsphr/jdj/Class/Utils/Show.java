package com.hmsphr.jdj.Class.Utils;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class Show implements Comparable<Show> {
    private int id;
    private String date;
    private String place;
    private long datestamp;

    public Show(int _id, String _date, String _place) {
        id = _id;
        date = _date;
        place = _place;

        datestamp = id;
        String[] ls = date.split("/");
        if (ls.length == 3)
            datestamp = (Integer.parseInt(ls[0])
                    +Integer.parseInt(ls[1])*100
                    +Integer.parseInt(ls[2])*10000)*1000 + id;

    }

    public String label() {
        return date+" - "+place;
    }

    public Long datestamp() {
        return datestamp;
    }

    public int compareTo(Show s) {
        if (this.datestamp < s.datestamp) return -1;
        else if (this.datestamp > s.datestamp) return 1;
        else return 0;
    }

    public int getId() {
        return id;
    }

    public String export() {
        Gson gson = new Gson();
        String s = gson.toJson(this);
        return s;
    }

    public static Show inflate(String json) {
        Gson gson = new Gson();
        if (json != null) return gson.fromJson(json, Show.class);
        else return null;
    }
}
