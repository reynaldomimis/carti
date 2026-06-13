package com.upreyvan.carti.utils;

import com.upreyvan.carti.R;
import com.upreyvan.carti.models.ColorChoice;
import java.util.ArrayList;
import java.util.List;

public class ColorHelper {

    public static List<ColorChoice> getAvailableColors() {
        List<ColorChoice> colors = new ArrayList<>();
        colors.add(new ColorChoice(R.color.carti_primary_green, R.color.mint_green_alpha));
        colors.add(new ColorChoice(R.color.icon_food, R.color.log_food));
        colors.add(new ColorChoice(R.color.icon_fare, R.color.log_fare));
        colors.add(new ColorChoice(R.color.icon_load, R.color.log_load));
        colors.add(new ColorChoice(R.color.icon_store, R.color.log_store));
        colors.add(new ColorChoice(R.color.icon_electricity, R.color.log_electricity));
        colors.add(new ColorChoice(R.color.icon_water, R.color.log_water));
        colors.add(new ColorChoice(R.color.icon_debt, R.color.log_debt));
        colors.add(new ColorChoice(R.color.status_red, R.color.status_red_tonal));
        colors.add(new ColorChoice(R.color.purple, R.color.tonal_button_bg));
        colors.add(new ColorChoice(R.color.carti_primary_blue, R.color.white_10));
        colors.add(new ColorChoice(R.color.dash_orange, R.color.dash_orange_alpha));
        colors.add(new ColorChoice(R.color.gray, R.color.surface_variant));
        
        // Additional colors
        colors.add(new ColorChoice(R.color.black, R.color.white_10));
        colors.add(new ColorChoice(R.color.gradient_start, R.color.white_10));
        colors.add(new ColorChoice(R.color.gradient_end, R.color.white_10));
        colors.add(new ColorChoice(R.color.green_darker, R.color.mint_green_alpha));
        colors.add(new ColorChoice(R.color.green_budget_status, R.color.mint_green_alpha));
        colors.add(new ColorChoice(R.color.chat_progress_primary, R.color.chat_progress_track));
        colors.add(new ColorChoice(R.color.dash_red, R.color.dash_red_alpha));
        
        return colors;
    }
}
