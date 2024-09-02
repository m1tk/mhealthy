package fr.android.mhealthy;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView responseTextView;  // TextView to show the response

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the TextView
        responseTextView = findViewById(R.id.responseTextView);

        // Correct the base URL
        ApiService apiService = RetrofitClient.getClient("https://192.168.1.44:8000/items/").create(ApiService.class);

        // GET request
        apiService.getItems().enqueue(new Callback<List<Item>>() {
            @Override
            public void onResponse(Call<List<Item>> call, Response<List<Item>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Item> items = response.body();
                    // Display items in the TextView
                    StringBuilder itemText = new StringBuilder();
                    for (Item item : items) {
                        itemText.append("ID: ").append(item.getId()).append("\n");
                        itemText.append("Name: ").append(item.getName()).append("\n");
                        itemText.append("Description: ").append(item.getDescription()).append("\n\n");
                    }
                    responseTextView.setText(itemText.toString());
                } else {
                    // Show failure message
                    Toast.makeText(MainActivity.this, "Failed to retrieve items.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Item>> call, Throwable t) {
                // Show error message
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // POST request
        Item newItem = new Item();
        newItem.setId(1);
        newItem.setName("Example Item");
        newItem.setDescription("This is an example description");

        apiService.addItem(newItem).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Show success message
                    Toast.makeText(MainActivity.this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    // Show failure message
                    Toast.makeText(MainActivity.this, "Failed to add item.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Show error message
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
