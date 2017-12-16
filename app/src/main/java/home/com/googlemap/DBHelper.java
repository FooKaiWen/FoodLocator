package home.com.googlemap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by user on 12/11/2017.
 */

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "restaurant_profile.db";
    public static final String CONTACTS_TABLE_NAME = "restaurant";
    public static final String RESTAURANT_COLUMN_ID = "id";
    public static final String RESTAURANT_COLUMN_NAME = "name";
    public static final String RESTAURANT_COLUMN_CUISINE = "cuisine";
    public static final String RESTAURANT_COLUMN_DISTANCE = "distance";
    public static final String RESTAURANT_COLUMN_WORK = "work";
    public static final String RESTAURANT_COLUMN_REST= "rest";
    public static final String RESTAURANT_COLUMN_TIME= "time";
    public static final String RESTAURANT_COLUMN_PRICE= "price";
    public static final String RESTAURANT_COLUMN_CONTACT= "contact";
    public static final String RESTAURANT_COLUMN_ADDRESS = "address";
    public static final String RESTAURANT_COLUMN_LATITUDE = "latitude";
    public static final String RESTAURANT_COLUMN_LONGITUDE = "longitude";
    public static final String RESTAURANT_COLUMN_IMAGENAME = "imagename";
    public static final String RESTAURANT_COLUMN_FOODTYPE = "foodtype";

    private HashMap hp;

    public DBHelper(Context context){
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL("create table restaurant" + "(id integer primary key, name text,cuisine text," +
                "distance blob, work blob, rest blob, time text, price text, contact text," +
                " address blob, latitude blob, longitude blob, imagename text, foodtype text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){

        db.execSQL("DROP TABLE IF EXISTS restaurant");
        onCreate(db);
    }

    public boolean insertRestaurant (String name, String cuisine, String distance, String work,
                                     String rest, String time, String price, String contact,
                                     String address, String latitude, String longitude, String imagename, String foodtype){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name",name);
        contentValues.put("cuisine",cuisine);
        contentValues.put("distance",distance);
        contentValues.put("work",work);
        contentValues.put("rest",rest);
        contentValues.put("time",time);
        contentValues.put("price",price);
        contentValues.put("contact",contact);
        contentValues.put("address",address);
        contentValues.put("latitude",latitude);
        contentValues.put("longitude",longitude);
        contentValues.put("imagename",imagename);
        contentValues.put("foodtype",foodtype);
        db.insert("restaurant",null,contentValues);
        return true;
    }

    public Cursor getData(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from restaurant where id="+id+"",null);
        return res;
    }

    public Cursor getDataforSpinner(String cuisine, String price){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from(select * from restaurant "+cuisine+") "+price+"",null);
        return res;
    }

    public Cursor getRandomDataforSpinner(String cuisine, String price, int randomNum){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from(select * from restaurant "+cuisine+") "+price+"",null);
        return res;
    }

    public Cursor getDataUsingName(String name){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from restaurant where name='"+name+"'",null);
        return res;
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        return (int) DatabaseUtils.queryNumEntries(db, CONTACTS_TABLE_NAME);
    }

    public ArrayList<String> getAllContacts(){
        ArrayList<String>   array_List = new ArrayList<String>();
        hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from restaurant order by id",null);
        res.moveToFirst();

        while(!res.isAfterLast()){
            array_List.add(res.getString(res.getColumnIndex(RESTAURANT_COLUMN_ID)) + ".   " + res.getString(res.getColumnIndex(RESTAURANT_COLUMN_NAME)));
            res.moveToNext();
        }
        return array_List;
    }
}
