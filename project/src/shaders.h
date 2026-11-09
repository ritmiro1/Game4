// shaders.h — встроенные GLSL 330 шейдеры игры «Тихая Заводь»
// Общий освещённый шейдер (terrain/props/персонаж/рыбы), вода, небо, пост-обработка, тень.
#pragma once

// ---------- общие функции шума ----------
#define NOISE_GLSL \
"float hash21(vec2 p){ p = fract(p*vec2(123.34, 456.21)); p += dot(p, p+45.32); return fract(p.x*p.y); }\n" \
"float vnoise(vec2 p){ vec2 i = floor(p); vec2 f = fract(p); f = f*f*(3.0-2.0*f);\n" \
"  float a = hash21(i); float b = hash21(i+vec2(1,0)); float c = hash21(i+vec2(0,1)); float d = hash21(i+vec2(1,1));\n" \
"  return mix(mix(a,b,f.x), mix(c,d,f.x), f.y); }\n" \
"float fbm(vec2 p){ float v = 0.0; float a = 0.5; for(int i=0;i<4;i++){ v += a*vnoise(p); p = p*2.03 + vec2(1.7,9.2); a *= 0.5; } return v; }\n" \
"float caustic(vec2 uv, float t){\n" \
"  vec2 p = uv; float c = 0.0; float s = 1.0;\n" \
"  for(int i=0;i<3;i++){ vec2 q = p*s + vec2(t*0.25*s, -t*0.17*s); float n = vnoise(q*3.0); c += abs(0.5 - n)*2.0/s; s *= 1.9; }\n" \
"  c = 1.0 - clamp(c, 0.0, 1.0); return pow(c, 3.0); }\n"

// ---------- LIT: освещённый шейдер с теневой картой ----------
static const char *LIT_VS =
"#version 330\n"
"in vec3 vertexPosition; in vec2 vertexTexCoord; in vec3 vertexNormal; in vec4 vertexColor;\n"
"uniform mat4 mvp; uniform mat4 matModel; uniform mat4 matNormal; uniform mat4 matView; uniform mat4 matProjection; uniform mat4 lightVP;\n"
"uniform float time; uniform float sway;\n"
"out vec3 fragPos; out vec3 fragNormal; out vec4 fragColor; out vec2 fragUV; out vec4 fragLightPos; out float viewDepth;\n"
"void main(){\n"
"  vec3 p = vertexPosition;\n"
"  vec4 wp = matModel*vec4(p,1.0);\n"
"  if (sway > 0.0) { float k = clamp(p.y, 0.0, 10.0)*sway; wp.x += sin(time*1.3 + wp.z*0.3)*0.12*k; wp.z += cos(time*1.1 + wp.x*0.3)*0.08*k; }\n"
"  fragPos = wp.xyz; fragNormal = normalize((matNormal*vec4(vertexNormal,0.0)).xyz); fragColor = vertexColor; fragUV = vertexTexCoord;\n"
"  fragLightPos = lightVP*wp;\n"
"  gl_Position = matProjection*matView*vec4(wp.xyz,1.0);\n"
"  viewDepth = gl_Position.w;\n"
"}\n";

