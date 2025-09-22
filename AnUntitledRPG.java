// AnUntitledRPG.java 

import java.util.*;
import java.text.SimpleDateFormat;

public class AnUntitledRPG {
    // ======= Core tools & state =======
    private static final Scanner IN = new Scanner(System.in);
    private static final Random RNG = new Random();
    private static final SimpleDateFormat TS = new SimpleDateFormat("HH:mm:ss");

    // Player state
    private static int hp = 20;
    private static final int MAX_HP = 20;
    private static int ruthless = 0;
    private static int merciful = 0;
    private static int notoriety = 0; // world reaction / decay

    // Inventory & journal
    private static final Map<String,Integer> inventory = new LinkedHashMap<>();
    private static final List<String> journal = new ArrayList<>();

    // Hub & flags
    private static String hub = "Town Square";
    private static final Map<String,Boolean> visited = new HashMap<>();
    private static final Map<String,Boolean> choiceMade = new HashMap<>();
    private static final Map<String,Boolean> npcFlags = new HashMap<>();

    // used item flags
    private static boolean usedHerbFlag = false;
    private static boolean usedShardFlag = false;

    // Final outcome
    private static String finalOutcome = ""; // spare / end / death / quit / ambiguous

    // pacing (0 fastest)
    private static final int PAUSE = 0;

    // ======= Entry point =======
    public static void main(String[] args) {
        setup();
        Art.banner();
        showPrologue();
        hubLoop();
        showEpilogue();
        printJournalFull();
        finalLine();
    }

    // ======= Setup =======
    private static void setup() {
        inventory.put("Herb", 1);
        inventory.put("Ration", 2);
        inventory.put("Shard", 0);

        // NPC flags initial
        npcFlags.put("Marnie_helped", false);
        npcFlags.put("Marnie_pruned", false);
        npcFlags.put("Pebble_trusted", false);
        npcFlags.put("Pebble_hurt", false);
        npcFlags.put("Traveler_robbed", false);
        npcFlags.put("Trader_helped", false);
        npcFlags.put("Library_burned", false);
        npcFlags.put("Guard_bribed", false);
        npcFlags.put("Guard_spared", false);
        npcFlags.put("BanditLeader_spared", false);
        npcFlags.put("BanditLeader_killed", false);
        npcFlags.put("Child_saved", false);

        // visited
        visited.put("Town Square", false);
        visited.put("Marnie's Farm", false);
        visited.put("Market Lane", false);
        visited.put("Old Library", false);
        visited.put("Bridge", false);
        visited.put("Guard Gate", false);
        visited.put("Bandit Camp", false);
        visited.put("Traveler Road", false);
        visited.put("Clock Hedge", false);
    }

