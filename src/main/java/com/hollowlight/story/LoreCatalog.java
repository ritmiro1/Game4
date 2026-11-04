package com.hollowlight.story;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The whole story, in the order the world can tell it.
 *
 * <p><strong>Spoilers live here.</strong> The design rule for this file: no fragment may
 * explain what a later fragment reveals. The truth about Iver and the truth about the player
 * are both stated plainly, but only in the Memorial Depths and the Heart, and only after
 * thirty fragments have quietly implied them.</p>
 */
public final class LoreCatalog {

    private static final Map<String, LoreEntry> BY_ID = new LinkedHashMap<>();
    private static final List<LoreEntry> ALL = new ArrayList<>();

    // ============================ CHAPTER ZERO — THE SURFACE =============================
    private static final LoreEntry SHRINE = add(new LoreEntry("shrine_dedication",
            LoreCategory.INSCRIPTION, 0, "The Dedication Stone", "",
            List.of("THIS WELL WAS DUG IN THE YEAR OF NINE NAMES.",
                    "IT IS NOT FOR WATER.",
                    "WHOEVER READS THIS: THE LAMPS ARE ALREADY LIT BELOW.",
                    "DO NOT ADD YOURS TO THEM.")));

    private static final LoreEntry SHRINE_LAMP = add(new LoreEntry("shrine_first_lamp",
            LoreCategory.ROOM, 0, "A Lamp Left Burning", "unknown",
            List.of("Someone has been keeping a lamp lit at the mouth of the shrine.",
                    "The oil is fresh. The wick is trimmed.",
                    "There is no one here. There has not been anyone here for a long time.")));

    // ====================== CHAPTER I — THE FORGOTTEN HALLS (LAYER 1) ===================
    private static final LoreEntry FIRST_DESCENT = add(new LoreEntry("page_the_first_descent",
            LoreCategory.JOURNAL, 1, "The Ninth Day of Ash", "Maravell Vane",
            List.of("They will tell you it was an accident. It was arithmetic.",
                    "Nine men in the seventh seam. Brannoc did the sum out loud in the yard: nine men",
                    "versus the price of the salt that would rot if the water got in.",
                    "He said the word 'sealed' the way you say 'closed', like a door, like a lid.",
                    "My brother was the seventh name on his list and the fourth on mine.")));

    private static final LoreEntry LAMPS_PROMISE = add(new LoreEntry("page_the_lamps",
            LoreCategory.JOURNAL, 1, "What My Father Taught Me", "Maravell Vane",
            List.of("Odren lost his eyes to the seam when I was eleven, and after that he counted",
                    "lamps instead of steps. He said: a lamp is not a light, Mara. A lamp is a",
                    "promise. You put one somewhere dark and you are saying, out loud, to nobody:",
                    "someone is coming back this way.",
                    "I have made four hundred promises. I intend to keep them all.")));

    private static final LoreEntry BRANNOC_LEDGER = add(new LoreEntry("brannoc_ledger_page",
            LoreCategory.LETTER, 1, "Ledger of the Hollowmere Mine, Entry 9", "Elder Brannoc Tull",
            List.of("Seam seven: collapse, 04:20. Nine names recorded and struck.",
                    "Shaft sealed 11:00 by order of the elder, to preserve the upper seam.",
                    "No survivors. No descent attempted.",
                    "Memorial bread and salt to be left on the ninth step for the families.",
                    "The elder's lamp was not relit that evening. — clerk's note, in another hand")));

    private static final LoreEntry BREAD_DEAD = add(new LoreEntry("inscription_bread_for_the_dead",
            LoreCategory.INSCRIPTION, 1, "Carved Above a Step", "",
            List.of("WE LEFT BREAD ON THE NINTH STEP.",
                    "IT IS STILL THERE.",
                    "NOBODY HAS EVER COME UP TO EAT IT.")));

    private static final LoreEntry COUNT_STEPS = add(new LoreEntry("inscription_count_the_steps",
            LoreCategory.INSCRIPTION, 1, "Carved Beside a Doorway", "",
            List.of("COUNT THE STEPS BETWEEN LAMPS.",
                    "IF YOU REACH ONE HUNDRED AND THERE IS NO LAMP,",
                    "STOP WALKING. YOU ARE NO LONGER WHERE YOU THINK.")));

