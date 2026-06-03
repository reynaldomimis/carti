package com.upreyvan.carti.data.local;

import com.upreyvan.carti.R;
import com.upreyvan.carti.model.IconChoice;

import java.util.ArrayList;
import java.util.List;

public class IconManager {
    public static List<IconChoice> getSystemIcons() {
        List<IconChoice> icons = new ArrayList<>();
        icons.add(new IconChoice("Food", R.drawable.ic_chart));
        icons.add(new IconChoice("Home", R.drawable.ic_home));
        icons.add(new IconChoice("Person", R.drawable.ic_person));
        icons.add(new IconChoice("Add", R.drawable.ic_add));
        icons.add(new IconChoice("Calendar", R.drawable.ic_calendar));
        icons.add(new IconChoice("Chart", R.drawable.ic_chart));
        icons.add(new IconChoice("Trophy", R.drawable.ic_trophy));

        // Android System Icons
        icons.add(new IconChoice("Food & Dining", android.R.drawable.ic_menu_gallery));
        icons.add(new IconChoice("Transport", android.R.drawable.ic_menu_directions));
        icons.add(new IconChoice("Bills", android.R.drawable.ic_menu_agenda));
        icons.add(new IconChoice("Grocery", android.R.drawable.ic_menu_add));
        icons.add(new IconChoice("Entertainment", android.R.drawable.ic_menu_camera));
        icons.add(new IconChoice("Health", android.R.drawable.ic_menu_info_details));
        icons.add(new IconChoice("Education", android.R.drawable.ic_menu_edit));
        icons.add(new IconChoice("Salary", android.R.drawable.ic_menu_save));
        icons.add(new IconChoice("Gift", android.R.drawable.btn_star_big_on));
        icons.add(new IconChoice("Savings", android.R.drawable.ic_menu_view));
        icons.add(new IconChoice("Shopping", android.R.drawable.ic_menu_gallery));
        icons.add(new IconChoice("Map", android.R.drawable.ic_dialog_map));
        icons.add(new IconChoice("Call", android.R.drawable.ic_menu_call));
        icons.add(new IconChoice("Compass", android.R.drawable.ic_menu_compass));
        icons.add(new IconChoice("Save", android.R.drawable.ic_menu_save));
        icons.add(new IconChoice("Camera", android.R.drawable.ic_menu_camera));
        icons.add(new IconChoice("Edit", android.R.drawable.ic_menu_edit));
        icons.add(new IconChoice("Delete", android.R.drawable.ic_menu_delete));
        icons.add(new IconChoice("Search", android.R.drawable.ic_menu_search));
        icons.add(new IconChoice("Directions", android.R.drawable.ic_menu_directions));
        icons.add(new IconChoice("Info", android.R.drawable.ic_menu_info_details));
        icons.add(new IconChoice("Agenda", android.R.drawable.ic_menu_agenda));
        icons.add(new IconChoice("View", android.R.drawable.ic_menu_view));
        icons.add(new IconChoice("Upload", android.R.drawable.ic_menu_upload));
        icons.add(new IconChoice("Close", android.R.drawable.ic_menu_close_clear_cancel));
        icons.add(new IconChoice("Star", android.R.drawable.btn_star_big_on));

        return icons;
    }
}