static const char *LIT_FS =
"#version 330\n"
"in vec3 fragPos; in vec3 fragNormal; in vec4 fragColor; in vec2 fragUV; in vec4 fragLightPos; in float viewDepth;\n"
"uniform vec4 colDiffuse; uniform sampler2D texture0; uniform sampler2D shadowMap;\n"
"uniform vec3 sunDir; uniform vec3 sunColor; uniform vec3 ambientColor; uniform vec3 fogColor; uniform vec3 viewPos;\n"
"uniform float time; uniform float fogDensity; uniform float waterLevel; uniform int shadowRes; uniform float emissive;\n"
"out vec4 finalColor;\n"
NOISE_GLSL
"void main(){\n"
"  vec3 N = normalize(fragNormal);\n"
"  vec3 L = normalize(-sunDir);\n"
"  vec3 V = normalize(viewPos - fragPos);\n"
"  vec4 base = colDiffuse*fragColor*texture(texture0, fragUV);\n"
"  float ndl = dot(N,L);\n"
"  // мягкий cel-shading: три ступени с плавными переходами\n"
"  float toon = smoothstep(-0.05, 0.15, ndl)*0.55 + smoothstep(0.35, 0.6, ndl)*0.45;\n"
"  // тени (PCF 3x3)\n"
"  vec3 lp = fragLightPos.xyz/fragLightPos.w; lp = lp*0.5 + 0.5;\n"
"  float shadow = 1.0;\n"
"  if (lp.x > 0.0 && lp.x < 1.0 && lp.y > 0.0 && lp.y < 1.0 && lp.z < 1.0) {\n"
"    float bias = max(0.0025*(1.0 - ndl), 0.0008); float s = 0.0; float texel = 1.0/float(shadowRes);\n"
"    for(int x=-1;x<=1;x++) for(int y=-1;y<=1;y++){ float d = texture(shadowMap, lp.xy + vec2(x,y)*texel).r; s += (lp.z - bias > d) ? 0.0 : 1.0; }\n"
"    shadow = s/9.0; shadow = mix(0.35, 1.0, shadow);\n"
"  }\n"
"  float wrap = 0.5 + 0.5*ndl;\n"
"  vec3 light = ambientColor*(0.6 + 0.4*wrap) + sunColor*toon*shadow;\n"
"  // блик\n"
"  vec3 H = normalize(L+V); float spec = pow(max(dot(N,H),0.0), 32.0)*0.25*shadow*step(0.0, ndl);\n"
"  // rim\n"
"  float rim = pow(1.0 - max(dot(N,V),0.0), 3.0)*0.25;\n"
"  vec3 col = base.rgb*light + sunColor*spec + ambientColor*rim;\n"
"  // под водой: тонирование + каустика + поглощение\n"
"  if (fragPos.y < waterLevel) {\n"
"    float depth = waterLevel - fragPos.y;\n"
"    float c = caustic(fragPos.xz*0.35, time)*max(ndl,0.0)*shadow;\n"
"    col += sunColor*c*0.8*exp(-depth*0.25);\n"
"    vec3 deep = vec3(0.05, 0.25, 0.32);\n"
"    col = mix(col, deep, clamp(depth*0.14, 0.0, 0.92));\n"
"  }\n"
"  col += base.rgb*emissive;\n"
"  // туман по расстоянию + высоте\n"
"  float dist = length(viewPos - fragPos);\n"
"  float fog = 1.0 - exp(-dist*fogDensity); fog = clamp(fog + clamp(-(fragPos.y)*0.0, 0.0, 0.0), 0.0, 1.0);\n"
"  col = mix(col, fogColor, fog);\n"
"  finalColor = vec4(col, clamp(viewDepth/300.0, 0.0, 1.0));\n"
"}\n";

// ---------- WATER ----------
static const char *WATER_VS =
"#version 330\n"
"in vec3 vertexPosition; in vec2 vertexTexCoord; in vec3 vertexNormal; in vec4 vertexColor;\n"
"uniform mat4 mvp; uniform mat4 matModel; uniform mat4 matView; uniform mat4 matProjection; uniform float time;\n"
"out vec3 fragPos; out vec3 fragNormal; out vec2 fragUV; out float viewDepth;\n"
"vec3 gerstner(vec2 xz, vec2 dir, float steep, float wl, float speed, float t, inout vec3 tang, inout vec3 bino){\n"
"  float k = 6.2831853/wl; float c = sqrt(9.8/k); vec2 d = normalize(dir); float f = k*(dot(d,xz) - c*speed*t); float a = steep/k;\n"
"  tang += vec3(-d.x*d.x*(steep*sin(f)), d.x*(steep*cos(f)), -d.x*d.y*(steep*sin(f)));\n"
"  bino += vec3(-d.x*d.y*(steep*sin(f)), d.y*(steep*cos(f)), -d.y*d.y*(steep*sin(f)));\n"
"  return vec3(d.x*(a*cos(f)), a*sin(f), d.y*(a*cos(f)));\n"
"}\n"
"void main(){\n"
"  vec4 wp = matModel*vec4(vertexPosition,1.0);\n"
"  vec3 tang = vec3(1,0,0); vec3 bino = vec3(0,0,1); vec3 p = wp.xyz;\n"
"  vec2 xz = wp.xz;\n"
"  p += gerstner(xz, vec2(1.0,0.6), 0.10, 9.0, 0.9, time, tang, bino);\n"
"  p += gerstner(xz, vec2(-0.7,0.3), 0.08, 5.5, 1.1, time, tang, bino);\n"
"  p += gerstner(xz, vec2(0.2,-1.0), 0.06, 3.0, 1.3, time, tang, bino);\n"
"  p += gerstner(xz, vec2(0.9,-0.4), 0.04, 1.6, 1.6, time, tang, bino);\n"
"  fragNormal = normalize(cross(bino, tang));\n"
"  fragPos = p; fragUV = vertexTexCoord;\n"
"  gl_Position = matProjection*matView*vec4(p, 1.0);\n"
"  viewDepth = gl_Position.w;\n"
"}\n";