    private static final LoreEntry TALLIES = add(new LoreEntry("tally_marks",
            LoreCategory.INSCRIPTION, 1, "Tallies", "",
            List.of("Four strokes, then a fifth crossing them out.",
                    "They stop being tidy somewhere past the two hundredth.",
                    "The last ones are scratched so shallow they barely took the stone.",
                    "Whoever counted these was running out of hand.")));

    private static final LoreEntry FOURTEEN_DAYS = add(new LoreEntry("page_fourteen_days_of_oil",
            LoreCategory.JOURNAL, 1, "What I Brought Down", "Maravell Vane",
            List.of("Fourteen days of oil. Twelve candles. A rope I do not trust.",
                    "Bread for two, which was habit and not hope.",
                    "A photograph, because I thought I would need his face to find him.",
                    "I have been down here long enough that the photograph is only grey now.")));

    private static final LoreEntry MINE_KIND = add(new LoreEntry("inscription_the_mine_is_kind",
            LoreCategory.INSCRIPTION, 1, "Carved Into a Wall That Breathes", "",
            List.of("THE MINE IS NOT CRUEL.",
                    "IT ONLY TAKES WHAT YOU HOLD OUT TO IT.",
                    "THAT IS WHY IT IS STILL HUNGRY.")));

    private static final LoreEntry FIRST_GIFT = add(new LoreEntry("page_first_gift",
            LoreCategory.JOURNAL, 1, "The Corridor That Opened", "Maravell Vane",
            List.of("I could not find the way past the fall. I sat down and I said, out loud,",
                    "like a fool: I will give anything.",
                    "And it asked. Not in words. It put the question in my head the way a hand",
                    "finds a pocket.",
                    "I gave it my mother's kitchen — the whole of it, the window, the smell of the",
                    "fat in the pan — and the stones moved aside like a held breath.",
                    "I did not know it was a trade. I thought it was a door.")));

    // ====================== CHAPTER II — THE DROWNED CORRIDORS (LAYER 2) ================
    private static final LoreEntry FLOOD_YEAR = add(new LoreEntry("page_the_flood_year",
            LoreCategory.JOURNAL, 2, "The Winter the River Came In", "Maravell Vane",
            List.of("Iver was nine. The river came into the upper seam and drowned the ponies",
                    "and Brannoc made us all stand in the yard while he counted the living.",
                    "Iver counted too, from the back, in a whisper, getting it wrong on purpose so",
                    "that the number would be bigger.",
                    "Down here the water is the same water. I know it is. I can taste the ash in it.")));

    private static final LoreEntry WATER_REMEMBERS = add(new LoreEntry("inscription_water_remembers",
            LoreCategory.INSCRIPTION, 2, "Written in Salt", "",
            List.of("THE WATER REMEMBERS THE SHAPE OF THE ROOM YOU FORGOT.",
                    "THAT IS WHY IT IS FULL OF DOORWAYS.")));

    private static final LoreEntry LETTER_TO_IVER = add(new LoreEntry("letter_to_iver",
            LoreCategory.LETTER, 2, "A Letter, Never Sent", "Maravell Vane",
            List.of("Iver —",
                    "I have gone down twice and come up once. On the way up I could not remember",
                    "what I was carrying. I only knew it was heavy and that it was yours.",
                    "If you are reading this it means I stopped walking and put the lamp down,",
                    "which is the only way a lamplighter ever gives up.",
                    "Come and find me. I will keep one lit.")));

    private static final LoreEntry BARGAIN = add(new LoreEntry("page_the_bargain",
            LoreCategory.JOURNAL, 2, "The Terms", "Maravell Vane",
            List.of("It does not want blood. I have offered. It has no use for blood.",
                    "It wants the things that make a person findable.",
                    "Names. Voices. The order of a face.",
                    "It builds with what it is given. I have walked through four rooms that were",
                    "made out of my own childhood and I did not recognise a single one.")));

    private static final LoreEntry PHOTOGRAPH = add(new LoreEntry("photograph",
            LoreCategory.ITEM, 2, "Water-Stained Photograph", "found",
            List.of("Two children in front of the mine office, squinting.",
                    "The smaller one is holding something up — a tin whistle, maybe.",
                    "The water has taken the faces. Only the two pairs of boots are certain.",
                    "Someone has written on the back, and the ink has run: 'the day he was given it.'")));

