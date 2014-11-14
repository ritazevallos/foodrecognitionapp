import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by rzevall1 on 11/13/14.
 */
public class SharplesMenu {
    private ArrayList<String> breakfastMenu;
    private ArrayList<String> lunchMenu;
    private ArrayList<String> dinnerMenu;
    private ArrayList<String> alwaysMenu;

    private Date breakfastStart;
    private Date breakfastEnd;
    private Date lunchStart;
    private Date lunchEnd;
    private Date dinnerStart;
    private Date dinnerEnd;

    public SharplesMenu(Context c){
        // constructor should fill in the breakfast, lunch, and dinner menu from the dash
        // as well as the current start and end times
        // here is a java web scraper: http://jsoup.org/

        breakfastMenu = new ArrayList<String>();
        lunchMenu = new ArrayList<String>();
        dinnerMenu = new ArrayList<String>();

        // we'd have to hand-type in the always menu (salad bar)
        alwaysMenu = new ArrayList<String>();

        // should probably throw an error if the times overlap for some reason?
    }

    public ArrayList<String> getMenu(Date rightNow){
        //todo: eventually after figuring out nutrition database interaction, don't want to just be passing around strings

        ArrayList<String> menu = new ArrayList<String>();

        if (!(rightNow.after(breakfastStart) || rightNow.after(breakfastEnd))){
            // it's breakfast right now!
            menu.addAll(breakfastMenu);
            menu.addAll(alwaysMenu);
        }
        else if (!(rightNow.after(lunchStart) || rightNow.after(lunchEnd))){
            // it's lunch right now!
            menu.addAll(lunchMenu);
            menu.addAll(alwaysMenu);
        }
        else if (!(rightNow.after(dinnerStart) || rightNow.after(dinnerEnd))){
            // it's dinner right now!
            menu.addAll(dinnerMenu);
            menu.addAll(alwaysMenu);
        }
        else {
            // take care of this case
            menu.addAll(alwaysMenu);
        }

        return menu;
    }
}
