// =====================================================================================
//  ТИХАЯ ЗАВОДЬ — уютная 3D-рыбалка на стилизованном острове
//  Движок: raylib 5.5 + собственные GLSL-шейдеры (вода Герстнера, каустика, тени, пост-обработка)
//  Язык интерфейса: русский. Сборка: Zig cc (кросс-компиляция в Windows x64).
// =====================================================================================
#include "raylib.h"
#include "raymath.h"
#include "rlgl.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <stdint.h>

#include "shaders.h"
#include "font_data.h"

#define GAME_VERSION "1.0.0"
#define WORLD_SIZE 220.0f
#define HM_RES 256
#define TERRAIN_N 200
#define MAX_PROPS 700
#define MAX_FISH 48
#define MAX_PARTICLES 1200
#define MAX_RIPPLES 16
#define NUM_SPECIES 9
#define NUM_ACH 12
#define DAY_LENGTH 480.0f   // секунд реального времени на игровые сутки

// ------------------------------------------------------------------------------------
//  Утилиты
// ------------------------------------------------------------------------------------
static float frand(void) { return (float)GetRandomValue(0, 100000) / 100000.0f; }
static float frange(float a, float b) { return a + (b - a) * frand(); }
static float sstep(float a, float b, float x) { x = Clamp((x - a) / (b - a), 0, 1); return x * x * (3 - 2 * x); }
static float damp(float cur, float target, float lambda, float dt) { return Lerp(cur, target, 1.0f - expf(-lambda * dt)); }
static Vector3 damp3(Vector3 c, Vector3 t, float l, float dt) { return Vector3Lerp(c, t, 1.0f - expf(-l * dt)); }

static float hash2(float x, float y) { float h = sinf(x * 127.1f + y * 311.7f) * 43758.5453f; return h - floorf(h); }
static float vnoise(float x, float y) {
    float ix = floorf(x), iy = floorf(y), fx = x - ix, fy = y - iy;
    fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
    float a = hash2(ix, iy), b = hash2(ix + 1, iy), c = hash2(ix, iy + 1), d = hash2(ix + 1, iy + 1);
    return Lerp(Lerp(a, b, fx), Lerp(c, d, fx), fy);
}
static float fbm(float x, float y) { float v = 0, a = 0.5f; for (int i = 0; i < 5; i++) { v += a * vnoise(x, y); x = x * 2.03f + 1.7f; y = y * 2.03f + 9.2f; a *= 0.5f; } return v; }

// ------------------------------------------------------------------------------------
//  Рельеф острова
// ------------------------------------------------------------------------------------
static float TerrainHeight(float x, float z) {
    float r = sqrtf(x * x + z * z);
    float ang = atan2f(z, x);
    float coast = 58.0f + 13.0f * sinf(ang * 3.0f + 1.3f) + 7.0f * sinf(ang * 7.0f + 0.4f) + 9.0f * (fbm(cosf(ang) * 2.0f + 5.0f, sinf(ang) * 2.0f + 5.0f) - 0.5f);
    float t = r / coast;
    float island = (1.0f - sstep(0.45f, 1.0f, t)) * 9.5f;
    float shelf = -1.2f - 7.5f * sstep(1.0f, 1.55f, t);
    float h = (t < 1.0f) ? island + shelf * sstep(0.7f, 1.0f, t) : shelf;
    h += (fbm(x * 0.045f + 3.1f, z * 0.045f + 7.7f) - 0.5f) * 5.0f * (0.4f + 0.6f * (1.0f - sstep(0.9f, 1.2f, t)));
    // озерцо-заводь внутри острова
    float px = x - 14.0f, pz = z + 10.0f; float pr = sqrtf(px * px + pz * pz);
    h -= 6.5f * (1.0f - sstep(6.0f, 16.0f, pr));
    // холм
    float hx = x + 22.0f, hz = z - 18.0f; float hr = sqrtf(hx * hx + hz * hz);
    h += 5.0f * (1.0f - sstep(4.0f, 20.0f, hr));
    return h;
}
static Vector3 TerrainNormal(float x, float z) {
    float e = 0.6f;
    float hl = TerrainHeight(x - e, z), hr = TerrainHeight(x + e, z), hd = TerrainHeight(x, z - e), hu = TerrainHeight(x, z + e);
    return Vector3Normalize((Vector3){ hl - hr, 2 * e, hd - hu });
}

// Волны Герстнера на CPU (синхронно с WATER_VS)
static float GerstnerY(float x, float z, float dx, float dz, float steep, float wl, float speed, float t) {
    float k = 6.2831853f / wl; float c = sqrtf(9.8f / k); float l = sqrtf(dx * dx + dz * dz); dx /= l; dz /= l;
    float f = k * ((dx * x + dz * z) - c * speed * t); float a = steep / k;
    return a * sinf(f);
}
static float WaveHeight(float x, float z, float t) {
    float y = 0;
    y += GerstnerY(x, z, 1.0f, 0.6f, 0.10f, 9.0f, 0.9f, t);
    y += GerstnerY(x, z, -0.7f, 0.3f, 0.08f, 5.5f, 1.1f, t);
    y += GerstnerY(x, z, 0.2f, -1.0f, 0.06f, 3.0f, 1.3f, t);
    y += GerstnerY(x, z, 0.9f, -0.4f, 0.04f, 1.6f, 1.6f, t);
    return y;
}

// ------------------------------------------------------------------------------------
//  Данные игры
// ------------------------------------------------------------------------------------
typedef enum { ST_MENU, ST_PLAY, ST_PAUSE, ST_SETTINGS, ST_CONTROLS, ST_ACHIEVEMENTS, ST_ATLAS, ST_VICTORY, ST_GAMEOVER } GameState;
typedef enum { PROP_TREE, PROP_ROCK, PROP_REED, PROP_BUSH, PROP_LILY, PROP_PIERPOST, PROP_PLANK, PROP_LOG, PROP_STUMP } PropType;
typedef enum { FS_IDLE, FS_CHARGE, FS_FLY, FS_WAIT, FS_BITE, FS_FIGHT, FS_CAUGHT, FS_LOST } FishState;

typedef struct { PropType type; Vector3 pos; float scale; float rot; Color col; Color col2; float phase; } Prop;
typedef struct { Vector3 pos; Vector3 vel; float yaw; int species; float size; float tailPhase; float wander; int targetBobber; float nibbleTimer; int nibbles; float cooldown; float life; } Fish;
typedef struct { Vector3 pos; Vector3 vel; float life; float maxLife; float size; Color col; int type; } Particle;
typedef struct { Vector2 pos; float age; float amp; int active; } Ripple;
typedef struct { const char *name; const char *latin; Color col; float minSize, maxSize; float minDepth; float rarity; int night; int score; float strength; } Species;
typedef struct { const char *name; const char *desc; int unlocked; } Achievement;

static const Species SPECIES[NUM_SPECIES] = {
    { "Плотва",        "обычная речная рыба",         { 190, 200, 210, 255 }, 0.2f, 0.6f,  0.6f, 1.00f, 0, 10,  0.25f },
    { "Окунь",         "полосатый хищник",            { 90, 150, 70, 255 },   0.3f, 1.2f,  0.8f, 0.80f, 0, 20,  0.40f },
    { "Карась",        "золотистый житель заводи",    { 220, 170, 60, 255 },  0.3f, 1.5f,  0.5f, 0.75f, 0, 25,  0.35f },
    { "Лещ",           "широкий и осторожный",        { 160, 140, 110, 255 }, 0.8f, 3.0f,  1.5f, 0.50f, 0, 40,  0.55f },
    { "Щука",          "зубастая торпеда",            { 70, 110, 60, 255 },   1.0f, 5.0f,  1.2f, 0.35f, 0, 70,  0.80f },
    { "Форель",        "радужная красавица",          { 230, 120, 140, 255 }, 0.4f, 2.0f,  1.0f, 0.40f, 0, 55,  0.65f },
    { "Налим",         "ночной донный хищник",        { 110, 90, 60, 255 },   0.8f, 3.5f,  2.0f, 0.45f, 1, 60,  0.60f },
    { "Судак",         "сумеречный охотник",          { 150, 160, 130, 255 }, 1.0f, 4.0f,  2.5f, 0.30f, 1, 80,  0.75f },
    { "Старый Сом",    "легенда Тихой Заводи",        { 60, 55, 70, 255 },    8.0f, 14.0f, 3.0f, 0.00f, 1, 500, 1.00f },
};

static Achievement ACH[NUM_ACH] = {
    { "Первый улов",        "Поймайте свою первую рыбу", 0 },
    { "Коллекционер",       "Поймайте 5 разных видов рыб", 0 },
    { "Полный атлас",       "Поймайте все 8 обычных видов", 0 },
    { "Ночной рыбак",       "Поймайте рыбу ночью", 0 },
    { "Трофей",             "Поймайте рыбу тяжелее 3 кг", 0 },
    { "Твёрдая рука",       "Поймайте 5 рыб подряд без срыва", 0 },
    { "Тысячник",           "Наберите 1000 очков", 0 },
    { "Старожил",           "Проведите на острове 3 дня", 0 },
    { "Походный повар",     "Приготовьте и съешьте 5 рыб у костра", 0 },
    { "Исследователь",      "Посетите все 4 смотровые точки острова", 0 },
    { "Терпение",           "Сделайте 50 забросов", 0 },
    { "Легенда Заводи",     "Поймайте Старого Сома", 0 },
};

typedef struct {
    // настройки
    int fullscreen, vsync, quality, showTutorial; float musicVol, sfxVol, sensitivity;
    // прогресс
    int score, highScore, totalCaught, caughtBySpecies[NUM_SPECIES], casts, streak, bestStreak, eaten, days, spotsVisited[4];
    float bestWeight; int achUnlocked[NUM_ACH]; int inventory[NUM_SPECIES]; float energy; float timeOfDay; int legendUnlocked; int victory;
    float playerX, playerZ, playerYaw;
} SaveData;

// ------------------------------------------------------------------------------------
//  Глобальное состояние
// ------------------------------------------------------------------------------------
static int screenW = 1280, screenH = 720;
static GameState state = ST_MENU, prevState = ST_MENU;
static SaveData S;
static Font font;
static float gTime = 0, dt = 0;
static Camera3D cam;
static float camYaw = 0.6f, camPitch = 0.35f, camDist = 6.5f, camShake = 0; static Vector3 camTargetSmooth, camPosSmooth;
static Vector3 playerPos, playerVel, playerLean; static float playerYaw = 0, playerBob = 0, walkPhase = 0, moveAmount = 0, playerYawSmooth = 0;
static float stepTimer = 0;

// рендер
static Shader shLit, shWater, shSky, shShadow, shPost, shLine;
static int locLitLightVP, locLitSun, locLitSunCol, locLitAmb, locLitFog, locLitView, locLitTime, locLitFogD, locLitWater, locLitShadowMap, locLitShadowRes, locLitSway, locLitEmissive;
static int locWSun, locWSunCol, locWAmb, locWFog, locWView, locWTime, locWFogD, locWWorld, locWSky, locWHor, locWRip, locWRipN;
static int locSSun, locSSunCol, locSSky, locSHor, locSTime, locSNight;
static int locPRes, locPTime, locPBloom, locPDof, locPFocus, locPVig, locPUnder, locPFlash;
static RenderTexture2D sceneRT, shadowRT; static int shadowRes = 2048;
static Texture2D hmTex, whiteTex, particleTex;
static Mesh meshTerrain, meshWater, meshSky, meshCube, meshSphere, meshCone, meshCyl, meshQuad, meshFishBody, meshTail;
static Material matLit, matWater, matSky, matShadow;
static Matrix lightVP; static Vector3 sunDir, sunColor, ambientColor, fogColor, skyColor, horizonColor; static float fogDensity = 0.004f, nightAmount = 0;

static Prop props[MAX_PROPS]; static int propCount = 0;
static Fish fish[MAX_FISH]; static int fishCount = 0;
static Particle parts[MAX_PARTICLES]; static int partCount = 0;
static Ripple ripples[MAX_RIPPLES]; static float rippleData[MAX_RIPPLES * 4];
static Vector3 pierPos = { 0 }, firePos = { 0 }; static float pierYaw = 0;
static Vector3 spots[4]; static const char *spotNames[4] = { "Холм ветров", "Сосновый мыс", "Тихая заводь", "Дальняя коса" };

// рыбалка
static FishState fstate = FS_IDLE; static float castPower = 0, fTimer = 0, tension = 0, lineLen = 0, fishPull = 0, fishPullTarget = 0;
static Vector3 bobberPos, bobberVel; static int hookedFish = -1; static float caughtSize = 0; static int caughtSpecies = 0; static int bobberInWater = 0;
static float rodBend = 0, rodBendTarget = 0; static float waitTime = 0;
static int fishNearBobber = -1; static float nibbleFlash = 0;
static float flashAmount = 0;

// UI
static char toastText[256] = ""; static float toastTimer = 0; static char msgText[256] = ""; static float msgTimer = 0;
static int tutorialStep = 0; static float tutorialTimer = 0;
static int menuSel = 0; static float menuAnim = 0; static int settingsSel = 0;
static float uiScale = 1.0f;
static int statusSound = 1;

// аудио
static Sound sndSplash, sndPlop, sndStep, sndClick, sndCatch, sndSnap, sndBite, sndEat, sndCast, sndAch, sndReel;
static AudioStream music; static short musicBuf[4096]; static double musicPhase[8]; static float musicT = 0; static float reelTimer = 0;

// ------------------------------------------------------------------------------------
//  Сохранение / загрузка
// ------------------------------------------------------------------------------------
static const char *SavePath(void) { static char p[1024]; snprintf(p, sizeof(p), "%stikhaya_zavod_save.dat", GetApplicationDirectory()); return p; }
static void DefaultSave(void) {
    memset(&S, 0, sizeof(S)); S.fullscreen = 0; S.vsync = 1; S.quality = 1; S.showTutorial = 1; S.musicVol = 0.6f; S.sfxVol = 0.8f; S.sensitivity = 1.0f;
    S.energy = 100; S.timeOfDay = 8.5f; S.playerX = 1e9f;
}
static void SaveGame(void) {
    for (int i = 0; i < NUM_ACH; i++) S.achUnlocked[i] = ACH[i].unlocked;
    S.playerX = playerPos.x; S.playerZ = playerPos.z; S.playerYaw = playerYaw;
    if (S.score > S.highScore) S.highScore = S.score;
    FILE *f = fopen(SavePath(), "wb"); if (!f) return;
    uint32_t magic = 0x5A41564F; fwrite(&magic, 4, 1, f); uint32_t sz = sizeof(S); fwrite(&sz, 4, 1, f); fwrite(&S, sizeof(S), 1, f); fclose(f);
}
static int LoadGame(void) {
    FILE *f = fopen(SavePath(), "rb"); if (!f) return 0;
    uint32_t magic = 0, sz = 0; if (fread(&magic, 4, 1, f) != 1 || magic != 0x5A41564F || fread(&sz, 4, 1, f) != 1 || sz != sizeof(S)) { fclose(f); return 0; }
    if (fread(&S, sizeof(S), 1, f) != 1) { fclose(f); return 0; } fclose(f);
    for (int i = 0; i < NUM_ACH; i++) ACH[i].unlocked = S.achUnlocked[i];
    return 1;
}