    private static final LoreEntry WHAT_I_GAVE_NEXT = add(new LoreEntry("page_what_i_gave_next",
            LoreCategory.JOURNAL, 2, "What I Gave Next", "Maravell Vane",
            List.of("His laugh. That is what it asked for, and I paid it, because the way past the",
                    "water was three feet wide and I was in a hurry.",
                    "I know it was a good laugh. I know it was the one he did when he was pretending",
                    "not to be pleased.",
                    "I cannot hear it anymore. I have tried, in quiet rooms, for a very long time.")));

    private static final LoreEntry CHOIR_NOTE = add(new LoreEntry("choir_note",
            LoreCategory.VOICE, 2, "What the Drowned Learn", "found",
            List.of("The ones in the flooded halls are not singing.",
                    "They are reciting: every name they have ever been told, in the order they were",
                    "told it, over and over, so that the water cannot take the list.",
                    "If you listen long enough you will hear a name you know.",
                    "That is how they get you to answer.")));

    private static final LoreEntry DO_NOT_ANSWER = add(new LoreEntry("inscription_do_not_answer",
            LoreCategory.INSCRIPTION, 2, "Under the Waterline", "",
            List.of("DO NOT ANSWER WHEN IT CALLS A NAME IN YOUR VOICE.",
                    "IT IS NOT CALLING FOR YOU.",
                    "IT IS PRACTISING.")));

    // ========================= CHAPTER III — THE LIVING WALLS (LAYER 3) ==================
    private static final LoreEntry WALLS_PULSE = add(new LoreEntry("page_the_walls_pulse",
            LoreCategory.JOURNAL, 3, "The Walls Have a Pulse", "Maravell Vane",
            List.of("The stone here is not stone. It gives when I lean on it, the way a cheek gives.",
                    "There are rooms below that I built by forgetting, and they are still warm.",
                    "I have started to be afraid of how easy it is.",
                    "You would think losing yourself would hurt. It is only very quiet.")));

    private static final LoreEntry WEARS_MY_FACE = add(new LoreEntry("inscription_it_wears_my_face",
            LoreCategory.INSCRIPTION, 3, "Scratched in Panic", "",
            List.of("IT WEARS MY FACE TO WALK THE HALLS I MADE.",
                    "IF YOU MEET ME AND I AM SMILING,",
                    "RUN. I HAVE NOT SMILED SINCE THE SEVENTH SEAM.")));

    private static final LoreEntry BRANNOC_SECOND = add(new LoreEntry("brannoc_second_ledger",
            LoreCategory.LETTER, 3, "Ledger of the Hollowmere Mine, Entry 41", "Elder Brannoc Tull",
            List.of("The Vane girl has gone down again. Third time. I have told the yard to stop",
                    "leaving the gate open for her; she does not use gates.",
                    "The seam above produced forty tons this quarter. The families are fed.",
                    "I keep a lamp on the sill. I do not know why I keep it.",
                    "I think there is a version of this where I went down with a rope, and the ledger",
                    "of that version is not written by me.")));

    private static final LoreEntry CANNOT_FIND_WAY_BACK = add(new LoreEntry("page_i_cannot_find_the_way_back",
            LoreCategory.JOURNAL, 3, "The Way Back", "Maravell Vane",
            List.of("I turned around today and could not find the way back up.",
                    "Not because the tunnels changed. Because I could not remember which direction",
                    "'up' had been for me, before.",
                    "I sat with the lamp until it burned low and then I did the arithmetic again.",
                    "Fourteen days of oil. Four hundred promises. One brother. I am sorry, Odren.")));

    private static final LoreEntry STITCHER_NOTE = add(new LoreEntry("stitcher_note",
            LoreCategory.ROOM, 3, "Her Mother's Sewing Kit", "found",
            List.of("A thousand threads run through the walls of this floor, all of them pulled tight.",
                    "They come from a kit: bone needles, wax thread, a pincushion shaped like a tomato,",
                    "given to a girl in the year the river came in and kept long after it was useful.",
                    "The thing that stitches the corridors shut is not malice.",
                    "It is somebody's mother, trying to make the place safe, and failing, and trying.")));

    private static final LoreEntry SORROW_MINE = add(new LoreEntry("inscription_the_sorrow_is_mine",
            LoreCategory.INSCRIPTION, 3, "In a Hall of Ribs", "",
            List.of("THE SORROW IN THESE HALLS IS NOT YOURS.",
                    "BUT IT IS CONTAGIOUS,",
                    "THE WAY ALL TRUE THINGS ARE.")));

