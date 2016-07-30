package dekk.pw.pokemate.tasks;

import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import dekk.pw.pokemate.Config;
import dekk.pw.pokemate.Context;
import dekk.pw.pokemate.PokeMateUI;
import dekk.pw.pokemate.Walking;

import java.util.ArrayList;

import static dekk.pw.pokemate.util.StringConverter.convertItemAwards;

/**
 * Created by TimD on 7/21/2016.
 */
public class TagPokestop extends Task implements Runnable{

    private static final int retryAmount = 50;

    TagPokestop(final Context context) {
        super(context);
    }

    @Override
    public void run() {
        try {
            MapObjects map = context.getApi().getMap().getMapObjects();
            ArrayList<Pokestop> pokestops = new ArrayList<>(map.getPokestops());
            if (pokestops.size() == 0) {
                return;
            }

            pokestops.stream()
                    .filter(Pokestop::canLoot)
                    .forEach(near -> {
                        Walking.setLocation(context);
                        try {
                             /* Softban Bypass */
                            PokestopLootResult result;
                            result = near.loot();
                            PokeMateUI.toast(resultMessage(result), Config.POKE + "Stop interaction!", "icons/pokestop.png");
                            switch (result.getResult()) {
                                case SUCCESS:
                                case INVENTORY_FULL:
                                    if (result.getExperience() == 0 && !Config.isSoftbanBypass()) { //Softbanned

                                        context.getWalking().set(false);
                                        PokeMateUI.toast("Softbanned! Bypassing..", Config.POKE + "Stop interaction!", "icons/pokestop.png");
                                        for (int i = 0; i<retryAmount; i++) {
                                            result = near.loot();
                                            if (result.getExperience() > 0) {
                                                PokeMateUI.toast("No longer softbanned", Config.POKE + "Stop interaction!", "icons/pokestop.png");
                                                break;
                                            }
                                        }
                                    }
                                    context.getWalking().set(true);
                                    break;
                            }
                        } catch (LoginFailedException | RemoteServerException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (LoginFailedException | RemoteServerException e) {
            e.printStackTrace();
        }
    }

    private String resultMessage(final PokestopLootResult result) {
        switch (result.getResult()) {
            case SUCCESS:
                String retstr = "Tagged pokestop [+" + result.getExperience() + "xp]";
                retstr += convertItemAwards(result.getItemsAwarded());
                return retstr;
            case INVENTORY_FULL:
                return "Tagged pokestop, but bag is full [+" + result.getExperience() + "xp]";
            case OUT_OF_RANGE:
                return "[CRITICAL]: COULD NOT TAG POKESTOP BECAUSE IT WAS OUT OF RANGE";
            case IN_COOLDOWN_PERIOD:
                return "[CRITICAL]: COULD NOT TAG POKESTOP BECAUSE IT WAS IN COOLDOWN";
            default:
                return "Failed Pokestop";
        }
    }

}
