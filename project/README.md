# Тихая Заводь (Tikhaya Zavod) — v1.0.0

Уютная 3D-рыбалка на стилизованном острове. Windows x64, один исполняемый файл, без зависимостей.
Интерфейс полностью на русском языке.

## Запуск
Запустите `TikhayaZavod_v1.0.0.exe`. Сохранение создаётся рядом с .exe (`tikhaya_zavod_save.dat`).
Требования: Windows 10/11 x64, видеокарта с OpenGL 3.3.

## Структура исходников
- `src/main.c` — движок, геймплей, UI, аудио, сохранения (один файл на C99).
- `src/shaders.h` — GLSL 330: вода Герстнера, освещение с тенями, небо, пост-обработка.
- `src/font_data.h` — встроенный шрифт Rubik Medium (SIL OFL 1.1).
- `docs/100_3D_GAME_IDEAS.md|json` — каталог 100 идей и скоринговая матрица.
- `docs/GDD_Тихая_Заводь.md` — дизайн-документ победившей игры.
- `assets/Rubik-Medium.ttf` — исходный шрифт.

## Сборка из исходников
Нужны raylib 5.5 (исходники) и Zig (или любой C-компилятор для Windows):
```
# объектные файлы raylib для Windows (PLATFORM_DESKTOP, OpenGL 3.3)
zig cc -target x86_64-windows-gnu -O2 -DPLATFORM_DESKTOP -DGRAPHICS_API_OPENGL_33 -Iraylib/src/external/glfw/include -c raylib/src/{rcore,rshapes,rtextures,rtext,rmodels,raudio,utils,rglfw}.c
# игра
zig cc -target x86_64-windows-gnu -O2 -std=gnu99 -DRAYMATH_STATIC_INLINE -Iraylib/src src/main.c *.o \
  -lgdi32 -lwinmm -lopengl32 -lshell32 -luser32 -lkernel32 -Wl,--subsystem,windows -o TikhayaZavod_v1.0.0.exe
```

## Управление
WASD — ходьба · Мышь — камера · Shift — бег · ЛКМ (удерж.) — заброс · ЛКМ — подсечка/подтягивание · ПКМ — смотать ·
F — приготовить рыбу у костра · R — отдохнуть у костра · Tab — атлас · Esc — пауза.