    private static final LoreEntry GAVE_VOICE = add(new LoreEntry("page_she_gave_her_voice",
            LoreCategory.JOURNAL, 3, "Trade Four", "Maravell Vane",
            List.of("I gave it my voice. It was the only thing left that it wanted and I was tired",
                    "of the sound of it echoing back at me.",
                    "I write now because writing is not sound.",
                    "If you hear a woman calling a name down here, it is not me calling.",
                    "I cannot call. I have not been able to call for eleven days.")));

    // ======================= CHAPTER IV — THE MEMORIAL DEPTHS (LAYER 4) =================
    private static final LoreEntry KITCHEN = add(new LoreEntry("page_a_kitchen_underground",
            LoreCategory.JOURNAL, 4, "There Is a Kitchen Down Here", "Maravell Vane",
            List.of("I found a room with a window in it. Underground. A window, with a curtain.",
                    "A table set for three. A pot on a cold hook. Two pairs of boots by the door and",
                    "a third pair that is smaller than I remember anyone's being.",
                    "I gave this room away in the second year. It took it out of me whole.",
                    "I am standing in my own kitchen and I do not know where the cups were kept.")));

    private static final LoreEntry IVER_FIRST = add(new LoreEntry("iver_first_carving",
            LoreCategory.INSCRIPTION, 4, "Scratched With a Nail", "Iver Vane",
            List.of("MARA.",
                    "I CAN HEAR YOU. I AM HERE. I HAVE BEEN HERE.",
                    "I AM NOT IN THE DEEP. I AM NEAR THE TOP.",
                    "PLEASE.")));

    private static final LoreEntry IVER_SECOND = add(new LoreEntry("iver_second_carving",
            LoreCategory.INSCRIPTION, 4, "Scratched Lower Down", "Iver Vane",
            List.of("YOU WALKED PAST AGAIN TODAY. I COULD SEE YOUR LAMP THROUGH THE FALL.",
                    "I CALLED UNTIL MY THROAT WAS RAW AND THEN I REALISED",
                    "YOU WERE CALLING TOO. YOU WERE CALLING MY NAME.",
                    "YOU WERE WALKING PAST ME AND SAYING MY NAME LIKE IT WAS A PRAYER",
                    "FOR SOMEONE ELSE.")));

    private static final LoreEntry IVER_LAST = add(new LoreEntry("iver_last_carving",
            LoreCategory.INSCRIPTION, 4, "The Last Marks", "Iver Vane",
            List.of("I HAVE STOPPED COUNTING. THE TALLIES ARE ONLY A HABIT NOW.",
                    "I AM NOT ANGRY. I WANT THAT WRITTEN.",
                    "YOU WERE NOT LEAVING ME. YOU WERE LOOKING FOR ME.",
                    "THAT IS THE ONLY THING I GOT RIGHT DOWN HERE, MARA:",
                    "SOMEBODY WAS ALWAYS LOOKING FOR ME. NOBODY EVER HAD TO BE ALONE.",
                    "IT WAS NEVER YOUR FAULT. — I")));

    private static final LoreEntry FACE_GAVE_AWAY = add(new LoreEntry("page_the_face_i_gave_away",
            LoreCategory.JOURNAL, 4, "What It Cost To Reach This Door", "Maravell Vane",
            List.of("I paid for the last door with his face.",
                    "I know the shape of the reasoning. It was not a shape, it was an exhaustion.",
                    "I need you to understand that I would have paid with my hands.",
                    "I would have paid with anything it could not have used.")));

    private static final LoreEntry LETTER_NEVER_READ = add(new LoreEntry("letter_never_read",
            LoreCategory.LETTER, 4, "The Letter", "Maravell Vane",
            List.of("Iver —",
                    "I found a room with your boots in it and I did not know they were yours",
                    "until I had already put them straight and turned to go.",
                    "I have gone past you. I am certain now. The arithmetic is unforgivable:",
                    "nine names at the top of the mine, and I have been walking down for eleven years",
                    "looking at the floor.",
                    "If there is any way for a person to be sorry enough to undo a thing,",
                    "tell me what it is, and I will do it in the dark, alone, with a bad lamp.")));