// ------------------------------------------------------------------------------------
//  Процедурный звук
// ------------------------------------------------------------------------------------
static Sound MakeSound(int frames, int kind) {
    short *data = (short *)malloc(frames * sizeof(short)); float sr = 22050; float ph = 0; float lp = 0; float br = 0;
    for (int i = 0; i < frames; i++) {
        float t = (float)i / sr, u = (float)i / frames; float v = 0; float n = frand() * 2 - 1;
        switch (kind) {
        case 0: { // всплеск: шум с низкочастотным фильтром и быстрым спадом
            lp += (n - lp) * (0.08f + 0.5f * (1 - u)); v = lp * expf(-u * 5.0f) * 2.5f + n * 0.15f * expf(-u * 9.0f); } break;
        case 1: { // плюх поплавка: нисходящий синус + капля
            float fr = 900.0f * expf(-u * 6.0f) + 120.0f; ph += fr / sr; v = sinf(ph * 6.2831853f) * expf(-u * 7.0f); br += (n - br) * 0.1f; v += br * 0.4f * expf(-u * 12.0f); } break;
        case 2: { // шаг по траве
            lp += (n - lp) * 0.3f; v = lp * expf(-u * 10.0f) * 1.2f; } break;
        case 3: { // клик UI
            ph += 1200.0f / sr; v = sinf(ph * 6.2831853f) * expf(-u * 25.0f) * 0.8f; } break;
        case 4: { // мелодия улова: три ноты
            int k = (int)(u * 3); float f = (k == 0) ? 523.25f : (k == 1) ? 659.25f : 783.99f; float lu = u * 3 - k; ph += f / sr;
            v = (sinf(ph * 6.2831853f) + 0.3f * sinf(ph * 12.566f)) * expf(-lu * 4.0f) * 0.6f; } break;
        case 5: { // обрыв лески
            ph += 2400.0f / sr; v = (n * 0.7f + sinf(ph * 6.2831853f) * 0.3f) * expf(-u * 18.0f); } break;
        case 6: { // поклёвка: глухой бульк
            float fr = 400.0f * expf(-u * 4.0f) + 80.0f; ph += fr / sr; v = sinf(ph * 6.2831853f) * expf(-u * 6.0f) * 0.9f; } break;
        case 7: { // еда: хруст
            lp += (n - lp) * 0.6f; v = lp * (0.5f + 0.5f * sinf(t * 40.0f)) * expf(-u * 3.0f); } break;
        case 8: { // свист заброса
            float fr = 300.0f + 1500.0f * u; lp += (n - lp) * 0.05f; ph += fr / sr; v = (lp * 2.0f + 0.15f * sinf(ph * 6.2831853f)) * sinf(u * 3.14159f); } break;
        case 9: { // достижение: арпеджио
            int k = (int)(u * 4); float f = (k == 0) ? 659.25f : (k == 1) ? 783.99f : (k == 2) ? 987.77f : 1318.5f; float lu = u * 4 - k; ph += f / sr;
            v = (sinf(ph * 6.2831853f) + 0.2f * sinf(ph * 18.85f)) * expf(-lu * 3.0f) * 0.5f; } break;
        case 10: { // трещотка катушки
            float tick = fmodf(t * 28.0f, 1.0f); lp += (n - lp) * 0.4f; v = lp * expf(-tick * 25.0f) * 0.9f; } break;
        }
        v = Clamp(v, -1, 1); data[i] = (short)(v * 32000);
    }
    Wave w = { 0 }; w.frameCount = frames; w.sampleRate = 22050; w.sampleSize = 16; w.channels = 1; w.data = data;
    Sound s = LoadSoundFromWave(w); free(data); return s;
}
static void PlaySpatial(Sound s, Vector3 at, float vol) {
    if (!IsAudioDeviceReady()) return;
    Vector3 d = Vector3Subtract(at, cam.position); float dist = Vector3Length(d);
    float att = Clamp(1.0f - dist / 60.0f, 0, 1); att *= att;
    Vector3 fwd = Vector3Normalize(Vector3Subtract(cam.target, cam.position)); Vector3 right = Vector3Normalize(Vector3CrossProduct(fwd, (Vector3){ 0, 1, 0 }));
    float pan = (dist > 0.01f) ? Vector3DotProduct(Vector3Scale(d, 1.0f / dist), right) : 0;
    SetSoundPan(s, Clamp(0.5f - pan * 0.4f, 0, 1)); SetSoundVolume(s, att * vol * S.sfxVol); PlaySound(s);
}
static void PlayUI(Sound s, float vol) { if (!IsAudioDeviceReady()) return; SetSoundPan(s, 0.5f); SetSoundVolume(s, vol * S.sfxVol); PlaySound(s); }

// фоновая музыка: мягкие аккорды + шум воды
static void UpdateMusic(void) {
    if (!IsAudioDeviceReady()) return;
    for (int guard = 0; guard < 4 && IsAudioStreamProcessed(music); guard++) {
        int n = 2048; static const float chords[4][3] = { { 220.0f, 261.63f, 329.63f }, { 174.61f, 220.0f, 261.63f }, { 196.0f, 246.94f, 293.66f }, { 164.81f, 196.0f, 246.94f } };
        static float br = 0; static float lp2 = 0;
        for (int i = 0; i < n; i++) {
            musicT += 1.0f / 22050.0f; int ci = ((int)(musicT / 10.0f)) % 4; float cu = fmodf(musicT, 10.0f) / 10.0f;
            float env = sinf(cu * 3.14159f); env = 0.35f + 0.65f * env;
            float v = 0;
            for (int k = 0; k < 3; k++) {
                float f = chords[ci][k] * (1.0f + 0.002f * sinf(musicT * 0.7f + k)); musicPhase[k] += f / 22050.0; if (musicPhase[k] > 1) musicPhase[k] -= 1;
                float p = (float)musicPhase[k]; v += (sinf(p * 6.2831853f) + 0.35f * sinf(p * 12.566f) + 0.12f * sinf(p * 25.13f)) * 0.12f;
                musicPhase[k + 3] += f * 0.5f / 22050.0; if (musicPhase[k + 3] > 1) musicPhase[k + 3] -= 1; v += sinf((float)musicPhase[k + 3] * 6.2831853f) * 0.06f;
            }
            // арпеджио-"капли"
            float beat = fmodf(musicT, 2.5f); int bi = ((int)(musicT / 2.5f)) % 3; float nf = chords[ci][bi] * 4.0f; musicPhase[6] += nf / 22050.0; if (musicPhase[6] > 1) musicPhase[6] -= 1;
            v += sinf((float)musicPhase[6] * 6.2831853f) * expf(-beat * 3.0f) * 0.08f * (0.5f + 0.5f * nightAmount);
            v *= env;
            // прибой
            float nz = frand() * 2 - 1; br += (nz - br) * 0.02f; lp2 += (br - lp2) * 0.05f; float surf = 0.5f + 0.5f * sinf(musicT * 0.45f); v += lp2 * 3.0f * (0.2f + 0.5f * surf);
            musicBuf[i] = (short)(Clamp(v, -1, 1) * 28000);
        }
        UpdateAudioStream(music, musicBuf, n);
    }
}

// ------------------------------------------------------------------------------------
//  UI-помощники
// ------------------------------------------------------------------------------------
static void Toast(const char *t) { snprintf(toastText, sizeof(toastText), "%s", t); toastTimer = 4.0f; }
static void Msg(const char *t, float secs) { snprintf(msgText, sizeof(msgText), "%s", t); msgTimer = secs; }
static void Unlock(int i) { if (ACH[i].unlocked) return; ACH[i].unlocked = 1; char b[256]; snprintf(b, sizeof(b), "Достижение: %s", ACH[i].name); Toast(b); PlayUI(sndAch, 0.9f); }
static void DrawTextC(const char *t, float x, float y, float sz, Color c) { Vector2 m = MeasureTextEx(font, t, sz, 1); DrawTextEx(font, t, (Vector2){ x - m.x / 2, y }, sz, 1, c); }
static void DrawTextS(const char *t, float x, float y, float sz, Color c) { DrawTextEx(font, t, (Vector2){ x + 2, y + 2 }, sz, 1, Fade(BLACK, 0.5f)); DrawTextEx(font, t, (Vector2){ x, y }, sz, 1, c); }
static void DrawTextCS(const char *t, float x, float y, float sz, Color c) { Vector2 m = MeasureTextEx(font, t, sz, 1); DrawTextS(t, x - m.x / 2, y, sz, c); }
static int Button(const char *t, float x, float y, float w, float h, int selected) {
    Vector2 m = GetMousePosition(); int hover = CheckCollisionPointRec(m, (Rectangle){ x, y, w, h });
    Color bg = (hover || selected) ? (Color){ 255, 200, 90, 230 } : (Color){ 20, 50, 70, 200 }; Color fg = (hover || selected) ? (Color){ 30, 30, 40, 255 } : RAYWHITE;
    DrawRectangleRounded((Rectangle){ x, y, w, h }, 0.35f, 8, bg); DrawRectangleRoundedLines((Rectangle){ x, y, w, h }, 0.35f, 8, Fade(WHITE, 0.35f));
    Vector2 ms = MeasureTextEx(font, t, h * 0.48f, 1); DrawTextEx(font, t, (Vector2){ x + (w - ms.x) / 2, y + (h - ms.y) / 2 }, h * 0.48f, 1, fg);
    if (hover && IsMouseButtonPressed(MOUSE_LEFT_BUTTON)) { PlayUI(sndClick, 0.7f); return 1; }
    return 0;
}
static void Panel(float x, float y, float w, float h) { DrawRectangleRounded((Rectangle){ x, y, w, h }, 0.08f, 8, (Color){ 10, 30, 45, 215 }); DrawRectangleRoundedLines((Rectangle){ x, y, w, h }, 0.08f, 8, Fade((Color){ 255, 220, 150, 255 }, 0.5f)); }

// ------------------------------------------------------------------------------------
//  Частицы и круги на воде
// ------------------------------------------------------------------------------------
static void Emit(Vector3 p, Vector3 v, float life, float size, Color c, int type) {
    if (partCount >= MAX_PARTICLES) return; Particle *q = &parts[partCount++]; q->pos = p; q->vel = v; q->life = life; q->maxLife = life; q->size = size; q->col = c; q->type = type;
}
static void Splash(Vector3 p, float power) {
    int n = (int)(12 + power * 30);
    for (int i = 0; i < n; i++) { float a = frand() * 6.283f, s = frange(0.5f, 2.5f) * (0.5f + power); Emit(p, (Vector3){ cosf(a) * s * 0.6f, frange(1.5f, 4.0f) * (0.4f + power), sinf(a) * s * 0.6f }, frange(0.4f, 0.9f), frange(0.06f, 0.16f) * (0.6f + power), (Color){ 230, 245, 255, 220 }, 0); }
    for (int i = 0; i < 6; i++) { float a = frand() * 6.283f; Emit(p, (Vector3){ cosf(a) * 0.6f, 0.3f, sinf(a) * 0.6f }, 0.6f, 0.5f * (0.5f + power), (Color){ 255, 255, 255, 120 }, 2); }
}
static void AddRipple(Vector2 p, float amp) {
    int best = 0; float oldest = -1; for (int i = 0; i < MAX_RIPPLES; i++) { if (!ripples[i].active) { best = i; break; } if (ripples[i].age > oldest) { oldest = ripples[i].age; best = i; } }
    ripples[best] = (Ripple){ p, 0, amp, 1 };
}
static void UpdateParticles(void) {
    for (int i = 0; i < partCount; i++) {
        Particle *q = &parts[i]; q->life -= dt;
        if (q->type == 0) { q->vel.y -= 9.8f * dt; q->pos = Vector3Add(q->pos, Vector3Scale(q->vel, dt)); float wh = WaveHeight(q->pos.x, q->pos.z, gTime); if (q->pos.y < wh && q->vel.y < 0) { q->life = 0; } }
        else if (q->type == 1) { q->pos = Vector3Add(q->pos, Vector3Scale(q->vel, dt)); q->vel.x += sinf(gTime * 1.3f + q->pos.z) * 0.3f * dt; q->vel.z += cosf(gTime * 1.1f + q->pos.x) * 0.3f * dt; }  // пыльца/светлячки
        else if (q->type == 2) { q->pos = Vector3Add(q->pos, Vector3Scale(q->vel, dt)); q->vel = Vector3Scale(q->vel, 1.0f - 2.0f * dt); q->size += dt * 0.5f; }  // брызги-туман
        else if (q->type == 3) { q->pos = Vector3Add(q->pos, Vector3Scale(q->vel, dt)); q->vel.y += 1.5f * dt; q->vel.x += frange(-1, 1) * dt * 2; q->vel.z += frange(-1, 1) * dt * 2; q->size *= 1.0f - 0.8f * dt; }  // искры костра
        else if (q->type == 4) { q->pos = Vector3Add(q->pos, Vector3Scale(q->vel, dt)); q->vel.y += 0.8f * dt; q->size += dt * 0.6f; }  // дым
        else if (q->type == 5) { q->vel.y -= 9.8f * dt; q->pos = Vector3Add(q->pos, Vector3Scale(q->vel, dt)); }  // капли
        if (q->life <= 0) { parts[i] = parts[--partCount]; i--; }
    }
    for (int i = 0; i < MAX_RIPPLES; i++) if (ripples[i].active) { ripples[i].age += dt; if (ripples[i].age > 3.5f) ripples[i].active = 0; }
    int n = 0; for (int i = 0; i < MAX_RIPPLES; i++) if (ripples[i].active) { rippleData[n * 4] = ripples[i].pos.x; rippleData[n * 4 + 1] = ripples[i].pos.y; rippleData[n * 4 + 2] = ripples[i].age; rippleData[n * 4 + 3] = ripples[i].amp; n++; }
    SetShaderValueV(shWater, locWRip, rippleData, SHADER_UNIFORM_VEC4, MAX_RIPPLES); SetShaderValue(shWater, locWRipN, &n, SHADER_UNIFORM_INT);
    // окружение: пыльца днём, светлячки ночью, искры костра
    if (frand() < dt * 25.0f) { float a = frand() * 6.283f, r = frange(3, 25); Vector3 p = { playerPos.x + cosf(a) * r, 0, playerPos.z + sinf(a) * r }; float h = TerrainHeight(p.x, p.z);
        if (h > 0.3f) { p.y = h + frange(0.3f, 2.5f); if (nightAmount > 0.5f) Emit(p, (Vector3){ frange(-0.4f, 0.4f), frange(-0.2f, 0.3f), frange(-0.4f, 0.4f) }, frange(3, 6), 0.09f, (Color){ 200, 255, 120, 255 }, 1); else Emit(p, (Vector3){ frange(-0.3f, 0.3f), frange(-0.1f, 0.15f), frange(-0.3f, 0.3f) }, frange(4, 8), 0.05f, (Color){ 255, 245, 200, 160 }, 1); } }
    if (frand() < dt * 30.0f) Emit((Vector3){ firePos.x + frange(-0.2f, 0.2f), firePos.y + 0.3f, firePos.z + frange(-0.2f, 0.2f) }, (Vector3){ frange(-0.3f, 0.3f), frange(1.0f, 2.5f), frange(-0.3f, 0.3f) }, frange(0.5f, 1.3f), frange(0.06f, 0.14f), (Color){ 255, 170, 60, 255 }, 3);
    if (frand() < dt * 6.0f) Emit((Vector3){ firePos.x, firePos.y + 0.8f, firePos.z }, (Vector3){ frange(-0.2f, 0.2f), 0.8f, frange(-0.2f, 0.2f) }, frange(2, 4), 0.3f, (Color){ 120, 120, 130, 90 }, 4);
}

