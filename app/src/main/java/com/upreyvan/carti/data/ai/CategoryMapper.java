package com.upreyvan.carti.data.ai;

import java.util.HashMap;
import java.util.Map;

public class CategoryMapper {
    private static final Map<String, String> MAPPING = new HashMap<>();

    public static final String[] CAT_FOOD = {
        "food", "pagkain", "meals", "mcdo", "jollibee", "kfc", "chowking", "mang inasal", "greenwich", "jabee", "burger king", "popeyes", "bonchon", "army navy", "tokyo tokyo", "shakeys", "pizza hut", "yellow cab", "angel's pizza", "don machi", "pancake house", "max's", "kenny rogers", "peri-peri", "pepper lunch", "yabu", "ippudo", "mendokoro", "marugame", "ramen", "sushi", "shabu-shabu", "hotpot", "samgyupsal", "kbbq", "buffet", "vikings", "starbucks", "coffee", "kape", "milk tea", "milktea", "tea", "coke", "soda", "softdrinks", "beer", "alak", "wine", "shot", "inumin", "juice", "shake", "frappe", "itlog", "egg", "bigas", "kanin", "tinapay", "bread", "ulam", "biscuit", "chips", "junk food", "snacks", "dunkin", "jco", "goldilocks", "red ribbon", "contis", "mary grace", "balut", "taho", "halo-halo", "street food", "restaurant", "resto", "breakfast", "lunch", "dinner", "tanghalian", "hapunan", "merienda", "chibog", "tsibog", "sharon", "shanghai", "kakanin", "pares", "siomai", "fishball", "kwek kwek", "tusok-tusok", "isaw", "mami", "goto", "lugaw", "tokwa", "pancit", "canton", "bihon", "palabok", "sisig", "lechon", "liempo", "takoyaki", "shawarma", "burger", "fries", "hotdog", "adidas", "betamax", "helmet", "walkman", "kikiam", "calamares", "tokneneng", "prokpop", "binalot", "silog", "tapsilog", "longsilog", "tocilog", "bangsilog", "chiksilog", "papak", "nguya", "lunok", "kumatok", "pantawid gutom", "meryendang pinoy", "midnight snack", "picnic", "handaan", "kainan", "kater", "catering", "eat all you can", "unli rice", "extra rice", "sb", "cbtl", "san miguel", "red horse", "rh", "emperador", "alfonso", "gsm", "quatro kantos", "empe", "tanduay", "smirnoff", "bacardi", "buko juice", "gulaman", "palamig", "sago", "mountain dew", "pepsi", "royal", "sprite", "rc cola", "sting", "cobra", "booster", "red bull", "gatorade", "pocari sweat"
    };

    public static final String[] CAT_BILLS = {
        "electricity", "meralco", "kuryente", "ilaw", "light bill", "electric bill", "power", "kuryente bill", "electric coop", "veco", "cebecco", "pelco", "panelco", "miralco", "bayad kuryente", "electrician", "utility bill", "monthly bill", "kuryenti", "water", "maynilad", "manila water", "tubig", "water bill", "nawasa", "prime water", "laguna water", "baliwag water", "primewater", "bayad tubig", "water refill", "internet", "pldt", "converge", "globe fiber", "wifi", "broadband", "skycable", "red fiber", "gf fiber", "dito", "starlink", "radius", "cable", "netflix", "spotify", "youtube premium", "disney+", "hbo", "viu", "canva", "icloud", "google one", "prime video", "cignal", "yt premium", "load", "gcash load", "smart load", "globe load", "tnt", "tm", "promo", "data", "load na", "paload", "reload", "top up", "autoload", "share-a-load", "e-load", "gosurf", "magic data", "rent", "apartment", "condo", "upuhan", "bayad sa bahay", "housing", "mortgage", "amortization", "paupa", "upa", "dorm", "room rent", "association dues", "condo dues", "boarding house", "bh", "amort", "bayad renta", "bedspace", "insurance", "philhealth", "sss", "pagibig", "st peter", "health insurance", "life insurance", "vul", "sunlife", "pru life", "allianz", "fwd", "axa", "manulife", "medicard", "hmo", "stpeter", "memorial plan", "lpg", "gasul", "solane", "petronas", "fiesta gas", "m-gas", "cleaning", "laundry", "lavada", "labada", "dry clean", "pa-laundry", "bill"
    };