    private static final LoreEntry MEMORIAL_BED = add(new LoreEntry("memorial_room_bed",
            LoreCategory.ROOM, 4, "A Bed With Two Pillows", "found",
            List.of("A narrow bed, made. Two pillows, one of them swollen with something.",
                    "Inside the second pillow: a child's tin whistle, wrapped in a page torn from a",
                    "journal, and a lock of dark hair tied with mine-twine.",
                    "The page is an inventory. Oil, candles, rope, bread for two.",
                    "All of it is crossed out except: 'and a reason'.")));

    private static final LoreEntry FORGOT_HIS_NAME = add(new LoreEntry("page_i_forgot_his_name_yesterday",
            LoreCategory.JOURNAL, 4, "Yesterday", "Maravell Vane",
            List.of("Yesterday I could not remember his name and I wrote down what I had left:",
                    "a brother. younger. a whistle. the seventh name.",
                    "And then I wrote underneath it, in the same hand,",
                    "'he was wearing boots'",
                    "because that was all I could still see.")));

    private static final LoreEntry MUSIC_BOX_TUNE = add(new LoreEntry("music_box_tune",
            LoreCategory.ITEM, 4, "Four Notes", "found",
            List.of("The box plays four notes and then a fifth one that is not there.",
                    "Odren whistled it coming up from the shaft so that the children in the yard",
                    "would know it was him and not a stranger with a lamp.",
                    "It is the tune of a man announcing he is still alive.",
                    "Iver's whistle only ever got the first two notes right.")));

    // ============================== CHAPTER V — THE HEART (LAYER 5) =====================
    private static final LoreEntry LAST_LAMP = add(new LoreEntry("page_the_last_lamp",
            LoreCategory.JOURNAL, 5, "The Last Entry", "Maravell Vane",
            List.of("I am at the bottom. There is a lamp here and it is the biggest thing I have ever",
                    "seen and it is keeping nothing alive except itself.",
                    "It asked for my sight and I gave it, and I am writing this by touch, and my",
                    "handwriting is going to be a disgrace.",
                    "There is one more thing it wants and I already know what it is.",
                    "It wants the memory of the boy I came down for. It has been patient about it.",
                    "If someone ever reads this: I am sorry about the lamps. I meant to keep them all.")));

    private static final LoreEntry HEART_ROOM = add(new LoreEntry("inscription_the_heart_is_a_room",
            LoreCategory.INSCRIPTION, 5, "On the Inside of the Last Door", "",
            List.of("THE HEART IS A ROOM WHERE ONE PERSON WAITS",
                    "AND ANOTHER PERSON KEEPS LOOKING.",
                    "IT IS NOT A PLACE. IT IS AN ARGUMENT",
                    "THAT HAS BEEN GOING ON FOR ELEVEN YEARS.")));

    private static final LoreEntry LAMPKEEPER_WHISPER = add(new LoreEntry("lampkeeper_whisper",
            LoreCategory.VOICE, 5, "What She Says", "the lamplighter",
            List.of("Iver?",
                    "Only that. She says it into every corridor, in a voice made of other people's",
                    "greetings, and every lamp in the labyrinth turns toward the sound.",
                    "She has been saying it since before you were born.",
                    "She is not asking the dark. She is asking you.")));

    private static final LoreEntry ENDING_RELEASE = add(new LoreEntry("ending_release",
            LoreCategory.VOICE, 5, "Extinguishing", "the lamplighter",
            List.of("You put your hand on the wick and the whole labyrinth stops pretending.",
                    "She looks at you — properly, with eyes she gave away eleven years ago —",
                    "and she says the name once, correctly, to your face.",
                    "Then the lamps go out, one by one, upstream of her, like people leaving a room",
                    "they were only ever waiting in.",
                    "In the morning the shrine is full of daylight and the mine is only a mine.")));

    private static final LoreEntry ENDING_KEEP = add(new LoreEntry("ending_keep",
            LoreCategory.VOICE, 5, "Keeping", "the lamplighter",
            List.of("You lift the wick out of her hands and it does not go out.",
                    "The labyrinth settles. Somewhere below, a voice that is almost yours begins",
                    "to say a name into the corridors, patiently, for whoever comes next.",
                    "Above, the villagers notice the lamp in the shrine has been relit.",
                    "They do not notice that it is burning a little differently now,",
                    "nor that the person keeping it never comes up for bread.")));

