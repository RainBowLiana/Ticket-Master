
/* Course: 22F-CST2335-011
 * Professor: Adewole Adewumi
 * Author: Kamelia Pezeshki
 * File name: SavedEvents.java
 * Date: 2022-12-06
 * Final Project
 */
package algonquin.cst2335.finalproject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.cst2335.finalproject.R;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 *
 */

public class SavedEvents extends Fragment {
    /**
     * ArrayList as a container for events
     */
    private ArrayList<Event> events = new ArrayList<>();

    /**
     * Instance of the ListAdapter class
     */
    ListAdapter eventAdapter;

    /**
     * Instance of the ListView class
     */
    ListView eventListView;

    /**
     * Instance of the MyOpenHelper class
     */
    MyOpenHelper myOpener;


    /**
     * Instance of the SQLiteDatabase class
     */
    SQLiteDatabase db;

    /**
     * default constructor
     */
    public SavedEvents() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    /**
     * this class is used to inflate the UI
     *
     * @param inflater : XML layout files can be loaded using a LayoutInflater object.
     * @param container  : works as a container for other views and layouts that is unseen.
     * @param savedInstanceState : a Bundle object reference that is sent to the onCreateView function
     * @return newView : contains the widgets that are in your layout and is the root object from your XML file.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Favorite Events");

        View newView = inflater.inflate(R.layout.fragment_saved_events, container, false);

        // Initialize list view
        eventListView = newView.findViewById(R.id.eventListView3);
        eventListView.setAdapter(eventAdapter = new ListAdapter());


        // Querying the database
        myOpener =  new MyOpenHelper(this.getActivity());
        db = myOpener.getWritableDatabase();
        Cursor cursor = db.rawQuery("Select * from " + MyOpenHelper.TABLE_NAME + ";", null);
        int idIndex =  cursor.getColumnIndex(MyOpenHelper.COL_ID);
        int dataIndex = cursor.getColumnIndex(MyOpenHelper.COL_DATA);

        while(cursor.moveToNext()) {
           String dbId = cursor.getString(idIndex);
           String dbData = cursor.getString(dataIndex);

           Gson gson = new Gson();
           JsonObject eventObject = gson.fromJson(dbData, JsonObject.class);
           String name = eventObject.get("name").getAsString();

           JsonObject priceRange = eventObject.get("priceRanges").getAsJsonArray().get(0).getAsJsonObject();

           String currency = priceRange.get("currency").getAsString();
           String minPrice  = priceRange.get("min").getAsString();
           String maxPrice = priceRange.get("max").getAsString();

           String formattedPrice = "From " + minPrice + " - " + maxPrice + " " + currency;

           String dateStartObject = eventObject.get("sales").getAsJsonObject().get("public").getAsJsonObject().get("startDateTime").getAsString();

           String imgUrl = eventObject.get("images").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();

           Event currentEvent = new Event(dbId, name, formattedPrice, imgUrl, dateStartObject);
           events.add(currentEvent);
       }

        return newView;
    }


    /**
     * The four functions of the implemented ListAdapter interface can be used with a customised list.
     */
    private class ListAdapter extends BaseAdapter {
        public int getCount() {
            return events.size();
        }

        public Event getItem(int position) {
            return events.get(position);
        }

        public long getItemId(int position) {
            return getItem(position).id;
        }

        public View getView(int position, View newView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            newView = inflater.inflate(R.layout.event_listview_layout, parent, false);

            // Putting name and price in TextView
            TextView tView = newView.findViewById(R.id.eventName);
            tView.setText(getItem(position).name + "\n" + getItem(position).price);

            // Putting image in ImageView
            ImageView iView = newView.findViewById(R.id.eventImg);
            Picasso.get().load(getItem(position).img_link).into(iView);

            // Delete button with alert dialogue
            Button deleteBtn = newView.findViewById(R.id.delete_btn);

            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setMessage("This was not your favorite event?");
                    builder.setCancelable(true);

                    builder.setPositiveButton(
                            "Nah :(",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int id) {
                                    db.delete(MyOpenHelper.TABLE_NAME, MyOpenHelper.COL_ID + "= ?" , new String[] { getItem(position)._id } );
                                    events.remove(position);
                                    eventAdapter.notifyDataSetChanged();
                                    Toast.makeText(getActivity(),"Event has been successfully erased!", Toast.LENGTH_LONG).show();
                                }
                            });
                    builder.setNegativeButton(
                            "Love it!",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            });

                    AlertDialog alert = builder.create();
                    alert.show();
                }
            });

            return newView;   //return the inflated view
        }
    }

    /**
     * class model for array list events
     */
    static class Event {
        String _id ;
        String name;
        String price;
        String img_link;
        String date;
        long id;

        Event(String _id, String name , String price, String img_link, String date) {
            this._id = _id;
            this.name = name;
            this.price = price;
            this.img_link = img_link;
            this.date = date;
        }

    }

}