// ------------------------------------------------------------------------------------
//  Построение мира
// ------------------------------------------------------------------------------------
static Mesh BuildTerrainMesh(void) {
    int n = TERRAIN_N; int vcount = (n + 1) * (n + 1); int tcount = n * n * 2;
    Mesh m = { 0 }; m.vertexCount = vcount; m.triangleCount = tcount;
    m.vertices = (float *)MemAlloc(vcount * 3 * sizeof(float)); m.normals = (float *)MemAlloc(vcount * 3 * sizeof(float)); m.texcoords = (float *)MemAlloc(vcount * 2 * sizeof(float));
    m.colors = (unsigned char *)MemAlloc(vcount * 4); m.indices = (unsigned short *)MemAlloc(tcount * 3 * sizeof(unsigned short));
    for (int j = 0; j <= n; j++) for (int i = 0; i <= n; i++) {
        int k = j * (n + 1) + i; float x = -WORLD_SIZE / 2 + WORLD_SIZE * i / n, z = -WORLD_SIZE / 2 + WORLD_SIZE * j / n; float h = TerrainHeight(x, z);
        m.vertices[k * 3] = x; m.vertices[k * 3 + 1] = h; m.vertices[k * 3 + 2] = z; Vector3 nn = TerrainNormal(x, z); m.normals[k * 3] = nn.x; m.normals[k * 3 + 1] = nn.y; m.normals[k * 3 + 2] = nn.z;
        m.texcoords[k * 2] = x * 0.1f; m.texcoords[k * 2 + 1] = z * 0.1f;
        float slope = 1.0f - nn.y; float nz = fbm(x * 0.15f, z * 0.15f);
        Color c; Color sand = { 222, 204, 150, 255 }, grass = { 96, 168, 72, 255 }, grass2 = { 70, 140, 66, 255 }, rock = { 120, 115, 110, 255 }, wet = { 150, 140, 100, 255 }, deep = { 70, 90, 80, 255 };
        if (h < -2.5f) c = deep; else if (h < 0.35f) c = ColorLerp(wet, sand, sstep(-2.5f, 0.3f, h)); else c = ColorLerp(sand, ColorLerp(grass, grass2, nz), sstep(0.35f, 1.4f, h));
        if (slope > 0.22f) c = ColorLerp(c, rock, sstep(0.22f, 0.45f, slope));
        if (h > 11.0f) c = ColorLerp(c, (Color){ 150, 150, 140, 255 }, sstep(11, 14, h));
        float v = 0.92f + 0.16f * nz; c.r = (unsigned char)Clamp(c.r * v, 0, 255); c.g = (unsigned char)Clamp(c.g * v, 0, 255); c.b = (unsigned char)Clamp(c.b * v, 0, 255);
        m.colors[k * 4] = c.r; m.colors[k * 4 + 1] = c.g; m.colors[k * 4 + 2] = c.b; m.colors[k * 4 + 3] = 255;
    }
    int t = 0; for (int j = 0; j < n; j++) for (int i = 0; i < n; i++) { int a = j * (n + 1) + i, b = a + 1, c = a + n + 1, d = c + 1; m.indices[t++] = a; m.indices[t++] = c; m.indices[t++] = b; m.indices[t++] = b; m.indices[t++] = c; m.indices[t++] = d; }
    UploadMesh(&m, false); return m;
}
static void AddProp(PropType type, Vector3 pos, float scale, float rot, Color c, Color c2) { if (propCount >= MAX_PROPS) return; props[propCount++] = (Prop){ type, pos, scale, rot, c, c2, frand() * 6.28f }; }
static void BuildWorld(void) {
    SetRandomSeed(1337); propCount = 0;
    // деревья
    for (int i = 0; i < 2000 && propCount < 260; i++) { float x = frange(-75, 75), z = frange(-75, 75); float h = TerrainHeight(x, z); Vector3 nn = TerrainNormal(x, z);
        if (h > 1.6f && nn.y > 0.8f && fbm(x * 0.08f + 20, z * 0.08f) > 0.42f) { float g = frange(0, 1); Color leaf = ColorLerp((Color){ 60, 130, 70, 255 }, (Color){ 120, 180, 60, 255 }, g); if (frand() < 0.2f) leaf = (Color){ 210, 140, 60, 255 }; AddProp(PROP_TREE, (Vector3){ x, h - 0.2f, z }, frange(0.8f, 1.6f), frand() * 6.28f, (Color){ 110, 80, 55, 255 }, leaf); } }
    // камни
    for (int i = 0; i < 1500 && propCount < 360; i++) { float x = frange(-95, 95), z = frange(-95, 95); float h = TerrainHeight(x, z); if (h > -3.0f && h < 12 && frand() < 0.5f) AddProp(PROP_ROCK, (Vector3){ x, h - 0.3f, z }, frange(0.5f, 2.2f), frand() * 6.28f, ColorLerp((Color){ 120, 118, 115, 255 }, (Color){ 150, 140, 125, 255 }, frand()), WHITE); }
    // камыш и кувшинки
    for (int i = 0; i < 3000 && propCount < 560; i++) { float x = frange(-90, 90), z = frange(-90, 90); float h = TerrainHeight(x, z); if (h > -0.6f && h < 0.25f) AddProp(PROP_REED, (Vector3){ x, h, z }, frange(0.8f, 1.5f), frand() * 6.28f, (Color){ 120, 160, 70, 255 }, (Color){ 100, 70, 40, 255 }); }
    for (int i = 0; i < 2000 && propCount < 620; i++) { float x = frange(-40, 40), z = frange(-40, 40); float h = TerrainHeight(x, z); if (h > -1.5f && h < -0.3f) AddProp(PROP_LILY, (Vector3){ x, 0, z }, frange(0.5f, 1.0f), frand() * 6.28f, (Color){ 70, 150, 80, 255 }, (Color){ 255, 200, 220, 255 }); }
    // кусты и пни
    for (int i = 0; i < 1000 && propCount < 680; i++) { float x = frange(-70, 70), z = frange(-70, 70); float h = TerrainHeight(x, z); if (h > 1.0f && frand() < 0.6f) AddProp(frand() < 0.8f ? PROP_BUSH : PROP_STUMP, (Vector3){ x, h, z }, frange(0.6f, 1.3f), frand() * 6.28f, (Color){ 80, 150, 70, 255 }, (Color){ 120, 85, 55, 255 }); }
    // пирс: ищем берег на востоке
    float bestD = 1e9f; for (float a = -0.6f; a < 0.6f; a += 0.02f) { for (float r = 30; r < 90; r += 0.5f) { float x = cosf(a) * r, z = sinf(a) * r; if (TerrainHeight(x, z) < 0.2f) { float d = fabsf(a); if (d < bestD) { bestD = d; pierPos = (Vector3){ x, 0, z }; pierYaw = a; } break; } } }
    Vector3 dir = { cosf(pierYaw), 0, sinf(pierYaw) };
    for (int i = 0; i < 14; i++) { Vector3 p = Vector3Add(pierPos, Vector3Scale(dir, -3.0f + i * 1.0f)); AddProp(PROP_PLANK, (Vector3){ p.x, 0.75f, p.z }, 1.0f, pierYaw, (Color){ 160, 120, 80, 255 }, WHITE); if (i % 3 == 0) { Vector3 side = { -dir.z, 0, dir.x }; AddProp(PROP_PIERPOST, Vector3Add(p, Vector3Scale(side, 1.0f)), 1, 0, (Color){ 110, 80, 55, 255 }, WHITE); AddProp(PROP_PIERPOST, Vector3Add(p, Vector3Scale(side, -1.0f)), 1, 0, (Color){ 110, 80, 55, 255 }, WHITE); } }
    // костёр около пирса на суше
    firePos = Vector3Add(pierPos, Vector3Scale(dir, -8.0f)); Vector3 side = { -dir.z, 0, dir.x }; firePos = Vector3Add(firePos, Vector3Scale(side, 4.0f)); firePos.y = TerrainHeight(firePos.x, firePos.z);
    for (int i = 0; i < 3; i++) AddProp(PROP_LOG, (Vector3){ firePos.x, firePos.y + 0.15f, firePos.z }, 1.0f, i * 2.09f, (Color){ 90, 60, 40, 255 }, WHITE);
    for (int i = 0; i < 8; i++) { float a = i * 0.785f; AddProp(PROP_ROCK, (Vector3){ firePos.x + cosf(a) * 0.9f, firePos.y - 0.1f, firePos.z + sinf(a) * 0.9f }, 0.35f, a, (Color){ 110, 105, 100, 255 }, WHITE); }
    // смотровые точки
    spots[0] = (Vector3){ -22, 0, 18 }; spots[1] = (Vector3){ 40, 0, -40 }; spots[2] = (Vector3){ 14, 0, -10 }; spots[3] = (Vector3){ -55, 0, -20 };
    for (int i = 0; i < 4; i++) { float bh = -100; Vector3 best = spots[i]; for (int k = 0; k < 60; k++) { float x = spots[i].x + frange(-10, 10), z = spots[i].z + frange(-10, 10); float h = TerrainHeight(x, z); if (i == 2) { if (h < 0.8f && h > 0.1f && h > bh) { bh = h; best = (Vector3){ x, h, z }; } } else if (h > bh) { bh = h; best = (Vector3){ x, h, z }; } } spots[i] = best; if (i != 2) spots[i].y = TerrainHeight(best.x, best.z); }
    // рыба
    fishCount = 0; for (int i = 0; i < 4000 && fishCount < MAX_FISH; i++) { float x = frange(-100, 100), z = frange(-100, 100); float h = TerrainHeight(x, z); if (h < -1.0f && h > -12) { Fish *f = &fish[fishCount++]; int sp; do { sp = GetRandomValue(0, NUM_SPECIES - 2); } while (frand() > SPECIES[sp].rarity || -h < SPECIES[sp].minDepth); f->species = sp; f->size = frange(SPECIES[sp].minSize, SPECIES[sp].maxSize); f->pos = (Vector3){ x, h * 0.5f - 0.3f, z }; f->yaw = frand() * 6.28f; f->targetBobber = 0; f->cooldown = 0; f->life = 1; } }
}
static void PlaceLegend(void) { // Старый Сом — в самой глубокой точке рядом с пирсом
    for (int i = 0; i < fishCount; i++) if (fish[i].species == NUM_SPECIES - 1) return;
    if (fishCount >= MAX_FISH) fishCount--; Fish *f = &fish[fishCount++]; float bestH = 0; Vector3 bp = pierPos;
    for (int k = 0; k < 400; k++) { float x = pierPos.x + frange(-25, 25), z = pierPos.z + frange(-25, 25); float h = TerrainHeight(x, z); if (h < bestH) { bestH = h; bp = (Vector3){ x, h * 0.6f, z }; } }
    f->species = NUM_SPECIES - 1; f->size = 11.0f; f->pos = bp; f->yaw = 0; f->targetBobber = 0; f->cooldown = 0; f->life = 1;
}

// ------------------------------------------------------------------------------------
//  Освещение / время суток
// ------------------------------------------------------------------------------------
static void UpdateDayCycle(void) {
    float t = S.timeOfDay; float sunAng = (t - 6.0f) / 12.0f * PI; // 6:00 восход, 18:00 закат
    float elev = sinf(sunAng); float az = -cosf(sunAng);
    Vector3 sunPos = Vector3Normalize((Vector3){ az * 0.8f, elev, 0.45f });
    float day = Clamp(elev * 3.0f + 0.2f, 0, 1); nightAmount = 1.0f - sstep(-0.15f, 0.12f, elev);
    float dusk = expf(-powf(elev * 4.0f, 2.0f));
    if (elev > -0.2f) sunDir = Vector3Negate(sunPos); else sunDir = Vector3Negate(Vector3Normalize((Vector3){ -az * 0.5f, 0.6f, -0.3f })); // луна
    Vector3 dayCol = { 1.0f, 0.95f, 0.85f }, duskCol = { 1.0f, 0.55f, 0.3f }, moonCol = { 0.25f, 0.32f, 0.5f };
    sunColor = Vector3Lerp(Vector3Lerp(dayCol, duskCol, dusk), moonCol, nightAmount); sunColor = Vector3Scale(sunColor, 0.9f + 0.4f * day);
    Vector3 ambDay = { 0.42f, 0.50f, 0.58f }, ambNight = { 0.10f, 0.13f, 0.22f }; ambientColor = Vector3Lerp(ambDay, ambNight, nightAmount);
    Vector3 skyDay = { 0.35f, 0.62f, 0.95f }, skyDusk = { 0.45f, 0.35f, 0.6f }, skyNight = { 0.03f, 0.05f, 0.12f };
    Vector3 horDay = { 0.75f, 0.88f, 0.98f }, horDusk = { 1.0f, 0.6f, 0.4f }, horNight = { 0.08f, 0.1f, 0.2f };
    skyColor = Vector3Lerp(Vector3Lerp(skyDay, skyDusk, dusk), skyNight, nightAmount); horizonColor = Vector3Lerp(Vector3Lerp(horDay, horDusk, dusk), horNight, nightAmount);
    fogColor = Vector3Lerp(horizonColor, skyColor, 0.3f); fogDensity = 0.0035f + 0.003f * nightAmount + 0.004f * dusk * (1.0f - nightAmount);
}

// ------------------------------------------------------------------------------------
//  Отрисовка сцены (общая для теневого и основного прохода)
// ------------------------------------------------------------------------------------
static int shadowPass = 0;
static void SetSway(float s) { if (!shadowPass) SetShaderValue(shLit, locLitSway, &s, SHADER_UNIFORM_FLOAT); }
static void SetEmissive(float e) { if (!shadowPass) SetShaderValue(shLit, locLitEmissive, &e, SHADER_UNIFORM_FLOAT); }
static void DrawM(Mesh *m, Matrix tr, Color c) {
    if (shadowPass) { DrawMesh(*m, matShadow, tr); } else { matLit.maps[MATERIAL_MAP_DIFFUSE].color = c; DrawMesh(*m, matLit, tr); }
}
static Matrix TRS(Vector3 p, Vector3 axis, float ang, Vector3 s) { return MatrixMultiply(MatrixMultiply(MatrixScale(s.x, s.y, s.z), MatrixRotate(axis, ang)), MatrixTranslate(p.x, p.y, p.z)); }
static Matrix TRSy(Vector3 p, float yaw, Vector3 s) { return TRS(p, (Vector3){ 0, 1, 0 }, yaw, s); }

