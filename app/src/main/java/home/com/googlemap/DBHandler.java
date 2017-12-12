package home.com.googlemap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHandler extends SQLiteOpenHelper {

    // Database Name
    private static final String DATABASE_NAME = "restInfo";
    private static final String TABLE_RESTAURANT = "restaurant";
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_LONGITUDE = "restaurant_longitude";
    private static final String KEY_LATITUDE = "restaurant_latitude";

    public DBHandler(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_RESTAURANT_TABLE = "CREATE TABLE " + TABLE_RESTAURANT + "("
                + KEY_ID + "INTEGER PRIMARY KEY," + KEY_NAME + "TEXT,"
                + KEY_LONGITUDE + "REAL," + KEY_LATITUDE + "REAL" + ")";
        db.execSQL(CREATE_RESTAURANT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESTAURANT);
        // Creating tables again
        onCreate(db);
    }

    // Adding new shop
    public void addRestaurant(Restaurant restaurant) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, restaurant.getName()); // Restaurant Name
        values.put(KEY_LONGITUDE, restaurant.getLongitude());
        values.put(KEY_LATITUDE, restaurant.getLatitude());

        // Inserting Row
        db.insert(TABLE_RESTAURANT, null, values);
        db.close(); // Closing database connection

    }

    // Getting one restaurant
    public Restaurant getData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_RESTAURANT, new String[]{KEY_ID,
                        KEY_NAME, KEY_LONGITUDE, KEY_LATITUDE}, KEY_ID + " =?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Restaurant restaurant = new Restaurant(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getString(2), cursor.getString(3));
        // return shop
        return restaurant;
    }

    // Getting All Shops
    public List<Restaurant> getAllRestaurant() {
        List<Restaurant> restaurantList = new ArrayList<Restaurant>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_RESTAURANT;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Restaurant restaurant = new Restaurant();
                restaurant.setId(Integer.parseInt(cursor.getString(0)));
                restaurant.setName(cursor.getString(1));
                restaurant.setLongitude(cursor.getString(2));
                restaurant.setLatitude(cursor.getString(3));

                // Adding contact to list
                restaurantList.add(restaurant);
            } while (cursor.moveToNext());
        }

        // return contact list
        return restaurantList;
    }

    // Getting shops Count
    public int getRestaurantCount() {
//        String countQuery = "SELECT * FROM " + TABLE_RESTAURANT;
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(countQuery, null);
//        cursor.close();
//// return count
//        return cursor.getCount();
        SQLiteDatabase db = this.getReadableDatabase();
        return (int) DatabaseUtils.queryNumEntries(db, TABLE_RESTAURANT);
    }
}