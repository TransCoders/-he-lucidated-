package gr.edu.serres.TrancCoder_TheElucitated.Activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import gr.edu.serres.TrancCoder_TheElucitated.Adapters.CustomAdapter;
import gr.edu.serres.TrancCoder_TheElucitated.Database.Database_Functions;
import gr.edu.serres.TrancCoder_TheElucitated.Objects.Inventory;
import gr.edu.serres.TrancCoder_TheElucitated.R;

/**
 * Created by tasos on 30/11/2016.
 */

public class InventoryScreenActivity extends AppCompatActivity {

    private static int counter = 0;
    private ArrayList<String> oneOne;
    private ListView listView;
    private Inventory inventory;
    private Database_Functions getInv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.inventory_layout);


        oneOne = new ArrayList<>();
        //Toast.makeText(getApplicationContext(),String.valueOf(delay.length),Toast.LENGTH_SHORT).show();

        //getInv.getUserInventory(userEmail);
        // Mήτσο εδώ θέλουμε ένα getUserEmail


        while (counter < inventory.getItemNames().size()) {
            oneOne.add(inventory.getItemNames().get(counter));
            counter++;
        }
        counter = 0;

        listView = (ListView) this.findViewById(R.id.ListView);

        ListAdapter adapter = new CustomAdapter(getApplicationContext(), oneOne);
        listView.setAdapter(adapter);

    }
}