    public static final String[] CAT_TRANSPORT = {
        "gas", "gasolina", "petrol", "shell", "petron", "caltex", "unleaded", "premium", "full tank", "pa-gas", "v-power", "blaze", "xcs", "puno tangke", "gasoline", "fuel", "fare", "jeep", "bus", "taxi", "grab", "angkas", "joyride", "moveit", "maxim", "tricycle", "pasahe", "mrt", "lrt", "pnr", "beep", "habal", "habal-habal", "dyip", "modern jeep", "sidecar", "fx", "van", "barker", "pasahero", "commute", "maintenance", "car wash", "oil change", "tires", "spare parts", "repair", "pampagawa", "gulong", "baterya", "overhaul", "tune up", "vulcanizing", "wheel align", "brake fluid", "car parts", "motor parts", "change oil", "car accessories", "casa", "pacheck", "linis motor", "punas", "detailing", "ceramic coating", "brake pad", "wiper", "parking", "parking fee", "parkingan", "toll", "rfid", "easytrip", "autosweep", "drive-thru", "skyway", "nlex", "slex", "cavitex", "tplex", "valet", "toll fee", "tollgate", "naiax", "transport", "motor", "honda click", "nmax", "aerox", "pcx", "mio", "adv", "sniper", "raider", "helmet", "evo", "ls2", "agv", "kyt"
    };

    public static final String[] CAT_GADGETS = {
        "gadget", "phone", "iphone", "samsung", "oppo", "vivo", "xiaomi", "realme", "huawei", "android", "redmi", "poco", "honor", "infinix", "tecno", "macbook", "ipad", "laptop", "pc", "tablet", "monitor", "mouse", "keyboard", "airpods", "earbuds", "headphones", "headset", "powerbank", "harddrive", "usb", "cable", "charger", "case", "accessory", "gaming chair", "nintendo", "switch", "playstation", "xbox", "steam", "gpu", "cpu", "ram", "motherboard", "router", "tempered glass", "ring light", "vape", "pod", "relx", "vape juice"
    };

    public static final String[] CAT_APPLIANCES = {
        "tv", "television", "fridge", "refrigerator", "ref", "aircon", "ac", "washing machine", "dryer", "microwave", "oven", "stove", "induction", "electric fan", "fan", "kettle", "rice cooker", "air fryer", "blender", "toaster", "vacuum", "water dispenser", "heater", "appliance"
    };

    public static final String[] CAT_SHOPPING = {
        "shopping", "shopee", "lazada", "tiktok", "budol", "check out", "co", "sale", "ukay", "damit", "shoes", "sapatos", "bag", "perfume", "watch", "relo", "jewelry", "porma", "uniqlo", "h&m", "zara", "bench", "penshoppe", "adidas", "nike", "crocs", "hardware", "ace hardware", "handyman", "wilcon", "furniture", "ikea", "mall", "spaylater", "lazpaylater", "cod", "pambili", "kamiseta", "oxgn", "tribal", "jeans", "pantalon", "shorts", "skirt", "dress", "gown", "jacket", "hoodie", "sweater", "cap", "sumbrero", "shades", "sunglasses", "grocery", "palengke", "supermarket", "puregold", "savemore", "robinsons", "landers", "snr", "7-eleven", "711", "ministop", "uncle johns", "lawson", "alfa mart", "dali", "oj", "sari-sari store", "tindahan", "talipapa", "wet market", "waltermart", "shopwise", "marketplace", "merrymart", "gaisano", "all day", "sibuyas", "bawang", "kamatis", "luya", "mantika", "oil", "asin", "suka", "toyo", "patis", "bagoong", "magic sarap", "knorr", "ajinomoto", "century tuna", "argentina", "555", "maling", "spam", "purefoods", "itlog na pula", "itlog na maalat", "monay", "gardenia", "skyflakes", "fita", "rebisco", "oreo", "piattos", "nova", "vcut", "chippy", "clover", "papas", "bagoong alamang", "corned beef", "itlog pugo"
    };

    public static final String[] CAT_PERSONAL = {
        "hygiene", "shampoo", "soap", "toothpaste", "sabon", "sepilyo", "salon", "barber", "gupit", "parlor", "facial", "spa", "massage", "hilot", "waxing", "nails", "pedicure", "manicure", "gym", "anytime fitness", "workout", "protein", "supplement", "lotion", "deodorant", "tawas", "napkin", "tissue", "wipes", "conditioner", "pantene", "creamsilk", "sunsilk", "head & shoulders", "dove", "safeguard", "colgate", "close up", "mouthwash", "listerine", "sanitary pad", "modess", "whisper", "wet wipes", "cotton buds", "shaver", "gillette", "shaving cream", "barbershop", "haircut", "rebond", "hair color", "foot spa"
    };

    public static final String[] CAT_HEALTH = {
        "medicine", "pharmacy", "mercury drug", "watsons", "gamot", "doctor", "hospital", "checkup", "clinic", "vitamins", "suplemento", "dentist", "medical", "hmo", "biogesic", "neozep", "solmux", "bioflu", "alaxan", "immunpro", "enervon", "centrum", "paracetamol", "ibuprofen", "amoxicillin", "antibiotic", "ascorbic acid", "myra e", "maintenance medicine", "bp monitor", "swab test", "bunot", "linis ngipin", "braces", "adjust brace", "x-ray", "lab test", "laboratorio"
    };

