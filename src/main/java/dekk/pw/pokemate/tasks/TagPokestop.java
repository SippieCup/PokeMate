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
import dekk.pw.pokemate.util.Time;

import java.util.ArrayList;

import static dekk.pw.pokemate.util.StringConverter.convertItemAwards;
import static dekk.pw.pokemate.util.Time.sleep;

/**
 * Created by TimD on 7/21/2016.
 */
public class TagPokestop extends Task implements Runnable {
    
    TagPokestop(final Context context) {
        super(context);
    }

    @Override
    public void run() {
        MapObjects map = null;
        while (context.getRunStatus()) {
            System.out.println("[Tag Pokestop] Starting Loop");
            try {
                context.APILock.attempt(1000);
                APIStartTime = System.currentTimeMillis();
                map = context.getApi().getMap().getMapObjects();
                APIElapsedTime = System.currentTimeMillis() - APIStartTime;
                if (APIElapsedTime < context.getMinimumAPIWaitTime()) {
                    sleep(context.getMinimumAPIWaitTime() - APIElapsedTime);
                }
            } catch (RemoteServerException e) {
                System.out.println("[Tag PokeStop] Ending Loop - Exceeded Rate Limit While PokeStops ");

            } catch (InterruptedException e) {
                System.out.println("[Tag PokeStop] Ending Loop - Interrupted");
                e.printStackTrace();

            } catch (LoginFailedException e) {
                //e.printStackTrace();
                System.out.println("[Tag PokeStop] Ending Loop - Login Failed");
            } finally {
                context.APILock.release();
            }
            ArrayList<Pokestop> pokestops = new ArrayList<>(map.getPokestops());
            if (pokestops.size() == 0) {
                System.out.println("[Tag PokeStop] Ending Loop - No Stops Found");
                continue;
            }
            System.out.println("[Tag PokeStop] " + pokestops.size() + " Pokestops Found.. Tagging");

            try {
                context.APILock.attempt(1000);
                pokestops.stream()
                .filter(Pokestop::canLoot)
                .forEach(near -> {
                    Walking.setLocation(context);
                    System.out.println("[Tag PokeStop] Tagging PokeStop in range");
                    String result = null;
                    APIStartTime = System.currentTimeMillis();
                    try {
                        result = resultMessage(near.loot());
                    } catch (LoginFailedException e) {
                        System.out.println("[Tag PokeStop] Ending Loop - Login Failed");
                    } catch (RemoteServerException e) {
                        System.out.println("[Tag PokeStop] Exceeded Rate Limit While looting");
                    }
                    APIElapsedTime = System.currentTimeMillis() - APIStartTime;
                    if (APIElapsedTime < context.getMinimumAPIWaitTime()) {
                        sleep(context.getMinimumAPIWaitTime() - APIElapsedTime);
                    }
                    PokeMateUI.toast(result, Config.POKE + "Stop interaction!", "icons/pokestop.png");
                });
            } catch (InterruptedException e) {
                System.out.println("[Tag PokeStop] Ending Loop - Interrupted");
                //e.printStackTrace();
            } finally {
                System.out.println("[Tag PokeStop] Ending Loop");
                context.APILock.release();
                Time.sleep(500);
            }
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