    private static final LoreEntry ENDING_RETURN = add(new LoreEntry("ending_return",
            LoreCategory.VOICE, 5, "Carrying Him Up", "Iver Vane",
            List.of("The whistle gets two notes out of you, and the dust in your pocket moves.",
                    "You say his name into the Heart, which has never once been given a name.",
                    "It is the one thing it cannot keep: something that was already being carried.",
                    "The dust goes quiet. The labyrinth lets go of the shape it was holding.",
                    "You climb for a long time. It is lighter than going down,",
                    "and at the ninth step there is bread, and you eat it, because it is for you.")));

    // ================================ SMALLER FRAGMENTS =================================
    private static final LoreEntry MEMORY_SHARD = add(new LoreEntry("memory_shard",
            LoreCategory.ITEM, 3, "A Shard of Someone", "found",
            List.of("A splinter of lamp glass with a memory caught in it, like a fly in amber.",
                    "Hold it and you are briefly certain of a summer you never had.",
                    "It dissolves in the mouth and takes the certainty with it.")));

    private static final LoreEntry CARVED_TOKEN = add(new LoreEntry("carved_token",
            LoreCategory.ITEM, 1, "Miner's Token", "found",
            List.of("A salt-pale disc with a number scratched on it: SEVEN.",
                    "Every man in the seventh seam carried one so that they could be counted",
                    "by a man at a table instead of by their faces.",
                    "This one was never handed in.")));

    private static final LoreEntry IVER_WHISTLE_NOTE = add(new LoreEntry("iver_whistle_note",
            LoreCategory.ITEM, 4, "Two Notes, Both Wrong", "Iver Vane",
            List.of("The whistle is tin and dented and only speaks two notes.",
                    "Whistle it in the deep and something will answer with the other three.",
                    "It will be the wrong thing answering. Answer it anyway,",
                    "because it has been eleven years since anyone did.")));

    private static final LoreEntry SAFE_ROOM_NOTE = add(new LoreEntry("safe_room_note",
            LoreCategory.ROOM, 0, "Someone Made This Room", "",
            List.of("A lamp. A blanket. A box with a lid that closes properly.",
                    "Somebody sat here and got their hands warm and then went on down.",
                    "The blanket is folded. The habits of being alive outlast the person.")));

    private static final LoreEntry ALTAR_INSPECTED = add(new LoreEntry("altar_inspected",
            LoreCategory.ROOM, 0, "The Altar Hums", "",
            List.of("Under the stone something is keeping time, very slowly.",
                    "Set a thing down here and the altar will look at it for a while,",
                    "and then it will give you back a version of it that works.")));

    private static final LoreEntry LAMPLIGHTERS_BAND = add(new LoreEntry("lamplighters_band",
            LoreCategory.ITEM, 5, "The Wearing Ring", "found",
            List.of("A plain iron band worn shiny on the inside where a thumb went round and round.",
                    "Lamplighters wear it so they can find their hand in the dark.",
                    "It is the only thing here that was ever used for comfort.")));

    private static final LoreEntry HEART_OF_THE_HOLLOW = add(new LoreEntry("heart_of_the_hollow",
            LoreCategory.ITEM, 5, "The Heart of the Hollow", "the lamplighter",
            List.of("It is warm and it is shaped like the inside of a hand.",
                    "It is not a heart. It is the thing she could not give away,",
                    "because it was the reason she came down,",
                    "and reasons are heavier than hearts.")));

    private static final LoreEntry ROOM_DINING = add(new LoreEntry("room_dining",
            LoreCategory.ROOM, 4, "A Table Set for Three", "found",
            List.of("Three plates. Two of them have food on them that was never eaten.",
                    "The third has a smaller plate stacked on top, for a child.",
                    "The chairs are pushed in.",
                    "The candles were lit and then snuffed and then not relit.")));

    private static final LoreEntry ROOM_GARDEN = add(new LoreEntry("room_garden",
            LoreCategory.ROOM, 2, "The Garden Below the Waterline", "found",
            List.of("Something has been growing in the flooded halls for a long time.",
                    "It is not a plant. It lights up when it hears breathing.",
                    "The pattern of it on the walls is deliberate, the way handwriting is deliberate.",
                    "It says, over and over: I AM STILL HERE, I AM STILL HERE.")));

