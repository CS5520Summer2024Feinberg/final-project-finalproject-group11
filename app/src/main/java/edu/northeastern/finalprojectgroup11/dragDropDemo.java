package edu.northeastern.finalprojectgroup11;

import android.content.ClipData;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class dragDropDemo extends AppCompatActivity {

    private TextView dragTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drag_drop_demo);

        dragTextView = findViewById(R.id.dragTextView);

        // Set up the drag listener for the TextView
        dragTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDrag(data, shadowBuilder, v, 0);
                return true;
            }
        });

        // Set up the drag listener for the GridLayout
        GridLayout gridLayout = findViewById(R.id.gridLayout);
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View block = gridLayout.getChildAt(i);
            block.setOnDragListener(new MyDragListener());
        }
    }

    private class MyDragListener implements OnDragListener {

        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light)); // Highlight the target
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundColor(getResources().getColor(android.R.color.darker_gray)); // Revert the highlight
                    return true;

                case DragEvent.ACTION_DROP:
                    View draggedView = (View) event.getLocalState();
                    draggedView.setX(v.getX());
                    draggedView.setY(v.getY());

                    // Get the location of the block
                    String location = ((TextView) v).getText().toString();
                    Toast.makeText(dragDropDemo.this, "Dropped at " + location, Toast.LENGTH_SHORT).show();
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    v.setBackgroundColor(getResources().getColor(android.R.color.darker_gray)); // Revert the highlight
                    return true;

                default:
                    break;
            }
            return false;
        }
    }
}