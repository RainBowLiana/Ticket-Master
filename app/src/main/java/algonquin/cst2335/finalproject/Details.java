
package algonquin.cst2335.finalproject;
/**
 * course: 22F-CST 2335-021
 * author Kamelia Pezeshki
 * professor Adewole Adewumi
 * student number: 040844200
 * file name: Detail.java
 */

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.cst2335.finalproject.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;

import algonquin.cst2335.finalproject.MyOpenHelper;


public class Details extends Fragment {
    Event e;
    TextView title, date, price;
    Button saveBtn, viewBtn;
    ImageView headImg;
    ProgressBar spinner;
    Snackbar snackbar;
    MyOpenHelper myOpener;
    SQLiteDatabase db;
    String lastSearchedEvent;

    public static final String ID = "ID";

    private String id;

    public Details() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //get the ID
        if (getArguments() != null) {
            id = getArguments().getString(ID);

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // DATABASE


        // initilize the onCreate
        myOpener = new MyOpenHelper(this.getActivity());
        // open the database
        db = myOpener.getWritableDatabase();


        // DATABASE END


        getActivity().setTitle("Detail");
        View newView = inflater.inflate(R.layout.fragment_details, container, false);

        // move oncreate
        spinner = newView.findViewById(R.id.spinner);
        spinner.setVisibility(View.GONE);
        title = (TextView) newView.findViewById(R.id.title);
        viewBtn = (Button) newView.findViewById(R.id.button2);
        headImg = (ImageView) newView.findViewById(R.id.imageView3);
        date = (TextView) newView.findViewById(R.id.date);
        saveBtn = (Button) newView.findViewById(R.id.button3);
        price = newView.findViewById(R.id.price);


        // call http get request
        String result = null;
        if (id != null) {
            String url = "https://app.ticketmaster.com/discovery/v2/events/" + id + ".json?apikey=ylAAMmRMEsPlfVwdNRsbPGzh9sUwbyNm";

            try {
                result = new RequestTask()
                        .execute(url)
                        .get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        if (result == null) {
            SharedPreferences prefs = this.getActivity().getSharedPreferences("SEARCH_DETAIL", Context.MODE_PRIVATE);
            lastSearchedEvent = prefs.getString("lastSearchedEvent", "");
            result = lastSearchedEvent;
        }
        //create an object of Gson
        Gson gson = new Gson();
        // we use result
        JsonObject eventObject = gson.fromJson(result, JsonObject.class);
        //get the name
        String name = eventObject.get("name").getAsString();
        //get the url
        String linkUrl = eventObject.get("url").getAsString();
        JsonObject priceRange = eventObject.get("priceRanges").getAsJsonArray().get(0).getAsJsonObject();
//get currency,min,max
        String currency = priceRange.get("currency").getAsString();
        String minPrice = priceRange.get("min").getAsString();
        String maxPrice = priceRange.get("max").getAsString();
//show price in this format
        String formattedPrice = "From " + minPrice + " " + currency + " To: " + maxPrice;
//get date by using sales
        String dateStartObject = eventObject.get("sales").getAsJsonObject().get("public").getAsJsonObject().get("startDateTime").getAsString();
        SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");


        //set name for title
        title.setText(name);
        price.setText(formattedPrice);
        try {
            //set date
            date.setText(format.parse(dateStartObject).toString().substring(0, 10));
        } catch (ParseException parseException) {
            parseException.printStackTrace();
        }

        //get the image
        String imgUrl = eventObject.get("images").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();

        Picasso.get().load(imgUrl).into(headImg);

//viewBtn setonClicklistener
        viewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            //use intent for going to linkurl page
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl));
                getActivity().startActivity(intent);
            }
        });

        lastSearchedEvent = eventObject.toString();
        /**
         * saveBtn onclick listener for saving the most recent event in the database
         */
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ContentValues cv = new ContentValues();
                cv.put(myOpener.COL_ID, id);
                cv.put(myOpener.COL_DATA, lastSearchedEvent);
                db.insert(myOpener.TABLE_NAME, null, cv);
                //snackbar for save the event

                snackbar = Snackbar.make(view, "Event Saved!", 3000);
                //set action in snack bar for ok and close it
                snackbar.setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        snackbar.dismiss();
                    }
                });
                snackbar.show();


            }
        });

        saveLastestEvent(lastSearchedEvent);


        return newView;
    }
    //save lastsearchevent by using sharepreference

    private void saveLastestEvent(String lastEvent) {
        SharedPreferences prefs = getActivity().getSharedPreferences("SEARCH_DETAIL", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("lastSearchedEvent", lastEvent);

        edit.commit();
    }

    class RequestTask extends AsyncTask<String, String, String> {
        @Override
        public String doInBackground(String... uri) {

            try {

                URL url = new URL(uri[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    return sb.toString();

                } finally {
                    urlConnection.disconnect();
                }

            } catch (Exception e) {
                System.out.println("Kamelia" + e.getMessage());
            }

            return "Done";
        }


        @Override
        protected void onPreExecute() {
            spinner.setVisibility(View.VISIBLE);
        }

        //Type 2
        public void onProgressUpdate(Integer... args) {
            spinner.setVisibility(View.VISIBLE);
        }

        //Type3
        public void onPostExecute(String fromDoInBackground) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    spinner.setVisibility(View.GONE);
                }
            }, 1000);
        }
    }

    public class Event {
        private String ind;
        private String name;
        private String url;
        private String img_link;
        private String date;

        public Event() {

        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getDate() {
            return date;
        }

        public String getImg_link() {
            return img_link;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setImg_link(String img_link) {
            this.img_link = img_link;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return ind;
        }

        public void setId(String ind) {
            this.ind = ind;
        }
    }

}