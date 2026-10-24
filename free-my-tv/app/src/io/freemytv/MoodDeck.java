package io.freemytv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextPaint;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * "Don't have time to browse?" - A deck of mood cards for nights when you can't decide.
 * Swipe left to skip a mood. Pick one and its posters spin like a slot-machine reel, land on a title,
 * count down from three and open it in its app.
 */
final class MoodDeck {

    static final class TitleItem {
        final String title;
        final String key;
        final String mood;
        final String appName;
        final String pkg;
        final String link;
        Bitmap poster;
        Bitmap backdrop;
        Bitmap monoBackdrop;

        TitleItem(String title, String key, String mood, String appName, String pkg, String link) {
            this.title = title;
            this.key = key;
            this.mood = mood;
            this.appName = appName;
            this.pkg = pkg;
            this.link = link;
        }
    }

    static final class Mood {
        final String name;
        final int color;
        final List<TitleItem> items = new ArrayList<>();

        Mood(String name, int color) {
            this.name = name;
            this.color = color;
        }
    }

    // States
    static final int STATE_DECK = 0;
    static final int STATE_SPINNING = 1;
    static final int STATE_COUNTDOWN = 2;

    private final Context context;
    private final List<Mood> moods = new ArrayList<>();
    private final Map<String, Bitmap> posterCache = new HashMap<>();
    private final Map<String, Bitmap> backdropCache = new HashMap<>();
    private final Map<String, Bitmap> monoBackdropCache = new HashMap<>();
    private final Random random = new Random();

    private int state = STATE_DECK;
    private int currentMoodIndex = 0;

    // Deck swipe animation
    private float swipeOffset = 0f; // 0 = rest, -1 = swiped out to left
    private boolean isSwiping = false;
    private long swipeStartTime = 0;
    private static final long SWIPE_DURATION_MS = 280;

    // Slot machine reel animation
    private List<TitleItem> reelItems = new ArrayList<>();
    private int reelTargetIndex = 0;
    private float reelScrollPos = 0f; // in units of card index
    private float reelStartScroll = 0f;
    private float reelTargetScroll = 0f;
    private long reelStartTime = 0;
    private static final long REEL_SPIN_DURATION_MS = 2200;

    // Countdown animation
    private long countdownStartTime = 0;
    private static final long COUNTDOWN_DURATION_MS = 3200; // 3 seconds countdown
    private TitleItem winningItem = null;

    // Background crossfade
    private Bitmap currentBg = null;
    private Bitmap previousBg = null;
    private float bgCrossfade = 1f; // 0 to 1
    private long bgFadeStartTime = 0;
    private static final long BG_FADE_DURATION_MS = 350;

    // Paints
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect srcRect = new Rect();
    private final RectF dstRect = new RectF();
    private final RectF cardRect = new RectF();
    private final RectF btnRect = new RectF();

    MoodDeck(Context context) {
        this.context = context.getApplicationContext();
        strokePaint.setStyle(Paint.Style.STROKE);
        initCatalogue();
    }

