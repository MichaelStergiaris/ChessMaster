package com.chessmaster.data;

import com.chessmaster.model.Color;
import java.util.*;

/**
 * Comprehensive repertoire of 48 of the most popular chess openings & classical variations,
 * grouped by ECO family with move-by-move coaching commentary and strategic plans.
 */
public final class OpeningDatabase {
    public static final String CAT_ALL = "All Openings";
    public static final String CAT_OPEN = "Open Games (1.e4 e5)";
    public static final String CAT_SEMI_OPEN = "Semi-Open (1.e4 ...)";
    public static final String CAT_CLOSED = "Closed & Queen's Gambit";
    public static final String CAT_INDIAN = "Indian Defenses (1.d4 Nf6)";
    public static final String CAT_FLANK = "Flank Openings";

    private static final List<Opening> OPENINGS = buildOpenings();

    public static List<Opening> getAll() {
        return OPENINGS;
    }

    public static List<String> getCategories() {
        return List.of(CAT_ALL, CAT_OPEN, CAT_SEMI_OPEN, CAT_CLOSED, CAT_INDIAN, CAT_FLANK);
    }

    private static Opening o(String id, String eco, String name, String variation, String cat,
                             Color side, String diff, String pop, String summary,
                             String[] moves, String[] notes) {
        return new Opening(id, eco, name, variation, cat, side, diff, pop, summary,
                Arrays.asList(moves), Arrays.asList(notes));
    }