    private static final LoreEntry OBSERVATION = add(new LoreEntry("observation_chamber",
            LoreCategory.ROOM, 3, "The Room That Watches", "found",
            List.of("A small room with a slot in the wall, at eye height, looking down a corridor.",
                    "There are worn patches on the floor where someone knelt for years.",
                    "You look through the slot. Somewhere down the corridor, something looks back",
                    "through the same slot, at the same time, and does not move.")));

    private static final LoreEntry WITHERED_CHOIR = add(new LoreEntry("choir_grief",
            LoreCategory.VOICE, 2, "Why They Sing", "found",
            List.of("The drowned do not want your life. They want your attention.",
                    "They are the nine, and then the forty after them, and the ones Brannoc did not",
                    "write down, and all of them would settle for being counted once.",
                    "That is what the singing is. It is a list.")));

    private static final LoreEntry FINAL_WARNING = add(new LoreEntry("final_warning",
            LoreCategory.INSCRIPTION, 5, "Before the Last Lamp", "",
            List.of("IF YOU GO THROUGH THIS DOOR YOU WILL BE THE THIRD THING",
                    "THAT HAS EVER LOVED HER FROM A DISTANCE.",
                    "SHE WILL NOT KNOW YOU. SHE WILL ASK ANYWAY.")));

    // ================================== PLACEMENT =======================================
    private static final List<String> LAYER_ONE_LORE = List.of(
            "page_the_first_descent", "page_the_lamps", "brannoc_ledger_page",
            "inscription_bread_for_the_dead", "inscription_count_the_steps", "tally_marks",
            "page_fourteen_days_of_oil", "inscription_the_mine_is_kind", "page_first_gift",
            "carved_token", "shrine_first_lamp", "safe_room_note", "altar_inspected");

    private static final List<String> LAYER_TWO_LORE = List.of(
            "page_the_flood_year", "inscription_water_remembers", "letter_to_iver",
            "page_the_bargain", "photograph", "page_what_i_gave_next", "choir_note",
            "inscription_do_not_answer", "room_garden", "choir_grief");

    private static final List<String> LAYER_THREE_LORE = List.of(
            "page_the_walls_pulse", "inscription_it_wears_my_face", "brannoc_second_ledger",
            "page_i_cannot_find_the_way_back", "stitcher_note", "inscription_the_sorrow_is_mine",
            "page_she_gave_her_voice", "memory_shard", "observation_chamber");

    private static final List<String> LAYER_FOUR_LORE = List.of(
            "page_a_kitchen_underground", "iver_first_carving", "iver_second_carving",
            "iver_last_carving", "page_the_face_i_gave_away", "letter_never_read",
            "memorial_room_bed", "page_i_forgot_his_name_yesterday", "music_box_tune",
            "iver_whistle_note", "room_dining");

    private static final List<String> LAYER_FIVE_LORE = List.of(
            "page_the_last_lamp", "inscription_the_heart_is_a_room", "lampkeeper_whisper",
            "final_warning", "lamplighters_band", "heart_of_the_hollow",
            "ending_release", "ending_keep", "ending_return");

    private LoreCatalog() {
    }

    private static LoreEntry add(LoreEntry entry) {
        if (BY_ID.containsKey(entry.id())) {
            throw new IllegalStateException("Duplicate lore id: " + entry.id());
        }
        BY_ID.put(entry.id(), entry);
        ALL.add(entry);
        return entry;
    }

    public static LoreEntry get(String id) {
        return BY_ID.get(id);
    }

    public static boolean contains(String id) {
        return BY_ID.containsKey(id);
    }

    public static List<LoreEntry> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static int size() {
        return ALL.size();
    }

    /** Fragments that can be placed in a given labyrinth layer, in reading order. */
    public static List<String> forLayer(int layer) {
        return switch (layer) {
            case 0 -> List.of("shrine_dedication", "shrine_first_lamp");
            case 1 -> LAYER_ONE_LORE;
            case 2 -> LAYER_TWO_LORE;
            case 3 -> LAYER_THREE_LORE;
            case 4 -> LAYER_FOUR_LORE;
            case 5 -> LAYER_FIVE_LORE;
            default -> List.of();
        };
    }

    /** Everything a player can discover by reading things, for journal totals. */
    public static List<LoreEntry> discoverable() {
        List<LoreEntry> out = new ArrayList<>();
        for (LoreEntry entry : ALL) {
            if (entry.id().startsWith("ending_") || entry.category() == LoreCategory.VOICE) {
                continue;
            }
            out.add(entry);
        }
        return out;
    }
}