    public static final String[] CAT_EDUCATION = {
        "tuition", "school", "books", "supplies", "ballpen", "notebook", "uniform", "enrollment", "exam", "baon", "graduation", "clearance", "projects", "field trip", "school fee", "miscellaneous", "dorm fee", "board exam", "review center", "lapis", "papel", "aklat", "tuition fee", "tf", "misc fee", "downpayment school", "matriculation", "pencil", "eraser", "ruler", "crayon", "marker", "highlighter", "sharpener", "pencil case", "bagpack", "school bag", "id lanyard", "pe uniform", "black shoes", "white shoes", "socks school", "contribution", "ambagan", "project fee", "thesis", "hardbound", "js prom", "retreat", "intrams"
    };

    public static final String[] CAT_WORK = {
        "office", "printing", "stationery", "work equipment", "desk", "photocopy", "id photo", "bond paper", "folder", "envelope", "ink", "toner", "stapler", "tape", "glue", "scissors", "xerox", "staple wire", "puncher", "clearbook", "whiteboard", "ergonomic chair", "office desk", "laptop stand", "webcam", "extension cord", "coworking", "scrub suit", "safety shoes"
    };

    public static final String[] CAT_INCOME = {
        "salary", "sweldo", "sahod", "bonus", "commission", "sideline", "freelance", "raket", "benta", "profit", "kita", "income", "payroll", "13th month", "incentive", "overtime", "ot pay", "hazard pay", "holiday pay", "kinsenas", "katapusan", "backpay", "suweldo", "sweldo day", "payday", "basic pay", "separation pay", "monetization", "tips", "barya", "tubo sa benta", "upwork pay", "fiverr pay", "va pay", "virtual assistant income", "crypto earnings", "p2p sell", "allowance", "baon", "remittance", "padala", "gift", "pasko", "birthday gift", "inheritance", "pamasko", "aguinaldo", "gcash received", "cashback", "refund", "rebate", "shopee pay later refund", "balato", "payout", "western union", "cebuana", "palawan express", "palawan padala", "moneygram", "cash gift", "ampaw", "angpao", "dividend", "interest", "investment return", "stock profit", "crypto profit", "trading", "staking", "yield", "mp2", "pagibig mp2", "stocks", "reit", "seabank interest", "maya interest", "gotyme interest", "cimb interest"
    };

    public static final String[] CAT_OTHERS = {
        "donation", "church", "tithes", "abuloy", "charity", "gift", "regalo", "surprise", "pet", "dog food", "cat food", "vet", "kuting", "aso", "travel", "hotel", "flight", "airplane", "booking", "bakasyon", "staycation", "abuloy", "entulon", "simbahan", "offering", "ambag", "kapilya", "katoliko", "iglesia", "inc", "ccf", "victory", "tithing", "love offering", "mass card", "gofundme", "red cross", "ninong", "ninang", "wedding gift", "souvenir", "pasalubong", "monito monita", "exchange gift", "birthday present", "pusa", "whiskas", "pedigree", "dogfood", "catfood", "aozi", "royal canin", "anti rabies", "vaccine dog", "pet clinic", "vet checkup", "pet express", "aquarium", "isda pet", "gupit aso", "leash", "collar", "cage", "kulungan", "passport", "visa", "cebu pacific", "pal", "airasia", "travel tax", "terminal fee", "luggage", "cebupac", "maleta", "agoda", "booking.com", "klook", "tour", "entrance fee", "wedding", "burial", "funeral", "wake"
    };

    static {
        putAll(CAT_FOOD, "Food");
        putAll(CAT_BILLS, "Bills");
        putAll(CAT_TRANSPORT, "Transportation");
        putAll(CAT_GADGETS, "Shopping");
        putAll(CAT_APPLIANCES, "Shopping");
        putAll(CAT_SHOPPING, "Shopping");
        putAll(CAT_PERSONAL, "Personal Care");
        putAll(CAT_HEALTH, "Health");
        putAll(CAT_EDUCATION, "Education");
        putAll(CAT_WORK, "Work");
        putAll(CAT_INCOME, "Income");
        putAll(CAT_OTHERS, "Others");
    }

    private static void putAll(String[] keys, String category) {
        for (String key : keys) MAPPING.put(key.toLowerCase(), category);
    }

    public static String getCategory(String item, String fallback) {
        if (item == null || item.isEmpty()) return fallback;
        String normalized = item.toLowerCase().trim();
        
        for (Map.Entry<String, String> entry : MAPPING.entrySet()) {
            String keyword = entry.getKey();
            if (java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(keyword) + "\\b", 
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(normalized).find()) {
                return entry.getValue();
            }
        }
        return (fallback != null && !fallback.isEmpty() && !fallback.equalsIgnoreCase("unknown")) ? fallback : "Others";
    }
}