static const char *WATER_FS =
"#version 330\n"
"in vec3 fragPos; in vec3 fragNormal; in vec2 fragUV; in float viewDepth;\n"
"uniform sampler2D texture0; /* heightmap R32 */\n"
"uniform vec3 sunDir; uniform vec3 sunColor; uniform vec3 ambientColor; uniform vec3 fogColor; uniform vec3 viewPos;\n"
"uniform float time; uniform float fogDensity; uniform float worldSize; uniform vec3 skyColor; uniform vec3 horizonColor;\n"
"uniform vec4 ripples[16]; uniform int rippleCount;\n"
"out vec4 finalColor;\n"
NOISE_GLSL
"void main(){\n"
"  vec3 N = normalize(fragNormal);\n"
"  // микро-нормали (рябь)\n"
"  vec2 uv = fragPos.xz;\n"
"  float n1 = vnoise(uv*0.9 + vec2(time*0.35, time*0.2)); float n2 = vnoise(uv*1.7 - vec2(time*0.25, time*0.3));\n"
"  N = normalize(N + vec3((n1-0.5)*0.35, 0.0, (n2-0.5)*0.35));\n"
"  // круги от объектов\n"
"  for (int i=0;i<rippleCount;i++){ vec4 r = ripples[i]; float d = length(fragPos.xz - r.xy); float age = r.z; float amp = r.w;\n"
"    float ring = exp(-pow((d - age*2.2)*2.0, 2.0))*amp*exp(-age*0.8)*smoothstep(0.0, 0.1, age);\n"
"    vec2 dir = (d>0.001)? (fragPos.xz - r.xy)/d : vec2(0.0); N = normalize(N + vec3(dir.x, 0.0, dir.y)*ring*1.5); }\n"
"  vec3 L = normalize(-sunDir); vec3 V = normalize(viewPos - fragPos);\n"
"  // глубина по карте высот\n"
"  vec2 huv = fragPos.xz/worldSize + 0.5;\n"
"  float ground = texture(texture0, clamp(huv, 0.0, 1.0)).r;\n"
"  float depth = max(fragPos.y - ground, 0.0);\n"
"  vec3 shallow = vec3(0.32, 0.78, 0.74); vec3 deep = vec3(0.02, 0.22, 0.38);\n"
"  vec3 waterCol = mix(shallow, deep, clamp(depth*0.22, 0.0, 1.0));\n"
"  // небо/френель\n"
"  float fres = pow(1.0 - max(dot(N, V), 0.0), 4.0); fres = mix(0.06, 0.85, fres);\n"
"  vec3 R = reflect(-V, N); float upness = clamp(R.y, 0.0, 1.0); vec3 refl = mix(horizonColor, skyColor, pow(upness, 0.5));\n"
"  float ndl = max(dot(N,L),0.0);\n"
"  vec3 col = mix(waterCol*(ambientColor*0.9 + sunColor*ndl*0.7), refl, fres);\n"
"  // каустика сквозь воду на мелководье\n"
"  float c = caustic(fragPos.xz*0.4, time); col += sunColor*c*0.35*exp(-depth*0.45)*ndl;\n"
"  // блик солнца\n"
"  vec3 H = normalize(L+V); float spec = pow(max(dot(N,H),0.0), 180.0)*2.0 + pow(max(dot(N,H),0.0), 20.0)*0.15;\n"
"  col += sunColor*spec*clamp(L.y*4.0, 0.0, 1.0);\n"
"  // пена у берега + на гребнях\n"
"  float foamNoise = fbm(uv*1.4 + vec2(time*0.15, -time*0.1));\n"
"  float edge = 1.0 - smoothstep(0.0, 1.1, depth + foamNoise*0.5);\n"
"  float line1 = smoothstep(0.35, 0.5, sin(depth*7.0 - time*1.8 + foamNoise*3.0))*(1.0 - smoothstep(0.4, 1.6, depth));\n"
"  float crest = smoothstep(0.55, 0.9, fragPos.y*2.5 + foamNoise*0.3)*0.5;\n"
"  float foam = clamp(edge*1.2 + line1*0.6 + crest, 0.0, 1.0);\n"
"  for (int i=0;i<rippleCount;i++){ vec4 r = ripples[i]; float d = length(fragPos.xz - r.xy); float ring = exp(-pow((d - r.z*2.2)*2.5, 2.0))*r.w*exp(-r.z*1.2); foam += ring*0.8; }\n"
"  col = mix(col, vec3(0.95, 0.98, 1.0)*(ambientColor*0.6+sunColor*0.6), clamp(foam,0.0,1.0));\n"
"  float alpha = clamp(0.55 + depth*0.25 + fres*0.3 + foam, 0.0, 1.0);\n"
"  alpha *= smoothstep(0.0, 0.12, depth);\n"
"  float dist = length(viewPos - fragPos); float fog = 1.0 - exp(-dist*fogDensity);\n"
"  col = mix(col, fogColor, clamp(fog,0.0,1.0));\n"
"  finalColor = vec4(col, alpha);\n"
"}\n";