static void DrawProps(void) {
    Vector3 cp = cam.position; float cull2 = 150.0f * 150.0f;
    for (int i = 0; i < propCount; i++) {
        Prop *p = &props[i]; Vector3 d = Vector3Subtract(p->pos, cp); if (d.x * d.x + d.z * d.z > cull2) continue;
        float s = p->scale;
        switch (p->type) {
        case PROP_TREE: {
            SetSway(0.0f); DrawM(&meshCyl, TRSy(p->pos, p->rot, (Vector3){ 0.25f * s, 1.8f * s, 0.25f * s }), p->col);
            SetSway(0.08f);
            DrawM(&meshCone, TRSy((Vector3){ p->pos.x, p->pos.y + 1.5f * s, p->pos.z }, p->rot, (Vector3){ 1.6f * s, 2.2f * s, 1.6f * s }), p->col2);
            DrawM(&meshCone, TRSy((Vector3){ p->pos.x, p->pos.y + 2.7f * s, p->pos.z }, p->rot + 0.5f, (Vector3){ 1.25f * s, 1.9f * s, 1.25f * s }), ColorBrightness(p->col2, 0.08f));
            DrawM(&meshCone, TRSy((Vector3){ p->pos.x, p->pos.y + 3.8f * s, p->pos.z }, p->rot + 1.0f, (Vector3){ 0.85f * s, 1.6f * s, 0.85f * s }), ColorBrightness(p->col2, 0.16f));
            SetSway(0.0f); } break;
        case PROP_ROCK: DrawM(&meshSphere, TRS(p->pos, (Vector3){ 0.3f, 1, 0.2f }, p->rot, (Vector3){ 0.6f * s, 0.42f * s, 0.5f * s }), p->col); break;
        case PROP_REED: {
            SetSway(0.25f);
            for (int k = 0; k < 4; k++) { float a = p->rot + k * 1.7f; Vector3 q = { p->pos.x + cosf(a) * 0.18f * s, p->pos.y, p->pos.z + sinf(a) * 0.18f * s };
                DrawM(&meshCyl, TRS(q, (Vector3){ cosf(a + 1.57f), 0, sinf(a + 1.57f) }, 0.12f, (Vector3){ 0.03f, 1.3f * s + k * 0.1f, 0.03f }), p->col);
                if (k < 2) DrawM(&meshCyl, TRSy((Vector3){ q.x, q.y + 1.25f * s, q.z }, 0, (Vector3){ 0.06f, 0.25f, 0.06f }), p->col2); }
            SetSway(0.0f); } break;
        case PROP_LILY: DrawM(&meshCyl, TRSy((Vector3){ p->pos.x, WaveHeight(p->pos.x, p->pos.z, gTime) + 0.02f, p->pos.z }, p->rot, (Vector3){ 0.45f * s, 0.02f, 0.45f * s }), p->col);
            if (s > 0.8f) DrawM(&meshSphere, TRSy((Vector3){ p->pos.x, WaveHeight(p->pos.x, p->pos.z, gTime) + 0.08f, p->pos.z }, 0, (Vector3){ 0.12f, 0.1f, 0.12f }), p->col2); break;
        case PROP_BUSH: SetSway(0.12f); DrawM(&meshSphere, TRSy(p->pos, p->rot, (Vector3){ 0.9f * s, 0.6f * s, 0.8f * s }), p->col); DrawM(&meshSphere, TRSy((Vector3){ p->pos.x + 0.4f * s, p->pos.y + 0.1f, p->pos.z + 0.2f * s }, p->rot, (Vector3){ 0.6f * s, 0.5f * s, 0.6f * s }), ColorBrightness(p->col, 0.1f)); SetSway(0.0f); break;
        case PROP_STUMP: DrawM(&meshCyl, TRSy(p->pos, p->rot, (Vector3){ 0.4f * s, 0.5f * s, 0.4f * s }), p->col2); break;
        case PROP_PIERPOST: DrawM(&meshCyl, TRSy((Vector3){ p->pos.x, -2.0f, p->pos.z }, 0, (Vector3){ 0.12f, 3.2f, 0.12f }), p->col); break;
        case PROP_PLANK: DrawM(&meshCube, TRSy(p->pos, -p->rot, (Vector3){ 1.0f, 0.12f, 2.3f }), p->col); break;
        case PROP_LOG: DrawM(&meshCyl, TRS(p->pos, (Vector3){ cosf(p->rot), 0, sinf(p->rot) }, 1.45f, (Vector3){ 0.12f, 1.0f, 0.12f }), p->col); break;
        }
    }
    // маркеры смотровых точек
    for (int i = 0; i < 4; i++) { if (S.spotsVisited[i]) continue; Vector3 p = spots[i]; float b = sinf(gTime * 2.0f + i) * 0.2f;
        SetEmissive(0.6f); DrawM(&meshCone, TRS((Vector3){ p.x, p.y + 1.6f + b, p.z }, (Vector3){ 1, 0, 0 }, PI, (Vector3){ 0.4f, 0.7f, 0.4f }), (Color){ 255, 220, 90, 255 }); SetEmissive(0.0f);
        DrawM(&meshCyl, TRSy(p, 0, (Vector3){ 0.08f, 1.0f, 0.08f }), (Color){ 120, 90, 60, 255 }); }
}
static void DrawFishes(void) {
    for (int i = 0; i < fishCount; i++) {
        Fish *f = &fish[i]; if (hookedFish == i && fstate == FS_CAUGHT) continue;
        const Species *sp = &SPECIES[f->species]; float L = 0.25f + f->size * 0.11f; if (f->species == NUM_SPECIES - 1) L = 2.2f;
        float wag = sinf(f->tailPhase) * 0.35f;
        Matrix body = MatrixMultiply(MatrixMultiply(MatrixScale(L, L * 0.28f, L * 0.4f), MatrixRotateY(-f->yaw)), MatrixTranslate(f->pos.x, f->pos.y, f->pos.z));
        DrawM(&meshFishBody, body, sp->col);
        Vector3 back = { f->pos.x - cosf(f->yaw) * L * 0.5f, f->pos.y, f->pos.z + sinf(f->yaw) * L * 0.5f };
        Matrix tail = MatrixMultiply(MatrixMultiply(MatrixMultiply(MatrixScale(L * 0.45f, L * 0.35f, L * 0.05f), MatrixRotateZ(PI / 2)), MatrixRotateY(-f->yaw + wag)), MatrixTranslate(back.x, back.y, back.z));
        DrawM(&meshCone, tail, ColorBrightness(sp->col, -0.15f));
        Matrix fin = MatrixMultiply(MatrixMultiply(MatrixScale(L * 0.25f, L * 0.3f, L * 0.04f), MatrixRotateY(-f->yaw)), MatrixTranslate(f->pos.x, f->pos.y + L * 0.2f, f->pos.z));
        DrawM(&meshCone, fin, ColorBrightness(sp->col, -0.2f));
    }
}
// персонаж с процедурной анимацией (ходьба, наклоны, удочка с IK-подобным наведением)
static Vector3 rodTip, handPos;
static void DrawPlayer(void) {
    float yaw = playerYawSmooth; Vector3 fwd = { sinf(yaw), 0, cosf(yaw) }; Vector3 right = { cosf(yaw), 0, -sinf(yaw) };
    Vector3 base = playerPos; base.y += playerBob;
    float leanX = Clamp(playerLean.x, -0.25f, 0.25f), leanZ = Clamp(playerLean.z, -0.25f, 0.25f);
    Matrix bodyRot = MatrixMultiply(MatrixMultiply(MatrixRotateX(leanZ), MatrixRotateZ(-leanX)), MatrixRotateY(yaw));
    Color skin = { 240, 200, 170, 255 }, shirt = { 220, 120, 70, 255 }, pants = { 60, 80, 120, 255 }, hat = { 90, 140, 90, 255 }, boots = { 70, 50, 40, 255 };
    // ноги
    float lp = sinf(walkPhase) * 0.6f * moveAmount, rp = -lp;
    for (int k = 0; k < 2; k++) { float ph = k ? rp : lp; Vector3 hip = Vector3Add(base, Vector3Scale(right, k ? 0.14f : -0.14f)); hip.y += 0.85f;
        Matrix m = MatrixMultiply(MatrixMultiply(MatrixMultiply(MatrixTranslate(0, -0.42f, 0), MatrixScale(0.16f, 0.85f, 0.18f)), MatrixRotateX(ph)), MatrixMultiply(MatrixRotateY(yaw), MatrixTranslate(hip.x, hip.y, hip.z)));
        DrawM(&meshCube, m, pants);
        Vector3 foot = Vector3Add(hip, Vector3Scale(fwd, sinf(ph) * 0.85f)); foot.y = hip.y - cosf(ph) * 0.85f + 0.06f;
        DrawM(&meshCube, TRSy(foot, yaw, (Vector3){ 0.18f, 0.12f, 0.3f }), boots); }
    // туловище
    Vector3 torso = base; torso.y += 1.25f; Matrix tm = MatrixMultiply(MatrixMultiply(MatrixScale(0.46f, 0.7f, 0.3f), bodyRot), MatrixTranslate(torso.x, torso.y, torso.z)); DrawM(&meshCube, tm, shirt);
    // голова и шляпа
    Vector3 head = base; head.y += 1.85f; head = Vector3Add(head, Vector3Scale(right, -leanX * 0.3f)); head = Vector3Add(head, Vector3Scale(fwd, leanZ * 0.3f));
    DrawM(&meshSphere, TRSy(head, yaw, (Vector3){ 0.26f, 0.28f, 0.26f }), skin);
    DrawM(&meshCyl, TRSy((Vector3){ head.x, head.y + 0.2f, head.z }, yaw, (Vector3){ 0.5f, 0.04f, 0.5f }), hat);
    DrawM(&meshCyl, TRSy((Vector3){ head.x, head.y + 0.22f, head.z }, yaw, (Vector3){ 0.28f, 0.22f, 0.28f }), hat);
    // руки: левая машет при ходьбе, правая держит удочку (наводится на поплавок — упрощённый IK)
    Vector3 shoulderL = Vector3Add(torso, Vector3Scale(right, -0.3f)); shoulderL.y += 0.28f; Vector3 shoulderR = Vector3Add(torso, Vector3Scale(right, 0.3f)); shoulderR.y += 0.28f;
    float armSwing = -sinf(walkPhase) * 0.5f * moveAmount;
    Matrix la = MatrixMultiply(MatrixMultiply(MatrixMultiply(MatrixTranslate(0, -0.3f, 0), MatrixScale(0.13f, 0.62f, 0.13f)), MatrixRotateX(armSwing)), MatrixMultiply(MatrixRotateY(yaw), MatrixTranslate(shoulderL.x, shoulderL.y, shoulderL.z)));
    DrawM(&meshCube, la, shirt);
    float rodPitch; Vector3 rodDir;
    if (fstate == FS_IDLE) { rodPitch = 0.9f + armSwing * 0.3f; rodDir = Vector3Normalize(Vector3Add(Vector3Scale(fwd, cosf(rodPitch)), (Vector3){ 0, sinf(rodPitch), 0 })); }
    else if (fstate == FS_CHARGE) { float back = Clamp(castPower, 0, 1); rodPitch = 1.2f + back * 1.0f; rodDir = Vector3Normalize(Vector3Add(Vector3Scale(fwd, cosf(rodPitch)), (Vector3){ 0, sinf(rodPitch), 0 })); }
    else { Vector3 to = Vector3Subtract(bobberPos, shoulderR); to.y += 1.5f; float horiz = sqrtf(to.x * to.x + to.z * to.z); rodPitch = Clamp(atan2f(to.y + 1.0f, horiz) + 0.35f - rodBend * 0.6f, -0.2f, 1.3f);
        Vector3 hd = (horiz > 0.01f) ? Vector3Normalize((Vector3){ to.x, 0, to.z }) : fwd; rodDir = Vector3Normalize(Vector3Add(Vector3Scale(hd, cosf(rodPitch)), (Vector3){ 0, sinf(rodPitch), 0 })); }
    Vector3 elbow = Vector3Add(shoulderR, Vector3Scale(fwd, 0.25f)); elbow.y -= 0.2f; handPos = Vector3Add(elbow, Vector3Scale(fwd, 0.3f)); handPos.y += 0.05f;
    Vector3 ad = Vector3Normalize(Vector3Subtract(handPos, shoulderR)); Vector3 mid = Vector3Scale(Vector3Add(handPos, shoulderR), 0.5f); Vector3 ax = Vector3CrossProduct((Vector3){ 0, 1, 0 }, ad); float an = acosf(Clamp(ad.y, -1, 1));
    if (Vector3Length(ax) < 0.001f) ax = (Vector3){ 1, 0, 0 };
    DrawM(&meshCube, TRS(mid, Vector3Normalize(ax), an, (Vector3){ 0.13f, 0.62f, 0.13f }), shirt);
    DrawM(&meshSphere, TRSy(handPos, 0, (Vector3){ 0.09f, 0.09f, 0.09f }), skin);
    // удочка: 5 сегментов с изгибом
    Vector3 p = handPos; Vector3 d = rodDir; float seg = 0.5f; Vector3 bendAxis = Vector3Normalize(Vector3CrossProduct(d, (Vector3){ 0, 1, 0 })); if (Vector3Length(bendAxis) < 0.01f) bendAxis = right;
    for (int k = 0; k < 5; k++) {
        float bendK = rodBend * 0.18f * (k + 1); d = Vector3Normalize(Vector3RotateByAxisAngle(d, bendAxis, bendK * 0.5f)); d.y -= rodBend * 0.08f * (k + 1); d = Vector3Normalize(d);
        Vector3 q = Vector3Add(p, Vector3Scale(d, seg)); Vector3 m = Vector3Scale(Vector3Add(p, q), 0.5f);
        Vector3 ax2 = Vector3CrossProduct((Vector3){ 0, 1, 0 }, d); float an2 = acosf(Clamp(d.y, -1, 1)); if (Vector3Length(ax2) < 0.001f) ax2 = (Vector3){ 1, 0, 0 };
        DrawM(&meshCyl, TRS(m, Vector3Normalize(ax2), an2, (Vector3){ 0.035f - k * 0.005f, seg, 0.035f - k * 0.005f }), (Color){ 150, 110, 70, 255 }); p = q;
    }
    rodTip = p;
    // катушка
    DrawM(&meshCyl, TRS(Vector3Add(handPos, Vector3Scale(rodDir, 0.35f)), right, PI / 2, (Vector3){ 0.08f, 0.06f, 0.08f }), (Color){ 60, 60, 70, 255 });
}
static void DrawBobber(void) {
    if (fstate == FS_IDLE || fstate == FS_CHARGE || fstate == FS_CAUGHT) return;
    Vector3 b = bobberPos; float tilt = sinf(gTime * 3.0f) * 0.1f; if (fstate == FS_BITE) tilt = sinf(gTime * 20.0f) * 0.4f;
    DrawM(&meshSphere, TRS(b, (Vector3){ 1, 0, 0 }, tilt, (Vector3){ 0.09f, 0.12f, 0.09f }), (Color){ 230, 60, 50, 255 });
    DrawM(&meshCyl, TRS((Vector3){ b.x, b.y + 0.12f, b.z }, (Vector3){ 1, 0, 0 }, tilt, (Vector3){ 0.025f, 0.2f, 0.025f }), WHITE);
}
static void DrawScene(void) {
    SetSway(0.0f); SetEmissive(0.0f);
    DrawM(&meshTerrain, MatrixIdentity(), WHITE);
    DrawProps(); DrawFishes(); DrawPlayer(); DrawBobber();
    // костёр: пламя
    if (!shadowPass) { SetEmissive(1.2f); float fl = 0.8f + 0.2f * sinf(gTime * 17.0f) + 0.1f * sinf(gTime * 29.0f);
        DrawM(&meshCone, TRSy((Vector3){ firePos.x, firePos.y + 0.25f, firePos.z }, gTime, (Vector3){ 0.35f, 0.8f * fl, 0.35f }), (Color){ 255, 160, 50, 255 });
        DrawM(&meshCone, TRSy((Vector3){ firePos.x, firePos.y + 0.3f, firePos.z }, -gTime * 1.3f, (Vector3){ 0.22f, 1.0f * fl, 0.22f }), (Color){ 255, 230, 120, 255 }); SetEmissive(0.0f); }
}
// леска: кривая Безье от кончика удилища до поплавка
static void DrawLineRod(void) {
    if (fstate == FS_IDLE || fstate == FS_CHARGE || fstate == FS_CAUGHT) return;
    Vector3 a = rodTip, b = bobberPos; Vector3 mid = Vector3Scale(Vector3Add(a, b), 0.5f); float slack = (fstate == FS_FIGHT) ? 0.05f : 0.35f + 0.1f * sinf(gTime * 2); mid.y -= Vector3Distance(a, b) * slack;
    Vector3 prev = a; for (int i = 1; i <= 16; i++) { float t = i / 16.0f; Vector3 p = Vector3Add(Vector3Add(Vector3Scale(a, (1 - t) * (1 - t)), Vector3Scale(mid, 2 * (1 - t) * t)), Vector3Scale(b, t * t)); DrawLine3D(prev, p, (Color){ 240, 240, 240, 255 }); prev = p; }
}
static void DrawParticles(void) {
    rlDisableDepthMask();
    for (int i = 0; i < partCount; i++) { Particle *q = &parts[i]; float u = q->life / q->maxLife; Color c = q->col; c.a = (unsigned char)(c.a * Clamp(u * 2.0f, 0, 1) * ((q->type == 1) ? (0.5f + 0.5f * sinf(gTime * 6 + q->pos.x * 10)) : 1.0f)); DrawBillboard(cam, particleTex, q->pos, q->size * ((q->type == 2 || q->type == 4) ? 1.0f : 1.0f), c); }
}