    private void initCatalogue() {
        PackageManager pm = context.getPackageManager();
        boolean hasNetmirror = HomeApps.launchIntent(pm, "app.netmirror.newtv") != null;
        boolean hasNetflix = HomeApps.launchIntent(pm, "com.netflix.ninja") != null || hasNetmirror;
        boolean hasPrime = HomeApps.launchIntent(pm, "com.amazon.amazonvideo.livingroom") != null
                || HomeApps.launchIntent(pm, "com.amazon.avod") != null;
        boolean hasHotstar = HomeApps.launchIntent(pm, "in.startv.hotstar") != null;
        boolean hasZee5 = HomeApps.launchIntent(pm, "com.graymatrix.did") != null;
        boolean hasSonyLiv = HomeApps.launchIntent(pm, "com.sonyliv") != null;

        String netflixPkg = hasNetmirror ? "app.netmirror.newtv" : "com.netflix.ninja";
        String primePkg = "com.amazon.amazonvideo.livingroom";
        String hotstarPkg = "in.startv.hotstar";
        String zeePkg = "com.graymatrix.did";
        String sonylivPkg = "com.sonyliv";

        Mood comfort = new Mood("Comfort sitcom", 0xFFF2B84B);
        Mood capes = new Mood("Capes and chaos", 0xFFE5484D);
        Mood gotham = new Mood("Gotham after dark", 0xFF3E63DD);
        Mood thrillers = new Mood("Thrillers", 0xFFFF6B35);
        Mood mind = new Mood("Mind games", 0xFF8E4EC6);
        Mood anything = new Mood("Anything at all", 0xFFB8C0CC);

        List<TitleItem> all = new ArrayList<>();

        // 1. Comfort sitcom
        if (hasPrime) {
            comfort.items.add(new TitleItem("The Big Bang Theory", "The Big Bang Theory", "Comfort sitcom", "Prime Video", primePkg,
                    "https://app.primevideo.com/detail?gti=amzn1.dv.gti.8ea9f8c6-2917-7443-41bb-98a445d4c8fb"));
        }
        if (hasNetflix) {
            comfort.items.add(new TitleItem("The Office", "The Office", "Comfort sitcom", "Netflix", netflixPkg,
                    "https://www.netflix.com/title/70136120"));
            comfort.items.add(new TitleItem("Friends", "Friends", "Comfort sitcom", "Netflix", netflixPkg,
                    "https://www.netflix.com/title/70153404"));
            comfort.items.add(new TitleItem("Brooklyn Nine-Nine", "Brooklyn Nine-Nine", "Comfort sitcom", "Netflix", netflixPkg,
                    "https://www.netflix.com/title/70281562"));
        }
        if (hasHotstar) {
            comfort.items.add(new TitleItem("How I Met Your Mother", "How I Met Your Mother", "Comfort sitcom", "JioHotstar", hotstarPkg,
                    "https://www.hotstar.com/in/shows/how-i-met-your-mother/8381"));
            comfort.items.add(new TitleItem("Modern Family", "Modern Family", "Comfort sitcom", "JioHotstar", hotstarPkg,
                    "https://www.hotstar.com/in/shows/modern-family/8298"));
        }

        // 2. Capes and chaos
        if (hasNetflix) {
            capes.items.add(new TitleItem("Spider-Man: No Way Home", "Spider-Man_ No Way Home", "Capes and chaos", "Netflix", netflixPkg,
                    "https://www.netflix.com/title/81446739"));
        }
        if (hasHotstar) {
            capes.items.add(new TitleItem("Avengers: Endgame", "Avengers_ Endgame", "Capes and chaos", "JioHotstar", hotstarPkg,
                    "https://www.hotstar.com/in/movies/avengers-endgame/1260009632"));
            capes.items.add(new TitleItem("Avengers: Infinity War", "Avengers_ Infinity War", "Capes and chaos", "JioHotstar", hotstarPkg,
                    "https://www.hotstar.com/in/movies/avengers-infinity-war/1660010670"));
            capes.items.add(new TitleItem("Deadpool", "Deadpool", "Capes and chaos", "JioHotstar", hotstarPkg,
                    "https://www.hotstar.com/in/movies/deadpool/1770003257"));
        }
        if (hasPrime) {
            capes.items.add(new TitleItem("The Boys", "The Boys", "Capes and chaos", "Prime Video", primePkg,
                    "https://app.primevideo.com/detail?gti=amzn1.dv.gti.20b3967d-9351-c063-42e7-0347209cb690"));
            capes.items.add(new TitleItem("Zack Snyder's Justice League", "Zack Snyder_s Justice League", "Capes and chaos", "Prime Video", primePkg,
                    "https://app.primevideo.com/detail?gti=amzn1.dv.gti.25a298a0-2f9a-4c28-98bb-849bf300cb10"));
        }

        // 3. Gotham after dark
        if (hasPrime) {
            gotham.items.add(new TitleItem("The Dark Knight", "The Dark Knight", "Gotham after dark", "Prime Video", primePkg,
                    "https://app.primevideo.com/detail?gti=amzn1.dv.gti.62a9f8cc-8db9-b7b8-8c01-7fa7b9f8f41e"));
            gotham.items.add(new TitleItem("The Batman", "The Batman", "Gotham after dark", "Prime Video", primePkg,
                    "https://app.primevideo.com/detail?gti=amzn1.dv.gti.4eb3b4ef-a6bc-452f-9dfb-ea5c635694a9"));
            gotham.items.add(new TitleItem("Batman Begins", "Batman Begins", "Gotham after dark", "Prime Video", primePkg,
                    "https://app.primevideo.com/detail?gti=amzn1.dv.gti.36a9f8cc-8b4d-ec53-8f6a-6cb0b82f8a84"));
            gotham.items.add(new TitleItem("The Dark Knight Rises", "The Dark Knight Rises", "Gotham after dark", "Prime Video", primePkg,
                    "https://app.primevideo.com/detail?gti=amzn1.dv.gti.42a9f8cc-9426-3d77-d648-5250ff5511dc"));
        }
        if (hasNetflix) {
            gotham.items.add(new TitleItem("Joker", "Joker", "Gotham after dark", "Netflix", netflixPkg,
                    "https://www.netflix.com/title/81092222"));
        }

        // 4. Thrillers
        if (hasHotstar) {
            thrillers.items.add(new TitleItem("Drishyam", "Drishyam", "Thrillers", "JioHotstar", hotstarPkg,
                    "https://www.hotstar.com/in/movies/drishyam/1000074218"));
            thrillers.items.add(new TitleItem("Ratsasan", "Ratsasan", "Thrillers", "JioHotstar", hotstarPkg,
                    "https://www.hotstar.com/in/movies/ratsasan/1000223788"));
        }
        if (hasPrime) {
            thrillers.items.add(new TitleItem("Drishyam 2", "Drishyam 2", "Thrillers", "Prime Video", primePkg,
                    "https://app.primevideo.com/detail?gti=amzn1.dv.gti.20b8fbc0-98d9-4753-9114-1c69ba545802"));
        }
        if (hasNetflix) {
            thrillers.items.add(new TitleItem("Andhadhun", "Andhadhun", "Thrillers", "Netflix", netflixPkg,
                    "https://www.netflix.com/title/81039381"));
            thrillers.items.add(new TitleItem("Kantara", "Kantara", "Thrillers", "Netflix", netflixPkg,
                    "https://www.netflix.com/title/81656709"));
        }
        if (hasSonyLiv) {
            thrillers.items.add(new TitleItem("Scam 1992", "Scam 1992_ The Harshad Mehta Story", "Thrillers", "Sony LIV", sonylivPkg,
                    "https://www.sonyliv.com/shows/scam-1992-the-harshad-mehta-story-1700000292"));
        }
        if (hasZee5) {
            thrillers.items.add(new TitleItem("Sunflower", "Sunflower", "Thrillers", "ZEE5", zeePkg,
                    "https://www.zee5.com/web-series/details/sunflower/0-6-3271"));
        }

        // 5. Mind games
        if (hasNetflix) {
            mind.items.add(new TitleItem("Inception", "Inception", "Mind games", "Netflix", netflixPkg,
                    "https://www.netflix.com/title/70131314"));
            mind.items.add(new TitleItem("Shutter Island", "Shutter Island", "Mind games", "Netflix", netflixPkg,
                    "https://www.netflix.com/title/70131946"));
            mind.items.add(new TitleItem("Interstellar", "Interstellar", "Mind games", "Netflix", netflixPkg,
                    "https://www.netflix.com/title/70305903"));
        }
        if (hasPrime) {
            mind.items.add(new TitleItem("The Prestige", "The Prestige", "Mind games", "Prime Video", primePkg,
                    "https://app.primevideo.com/detail?gti=amzn1.dv.gti.8ba9f8ce-ec52-c2cb-4652-32a222ee4ab0"));
        }
        if (hasHotstar) {
            mind.items.add(new TitleItem("Fight Club", "Fight Club", "Mind games", "JioHotstar", hotstarPkg,
                    "https://www.hotstar.com/in/movies/fight-club/1770000938"));
        }

        // 6. Anything at all (holds every title automatically)
        all.addAll(comfort.items);
        all.addAll(capes.items);
        all.addAll(gotham.items);
        all.addAll(thrillers.items);
        all.addAll(mind.items);
        anything.items.addAll(all);

        moods.clear();
        if (!comfort.items.isEmpty()) moods.add(comfort);
        if (!capes.items.isEmpty()) moods.add(capes);
        if (!gotham.items.isEmpty()) moods.add(gotham);
        if (!thrillers.items.isEmpty()) moods.add(thrillers);
        if (!mind.items.isEmpty()) moods.add(mind);
        if (!anything.items.isEmpty()) moods.add(anything);

        updateCurrentMoodScene();
    }

