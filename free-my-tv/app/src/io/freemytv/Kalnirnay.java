package io.freemytv;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Important Marathi calendar (Kalnirnay / Panchang) events, vrats and festivals.
 */
final class Kalnirnay {
    private Kalnirnay() {}

    private static final Map<String, String> EVENTS = new HashMap<>();

    static {
        // October 2026
        EVENTS.put("2026-10-25", "🌕 कोजागरी पौर्णिमा");
        EVENTS.put("2026-10-29", "संकष्टी चतुर्थी (चंद्रोदय ८:५१)");

        // November 2026 (Diwali & Kartik)
        EVENTS.put("2026-11-05", "रमा एकादशी व वसू बारस");
        EVENTS.put("2026-11-06", "🪔 धनत्रयोदशी (यमदीपदान)");
        EVENTS.put("2026-11-07", "रूप चतुर्दशी");
        EVENTS.put("2026-11-08", "🪔 नरक चतुर्दशी व लक्ष्मीपूजन");
        EVENTS.put("2026-11-09", "दर्श अमावास्या");
        EVENTS.put("2026-11-10", "बलिप्रतिपदा - दिवाळी पाडवा");
        EVENTS.put("2026-11-11", "भाऊबीज / यमद्वितीया");
        EVENTS.put("2026-11-13", "विनायक चतुर्थी");
        EVENTS.put("2026-11-20", "कार्तिकी प्रबोधिनी एकादशी");
        EVENTS.put("2026-11-21", "तुळशी विवाह आरंभ");
        EVENTS.put("2026-11-23", "वैकुंठ चतुर्दशी");
        EVENTS.put("2026-11-24", "त्रिपुरारी पौर्णिमा / देवदिवाळी");

        // December 2026 (Margashirsha)
        EVENTS.put("2026-12-04", "उत्पन्ना एकादशी");
        EVENTS.put("2026-12-09", "मार्गशीर्ष महिना आरंभ");
        EVENTS.put("2026-12-10", "१ला मार्गशीर्ष गुरुवार (महालक्ष्मी व्रत)");
        EVENTS.put("2026-12-13", "विनायक चतुर्थी");
        EVENTS.put("2026-12-15", "चंपा षष्ठी (खंडोबा उत्सव)");
        EVENTS.put("2026-12-17", "२रा मार्गशीर्ष गुरुवार (महालक्ष्मी व्रत)");
        EVENTS.put("2026-12-20", "मोक्षदा एकादशी व गीता जयंती");
        EVENTS.put("2026-12-23", "दत्तात्रेय जयंती");
        EVENTS.put("2026-12-24", "३रा मार्गशीर्ष गुरुवार (महालक्ष्मी व्रत)");
        EVENTS.put("2026-12-26", "संकष्टी चतुर्थी");
        EVENTS.put("2026-12-31", "४था मार्गशीर्ष गुरुवार (महालक्ष्मी व्रत)");
    }

    private static String key(Calendar c) {
        return String.format("%04d-%02d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
    }

    /** Returns today's festival, or tomorrow's if today has none, or null. */
    static String getEvent() {
        Calendar cal = Calendar.getInstance();
        String todayKey = key(cal);
        if (EVENTS.containsKey(todayKey)) {
            return EVENTS.get(todayKey);
        }

        // Check tomorrow
        cal.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrowKey = key(cal);
        if (EVENTS.containsKey(tomorrowKey)) {
            return "उद्या: " + EVENTS.get(tomorrowKey);
        }

        // Check upcoming in the next 3 days
        for (int i = 2; i <= 3; i++) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            String nextKey = key(cal);
            if (EVENTS.containsKey(nextKey)) {
                return EVENTS.get(nextKey);
            }
        }
        return null;
    }
}