// ------------------------------------------------------------------------------------
//  Инициализация рендера
// ------------------------------------------------------------------------------------
static RenderTexture2D LoadShadowRT(int w, int h) {
    RenderTexture2D t = { 0 }; t.id = rlLoadFramebuffer(); t.texture.width = w; t.texture.height = h;
    if (t.id > 0) { rlEnableFramebuffer(t.id); t.depth.id = rlLoadTextureDepth(w, h, false); t.depth.width = w; t.depth.height = h; t.depth.format = 19; t.depth.mipmaps = 1;
        rlFramebufferAttach(t.id, t.depth.id, RL_ATTACHMENT_DEPTH, RL_ATTACHMENT_TEXTURE2D, 0); rlDisableFramebuffer(); }
    return t;
}
static void ApplyQuality(void) {
    int res = (S.quality == 0) ? 1024 : (S.quality == 1) ? 2048 : 4096; if (res != shadowRes || shadowRT.id == 0) { if (shadowRT.id) { rlUnloadFramebuffer(shadowRT.id); rlUnloadTexture(shadowRT.depth.id); } shadowRes = res; shadowRT = LoadShadowRT(res, res); }
    SetShaderValue(shLit, locLitShadowRes, &shadowRes, SHADER_UNIFORM_INT);
    float bloom = (S.quality == 0) ? 0.4f : 0.9f, dof = (S.quality == 0) ? 0.0f : (S.quality == 1) ? 0.6f : 1.0f; SetShaderValue(shPost, locPBloom, &bloom, SHADER_UNIFORM_FLOAT); SetShaderValue(shPost, locPDof, &dof, SHADER_UNIFORM_FLOAT);
}
static void ResizeTargets(void) {
    screenW = GetScreenWidth(); screenH = GetScreenHeight(); if (screenW < 320) screenW = 320; if (screenH < 200) screenH = 200;
    if (sceneRT.id) UnloadRenderTexture(sceneRT); sceneRT = LoadRenderTexture(screenW, screenH); SetTextureFilter(sceneRT.texture, TEXTURE_FILTER_BILINEAR);
    float res[2] = { (float)screenW, (float)screenH }; SetShaderValue(shPost, locPRes, res, SHADER_UNIFORM_VEC2); uiScale = Clamp(screenH / 720.0f, 0.6f, 2.0f);
}
static void InitRender(void) {
    shLit = LoadShaderFromMemory(LIT_VS, LIT_FS); shWater = LoadShaderFromMemory(WATER_VS, WATER_FS); shSky = LoadShaderFromMemory(SKY_VS, SKY_FS); shShadow = LoadShaderFromMemory(SHADOW_VS, SHADOW_FS); shPost = LoadShaderFromMemory(0, POST_FS);
    shLine = LoadShaderFromMemory(0, LINE_FS);
    shLit.locs[SHADER_LOC_MATRIX_MODEL] = GetShaderLocation(shLit, "matModel"); shLit.locs[SHADER_LOC_MATRIX_NORMAL] = GetShaderLocation(shLit, "matNormal"); shLit.locs[SHADER_LOC_MATRIX_VIEW] = GetShaderLocation(shLit, "matView"); shLit.locs[SHADER_LOC_MATRIX_PROJECTION] = GetShaderLocation(shLit, "matProjection");
    shLit.locs[SHADER_LOC_VECTOR_VIEW] = GetShaderLocation(shLit, "viewPos");
    locLitLightVP = GetShaderLocation(shLit, "lightVP"); locLitSun = GetShaderLocation(shLit, "sunDir"); locLitSunCol = GetShaderLocation(shLit, "sunColor"); locLitAmb = GetShaderLocation(shLit, "ambientColor"); locLitFog = GetShaderLocation(shLit, "fogColor");
    locLitView = GetShaderLocation(shLit, "viewPos"); locLitTime = GetShaderLocation(shLit, "time"); locLitFogD = GetShaderLocation(shLit, "fogDensity"); locLitWater = GetShaderLocation(shLit, "waterLevel"); locLitShadowMap = GetShaderLocation(shLit, "shadowMap"); locLitShadowRes = GetShaderLocation(shLit, "shadowRes");
    locLitSway = GetShaderLocation(shLit, "sway"); locLitEmissive = GetShaderLocation(shLit, "emissive");
    shWater.locs[SHADER_LOC_MATRIX_MODEL] = GetShaderLocation(shWater, "matModel"); shWater.locs[SHADER_LOC_MATRIX_VIEW] = GetShaderLocation(shWater, "matView"); shWater.locs[SHADER_LOC_MATRIX_PROJECTION] = GetShaderLocation(shWater, "matProjection");
    locWSun = GetShaderLocation(shWater, "sunDir"); locWSunCol = GetShaderLocation(shWater, "sunColor"); locWAmb = GetShaderLocation(shWater, "ambientColor"); locWFog = GetShaderLocation(shWater, "fogColor"); locWView = GetShaderLocation(shWater, "viewPos"); locWTime = GetShaderLocation(shWater, "time");
    locWFogD = GetShaderLocation(shWater, "fogDensity"); locWWorld = GetShaderLocation(shWater, "worldSize"); locWSky = GetShaderLocation(shWater, "skyColor"); locWHor = GetShaderLocation(shWater, "horizonColor"); locWRip = GetShaderLocation(shWater, "ripples"); locWRipN = GetShaderLocation(shWater, "rippleCount");
    locSSun = GetShaderLocation(shSky, "sunDir"); locSSunCol = GetShaderLocation(shSky, "sunColor"); locSSky = GetShaderLocation(shSky, "skyColor"); locSHor = GetShaderLocation(shSky, "horizonColor"); locSTime = GetShaderLocation(shSky, "time"); locSNight = GetShaderLocation(shSky, "night");
    locPRes = GetShaderLocation(shPost, "resolution"); locPTime = GetShaderLocation(shPost, "time"); locPBloom = GetShaderLocation(shPost, "bloomStrength"); locPDof = GetShaderLocation(shPost, "dofStrength"); locPFocus = GetShaderLocation(shPost, "focusDepth"); locPVig = GetShaderLocation(shPost, "vignette"); locPUnder = GetShaderLocation(shPost, "underwater"); locPFlash = GetShaderLocation(shPost, "flash");
    float ws = WORLD_SIZE; SetShaderValue(shWater, locWWorld, &ws, SHADER_UNIFORM_FLOAT); float zero = 0; SetShaderValue(shLit, locLitWater, &zero, SHADER_UNIFORM_FLOAT); float vig = 0.35f; SetShaderValue(shPost, locPVig, &vig, SHADER_UNIFORM_FLOAT);
    // карта высот для воды
    float *hm = (float *)MemAlloc(HM_RES * HM_RES * sizeof(float));
    for (int j = 0; j < HM_RES; j++) for (int i = 0; i < HM_RES; i++) { float x = -WORLD_SIZE / 2 + WORLD_SIZE * i / (HM_RES - 1), z = -WORLD_SIZE / 2 + WORLD_SIZE * j / (HM_RES - 1); hm[j * HM_RES + i] = TerrainHeight(x, z); }
    Image him = { hm, HM_RES, HM_RES, 1, PIXELFORMAT_UNCOMPRESSED_R32 }; hmTex = LoadTextureFromImage(him); SetTextureFilter(hmTex, TEXTURE_FILTER_BILINEAR); MemFree(hm);
    Image wi = GenImageColor(4, 4, WHITE); whiteTex = LoadTextureFromImage(wi); UnloadImage(wi);
    Image pi = GenImageGradientRadial(64, 64, 0.0f, WHITE, (Color){ 255, 255, 255, 0 }); particleTex = LoadTextureFromImage(pi); UnloadImage(pi); SetTextureFilter(particleTex, TEXTURE_FILTER_BILINEAR);
    // меши
    meshTerrain = BuildTerrainMesh(); meshWater = GenMeshPlane(420, 420, 210, 210); meshSky = GenMeshSphere(1.0f, 12, 16);
    meshCube = GenMeshCube(1, 1, 1); meshSphere = GenMeshSphere(0.5f, 8, 10); meshCone = GenMeshCone(0.5f, 1.0f, 8); meshCyl = GenMeshCylinder(0.5f, 1.0f, 8); meshQuad = GenMeshPlane(1, 1, 1, 1);
    meshFishBody = GenMeshSphere(0.5f, 8, 10); meshTail = GenMeshCone(0.5f, 1.0f, 4);
    matLit = LoadMaterialDefault(); matLit.shader = shLit; matLit.maps[MATERIAL_MAP_DIFFUSE].texture = whiteTex;
    matWater = LoadMaterialDefault(); matWater.shader = shWater; matWater.maps[MATERIAL_MAP_DIFFUSE].texture = hmTex;
    matSky = LoadMaterialDefault(); matSky.shader = shSky; matShadow = LoadMaterialDefault(); matShadow.shader = shShadow;
    shadowRT.id = 0; sceneRT.id = 0; ApplyQuality(); ResizeTargets();
}

// ------------------------------------------------------------------------------------
//  Игровая логика
// ------------------------------------------------------------------------------------
static void ResetFishing(void) { fstate = FS_IDLE; hookedFish = -1; tension = 0; rodBendTarget = 0; fishNearBobber = -1; for (int i = 0; i < fishCount; i++) fish[i].targetBobber = 0; }
static void NewGame(void) {
    int keepSettings[4] = { S.fullscreen, S.vsync, S.quality, S.showTutorial }; float vols[3] = { S.musicVol, S.sfxVol, S.sensitivity }; int hs = S.highScore; int ach[NUM_ACH]; for (int i = 0; i < NUM_ACH; i++) ach[i] = ACH[i].unlocked;
    DefaultSave(); S.fullscreen = keepSettings[0]; S.vsync = keepSettings[1]; S.quality = keepSettings[2]; S.showTutorial = keepSettings[3]; S.musicVol = vols[0]; S.sfxVol = vols[1]; S.sensitivity = vols[2]; S.highScore = hs; for (int i = 0; i < NUM_ACH; i++) { ACH[i].unlocked = ach[i]; S.achUnlocked[i] = ach[i]; }
    BuildWorld(); Vector3 dir = { cosf(pierYaw), 0, sinf(pierYaw) }; playerPos = Vector3Add(pierPos, Vector3Scale(dir, -6.0f)); playerPos.y = TerrainHeight(playerPos.x, playerPos.z); playerYaw = pierYaw + PI / 2 - atan2f(dir.z, dir.x) + atan2f(dir.x, dir.z); playerYaw = atan2f(dir.x, dir.z); playerYawSmooth = playerYaw;
    camYaw = -playerYaw + PI; camYaw = atan2f(-dir.x, -dir.z) ; camPitch = 0.3f; camTargetSmooth = playerPos; camPosSmooth = Vector3Add(playerPos, (Vector3){ 0, 3, -6 });
    ResetFishing(); partCount = 0; tutorialStep = S.showTutorial ? 1 : 0; tutorialTimer = 0; waitTime = 0; S.victory = 0;
}
static void ContinueGame(void) {
    BuildWorld(); if (S.legendUnlocked) PlaceLegend();
    if (S.playerX < 1e8f) { playerPos = (Vector3){ S.playerX, 0, S.playerZ }; playerYaw = S.playerYaw; } else { Vector3 dir = { cosf(pierYaw), 0, sinf(pierYaw) }; playerPos = Vector3Add(pierPos, Vector3Scale(dir, -6.0f)); playerYaw = atan2f(dir.x, dir.z); }
    playerPos.y = TerrainHeight(playerPos.x, playerPos.z); playerYawSmooth = playerYaw; camYaw = playerYaw + PI; camPitch = 0.3f; camTargetSmooth = playerPos; camPosSmooth = playerPos; ResetFishing(); partCount = 0; tutorialStep = 0;
}
static int CountSpecies(void) { int n = 0; for (int i = 0; i < NUM_SPECIES - 1; i++) if (S.caughtBySpecies[i] > 0) n++; return n; }
static void CheckAchievements(void) {
    if (S.totalCaught >= 1) Unlock(0); if (CountSpecies() >= 5) Unlock(1); if (CountSpecies() >= 8) Unlock(2); if (S.bestWeight > 3.0f) Unlock(4); if (S.bestStreak >= 5) Unlock(5); if (S.score >= 1000) Unlock(6); if (S.days >= 3) Unlock(7); if (S.eaten >= 5) Unlock(8);
    int sv = 0; for (int i = 0; i < 4; i++) sv += S.spotsVisited[i]; if (sv == 4) Unlock(9); if (S.casts >= 50) Unlock(10); if (S.caughtBySpecies[NUM_SPECIES - 1] > 0) Unlock(11);
    if (!S.legendUnlocked && CountSpecies() >= 6) { S.legendUnlocked = 1; PlaceLegend(); Msg("Старики говорят: ночью у пирса, на глубине, ходит Старый Сом...", 7); }
}
static const char *CurrentTask(void) {
    static char b[256];
    if (S.totalCaught == 0) return "Задание: поймайте первую рыбу у пирса";
    if (CountSpecies() < 6) { snprintf(b, sizeof(b), "Задание: откройте 6 видов рыб (%d/6)", CountSpecies()); return b; }
    if (S.caughtBySpecies[NUM_SPECIES - 1] == 0) return "Задание: поймайте Старого Сома ночью на глубине у пирса";
    return "Остров ваш. Ловите в своё удовольствие!";
}

static void UpdateFish(void) {
    for (int i = 0; i < fishCount; i++) {
        Fish *f = &fish[i]; if (hookedFish == i) continue; const Species *sp = &SPECIES[f->species];
        float speed = 0.8f + f->size * 0.1f; if (f->species == NUM_SPECIES - 1) speed = 0.7f;
        f->tailPhase += dt * (4.0f + speed * 2.0f); f->cooldown -= dt;
        int bobberActive = (fstate == FS_WAIT) && bobberInWater; float toB = bobberActive ? Vector2Distance((Vector2){ f->pos.x, f->pos.z }, (Vector2){ bobberPos.x, bobberPos.z }) : 1e9f;
        int nightOK = sp->night ? (nightAmount > 0.4f) : 1; if (sp->night && nightAmount < 0.4f && frand() < 0.3f) nightOK = 1;
        if (!f->targetBobber && bobberActive && toB < 14.0f && f->cooldown <= 0 && fishNearBobber < 0 && nightOK && frand() < dt * (0.25f + 0.15f * (float)waitTime / 10.0f)) { f->targetBobber = 1; fishNearBobber = i; f->nibbles = GetRandomValue(1, 3); f->nibbleTimer = frange(1.0f, 2.5f); }
        if (f->targetBobber) {
            if (!bobberActive) { f->targetBobber = 0; fishNearBobber = -1; continue; }
            float ty = atan2f(bobberPos.x - f->pos.x, bobberPos.z - f->pos.z); float dy = ty - f->yaw; while (dy > PI) dy -= 2 * PI; while (dy < -PI) dy += 2 * PI; f->yaw += Clamp(dy, -2.0f * dt, 2.0f * dt);
            if (toB > 0.9f) { f->pos.x += sinf(f->yaw) * speed * dt; f->pos.z += cosf(f->yaw) * speed * dt; }
            else { f->nibbleTimer -= dt; if (f->nibbleTimer <= 0) { if (f->nibbles > 0) { f->nibbles--; f->nibbleTimer = frange(0.8f, 2.0f); nibbleFlash = 0.4f; AddRipple((Vector2){ bobberPos.x, bobberPos.z }, 0.35f); PlaySpatial(sndPlop, bobberPos, 0.5f); bobberVel.y -= 0.8f; }
                    else { fstate = FS_BITE; fTimer = 0; hookedFish = i; AddRipple((Vector2){ bobberPos.x, bobberPos.z }, 0.8f); Splash(bobberPos, 0.3f); PlaySpatial(sndBite, bobberPos, 1.0f); bobberVel.y -= 2.5f; camShake = 0.15f; } } }
            float depthTarget = TerrainHeight(f->pos.x, f->pos.z) * 0.5f - 0.2f; f->pos.y = damp(f->pos.y, fminf(depthTarget, -0.4f), 2.0f, dt);
        } else {
            f->wander += frange(-1.5f, 1.5f) * dt; f->yaw += f->wander * dt; f->wander *= 1.0f - dt * 0.5f;
            float nx = f->pos.x + sinf(f->yaw) * speed * dt, nz = f->pos.z + cosf(f->yaw) * speed * dt; float h = TerrainHeight(nx, nz);
            if (h < -0.9f && h > -14.0f && fabsf(nx) < 105 && fabsf(nz) < 105) { f->pos.x = nx; f->pos.z = nz; } else { f->yaw += PI * 0.5f + frand(); }
            float depthTarget = TerrainHeight(f->pos.x, f->pos.z) * 0.55f - 0.3f + sinf(gTime * 0.5f + i) * 0.3f; f->pos.y = damp(f->pos.y, fminf(depthTarget, -0.5f), 1.0f, dt);
            // прыжок рыбы (редко)
            if (frand() < dt * 0.004f && f->pos.y > -3.0f) { Vector3 sp2 = { f->pos.x, WaveHeight(f->pos.x, f->pos.z, gTime), f->pos.z }; Splash(sp2, 0.4f); AddRipple((Vector2){ f->pos.x, f->pos.z }, 0.6f); PlaySpatial(sndSplash, sp2, 0.6f); }
        }
    }
}

static void StartFight(void) {
    fstate = FS_FIGHT; tension = 0.3f; Fish *f = &fish[hookedFish]; lineLen = Vector2Distance((Vector2){ playerPos.x, playerPos.z }, (Vector2){ bobberPos.x, bobberPos.z }); fishPull = 0.2f; fishPullTarget = 0.4f; fTimer = 0; camShake = 0.25f; Splash(bobberPos, 0.6f); PlaySpatial(sndSplash, bobberPos, 1.0f);
    (void)f; Msg("Удерживайте ЛКМ, чтобы подтягивать. Следите за натяжением!", 4);
    if (tutorialStep == 4) { tutorialStep = 5; }
}
static void FinishCatch(void) {
    Fish *f = &fish[hookedFish]; caughtSpecies = f->species; caughtSize = f->size; const Species *sp = &SPECIES[caughtSpecies];
    int pts = (int)(sp->score * (0.6f + f->size / sp->maxSize)); if (nightAmount > 0.5f) pts = (int)(pts * 1.25f);
    S.score += pts; S.totalCaught++; S.caughtBySpecies[caughtSpecies]++; S.inventory[caughtSpecies]++; S.streak++; if (S.streak > S.bestStreak) S.bestStreak = S.streak; if (f->size > S.bestWeight) S.bestWeight = f->size;
    if (nightAmount > 0.5f) Unlock(3);
    char b[256]; snprintf(b, sizeof(b), "Поймали: %s — %.1f кг  (+%d очков)", sp->name, f->size, pts); Toast(b); PlayUI(sndCatch, 0.9f); flashAmount = 0.35f; camShake = 0.2f;
    fstate = FS_CAUGHT; fTimer = 0;
    // рыба уплывает/пересоздаётся в другом месте
    if (caughtSpecies == NUM_SPECIES - 1) { S.victory = 1; } else { for (int k = 0; k < 200; k++) { float x = frange(-100, 100), z = frange(-100, 100); float h = TerrainHeight(x, z); if (h < -1.0f && h > -12) { f->pos = (Vector3){ x, h * 0.5f, z }; f->size = frange(sp->minSize, sp->maxSize); break; } } }
    f->targetBobber = 0; fishNearBobber = -1; hookedFish = -1; CheckAchievements();
    if (tutorialStep == 5) { tutorialStep = 6; tutorialTimer = 0; }
}
static void LoseFish(const char *why) { Fish *f = &fish[hookedFish]; f->targetBobber = 0; f->cooldown = 15; fishNearBobber = -1; hookedFish = -1; S.streak = 0; fstate = FS_LOST; fTimer = 0; Msg(why, 3); PlayUI(sndSnap, 0.8f); camShake = 0.1f; }