    List<TitleItem> allTitles() {
        if (moods.isEmpty()) return Collections.emptyList();
        return moods.get(moods.size() - 1).items; // Anything at all contains all
    }

    int getState() {
        return state;
    }

    void resetToDeck() {
        state = STATE_DECK;
        isSwiping = false;
        swipeOffset = 0f;
        winningItem = null;
        updateCurrentMoodScene();
    }

    void swipeLeft() {
        if (moods.isEmpty() || isSwiping) return;
        isSwiping = true;
        swipeStartTime = SystemClock.uptimeMillis();
    }

    void activateCurrentMood() {
        if (moods.isEmpty()) return;
        Mood m = moods.get(currentMoodIndex);
        if (m.items.isEmpty()) return;

        // Build reel: repeat items so it spins nicely
        reelItems.clear();
        List<TitleItem> pool = new ArrayList<>(m.items);
        Collections.shuffle(pool, random);

        // Pick the winning item
        winningItem = pool.get(random.nextInt(pool.size()));

        // Create a long reel track of ~25 items
        for (int i = 0; i < 5; i++) {
            List<TitleItem> copy = new ArrayList<>(m.items);
            Collections.shuffle(copy, random);
            reelItems.addAll(copy);
        }
        // Place winning item at target index (around index 18)
        reelTargetIndex = Math.min(18, reelItems.size() - 2);
        reelItems.set(reelTargetIndex, winningItem);

        reelStartScroll = 0f;
        reelTargetScroll = reelTargetIndex;
        reelScrollPos = 0f;
        reelStartTime = SystemClock.uptimeMillis();
        state = STATE_SPINNING;
    }

