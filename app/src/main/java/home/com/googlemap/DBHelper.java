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
    public static final String RESTAURANT_COLUMN_ADRRESS= "address";
    public static final String RESTAURANT_COLUMN_LATITUDE = "latitude";
    public static final String RESTAURANT_COLUMN_LONGITUDE = "longitude";

    private HashMap hp;

    public DBHelper(Context context){
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL("create table restaurant" + "(id integer primary key, name text,cuisine text," +
                "distance real, work blob, rest blob, time text, price blob, contact text," +
                " address blob, latitude real, longitude real)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){

        db.execSQL("DROP TABLE IF EXISTS contacts");
        onCreate(db);
    }

    public boolean insertRestaurant (String name, String cuisine, String distance, String work,
                                     String rest, String time, String price, String contact,
                                     String address, String latitude, String longitude){
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
        db.insert("restaurant",null,contentValues);
        return true;
    }

    public Cursor getData(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from restaurant where id="+id+"",null);
        return res;
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, CONTACTS_TABLE_NAME);
        return numRows;
    }

    public boolean updateContact (Integer id, String name, String phone, String email, String homepage){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name",name);
        contentValues.put("phone",phone);
        contentValues.put("email",email);
        contentValues.put("homepage",homepage);
        db.update("contacts",contentValues,"id= ?",new String[]{Integer.toString(id)});
        return true;
    }

    public void deleteContact (Integer id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("contacts","id= ?",new String[]{Integer.toString(id)});

        while(id < numberOfRows()+1){
            ContentValues contentValues = new ContentValues();
            contentValues.put("id", id);
            db.update("contacts", contentValues, "id = ? ", new String[] { Integer.toString(id + 1)});
            id++;
        }
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