static void UpdateFishing(void) {
    rodBend = damp(rodBend, rodBendTarget, 8.0f, dt); nibbleFlash = fmaxf(0, nibbleFlash - dt);
    switch (fstate) {
    case FS_IDLE: {
        rodBendTarget = 0;
        if (IsMouseButtonPressed(MOUSE_LEFT_BUTTON) && state == ST_PLAY) { fstate = FS_CHARGE; castPower = 0; }
    } break;
    case FS_CHARGE: {
        castPower += dt * 0.8f; if (castPower > 1.2f) castPower = 1.2f; rodBendTarget = -0.3f * Clamp(castPower, 0, 1);
        if (IsMouseButtonReleased(MOUSE_LEFT_BUTTON)) {
            float p = Clamp(castPower, 0.15f, 1.0f); Vector3 fwd = { sinf(playerYaw), 0, cosf(playerYaw) }; Vector3 camFwd = Vector3Normalize(Vector3Subtract(cam.target, cam.position)); camFwd.y = 0; if (Vector3Length(camFwd) > 0.01f) fwd = Vector3Normalize(camFwd);
            playerYaw = atan2f(fwd.x, fwd.z); bobberPos = rodTip; bobberVel = Vector3Add(Vector3Scale(fwd, 6.0f + p * 14.0f), (Vector3){ 0, 4.0f + p * 5.0f, 0 }); fstate = FS_FLY; bobberInWater = 0; S.casts++; S.energy -= 1.5f; rodBendTarget = 0.6f; PlayUI(sndCast, 0.6f); camShake = 0.05f;
            if (tutorialStep == 2) { tutorialStep = 3; tutorialTimer = 0; }
        }
    } break;
    case FS_FLY: {
        bobberVel.y -= 9.8f * dt; bobberPos = Vector3Add(bobberPos, Vector3Scale(bobberVel, dt)); rodBendTarget = damp(rodBendTarget, 0.1f, 4, dt);
        float wh = WaveHeight(bobberPos.x, bobberPos.z, gTime); float th = TerrainHeight(bobberPos.x, bobberPos.z);
        if (bobberPos.y <= th + 0.1f) { // упал на землю
            fstate = FS_LOST; fTimer = 0; Msg("Поплавок упал на берег. Забросьте снова.", 2.5f); }
        else if (bobberPos.y <= wh && th < -0.4f) { bobberPos.y = wh; bobberVel = Vector3Scale(bobberVel, 0.1f); bobberInWater = 1; fstate = FS_WAIT; waitTime = 0; Splash(bobberPos, 0.5f); AddRipple((Vector2){ bobberPos.x, bobberPos.z }, 0.8f); PlaySpatial(sndPlop, bobberPos, 1.0f); if (tutorialStep == 3) { tutorialStep = 4; tutorialTimer = 0; } }
        else if (bobberPos.y <= wh) { fstate = FS_LOST; fTimer = 0; Msg("Слишком мелко — рыба здесь не водится.", 2.5f); }
        if (Vector3Distance(bobberPos, playerPos) > 60) { fstate = FS_LOST; fTimer = 0; }
    } break;
    case FS_WAIT: {
        waitTime += dt; float wh = WaveHeight(bobberPos.x, bobberPos.z, gTime); // плавучесть: пружина к поверхности
        float k = 40.0f, c = 6.0f; bobberVel.y += ((wh - bobberPos.y) * k - bobberVel.y * c) * dt; bobberPos.y += bobberVel.y * dt;
        bobberPos.x += sinf(gTime * 0.3f) * 0.05f * dt; rodBendTarget = 0.08f + 0.02f * sinf(gTime * 2.0f);
        if (fabsf(bobberVel.y) > 0.6f && frand() < dt * 3.0f) AddRipple((Vector2){ bobberPos.x, bobberPos.z }, 0.15f);
        if (IsMouseButtonPressed(MOUSE_RIGHT_BUTTON)) { if (fishNearBobber >= 0) { fish[fishNearBobber].targetBobber = 0; fish[fishNearBobber].cooldown = 8; fishNearBobber = -1; } fstate = FS_LOST; fTimer = 0; }
    } break;
    case FS_BITE: {
        fTimer += dt; rodBendTarget = 0.35f + sinf(gTime * 25.0f) * 0.1f; float wh = WaveHeight(bobberPos.x, bobberPos.z, gTime); bobberPos.y = wh - 0.25f + sinf(gTime * 18.0f) * 0.1f;
        if (frand() < dt * 10.0f) AddRipple((Vector2){ bobberPos.x, bobberPos.z }, 0.25f);
        if (IsMouseButtonPressed(MOUSE_LEFT_BUTTON)) { StartFight(); }
        else if (fTimer > 1.4f) { LoseFish("Рыба сорвалась — подсекайте быстрее (ЛКМ)!"); }
    } break;
    case FS_FIGHT: {
        fTimer += dt; Fish *f = &fish[hookedFish]; const Species *sp = &SPECIES[f->species]; float str = sp->strength * (0.6f + 0.4f * f->size / sp->maxSize);
        if (frand() < dt * 1.2f) fishPullTarget = frange(0.1f, 1.0f) * str; fishPull = damp(fishPull, fishPullTarget, 3.0f, dt);
        int reeling = IsMouseButtonDown(MOUSE_LEFT_BUTTON);
        float target = fishPull * 0.9f + (reeling ? 0.45f : -0.2f); tension = Clamp(tension + (target - tension) * dt * 3.0f, 0, 1.3f);
        if (reeling) { lineLen -= dt * (3.0f - fishPull * 1.5f); reelTimer -= dt; if (reelTimer <= 0) { PlayUI(sndReel, 0.35f); reelTimer = 0.4f; } } else lineLen += dt * fishPull * 1.5f;
        rodBendTarget = 0.3f + tension * 0.9f; camShake = fmaxf(camShake, tension * 0.08f);
        // рыба мечется
        float ang = gTime * 1.5f + hookedFish; Vector2 toP = Vector2Subtract((Vector2){ playerPos.x, playerPos.z }, (Vector2){ bobberPos.x, bobberPos.z }); if (Vector2Length(toP) < 0.01f) toP = (Vector2){ 0, 1 }; toP = Vector2Normalize(toP);
        Vector2 side = { -toP.y, toP.x }; Vector2 want = Vector2Add(Vector2Scale(toP, 1.0f), Vector2Scale(side, sinf(ang) * 1.2f * fishPull));
        Vector2 bp = { playerPos.x, playerPos.z }; bp = Vector2Add(bp, Vector2Scale(toP, -lineLen)); Vector2 cur = { bobberPos.x, bobberPos.z }; cur = Vector2Lerp(cur, Vector2Add(bp, Vector2Scale(side, sinf(ang) * 2.0f * fishPull)), 1 - expf(-3 * dt));
        bobberPos.x = cur.x; bobberPos.z = cur.y; float wh = WaveHeight(bobberPos.x, bobberPos.z, gTime); bobberPos.y = wh - 0.2f - fishPull * 0.4f;
        f->pos = (Vector3){ bobberPos.x - toP.x * 0.5f, bobberPos.y - 0.4f, bobberPos.z - toP.y * 0.5f }; f->yaw = atan2f(-toP.x, -toP.y) + sinf(ang * 2) * 0.5f; f->tailPhase += dt * 20.0f;
        if (frand() < dt * 6.0f * (0.3f + fishPull)) { AddRipple((Vector2){ bobberPos.x, bobberPos.z }, 0.3f + fishPull * 0.4f); if (frand() < 0.3f) { Splash(bobberPos, fishPull * 0.5f); if (frand() < 0.4f) PlaySpatial(sndSplash, bobberPos, 0.4f); } }
        (void)want;
        if (tension >= 1.0f) { fTimer += dt * 3.0f; if (frand() < dt * 2.0f * (tension - 0.9f) * 10.0f) { LoseFish("Леска оборвалась! Отпускайте ЛКМ, когда натяжение в красной зоне."); break; } }
        if (lineLen > 45.0f) { LoseFish("Рыба ушла слишком далеко и сорвалась."); break; }
        if (TerrainHeight(bobberPos.x, bobberPos.z) > -0.3f || lineLen < 2.5f) { FinishCatch(); }
    } break;
    case FS_CAUGHT: { fTimer += dt; rodBendTarget = 0; if (fTimer > 2.2f) { fstate = FS_IDLE; if (S.victory && state == ST_PLAY) { state = ST_VICTORY; SaveGame(); EnableCursor(); } } } break;
    case FS_LOST: { fTimer += dt; rodBendTarget = 0; if (bobberInWater && fTimer < 0.6f) { Vector3 to = Vector3Subtract(rodTip, bobberPos); bobberPos = Vector3Add(bobberPos, Vector3Scale(to, dt * 3.0f)); } if (fTimer > 0.7f) fstate = FS_IDLE; } break;
    }
    if (fstate == FS_WAIT || fstate == FS_BITE || fstate == FS_FIGHT) { if (Vector3Distance(playerPos, bobberPos) > 45.0f) { if (hookedFish >= 0) LoseFish("Леска порвалась — вы ушли слишком далеко."); else { fstate = FS_LOST; fTimer = 0; } } }
}

static void UpdatePlayer(void) {
    // камера мышью
    Vector2 md = GetMouseDelta(); camYaw -= md.x * 0.0028f * S.sensitivity; camPitch = Clamp(camPitch + md.y * 0.0022f * S.sensitivity, -0.35f, 1.2f);
    camDist = Clamp(camDist - GetMouseWheelMove() * 0.6f, 3.0f, 12.0f);
    Vector3 camFwd = { -sinf(camYaw), 0, -cosf(camYaw) }; Vector3 camRight = { cosf(camYaw), 0, -sinf(camYaw) };
    Vector3 mv = { 0 }; int canMove = (fstate == FS_IDLE || fstate == FS_LOST || fstate == FS_CAUGHT || fstate == FS_FLY || fstate == FS_WAIT);
    if (canMove) { if (IsKeyDown(KEY_W)) mv = Vector3Add(mv, camFwd); if (IsKeyDown(KEY_S)) mv = Vector3Subtract(mv, camFwd); if (IsKeyDown(KEY_D)) mv = Vector3Add(mv, camRight); if (IsKeyDown(KEY_A)) mv = Vector3Subtract(mv, camRight); }
    int running = IsKeyDown(KEY_LEFT_SHIFT) && S.energy > 5; float speed = running ? 7.5f : 4.2f; if (Vector3Length(mv) > 0.01f) { mv = Vector3Normalize(mv); playerYaw = atan2f(mv.x, mv.z); } else speed = 0;
    if (fstate == FS_WAIT && Vector3Length(mv) > 0.01f) speed *= 0.5f;
    Vector3 targetVel = Vector3Scale(mv, speed); playerVel = damp3(playerVel, targetVel, 10.0f, dt);
    Vector3 np = Vector3Add(playerPos, Vector3Scale(playerVel, dt)); float nh = TerrainHeight(np.x, np.z); float wh = WaveHeight(np.x, np.z, gTime);
    int onPier = 0; { Vector3 dir = { cosf(pierYaw), 0, sinf(pierYaw) }; Vector3 rel = Vector3Subtract(np, pierPos); float along = Vector3DotProduct(rel, dir), across = Vector3DotProduct(rel, (Vector3){ -dir.z, 0, dir.x }); if (along > -3.6f && along < 11.0f && fabsf(across) < 1.2f) onPier = 1; }
    float floorY = onPier ? fmaxf(nh, 0.82f) : nh;
    if (floorY > wh - 1.1f || onPier) { if (fabsf(np.x) < 108 && fabsf(np.z) < 108) playerPos = np; } else playerVel = Vector3Scale(playerVel, 0.2f);
    float ph = onPier ? floorY : TerrainHeight(playerPos.x, playerPos.z); playerPos.y = damp(playerPos.y, ph, 15.0f, dt);
    float spd = Vector3Length(playerVel); moveAmount = damp(moveAmount, Clamp(spd / 4.0f, 0, 1), 8, dt); walkPhase += dt * spd * 2.2f; playerBob = fabsf(sinf(walkPhase)) * 0.06f * moveAmount;
    // наклон тела в сторону ускорения (пружина)
    Vector3 accel = Vector3Scale(Vector3Subtract(targetVel, playerVel), 0.04f); Vector3 fwd = { sinf(playerYaw), 0, cosf(playerYaw) }; Vector3 right = { cosf(playerYaw), 0, -sinf(playerYaw) };
    Vector3 leanT = { Vector3DotProduct(accel, right) + spd * 0.0f, 0, Vector3DotProduct(accel, fwd) + spd * 0.025f }; playerLean = damp3(playerLean, leanT, 6.0f, dt);
    float dy = playerYaw - playerYawSmooth; while (dy > PI) dy -= 2 * PI; while (dy < -PI) dy += 2 * PI; playerYawSmooth += dy * (1 - expf(-12 * dt));
    // шаги и брызги при ходьбе по мелководью
    stepTimer -= dt * spd; if (stepTimer <= 0 && spd > 0.5f) { stepTimer = 2.2f; int wet = (!onPier && playerPos.y < wh + 0.1f); if (wet) { Vector3 sp = playerPos; sp.y = wh; Splash(sp, 0.15f); AddRipple((Vector2){ playerPos.x, playerPos.z }, 0.3f); PlaySpatial(sndPlop, playerPos, 0.3f); } else PlaySpatial(sndStep, playerPos, onPier ? 0.5f : 0.3f); }
    // энергия
    S.energy -= dt * (running && spd > 1 ? 0.9f : 0.12f); if (S.energy <= 0) { S.energy = 0; state = ST_GAMEOVER; SaveGame(); EnableCursor(); }
    // смотровые точки
    for (int i = 0; i < 4; i++) if (!S.spotsVisited[i] && Vector3Distance(playerPos, spots[i]) < 3.0f) { S.spotsVisited[i] = 1; char b[128]; snprintf(b, sizeof(b), "Открыта смотровая точка: %s (+50 очков)", spotNames[i]); Toast(b); S.score += 50; PlayUI(sndAch, 0.5f); CheckAchievements(); }
    // еда / отдых
    int nearFire = Vector3Distance(playerPos, firePos) < 3.5f;
    if (IsKeyPressed(KEY_F)) { int any = -1; for (int i = 0; i < NUM_SPECIES - 1; i++) if (S.inventory[i] > 0) { any = i; break; }
        if (any < 0) Msg("В сумке нет рыбы. Поймайте что-нибудь!", 2.5f); else if (!nearFire) Msg("Рыбу нужно готовить у костра (рядом с пирсом).", 2.5f);
        else { S.inventory[any]--; S.eaten++; S.energy = fminf(100, S.energy + 25 + SPECIES[any].score * 0.3f); char b[128]; snprintf(b, sizeof(b), "Вы приготовили и съели: %s. Энергия восстановлена!", SPECIES[any].name); Msg(b, 3); PlayUI(sndEat, 0.8f); for (int k = 0; k < 10; k++) Emit((Vector3){ firePos.x, firePos.y + 0.5f, firePos.z }, (Vector3){ frange(-1, 1), frange(1, 3), frange(-1, 1) }, 1.0f, 0.1f, (Color){ 255, 200, 80, 255 }, 3); CheckAchievements(); } }
    if (IsKeyPressed(KEY_R)) { if (!nearFire) Msg("Отдыхать можно только у костра.", 2.5f); else { if (S.timeOfDay > 18.0f || S.timeOfDay < 6.0f) { S.timeOfDay = 6.5f; S.days++; Msg("Вы переночевали у костра. Наступило утро.", 3); } else { S.timeOfDay = 20.5f; Msg("Вы подремали у костра до вечера.", 3); } S.energy = fminf(100, S.energy + 40); flashAmount = 0.8f; CheckAchievements(); } }
}

static void UpdateGameCamera(void) {
    Vector3 target = playerPos; target.y += 1.6f;
    if (fstate == FS_FIGHT || fstate == FS_BITE || fstate == FS_WAIT) target = Vector3Lerp(target, bobberPos, 0.3f);
    camTargetSmooth = damp3(camTargetSmooth, target, 7.0f, dt);
    Vector3 off = { sinf(camYaw) * cosf(camPitch), sinf(camPitch), cosf(camYaw) * cosf(camPitch) }; Vector3 want = Vector3Add(camTargetSmooth, Vector3Scale(off, camDist));
    // столкновение с рельефом
    float th = TerrainHeight(want.x, want.z) + 0.6f; if (want.y < th) want.y = th;
    camPosSmooth = damp3(camPosSmooth, want, 9.0f, dt);
    camShake = damp(camShake, 0, 6.0f, dt); Vector3 sh = { sinf(gTime * 37.0f) * camShake, cosf(gTime * 41.0f) * camShake, 0 };
    cam.position = Vector3Add(camPosSmooth, sh); cam.target = Vector3Add(camTargetSmooth, Vector3Scale(sh, 0.5f)); cam.up = (Vector3){ 0, 1, 0 }; cam.projection = CAMERA_PERSPECTIVE;
    cam.fovy = damp(cam.fovy, (fstate == FS_FIGHT) ? 52.0f : 60.0f, 3.0f, dt);
}