    void spinAgain() {
        activateCurrentMood();
    }

    void openWinningApp() {
        if (winningItem == null) return;
        launchItem(winningItem);
        resetToDeck();
    }

    private void launchItem(TitleItem item) {
        if (item.pkg == null) return;
        PackageManager pm = context.getPackageManager();
        Intent intent = null;

        if (item.link != null && !item.link.isEmpty()) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.link));
            intent.setPackage(item.pkg);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (pm.queryIntentActivities(intent, 0).isEmpty()) {
                intent = null;
            }
        }
        if (intent == null) {
            intent = HomeApps.launchIntent(pm, item.pkg);
        }
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
            } catch (Exception ignored) {}
        }
    }

    private void updateCurrentMoodScene() {
        if (moods.isEmpty()) return;
        Mood m = moods.get(currentMoodIndex);
        if (m.items.isEmpty()) return;
        TitleItem representative = m.items.get(0);
        setBackdropScene(getMonoBackdrop(representative));
    }

    private void setBackdropScene(Bitmap b) {
        if (b == null || b == currentBg) return;
        previousBg = currentBg;
        currentBg = b;
        bgCrossfade = 0f;
        bgFadeStartTime = SystemClock.uptimeMillis();
    }

    boolean isAnimating() {
        return bgCrossfade < 1f || isSwiping || state == STATE_SPINNING || state == STATE_COUNTDOWN;
    }

    boolean updateAnimations() {
        long now = SystemClock.uptimeMillis();
        boolean needsInvalidate = false;

        // Background crossfade
        if (bgCrossfade < 1f) {
            float t = (now - bgFadeStartTime) / (float) BG_FADE_DURATION_MS;
            if (t >= 1f) {
                bgCrossfade = 1f;
                previousBg = null;
            } else {
                bgCrossfade = t;
            }
            needsInvalidate = true;
        }

        // Swipe animation
        if (isSwiping) {
            float t = (now - swipeStartTime) / (float) SWIPE_DURATION_MS;
            if (t >= 1f) {
                isSwiping = false;
                swipeOffset = 0f;
                currentMoodIndex = (currentMoodIndex + 1) % moods.size();
                updateCurrentMoodScene();
            } else {
                swipeOffset = t; // 0 to 1
            }
            needsInvalidate = true;
        }

        // Reel spinning animation
        if (state == STATE_SPINNING) {
            float t = (now - reelStartTime) / (float) REEL_SPIN_DURATION_MS;
            if (t >= 1f) {
                reelScrollPos = reelTargetScroll;
                state = STATE_COUNTDOWN;
                countdownStartTime = now;
                if (winningItem != null) {
                    setBackdropScene(getMonoBackdrop(winningItem));
                }
            } else {
                // Cubic deceleration curve (fast start, smooth snap stop)
                float eased = 1f - (float) Math.pow(1f - t, 3);
                reelScrollPos = reelStartScroll + (reelTargetScroll - reelStartScroll) * eased;
            }
            needsInvalidate = true;
        }

        // Countdown animation
        if (state == STATE_COUNTDOWN) {
            float t = (now - countdownStartTime) / (float) COUNTDOWN_DURATION_MS;
            if (t >= 1f) {
                openWinningApp();
            }
            needsInvalidate = true;
        }

        return needsInvalidate;
    }

    void drawBackground(Canvas c, int w, int h) {
        updateAnimations();
        drawBackgroundInternal(c, w, h);
    }

    void drawForeground(Canvas c, int w, int h, float k) {
        if (moods.isEmpty()) return;

        // Draw Top Section Header: "Don't have time to browse?"
        String header = "Don't have time to browse?";
        TextPaint headerPaint = Ui.displayMedium != null ? new TextPaint(Ui.text(Ui.displayMedium, 36 * k, 0xFFFFFFFF))
                : new TextPaint(Ui.text(Ui.semibold, 36 * k, 0xFFFFFFFF));
        headerPaint.setTextAlign(Paint.Align.CENTER);
        headerPaint.setShadowLayer(16 * k, 0, 3 * k, 0xDD000000);
        c.drawText(header, w / 2f, 130 * k, headerPaint);

        Mood mood = moods.get(currentMoodIndex);

        if (state == STATE_DECK) {
            drawDeck(c, w, h, k, mood);
        } else {
            drawReel(c, w, h, k, mood);
        }
    }

    private void drawBackgroundInternal(Canvas c, int w, int h) {
        if (currentBg != null) {
            bitmapPaint.setAlpha(255);
            srcRect.set(0, 0, currentBg.getWidth(), currentBg.getHeight());
            dstRect.set(0, 0, w, h);

            if (previousBg != null && bgCrossfade < 1f) {
                bitmapPaint.setAlpha(Math.round(255 * (1f - bgCrossfade)));
                srcRect.set(0, 0, previousBg.getWidth(), previousBg.getHeight());
                c.drawBitmap(previousBg, srcRect, dstRect, bitmapPaint);
                bitmapPaint.setAlpha(Math.round(255 * bgCrossfade));
                srcRect.set(0, 0, currentBg.getWidth(), currentBg.getHeight());
            }

            c.drawBitmap(currentBg, srcRect, dstRect, bitmapPaint);
        } else {
            fillPaint.setColor(0xFF090D14);
            c.drawRect(0, 0, w, h, fillPaint);
        }

        // Vignette & Dark Overlay: heavy bottom and edge scrim
        LinearGradient scrim = new LinearGradient(0, 0, 0, h,
                new int[] {0x99000000, 0x44000000, 0xDD090D14, 0xFF090D14},
                new float[] {0f, 0.45f, 0.85f, 1f}, Shader.TileMode.CLAMP);
        fillPaint.setShader(scrim);
        c.drawRect(0, 0, w, h, fillPaint);
        fillPaint.setShader(null);
    }

    private void drawDeck(Canvas c, int w, int h, float k, Mood mood) {
        float cx = w / 2f;
        float cy = 490 * k;

        // Draw soft mood glow behind the deck
        RadialGradient glow = new RadialGradient(cx, cy, 340 * k,
                new int[] {(mood.color & 0x00FFFFFF) | 0x48000000, 0x00000000},
                null, Shader.TileMode.CLAMP);
        glowPaint.setShader(glow);
        c.drawCircle(cx, cy, 340 * k, glowPaint);
        glowPaint.setShader(null);

        // 3rd card peeking behind
        int idx3 = (currentMoodIndex + 2) % moods.size();
        drawDeckCard(c, cx, cy - 42 * k, 0.88f, 0f, 0.40f, moods.get(idx3), k);

        // 2nd card peeking behind
        int idx2 = (currentMoodIndex + 1) % moods.size();
        drawDeckCard(c, cx, cy - 22 * k, 0.94f, 0f, 0.65f, moods.get(idx2), k);

        // Top card (with swipe animation if active)
        if (isSwiping) {
            float flyX = -cx * 1.5f * swipeOffset;
            float rot = -14f * swipeOffset;
            float alpha = 1f - swipeOffset;
            drawDeckCard(c, cx + flyX, cy, 1f, rot, alpha, mood, k);
        } else {
            drawDeckCard(c, cx, cy, 1f, 0f, 1f, mood, k);
        }
    }

    private void drawDeckCard(Canvas c, float cx, float cy, float scale, float rotation, float alpha, Mood mood, float k) {
        c.save();
        c.translate(cx, cy);
        if (rotation != 0) c.rotate(rotation);
        if (scale != 1f) c.scale(scale, scale);

        float cw = 440 * k;
        float ch = 540 * k;
        cardRect.set(-cw / 2f, -ch / 2f, cw / 2f, ch / 2f);

        // Card background: mood gradient fading to near black
        int cTop = blend(0xFF141722, mood.color, 0.22f);
        int cBot = 0xFF0D0F14;
        LinearGradient cardGrad = new LinearGradient(0, cardRect.top, 0, cardRect.bottom,
                new int[] {cTop, cBot}, null, Shader.TileMode.CLAMP);
        fillPaint.setShader(cardGrad);
        fillPaint.setAlpha(Math.round(255 * alpha));
        c.drawRoundRect(cardRect, 24 * k, 24 * k, fillPaint);
        fillPaint.setShader(null);

        // Card subtle rim
        strokePaint.setColor(blend(0x22FFFFFF, mood.color, 0.45f));
        strokePaint.setStrokeWidth(1.8f * k);
        strokePaint.setAlpha(Math.round(180 * alpha));
        c.drawRoundRect(cardRect, 24 * k, 24 * k, strokePaint);

        // First 3 posters fanned out
        List<TitleItem> items = mood.items;
        float pw = 150 * k, ph = 225 * k;

        // Left poster (tilted left)
        if (items.size() > 1) {
            drawFannedPoster(c, -85 * k, -60 * k, pw * 0.90f, ph * 0.90f, -12f, alpha * 0.85f, items.get(1), k);
        }
        // Right poster (tilted right)
        if (items.size() > 2) {
            drawFannedPoster(c, 85 * k, -60 * k, pw * 0.90f, ph * 0.90f, 12f, alpha * 0.85f, items.get(2), k);
        }
        // Center poster (straight, on top)
        if (!items.isEmpty()) {
            drawFannedPoster(c, 0, -55 * k, pw * 1.05f, ph * 1.05f, 0f, alpha, items.get(0), k);
        }

        // Mood Name
        TextPaint titlePaint = Ui.displaySemibold != null ? new TextPaint(Ui.text(Ui.displaySemibold, 34 * k, 0xFFFFFFFF))
                : new TextPaint(Ui.text(Ui.semibold, 34 * k, 0xFFFFFFFF));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setAlpha(Math.round(255 * alpha));
        c.drawText(mood.name, 0, 160 * k, titlePaint);

        // Title count: "N titles"
        TextPaint countPaint = new TextPaint(Ui.text(Ui.medium, 22 * k, 0xB8FFFFFF));
        countPaint.setTextAlign(Paint.Align.CENTER);
        countPaint.setAlpha(Math.round(200 * alpha));
        c.drawText(items.size() + " titles", 0, 202 * k, countPaint);

        c.restore();
    }

    private void drawFannedPoster(Canvas c, float px, float py, float pw, float ph, float rot, float alpha, TitleItem item, float k) {
        c.save();
        c.translate(px, py);
        if (rot != 0) c.rotate(rot);

        dstRect.set(-pw / 2f, -ph / 2f, pw / 2f, ph / 2f);

        // Poster shadow
        fillPaint.setColor(0x88000000);
        c.drawRoundRect(new RectF(dstRect.left + 2 * k, dstRect.top + 4 * k, dstRect.right + 2 * k, dstRect.bottom + 6 * k),
                14 * k, 14 * k, fillPaint);

        Bitmap b = getPoster(item);
        if (b != null) {
            bitmapPaint.setAlpha(Math.round(255 * alpha));
            srcRect.set(0, 0, b.getWidth(), b.getHeight());
            c.drawBitmap(b, srcRect, dstRect, bitmapPaint);
        } else {
            fillPaint.setColor(0xFF222834);
            fillPaint.setAlpha(Math.round(255 * alpha));
            c.drawRoundRect(dstRect, 14 * k, 14 * k, fillPaint);
        }

        // Poster outline
        strokePaint.setColor(0x33FFFFFF);
        strokePaint.setStrokeWidth(1.2f * k);
        strokePaint.setAlpha(Math.round(150 * alpha));
        c.drawRoundRect(dstRect, 14 * k, 14 * k, strokePaint);

        c.restore();
    }

    private void drawReel(Canvas c, int w, int h, float k, Mood mood) {
        float cx = w / 2f;
        float cy = 440 * k;
        float cardW = 180 * k;
        float cardH = 270 * k;
        float cardGap = 24 * k;
        float cardPitch = cardW + cardGap;

        if (reelItems.isEmpty()) return;

        // Draw horizontal slot machine reel
        for (int i = 0; i < reelItems.size(); i++) {
            float distFromCenter = (i - reelScrollPos);
            float cardX = cx + distFromCenter * cardPitch;

            // Only draw cards within visible screen bounds
            if (cardX < -cardW || cardX > w + cardW) continue;

            float absDist = Math.abs(distFromCenter);
            float scale = Math.max(0.72f, 1.06f - absDist * 0.09f);
            float alpha = Math.max(0.12f, 1f - absDist * 0.28f);

            c.save();
            c.translate(cardX, cy);
            c.scale(scale, scale);

            dstRect.set(-cardW / 2f, -cardH / 2f, cardW / 2f, cardH / 2f);

            // Center card landing illuminated border & glow
            boolean isCenterLanded = (state == STATE_COUNTDOWN && Math.abs(distFromCenter) < 0.35f);
            if (isCenterLanded) {
                // Radiant glow in mood color
                RadialGradient centerGlow = new RadialGradient(0, 0, cardW * 0.95f,
                        new int[] {(mood.color & 0x00FFFFFF) | 0x55000000, 0x00000000}, null, Shader.TileMode.CLAMP);
                glowPaint.setShader(centerGlow);
                c.drawCircle(0, 0, cardW * 0.95f, glowPaint);
                glowPaint.setShader(null);
            }

            // Drop shadow
            fillPaint.setColor(0x99000000);
            c.drawRoundRect(new RectF(dstRect.left, dstRect.top + 6 * k, dstRect.right, dstRect.bottom + 8 * k),
                    16 * k, 16 * k, fillPaint);

            TitleItem it = reelItems.get(i);
            Bitmap b = getPoster(it);
            if (b != null) {
                bitmapPaint.setAlpha(Math.round(255 * alpha));
                srcRect.set(0, 0, b.getWidth(), b.getHeight());
                c.drawBitmap(b, srcRect, dstRect, bitmapPaint);
            } else {
                fillPaint.setColor(0xFF222834);
                fillPaint.setAlpha(Math.round(255 * alpha));
                c.drawRoundRect(dstRect, 16 * k, 16 * k, fillPaint);
            }

            // Border
            if (isCenterLanded) {
                strokePaint.setColor(mood.color);
                strokePaint.setStrokeWidth(3.5f * k);
                strokePaint.setAlpha(255);
            } else {
                strokePaint.setColor(0x33FFFFFF);
                strokePaint.setStrokeWidth(1.2f * k);
                strokePaint.setAlpha(Math.round(150 * alpha));
            }
            c.drawRoundRect(dstRect, 16 * k, 16 * k, strokePaint);

            c.restore();
        }

        // If landed in COUNTDOWN state: display title, countdown progress, actions, and "More apps"
        if (state == STATE_COUNTDOWN && winningItem != null) {
            long elapsed = SystemClock.uptimeMillis() - countdownStartTime;
            float progress = Math.min(1f, elapsed / (float) COUNTDOWN_DURATION_MS);
            int secondsLeft = Math.max(1, (int) Math.ceil((COUNTDOWN_DURATION_MS - elapsed) / 1000f));

            // Title
            TextPaint tPaint = Ui.displaySemibold != null ? new TextPaint(Ui.text(Ui.displaySemibold, 36 * k, 0xFFFFFFFF))
                    : new TextPaint(Ui.text(Ui.semibold, 36 * k, 0xFFFFFFFF));
            tPaint.setTextAlign(Paint.Align.CENTER);
            tPaint.setShadowLayer(14 * k, 0, 3 * k, 0xEE000000);
            c.drawText(winningItem.title, cx, cy + 185 * k, tPaint);

            // Subtitle: "Opening in 3... OK to play now · > spin again"
            TextPaint subPaint = new TextPaint(Ui.text(Ui.medium, 22 * k, 0xD9FFFFFF));
            subPaint.setTextAlign(Paint.Align.CENTER);
            subPaint.setShadowLayer(10 * k, 0, 2 * k, 0xCC000000);
            String sub = "Opening in " + secondsLeft + "...  OK to play now  ·  › spin again";
            c.drawText(sub, cx, cy + 225 * k, subPaint);

            // Progress bar
            float barW = 280 * k;
            float barH = 5 * k;
            float barY = cy + 252 * k;
            btnRect.set(cx - barW / 2f, barY, cx + barW / 2f, barY + barH);

            // Track
            fillPaint.setColor(0x33FFFFFF);
            c.drawRoundRect(btnRect, barH / 2f, barH / 2f, fillPaint);

            // Fill
            float fillW = barW * progress;
            RectF fillRect = new RectF(btnRect.left, barY, btnRect.left + fillW, barY + barH);
            fillPaint.setColor(mood.color);
            c.drawRoundRect(fillRect, barH / 2f, barH / 2f, fillPaint);

            // "More apps" pill button at bottom
            float btnW = 190 * k;
            float btnH = 52 * k;
            float btnY = cy + 300 * k;
            btnRect.set(cx - btnW / 2f, btnY, cx + btnW / 2f, btnY + btnH);

            fillPaint.setColor(0x33FFFFFF);
            c.drawRoundRect(btnRect, btnH / 2f, btnH / 2f, fillPaint);
            strokePaint.setColor(0x44FFFFFF);
            strokePaint.setStrokeWidth(1.2f * k);
            c.drawRoundRect(btnRect, btnH / 2f, btnH / 2f, strokePaint);

            TextPaint pillPaint = new TextPaint(Ui.text(Ui.semibold, 21 * k, 0xFFFFFFFF));
            pillPaint.setTextAlign(Paint.Align.CENTER);
            c.drawText("More apps", cx, btnY + 33 * k, pillPaint);
        }
    }

    private Bitmap getPoster(TitleItem it) {
        if (it == null) return null;
        if (posterCache.containsKey(it.key)) return posterCache.get(it.key);

        Bitmap b = loadAssetImage("tv-assets/posters/" + it.key + ".jpg");
        if (b == null) b = loadDiskImage("Downloads/tv-assets/posters/" + it.title + ".jpg");
        if (b != null) posterCache.put(it.key, b);
        return b;
    }

    private Bitmap getMonoBackdrop(TitleItem it) {
        if (it == null) return null;
        if (monoBackdropCache.containsKey(it.key)) return monoBackdropCache.get(it.key);

        Bitmap b = loadAssetImage("tv-assets/backdrops/" + it.key + ".jpg");
        if (b == null) b = loadDiskImage("Downloads/tv-assets/backdrops/" + it.title + ".jpg");

        if (b != null) {
            // Convert to heavily darkened, high-contrast monochrome
            Bitmap mono = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(mono);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0f); // monochrome
            ColorMatrix scale = new ColorMatrix();
            scale.setScale(0.38f, 0.38f, 0.40f, 1f); // heavily darkened
            cm.postConcat(scale);
            p.setColorFilter(new ColorMatrixColorFilter(cm));
            c.drawBitmap(b, 0, 0, p);

            monoBackdropCache.put(it.key, mono);
            return mono;
        }
        return null;
    }

    private Bitmap loadAssetImage(String path) {
        try {
            AssetManager am = context.getAssets();
            try (InputStream in = am.open(path)) {
                return BitmapFactory.decodeStream(in);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private Bitmap loadDiskImage(String relativePath) {
        try {
            File f = new File(Environment.getExternalStorageDirectory(), relativePath);
            if (f.exists()) {
                return BitmapFactory.decodeFile(f.getAbsolutePath());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static int blend(int a, int b, float f) {
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return (Math.round(aa + (ba - aa) * f) << 24) | (Math.round(ar + (br - ar) * f) << 16)
                | (Math.round(ag + (bg - ag) * f) << 8) | Math.round(ab + (bb - ab) * f);
    }
}
