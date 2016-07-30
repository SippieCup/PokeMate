package dekk.pw.pokemate.tasks;

import com.pokegoapi.api.map.pokemon.EvolutionResult;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import dekk.pw.pokemate.Config;
import dekk.pw.pokemate.Context;
import dekk.pw.pokemate.PokeMateUI;
import dekk.pw.pokemate.util.StringConverter;
import dekk.pw.pokemate.util.Time;
import javafx.scene.image.Image;

import java.io.DataInputStream;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import static dekk.pw.pokemate.util.Time.sleep;

/**
 * Created by TimD on 7/22/2016.
 */
public class EvolvePokemon extends Task implements Runnable {
    private static final ConcurrentHashMap<Integer, Integer> CANDY_AMOUNTS = new ConcurrentHashMap<>();

    static {
        try {
            //We will read in from the compacted file...
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            DataInputStream dis = new DataInputStream(classloader.getResourceAsStream("evolve.dat"));
            int count = dis.readInt();
            for (int i = 0; i < count; i++) {
                CANDY_AMOUNTS.put(dis.readInt(), dis.readInt());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    EvolvePokemon(final Context context) {
        super(context);
    }

    @Override
    public void run() {
        int i=0;
        System.out.println("[Evolve] Activating..");
        while(context.getRunStatus()) {
            if (i==10) { System.out.println("[Evolve] Active"); i=0; }
            i++;
            try {
                context.APILock.attempt(1000);
                APIStartTime = System.currentTimeMillis();
                CopyOnWriteArrayList<Pokemon> pokeList = new CopyOnWriteArrayList<>(context.getApi().getInventories().getPokebank().getPokemons());
                APIElapsedTime = System.currentTimeMillis() - APIStartTime;
                if (APIElapsedTime < context.getMinimumAPIWaitTime()) {
                    sleep(context.getMinimumAPIWaitTime() - APIElapsedTime);
                }
                context.APILock.release();
                for (Pokemon pokemon : pokeList)
                    if (!Config.isWhitelistEnabled() || Config.getWhitelistedPokemon().contains(pokemon.getPokemonId().getNumber())) {
                        int number = pokemon.getPokemonId().getNumber();
                        if (CANDY_AMOUNTS.containsKey(number)) {
                            int required = CANDY_AMOUNTS.get(number);
                            if (required < 1) continue;
                            if (pokemon.getCandy() >= required) {
                                EvolutionResult result = pokemon.evolve();
                                if (result != null && result.isSuccessful()) {
                                    String evolutionresult = StringConverter.titleCase(pokemon.getPokemonId().name()) + " has evolved into " + StringConverter.titleCase(result.getEvolvedPokemon().getPokemonId().name()) + " costing " + required + " candies";
                                    PokeMateUI.toast(evolutionresult, Config.POKE + "mon evolved!", "icons/" + pokemon.getPokemonId().getNumber() + ".png");
                                }
                            }
                        }
                    }
            } catch (RemoteServerException | LoginFailedException e1) {
                System.out.println("[EvolvePokemon] Hit Rate Limited");
                e1.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("[] Error - Timed out waiting for API");
                // e.printStackTrace();
            }
        }
    }
}