// ------------------------------------------------------------------------------------
//  Рендер кадра
// ------------------------------------------------------------------------------------
static void SetCommonUniforms(void) {
    float sd[3] = { sunDir.x, sunDir.y, sunDir.z }, sc[3] = { sunColor.x, sunColor.y, sunColor.z }, ac[3] = { ambientColor.x, ambientColor.y, ambientColor.z }, fc[3] = { fogColor.x, fogColor.y, fogColor.z }, vp[3] = { cam.position.x, cam.position.y, cam.position.z };
    float sk[3] = { skyColor.x, skyColor.y, skyColor.z }, hz[3] = { horizonColor.x, horizonColor.y, horizonColor.z };
    SetShaderValue(shLit, locLitSun, sd, SHADER_UNIFORM_VEC3); SetShaderValue(shLit, locLitSunCol, sc, SHADER_UNIFORM_VEC3); SetShaderValue(shLit, locLitAmb, ac, SHADER_UNIFORM_VEC3); SetShaderValue(shLit, locLitFog, fc, SHADER_UNIFORM_VEC3); SetShaderValue(shLit, locLitView, vp, SHADER_UNIFORM_VEC3);
    SetShaderValue(shLit, locLitTime, &gTime, SHADER_UNIFORM_FLOAT); SetShaderValue(shLit, locLitFogD, &fogDensity, SHADER_UNIFORM_FLOAT); SetShaderValueMatrix(shLit, locLitLightVP, lightVP);
    SetShaderValue(shWater, locWSun, sd, SHADER_UNIFORM_VEC3); SetShaderValue(shWater, locWSunCol, sc, SHADER_UNIFORM_VEC3); SetShaderValue(shWater, locWAmb, ac, SHADER_UNIFORM_VEC3); SetShaderValue(shWater, locWFog, fc, SHADER_UNIFORM_VEC3); SetShaderValue(shWater, locWView, vp, SHADER_UNIFORM_VEC3);
    SetShaderValue(shWater, locWTime, &gTime, SHADER_UNIFORM_FLOAT); SetShaderValue(shWater, locWFogD, &fogDensity, SHADER_UNIFORM_FLOAT); SetShaderValue(shWater, locWSky, sk, SHADER_UNIFORM_VEC3); SetShaderValue(shWater, locWHor, hz, SHADER_UNIFORM_VEC3);
    SetShaderValue(shSky, locSSun, sd, SHADER_UNIFORM_VEC3); SetShaderValue(shSky, locSSunCol, sc, SHADER_UNIFORM_VEC3); SetShaderValue(shSky, locSSky, sk, SHADER_UNIFORM_VEC3); SetShaderValue(shSky, locSHor, hz, SHADER_UNIFORM_VEC3); SetShaderValue(shSky, locSTime, &gTime, SHADER_UNIFORM_FLOAT); SetShaderValue(shSky, locSNight, &nightAmount, SHADER_UNIFORM_FLOAT);
    SetShaderValue(shPost, locPTime, &gTime, SHADER_UNIFORM_FLOAT);
    float focus = Clamp(Vector3Distance(cam.position, (fstate == FS_FIGHT || fstate == FS_WAIT || fstate == FS_BITE) ? bobberPos : playerPos) / 300.0f, 0, 1); SetShaderValue(shPost, locPFocus, &focus, SHADER_UNIFORM_FLOAT);
    float under = (cam.position.y < WaveHeight(cam.position.x, cam.position.z, gTime)) ? 1.0f : 0.0f; SetShaderValue(shPost, locPUnder, &under, SHADER_UNIFORM_FLOAT);
    SetShaderValue(shPost, locPFlash, &flashAmount, SHADER_UNIFORM_FLOAT);
}
static void RenderWorld(void) {
    SetCommonUniforms();
    // --- теневой проход ---
    Camera3D lc = { 0 }; lc.position = Vector3Subtract(playerPos, Vector3Scale(sunDir, 80.0f)); lc.target = playerPos; lc.up = (Vector3){ 0, 1, 0 }; lc.projection = CAMERA_ORTHOGRAPHIC; lc.fovy = 70.0f;
    shadowPass = 1; rlSetClipPlanes(5.0, 200.0);
    BeginTextureMode(shadowRT); ClearBackground(WHITE); BeginMode3D(lc);
    Matrix lv = rlGetMatrixModelview(), lp = rlGetMatrixProjection(); lightVP = MatrixMultiply(lv, lp);
    DrawScene();
    EndMode3D(); EndTextureMode(); shadowPass = 0; rlSetClipPlanes(0.1, 600.0);
    SetShaderValueMatrix(shLit, locLitLightVP, lightVP);
    // --- основной проход ---
    BeginTextureMode(sceneRT); ClearBackground((Color){ 0, 0, 0, 255 });
    BeginMode3D(cam);
    // небо
    rlDisableDepthMask(); rlDisableBackfaceCulling(); DrawMesh(meshSky, matSky, MatrixMultiply(MatrixScale(500, 500, 500), MatrixTranslate(cam.position.x, cam.position.y, cam.position.z))); rlEnableBackfaceCulling(); rlEnableDepthMask();
    // непрозрачная геометрия: alpha = глубина
    rlSetBlendFactorsSeparate(RL_ONE, RL_ZERO, RL_ONE, RL_ZERO, RL_FUNC_ADD, RL_FUNC_ADD); BeginBlendMode(BLEND_CUSTOM_SEPARATE);
    int slot = 10; rlActiveTextureSlot(slot); rlEnableTexture(shadowRT.depth.id); SetShaderValue(shLit, locLitShadowMap, &slot, SHADER_UNIFORM_INT); rlActiveTextureSlot(0);
    DrawScene();
    BeginShaderMode(shLine); DrawLineRod(); EndShaderMode();
    // вода: цвет смешивается, alpha (глубина) сохраняется
    rlSetBlendFactorsSeparate(RL_SRC_ALPHA, RL_ONE_MINUS_SRC_ALPHA, RL_ZERO, RL_ONE, RL_FUNC_ADD, RL_FUNC_ADD); BeginBlendMode(BLEND_CUSTOM_SEPARATE);
    rlDisableBackfaceCulling(); DrawMesh(meshWater, matWater, MatrixIdentity()); rlEnableBackfaceCulling();
    DrawParticles();
    EndBlendMode(); rlEnableDepthMask();
    EndMode3D(); EndTextureMode();
}

// ------------------------------------------------------------------------------------
//  Интерфейс (русский)
// ------------------------------------------------------------------------------------
static void DrawHUD(void) {
    float s = uiScale; float W = screenW, H = screenH;
    // энергия
    Panel(20 * s, 20 * s, 300 * s, 92 * s);
    DrawTextEx(font, "Энергия", (Vector2){ 34 * s, 28 * s }, 20 * s, 1, RAYWHITE);
    DrawRectangleRounded((Rectangle){ 34 * s, 54 * s, 270 * s, 14 * s }, 0.5f, 6, Fade(BLACK, 0.5f)); DrawRectangleRounded((Rectangle){ 34 * s, 54 * s, 270 * s * (S.energy / 100.0f), 14 * s }, 0.5f, 6, S.energy > 25 ? (Color){ 120, 220, 110, 255 } : (Color){ 230, 90, 70, 255 });
    char b[256]; int inv = 0; for (int i = 0; i < NUM_SPECIES; i++) inv += S.inventory[i];
    snprintf(b, sizeof(b), "Счёт: %d    Сумка: %d    Атлас: %d/8", S.score, inv, CountSpecies()); DrawTextEx(font, b, (Vector2){ 34 * s, 76 * s }, 18 * s, 1, (Color){ 255, 230, 170, 255 });
    // время
    int hh = (int)S.timeOfDay, mm = (int)((S.timeOfDay - hh) * 60); snprintf(b, sizeof(b), "День %d   %02d:%02d", S.days + 1, hh, mm); Panel(W - 220 * s, 20 * s, 200 * s, 44 * s); DrawTextC(b, W - 120 * s, 30 * s, 22 * s, RAYWHITE);
    // задание
    DrawTextCS(CurrentTask(), W / 2, 22 * s, 20 * s, (Color){ 255, 240, 200, 255 });
    // подсказки взаимодействия
    int nearFire = Vector3Distance(playerPos, firePos) < 3.5f; if (nearFire && fstate == FS_IDLE) DrawTextCS("[F] Приготовить рыбу     [R] Отдохнуть у костра", W / 2, H - 120 * s, 20 * s, (Color){ 255, 220, 140, 255 });
    // состояние рыбалки
    if (fstate == FS_CHARGE) { float p = Clamp(castPower, 0, 1); DrawRectangleRounded((Rectangle){ W / 2 - 120 * s, H - 70 * s, 240 * s, 16 * s }, 0.5f, 6, Fade(BLACK, 0.6f)); DrawRectangleRounded((Rectangle){ W / 2 - 120 * s, H - 70 * s, 240 * s * p, 16 * s }, 0.5f, 6, (Color){ 255, 200, 80, 255 }); DrawTextCS("Сила заброса", W / 2, H - 96 * s, 18 * s, RAYWHITE); }
    if (fstate == FS_WAIT) { DrawTextCS(nibbleFlash > 0 ? "Клюёт?.. Ждите настоящую поклёвку" : "Ждём поклёвку…  (ПКМ — смотать)", W / 2, H - 70 * s, 20 * s, nibbleFlash > 0 ? (Color){ 255, 230, 120, 255 } : Fade(RAYWHITE, 0.8f)); }
    if (fstate == FS_BITE) { float pulse = 0.7f + 0.3f * sinf(gTime * 20); DrawTextCS("ПОДСЕКАЙТЕ! [ЛКМ]", W / 2, H / 2 - 120 * s, 46 * s, (Color){ 255, (unsigned char)(200 * pulse), 60, 255 }); }
    if (fstate == FS_FIGHT) {
        float bw = 360 * s, bx = W / 2 - bw / 2, by = H - 90 * s; DrawRectangleRounded((Rectangle){ bx, by, bw, 22 * s }, 0.5f, 6, Fade(BLACK, 0.65f));
        DrawRectangleRounded((Rectangle){ bx + bw * 0.72f, by, bw * 0.28f, 22 * s }, 0.5f, 6, Fade((Color){ 230, 70, 60, 255 }, 0.45f)); DrawRectangleRounded((Rectangle){ bx + bw * 0.35f, by, bw * 0.37f, 22 * s }, 0.5f, 6, Fade((Color){ 120, 220, 110, 255 }, 0.35f));
        float t = Clamp(tension / 1.3f, 0, 1); DrawRectangleRounded((Rectangle){ bx, by, bw * t, 22 * s }, 0.5f, 6, tension > 0.95f ? (Color){ 255, 60, 50, 255 } : (Color){ 255, 220, 100, 255 });
        DrawTextCS("Натяжение лески", W / 2, by - 28 * s, 18 * s, RAYWHITE);
        snprintf(b, sizeof(b), "Дистанция: %.1f м   %s", lineLen, SPECIES[fish[hookedFish].species].name); DrawTextCS(b, W / 2, by + 28 * s, 18 * s, (Color){ 255, 240, 200, 255 });
    }
    // прицел
    if (fstate == FS_IDLE || fstate == FS_CHARGE) DrawCircleLines((int)(W / 2), (int)(H / 2), 4, Fade(WHITE, 0.6f));
    // сообщения
    if (msgTimer > 0) { Vector2 m = MeasureTextEx(font, msgText, 22 * s, 1); DrawRectangleRounded((Rectangle){ W / 2 - m.x / 2 - 16 * s, H - 170 * s, m.x + 32 * s, 40 * s }, 0.4f, 8, Fade(BLACK, 0.6f)); DrawTextC(msgText, W / 2, H - 162 * s, 22 * s, (Color){ 255, 245, 220, 255 }); }
    if (toastTimer > 0) { float a = Clamp(toastTimer, 0, 1); Vector2 m = MeasureTextEx(font, toastText, 24 * s, 1); float y = 120 * s - (1 - Clamp((4 - toastTimer) * 3, 0, 1)) * 40 * s;
        DrawRectangleRounded((Rectangle){ W / 2 - m.x / 2 - 20 * s, y, m.x + 40 * s, 46 * s }, 0.4f, 8, Fade((Color){ 255, 200, 90, 255 }, 0.9f * a)); DrawTextC(toastText, W / 2, y + 10 * s, 24 * s, Fade((Color){ 40, 30, 20, 255 }, a)); }
    // обучение
    if (tutorialStep > 0 && tutorialStep < 7) {
        const char *tt[7] = { "", "Добро пожаловать на остров! WASD — ходить, мышь — камера, Shift — бег. Подойдите к пирсу.", "Зажмите ЛКМ, чтобы накопить силу заброса, и отпустите, целясь в воду.", "Поплавок летит! Подождите, пока он опустится на воду.", "Ждите поклёвку. Мелкие подёргивания — рыба пробует наживку. Не спешите!", "Рыба на крючке! Удерживайте ЛКМ, чтобы подтягивать, и отпускайте, когда натяжение в красной зоне.", "Отлично! Рыба в сумке. У костра [F] — приготовить и съесть, [R] — отдохнуть. Откройте 6 видов, чтобы встретить легенду." };
        Vector2 m = MeasureTextEx(font, tt[tutorialStep], 20 * s, 1); float pw = fminf(m.x + 40 * s, W - 40 * s);
        Panel(W / 2 - pw / 2, H - 240 * s, pw, 56 * s); DrawTextEx(font, "Обучение", (Vector2){ W / 2 - pw / 2 + 14 * s, H - 236 * s }, 14 * s, 1, (Color){ 255, 200, 90, 255 }); DrawTextC(tt[tutorialStep], W / 2, H - 218 * s, 20 * s, RAYWHITE);
        if (tutorialStep == 1 && Vector3Distance(playerPos, pierPos) < 8) tutorialStep = 2; if (tutorialStep == 6) { tutorialTimer += dt; if (tutorialTimer > 10) { tutorialStep = 7; S.showTutorial = 0; } }
    }
    // мини-карта
    { float mw = 150 * s, mx = W - mw - 20 * s, my = 80 * s; DrawRectangleRounded((Rectangle){ mx, my, mw, mw }, 0.1f, 6, Fade((Color){ 20, 60, 90, 255 }, 0.75f));
        for (int i = 0; i < 4; i++) { Vector3 p = spots[i]; DrawCircle((int)(mx + (p.x / WORLD_SIZE + 0.5f) * mw), (int)(my + (p.z / WORLD_SIZE + 0.5f) * mw), 3 * s, S.spotsVisited[i] ? GRAY : (Color){ 255, 220, 90, 255 }); }
        DrawCircle((int)(mx + (pierPos.x / WORLD_SIZE + 0.5f) * mw), (int)(my + (pierPos.z / WORLD_SIZE + 0.5f) * mw), 3 * s, (Color){ 200, 150, 90, 255 });
        DrawCircle((int)(mx + (playerPos.x / WORLD_SIZE + 0.5f) * mw), (int)(my + (playerPos.z / WORLD_SIZE + 0.5f) * mw), 4 * s, (Color){ 255, 90, 70, 255 });
        DrawTextEx(font, "Карта", (Vector2){ mx + 6 * s, my + 2 * s }, 14 * s, 1, Fade(WHITE, 0.7f)); }
}
static void DrawTitle(const char *t, float y) { DrawTextCS(t, screenW / 2.0f, y, 48 * uiScale, (Color){ 255, 225, 150, 255 }); }
static void DrawMenu(void) {
    float s = uiScale, W = screenW, H = screenH; menuAnim += dt;
    DrawRectangleGradientV(0, 0, screenW, screenH, Fade(BLACK, 0.15f), Fade(BLACK, 0.55f));
    DrawTextCS("ТИХАЯ ЗАВОДЬ", W / 2, 90 * s + sinf(menuAnim) * 4, 84 * s, (Color){ 255, 235, 180, 255 }); DrawTextCS("уютная 3D-рыбалка на забытом острове", W / 2, 180 * s, 24 * s, Fade(RAYWHITE, 0.85f));
    float bw = 320 * s, bh = 54 * s, bx = W / 2 - bw / 2, by = 250 * s; int hasSave = (S.totalCaught > 0 || S.casts > 0) && S.energy > 0;
    int row = 0;
    if (hasSave && Button("Продолжить", bx, by + row++ * 64 * s, bw, bh, 0)) { ContinueGame(); state = ST_PLAY; DisableCursor(); }
    if (Button("Новая игра", bx, by + row++ * 64 * s, bw, bh, 0)) { NewGame(); state = ST_PLAY; DisableCursor(); }
    if (Button("Настройки", bx, by + row++ * 64 * s, bw, bh, 0)) { prevState = ST_MENU; state = ST_SETTINGS; }
    if (Button("Управление", bx, by + row++ * 64 * s, bw, bh, 0)) { prevState = ST_MENU; state = ST_CONTROLS; }
    if (Button("Достижения", bx, by + row++ * 64 * s, bw, bh, 0)) { prevState = ST_MENU; state = ST_ACHIEVEMENTS; }
    if (Button("Выход", bx, by + row++ * 64 * s, bw, bh, 0)) { SaveGame(); CloseWindow(); exit(0); }
    char b[128]; snprintf(b, sizeof(b), "Рекорд: %d очков", S.highScore); DrawTextCS(b, W / 2, H - 70 * s, 22 * s, (Color){ 255, 220, 150, 255 });
    DrawTextEx(font, "v" GAME_VERSION " · raylib 5.5 · Пресет А: стилизованная вода и физика природы", (Vector2){ 16 * s, H - 30 * s }, 16 * s, 1, Fade(WHITE, 0.6f));
}
static void DrawPause(void) {
    float s = uiScale, W = screenW, H = screenH; DrawRectangle(0, 0, screenW, screenH, Fade(BLACK, 0.5f)); DrawTitle("ПАУЗА", 120 * s);
    float bw = 320 * s, bh = 52 * s, bx = W / 2 - bw / 2, by = 220 * s; int row = 0;
    if (Button("Продолжить", bx, by + row++ * 62 * s, bw, bh, 0)) { state = ST_PLAY; DisableCursor(); }
    if (Button("Атлас рыб", bx, by + row++ * 62 * s, bw, bh, 0)) { prevState = ST_PAUSE; state = ST_ATLAS; }
    if (Button("Настройки", bx, by + row++ * 62 * s, bw, bh, 0)) { prevState = ST_PAUSE; state = ST_SETTINGS; }
    if (Button("Управление", bx, by + row++ * 62 * s, bw, bh, 0)) { prevState = ST_PAUSE; state = ST_CONTROLS; }
    if (Button("Достижения", bx, by + row++ * 62 * s, bw, bh, 0)) { prevState = ST_PAUSE; state = ST_ACHIEVEMENTS; }
    if (Button("Сохранить и выйти в меню", bx, by + row++ * 62 * s, bw, bh, 0)) { SaveGame(); state = ST_MENU; }
    (void)H;
}
static void DrawSettings(void) {
    float s = uiScale, W = screenW, H = screenH; DrawRectangle(0, 0, screenW, screenH, Fade(BLACK, 0.55f)); DrawTitle("НАСТРОЙКИ", 60 * s);
    float bw = 520 * s, bh = 48 * s, bx = W / 2 - bw / 2, by = 150 * s; char b[128]; int row = 0; const char *q[3] = { "Низкое", "Среднее", "Высокое" };
    snprintf(b, sizeof(b), "Качество графики: %s", q[S.quality]); if (Button(b, bx, by + row++ * 60 * s, bw, bh, 0)) { S.quality = (S.quality + 1) % 3; ApplyQuality(); }
    snprintf(b, sizeof(b), "Полноэкранный режим: %s", S.fullscreen ? "Вкл" : "Выкл"); if (Button(b, bx, by + row++ * 60 * s, bw, bh, 0)) { S.fullscreen = !S.fullscreen; ToggleBorderlessWindowed(); }
    snprintf(b, sizeof(b), "Вертикальная синхронизация: %s", S.vsync ? "Вкл" : "Выкл"); if (Button(b, bx, by + row++ * 60 * s, bw, bh, 0)) { S.vsync = !S.vsync; if (S.vsync) SetTargetFPS(0), SetWindowState(FLAG_VSYNC_HINT); else ClearWindowState(FLAG_VSYNC_HINT), SetTargetFPS(144); }
    snprintf(b, sizeof(b), "Громкость музыки: %d%%", (int)(S.musicVol * 100)); if (Button(b, bx, by + row++ * 60 * s, bw, bh, 0)) { S.musicVol += 0.2f; if (S.musicVol > 1.01f) S.musicVol = 0; SetAudioStreamVolume(music, S.musicVol); }
    snprintf(b, sizeof(b), "Громкость эффектов: %d%%", (int)(S.sfxVol * 100)); if (Button(b, bx, by + row++ * 60 * s, bw, bh, 0)) { S.sfxVol += 0.2f; if (S.sfxVol > 1.01f) S.sfxVol = 0; }
    snprintf(b, sizeof(b), "Чувствительность мыши: %.1f", S.sensitivity); if (Button(b, bx, by + row++ * 60 * s, bw, bh, 0)) { S.sensitivity += 0.25f; if (S.sensitivity > 2.01f) S.sensitivity = 0.5f; }
    snprintf(b, sizeof(b), "Обучение в новой игре: %s", S.showTutorial ? "Вкл" : "Выкл"); if (Button(b, bx, by + row++ * 60 * s, bw, bh, 0)) { S.showTutorial = !S.showTutorial; }
    if (Button("Назад", bx, by + row++ * 60 * s + 20 * s, bw, bh, 0) || IsKeyPressed(KEY_ESCAPE)) { SaveGame(); state = prevState; if (prevState == ST_PAUSE) { } }
    (void)H;
}
static void DrawControls(void) {
    float s = uiScale, W = screenW, H = screenH; DrawRectangle(0, 0, screenW, screenH, Fade(BLACK, 0.55f)); DrawTitle("УПРАВЛЕНИЕ", 60 * s);
    const char *lines[] = { "W A S D — передвижение", "Мышь — поворот камеры, колесо — приближение", "Shift — бег (тратит энергию)", "ЛКМ (удерживать) — накопить силу заброса, отпустить — забросить", "ЛКМ — подсечь при поклёвке; удерживать — подтягивать рыбу", "ПКМ — смотать леску", "F — приготовить и съесть рыбу у костра", "R — отдохнуть у костра (восстанавливает энергию, меняет время суток)", "Tab — атлас рыб", "Esc — пауза / меню" };
    int n = sizeof(lines) / sizeof(lines[0]); Panel(W / 2 - 400 * s, 140 * s, 800 * s, n * 36 * s + 30 * s);
    for (int i = 0; i < n; i++) DrawTextEx(font, lines[i], (Vector2){ W / 2 - 380 * s, 156 * s + i * 36 * s }, 22 * s, 1, RAYWHITE);
    if (Button("Назад", W / 2 - 160 * s, H - 100 * s, 320 * s, 50 * s, 0) || IsKeyPressed(KEY_ESCAPE)) state = prevState;
}
static void DrawAchievements(void) {
    float s = uiScale, W = screenW, H = screenH; DrawRectangle(0, 0, screenW, screenH, Fade(BLACK, 0.55f)); DrawTitle("ДОСТИЖЕНИЯ", 50 * s);
    int n = 0; for (int i = 0; i < NUM_ACH; i++) n += ACH[i].unlocked; char b[64]; snprintf(b, sizeof(b), "Открыто: %d из %d", n, NUM_ACH); DrawTextCS(b, W / 2, 110 * s, 22 * s, RAYWHITE);
    for (int i = 0; i < NUM_ACH; i++) { int col = i % 2, row = i / 2; float x = W / 2 - 420 * s + col * 430 * s, y = 150 * s + row * 78 * s; Color c = ACH[i].unlocked ? (Color){ 255, 200, 90, 220 } : (Color){ 40, 60, 80, 200 };
        DrawRectangleRounded((Rectangle){ x, y, 410 * s, 68 * s }, 0.25f, 6, c); DrawCircle((int)(x + 34 * s), (int)(y + 34 * s), 20 * s, ACH[i].unlocked ? (Color){ 255, 240, 200, 255 } : Fade(BLACK, 0.4f));
        if (ACH[i].unlocked) DrawTextC("★", x + 34 * s, y + 20 * s, 26 * s, (Color){ 200, 140, 40, 255 });
        DrawTextEx(font, ACH[i].name, (Vector2){ x + 66 * s, y + 10 * s }, 22 * s, 1, ACH[i].unlocked ? (Color){ 40, 30, 20, 255 } : RAYWHITE); DrawTextEx(font, ACH[i].desc, (Vector2){ x + 66 * s, y + 38 * s }, 16 * s, 1, ACH[i].unlocked ? (Color){ 70, 50, 30, 255 } : Fade(RAYWHITE, 0.7f)); }
    if (Button("Назад", W / 2 - 160 * s, H - 70 * s, 320 * s, 50 * s, 0) || IsKeyPressed(KEY_ESCAPE)) state = prevState;
}
static void DrawAtlas(void) {
    float s = uiScale, W = screenW, H = screenH; DrawRectangle(0, 0, screenW, screenH, Fade(BLACK, 0.55f)); DrawTitle("АТЛАС РЫБ", 50 * s);
    for (int i = 0; i < NUM_SPECIES; i++) { int col = i % 3, row = i / 3; float x = W / 2 - 480 * s + col * 325 * s, y = 130 * s + row * 150 * s; int known = S.caughtBySpecies[i] > 0; Panel(x, y, 310 * s, 136 * s);
        DrawCircle((int)(x + 40 * s), (int)(y + 40 * s), 24 * s, known ? SPECIES[i].col : Fade(BLACK, 0.4f)); char b[128];
        DrawTextEx(font, known ? SPECIES[i].name : "???", (Vector2){ x + 76 * s, y + 14 * s }, 24 * s, 1, (Color){ 255, 225, 150, 255 }); DrawTextEx(font, known ? SPECIES[i].latin : "ещё не поймана", (Vector2){ x + 76 * s, y + 44 * s }, 16 * s, 1, Fade(RAYWHITE, 0.85f));
        if (known) snprintf(b, sizeof(b), "Поймано: %d   В сумке: %d", S.caughtBySpecies[i], S.inventory[i]); else snprintf(b, sizeof(b), "Активность: %s", SPECIES[i].night ? "ночью" : "днём"); DrawTextEx(font, b, (Vector2){ x + 16 * s, y + 80 * s }, 16 * s, 1, RAYWHITE);
        snprintf(b, sizeof(b), "Очки: %d   Вес: %.1f–%.1f кг", SPECIES[i].score, SPECIES[i].minSize, SPECIES[i].maxSize); DrawTextEx(font, b, (Vector2){ x + 16 * s, y + 104 * s }, 16 * s, 1, Fade(RAYWHITE, 0.7f)); }
    if (Button("Назад", W / 2 - 160 * s, H - 60 * s, 320 * s, 48 * s, 0) || IsKeyPressed(KEY_ESCAPE)) { state = prevState; if (prevState == ST_PLAY) DisableCursor(); }
}
static void DrawEndScreen(int victory) {
    float s = uiScale, W = screenW, H = screenH; DrawRectangle(0, 0, screenW, screenH, Fade(victory ? (Color){ 20, 40, 30, 255 } : (Color){ 40, 10, 10, 255 }, 0.65f));
    DrawTitle(victory ? "ЛЕГЕНДА ПОЙМАНА!" : "ВЫ ВЫБИЛИСЬ ИЗ СИЛ", 110 * s); char b[256];
    DrawTextCS(victory ? "Старый Сом наконец-то в ваших руках. Остров запомнит эту ночь." : "Энергия закончилась. Не забывайте есть и отдыхать у костра.", W / 2, 190 * s, 24 * s, RAYWHITE);
    snprintf(b, sizeof(b), "Очки: %d     Поймано рыб: %d     Видов: %d/8     Рекорд: %d", S.score, S.totalCaught, CountSpecies(), S.highScore > S.score ? S.highScore : S.score); DrawTextCS(b, W / 2, 250 * s, 24 * s, (Color){ 255, 225, 150, 255 });
    float bw = 320 * s; if (victory) { if (Button("Продолжить рыбачить", W / 2 - bw / 2, 330 * s, bw, 52 * s, 0)) { S.victory = 0; state = ST_PLAY; DisableCursor(); } }
    if (Button("Новая игра", W / 2 - bw / 2, (victory ? 400 : 330) * s, bw, 52 * s, 0)) { NewGame(); state = ST_PLAY; DisableCursor(); }
    if (Button("В главное меню", W / 2 - bw / 2, (victory ? 470 : 400) * s, bw, 52 * s, 0)) { SaveGame(); state = ST_MENU; }
    (void)H;
}