    private static List<Opening> buildOpenings() {
        List<Opening> list = new ArrayList<>();

        // ==================== OPEN GAMES (1.e4 e5) ====================
        list.add(o("ruy-morphy", "C78", "Ruy Lopez", "Morphy Defense (Closed)", CAT_OPEN,
            Color.WHITE, "Intermediate", "Very High",
            "The king of classical openings. White pressures Black's e5 defender on c6, builds a strong pawn center with c3+d4, and prepares a long-term kingside attack.",
            new String[]{"e4","e5","Nf3","Nc6","Bb5","a6","Ba4","Nf6","O-O","Be7","Re1","b5","Bb3","d6","c3","O-O","h3"},
            new String[]{
                "Claims central space and opens lines for the queen and light-squared bishop.",
                "Black mirrors in the center to fight for equal space.",
                "Develops with tempo, immediately attacking Black's e5 pawn.",
                "Defends e5 naturally while developing a queenside minor piece.",
                "The Ruy Lopez! Pressures the defender of e5 and prepares kingside castling.",
                "Morphy Defense: puts the question to White's bishop immediately.",
                "Maintains the pin/pressure on the a4-e8 diagonal.",
                "Develops and counter-attacks White's e4 pawn.",
                "Secures the king and prepares Re1 to protect e4.",
                "Solid development preparing kingside castling.",
                "Defends e4 and threatens Bxc6 followed by Nxe5.",
                "Breaks the pin and gains queenside space.",
                "Retreats to the ideal diagonal targeting f7.",
                "Reinforces e5 solidly and opens the c8-bishop's diagonal.",
                "Prepares the classic d4 central push and gives the bishop a c2 retreat.",
                "Black castles into safety.",
                "Prevents ...Bg4 pinning the f3 knight before White pushes d4."
            }));

        list.add(o("ruy-berlin", "C65", "Ruy Lopez", "Berlin Defense", CAT_OPEN,
            Color.BLACK, "Advanced", "Very High",
            "World-championship favorite popularized by Kramnik. Black trades queens early for an ironclad endgame structure with the bishop pair.",
            new String[]{"e4","e5","Nf3","Nc6","Bb5","Nf6","O-O","Nxe4","d4","Nd6","Bxc6","dxc6","dxe5","Nf5","Qxd8+","Kxd8"},
            new String[]{
                "White opens with the king's pawn.",
                "Black claims the center symmetrically.",
                "White attacks e5.",
                "Black defends e5.",
                "The Spanish Bishop applies pressure.",
                "The Berlin Defense! Counter-attacks e4 immediately.",
                "White prioritizes king safety and invites the endgame.",
                "Black captures the central pawn.",
                "Opens the e-file to strike at Black's exposed knight and king.",
                "Attacks the b5 bishop and clears the e-file.",
                "Damages Black's queenside pawn structure.",
                "Recaptures toward the center and opens the d-file.",
                "Reclaims material and forces the knight to move.",
                "Centralizes the knight before queens come off.",
                "Trades queens, stripping Black of castling rights.",
                "The famous 'Berlin Wall': Black's king is safe and the two bishops give rock-solid equality."
            }));

        list.add(o("ruy-marshall", "C89", "Ruy Lopez", "Marshall Attack", CAT_OPEN,
            Color.BLACK, "Advanced", "High",
            "One of the sharpest gambits at grandmaster level. Black sacrifices the e5 pawn for rapid piece development and a raging kingside attack.",
            new String[]{"e4","e5","Nf3","Nc6","Bb5","a6","Ba4","Nf6","O-O","Be7","Re1","b5","Bb3","O-O","c3","d5","exd5","Nxd5","Nxe5","Nxe5","Rxe5","c6"},
            new String[]{
                "Central claim.", "Symmetrical reply.", "Attacks e5.", "Defends e5.",
                "Ruy Lopez.", "Morphy Defense.", "Keeps pressure.", "Attacks e4.",
                "Castles kingside.", "Develops smoothly.", "Protects e4.", "Gains queenside space.",
                "Retreats to the strong diagonal.", "Black castles early, hinting at the Marshall!",
                "Prepares d4, stepping into the gambit.", "The Marshall Attack! Strikes the center immediately.",
                "White accepts the challenge.", "Activates the knight centrally.",
                "White wins the offered pawn.", "Clears lines toward White's kingside.",
                "Rook recaptures, temporarily exposed on e5.", "Defends d5 and prepares ...Bd6 harassing the rook and aiming at h2."
            }));

        list.add(o("italian-giuoco", "C53", "Italian Game", "Giuoco Piano", CAT_OPEN,
            Color.WHITE, "Beginner", "Very High",
            "Classic, harmonious development targeting the weak f7 square. Great for players of all levels seeking clear plans and tactical opportunities.",
            new String[]{"e4","e5","Nf3","Nc6","Bc4","Bc5","c3","Nf6","d3","d6","O-O","O-O","Re1","a6","Bb3","Ba7"},
            new String[]{
                "Controls d5 and f5.", "Contests the center.", "Attacks e5.", "Defends e5.",
                "The Italian Bishop eyes f7, Black's weakest square.", "Black mirrors, targeting f2.",
                "Prepares d4 or supports a solid central setup.", "Develops and attacks e4.",
                "Giuoco Pianissimo: defends e4 solidly and keeps tension.", "Opens the c8 bishop and fortifies e5.",
                "Tucks the king safely away.", "Black castles as well.",
                "Supports e4 and prepares Nbd2-f1-g3 maneuver.", "Creates a safe a7 retreat for the dark-squared bishop.",
                "Steps away from ...Na5 harassment.", "Preserves the active bishop on the a7-g1 diagonal."
            }));

        list.add(o("italian-evans", "C51", "Italian Game", "Evans Gambit", CAT_OPEN,
            Color.WHITE, "Intermediate", "High",
            "Romantic-era classic! White sacrifices the b-pawn to gain massive tempos, build a huge center with c3+d4, and launch a ferocious attack.",
            new String[]{"e4","e5","Nf3","Nc6","Bc4","Bc5","b4","Bxb4","c3","Ba5","d4","exd4","O-O","d6","cxd4","Bb6"},
            new String[]{
                "King's pawn opening.", "Classical response.", "Develops with attack.", "Defends e5.",
                "Targets f7.", "Classical mirror.", "The Evans Gambit! Deflects the bishop to gain speed.",
                "Black accepts the pawn.", "Attacks the bishop with tempo while preparing d4.",
                "Retreats while maintaining a pin on the a5-e1 diagonal.", "Seizes the center immediately!",
                "Black opens the e-file.", "White castles fast, ignoring the pawn for huge initiative.",
                "Solidifies the center and prepares development.", "White establishes an imposing central pawn duo.",
                "Black retreats the bishop to a safe diagonal; sharp battle ahead."
            }));

        list.add(o("italian-two-knights", "C57", "Italian Game", "Two Knights Defense", CAT_OPEN,
            Color.WHITE, "Beginner", "Very High",
            "Sharp tactical battle where White immediately attacks f7 with Ng5, testing whether Black knows the principled Polerio counter-attack.",
            new String[]{"e4","e5","Nf3","Nc6","Bc4","Nf6","Ng5","d5","exd5","Na5","Bb5+","c6","dxc6","bxc6","Be2","h6","Nf3","e4"},
            new String[]{
                "Central control.", "Symmetrical reply.", "Develops knight.", "Defends e5.",
                "Aims at f7.", "Two Knights Defense: counter-attacks e4.", "Immediately attacks f7 with knight and bishop!",
                "The only strong reply—blocks the bishop's diagonal.", "Captures the pawn.",
                "Polerio Defense: avoids the Fried Liver (Nxd5) and attacks c4!", "Checks to gain time before moving the bishop.",
                "Blocks the check while challenging the d5 pawn.", "White grabs a pawn.", "Black recaptures with active piece play.",
                "Safe retreat keeping the extra pawn.", "Kicks the knight away from g5.", "Retreats to safety.", "Black grabs space and initiative for the pawn."
            }));

        list.add(o("scotch-game", "C45", "Scotch Game", "Classical Main Line", CAT_OPEN,
            Color.WHITE, "Beginner", "High",
            "Kasparov's favorite weapon! White immediately breaks open the center on move 3, avoiding long Ruy Lopez theory for open, dynamic play.",
            new String[]{"e4","e5","Nf3","Nc6","d4","exd4","Nxd4","Bc5","Be3","Qf6","c3","Nge7","Bc4","Ne5","Be2","Qg6"},
            new String[]{
                "Claims center.", "Replies symmetrically.", "Attacks e5.", "Defends e5.",
                "The Scotch! Immediately challenges Black's center.", "Black almost always captures to avoid conceding space.",
                "Recaptures centrally with open lines.", "Develops actively, hitting d4.", "Defends d4 and develops smoothly.",
                "Pressures d4 a third time and eyes f2.", "Solidifies the d4 knight and controls b4/d4.",
                "Prepares castling and ...d5.", "Active square targeting f7.", "Centralizes with tempo on the bishop.",
                "Keeps the bishop pair safely.", "Targets g2 and e4—rich, imbalanced middlegame!"
            }));

        list.add(o("vienna-gambit", "C29", "Vienna Game", "Vienna Gambit", CAT_OPEN,
            Color.WHITE, "Intermediate", "High",
            "Aggressive, practical club weapon! White delays Nf3 to strike with f4—if Black accepts carelessly, White gets a crushing central attack.",
            new String[]{"e4","e5","Nc3","Nf6","f4","d5","fxe5","Nxe4","Nf3","Bc5","d4","Bb4","Bd2","Nxd2","Qxd2","O-O"},
            new String[]{
                "King's pawn.", "Symmetrical.", "The Vienna Game: guards e4 and keeps f4 available.",
                "Black attacks e4 immediately.", "The Vienna Gambit! Threatens fxe5 and massive center.",
                "The principled counter-strike in the center.", "Captures e5, hitting the knight.",
                "Black jumps into e4.", "Develops and controls d4/e5.", "Aims at f2.",
                "Seizes the center with tempo on the bishop.", "Pins the c3 knight.", "Breaks the pin cleanly.",
                "Black trades off White's bishop.", "Recaptures and prepares O-O-O or Bd3.", "Black castles to safety."
            }));

        list.add(o("kings-gambit", "C33", "King's Gambit", "Accepted (Bishop's/Knight's)", CAT_OPEN,
            Color.WHITE, "Intermediate", "Medium",
            "The ultimate romantic attacking opening. White sacrifices the f-pawn on move 2 to deflect Black's e-pawn and dominate the center and f-file.",
            new String[]{"e4","e5","f4","exf4","Nf3","g5","h4","g4","Ne5","Nf6","Bc4","d5","exd5","Bd6","d4","Nh5"},
            new String[]{
                "Claims center.", "Symmetrical.", "The King's Gambit! Attacks e5 from the flank.",
                "Black accepts the gambit pawn.", "Prevents ...Qh4+ and develops.", "Black tries to cling to the f4 pawn.",
                "Immediately undermines Black's kingside pawn chain.", "Pushes forward, attacking the knight.",
                "Kieseritzky Gambit: centralizes the knight boldly.", "Develops and attacks e4.",
                "Targets f7 with tactical pressure.", "Counter-strikes in the center.", "Captures with open lines.",
                "Develops and attacks the e5 knight.", "Claims full central control.", "Defends f4 and eyes g3—wild tactical fight!"
            }));

        list.add(o("petrov-defense", "C42", "Petrov's Defense", "Classical Main Line", CAT_OPEN,
            Color.BLACK, "Intermediate", "High",
            "Ultra-solid counter-attacking defense favored by elite grandmasters like Caruana. Instead of defending e5, Black immediately attacks e4.",
            new String[]{"e4","e5","Nf3","Nf6","Nxe5","d6","Nf3","Nxe4","d4","d5","Bd3","Nc6","O-O","Be7","c4","Nb4"},
            new String[]{
                "White opens e4.", "Black mirrors.", "White attacks e5.", "Petrov's Defense! Immediate counter-attack.",
                "White captures e5.", "Crucial! Chases the knight before taking e4.", "Knight retreats.",
                "Now Black safely restores material equality.", "White seizes central space.", "Black anchors the e4 knight.",
                "Develops and eyes the e4 knight.", "Develops with pressure on d4.", "White castles.",
                "Prepares castling and keeps the e-file safe.", "White challenges the d5 pawn.", "Black trades off White's strong light-squared bishop."
            }));

        list.add(o("philidor-defense", "C41", "Philidor Defense", "Hanham Variation", CAT_OPEN,
            Color.BLACK, "Beginner", "Medium",
            "Resilient, compact defense where Black protects e5 with d6, avoiding sharp tactical traps while preparing a counter-punch with ...f5 or ...c6.",
            new String[]{"e4","e5","Nf3","d6","d4","Nd7","Bc4","c6","O-O","Be7","a4","Ngf6","Nc3","O-O"},
            new String[]{
                "Central control.", "Classical reply.", "Attacks e5.", "The Philidor Defense: solid support for e5.",
                "White takes the full center.", "Hanham setup: defends e5 without blocking the f-pawn.",
                "Develops targeting f7.", "Controls d5 and prepares ...b5 or ...Qc7.", "King safety.",
                "Solid development.", "Prevents ...b5 queenside expansion.", "Develops and pressures e4.",
                "Supports e4 and completes minor-piece development.", "Black castles into a sturdy, counter-attacking fortress."
            }));

        list.add(o("four-knights", "C47", "Four Knights Game", "Scotch Four Knights", CAT_OPEN,
            Color.WHITE, "Beginner", "High",
            "Clean, principled opening that follows every classical rule: develop knights before bishops, control the center, and castle early.",
            new String[]{"e4","e5","Nf3","Nc6","Nc3","Nf6","d4","exd4","Nxd4","Bb4","Nxc6","bxc6","Bd3","d5","exd5","cxd5","O-O","O-O"},
            new String[]{
                "Opens center.", "Matches center.", "Attacks e5.", "Defends e5.",
                "Develops the second knight.", "Completes the Four Knights.", "Strikes in the center for active play.",
                "Opens the e-file.", "Centralizes the knight.", "Pins c3 and pressures e4.",
                "Doubles Black's c-pawns.", "Recaptures toward the center.", "Defends e4 and aims kingside.",
                "Black fights for central space.", "Clarifies the center.", "Black gains a strong central pawn.",
                "White castles smoothly.", "Black castles—balanced, open position."
            }));

        list.add(o("ponziani", "C44", "Ponziani Opening", "Main Line", CAT_OPEN,
            Color.WHITE, "Beginner", "Medium",
            "Tricky surprise weapon! White prepares an immediate d4 pawn center with c3, setting tactical traps for unprepared Black players.",
            new String[]{"e4","e5","Nf3","Nc6","c3","Nf6","d4","Nxe4","d5","Ne7","Nxe5","Ng6","Bd3","Nxf2","Kxf2","Nxe5"},
            new String[]{
                "Central opening.", "Symmetrical.", "Attacks e5.", "Defends e5.",
                "The Ponziani! Prepares a full pawn center on d4.", "Best response: counter-attacks e4.",
                "White pushes d4 anyway.", "Black grabs the e4 pawn.", "Kicks the c6 knight with space gain.",
                "Knight repositions toward the kingside.", "White regains the pawn.", "Challenges the e5 knight.",
                "Develops rapidly.", "Sharp tactical shot! Forks queen and rook.", "White accepts the sacrifice.", "Wild, double-edged tactics!"
            }));

        // ==================== SEMI-OPEN GAMES (1.e4 ...) ====================
        list.add(o("sicilian-najdorf", "B90", "Sicilian Defense", "Najdorf Variation", CAT_SEMI_OPEN,
            Color.BLACK, "Advanced", "Very High",
            "The Rolls-Royce of chess openings—Bobby Fischer and Garry Kasparov's signature weapon. Rich, asymmetrical, and fighting for a win from move 1.",
            new String[]{"e4","c5","Nf3","d6","d4","cxd4","Nxd4","Nf6","Nc3","a6","Be3","e5","Nb3","Be6","f3","Be7","Qd2","O-O","O-O-O"},
            new String[]{
                "White takes central space.", "The Sicilian! Fights for d4 asymmetrically.", "Prepares d4.",
                "Controls e5 and prepares ...Nf6.", "Open Sicilian: White claims open lines.", "Black trades a flank pawn for a center pawn!",
                "Centralizes the knight.", "Attacks e4 immediately.", "Defends e4.",
                "The iconic 5...a6! Controls b5 and prepares ...e5 or ...b5.", "English Attack: flexible development.",
                "Claims central space and kicks the d4 knight!", "Retreats while eyeing c5/a5.",
                "Solidly controls the d5 square.", "Blunts ...Ng4 and prepares a g4 pawn storm.",
                "Prepares kingside castling.", "Connects rooks and supports O-O-O.", "Black castles into the coming storm.",
                "Opposite-side castling—an all-out tactical race!"
            }));

        list.add(o("sicilian-dragon", "B70", "Sicilian Defense", "Dragon Variation", CAT_SEMI_OPEN,
            Color.BLACK, "Intermediate", "Very High",
            "Explosive opposite-side castling battles! Black fianchettoes the dark-squared bishop on g7 to breathe fire down the h8-a1 diagonal.",
            new String[]{"e4","c5","Nf3","d6","d4","cxd4","Nxd4","Nf6","Nc3","g6","Be3","Bg7","f3","O-O","Qd2","Nc6","Bc4","Bd7","O-O-O"},
            new String[]{
                "King's pawn.", "Sicilian Defense.", "Prepares d4.", "Controls e5.", "Open Sicilian.", "Trades flank for center pawn.",
                "Central knight.", "Attacks e4.", "Defends e4.", "The Dragon! Prepares the monstrous g7 bishop.",
                "Yugoslav Attack begins.", "The Dragon Bishop dominates the long diagonal.", "Protects e4 and prepares g4-h4.",
                "Black castles kingside.", "Prepares Bh6 to trade off Black's Dragon bishop.", "Develops and pressures d4.",
                "Controls d5 and guards b3.", "Prepares ...Rc8 to attack White's king.", "Opposite-side castling—razor-sharp attack and counter-attack!"
            }));

        list.add(o("sicilian-acc-dragon", "B35", "Sicilian Defense", "Accelerated Dragon", CAT_SEMI_OPEN,
            Color.BLACK, "Intermediate", "High",
            "Smart move-order trick: Black omits ...d6 to play ...d5 in one move, sidestepping the fiercest Yugoslav Attack lines.",
            new String[]{"e4","c5","Nf3","Nc6","d4","cxd4","Nxd4","g6","Nc3","Bg7","Be3","Nf6","Bc4","O-O","Bb3","d6","f3","Bd7"},
            new String[]{
                "e4.", "Sicilian.", "Nf3.", "Develops knight first, delaying ...d6.", "Open Sicilian.", "Captures center pawn.",
                "Recaptures.", "Accelerated Dragon fianchetto!", "Develops smoothly.", "Dominates the long diagonal.",
                "Supports d4.", "Develops and attacks e4.", "Eyes f7.", "Quick kingside castling.",
                "Avoids ...Na5 or ...Nxe4 tricks.", "Reinforces e5 cleanly.", "Protects e4.", "Prepares ...Rc8 and queenside play."
            }));

        list.add(o("sicilian-sveshnikov", "B33", "Sicilian Defense", "Sveshnikov Variation", CAT_SEMI_OPEN,
            Color.BLACK, "Advanced", "High",
            "Magnus Carlsen's world championship weapon! Black accepts a backward d6 pawn in return for tremendous piece activity and a raging ...f5 break.",
            new String[]{"e4","c5","Nf3","Nc6","d4","cxd4","Nxd4","Nf6","Nc3","e5","Ndb5","d6","Bg5","a6","Na3","b5","Nd5","Be7","Bxf6","Bxf6"},
            new String[]{
                "e4.", "Sicilian.", "Nf3.", "Nc6.", "Open Sicilian.", "cxd4.", "Nxd4.", "Attacks e4.", "Defends e4.",
                "The Sveshnikov! Boldly grabs central space.", "Heads straight for the weak d6 square.", "Defends d6 and prepares ...a6.",
                "Pins f6 to control d5.", "Kicks the intrusive b5 knight.", "Knight retreats to the rim.", "Gains queenside space and threatens ...b4.",
                "White plants a monster knight on d5.", "Develops and challenges the pin.", "Removes the defender.", "Black's bishop pair and ...f5 break create fierce counterplay!"
            }));

        list.add(o("sicilian-alapin", "B22", "Sicilian Defense", "Alapin Variation", CAT_SEMI_OPEN,
            Color.WHITE, "Beginner", "Very High",
            "The #1 practical anti-Sicilian! White plays 2.c3 to build a classical e4+d4 pawn center without drowning in sharp Open Sicilian theory.",
            new String[]{"e4","c5","c3","Nf6","e5","Nd5","d4","cxd4","Nf3","Nc6","cxd4","d6","Bc4","Nb6","Bb5","dxe5","Nxe5","Bd7"},
            new String[]{
                "e4.", "Sicilian.", "The Alapin! Prepares d4 with a pawn recapture.", "Black counter-attacks e4 immediately.",
                "Gains space and kicks the knight.", "Centralizes the knight.", "White claims the center.", "Black captures.",
                "Develops smoothly (avoiding early queen exposure).", "Pressures d4 and e5.", "Restores the ideal d4 pawn.",
                "Challenges White's advanced e5 pawn.", "Develops with tempo on the d5 knight!", "Retreats and attacks the c4 bishop.",
                "Pins the c6 knight.", "Opens the center.", "Active central knight.", "Unpins cleanly—rich strategic play."
            }));

        list.add(o("sicilian-rossolimo", "B30", "Sicilian Defense", "Rossolimo Attack", CAT_SEMI_OPEN,
            Color.WHITE, "Intermediate", "High",
            "Elite grandmaster favorite against 2...Nc6. White sidesteps the Sveshnikov and puts structural pressure on Black's queenside.",
            new String[]{"e4","c5","Nf3","Nc6","Bb5","g6","Bxc6","dxc6","d3","Bg7","h3","Nf6","Nc3","O-O","Be3","b6","Qd2"},
            new String[]{
                "e4.", "Sicilian.", "Nf3.", "Nc6.", "The Rossolimo! Pins and threatens to double Black's pawns.",
                "Prepares ...Bg7.", "Inflicts doubled c-pawns right away.", "Opens lines for the c8 bishop.",
                "Solid setup supporting e4.", "Fianchettoes smoothly.", "Prevents ...Bg4.", "Develops kingside.",
                "Reinforces e4.", "Castles.", "Pressures c5 and prepares Bh6.", "Defends c5 solidly.", "Prepares Bh6 to neutralize Black's fianchetto."
            }));

        list.add(o("sicilian-closed", "B23", "Sicilian Defense", "Closed Sicilian", CAT_SEMI_OPEN,
            Color.WHITE, "Beginner", "High",
            "Strategic kingside attacking setup. White develops quietly with Nc3, g3, Bg2, d3, and launches an f4-f5 pawn storm.",
            new String[]{"e4","c5","Nc3","Nc6","g3","g6","Bg2","Bg7","d3","d6","f4","e6","Nf3","Nge7","O-O","O-O","Be3"},
            new String[]{
                "e4.", "Sicilian.", "Closed Sicilian: keeps the center closed.", "Controls d4.",
                "Prepares kingside fianchetto.", "Black mirrors on the long diagonal.", "Controls d5 and shields White's king.",
                "Black's bishop opposes on g7.", "Supports e4 cleanly.", "Controls e5.", "Starts the classic kingside expansion!",
                "Prepares ...Nge7 and ...d5.", "Develops harmoniously.", "Avoids blocking the f-pawn.", "King safety.", "Castles.", "Prepares Qd2 and kingside attack."
            }));

        list.add(o("french-advance", "C02", "French Defense", "Advance Variation", CAT_SEMI_OPEN,
            Color.BLACK, "Beginner", "Very High",
            "Classic pawn-chain battle! White grabs space with e5, while Black immediately undermines the base of White's chain with ...c5 and ...Qb6.",
            new String[]{"e4","e6","d4","d5","e5","c5","c3","Nc6","Nf3","Qb6","a3","c4","Nbd2","Na5","Be2","Bd7"},
            new String[]{
                "Central claim.", "The French Defense: prepares ...d5 safely.", "White takes the full center.",
                "Strikes at e4 immediately.", "Advance Variation: gains kingside space.", "Attacks the base of White's pawn chain on d4!",
                "Fortifies d4 solidly.", "Increases pressure on d4.", "Defends d4 and develops.",
                "Triple-attacks d4 and eyes b2!", "Prepares b4 queenside counterplay.", "Locks the queenside and claims space.",
                "Develops the queenside knight.", "Clamps down on b3.", "Prepares castling.", "Clears the back rank and controls the queenside."
            }));

        list.add(o("french-winawer", "C15", "French Defense", "Winawer Variation", CAT_SEMI_OPEN,
            Color.BLACK, "Advanced", "High",
            "Sharp, deeply strategic battleground where Black trades the dark-squared bishop to shatter White's queenside pawns.",
            new String[]{"e4","e6","d4","d5","Nc3","Bb4","e5","c5","a3","Bxc3+","bxc3","Ne7","Qg4","Qc7","Qxg7","Rg8","Qxh7","cxd4"},
            new String[]{
                "e4.", "French Defense.", "d4.", "Challenges e4.", "Classical defense of e4.", "The Winawer! Pins c3 and threatens ...dxe4.",
                "Gains space and closes the center.", "Strikes at d4 immediately.", "Forces the bishop's decision.",
                "Doubles White's c-pawns.", "Recaptures, gaining the bishop pair.", "Prepares ...Qc7 and ...Nbc6.",
                "Poisoned Pawn line! Immediately targets g7.", "Defends c5/e5 and prepares counter-attack.",
                "White grabs the g7 pawn.", "Counters on the open g-file!", "White takes h7 too.", "Black demolishes White's center—thrilling tactical chaos!"
            }));

        list.add(o("french-tarrasch", "C03", "French Defense", "Tarrasch Variation", CAT_SEMI_OPEN,
            Color.WHITE, "Intermediate", "High",
            "Positional favorite avoiding the Winawer pin. White defends e4 with Nd2, keeping c3 available to bolster d4.",
            new String[]{"e4","e6","d4","d5","Nd2","Nf6","e5","Nfd7","Bd3","c5","c3","Nc6","Ne2","cxd4","cxd4","f6"},
            new String[]{
                "e4.", "French.", "d4.", "d5.", "The Tarrasch: avoids ...Bb4 pins.", "Attacks e4.",
                "Gains space.", "Knight retreats to support ...c5 and ...f6.", "Targets h7.", "Standard French central break.",
                "Reinforces d4 solidly.", "Pressures d4.", "Prepares Nf3/O-O while keeping the f-pawn free.", "Opens the c-file.",
                "Maintains the d4 outpost.", "Strikes at the head of White's pawn chain!"
            }));

        list.add(o("caro-classical", "B18", "Caro-Kann Defense", "Classical Variation", CAT_SEMI_OPEN,
            Color.BLACK, "Beginner", "Very High",
            "Rock-solid yet dynamic! Unlike the French, Black frees the light-squared bishop to f5 before playing ...e6, yielding a pristine pawn structure.",
            new String[]{"e4","c6","d4","d5","Nc3","dxe4","Nxe4","Bf5","Ng3","Bg6","h4","h6","Nf3","Nd7","h5","Bh7","Bd3","Bxd3","Qxd3"},
            new String[]{
                "e4.", "The Caro-Kann! Prepares ...d5 without blocking the c8 bishop.", "Takes the center.", "Challenges e4.",
                "Defends e4.", "Opens the center.", "Centralizes the knight.", "The point of the Caro-Kann: develops the bishop OUTSIDE the pawn chain!",
                "Attacks the bishop.", "Tucks safely on g6.", "White gains kingside space, threatening h5.", "Creates an h7 escape hatch!",
                "Develops smoothly.", "Prevents Ne5 and prepares ...Ngf6.", "Fixes the kingside.", "Safe haven.",
                "Challenges Black's active bishop.", "Black trades cleanly.", "Solid, healthy position for both sides."
            }));

        list.add(o("caro-advance", "B12", "Caro-Kann Defense", "Advance Variation", CAT_SEMI_OPEN,
            Color.WHITE, "Intermediate", "Very High",
            "Most popular modern test of the Caro-Kann! White claims space with e5 and plays aggressively on the kingside (Short or Tal variation).",
            new String[]{"e4","c6","d4","d5","e5","Bf5","Nf3","e6","Be2","c5","Be3","cxd4","Nxd4","Ne7","O-O","Nbc6"},
            new String[]{
                "e4.", "Caro-Kann.", "d4.", "d5.", "Advance Variation: seizes space immediately.", "Activates the light-squared bishop.",
                "Short Variation: smooth development.", "Builds a solid dark-square shell.", "Prepares kingside castling.",
                "Black challenges the center.", "Develops and pressures c5.", "Opens the c-file.", "Centralizes the knight.",
                "Develops flexibly.", "King safety.", "Contests the central squares."
            }));

        list.add(o("scandinavian", "B01", "Scandinavian Defense", "Main Line (3...Qa5)", CAT_SEMI_OPEN,
            Color.BLACK, "Beginner", "High",
            "Direct, practical, and easy to learn! Black challenges White's e4 pawn on move 1 and immediately creates an open, clear pawn structure.",
            new String[]{"e4","d5","exd5","Qxd5","Nc3","Qa5","d4","Nf6","Nf3","c6","Bc4","Bf5","Bd2","e6"},
            new String[]{
                "e4.", "Scandinavian Defense: immediate central clash!", "White captures.", "Black recaptures cleanly.",
                "Gains a tempo attacking the queen.", "The classical Qa5 retreat pins c3 and stays active.", "White takes central space.",
                "Develops and controls d5.", "Develops smoothly.", "Gives the queen a c7 refuge and guards b5.",
                "Active bishop square.", "Develops outside the pawn chain before ...e6!", "Prepares Nd5 discovered attacks.", "Solid Caro-Kann-like structure."
            }));

        list.add(o("pirc-defense", "B07", "Pirc Defense", "Austrian Attack", CAT_SEMI_OPEN,
            Color.BLACK, "Intermediate", "Medium",
            "Hypermodern counter-attacking defense. Black lets White build a broad pawn center with the aim of undermining and counter-attacking it.",
            new String[]{"e4","d6","d4","Nf6","Nc3","g6","f4","Bg7","Nf3","O-O","Bd3","Na6","O-O","c5","d5","Nc7"},
            new String[]{
                "e4.", "Pirc Defense: flexible hypermodern setup.", "Takes space.", "Attacks e4.", "Defends e4.",
                "Prepares kingside fianchetto.", "Austrian Attack: White builds a massive three-pawn center!", "Eyes d4 and e5.",
                "Develops.", "Castles quickly.", "Supports e4 and aims kingside.", "Prepares ...c5 and ...Nc7 to undermine d4!",
                "Castles.", "Strikes at White's big center!", "Closes the center.", "Prepares ...b5 queenside counterplay."
            }));

        list.add(o("alekhine-defense", "B02", "Alekhine's Defense", "Four Pawns / Modern", CAT_SEMI_OPEN,
            Color.BLACK, "Intermediate", "Medium",
            "Provocative hypermodern weapon! Black lures White's central pawns forward on move 1 so they become overextended targets.",
            new String[]{"e4","Nf6","e5","Nd5","d4","d6","Nf3","Bg4","Be2","e6","O-O","Be7","c4","Nb6","exd6","cxd6"},
            new String[]{
                "e4.", "Alekhine's Defense! Provokes e5.", "Gains space with tempo.", "Centralizes.", "Claims center.",
                "Immediately attacks White's advanced e5 pawn.", "Modern Variation: keeps pawns solid.", "Pins f3 and pressures d4.",
                "Breaks the pin.", "Solidifies the position.", "Castles.", "Prepares castling.", "Kicks the d5 knight.",
                "Pressures c4.", "Clarifies the center.", "Black gets a solid, dynamic counter-attacking structure."
            }));

        // ==================== CLOSED & QUEEN'S GAMBIT ====================
        list.add(o("qgd-main", "D37", "Queen's Gambit Declined", "Classical Main Line", CAT_CLOSED,
            Color.WHITE, "Beginner", "Very High",
            "One of the oldest and most respected openings in chess history. Rich positional maneuvering, minority attacks, and timeless classical principles.",
            new String[]{"d4","d5","c4","e6","Nc3","Nf6","Bg5","Be7","e3","O-O","Nf3","Nbd7","Rc1","c6","Bd3","dxc4","Bxc4","Nd5"},
            new String[]{
                "Claims central space with the protected d-pawn.", "Black responds symmetrically.", "The Queen's Gambit! Challenges d5 from the flank.",
                "Declines solidly, maintaining a central stronghold.", "Pressures d5 and controls e4.", "Develops and guards d5.",
                "Pins the f6 knight to weaken d5.", "Unpins calmly.", "Supports d4 and opens the f1 bishop.", "Black castles.",
                "Develops smoothly.", "Prepares ...c6 and ...dxc4.", "Places the rook on the future open c-file.",
                "Reinforces d5.", "Active square targeting h7.", "Waits for White to move the bishop before taking!",
                "Recaptures cleanly.", "Black simplifies to free the cramped position."
            }));

        list.add(o("qga-main", "D20", "Queen's Gambit Accepted", "Classical Main Line", CAT_CLOSED,
            Color.BLACK, "Intermediate", "High",
            "Active, honest chess! Black takes the c4 pawn not to keep it, but to trade White's central influence for easy piece development and a ...c5 break.",
            new String[]{"d4","d5","c4","dxc4","Nf3","Nf6","e3","e6","Bxc4","c5","O-O","a6","Bb3","cxd4","exd4","Nc6"},
            new String[]{
                "d4.", "d5.", "Queen's Gambit.", "Accepted! Yields the center temporarily for free piece play.",
                "Prevents ...e5 and develops.", "Controls e4.", "Prepares to regain the c4 pawn.", "Opens the dark-squared bishop.",
                "White restores material.", "The key freeing break! Immediately strikes White's center.", "Castles.",
                "Prepares ...b5 to gain queenside space and fianchetto on b7.", "Tucks the bishop away safely.",
                "Creates an isolated queen's pawn (IQP) battle.", "White gets active pieces; Black targets d4.", "Develops with pressure on d4."
            }));

        list.add(o("slav-defense", "D10", "Slav Defense", "Main Line", CAT_CLOSED,
            Color.BLACK, "Intermediate", "Very High",
            "Super-reliable response to 1.d4! By supporting d5 with ...c6, Black keeps the c8 bishop free to develop outside the pawn chain.",
            new String[]{"d4","d5","c4","c6","Nf3","Nf6","Nc3","dxc4","a4","Bf5","e3","e6","Bxc4","Bb4","O-O","O-O"},
            new String[]{
                "d4.", "d5.", "Queen's Gambit.", "The Slav Defense! Supports d5 without locking in the c8 bishop.",
                "Develops.", "Controls e4.", "Pressures d5.", "Takes the pawn right before developing the bishop.",
                "Stops ...b5 from holding the extra pawn.", "Develops the light-squared bishop actively!", "Prepares Bxc4.",
                "Solidifies the center.", "Reclaims the pawn.", "Pins c3 and controls e4.", "White castles.", "Black castles—harmonious, rock-solid play!"
            }));

        list.add(o("semi-slav", "D43", "Semi-Slav Defense", "Meran Variation", CAT_CLOSED,
            Color.BLACK, "Advanced", "High",
            "Dynamic hybrid of the QGD and Slav. Black builds a rock-solid pawn triangle (c6-d5-e6) before exploding onto the queenside with ...dxc4 and ...b5!",
            new String[]{"d4","d5","c4","c6","Nf3","Nf6","Nc3","e6","e3","Nbd7","Bd3","dxc4","Bxc4","b5","Bd3","Bb7"},
            new String[]{
                "d4.", "d5.", "c4.", "Slav move.", "Nf3.", "Nf6.", "Nc3.", "Completes the Semi-Slav triangle!",
                "Solid support for c4.", "Supports ...e5 or ...dxc4.", "Develops aggressively.", "Meran Variation: takes after Bd3 to win a tempo!",
                "Recaptures.", "Attacks the bishop and grabs queenside space!", "Retreats.", "Fianchettoes the bishop to dominate the long diagonal."
            }));

        list.add(o("london-system", "D02", "London System", "Classical Pyramid", CAT_CLOSED,
            Color.WHITE, "Beginner", "Very High",
            "The ultimate universal system! Easy to learn, ultra-solid, and full of sneaky kingside attacks with Ne5, Bd3, and Qh5.",
            new String[]{"d4","d5","Bf4","Nf6","e3","c5","c3","Nc6","Nd2","e6","Ngf3","Bd6","Bg3","O-O","Bd3"},
            new String[]{
                "Claims central space.", "Symmetrical reply.", "The London Bishop develops outside the pawn chain on move 2!",
                "Black develops.", "Builds the solid dark-square structure.", "Black challenges the center.",
                "Completes the ironclad London pawn pyramid (c3-d4-e3)!", "Pressures d4.",
                "Guards e4 and prepares Bd3/Ngf3.", "Opens Black's dark-squared bishop.", "Controls the crucial e5 outpost!",
                "Challenges White's strong London bishop.", "The classic retreat! If ...Bxg3, hxg3 opens the h-file for White's rook!",
                "Black castles.", "Aims straight at h7—ready for Ne5 and kingside attack!"
            }));

        list.add(o("jobava-london", "D00", "Jobava London", "Aggressive Nc3 Setup", CAT_CLOSED,
            Color.WHITE, "Intermediate", "High",
            "The ferocious modern cousin of the London! Pairing Bf4 with Nc3 immediately threatens Nb5 targeting c7, leading to sharp, creative battles.",
            new String[]{"d4","d5","Nc3","Nf6","Bf4","e6","e3","Bd6","Bg3","O-O","Bd3","c5","Nf3","Nc6"},
            new String[]{
                "d4.", "d5.", "Unusual and aggressive: threatens e4 or Nb5!", "Controls e4.",
                "The Jobava London! Immediately eyes c7 alongside Nb5.", "Guards against immediate Nb5 tricks via ...Bd6/Na6 ideas.",
                "Solid support.", "Challenges the f4 bishop.", "Classic retreat inviting an open h-file.", "Castles.",
                "Eyes h7.", "Strikes the center.", "Develops smoothly.", "Rich, original middlegame."
            }));

        list.add(o("catalan-open", "E04", "Catalan Opening", "Open Catalan", CAT_CLOSED,
            Color.WHITE, "Advanced", "Very High",
            "Magnus Carlsen and Vladimir Kramnik's positional masterpiece! Combines the Queen's Gambit with a deadly g2 bishop pressuring Black's entire queenside.",
            new String[]{"d4","Nf6","c4","e6","g3","d5","Bg2","dxc4","Nf3","Be7","O-O","O-O","Qc2","a6","Qxc4","b5","Qc2","Bb7"},
            new String[]{
                "d4.", "Nf6.", "c4.", "e6.", "The Catalan! Prepares a long-term monster bishop on g2.",
                "Black claims the center.", "X-rays d5 and the entire queenside.", "Open Catalan: grabs c4 to gain time.",
                "Develops and leaves the c4 pawn temporarily.", "Prepares castling.", "King safety first.", "Castles.",
                "Prepares to recapture c4 cleanly.", "Prepares ...b5.", "Restores material.", "Gains queenside space.",
                "Drops back to the c-file.", "Opposes White's Catalan bishop—strategic battle!"
            }));

        list.add(o("trompowsky", "A45", "Trompowsky Attack", "Main Line (2...Ne4)", CAT_CLOSED,
            Color.WHITE, "Intermediate", "Medium",
            "Practical surprise weapon against 1...Nf6! Immediately threatens to damage Black's kingside pawns and bypasses Indian Defense theory.",
            new String[]{"d4","Nf6","Bg5","Ne4","Bf4","d5","e3","c5","Bd3","Nf6","c3","Nc6","Nd2"},
            new String[]{
                "d4.", "Nf6.", "The Trompowsky! Sidesteps King's Indian and Nimzo-Indian completely.",
                "Black's sharpest reply: attacks the bishop immediately.", "Retreats to a strong London-like diagonal.",
                "Claims central space.", "Solid support.", "Challenges d4.", "Attacks the e4 knight.", "Retreats safely.",
                "Solid pyramid.", "Develops.", "Flexible development with easy plans."
            }));

        // ==================== INDIAN DEFENSES (1.d4 Nf6) ====================
        list.add(o("kid-classical", "E90", "King's Indian Defense", "Classical Main Line", CAT_INDIAN,
            Color.BLACK, "Intermediate", "Very High",
            "Kasparov and Fischer's fiercest weapon against 1.d4! Black allows White a big center, then unleashes a devastating ...f5-f4-g5 kingside mate attack.",
            new String[]{"d4","Nf6","c4","g6","Nc3","Bg7","e4","d6","Nf3","O-O","Be2","e5","O-O","Nc6","d5","Ne7","Ne1","Nd7","Be3","f5"},
            new String[]{
                "d4.", "Controls e4.", "Claims queenside space.", "Prepares the King's Indian fianchetto!", "Takes central control.",
                "The iconic dark-squared bishop.", "White builds a huge pawn center.", "Controls e5 and prepares ...e5.",
                "Develops.", "Quick castling.", "Solid development.", "Strikes at White's center!",
                "White castles.", "Pressures d4!", "Mar del Plata structure: locks the center! White attacks queenside, Black attacks kingside.",
                "Knight pivots toward the kingside.", "Prepares Nd3 and c5.", "Clears the f-pawn to launch the attack!",
                "Controls c5.", "THE BREAK! Black launches a ferocious kingside pawn storm!"
            }));

        list.add(o("nimzo-indian", "E20", "Nimzo-Indian Defense", "Rubinstein Main Line", CAT_INDIAN,
            Color.BLACK, "Intermediate", "Very High",
            "The gold standard of hypermodern chess. Black pins White's c3 knight immediately to fight for e4 with pieces rather than pawns.",
            new String[]{"d4","Nf6","c4","e6","Nc3","Bb4","e3","O-O","Bd3","d5","Nf3","c5","O-O","Nc6","a3","Bxc3","bxc3","dxc4","Bxc4","Qc7"},
            new String[]{
                "d4.", "Nf6.", "c4.", "Opens the f8 bishop.", "Threatens e4.", "The Nimzo-Indian! Pins c3 and stops e4.",
                "Rubinstein Variation: solid and flexible.", "Immediate king safety.", "Develops aiming kingside.",
                "Stakes a claim in the center.", "Develops.", "Strikes at White's center.", "White castles.",
                "Increases central pressure.", "Forces the bishop to decide.", "Doubles White's c-pawns.", "Recaptures.",
                "Clarifies the center.", "Activates the bishop.", "Prepares ...e5! Classic strategic clash."
            }));

        list.add(o("queens-indian", "E12", "Queen's Indian Defense", "Fianchetto Main Line", CAT_INDIAN,
            Color.BLACK, "Intermediate", "High",
            "Harmonious partner to the Nimzo-Indian when White plays 3.Nf3. Black fianchettoes on b7 to dominate the crucial e4 square.",
            new String[]{"d4","Nf6","c4","e6","Nf3","b6","g3","Ba6","b3","Bb4+","Bd2","Be7","Bg2","c6","O-O","d5"},
            new String[]{
                "d4.", "Nf6.", "c4.", "e6.", "Avoids the Nimzo pin.", "Queen's Indian! Prepares ...Bb7 or ...Ba6.",
                "White fianchettoes to contest the long diagonal.", "Modern line: immediately attacks the c4 pawn!",
                "Defends c4.", "Disrupts White's coordination with check.", "Blocks cleanly.", "Retreats safely.",
                "Completes fianchetto.", "Supports ...d5.", "Castles.", "Claims a rock-solid central share."
            }));

        list.add(o("grunfeld-exchange", "D85", "Grünfeld Defense", "Exchange Variation", CAT_INDIAN,
            Color.BLACK, "Advanced", "Very High",
            "Super-dynamic hypermodern favorite of MVL and Nepomniachtchi. Black lets White build a massive center, then destroys it with ...c5 and the g7 bishop!",
            new String[]{"d4","Nf6","c4","g6","Nc3","d5","cxd5","Nxd5","e4","Nxc3","bxc3","Bg7","Nf3","c5","Be3","Qa5","Qd2","Nc6"},
            new String[]{
                "d4.", "Nf6.", "c4.", "g6.", "Nc3.", "The Grünfeld! Immediate central counter-strike.",
                "Exchange Variation: White grabs a huge center.", "Recaptures centrally.", "White takes the full center!",
                "Trades off the knight.", "Massive central pawn phalanx.", "The g7 bishop X-rays d4 and c3!",
                "Develops solidly.", "The signature Grünfeld blow! Immediately attacks d4.", "Reinforces d4.",
                "Pressures c3 and d4 simultaneously.", "Defends c3.", "Adds yet another attacker to d4!"
            }));

        list.add(o("modern-benoni", "A60", "Modern Benoni", "Classical Main Line", CAT_INDIAN,
            Color.BLACK, "Advanced", "Medium",
            "Asymmetrical, fighting defense where Black creates a queenside pawn majority and aims for dynamic piece play along the e-file and dark squares.",
            new String[]{"d4","Nf6","c4","c5","d5","e6","Nc3","exd5","cxd5","d6","e4","g6","Nf3","Bg7","Be2","O-O","O-O","Re8"},
            new String[]{
                "d4.", "Nf6.", "c4.", "The Benoni! Immediately fights for dark squares.", "White claims space.",
                "Undermines d5 right away.", "Develops.", "Creates the asymmetrical Benoni structure.", "White has a central majority; Black has a queenside majority.",
                "Stops e5.", "White builds a big center.", "Prepares the deadly g7 bishop.", "Develops.", "Slices down the a1-h8 diagonal.",
                "Prepares castling.", "Black castles.", "White castles.", "Pressures White's e4 pawn immediately!"
            }));

        list.add(o("benko-gambit", "A57", "Benko Gambit", "Fully Accepted", CAT_INDIAN,
            Color.BLACK, "Intermediate", "Medium",
            "Positional gambit extraordinaire! Black sacrifices the b-pawn on move 3 to open the a- and b-files for enduring endgame-long queenside pressure.",
            new String[]{"d4","Nf6","c4","c5","d5","b5","cxb5","a6","bxa6","Bxa6","Nc3","d6","e4","Bxf1","Kxf1","g6"},
            new String[]{
                "d4.", "Nf6.", "c4.", "c5.", "d5.", "The Benko Gambit! Immediately undermines White's queenside.",
                "White accepts.", "Offers a second pawn to open BOTH the a- and b-files!", "Fully accepted.",
                "Develops with tempo on f1.", "Controls e4.", "Solidifies the center.", "White grabs central space.",
                "Strips White of castling rights!", "White must recapture with the king.", "Prepares ...Bg7, ...O-O, and relentless rook pressure on a2 and b2!"
            }));

        list.add(o("dutch-leningrad", "A87", "Dutch Defense", "Leningrad Variation", CAT_INDIAN,
            Color.BLACK, "Intermediate", "Medium",
            "Ambitious, aggressive reply to 1.d4! Combines the Dutch ...f5 with a King's Indian fianchetto for fierce kingside and central play.",
            new String[]{"d4","f5","g3","Nf6","Bg2","g6","Nf3","Bg7","O-O","O-O","c4","d6","Nc3","Qe8"},
            new String[]{
                "d4.", "The Dutch Defense! Immediately fights for the e4 square.", "White fianchettoes to challenge the long diagonal.",
                "Controls e4.", "Strong diagonal.", "Leningrad setup: prepares ...Bg7 and ...e5!", "Develops.",
                "Active dark-squared bishop.", "Castles.", "Castles.", "Claims queenside space.", "Prepares ...e5.",
                "Develops.", "Classic Leningrad queen move—prepares ...e5 and eyes the kingside!"
            }));

        // ==================== FLANK OPENINGS ====================
        list.add(o("english-symmetrical", "A30", "English Opening", "Symmetrical Variation", CAT_FLANK,
            Color.WHITE, "Intermediate", "High",
            "Flexible, strategic powerhouse! White controls d5 from the flank, keeping options open to transpose or outmaneuver Black on the long diagonal.",
            new String[]{"c4","c5","Nc3","Nc6","g3","g6","Bg2","Bg7","Nf3","Nf6","O-O","O-O","d4","cxd4","Nxd4"},
            new String[]{
                "The English Opening! Controls d5 without committing central pawns.", "Symmetrical reply.", "Controls d5 and e4.",
                "Black mirrors.", "Prepares kingside fianchetto.", "Black mirrors.", "Dominates the h1-a8 diagonal.",
                "Opposes on the long diagonal.", "Develops smoothly.", "Develops.", "King safety.", "Castles.",
                "White breaks open the center first!", "Black captures.", "Centralized knight with strong initiative."
            }));

        list.add(o("english-reversed-sicilian", "A20", "English Opening", "Reversed Sicilian (1...e5)", CAT_FLANK,
            Color.WHITE, "Intermediate", "Very High",
            "Like playing the Sicilian Defense with an extra tempo! White controls d5 with the c-pawn, g2 bishop, and Nc3, aiming for b4-b5 expansion.",
            new String[]{"c4","e5","Nc3","Nf6","Nf3","Nc6","g3","d5","cxd5","Nxd5","Bg2","Nb6","O-O","Be7","d3","O-O","Be3"},
            new String[]{
                "English Opening.", "Black claims the center, creating a Reversed Sicilian!", "Controls d5.", "Develops.",
                "Attacks e5.", "Defends e5.", "Prepares the mighty g2 Dragon bishop—with an extra tempo!", "Black strikes centrally.",
                "Trades flank for center pawn.", "Recaptures.", "Fianchettoes with pressure on d5.", "Retreats away from the g2 bishop's fire.",
                "Castles.", "Develops.", "Controls e4.", "Castles.", "Smooth, harmonious queenside pressure."
            }));

        list.add(o("reti-opening", "A09", "Réti Opening", "Classical Fianchetto", CAT_FLANK,
            Color.WHITE, "Intermediate", "High",
            "Pure hypermodern chess! Named after Richard Réti, White controls the center from afar with pieces and strikes back with c4 and b3.",
            new String[]{"Nf3","d5","c4","d4","e3","Nc6","exd4","Nxd4","Nxd4","Qxd4","Nc3","e5","d3","Nf6","Be3"},
            new String[]{
                "The Réti! Flexible, controls e5 and d4.", "Black occupies the center.", "Immediately challenges d5 from the flank!",
                "Black gains space.", "Undermines d4 right away.", "Supports d4.", "Opens the center.", "Centralizes.",
                "Trades off the active knight.", "Queen recaptures.", "Develops with tempo on the queen!", "Black takes central space.",
                "Solid support.", "Develops.", "Develops with tempo again—lead in development!"
            }));

        list.add(o("birds-opening", "A02", "Bird's Opening", "Dutch Reversed", CAT_FLANK,
            Color.WHITE, "Beginner", "Medium",
            "Fun, offbeat attacking weapon! White clamps down on the e5 square from move 1, playing a Dutch Defense with an extra move.",
            new String[]{"f4","d5","Nf3","Nf6","e3","g6","b3","Bg7","Bb2","O-O","Be2","c5","O-O","Nc6"},
            new String[]{
                "Bird's Opening! Immediately grabs control of e5.", "Black claims central space.", "Controls e5 and guards h4.",
                "Develops.", "Solid support.", "Black fianchettoes.", "Prepares Bb2 to supercharge control of e5!",
                "Long diagonal.", "The b2 bishop slices right toward Black's king!", "Castles.", "Prepares castling.",
                "Black challenges the center.", "Castles.", "Rich, unorthodox strategic battle."
            }));

        list.add(o("nimzo-larsen", "A01", "Nimzowitsch-Larsen Attack", "1.b3 Classical", CAT_FLANK,
            Color.WHITE, "Beginner", "Medium",
            "Creative hypermodern weapon popularized by Bent Larsen and Hikaru Nakamura. White fianchettoes on b2 immediately to pressure e5 and Black's kingside.",
            new String[]{"b3","e5","Bb2","Nc6","e3","Nf6","Bb5","Bd6","Na3","O-O","Nc4","Re8","Ne2"},
            new String[]{
                "1.b3! Sidesteps all mainstream theory.", "Black claims the center.", "Immediately attacks e5!",
                "Defends e5.", "Opens the light-squared bishop.", "Develops.", "Pins the defender of e5!",
                "Protects e5 solidly.", "Heads straight to c4 to attack the d6 bishop!", "Castles.",
                "Pressures d6 and e5.", "Supports e5.", "Flexible kingside development—fun, creative chess!"
            }));

        return Collections.unmodifiableList(list);
    }
}