    // ======= Small utilities =======
    private static void println(String s) { System.out.println(s); }
    private static void println() { System.out.println(); }
    private static void print(String s) { System.out.print(s); }
    private static void pause(int ms) { if (PAUSE <= 0) return; try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
    private static String input() {
        try { return IN.nextLine(); } catch (NoSuchElementException | IllegalStateException e) { return ""; }
    }
    private static void waitEnter() {
        println("\n(Press Enter to continue)");
        input();
    }
    private static void addJournal(String entry) {
        journal.add(0, "[" + TS.format(new Date()) + "] " + entry);
        if (journal.size() > 300) journal.remove(journal.size()-1);
    }
    private static void adjustNotoriety(int d) {
        notoriety += d;
        if (notoriety < 0) notoriety = 0;
    }
    private static String inventoryToString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String,Integer> e : inventory.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(e.getKey()).append(" x").append(e.getValue());
            first = false;
        }
        return sb.toString();
    }
    private static void showStatus() {
        println("\n--- STATUS ---");
        println("HP: " + hp + "/" + MAX_HP);
        println("Ruthless: " + ruthless + "  Merciful: " + merciful + "  Notoriety: " + notoriety);
        println("Inventory: " + inventoryToString());
        println("----------------\n");
    }

    // ======= Prologue & title =======
    private static void showPrologue() {
        println("\nWelcome. You wake in a town that is easy to love at first glance.");
        Art.sceneTown();
        println("Bells sound in the square, bread is baked nearby, and people hum their small routines.");
        println("You remember a phrase once heard but ignored: 'Ruthlessness is mercy upon ourselves.'");
        addJournal("Awoke in the town. Heard a phrase once and ignored it.");
        waitEnter();
    }

    // ======= Main hub loop (simplified menus) =======
    private static void hubLoop() {
        while (true) {
            if (shouldForceDescent()) {
                println("\nThe bell's tone deepens. Windows close. People hurry along private routes.");
                waitEnter();
                mirrorDescent();
                break;
            }

            println("\n==== HUB: " + hub + " ====");
            if (!visited.getOrDefault(hub,false)) {
                visited.put(hub, true);
                addJournal("Visited hub: " + hub);
            }

            // show hub description (non-blocking)
            switch (hub) {
                case "Town Square": showTownSquare(); break;
                case "Marnie's Farm": showMarnie(); break;
                case "Market Lane": showMarket(); break;
                case "Old Library": showLibrary(); break;
                case "Bridge": showBridge(); break;
                case "Guard Gate": showGuard(); break;
                case "Bandit Camp": showBanditCamp(); break;
                case "Traveler Road": showTraveler(); break;
                case "Clock Hedge": showClockBeast(); break;
                default: showTownSquare(); break;
            }

            // simplified main menu
            println("Choose an action:");
            println("  1) Travel");
            println("  2) Interact");
            println("  3) Explore");
            println("  4) Status");
            println("  5) Journal");
            println("  6) Help");
            println("  7) Quit");
            print("> ");
            String cmd = input().trim().toLowerCase();

            if (cmd.equals("1") || cmd.equals("travel")) {
                showTravelMenu();
            } else if (cmd.equals("2") || cmd.equals("interact")) {
                showInteractMenu();
            } else if (cmd.equals("3") || cmd.equals("explore")) {
                showExploreMenu();
            } else if (cmd.equals("4") || cmd.equals("status")) {
                showStatus();
            } else if (cmd.equals("5") || cmd.equals("journal")) {
                printJournalRecent();
            } else if (cmd.equals("6") || cmd.equals("help")) {
                showHelp();
            } else if (cmd.equals("7") || cmd.equals("quit") || cmd.equals("exit")) {
                finalOutcome = "quit"; addJournal("Player quit the game."); println("You leave the town; the bell keeps time without you."); break;
            } else {
                // allow direct words for convenience (e.g., "market" to travel)
                if (cmd.isEmpty()) println("Enter a number or word for the action.");
                else {
                    // try interpret quick travel words
                    travelTo(cmd);
                }
            }
        }
    }

    // travel submenu
    private static void showTravelMenu() {
        List<String> dests = Arrays.asList("Town Square","Marnie's Farm","Market Lane","Old Library","Bridge","Guard Gate","Bandit Camp","Traveler Road","Clock Hedge");
        println("\nTravel - pick destination:");
        for (int i = 0; i < dests.size(); i++) println("  " + (i+1) + ") " + dests.get(i));
        println("  0) Cancel");
        print("> ");
        String s = input().trim();
        if (s.equals("0") || s.equalsIgnoreCase("cancel")) return;
        try {
            int pick = Integer.parseInt(s);
            if (pick >= 1 && pick <= dests.size()) {
                hub = dests.get(pick-1);
                return;
            }
        } catch (NumberFormatException ignored) {}
        // fallback to text match
        travelTo(s);
    }

    // interact submenu (shows available NPCs in current hub)
    private static void showInteractMenu() {
        List<String> options = new ArrayList<>();
        switch (hub) {
            case "Marnie's Farm": options.add("Marnie"); break;
            case "Town Square": options.add("Pebble"); break;
            case "Market Lane": options.add("Trader"); break;
            case "Old Library": options.add("Scribe"); break;
            case "Guard Gate": options.add("Guard"); break;
            case "Bandit Camp": options.add("Bandit Leader"); break;
            default: /* none */ break;
        }
        if (options.isEmpty()) {
            println("There's no one directly to interact with here.");
            return;
        }
        println("\nInteract - pick a person:");
        for (int i = 0; i < options.size(); i++) println("  " + (i+1) + ") " + options.get(i));
        println("  0) Cancel");
        print("> ");
        String s = input().trim();
        if (s.equals("0") || s.equalsIgnoreCase("cancel")) return;
        try {
            int pick = Integer.parseInt(s);
            if (pick >= 1 && pick <= options.size()) {
                interact(options.get(pick-1));
                return;
            }
        } catch (NumberFormatException ignored) {}
        interact(s);
    }

    // explore submenu (context-sensitive)
    private static void showExploreMenu() {
        List<String> opts = new ArrayList<>();
        if (hub.equals("Town Square")) {
            opts.add("Under the bell");
            opts.add("Clock Hedge");
            opts.add("Meadow");
        } else if (hub.equals("Marnie's Farm")) {
            opts.add("Garden");
            opts.add("Meadow");
        } else if (hub.equals("Market Lane")) {
            opts.add("Stalls");
            opts.add("Back alley");
        } else if (hub.equals("Old Library")) {
            opts.add("Shelves");
            opts.add("Basement");
        } else if (hub.equals("Bridge")) {
            opts.add("River bank");
            opts.add("Bridge");
        } else {
            opts.add("Look around");
        }
        println("\nExplore - pick:");
        for (int i = 0; i < opts.size(); i++) println("  " + (i+1) + ") " + opts.get(i));
        println("  0) Cancel");
        print("> ");
        String s = input().trim();
        if (s.equals("0") || s.equalsIgnoreCase("cancel")) return;
        try {
            int pick = Integer.parseInt(s);
            if (pick >= 1 && pick <= opts.size()) {
                explore(opts.get(pick-1));
                return;
            }
        } catch (NumberFormatException ignored) {}
        explore(s);
    }

    private static boolean shouldForceDescent() {
        return notoriety >= 10;
    }

    private static void showHelp() {
        println("\n--- HELP ---");
        println("Use the simple menus: choose a number or type a short word.");
        println("Travel: choose where to go.");
        println("Interact: talk/act with people present.");
        println("Explore: search nearby features.");
        println("Status: show stats.");
        println("Journal: show recent entries.");
        println("Quit: exit the game.\n");
    }

    private static void travelTo(String dest) {
        dest = dest.toLowerCase();
        if (dest.contains("marn")) hub = "Marnie's Farm";
        else if (dest.contains("market")) hub = "Market Lane";
        else if (dest.contains("library")) hub = "Old Library";
        else if (dest.contains("bridge") && !dest.contains("broken")) hub = "Bridge";
        else if (dest.contains("guard")) hub = "Guard Gate";
        else if (dest.contains("bandit")) hub = "Bandit Camp";
        else if (dest.contains("traveler") || dest.contains("road")) hub = "Traveler Road";
        else if (dest.contains("clock") || dest.contains("hedge")) hub = "Clock Hedge";
        else if (dest.contains("town") || dest.contains("square")) hub = "Town Square";
        else println("Unknown destination: " + dest);
    }

    private static void interact(String who) {
        who = who.toLowerCase();
        if ((who.contains("marn") || who.contains("marnie")) ) marnieInteract();
        else if (who.contains("pebble")) pebbleInteract();
        else if (who.contains("trader")) traderInteract();
        else if (who.contains("scribe")) scribeInteract();
        else if (who.contains("guard")) guardInteract();
        else if (who.contains("bandit") || who.contains("leader")) banditLeaderInteract();
        else println("No one by that name exists here or they are unavailable.");
    }

    private static void explore(String what) {
        what = what.toLowerCase();
        if (what.contains("garden") || what.contains("meadow")) meadowExplore();
        else if (what.contains("stalls") || what.contains("market")) showMarket();
        else if (what.contains("under") || what.contains("descent") || what.contains("bell")) {
            println("You find a stair that leads beneath the bell, but you step back for now.");
            addJournal("Discovered stair under bell but resisted descent.");
        } else {
            println("You search " + what + " but find nothing notable.");
        }
    }

    // ======= Scenes / NPCs =======

    // Town Square (hub hub)
    private static void showTownSquare() {
        Art.square();
        println("You stand beneath the bell. People pass by with small, familiar motions.");
        println("Nearby: Marnie's Farm | Market Lane | Old Library | Bridge | Guard Gate");
        addJournal("Stood under the bell in the Town Square.");
    }

    // Marnie's Farm (practical)
    private static void showMarnie() {
        Art.marnieFarm();
        println("Marnie works as though she reads the weather in the plants. She speaks plainly.");
        addJournal("Approached Marnie's Farm.");
    }

    private static void marnieInteract() {
        println("\nMarnie regards you with soil-darkened hands.");
        if (!choiceMade.getOrDefault("Marnie_choice", false)) {
            println("She says: 'The sick row must be pruned.'");
            println("  1) Help fix irrigation (kind)");
            println("  2) Prune the sick row (hard)");
            println("  3) Walk away");
            print("> ");
            String c = input().trim().toLowerCase();
            if (c.equals("1") || c.contains("help")) {
                println("You grease the wheel and steady the stream. Marnie gives you a ration.");
                inventory.put("Ration", inventory.getOrDefault("Ration",0)+1);
                merciful++; addJournal("Helped Marnie; gained a Ration.");
                npcFlags.put("Marnie_helped", true);
                adjustNotoriety(-1);
            } else if (c.equals("2") || c.contains("prune") || c.contains("thin")) {
                println("You thin the sick plants. They are cut, and the smell is sharp and important.");
                ruthless++; addJournal("Pruned plants with Marnie; uneasy feeling.");
                npcFlags.put("Marnie_pruned", true);
                adjustNotoriety(2);
            } else {
                println("You leave the farm. Marnie watches you go.");
                addJournal("Left Marnie's farm without acting.");
            }
            choiceMade.put("Marnie_choice", true);
        } else {
            if (npcFlags.getOrDefault("Marnie_helped", false)) println("Marnie hums and offers a small cup of tea.");
            else if (npcFlags.getOrDefault("Marnie_pruned", false)) println("Marnie moves with a narrow calm about her.");
            else println("Marnie tends the rows and nods at you.");
        }
        waitEnter();
    }

    // Pebble (child)
    private static void pebbleInteract() {
        Art.pebble();
        println("Pebble stacks stones into little families. He looks up with a bright, trusting face.");
        if (!choiceMade.getOrDefault("Pebble_choice", false)) {
            println("  1) Teach a game (kind)");
            println("  2) Knock his tower (cruel)");
            println("  3) Slip a bead into your pocket (sly)");
            print("> ");
            String c = input().trim().toLowerCase();
            if (c.equals("1") || c.contains("teach") || c.contains("game")) {
                println("You teach a counting rhyme. Pebble laughs and gives you a carved bead.");
                inventory.put("Shard", inventory.getOrDefault("Shard",0)+1);
                merciful++; addJournal("Taught Pebble a game; received a bead.");
                npcFlags.put("Pebble_trusted", true);
            } else if (c.equals("2") || c.contains("knock") || c.contains("cruel")) {
                println("You flick the stack. Pebble cries and his trust dims.");
                ruthless++; addJournal("Knocked Pebble's stack; trust broken.");
                npcFlags.put("Pebble_hurt", true);
                adjustNotoriety(2);
            } else if (c.equals("3") || c.contains("take") || c.contains("sly")) {
                println("You pocket a bead while Pebble stares at a bug. Later he notices and cries.");
                inventory.put("Shard", inventory.getOrDefault("Shard",0)+1);
                ruthless++; addJournal("Stole Pebble's bead; felt small cold.");
                adjustNotoriety(1);
            } else {
                println("Pebble keeps stacking stones.");
            }
            choiceMade.put("Pebble_choice", true);
        } else {
            if (npcFlags.getOrDefault("Pebble_trusted", false)) println("Pebble waves and shows you a new small tower.");
            else if (npcFlags.getOrDefault("Pebble_hurt", false)) println("Pebble keeps the stones low now, watching people more warily.");
            else println("Pebble continues his careful stacking.");
        }
        waitEnter();
    }

    // Market & Trader
    private static void showMarket() {
        Art.market();
        println("Stalls cluster like small islands. Bread, trinkets, and whispered rumors exchange hands here.");
        addJournal("Entered the Market Lane.");
    }

    private static void traderInteract() {
        Art.trader();
        println("The trader is plump, smiling, hands always busy.");
        if (!npcFlags.getOrDefault("Trader_helped", false)) {
            println("He asks: 'Carry a small package for me?' (yes/no)");
            print("> ");
            String c = input().trim().toLowerCase();
            if (c.equals("yes") || c.equals("y")) {
                println("You return with the package delivered. He gives you a map-scrap.");
                npcFlags.put("Trader_helped", true);
                merciful++; addJournal("Delivered trader's package; received a map-scrap.");
            } else {
                println("He shrugs and offers a trinket for a ration instead.");
                if (inventory.getOrDefault("Ration",0) > 0) {
                    inventory.put("Ration", inventory.get("Ration") - 1);
                    addJournal("Bought trinket from trader for a ration.");
                } else addJournal("Declined trader's favor; no purchase.");
            }
        } else {
            println("The trader winks; 'Good to see you again.'");
        }
        waitEnter();
    }

    // ======= More scenes & interactions =======

    // Library & Scribe
    private static void showLibrary() {
        Art.library();
        println("Shelves lean like tired spines. A lone scribe writes and the words sometimes fall away.");
        addJournal("Entered the Old Library.");
        waitEnter();
    }

    private static void scribeInteract() {
        Art.scribe();
        println("The scribe's pen moves like someone clearing cobwebs from a window.");
        if (!choiceMade.getOrDefault("Scribe_choice", false)) {
            println("He murmurs: 'Intent shapes consequence.' Options: (1) ask about intent  (2) test him about burning pages  (3) leave");
            print("> ");
            String c = input().trim().toLowerCase();
            if (c.equals("1") || c.contains("ask") || c.contains("intent")) {
                println("'Comfort that hides cost breeds new cost,' he says. 'Be wary of easy repairs.'");
                addJournal("Spoke with the scribe about intent and hidden cost.");
            } else if (c.equals("2") || c.contains("burn") || c.contains("test")) {
                println("You ask whether burning knowledge is ever kind. He stares and replies: 'Sometimes heat reveals what honesty hides.'");
                addJournal("Questioned the scribe about burning pages.");
            } else {
                println("You step away from the scribe's vanishing ink.");
                addJournal("Left the scribe without pressing.");
            }
            choiceMade.put("Scribe_choice", true);
        } else {
            println("The scribe keeps writing. Ink lifts and falls as if breathing.");
        }
        waitEnter();
    }

    // Bridge scene (child keepsake)
    private static void showBridge() {
        Art.bridge();
        println("A wooden bridge creaks. On the bank a child sobs: a keepsake slipped into the cold water.");
        addJournal("Reached the Broken Bridge; a child's keepsake lost.");
    }

    private static void bridgeScene() {
        showBridge();
        if (!choiceMade.getOrDefault("Bridge_choice", false)) {
            println("Options: (1) dive to retrieve (2) throw rope (3) take keepsake while they cry (4) leave");
            print("> ");
            String c = input().trim().toLowerCase();
            if (c.equals("1") || c.contains("dive")) {
                println("You dive and fish the keepsake from the current. The child hugs you and laughs wetly.");
                merciful++; npcFlags.put("Child_saved", true); addJournal("Dove for the child's keepsake and saved them.");
                adjustNotoriety(-1);
            } else if (c.equals("2") || c.contains("throw") || c.contains("rope")) {
                println("You toss a rope and pull the keepsake back. The child cheers and hides it close to their chest.");
                merciful++; addJournal("Threw rope and saved the child's keepsake.");
            } else if (c.equals("3") || c.contains("take") || c.contains("steal")) {
                println("You slip the keepsake into your pocket while the child cries. A woman glares and the air grows colder.");
                ruthless++; adjustNotoriety(2); addJournal("Stole the child's keepsake; felt colder afterward.");
            } else {
                println("You leave the child and the river to their own small grief.");
                addJournal("Left the bridge without helping.");
            }
            choiceMade.put("Bridge_choice", true);
        } else {
            if (npcFlags.getOrDefault("Child_saved", false)) println("The child waves from a window, the keepsake safe.");
            else println("The river flows on and the bridge creaks as before.");
        }
        waitEnter();
    }

    // Guard gate (lawful sentinel)
    private static void showGuard() {
        Art.guard();
        println("A sentinel in dull steel stands at the gate, expression set into habit.");
        addJournal("Approached the Guard Gate.");
    }

    private static void guardInteract() {
        showGuard();
        if (!choiceMade.getOrDefault("Guard_choice", false)) {
            println("She says: 'We keep lists beneath the bell.' Options: (1) bribe (2) plead mercy (3) fight (4) leave");
            print("> ");
            String c = input().trim().toLowerCase();
            if (c.equals("1") || c.contains("bribe") || c.contains("coin")) {
                println("You slip a coin. She tucks it away and steps aside with a practiced look.");
                npcFlags.put("Guard_bribed", true); ruthless++; adjustNotoriety(1); addJournal("Bribed the gate guard.");
            } else if (c.equals("2") || c.contains("plead") || c.contains("talk")) {
                println("You ask her to pass you on mercy grounds. She listens, then says, 'Words matter.' and lets you by.");
                npcFlags.put("Guard_spared", true); merciful++; addJournal("Pleaded with guard and passed.");
            } else if (c.equals("3") || c.contains("fight") || c.contains("attack")) {
                println("You fight. The sentinel collapses and the gate opens through absence of a keeper.");
                fightMini("Gate Sentinel", 14, 4);
                ruthless++; adjustNotoriety(2); addJournal("Fought the guard and forced passage.");
            } else {
                println("You step away from the gate.");
            }
            choiceMade.put("Guard_choice", true);
        } else {
            if (npcFlags.getOrDefault("Guard_bribed", false)) println("The guard meets your eyes less and looks away.");
            else if (npcFlags.getOrDefault("Guard_spared", false)) println("She nods as though you had done a small right.");
            else println("She stands and does her duty.");
        }
        waitEnter();
    }

    // Bandit camp
    private static void showBanditCamp() {
        Art.bandit();
        println("Tents rustle like mouths. The bandits' laughter feels like knives warming.");
        addJournal("Approached bandit camp.");
    }

    private static void banditCampScene() {
        showBanditCamp();
        if (!choiceMade.getOrDefault("Bandit_choice", false)) {
            println("Options: (1) rescue trader (2) attack camp (3) negotiate (4) set trap (5) leave");
            print("> ");
            String c = input().trim().toLowerCase();
            if (c.equals("1") || c.contains("rescue")) {
                println("You sneak and free a trader tied to a post. He thanks you, voice shaking.");
                merciful++; adjustNotoriety(-1); addJournal("Rescued trader from bandit camp.");
            } else if (c.equals("2") || c.contains("attack") || c.contains("fight")) {
                println("You attack. Violence erupts and the leader falls begging for mercy.");
                fightMini("Bandit Leader", 18, 5);
                ruthless += 2; adjustNotoriety(3); addJournal("Attacked bandit camp; many fell.");
                if (!choiceMade.getOrDefault("BanditLeader_final", false)) {
                    println("Leader bleeds: 'Spare me, end me, or ask me to leave.' (spare/kill/question)");
                    print("> ");
                    String d = input().trim().toLowerCase();
                    if (d.startsWith("spare")) {
                        println("You spare him. He limps away, plans folding quietly in his head.");
                        merciful++; npcFlags.put("BanditLeader_spared", true); addJournal("Spared the bandit leader.");
                    } else if (d.startsWith("kill") || d.contains("end")) {
                        println("You end him. His eyes go glassy and the camp smells of cold smoke.");
                        ruthless++; npcFlags.put("BanditLeader_killed", true); adjustNotoriety(2); addJournal("Executed the bandit leader.");
                    } else {
                        println("You question him and let him go with a debt to remember.");
                        addJournal("Questioned the bandit leader and let him live.");
                    }
                    choiceMade.put("BanditLeader_final", true);
                }
            } else if (c.equals("3") || c.contains("negotiate") || c.contains("talk")) {
                println("You negotiate a deal. The bandits leave the trader but eye you differently.");
                merciful++; adjustNotoriety(-1); addJournal("Negotiated with bandits; uneasy peace.");
            } else if (c.equals("4") || c.contains("trap") || c.contains("set")) {
                println("You set a trap that later catches a pair of bandits. The rest scatter into night.");
                ruthless++; adjustNotoriety(1); addJournal("Set a trap at bandit camp; took captives.");
            } else {
                println("You leave the tents to their dark songs.");
            }
            choiceMade.put("Bandit_choice", true);
        } else {
            println("The camp hums with the echo of what you did before.");
        }
        waitEnter();
    }

    private static void banditLeaderInteract() {
        println("The leader is not present now unless you forced a confrontation earlier.");
    }

    // Clock Hedge
    private static void showClockBeast() {
        Art.clockBeast();
        println("A creature of gears and tiny faces nests in the hedge.");
        waitEnter();
    }

    // ======= Missing-ish helper scenes =======
    private static void showTraveler() {
        Art.traveler();
        println("A lone traveler sits on a bench, polishing boots and breathing in road-smoke stories.");
        addJournal("Encountered a traveler on the road.");
        waitEnter();
    }

    private static void meadowExplore() {
        Art.sceneTown(); // reuse town art as meadow-ish fallback
        println("You wander into a quiet meadow. The air smells of grass and distant ovens.");
        println("You find a small scrap of something — nothing bright, but memory-friendly.");
        addJournal("Explored a nearby meadow and found a small scrap.");
        if (RNG.nextInt(100) < 18) {
            println("Under a tuft of grass you find a small carved shard.");
            inventory.put("Shard", inventory.getOrDefault("Shard",0)+1);
            addJournal("Found a Shard in the meadow.");
        }
        waitEnter();
    }

    // fightMini - simple combat helper used by guard and bandits
    private static void fightMini(String enemyName, int enemyHp, int enemyAtk) {
        println("\n--- COMBAT: " + enemyName + " ---");
        int eHp = enemyHp;
        while (eHp > 0 && hp > 0) {
            println("\nYou: " + hp + " HP   |   " + enemyName + ": " + eHp + " HP");
            println("Options: strike | flee | status");
            print("> ");
            String cmd = input().trim().toLowerCase();
            if (cmd.equals("status")) { showStatus(); continue; }
            if (cmd.equals("flee") || cmd.equals("run")) {
                if (RNG.nextInt(100) < 50) {
                    println("You slip away while the enemy blinks. Flee successful.");
                    addJournal("Fled from combat with " + enemyName + ".");
                    return;
                } else {
                    println("You fail to get away; the enemy strikes as you turn.");
                    int ed = Math.max(1, enemyAtk - 1 + RNG.nextInt(3));
                    println(enemyName + " hits you for " + ed + " damage.");
                    hp -= ed;
                    if (hp <= 0) break;
                    continue;
                }
            }
            if (cmd.startsWith("strike") || cmd.startsWith("attack") || cmd.equals("fight")) {
                int dmg = 3 + RNG.nextInt(4);
                println("You strike and deal " + dmg + " damage.");
                eHp -= dmg;
                addJournal("Hit " + enemyName + " for " + dmg + " damage.");
            } else {
                println("Hesitation costs you a small wound.");
                hp -= 1;
            }
            // enemy turn if alive
            if (eHp > 0) {
                int ed = Math.max(1, enemyAtk - 1 + RNG.nextInt(3));
                println(enemyName + " strikes back for " + ed + " damage.");
                hp -= ed;
            }
        }
        if (hp <= 0) {
            println("\nYou slump. " + enemyName + " stands over you.");
            addJournal("Fell in combat against " + enemyName + ".");
            finalOutcome = "death";
            hp = 0;
            return;
        }
        if (eHp <= 0) {
            println("\n" + enemyName + " falls. The fight ends.");
            addJournal("Defeated " + enemyName + " in combat.");
        }
    }

    // ======= Mirror descent & final (mirror wounds mirrored to player) =======
    private static void mirrorDescent() {
        println("\nYou take the stair under the bell. Each step presses memory into your chest.");
        addJournal("Descended under the bell toward the Mirror Chamber.");
        waitEnter();
        mirrorChamber();
    }

    private static void mirrorChamber() {
        Art.mirror();
        println("A bowl of black water reflects you, then steps out wearing your face.");
        addJournal("Entered Mirror Chamber; reflection manifest.");
        int mirrorHp = 20 + Math.max(0, ruthless - merciful);
        int pHp = hp;

        // show a few memory lines for atmosphere
        int n = Math.min(6, journal.size());
        if (n > 0) {
            println("\nMemory fragments float around:");
            for (int i = 0; i < n; i++) {
                println("  " + journal.get(i));
                pause(90);
            }
        }
        waitEnter();
        println("'You said you didn't believe hard things. Do you still not believe them?' your face asks.");
        waitEnter();

        while (pHp > 0 && mirrorHp > 0) {
            println("\nYou: " + pHp + " HP | Mirror: " + mirrorHp + " HP");
            println("Commands: strike / mercy / act <word> / item <name> / recall / status");
            print("> ");
            String raw = input().trim();
            String cmd = raw.toLowerCase();

            if (cmd.equals("status")) { showStatus(); continue; }
            if (cmd.equals("recall")) {
                if (!journal.isEmpty()) {
                    String mem = journal.get(RNG.nextInt(journal.size()));
                    println("You recall: " + mem);
                    if (RNG.nextInt(100) < 50) {
                        println("The memory steadies you. +3 HP.");
                        pHp = Math.min(MAX_HP, pHp + 3);
                    } else {
                        println("It pricks. -1 HP.");
                        pHp -= 1;
                    }
                } else println("Your memories feel thin.");
                continue;
            }

            if (cmd.startsWith("strike") || cmd.startsWith("attack") || cmd.equals("fight")) {
                int dmg = 3 + RNG.nextInt(6);
                println("You strike and deal " + dmg + " damage to the mirror.");
                mirrorHp -= dmg;

                // NEW: every wound you inflict on the mirror appears on your own skin
                pHp -= dmg;
                println("A wound blooms on your skin where the mirror took the blow. You take " + dmg + " damage.");
                addJournal("Struck the mirror for " + dmg + " damage and suffered the wound.");

                ruthless++; adjustNotoriety(1);
                maybeCorruptJournalByViolence();
            } else if (cmd.startsWith("mercy")) {
                println("You lower your weapon. The mirror looks at your hands.");
                if (merciful > ruthless && RNG.nextInt(100) < 70) {
                    int down = 2 + RNG.nextInt(2);
                    println("Mercy finds root; the mirror weakens. -"+ down +" HP to mirror.");
                    mirrorHp = Math.max(0, mirrorHp - down);
                    // mirror wound mirrored to you
                    pHp -= down;
                    println("The mirror's softening cuts you as well — you take " + down + " damage.");
                    addJournal("Mercy affected the mirror; took " + down + " reflected damage.");
                } else {
                    println("It replies with your voice: 'Mercy hides cost.' -2 HP to you.");
                    pHp -= 2; addJournal("Mercy failed to sway the mirror.");
                }
            } else if (cmd.startsWith("act ")) {
                String[] sp = cmd.split(" ",2);
                String word = sp.length > 1 ? sp[1] : "";
                if (word.isEmpty()) println("You try to act but no word forms.");
                else {
                    println("You say: '" + word + "'. The mirror returns it, different.");
                    String taunt = mirrorAdaptiveTaunt(word);
                    println("Mirror: \"" + taunt + "\"");
                    addJournal("Mirror replied to '" + word + "': " + taunt);
                    if (RNG.nextInt(100) < 35 && merciful > ruthless) {
                        int down = 3;
                        println("Your act finds purchase; -"+down+" mirror HP.");
                        mirrorHp = Math.max(0, mirrorHp - down);
                        // reflected to player
                        pHp -= down;
                        println("The mirror's recoil scorches you — you take " + down + " damage.");
                    } else {
                        println("Your word recoils and scratches: -1 HP.");
                        pHp -= 1;
                    }
                }
            } else if (cmd.startsWith("item ")) {
                String[] p = cmd.split(" ",2);
                String it = p.length > 1 ? p[1].trim() : "";
                if (it.equals("herb") && inventory.getOrDefault("Herb",0) > 0) {
                    println("You chew an Herb. Warmth returns. +6 HP.");
                    inventory.put("Herb", inventory.get("Herb") - 1);
                    usedHerbFlag = true;
                    pHp = Math.min(MAX_HP, pHp + 6);
                    addJournal("Used Herb in final chamber.");
                } else if (it.equals("shard") && inventory.getOrDefault("Shard",0) > 0) {
                    int rem = 4 + RNG.nextInt(3);
                    println("You press a shard into the mirror. A scream of glass; mirror loses " + rem + " HP.");
                    inventory.put("Shard", inventory.get("Shard") - 1);
                    usedShardFlag = true;
                    mirrorHp = Math.max(0, mirrorHp - rem);
                    // reflected damage to player
                    pHp -= rem;
                    println("Glass flares on your skin where you touched the mirror — you take " + rem + " damage.");
                    addJournal("Used Shard to injure the mirror for " + rem + " and suffered it back.");
                } else if (it.equals("ration") && inventory.getOrDefault("Ration",0) > 0) {
                    println("You eat a ration and steady yourself. +4 HP.");
                    inventory.put("Ration", inventory.get("Ration") - 1);
                    pHp = Math.min(MAX_HP, pHp + 4);
                    addJournal("Ate a ration in the Mirror Chamber.");
                } else {
                    println("You don't have that item or it's not usable now.");
                }
            } else {
                println("Your hesitation becomes a small wound.");
                pHp -= (notoriety >= 5 ? 2 : 1);
            }

            // mirror reacts
            if (mirrorHp > 0) {
                if (RNG.nextInt(100) < 55 + Math.min(20, notoriety*4)) {
                    String taunt = mirrorAdaptiveTaunt(null);
                    println("Mirror -> \"" + taunt + "\"");
                    if (RNG.nextInt(100) < 30 + Math.min(25, notoriety*3)) {
                        int sap = 1 + RNG.nextInt(3);
                        println("Its words sting and you lose " + sap + " HP.");
                        pHp -= sap;
                        maybeCorruptJournal();
                    }
                } else {
                    int shove = 1 + RNG.nextInt(2);
                    println("The mirror leans and gravity becomes a small, painful thing: -" + shove + " HP.");
                    pHp -= shove;
                }
            }

            if (pHp <= 0) {
                println("\nYour knees give. The mirror kneels above you and studies your face with your eyes.");
                finalOutcome = "death";
                addJournal("Collapsed in Mirror Chamber and died.");
                hp = 0;
                break;
            }
            if (mirrorHp <= 0) {
                println("\nThe mirror shivers and cracks into paper-like shards, which rearrange themselves into two choices: spare / end.");
                addJournal("Mirror broken; final choice presented.");
                finalOutcome = finalShardChoice();
                break;
            }
        } // while
        // update global hp
        if (pHp > 0) hp = pHp;
    }

    private static String finalShardChoice() {
        println("Between the shards two words form: 'spare' (merge/accept) or 'end' (shatter yourself into silence).");
        while (true) {
            print("> ");
            String c = input().trim().toLowerCase();
            if (c.equals("spare") || c.equals("merge") || c.equals("accept")) {
                addJournal("Final shard decision: spare/merge.");
                return "spare";
            } else if (c.equals("end") || c.equals("shatter") || c.equals("kill")) {
                addJournal("Final shard decision: end/shatter.");
                return "end";
            } else println("Type 'spare' or 'end' to choose.");
        }
    }

    // Mirror taunt builder
    private static String mirrorAdaptiveTaunt(String seed) {
        List<String> pool = new ArrayList<>();
        pool.add("You called cruelty a tool until it became a blunt instrument.");
        pool.add("Names were counted until names were all that mattered.");
        pool.add("You learned to be tidy with endings; endings learned to be tidy with you.");
        pool.add("Mercy is a shape you keep to make yourself sleep; does it keep you awake?");

        if (npcFlags.getOrDefault("Traveler_robbed", false)) pool.add("You took from a storyteller; stories remember the touch.");
        if (npcFlags.getOrDefault("Child_saved", false)) pool.add("A child's keepsake watches you from a pocket where it remembers your face.");
        if (npcFlags.getOrDefault("Library_burned", false)) pool.add("You burned words so a comfort could be born; comfort eats curiosity.");
        if (usedHerbFlag) pool.add("You mended the skin and taught the soul to adjust.");
        if (usedShardFlag) pool.add("You learnt the cut of your own jaw in the angle of glass.");

        if (ruthless > merciful + 3) {
            pool.add("You are efficient when you take; efficiency wants company.");
        } else if (merciful > ruthless + 3) {
            pool.add("Mercy clings to you like a coat; is it warmth or ballast?");
        } else {
            pool.add("The seam between saving and taking is thinning with each step.");
        }

        if (seed != null && !seed.trim().isEmpty()) {
            pool.add("You said '" + seed + "' — will you mean it when the tally is put away?");
            pool.add("Your word returns in the water as: '" + seed + "'.");
        }
        pool.add("are you proud of yourself?");

        return pool.get(RNG.nextInt(pool.size()));
    }

    // Journal corruption helpers
    private static void maybeCorruptJournal() {
        if (journal.isEmpty()) return;
        if (RNG.nextInt(100) < Math.min(35, notoriety * 7)) {
            int idx = RNG.nextInt(journal.size());
            String s = journal.get(idx);
            if (s.length() < 8) return;
            int cut = Math.max(3, RNG.nextInt(s.length() - 3));
            String corrupted = s.substring(0, Math.min(cut, s.length())) + "...";
            journal.set(idx, corrupted);
            println("(A memory warps into: '" + corrupted + "')");
        }
    }

    private static void maybeCorruptJournalByViolence() {
        if (RNG.nextInt(100) < 40 + Math.min(40, notoriety * 6)) maybeCorruptJournal();
    }

    // ======= Epilogue, journal prints, final line =======
    private static void showEpilogue() {
        println("\n=== EPILOGUE ===");
        if ("quit".equals(finalOutcome)) {
            println("You left the town. The bell keeps its rhythm and the streets rearrange their small habits.");
            addJournal("Player left town before final choice.");
            return;
        }
        if ("death".equals(finalOutcome)) {
            println("You died beneath the bell. People remember your absence; some light candles, others warn children.");
            addJournal("Epilogue: death under the bell.");
            return;
        }
        if ("spare".equals(finalOutcome)) {
            addJournal("Epilogue: chose spare/merge with shards.");
            if (ruthless > merciful) {
                println("You merge with the shards. The town continues, quieter, more careful to hide its wounds.");
            } else {
                println("You merge and carry compassion that aches. Gardens reopen slowly and a bell tolls differently.");
            }
            return;
        }
        if ("end".equals(finalOutcome)) {
            addJournal("Epilogue: chose end/shatter.");
            println("You shatter the mirror and claim silence. Some call it justice; others call it erasure.");
            if (ruthless > merciful) println("That silence becomes order; fewer songs are heard.");
            else println("They call it a necessary end; it tastes like cold linen.");
            return;
        }
        println("The town goes on. People remember you in half-glances and in small gestures.");
        addJournal("Epilogue: town continues with scars and songs.");
    }

    private static void printJournalRecent() {
        println("\n--- Recent Journal ---");
        int n = Math.min(10, journal.size());
        for (int i = 0; i < n; i++) println(journal.get(i));
        println("----------------------\n");
    }

    private static void printJournalFull() {
        println("\n=== FULL JOURNAL ===");
        for (String s : journal) println(s);
        println("====================\n");
    }

    // Final required exact line
    private static void finalLine() {
        System.out.println("are you proud of yourself?");
    }

    // ======= Small helpers used earlier =======
    private static boolean usedHerb() { return usedHerbFlag; }
    private static boolean usedShard() { return usedShardFlag; }

    // ======= Art helper class (original ASCII art for characters & locations) =======
    private static class Art {
        private static void println(String s) { System.out.println(s); }
        private static void pause(int ms) { if (PAUSE <= 0) return; try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

        static void banner() {
            println("\n  ╔════════════════════════════════════════════════════════════════╗");
            println("  ║                        AN UNTITLED RPG                         ║");
            println("  ║      a surreal, hub-based terminal RPG — choices matter        ║");
            println("  ╚════════════════════════════════════════════════════════════════╝\n");
            pause(180);
        }

        static void sceneTown() {
            println("          _._");
            println("      .-\"     \"-.");
            println("     /  .-\"\"\"-.  \\   The Town is ordinary at first: bread and bell.");
            println("    /  /  .-.  \\  \\");
            println("   |  |  /   \\  |  |");
            println("    \\  \\  `-'  /  /");
            println("     '-.___.-'._/ ");
            pause(120);
        }

        static void square() {
            println("╔════════════════════════════════════════════════════╗");
            println("║   [Town Square]  • bell  • market  • farm  • bridge║");
            println("╚════════════════════════════════════════════════════╝");
            pause(80);
        }

        static void marnieFarm() {
            println("      .-._                                 ");
            println("     /  _ `-.      _                       ");
            println("    |  (.)  |    .' \\   Marnie's Farm — earth, rope, steady hands");
            println("     \\     /    /   |");
            println("      `---'    /_.-' ");
            pause(80);
        }

        static void pebble() {
            println("     .--.");
            println("    (    )   Pebble — small and steady, his towers are little worlds.");
            println("     `--'");
            pause(80);
        }

        static void market() {
            println("   .-._.-._.-._.-._.-.");
            println("  /  Market Lane — stalls, bread, and gossip  \\");
            println("  '---------------------------------------------'");
            pause(80);
        }

        static void trader() {
            println("    ___");
            println("  .-\"   \"-.");
            println(" /  .-.-.  \\   Trader — warm hands, quick smiles");
            println(" \\  `-'-'  /");
            println("  `-.___.-'");
            pause(80);
        }

        static void traveler() {
            println("     _.._");
            println("   .'    '.   Traveler — boots soaked in road and story");
            println("  /  .--.  \\");
            println("  '--'  '--'");
            pause(80);
        }

        static void clockBeast() {
            println("      .----.");
            println("     / .--. \\   Clock-Beast — gears and tired faces");
            println("    / /    \\ \\");
            println("    | |    | |");
            println("     \\ \\__/ /");
            println("      `----'");
            pause(90);
        }

        static void library() {
            println("    ____||____");
            println("   /__________\\  Old Library — ink, dust, vanishing lines");
            println("   |  _   _  |");
            println("   | (_) (_) |");
            println("   '---------'");
            pause(100);
        }

        static void scribe() {
            println("    .-._");
            println("   (o o )   The Scribe — careful, quiet, ink like breath");
            println("    |=|");
            println("   __|__");
            pause(80);
        }

        static void bridge() {
            println("   ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~");
            println("  /  The Broken Bridge  \\  — water and small, urgent things");
            println("  '---------------------'");
            pause(80);
        }

        static void guard() {
            println("     .-\"\"-.");
            println("    / .--. \\  Guard — duty folded into bone and habit");
            println("   / /    \\ \\");
            println("   | |    | |");
            println("    \\ \\__/ /");
            println("     '----'");
            pause(80);
        }

        static void bandit() {
            println("   ,/\\,   Bandit Camp — tents like teeth");
            println("  //  \\\\");
            println("  `-..-'");
            pause(80);
        }

        static void mirror() {
            println("       .-\"\"-.");
            println("     .' .--. '.   Mirror Chamber — a bowl of black water, bell above");
            println("    /  /    \\  \\");
            println("   |  |      |  |");
            println("    \\  \\    /  /");
            println("     '. '~~' .'");
            println("       `----'");
            pause(140);
        }
    }

} // end of AnUntitledRPG class