// ------------------------------------------------------------------------------------
//  main
// ------------------------------------------------------------------------------------
int main(void) {
    DefaultSave(); int loaded = LoadGame();
    SetConfigFlags(FLAG_MSAA_4X_HINT | FLAG_WINDOW_RESIZABLE | (S.vsync ? FLAG_VSYNC_HINT : 0));
    SetTraceLogLevel(LOG_WARNING);
    InitWindow(1280, 720, "Тихая Заводь — v" GAME_VERSION); SetExitKey(KEY_NULL); if (!S.vsync) SetTargetFPS(144);
    if (S.fullscreen) ToggleBorderlessWindowed();
    InitAudioDevice();
    // шрифт с кириллицей
    { int cps[512]; int n = 0; for (int c = 32; c < 127; c++) cps[n++] = c; for (int c = 0x400; c <= 0x45F; c++) cps[n++] = c; int extra[] = { 0x2014, 0x2013, 0x2026, 0x00AB, 0x00BB, 0x2116, 0x2605, 0x00B0 }; for (int i = 0; i < 8; i++) cps[n++] = extra[i];
        font = LoadFontFromMemory(".ttf", FONT_DATA, (int)FONT_DATA_SIZE, 48, cps, n); SetTextureFilter(font.texture, TEXTURE_FILTER_BILINEAR); }
    InitRender();
    sndSplash = MakeSound(11025, 0); sndPlop = MakeSound(6000, 1); sndStep = MakeSound(2500, 2); sndClick = MakeSound(1500, 3); sndCatch = MakeSound(22050, 4); sndSnap = MakeSound(5000, 5); sndBite = MakeSound(6000, 6); sndEat = MakeSound(12000, 7); sndCast = MakeSound(7000, 8); sndAch = MakeSound(26000, 9); sndReel = MakeSound(8000, 10);
    SetAudioStreamBufferSizeDefault(2048); music = LoadAudioStream(22050, 16, 1); SetAudioStreamVolume(music, S.musicVol); PlayAudioStream(music);
    BuildWorld(); // мир для фона меню
    { Vector3 dir = { cosf(pierYaw), 0, sinf(pierYaw) }; playerPos = Vector3Add(pierPos, Vector3Scale(dir, -6.0f)); playerPos.y = TerrainHeight(playerPos.x, playerPos.z); playerYaw = atan2f(dir.x, dir.z); playerYawSmooth = playerYaw; camTargetSmooth = playerPos; camPosSmooth = Vector3Add(playerPos, (Vector3){ 0, 4, -8 }); }
    cam.fovy = 60; cam.up = (Vector3){ 0, 1, 0 }; cam.projection = CAMERA_PERSPECTIVE; float menuTime = loaded ? S.timeOfDay : 8.5f; (void)menuTime;
    while (!WindowShouldClose()) {
        dt = GetFrameTime(); if (dt > 0.05f) dt = 0.05f; gTime += dt;
        if (IsWindowResized()) ResizeTargets();
        UpdateMusic(); if (toastTimer > 0) toastTimer -= dt; if (msgTimer > 0) msgTimer -= dt; flashAmount = damp(flashAmount, 0, 4, dt);
        if (state == ST_PAUSE && IsKeyPressed(KEY_ESCAPE)) { state = ST_PLAY; DisableCursor(); }
        else if (state == ST_ATLAS && IsKeyPressed(KEY_TAB)) { state = prevState; if (prevState == ST_PLAY) DisableCursor(); }
        else if (state == ST_PLAY) {
            S.timeOfDay += dt * 24.0f / DAY_LENGTH; if (S.timeOfDay >= 24.0f) { S.timeOfDay -= 24.0f; S.days++; CheckAchievements(); }
            UpdatePlayer(); if (state == ST_PLAY) { UpdateFishing(); UpdateFish(); }
            if (IsKeyPressed(KEY_ESCAPE)) { state = ST_PAUSE; EnableCursor(); SaveGame(); }
            if (IsKeyPressed(KEY_TAB)) { prevState = ST_PLAY; state = ST_ATLAS; EnableCursor(); }
        } else if (state == ST_MENU) { // плавный облёт камерой
            camYaw += dt * 0.05f; camPitch = 0.25f; camDist = 9.0f; S.timeOfDay = loaded ? S.timeOfDay : 9.0f + sinf(gTime * 0.02f) * 2.0f; if (!loaded) { } 
        }
        UpdateDayCycle(); UpdateGameCamera(); UpdateParticles();
        RenderWorld();
        BeginDrawing(); ClearBackground(BLACK);
        BeginShaderMode(shPost); DrawTextureRec(sceneRT.texture, (Rectangle){ 0, 0, (float)sceneRT.texture.width, -(float)sceneRT.texture.height }, (Vector2){ 0, 0 }, WHITE); EndShaderMode();
        switch (state) {
        case ST_PLAY: DrawHUD(); break;
        case ST_MENU: DrawMenu(); break;
        case ST_PAUSE: DrawPause(); break;
        case ST_SETTINGS: DrawSettings(); break;
        case ST_CONTROLS: DrawControls(); break;
        case ST_ACHIEVEMENTS: DrawAchievements(); break;
        case ST_ATLAS: DrawAtlas(); break;
        case ST_VICTORY: DrawEndScreen(1); break;
        case ST_GAMEOVER: DrawEndScreen(0); break;
        }
        if (state != ST_PLAY) { char b[64]; snprintf(b, sizeof(b), "%d FPS", GetFPS()); DrawTextEx(font, b, (Vector2){ screenW - 90 * uiScale, screenH - 28 * uiScale }, 16 * uiScale, 1, Fade(WHITE, 0.5f)); }
        EndDrawing();
    }
    SaveGame(); CloseAudioDevice(); CloseWindow(); return 0;
}