// ---------- SKY (полусфера) ----------
static const char *SKY_VS =
"#version 330\n"
"in vec3 vertexPosition; uniform mat4 mvp; out vec3 dir;\n"
"void main(){ dir = vertexPosition; gl_Position = mvp*vec4(vertexPosition,1.0); }\n";

static const char *SKY_FS =
"#version 330\n"
"in vec3 dir; uniform vec3 sunDir; uniform vec3 sunColor; uniform vec3 skyColor; uniform vec3 horizonColor; uniform float time; uniform float night;\n"
"out vec4 finalColor;\n"
NOISE_GLSL
"void main(){\n"
"  vec3 d = normalize(dir); float h = clamp(d.y, -1.0, 1.0);\n"
"  vec3 col = mix(horizonColor, skyColor, pow(max(h,0.0), 0.6));\n"
"  if (h < 0.0) col = horizonColor*0.9;\n"
"  vec3 L = normalize(-sunDir); float sd = max(dot(d, L), 0.0);\n"
"  col += sunColor*(pow(sd, 400.0)*3.0 + pow(sd, 8.0)*0.25);\n"
"  // облака\n"
"  if (h > 0.02) { vec2 uv = d.xz/(h+0.15)*1.6 + vec2(time*0.012, time*0.006);\n"
"    float c = fbm(uv*1.5); c = smoothstep(0.52, 0.75, c); vec3 cloud = mix(horizonColor, vec3(1.0), 0.7)*(0.75+0.25*sd);\n"
"    col = mix(col, cloud, c*smoothstep(0.02, 0.2, h)*(1.0 - night*0.6)); }\n"
"  // звёзды\n"
"  if (night > 0.01 && h > 0.0) { vec2 suv = d.xz/(h+0.3)*60.0; float s = hash21(floor(suv)); float st = step(0.985, s)*smoothstep(0.0,1.0,h)*night; col += vec3(st)*(0.6+0.4*sin(time*3.0+s*100.0)); }\n"
"  finalColor = vec4(col, 1.0);\n"
"}\n";

// ---------- SHADOW (depth only) ----------
static const char *SHADOW_VS =
"#version 330\n"
"in vec3 vertexPosition; uniform mat4 mvp;\n"
"void main(){ gl_Position = mvp*vec4(vertexPosition,1.0); }\n";
static const char *SHADOW_FS =
"#version 330\n"
"out vec4 finalColor; void main(){ finalColor = vec4(1.0); }\n";

// ---------- POST: блум + DOF (по alpha-глубине) + виньетка + грейдинг ----------
static const char *POST_FS =
"#version 330\n"
"in vec2 fragTexCoord; in vec4 fragColor; uniform sampler2D texture0; uniform vec2 resolution; uniform float time;\n"
"uniform float bloomStrength; uniform float dofStrength; uniform float focusDepth; uniform float vignette; uniform float underwater; uniform float flash;\n"
"out vec4 finalColor;\n"
"vec3 sampleBlur(vec2 uv, float radius){ vec3 acc = vec3(0.0); float tot = 0.0; vec2 px = radius/resolution;\n"
"  for(int x=-2;x<=2;x++) for(int y=-2;y<=2;y++){ float w = 1.0/(1.0+float(x*x+y*y)*0.4); acc += texture(texture0, uv + vec2(x,y)*px).rgb*w; tot += w; } return acc/tot; }\n"
"void main(){\n"
"  vec2 uv = fragTexCoord;\n"
"  if (underwater > 0.0) uv += vec2(sin(uv.y*20.0+time*2.0), cos(uv.x*20.0+time*1.7))*0.004*underwater;\n"
"  vec4 src = texture(texture0, uv); vec3 col = src.rgb; float depth = src.a;\n"
"  // DOF: размытие по разнице глубины относительно фокуса\n"
"  float coc = clamp(abs(depth - focusDepth)*6.0 - 0.05, 0.0, 1.0)*dofStrength;\n"
"  if (coc > 0.01) col = mix(col, sampleBlur(uv, 1.0 + coc*3.0), coc);\n"
"  // bloom: яркие участки размываем и добавляем\n"
"  vec3 bl = sampleBlur(uv, 4.0); vec3 bright = max(bl - vec3(0.75), vec3(0.0)); col += bright*bloomStrength*1.6;\n"
"  // лёгкая хроматическая аберрация по краям\n"
"  vec2 c = uv - 0.5; float r2 = dot(c,c);\n"
"  col.r = mix(col.r, texture(texture0, uv + c*r2*0.02).r, 0.5);\n"
"  // грейдинг: тёплые света, холодные тени\n"
"  col = pow(col, vec3(0.95)); col = mix(col, col*vec3(1.05, 1.0, 0.92), 0.3);\n"
"  float lum = dot(col, vec3(0.3,0.59,0.11)); col = mix(vec3(lum), col, 1.12);\n"
"  // виньетка\n"
"  col *= 1.0 - vignette*smoothstep(0.2, 0.9, r2*2.2);\n"
"  if (underwater > 0.0) col = mix(col, col*vec3(0.5,0.85,1.0), underwater*0.6);\n"
"  col += vec3(flash);\n"
"  finalColor = vec4(col, 1.0);\n"
"}\n";

// ---------- PARTICLE / BILLBOARD (без освещения, с туманом) ----------
static const char *UNLIT_FS =
"#version 330\n"
"in vec2 fragTexCoord; in vec4 fragColor; uniform sampler2D texture0; uniform vec4 colDiffuse;\n"
"out vec4 finalColor;\n"
"void main(){ vec4 t = texture(texture0, fragTexCoord)*colDiffuse*fragColor; finalColor = t; }\n";

// ---------- LINE: леска/линии, alpha = линейная глубина (из gl_FragCoord) ----------
static const char *LINE_FS =
"#version 330\n"
"in vec2 fragTexCoord; in vec4 fragColor; uniform sampler2D texture0; uniform vec4 colDiffuse;\n"
"out vec4 finalColor;\n"
"void main(){ vec4 t = texture(texture0, fragTexCoord)*colDiffuse*fragColor; float z = gl_FragCoord.z*2.0 - 1.0; float n = 0.1; float f = 600.0;\n"
"  float lin = 2.0*n*f/(f + n - z*(f - n)); finalColor = vec4(t.rgb, clamp(lin/300.0, 0.0, 1.0)); }\n";
